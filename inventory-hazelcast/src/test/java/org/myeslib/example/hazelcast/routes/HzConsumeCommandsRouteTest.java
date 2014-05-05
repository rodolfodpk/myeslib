package org.myeslib.example.hazelcast.routes;

import java.util.UUID;

import javax.inject.Singleton;
import javax.sql.DataSource;

import org.apache.camel.CamelContext;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myeslib.core.data.AggregateRootHistory;
import org.myeslib.core.data.Snapshot;
import org.myeslib.core.data.UnitOfWork;
import org.myeslib.core.storage.SnapshotReader;
import org.myeslib.example.SampleDomain.CreateInventoryItem;
import org.myeslib.example.SampleDomain.IncreaseInventory;
import org.myeslib.example.SampleDomain.InventoryItemAggregateRoot;
import org.myeslib.example.SampleDomain.ItemDescriptionGeneratorService;
import org.myeslib.example.hazelcast.modules.CamelModule;
import org.myeslib.example.hazelcast.modules.DatabaseModule;
import org.myeslib.example.hazelcast.modules.HazelcastModule;
import org.myeslib.example.hazelcast.modules.InventoryItemModule;
import org.myeslib.util.jdbi.AggregateRootHistoryReaderDao;
import org.myeslib.util.jdbi.JdbiAggregateRootHistoryReaderDao;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;
import com.googlecode.flyway.core.Flyway;
import com.hazelcast.core.IMap;

public class HzConsumeCommandsRouteTest extends CamelTestSupport {

	private static Injector injector ;
	
	@Produce(uri = "direct:handle-inventory-item-command")
	protected ProducerTemplate template;

	@Inject
	DataSource ds;
	
	@Inject
	HzConsumeCommandsRoute consumeCommandsRoute; 
	
	@Inject
	ItemDescriptionGeneratorService service;
	
	@Inject
	SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader;

    @Inject
    AggregateRootHistoryReaderDao<UUID> dao;

    @Inject
    IMap<UUID, AggregateRootHistory> inventoryItemMap;
	  
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
            binder.bind(AggregateRootHistoryReaderDao.class).to(JdbiAggregateRootHistoryReaderDao.class).asEagerSingleton();
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
		return c;
	}
	
	@Test
	public void test() {

        final UUID id = UUID.randomUUID();
        CreateInventoryItem command1 = new CreateInventoryItem(UUID.randomUUID(), id);
		template.sendBody(command1);
		
		IncreaseInventory command2 = new IncreaseInventory(UUID.randomUUID(), command1.getId(), 2, 1L);
		template.requestBody(command2, UnitOfWork.class);

        AggregateRootHistory fromDb = dao.get(id);
        AggregateRootHistory fromMap = inventoryItemMap.get(id);

        assertEquals(fromMap, fromDb);
		
		Snapshot<InventoryItemAggregateRoot> snapshot = snapshotReader.get(command1.getId());
		
		assertTrue(snapshot.getAggregateInstance().getAvailable() == 2);
        assertTrue(snapshot.getVersion().equals(2L));

	}
	
   @Override
    protected RouteBuilder createRouteBuilder() {
	   return consumeCommandsRoute;
   }

}
