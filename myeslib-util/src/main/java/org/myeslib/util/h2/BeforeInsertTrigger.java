package org.myeslib.util.h2;

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
public class BeforeInsertTrigger implements Trigger {

	String tableName;

	@Override
	public void init(Connection conn, String schemaName, String triggerName,
			String tableName, boolean before, int type) throws SQLException {
		this.tableName = tableName;
	}

	@Override
	public void fire(final Connection conn, final Object[] oldRow, final Object[] newRow)
			throws SQLException {

		// TODO insert into parent table 
		
//		System.out.println("insert values");
//		for (Object o: newRow) {
//			System.out.println(o);
//		}
		
		final String id = (String) newRow[0];
		BigDecimal newVersion = (BigDecimal) newRow[1]; 
		
		DBI dbi = new DBI(new ConnectionFactory() {
			@Override
			public Connection openConnection() throws SQLException {
				return conn;
			}
		});

		Handle h = dbi.open();
		String sql = String.format("select max(version) from %s where id = :id", tableName);
		BigDecimal lastVersion = h.createQuery(sql).bind("id", id).map(BigDecimalMapper.FIRST).first();
		
		log.debug("id {}, old version {}, new version {}", id, lastVersion, newVersion);

		if (lastVersion!= null && newVersion.intValue() != lastVersion.intValue()+1) {
			String msg = String.format("new version ( %s ) does not match the last version +1 ( %s )", newVersion, lastVersion);
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
