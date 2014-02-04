package org.myeslib.storage.jdbi.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;

import org.myeslib.data.AggregateRootHistory;
import org.myeslib.data.UnitOfWork;
import org.skife.jdbi.v2.Handle;

import com.google.common.base.Function;

@AllArgsConstructor
public class JdbiAggregateRootReaderRepository implements AggregateRootReaderRepository<UUID>{

	private final String tableName;
	private final Function<Map<String, Object>, UnitOfWork> fromMap;
	
	/*
	 * (non-Javadoc)
	 * @see org.myeslib.database.AggregateRootReaderRepository#get(java.lang.Object, org.skife.jdbi.v2.Handle)
	 */
	@Override
	public AggregateRootHistory get(final UUID id, final Handle handle) {
		AggregateRootHistory a = new AggregateRootHistory();
		List<UnitOfWork> past = getPast(id, handle);
		for (UnitOfWork uow: past) {
			a.add(uow);
		}
		return a;
	}

	private List<UnitOfWork> getPast(final UUID id, final Handle handle) {
		List<UnitOfWork> result = new ArrayList<>();
		List<Map<String, Object>> rs = handle.select(String.format("select * from from %s where aggregate_root_id = '%s' order by uow_timestamp", tableName, id.toString()));
		for (Map<String, Object> row : rs){
			UnitOfWork uow = fromMap.apply(row);
			result.add(uow);
		}
		return result;
	}
 	
}