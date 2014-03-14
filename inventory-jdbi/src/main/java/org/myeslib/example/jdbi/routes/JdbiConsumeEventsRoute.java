package org.myeslib.example.jdbi.routes;

import java.math.BigDecimal;
import java.util.List;
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
import org.myeslib.util.hazelcast.HzJobLocker;
import org.myeslib.util.jdbi.ArTablesMetadata;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.skife.jdbi.v2.util.BigDecimalMapper;
import org.skife.jdbi.v2.util.StringMapper;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

public class JdbiConsumeEventsRoute extends RouteBuilder {

	final static String INVENTORY_ITEM_ID = "inventoryItemId";
	final static String PREVIOUS_SEQ_NUMBER = "previousSeqNumber";
	final static String LATEST_SEQ_NUMBER = "latestSeqNumber";
	final static String HOW_MANY_UOWS_FOUND = "howManyUowFound";

	final HzJobLocker jobLocker;
	final DBI dbi;
	final ArTablesMetadata tablesMetadata;
	final SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader;

	@Inject
	public JdbiConsumeEventsRoute(HzJobLocker jobLocker, DBI dbi, ArTablesMetadata tablesMetadata, 
								  SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader) {
		this.jobLocker = jobLocker;
		this.dbi = dbi;
		this.tablesMetadata = tablesMetadata;
		this.snapshotReader = snapshotReader;
	}

	@Override
	public void configure() throws Exception {

	  from("timer://consume-uow?fixedRate=true&period=60s")
		.routeId("timer:consume-uow")  
	  	.choice()
	  	    .when(method(jobLocker, "lock"))
	  		   .to("direct:collectEvents")
	  		.otherwise()
	  		   .log(HzJobLocker.LOCK_FAILED_MSG) 
  		.end();		
		  
	  from("direct:collectEvents")
		.routeId("direct:collectEvents")
	    .log("Starting new job") 
        .process(new GetPreviousSequenceNumber())
        .log("previousSeqNumber for this aggregateRoot ${header.previousSeqNumber}")
        .process(new GetLatestSeqNumber())
        .log("latestSeqNumber for this aggregateRoot ${header.latestSeqNumber}")
        .process(new GetIdsFromInterval())
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
				e.getOut().setHeader(INVENTORY_ITEM_ID, id);
				e.getOut().setBody(snapshot);	
			}
		})
	  	.wireTap("direct:reflect-last-snapshot")
	  	.wireTap("direct:reflect-query-model")
	  	.setBody(header(LATEST_SEQ_NUMBER))
	  	.aggregate(constant(true), new UseLatestAggregationStrategy()).completionSize(header(HOW_MANY_UOWS_FOUND))
	  	.log("now let's update the last sequence synchronized")
 	  	.process(new UpdatePreviousSequenceNumber())
	  	;
      
      from("direct:reflect-last-snapshot")
         .routeId("direct:reflect-last-snapshot")
         //.log("updating the last snapshot map")
 	 	 .setHeader(HazelcastConstants.OPERATION, constant(HazelcastConstants.PUT_OPERATION))
 	 	 .setHeader(HazelcastConstants.OBJECT_ID, header(INVENTORY_ITEM_ID))
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
	
	class GetPreviousSequenceNumber implements Processor {
		@Override
		public void process(Exchange e) throws Exception {
			final BigDecimal previousSeqNumber = dbi.withHandle(new HandleCallback<BigDecimal>() {
				@Override
				public BigDecimal withHandle(Handle handle) throws Exception {
					String sqlGetLastSeqNumber = "select last_seq_number from query_model_sync where aggregateRootName = :aggregateRootName";
					log.debug(sqlGetLastSeqNumber);
					return handle.createQuery(sqlGetLastSeqNumber)
							.bind("aggregateRootName", tablesMetadata.getAggregateRootName())
							.map(BigDecimalMapper.FIRST)
							.first();
				}
			});
			e.getOut().setHeader(PREVIOUS_SEQ_NUMBER, previousSeqNumber == null ? 0 : previousSeqNumber);
		}
	}

	class GetLatestSeqNumber implements Processor {
		@Override
		public void process(Exchange e) throws Exception {
			final BigDecimal previousSeqNumber = e.getIn().getHeader(PREVIOUS_SEQ_NUMBER, BigDecimal.class);
			final BigDecimal latestSeqNumber = dbi.withHandle(new HandleCallback<BigDecimal>() {
				@Override
				public BigDecimal withHandle(Handle handle) throws Exception {
					String sqlGetIdsSinceLastSeqNumber = 
							String.format("select max(seq_number) from %s where seq_number > :previous_seq_number", tablesMetadata.getUnitOfWorkTable());
					log.debug(sqlGetIdsSinceLastSeqNumber);
					return handle.createQuery(sqlGetIdsSinceLastSeqNumber)
							.bind("previous_seq_number", previousSeqNumber)
							.map(BigDecimalMapper.FIRST)
							.first();
				}
			});
			e.getOut().setHeader(PREVIOUS_SEQ_NUMBER, previousSeqNumber);
			e.getOut().setHeader(LATEST_SEQ_NUMBER, latestSeqNumber == null ? previousSeqNumber : latestSeqNumber);
		}
	}

	class GetIdsFromInterval implements Processor {
		@Override
		public void process(Exchange e) throws Exception {
			final BigDecimal previousSeqNumber = e.getIn().getHeader(PREVIOUS_SEQ_NUMBER, BigDecimal.class);
			final BigDecimal latestSeqNumber = e.getIn().getHeader(LATEST_SEQ_NUMBER, BigDecimal.class);
			List<String> ids = dbi.withHandle(new HandleCallback<List<String>>() {
				@Override
				public List<String> withHandle(Handle handle) throws Exception {
					String sqlGetIdsSinceLastSeqNumber = 
							String.format("select distinct id from %s where seq_number between :previous_seq_number +1 and :latest_seq_number", 
									tablesMetadata.getUnitOfWorkTable());
					log.debug(sqlGetIdsSinceLastSeqNumber);
					return handle.createQuery(sqlGetIdsSinceLastSeqNumber)
							.bind("previous_seq_number", previousSeqNumber)
							.bind("latest_seq_number", latestSeqNumber)
							.map(StringMapper.FIRST)
							.list();
				}
			});
			List<UUID> uuids = Lists.transform(ids, new Function<String, UUID>() {
				@Override
				public UUID apply(String input) {
					return UUID.fromString((String) input);
				}
			});				
			e.getOut().setBody(uuids, List.class);
			e.getOut().setHeader(HOW_MANY_UOWS_FOUND, ids.size());
			e.getOut().setHeader(LATEST_SEQ_NUMBER, latestSeqNumber);
		}
	}

	class UpdatePreviousSequenceNumber implements Processor {
		@Override
		public void process(Exchange e) throws Exception {
			final BigDecimal lastSeqNumber = e.getIn().getHeader(LATEST_SEQ_NUMBER, BigDecimal.class);
			int ok = dbi.withHandle(new HandleCallback<Integer>() {
				@Override
				public Integer withHandle(Handle handle) throws Exception {
					String getLastUpdate = "update query_model_sync set last_seq_number = :last_seq_number where aggregateRootName = :aggregateRootName";
					log.debug(getLastUpdate);
					return handle.createStatement(getLastUpdate)
							.bind("last_seq_number", lastSeqNumber)
							.bind("aggregateRootName", tablesMetadata.getAggregateRootName())
							.execute();
				}
			});
			log.info("updated previous sequence ok = {}", ok);
		}
	}

}

