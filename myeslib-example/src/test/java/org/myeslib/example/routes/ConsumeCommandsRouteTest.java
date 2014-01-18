package org.myeslib.example.routes;

import static org.myeslib.example.infra.HazelcastMaps.INVENTORY_ITEM_AGGREGATE_HISTORY;
import static org.myeslib.example.infra.HazelcastMaps.INVENTORY_ITEM_LAST_SNAPSHOT;

import java.util.UUID;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.Test;
import org.myeslib.example.SampleCoreDomain.CreateInventoryItem;
import org.myeslib.example.SampleCoreDomain.IncreaseInventory;
import org.myeslib.example.SampleCoreDomain.InventoryItemAggregateRoot;
import org.myeslib.example.infra.GsonFactory;
import org.myeslib.example.infra.HazelcastFactory;
import org.myeslib.example.infra.HazelcastMaps;
import org.myeslib.example.routes.ConsumeCommandsRoute;
import org.myeslib.hazelcast.AggregateRootHistoryMapFactory;
import org.myeslib.hazelcast.AggregateRootHistoryTxMapFactory;
import org.myeslib.hazelcast.AggregateRootSnapshotMapFactory;
import org.myeslib.hazelcast.JustAnotherHazelcastComponent;
import org.myeslib.hazelcast.SnapshotReader;
import org.myeslib.hazelcast.TransactionalCommandHandler;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.skife.jdbi.v2.util.ByteArrayMapper;

import com.google.gson.Gson;
import com.hazelcast.core.HazelcastInstance;

@Slf4j
public class ConsumeCommandsRouteTest extends CamelTestSupport {

	@Produce(uri = "direct:handle-create")
	protected ProducerTemplate template;
	
	@EndpointInject(uri = "mock:result")
	protected MockEndpoint resultEndpoint;
	
	DataSource ds = JdbcConnectionPool.create("jdbc:h2:mem:test;MODE=Oracle", "scott", "tiger");
	Gson gson = new GsonFactory().create();
	HazelcastInstance hazelcastInstance = new HazelcastFactory(ds, gson).get();
	AggregateRootHistoryMapFactory<UUID, InventoryItemAggregateRoot> aggregateMapFactory = new AggregateRootHistoryMapFactory<>(hazelcastInstance);
	AggregateRootSnapshotMapFactory<UUID, InventoryItemAggregateRoot> snapshotMapFactory = new AggregateRootSnapshotMapFactory<>(hazelcastInstance);

	@Test
	public void test() {
		
		CreateInventoryItem command1 = new CreateInventoryItem(UUID.randomUUID());
		command1.setService(new ServiceJustForTest());
		template.sendBody(command1);
		
		IncreaseInventory command2 = new IncreaseInventory(command1.getId(), 2);
		template.sendBody(command2);
		
		// TODO assertions
		
		log.info("value on aggregateRootMap: {}", aggregateMapFactory.get(HazelcastMaps.INVENTORY_ITEM_AGGREGATE_HISTORY.name()).get(command1.getId()));
		log.info("value on table: \n{}", getAggregateRootHistoryAsJson(command1.getId().toString()));
		log.info("value on snapshotMap: {}", snapshotMapFactory.get(HazelcastMaps.INVENTORY_ITEM_LAST_SNAPSHOT.name()).get(command1.getId()));
		
	}
	
   @Override
    protected RouteBuilder createRouteBuilder() {
	   
		JustAnotherHazelcastComponent hz = new JustAnotherHazelcastComponent(hazelcastInstance);
		
		context.addComponent("hz", hz);
		
		AggregateRootHistoryTxMapFactory<UUID, InventoryItemAggregateRoot> txMapFactory = new AggregateRootHistoryTxMapFactory<>();
		
		SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader = 
				new SnapshotReader<>(aggregateMapFactory.get(INVENTORY_ITEM_AGGREGATE_HISTORY.name()), 
											 snapshotMapFactory.get(INVENTORY_ITEM_LAST_SNAPSHOT.name()));
	
		TransactionalCommandHandler<UUID, InventoryItemAggregateRoot> txProcessor = 
				new TransactionalCommandHandler<>(hazelcastInstance, txMapFactory, INVENTORY_ITEM_AGGREGATE_HISTORY.name());	

		return new ConsumeCommandsRoute(snapshotReader, txProcessor);
    }
	
	String getAggregateRootHistoryAsJson(final String id){
		
		DBI dbi = new DBI(ds);
		
		byte[] clob = dbi.withHandle(new HandleCallback<byte[]>() {
			@Override
			public byte[] withHandle(Handle h) throws Exception {
				return h.createQuery(String.format("select aggregate_root_data from %s where id = :id", HazelcastMaps.INVENTORY_ITEM_AGGREGATE_HISTORY.name()))
						.bind("id", id)
				 .map(ByteArrayMapper.FIRST).first();
			}
		});
		
		return new String(clob);
	}
}
