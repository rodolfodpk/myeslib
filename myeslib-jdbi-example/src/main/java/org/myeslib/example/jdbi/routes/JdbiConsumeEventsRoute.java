package org.myeslib.example.jdbi.routes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.hazelcast.HazelcastConstants;
import org.apache.camel.processor.aggregate.UseLatestAggregationStrategy;
import org.myeslib.core.data.Snapshot;
import org.myeslib.core.storage.SnapshotReader;
import org.myeslib.example.SampleDomain.InventoryItemAggregateRoot;
import org.myeslib.example.jdbi.infra.HazelcastData;
import org.myeslib.util.jdbi.ArTablesMetadata;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.skife.jdbi.v2.util.BigDecimalMapper;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

public class JdbiConsumeEventsRoute extends RouteBuilder {

	final static String PAYMENT_ID = "paymentId";
	final static String LAST_SEQ_NUMBER = "lastSeqNumber";
	final static String HOW_MANY_UOWS_FOUND = "howManyUowFound";
	
	final DBI dbi;
	final ArTablesMetadata tablesMetadata;
	final SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader;

	@Inject
	public JdbiConsumeEventsRoute(DBI dbi, ArTablesMetadata tablesMetadata, SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader) {
		this.dbi = dbi;
		this.tablesMetadata = tablesMetadata;
		this.snapshotReader = snapshotReader;
	}

	@Override
	public void configure() throws Exception {

	  from("timer://consume-uow?fixedRate=true&period=60s")
		.routeId("timer:consume-uow")
        .log("Starting new job") // TODO lock a map entry for this aggregateRoot while processing
        .process(new GetLastSequenceNumber())
        .log("lastSeqNumber for this aggregateRoot ${body}")
        .process(new GetIdsSinceLastSeqNumber())
        .choice()
        	.when(header(HOW_MANY_UOWS_FOUND).isGreaterThan(0))
        		.log("found ${header.howManyUowFound} events to synchronize")
        		.to("direct:sincronizeNewEvents")
        	.otherwise()
        		.log("did not found anything to synchronize")
        .end()
       ; 
        
      from("direct:sincronizeNewEvents")  	
        .split(body()).shareUnitOfWork()
        .streaming().parallelProcessing()
        .process(new Processor() {
			@Override
			public void process(Exchange e) throws Exception {
				UUID id = e.getIn().getBody(UUID.class);
				Snapshot<InventoryItemAggregateRoot> snapshot = snapshotReader.get(id);
				e.getOut().setHeaders(e.getIn().getHeaders());
				e.getOut().setHeader(PAYMENT_ID, id);
				e.getOut().setBody(snapshot);	
			}
		})
	  	.wireTap("direct:reflect-last-snapshot")
	  	.wireTap("direct:reflect-query-model")
	  	.setBody(header(LAST_SEQ_NUMBER))
	  	.aggregate(constant(true), new UseLatestAggregationStrategy()).completionSize(header(HOW_MANY_UOWS_FOUND))
	  	.log("now let's update the last sequence synchronized")
 	  	.process(new UpdateLastSequenceNumber())
	  	;
      
      from("direct:reflect-last-snapshot")
         .routeId("direct:reflect-last-snapshot")
         //.log("updating the last snapshot map")
 	 	 .setHeader(HazelcastConstants.OPERATION, constant(HazelcastConstants.PUT_OPERATION))
 	 	 .setHeader(HazelcastConstants.OBJECT_ID, header(PAYMENT_ID))
 	 	 .toF("hz:%s%s", HazelcastConstants.MAP_PREFIX, HazelcastData.INVENTORY_ITEM_LAST_SNAPSHOT.name());
      
      from("direct:reflect-query-model")
      	.routeId("direct:reflect-query-model")
      	//.log("updating the query model on database")
        .process(new Processor() {
			@Override
			public void process(Exchange e) throws Exception {
				// TODO.. update query model on database
			}
		});
      
	}
	
	class GetLastSequenceNumber implements Processor {
		@Override
		public void process(Exchange e) throws Exception {
			final BigDecimal lastSeqNumber = dbi.withHandle(new HandleCallback<BigDecimal>() {
				@Override
				public BigDecimal withHandle(Handle handle) throws Exception {
					String sqlGetLastSeqNumber = "select last_seq_number from query_model_sync where aggregateRootName = :aggregateRootName";
					log.info(sqlGetLastSeqNumber);
					return handle.createQuery(sqlGetLastSeqNumber)
							.bind("aggregateRootName", tablesMetadata.getAggregateRootName())
							.map(BigDecimalMapper.FIRST)
							.first();
				}
			});
			e.getOut().setBody(lastSeqNumber == null ? 0 : lastSeqNumber, BigDecimal.class);
		}
	}

	class GetIdsSinceLastSeqNumber implements Processor {
		@Override
		public void process(Exchange e) throws Exception {
			final BigDecimal lastSeqNumber = e.getIn().getBody(BigDecimal.class);
			List<Map<String, Object>> ids = dbi.withHandle(new HandleCallback<List<Map<String, Object>>>() {
				@Override
				public List<Map<String, Object>> withHandle(Handle handle) throws Exception {
					String sqlGetIdsSinceLastSeqNumber = 
							String.format("select id, seq_number from %s where seq_number > :last_seq_number order by seq_number", 
									tablesMetadata.getUnitOfWorkTable());
					log.info(sqlGetIdsSinceLastSeqNumber);
					return handle.createQuery(sqlGetIdsSinceLastSeqNumber)
							.bind("last_seq_number", lastSeqNumber)
							.list();
				}
			});
			if (ids.size()>0){
				final BigDecimal lastSeqNumberFromUow = (BigDecimal) ids.get(ids.size()-1).get("seq_number"); 
				List<UUID> uuids = Lists.transform(ids, new Function<Map<String, Object>, UUID>() {
					@Override
					public UUID apply(Map<String, Object> input) {
						return UUID.fromString((String) input.get("id"));
					}
				});				
				e.getOut().setBody(uuids, List.class);
				e.getOut().setHeader(LAST_SEQ_NUMBER, lastSeqNumberFromUow);
			}
			e.getOut().setHeader(HOW_MANY_UOWS_FOUND, ids.size());
		}
	}

	class UpdateLastSequenceNumber implements Processor {
		@Override
		public void process(Exchange e) throws Exception {
			final BigDecimal lastSeqNumber = e.getIn().getHeader(LAST_SEQ_NUMBER, BigDecimal.class);
			int ok = dbi.withHandle(new HandleCallback<Integer>() {
				@Override
				public Integer withHandle(Handle handle) throws Exception {
					String getLastUpdate = "update query_model_sync set last_seq_number = :last_seq_number where aggregateRootName = :aggregateRootName";
					log.info(getLastUpdate);
					return handle.createStatement(getLastUpdate)
							.bind("last_seq_number", lastSeqNumber)
							.bind("aggregateRootName", tablesMetadata.getAggregateRootName())
							.execute();
				}
			});
			log.info("updated last sequence ok = {}", ok);
		}
	}

}

