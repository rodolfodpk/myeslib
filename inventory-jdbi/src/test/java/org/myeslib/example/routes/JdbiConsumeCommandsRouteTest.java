package org.myeslib.example.routes;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.UUID;

import javax.inject.Singleton;
import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;

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
import org.mockito.Mockito;
import org.myeslib.core.data.AggregateRootHistory;
import org.myeslib.core.data.Snapshot;
import org.myeslib.core.data.UnitOfWork;
import org.myeslib.core.storage.SnapshotReader;
import org.myeslib.example.SampleDomain.CreateInventoryItem;
import org.myeslib.example.SampleDomain.IncreaseInventory;
import org.myeslib.example.SampleDomain.InventoryItemAggregateRoot;
import org.myeslib.example.SampleDomain.ItemDescriptionGeneratorService;
import org.myeslib.example.jdbi.modules.CamelModule;
import org.myeslib.example.jdbi.modules.DatabaseModule;
import org.myeslib.example.jdbi.modules.HazelcastModule;
import org.myeslib.example.jdbi.modules.InventoryItemModule;
import org.myeslib.example.jdbi.routes.JdbiConsumeCommandsRoute;
import org.myeslib.util.jdbi.AggregateRootHistoryReaderDao;
import org.myeslib.util.jdbi.ArTablesMetadata;
import org.skife.jdbi.v2.DBI;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.util.Modules;
import com.googlecode.flyway.core.Flyway;

@Slf4j
public class JdbiConsumeCommandsRouteTest extends CamelTestSupport {

	private static Injector injector ;
	
	@Produce(uri = "direct:processCommand")
	protected ProducerTemplate template;
	
	@Inject
	DataSource datasource;
	
	@Inject
	DBI dbi;
	
	@Inject
	ArTablesMetadata metadata;
	
	@Inject
	JdbiConsumeCommandsRoute consumeCommandsRoute; 
	
	@Inject
	ItemDescriptionGeneratorService service;
	
	@Inject
	SnapshotReader<UUID, InventoryItemAggregateRoot> snapshotReader;
	
	@Inject 
	AggregateRootHistoryReaderDao<UUID> historyReader;
	
	@BeforeClass public static void staticSetUp() throws Exception {
		injector = Guice.createInjector(new CamelModule(1, 1), new HazelcastModule(), 
				Modules.override(new DatabaseModule(1, 1 ), new InventoryItemModule()).with(new TestModule()));
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
			binder.bind(ItemDescriptionGeneratorService.class)
				.toInstance(Mockito.mock(ItemDescriptionGeneratorService.class));
		}
	}

	@Before public void setUp() throws Exception {
		injector.injectMembers(this);
		super.setUp();
		Flyway flyway = new Flyway();
        flyway.setDataSource(datasource);
        //flyway.setLocations("../myeslib-database/src/main/resources/db/h2");
        flyway.migrate();
	}

	@Override
	public CamelContext createCamelContext() {
		CamelContext c = new DefaultCamelContext();
		return c;
	}
	
	@Test
	public void test() {
		
		when(service.generate(any(UUID.class))).thenReturn("an inventory item description from mock");
		
		UUID id = UUID.randomUUID();
		CreateInventoryItem command1 = new CreateInventoryItem(id, 0L, null);
		command1.setService(service);;
		template.sendBody(command1); 
		
		IncreaseInventory command2 = new IncreaseInventory(id, 2, 1L);
		UnitOfWork uow = template.requestBody(command2, UnitOfWork.class);
		
		AggregateRootHistory historyFromDatabase = historyReader.get(id);
		
		UnitOfWork lastUow = historyFromDatabase.getUnitsOfWork().get(historyFromDatabase.getUnitsOfWork().size()-1);
		
		assertEquals(uow, lastUow);
		
		Snapshot<InventoryItemAggregateRoot> snapshot = snapshotReader.get(command1.getId());
		
		assertTrue(snapshot.getAggregateInstance().getAvailable() == 2);
		
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
	

}
