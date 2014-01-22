package org.myeslib.hazelcast;

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

import org.myeslib.data.AggregateRootHistory;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.PreparedBatch;
import org.skife.jdbi.v2.TransactionCallback;
import org.skife.jdbi.v2.TransactionStatus;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.skife.jdbi.v2.util.ByteArrayMapper;
import org.skife.jdbi.v2.util.StringMapper;

import com.google.gson.Gson;
import com.hazelcast.core.MapStore;

@Slf4j
public class AggregateRootHistoryMapStore implements MapStore<UUID, AggregateRootHistory>{

	private final DBI dbi;
	private final String tableName;
	private final Gson gson;
	
	public AggregateRootHistoryMapStore(DataSource ds, String tableName, Gson gson){
		this.dbi = new DBI(ds);
		this.tableName = tableName;
		this.gson = gson;
	}
	
	public void createTableForQueue() {
		log.warn(String.format("checking if table %s exists for queue storage", tableName));
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
		log.info(String.format("loading all keys  within table %s", tableName));
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
		return result;	
	}
	
	@Override
	public AggregateRootHistory load(final UUID id) {
		AggregateRootHistory result = dbi.withHandle(new HandleCallback<AggregateRootHistory>() {
			@Override
			public AggregateRootHistory withHandle(Handle h) throws Exception {
				byte[] clob = dbi.withHandle(new HandleCallback<byte[]>() {
					@Override
					public byte[] withHandle(Handle h) throws Exception {
						return h.createQuery(String.format("select aggregate_root_data from %s where id = :id", tableName))
								.bind("id", id)
						 .map(ByteArrayMapper.FIRST).first();
					}
				});
				AggregateRootHistory _result = clob == null ? null : gson.fromJson(new String(clob), AggregateRootHistory.class);
				return _result; 
			}
		});
		return result;
	}

	@Override
	public Map<UUID, AggregateRootHistory> loadAll(Collection<UUID> ids) {
		Map<UUID, AggregateRootHistory> result = new HashMap<>();
		for (UUID id : ids){
			result.put(id, load(id));
		}
		return result;
	}

	@Override
	public void delete(UUID id) {
		deleteAll(Arrays.asList(id));
	}

	@Override
	public void deleteAll(final Collection<UUID> ids) {
		log.info(String.format("deleting all within table %s", tableName));
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
	public void store(UUID id, AggregateRootHistory value) {
		Map<UUID, AggregateRootHistory> map = new HashMap<>();
		map.put(id, value);
		storeAll(map);
	}

	@Override
	public void storeAll(final Map<UUID, AggregateRootHistory> id_value_pairs) {
		log.info(String.format("storing all within table %s", tableName));		
		
		final Set<UUID> currentKeysOnTable = loadAllKeys();
		
		dbi.inTransaction(new TransactionCallback<Integer>() {
			@Override
			public Integer inTransaction(Handle h, TransactionStatus ts)
					throws Exception {
				PreparedBatch pb = h.prepareBatch(String.format("insert into %s values (:id, :aggregate_root_data)", tableName));
				for (Entry<UUID, AggregateRootHistory> entry : id_value_pairs.entrySet()){
					// lets insert only the new ones
					if (!currentKeysOnTable.contains(entry.getKey())){
						final String id = entry.getKey().toString();
						final String asJson = gson.toJson(entry.getValue());
						pb.add().bind("id", id).bind("aggregate_root_data", asJson.getBytes());		
						log.debug(String.format("inserting with id %s into table %s", id, tableName));	
					}
				}
				return pb.execute().length;
			}
		}) ;
		
		dbi.inTransaction(new TransactionCallback<Integer>() {
			@Override
			public Integer inTransaction(Handle h, TransactionStatus ts)
					throws Exception {
				PreparedBatch pb = h.prepareBatch(String.format("update %s set aggregate_root_data = :aggregate_root_data where id = :id", tableName));
				for (Entry<UUID, AggregateRootHistory> entry : id_value_pairs.entrySet()){
					// lets update the existing
					if (currentKeysOnTable.contains(entry.getKey())){
						final String id = entry.getKey().toString();
						final String asJson = gson.toJson(entry.getValue());
						pb.add().bind("id", id).bind("aggregate_root_data", asJson.getBytes());	
						log.debug(String.format("updating with id %s into table %s", id, tableName));	
					}
				}
				return pb.execute().length;
			}
		}) ;
		
	}

}
