package org.myeslib.util.hazelcast;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;

import org.myeslib.util.jdbi.ClobMapperToString;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.PreparedBatch;
import org.skife.jdbi.v2.TransactionCallback;
import org.skife.jdbi.v2.TransactionIsolationLevel;
import org.skife.jdbi.v2.TransactionStatus;
import org.skife.jdbi.v2.tweak.HandleCallback;

import com.hazelcast.core.QueueStore;

@Slf4j
public class HzStringQueueStore implements QueueStore<String> {
	
	private final DBI dbi;
	private final String tableName;

	public HzStringQueueStore(final DataSource ds, final String tableName){
		this.dbi = new DBI(ds);
		this.tableName = tableName;
	}
	
	public void createTableForQueue() {
		log.warn(String.format("checking if table %s exists for queue storage", tableName));
		dbi.inTransaction(new TransactionCallback<Integer>() {
			@Override
			public Integer inTransaction(Handle h, TransactionStatus ts)
					throws Exception {
				h.execute(String.format("create table if not exists %s(id bigint, value clob)", tableName));
				return 0;
			}
		}) ;
	}

	@Override
	public void delete(Long id) {
		deleteAll(Arrays.asList(id));
	}

	@Override
	public void deleteAll(final Collection<Long> ids) {
		log.info(String.format("deleting all within table %s", tableName));
		dbi.inTransaction(new TransactionCallback<Integer>() {
			@Override
			public Integer inTransaction(Handle h, TransactionStatus ts)
					throws Exception {
				PreparedBatch pb = h.prepareBatch(String.format("delete from %s where id = :id", tableName));
				for (Long id: ids){
					 pb.add().bind("id", id);
				}
				return pb.execute().length;
			}
		}) ;
	}

	@Override
	public String load(final Long id) {
		String result = null;
		try {
			log.info("will load {} from table {}", id.toString(), tableName);
			result = dbi.inTransaction(TransactionIsolationLevel.READ_COMMITTED,
					new TransactionCallback<String>() {
						@Override
						public String inTransaction(Handle h, TransactionStatus ts) throws Exception {
							String sql = String.format("select value from %s where id = :id", tableName);
							return h.createQuery(sql).bind("id", id).map(ClobMapperToString.FIRST).first();
						}
					});
			if (result==null) {
				log.warn("found a null value for id {}", id.toString());
			} else {
				log.info("loaded {} {} from table {}", id.toString(), result, tableName);
			}
		} catch (Exception e) {
			log.error("error when loading {} from table {}", id.toString(), tableName);
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public Map<Long, String> loadAll(Collection<Long> ids) {
		log.info(String.format("loading all within table %s", tableName));
		Map<Long, String> result = new HashMap<>(); 
		for (Long id : ids){
			result.put(id, load(id));
		}
		return result;
	}

	@Override
	public Set<Long> loadAllKeys() {
		log.info(String.format("loading all keys  within table %s", tableName));
		List<Long> result = dbi.withHandle(new HandleCallback<List<Long>>() {
			@Override
			public List<Long> withHandle(Handle h) throws Exception {
				return h.createQuery(String.format("select id from %s", tableName))
				 .map(Long.class).list();
			}
		});
		return new HashSet<Long>(result);
	}

	@Override
	public void store(Long id, String value) {
		Map<Long, String> map = new HashMap<>();
		map.put(id, value);
		storeAll(map);
	}

	@Override
	public void storeAll(final Map<Long, String> items) {
		log.info(String.format("storing all within table %s", tableName));		
		dbi.inTransaction(new TransactionCallback<Integer>() {
			@Override
			public Integer inTransaction(Handle h, TransactionStatus ts)
					throws Exception {
				PreparedBatch pb = h.prepareBatch(String.format("insert into %s (id, value) values (:id, :value)", tableName));
				for (Map.Entry<Long, String> entry : items.entrySet()){
					log.info("store " + entry.getKey() + ", value = " + entry.getValue());
					pb.add().bind("id", entry.getKey()).bind("value", entry.getValue());
				}
				return pb.execute().length;
			}
		}) ;
	}
	
}
