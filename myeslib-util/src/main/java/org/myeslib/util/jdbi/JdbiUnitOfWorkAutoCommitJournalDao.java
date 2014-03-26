package org.myeslib.util.jdbi;

import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

import org.myeslib.core.data.UnitOfWork;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.TransactionCallback;
import org.skife.jdbi.v2.TransactionIsolationLevel;
import org.skife.jdbi.v2.TransactionStatus;

import com.google.common.base.Function;
import com.google.inject.Inject;

@Slf4j
public class JdbiUnitOfWorkAutoCommitJournalDao implements UnitOfWorkJournalDao<UUID> {
	
	@Inject
	public JdbiUnitOfWorkAutoCommitJournalDao(DBI dbi,
                                              ArTablesMetadata tables,
                                              Function<UnitOfWork, String> toStringFunction) {
		
		this.dbi = dbi;
		this.tables = tables;
		this.toStringFunction = toStringFunction;
	}

	private final DBI dbi;
	private final ArTablesMetadata tables;
	private final Function<UnitOfWork, String> toStringFunction;
	
	/*
	 * (non-Javadoc)
	 * @see org.myeslib.util.jdbi.UnitOfWorkJournalDao#append(java.lang.Object, org.myeslib.core.data.UnitOfWork)
	 */
	@Override
	public void append(final UUID id, final UnitOfWork uow) {

	    final String sql = String.format("insert into %s (id, uow_data, version) values (:id, :uow_data, :version)", tables.getUnitOfWorkTable());
		
		log.debug(sql);
		
		final String asString = toStringFunction.apply(uow);
		
		dbi.inTransaction(TransactionIsolationLevel.READ_COMMITTED, new TransactionCallback<Integer>() {
			@Override
			public Integer inTransaction(Handle conn, TransactionStatus status)
					throws Exception {
				conn.createStatement(sql)
				.bind("id", id.toString())
				.bind("uow_data", asString)
			    .bind("version", uow.getVersion())
			    .execute();
				return null;
			}
		}) ;
		
		log.debug("wrote uow");
	}

}
