package org.myeslib.jdbi;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.Before;
import org.junit.Test;
import org.myeslib.core.data.UnitOfWork;
import org.myeslib.example.SampleDomain.DecreaseInventory;
import org.myeslib.example.SampleDomain.IncreaseInventory;
import org.myeslib.example.SampleDomain.InventoryDecreased;
import org.myeslib.example.SampleDomain.InventoryIncreased;
import org.myeslib.example.SampleDomainGsonFactory;
import org.myeslib.jdbi.JdbiAggregateRootHistoryWriter;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.TransactionIsolationLevel;

import com.google.common.io.CharStreams;
import com.google.gson.Gson;

public class JdbiAggregateRootHistoryWriterTest {

	final Gson gson = new SampleDomainGsonFactory().create();

	DataSource ds ;
	DBI dbi ;
	
	@Before
	public void setup() {
		ds = JdbcConnectionPool.create("jdbc:h2:mem:test;MODE=Oracle", "scott", "tiger");
		dbi =  new DBI(ds);
		Handle handle = dbi.open();
		JdbiAggregateRootHistoryWriter w = new JdbiAggregateRootHistoryWriter("a1_units_of_work", gson);
		w.createTables(handle);
		handle.close();
	}

	@Test 
	public void testJustOne() throws UnsupportedEncodingException, IOException, SQLException {
		
		Handle handle = dbi.open();
		handle.setTransactionIsolation(TransactionIsolationLevel.SERIALIZABLE);
		handle.begin();

		JdbiAggregateRootHistoryWriter w = new JdbiAggregateRootHistoryWriter("a1_units_of_work", gson);
	
		UUID id = UUID.randomUUID();

		UnitOfWork newUow = UnitOfWork.create(new IncreaseInventory(id, 1), 0l, Arrays.asList(new InventoryIncreased(id, 1)));
	
		w.insert(id, newUow, handle);
		
		handle.commit();
		handle.close();
		
		Handle readHandle = dbi.open();
		
		List<Map<String, Object>> rs = readHandle.select("select * from a1_units_of_work where aggregate_root_id = ?", id.toString());
		
		Map<String, Object> row = rs.get(0);
		
		// checks all fields except timestamp since db will change it on insert
		Clob uowAsClob = (Clob) row.get("uow_data");
		String uowAsString = CharStreams.toString( new InputStreamReader( uowAsClob.getAsciiStream(), "UTF-8" ) );
		UnitOfWork uowFromDb = gson.fromJson(uowAsString, UnitOfWork.class);
		assertEquals(newUow.getCommand(), uowFromDb.getCommand());
		assertEquals(newUow.getEvents(), uowFromDb.getEvents());
		assertEquals(((Number) newUow.getVersion()).longValue(), ((Number)row.get("version")).longValue());

	}
	
	@Test
	public void testRollback() throws UnsupportedEncodingException, IOException, SQLException {
		
		Handle handle = dbi.open();
		handle.setTransactionIsolation(TransactionIsolationLevel.SERIALIZABLE);
		handle.begin();
		
		JdbiAggregateRootHistoryWriter w = new JdbiAggregateRootHistoryWriter("a1_units_of_work", gson);
		
		UUID id = UUID.randomUUID();

		UnitOfWork newUow = UnitOfWork.create(new IncreaseInventory(id, 1), 0l, Arrays.asList(new InventoryIncreased(id, 1)));
	
		w.insert(id, newUow, handle);

		handle.rollback();
		handle.close();

		Handle readHandle = dbi.open();
		
		List<Map<String, Object>> rs = readHandle.select("select * from a1_units_of_work where aggregate_root_id = ?", id.toString());
		
		assertEquals(rs.size(), 0);
		
	}
	
	@Test
	public void testACouple() throws UnsupportedEncodingException, IOException, SQLException {
		
		Handle handle = dbi.open();

		JdbiAggregateRootHistoryWriter w = new JdbiAggregateRootHistoryWriter("a1_units_of_work", gson);
		
		UUID id = UUID.randomUUID();

		UnitOfWork newUow1 = UnitOfWork.create(new IncreaseInventory(id, 1), 0l, Arrays.asList(new InventoryIncreased(id, 1)));
		UnitOfWork newUow2 = UnitOfWork.create(new DecreaseInventory(id, 2), 0l, Arrays.asList(new InventoryDecreased(id, 2)));
	
		w.insert(id, newUow1, handle);
		w.insert(id, newUow2, handle);

		handle.commit();
		handle.close();
		
		Handle readHandle = dbi.open();
		
		List<Map<String, Object>> rs = readHandle.select("select * from a1_units_of_work where aggregate_root_id = ?", id.toString());
		
		// checks all fields except timestamp since db will change it on insert
		Map<String, Object> row1 = rs.get(0);
		Clob uowAsClob1 = (Clob) row1.get("uow_data");
		String uowAsString1 = CharStreams.toString( new InputStreamReader( uowAsClob1.getAsciiStream(), "UTF-8" ) );
		UnitOfWork uowFromDb1 = gson.fromJson(uowAsString1, UnitOfWork.class);
		assertEquals(newUow1.getCommand(), uowFromDb1.getCommand());
		assertEquals(newUow1.getEvents(), uowFromDb1.getEvents());
		assertEquals(((Number) newUow1.getVersion()).longValue(), ((Number)row1.get("version")).longValue());
	
		Map<String, Object> row2 = rs.get(1);
		Clob uowAsClob2 = (Clob) row2.get("uow_data");
		String uowAsString2 = CharStreams.toString( new InputStreamReader( uowAsClob2.getAsciiStream(), "UTF-8" ) );
		UnitOfWork uowFromDb2 = gson.fromJson(uowAsString2, UnitOfWork.class);
		assertEquals(newUow2.getCommand(), uowFromDb2.getCommand());
		assertEquals(newUow2.getEvents(), uowFromDb2.getEvents());
		assertEquals(((Number) newUow2.getVersion()).longValue(), ((Number)row2.get("version")).longValue());
	
	}
	
	
}
