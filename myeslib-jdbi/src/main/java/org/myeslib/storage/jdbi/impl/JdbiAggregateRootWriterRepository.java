package org.myeslib.storage.jdbi.impl;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.myeslib.data.UnitOfWork;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.TransactionCallback;
import org.skife.jdbi.v2.TransactionStatus;

import com.google.gson.Gson;

@AllArgsConstructor
@Slf4j
public class JdbiAggregateRootWriterRepository implements AggregateRootWriterRepository<UUID>{

	private final String tableName;
	private final Gson gson;
	
	/*
	 * (non-Javadoc)
	 * @see org.myeslib.database.AggregateRootWriterRepository#insert(java.lang.Object, org.myeslib.data.UnitOfWork, org.skife.jdbi.v2.Handle)
	 */
	@Override
	public void insert(final UUID id, final UnitOfWork uow, Handle handle) {
		
		final String sql = String.format("insert into %s (aggregate_root_id, uow_data, version, uow_timestamp) "
								  + " values (:aggregate_root_id, :uow_data, :version, CURRENT_TIMESTAMP) "
								  , tableName) ;
		
		 handle.createStatement(sql)
			.bind("aggregate_root_id", id.toString())
			.bind("uow_data", gson.toJson(uow))
			.bind("version", uow.getVersion())
			.execute();
		
	}
	
	public void createTables(Handle handle) {
		
		log.warn(String.format("checking if table for %s exists for AR storage", tableName));
		
		handle.inTransaction(new TransactionCallback<Integer>() {
			@Override
			public Integer inTransaction(Handle h, TransactionStatus ts)
					throws Exception {

				h.execute(String.format("create table if not exists %s("
										+ "aggregate_root_id varchar(36) not null, "
									    + "uow_data clob not null, "
									    + "version number(19,0) not null, "
									    + "uow_timestamp timestamp(9) not null) ", 
					    tableName));

				
				return 0;
			}
		}) ;
	}

}
