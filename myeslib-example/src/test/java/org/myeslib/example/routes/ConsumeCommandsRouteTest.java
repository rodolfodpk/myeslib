package org.myeslib.example.routes;

import java.util.UUID;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myeslib.data.AggregateRootHistory;
import org.myeslib.data.UnitOfWork;
import org.myeslib.example.ExampleModule;
import org.myeslib.example.SampleCoreDomain.CreateInventoryItem;
import org.myeslib.example.SampleCoreDomain.IncreaseInventory;
import org.myeslib.example.infra.HazelcastMaps;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.skife.jdbi.v2.util.ByteArrayMapper;

import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

@Slf4j
public class ConsumeCommandsRouteTest extends CamelTestSupport {

	private static Injector injector ;
	@Produce(uri = "direct:handle-inventory-item-command")
	protected ProducerTemplate template;
	
	@EndpointInject(uri = "mock:result")
	protected MockEndpoint resultEndpoint;
	
	@Inject
	DataSource ds;
	
	@Inject
	ConsumeCommandsRoute consumeCommandsRoute;
	
	@BeforeClass public static void staticSetUp() throws Exception {
		injector = Guice.createInjector(new ExampleModule());
	}

	@Before public void setUp() throws Exception {
		injector.injectMembers(this);
		super.setUp();
	}

	@Test
	public void test() {
		
		CreateInventoryItem command1 = new CreateInventoryItem(UUID.randomUUID());
		command1.setService(new ServiceJustForTest());
		template.sendBody(command1);
		
		IncreaseInventory command2 = new IncreaseInventory(command1.getId(), 2);
		UnitOfWork uow = template.requestBody(command2, UnitOfWork.class);
		
		String fromDatabaseAsJson = getAggregateRootHistoryAsJson(command1.getId().toString());
		AggregateRootHistory historyFromDatabase = injector.getInstance(Gson.class).fromJson(fromDatabaseAsJson, AggregateRootHistory.class);
		UnitOfWork lastUow = historyFromDatabase.getUnitsOfWork().get(historyFromDatabase.getUnitsOfWork().size()-1);
		
		assertEquals(uow, lastUow);
		
		log.info("result value after sending the command: {}", uow);
//		log.info("value on aggregateRootMap: {}", aggregateMapFactory.get(HazelcastMaps.INVENTORY_ITEM_AGGREGATE_HISTORY.name()).get(command1.getId()));
//		log.info("value on table: \n{}", getAggregateRootHistoryAsJson(command1.getId().toString()));
//		log.info("value on snapshotMap: {}", snapshotMapFactory.get(HazelcastMaps.INVENTORY_ITEM_LAST_SNAPSHOT.name()).get(command1.getId()));
//		
	}
	
   @Override
    protected RouteBuilder createRouteBuilder() {
	   
	   return consumeCommandsRoute;
	   
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
