package org.myeslib.example.routes;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.UUID;

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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.myeslib.core.data.AggregateRootHistory;
import org.myeslib.core.data.Snapshot;
import org.myeslib.core.data.UnitOfWork;
import org.myeslib.core.storage.SnapshotReader;
import org.myeslib.example.HzExampleModule;
import org.myeslib.example.SampleDomain.CreateInventoryItem;
import org.myeslib.example.SampleDomain.IncreaseInventory;
import org.myeslib.example.SampleDomain.InventoryItemAggregateRoot;
import org.myeslib.example.SampleDomain.ItemDescriptionGeneratorService;
import org.myeslib.example.infra.HazelcastData;
import org.myeslib.util.hazelcast.HzCamelComponent;
import org.myeslib.util.jdbi.ClobToStringMapper;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.tweak.HandleCallback;

import com.google.gson.Gson;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;

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
	
	@BeforeClass public static void staticSetUp() throws Exception {
		injector = Guice.createInjector(Modules.override(new HzExampleModule()).with(new TestModule()));
	}

	public static class TestModule implements Module {
		@Override
		public void configure(Binder binder) {
			binder.bindConstant().annotatedWith(Names.named("originUri")).to("direct:handle-inventory-item-command");
			binder.bindConstant().annotatedWith(Names.named("eventsDestinationUri")).to("mock:result");
			binder.bind(ItemDescriptionGeneratorService.class)
				.toInstance(Mockito.mock(ItemDescriptionGeneratorService.class));
		}
	}

	@Before public void setUp() throws Exception {
		injector.injectMembers(this);
		super.setUp();
	}

	@Override
	public CamelContext createCamelContext() {
		CamelContext c = new DefaultCamelContext();
		c.addComponent("hz", injector.getInstance(HzCamelComponent.class));
		return c;
	}
	
	@Test
	public void test() {
		
		when(service.generate(any(UUID.class))).thenReturn("an inventory item description from mock");
		
		CreateInventoryItem command1 = new CreateInventoryItem(UUID.randomUUID(), 0L, null);
		command1.setService(service);
		template.sendBody(command1);
		
		IncreaseInventory command2 = new IncreaseInventory(command1.getId(), 2, 1L);
		UnitOfWork uow = template.requestBody(command2, UnitOfWork.class);
		
		String fromDatabaseAsJson = getAggregateRootHistoryAsJson(command1.getId().toString());
		AggregateRootHistory historyFromDatabase = injector.getInstance(Gson.class).fromJson(fromDatabaseAsJson, AggregateRootHistory.class);
		UnitOfWork lastUow = historyFromDatabase.getUnitsOfWork().get(historyFromDatabase.getUnitsOfWork().size()-1);
		
		assertEquals(uow, lastUow);
		
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
