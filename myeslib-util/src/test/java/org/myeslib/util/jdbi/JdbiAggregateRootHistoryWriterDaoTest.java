package org.myeslib.util.jdbi;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.UUID;

import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myeslib.core.data.AggregateRootHistory;
import org.myeslib.core.data.UnitOfWork;
import org.myeslib.example.SampleDomain.DecreaseInventory;
import org.myeslib.example.SampleDomain.IncreaseInventory;
import org.myeslib.example.SampleDomain.InventoryDecreased;
import org.myeslib.example.SampleDomain.InventoryIncreased;
import org.myeslib.example.SampleDomainGsonFactory;
import org.myeslib.util.gson.UowFromStringFunction;
import org.myeslib.util.gson.UowToStringFunction;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;

import com.google.common.base.Function;
import com.google.gson.Gson;
import com.googlecode.flyway.core.Flyway;


public class JdbiAggregateRootHistoryWriterDaoTest {

	static JdbcConnectionPool pool ;
	static DBI dbi ;
	static Gson gson ;
	static ArTablesMetadata metadata ;
	static Function<UnitOfWork, String> toStringFunction;
	static JdbiAggregateRootHistoryReaderDao reader;
	
	@BeforeClass
	public static void setup() {
		pool = JdbcConnectionPool.create("jdbc:h2:mem:test;MODE=Oracle", "scott", "tiger");
		dbi = new DBI(pool);
		gson = new SampleDomainGsonFactory().create();
		metadata = new ArTablesMetadata("inventory_item");
		toStringFunction = new UowToStringFunction(gson);
		reader = new JdbiAggregateRootHistoryReaderDao(metadata, dbi, new UowFromStringFunction(gson));
		Flyway flyway = new Flyway();
        flyway.setDataSource(pool);
        //flyway.setLocations("../myeslib-database/src/main/resources/db/h2");
        flyway.migrate();
	}

	
	@Test
	public void firstTransactionOnEmptyHistory() {
		
		UUID id = UUID.randomUUID();

		AggregateRootHistory toSave = new AggregateRootHistory();
		UnitOfWork newUow = UnitOfWork.create(new IncreaseInventory(id, 1, 0L), Arrays.asList(new InventoryIncreased(id, 1)));
		toSave.add(newUow);
		
		Handle h = dbi.open();
		
		JdbiAggregateRootHistoryWriterDao store = new JdbiAggregateRootHistoryWriterDao(h, metadata, toStringFunction);
		
		store.insert(id, newUow);
		
		AggregateRootHistory fromDb = reader.get(id);
		
		assertEquals(toSave, fromDb);
		
	}
	
	@Test
	public void rollback() {
		
		UUID id = UUID.randomUUID();

		AggregateRootHistory toSave = new AggregateRootHistory();
		UnitOfWork newUow = UnitOfWork.create(new IncreaseInventory(id, 1, 0L), Arrays.asList(new InventoryIncreased(id, 1)));
		toSave.add(newUow);
		
		Handle h = dbi.open();
		
		try {
			h.begin();
			JdbiAggregateRootHistoryWriterDao store = new JdbiAggregateRootHistoryWriterDao(h, metadata, toStringFunction);
			store.insert(id, newUow);
			h.rollback();

		} finally {
			h.close() ;
		}
		
		AggregateRootHistory fromDb = reader.get(id);
		
		assertEquals(fromDb.getLastVersion().intValue(), 0);
		
	}
	

	@Test
	public void appendNewOnPreviousVersion() {
		
		UUID id = UUID.randomUUID();

		UnitOfWork existingUow = UnitOfWork.create(new IncreaseInventory(id, 1, 0L), Arrays.asList(new InventoryIncreased(id, 1)));
		AggregateRootHistory existing = new AggregateRootHistory();
		existing.add(existingUow);
		
		UnitOfWork newUow = UnitOfWork.create(new DecreaseInventory(id, 1, 1L), Arrays.asList(new InventoryDecreased(id, 1)));
		
		Handle h = dbi.open();
		
		JdbiAggregateRootHistoryWriterDao store = new JdbiAggregateRootHistoryWriterDao(h, metadata, toStringFunction);
			
		store.insert(id, existingUow);
		
		store.insert(id, newUow);

		AggregateRootHistory fromDb = reader.get(id);
		
		assertEquals(fromDb.getLastVersion().intValue(), 2);

		
	}
	
	@Test(expected=UnableToExecuteStatementException.class)
	public void databaseIsHandlingOptimisticLocking() {
		
		UUID id = UUID.randomUUID();

		UnitOfWork existingUow = UnitOfWork.create(new IncreaseInventory(id, 1, 0L),  Arrays.asList(new InventoryIncreased(id, 1)));

		AggregateRootHistory existing = new AggregateRootHistory();

		existing.add(existingUow);

		Handle h = dbi.open();
		
		JdbiAggregateRootHistoryWriterDao store = new JdbiAggregateRootHistoryWriterDao(h, metadata, toStringFunction);

		store.insert(id, existingUow);
		
		UnitOfWork newUow = UnitOfWork.create(new DecreaseInventory(id, 1, 0L), Arrays.asList(new InventoryDecreased(id, 1)));
		
		store.insert(id, newUow);
		
	}

}
