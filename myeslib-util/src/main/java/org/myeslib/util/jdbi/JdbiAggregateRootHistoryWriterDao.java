package org.myeslib.util.jdbi;

import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

import org.myeslib.core.data.UnitOfWork;
import org.skife.jdbi.v2.Handle;

import com.google.common.base.Function;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

@Slf4j
public class JdbiAggregateRootHistoryWriterDao implements AggregateRootHistoryWriterDao<UUID>{
	
	@Inject
	public JdbiAggregateRootHistoryWriterDao(@Assisted Handle handle,
			ArTablesMetadata tables,
			Function<UnitOfWork, String> toStringFunction) {
		
		this.handle = handle;
		this.tables = tables;
		this.toStringFunction = toStringFunction;
	}

	private final Handle handle;
	private final ArTablesMetadata tables;
	private final Function<UnitOfWork, String> toStringFunction;
	
	/*
	 * (non-Javadoc)
	 * @see org.myeslib.util.jdbi.AggregateRootHistoryWriterDao#insert(java.lang.Object, org.myeslib.core.data.UnitOfWork)
	 */
	@Override
	public void insert(final UUID id, final UnitOfWork uow) {

	String sql = String.format("insert into %s (id, uow_data, version) values (:id, :uow_data, :version)", tables.getUnitOfWorkTable());
		
		log.debug(sql);
		
		String asString = toStringFunction.apply(uow);
		
		handle.createStatement(sql)
			.bind("id", id.toString())
			.bind("uow_data", asString)
		    .bind("version", uow.getVersion())
		    .execute();
		
	}

}
