package org.myeslib.example.routes;

import java.util.UUID;

import javax.inject.Singleton;
import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myeslib.core.data.Snapshot;
import org.myeslib.core.data.UnitOfWork;
import org.myeslib.core.storage.SnapshotReader;
import org.myeslib.example.SampleDomain.CreateInventoryItem;
import org.myeslib.example.SampleDomain.IncreaseInventory;
import org.myeslib.example.SampleDomain.InventoryItemAggregateRoot;
import org.myeslib.example.SampleDomain.ItemDescriptionGeneratorService;
import org.myeslib.example.hazelcast.infra.HazelcastData;
import org.myeslib.example.hazelcast.modules.CamelModule;
import org.myeslib.example.hazelcast.modules.DatabaseModule;
import org.myeslib.example.hazelcast.modules.HazelcastModule;
import org.myeslib.example.hazelcast.modules.InventoryItemModule;
import org.myeslib.example.hazelcast.routes.HzConsumeCommandsRoute;
import org.myeslib.util.hazelcast.HzCamelComponent;
import org.myeslib.util.jdbi.ArTablesMetadata;
import org.myeslib.util.jdbi.ClobToStringMapper;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.tweak.HandleCallback;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;
import com.googlecode.flyway.core.Flyway;

@Slf4j
public class HzConsumeCommandsRouteTest extends CamelTestSupport {

	private static Injector injector ;
	
	@Produce(uri = "direct:handle-inventory-item-command")
	protected ProducerTemplate template;
	
	@EndpointInject(uri = "mock:result")
	protected MockEndpoint resultEndpoint;
	
	@Inject
	DataSource ds;
	
	@Inject
	HzConsumeCommandsRoute consumeCommandsRoute; 
	
	@Inject
	ItemDescriptionGeneratorService service;
	
	@Inject
	SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader; 
	
	@Inject 
	DBI dbi;
	
	@Inject
	ArTablesMetadata metadata;
	  
	@BeforeClass public static void staticSetUp() throws Exception {
		
		injector =  Guice.createInjector(Modules.override(new CamelModule(1, 1, 1), new DatabaseModule(1, 1)).with(new TestModule()), 
				 new HazelcastModule(0), new InventoryItemModule());
		
	}

	public static class TestModule implements Module {
		@Provides
		@Singleton
		public DataSource datasource() {
			JdbcConnectionPool pool = JdbcConnectionPool.create("jdbc:h2:mem:test;MODE=Oracle", "scott", "tiger");
			return pool;
		}
		@Override
		public void configure(Binder binder) {
			binder.bindConstant().annotatedWith(Names.named("originUri")).to("direct:handle-inventory-item-command");
			binder.bindConstant().annotatedWith(Names.named("eventsDestinationUri")).to("mock:result");
		}
	}

	@Before public void setUp() throws Exception {
		injector.injectMembers(this);
		super.setUp();
		Flyway flyway = new Flyway();
        flyway.setDataSource(ds);
        flyway.migrate();
	}

	@Override
	public CamelContext createCamelContext() {
		CamelContext c = new DefaultCamelContext();
		c.addComponent("hz", injector.getInstance(HzCamelComponent.class));
		return c;
	}
	
	@Test
	public void test() {
		
		CreateInventoryItem command1 = new CreateInventoryItem(UUID.randomUUID(), 0L, null);
		command1.setService(service);
		template.sendBody(command1);
		
		IncreaseInventory command2 = new IncreaseInventory(command1.getId(), 2, 1L);
		UnitOfWork uow = template.requestBody(command2, UnitOfWork.class);
		
//		String fromDatabaseAsJson = getAggregateRootHistoryAsJson(command1.getId().toString());
//		AggregateRootHistory historyFromDatabase = injector.getInstance(Gson.class).fromJson(fromDatabaseAsJson, AggregateRootHistory.class);
//		UnitOfWork lastUow = historyFromDatabase.getUnitsOfWork().get(historyFromDatabase.getUnitsOfWork().size()-1);
//		
//		assertEquals(uow, lastUow);
		
		Snapshot<InventoryItemAggregateRoot> snapshot = snapshotReader.get(command1.getId());
		
		assertTrue(snapshot.getAggregateInstance().getAvaliable() == 2);
		
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
		
		String clob = dbi.withHandle(new HandleCallback<String>() {
			@Override
			public String withHandle(Handle h) throws Exception {
				return h.createQuery(String.format("select aggregate_root_data from %s where id = :id", HazelcastData.INVENTORY_ITEM_AGGREGATE_HISTORY.name()))
						.bind("id", id)
				 .map(ClobToStringMapper.FIRST).first();
			}
		});
		
		return clob;
	}
}
