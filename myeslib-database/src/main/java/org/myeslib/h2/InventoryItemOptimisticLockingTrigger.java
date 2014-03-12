package org.myeslib.h2;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

import lombok.extern.slf4j.Slf4j;

import org.h2.api.Trigger;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.tweak.ConnectionFactory;
import org.skife.jdbi.v2.util.BigDecimalMapper;

@Slf4j
public class InventoryItemOptimisticLockingTrigger implements Trigger {

	String targetTable;        // per convention this has the suffix _uow
	String aggregateRootTable; // and the companion aggregate root table has the suffix _ar

	@Override
	public void init(Connection conn, String schemaName, String triggerName,
			String tableName, boolean before, int type) throws SQLException {
		this.targetTable = tableName;
		this.aggregateRootTable = targetTable.toUpperCase().replace("_UOW", "_AR");
	}

	@Override
	public void fire(final Connection conn, final Object[] oldRow, final Object[] newRow)
			throws SQLException {
		
		final String id = (String) newRow[0];
		final BigDecimal newVersion = (BigDecimal) newRow[1]; 
		
		DBI dbi = new DBI(new ConnectionFactory() {
			@Override
			public Connection openConnection() throws SQLException {
				return conn;
			}
		});

		Handle h = dbi.open();

		String sql1 = String.format("select version from %s where id = :id", aggregateRootTable);
		
		log.debug(sql1);
		
		BigDecimal lastVersion = h.createQuery(sql1).bind("id", id).map(BigDecimalMapper.FIRST).first();

		log.debug("uowTable, arTable, id {}, old version {}, new version {}", targetTable, aggregateRootTable, id, lastVersion, newVersion);

		if (lastVersion==null){
			validateAndProceedOnAggregateRootTable(id, newVersion, h);
		} else {
			validateAndProceedOnUnitOfWorkTable(id, newVersion, h, lastVersion);
		}
		
		String sqlSequence = String.format("SELECT seq_%s.nextval from dual", targetTable);
		
		BigDecimal next = h.createQuery(sqlSequence).map(BigDecimalMapper.FIRST).first();
		
		newRow[3] = next; // seq_number assignment (does it really work this way ?)
	
		log.debug("next --> {}", next);

	}

	private void validateAndProceedOnUnitOfWorkTable(final String id, final BigDecimal newVersion, Handle h, BigDecimal lastVersion) throws SQLException {
		if (newVersion.intValue() != lastVersion.intValue()+1) {
			String msg = String.format("new version ( %s ) does not match the last version +1 ( %s )", newVersion.toString(), lastVersion.toString());
			log.error(msg);
			throw new SQLException(msg);
		}			
		int ok = h.createStatement(String.format("update %s set version = :version, last_update = CURRENT_TIMESTAMP where id = :id", aggregateRootTable))
				  .bind("id", id)
				  .bind("version", newVersion)
				  .execute();
		if (ok!=1){
			String msg = String.format("aggregateRoot row (%s) was not updated", id);
			log.error(msg);
			throw new SQLException(msg);
		}
	}

	private void validateAndProceedOnAggregateRootTable(final String id, final BigDecimal newVersion, Handle h) throws SQLException {
		if (newVersion.intValue()!=1){
			String msg = String.format("new version ( %s ) should be =1 on first unit of work for aggregate root (%s)", newVersion.toString(), id);
			log.error(msg);
			throw new SQLException(msg);
		}
		int ok = h.createStatement(String.format("insert into %s (id, version, last_update) values (:id, :version, CURRENT_TIMESTAMP)", aggregateRootTable))
				  .bind("id", id)
				  .bind("version", newVersion)
				  .execute();
		if (ok!=1){
			String msg = String.format("aggregateRoot row (%s) was not inserted", id);
			log.error(msg);
			throw new SQLException(msg);
		}
	}

	@Override
	public void close() throws SQLException {
	}

	@Override
	public void remove() throws SQLException {
	}

}
