package org.myeslib.util.hazelcast;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

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
import org.skife.jdbi.v2.util.StringMapper;

import com.hazelcast.core.MapStore;

@Slf4j
public class HzStringMapStore implements MapStore<UUID, String>{

	private final DBI dbi;
	private final String tableName;
	
	public HzStringMapStore(DataSource ds, String tableName){
		this.dbi = new DBI(ds);
		this.tableName = tableName;
	}
	
	public void createTableForMap() {
		log.warn(String.format("checking if table %s exists for map storage", tableName));
		dbi.inTransaction(new TransactionCallback<Integer>() {
			@Override
			public Integer inTransaction(Handle h, TransactionStatus ts)
					throws Exception {
				h.execute(String.format("create table if not exists %s(id varchar(36) not null, "
																    + "aggregate_root_data clob not null, "
																    + "constraint aggregate_pk primary key (id))", 
									    tableName));
				return 0;
			}
		}) ;
	}
	
	@Override
	public Set<UUID> loadAllKeys() {
		// TODO define how many keys will be pre-loaded
		log.debug("loading all keys from table {}", tableName);
		Set<UUID> result = dbi.withHandle(new HandleCallback<Set<UUID>>() {
			@Override
			public Set<UUID> withHandle(Handle h) throws Exception {
				List<String> strResult = h.createQuery(String.format("select id from %s", tableName))
										  .map(new StringMapper()).list();
				Set<UUID> uResult = new HashSet<>();
				for (String uuid : strResult){
					uResult.add(UUID.fromString(uuid));
				}
				return uResult;
			}
		});
		log.debug("{} keys within table {} were loaded", result.size(), tableName);
		return result;	
	}
	
	@Override
	public String load(final UUID id) {
		String result = null;
		try {
			log.debug("will load {} from table {}", id.toString(), tableName);
			result = dbi.inTransaction(TransactionIsolationLevel.READ_COMMITTED,
					new TransactionCallback<String>() {
						@Override
						public String inTransaction(Handle h, TransactionStatus ts) throws Exception {
							String sql = String.format("select aggregate_root_data from %s where id = :id", tableName);
							return h.createQuery(sql).bind("id", id.toString()).map(ClobMapperToString.FIRST).first();
						}
					});
			// http://stackoverflow.com/questions/9779324/program-hangs-after-retrieving-100-rows-containg-clob
			if (result == null) {
				log.debug("found a null or zero length value for id {}", id.toString());
			} else {
				log.debug("loaded {} {} from table {}", id.toString(), result.replace("\n", "").substring(0, Math.min(10, result.length()-1)), tableName);
			}
		} catch (Exception e) {
			log.error("error when loading {} from table {}", id.toString(), tableName);
			e.printStackTrace();
		} finally {
		}
		return result;
	}

	@Override
	public Map<UUID, String> loadAll(Collection<UUID> ids) {
		log.debug("loading {} keys  within table {}", ids.size(), tableName);
		Map<UUID, String> result = new HashMap<>();
		for (UUID id : ids){
			result.put(id, load(id));
		}
		log.debug("{} keys were loaded from table {}", result.size(), tableName);
		return result;
	}

	@Override
	public void delete(UUID id) {
		deleteAll(Arrays.asList(id));
	}

	@Override
	public void deleteAll(final Collection<UUID> ids) {
		log.debug("deleting {} rows within table {}", ids.size(), tableName);
		dbi.inTransaction(new TransactionCallback<Integer>() {
			@Override
			public Integer inTransaction(Handle h, TransactionStatus ts)
					throws Exception {
				PreparedBatch pb = h.prepareBatch(String.format("delete from %s where id = :id", tableName));
				for (UUID id: ids){
					 pb.add().bind("id", id.toString());
				}
				return pb.execute().length;
			}
		}) ;
	}

	@Override
	public void store(UUID id, String value) {
		Map<UUID, String> map = new HashMap<>();
		map.put(id, value);
		storeAll(map);
	}

	@Override
	public void storeAll(final Map<UUID, String> id_value_pairs) { 
		
		log.debug("storing {} rows within table {}", id_value_pairs.size(), tableName);		

		dbi.inTransaction(new TransactionCallback<Integer>() {
			@Override
			public Integer inTransaction(Handle h, TransactionStatus ts)
					throws Exception {
				
				int inserts = 0, updates = 0;
				boolean hasInsert = false, hasUpdate = false;
				PreparedBatch pbInsert = h.prepareBatch(String.format("insert into %s (id, aggregate_root_data) values (:id, :aggregate_root_data)", tableName));
				PreparedBatch pbUpdate = h.prepareBatch(String.format("update %s set aggregate_root_data = :aggregate_root_data where id = :id", tableName));
				
				for (Entry<UUID, String> entry : id_value_pairs.entrySet()){
				
					final String id = entry.getKey().toString();
					final String value = entry.getValue();
					
					String idOnTable = h.createQuery(String.format("select id from %s where id = :id", tableName)).bind("id", id.toString()).map(StringMapper.FIRST).first();
					
					if (id.toString().equals(idOnTable)) {
						hasUpdate = true;
						pbUpdate.add().bind("id", id.toString()).bind("aggregate_root_data", value);	
						log.debug(String.format("updating with id %s into table %s", id, tableName));
					} else {
						hasInsert = true;
						pbInsert.add().bind("id", id.toString()).bind("aggregate_root_data", value);		// value.getBytes... testando sem o 
						log.debug(String.format("inserting with id %s into table %s", id, tableName));	
					}
					
				}
				
				if (hasInsert) {
					inserts = pbInsert.execute().length;
				}
				if (hasUpdate) {
					updates = pbUpdate.execute().length;
				}
				
				return inserts + updates;
			}
		}) ;
		
	}

}
