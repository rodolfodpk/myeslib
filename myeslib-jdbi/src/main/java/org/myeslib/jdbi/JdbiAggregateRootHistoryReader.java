package org.myeslib.jdbi;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.myeslib.core.data.AggregateRootHistory;
import org.myeslib.core.data.UnitOfWork;
import org.myeslib.util.jdbi.ArTablesMetadata;
import org.myeslib.util.jdbi.ClobMapperToString;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import com.google.common.base.Function;
import com.google.inject.Inject;

@Slf4j
public class JdbiAggregateRootHistoryReader implements AggregateRootHistoryReader<UUID>{

	@Inject
	public JdbiAggregateRootHistoryReader(ArTablesMetadata tables,
			DBI dbi, 
			Function<String, UnitOfWork> fromStringFunction) {
		
		this.tables = tables;
		this.dbi = dbi;
		this.fromStringFunction = fromStringFunction;
	}

	private final ArTablesMetadata tables;
	private final DBI dbi;
	private final Function<String, UnitOfWork> fromStringFunction;

	/*
	 * (non-Javadoc)
	 * @see org.myeslib.jdbi.AggregateRootHistoryReader#get(java.lang.Object)
	 */
	@Override
	public AggregateRootHistory get(final UUID id) {
	
		final AggregateRootHistory arh = new AggregateRootHistory();
		
		try {

			log.debug("will load {} from {}", id.toString(), tables.getAggregateRootTable());
			
			List<UowRecord> unitsOfWork = dbi.withHandle(new HandleCallback<List<UowRecord>>() {
				String sql = String.format("select id, version, uow_data from %s where id = :id order by version", tables.getUnitOfWorkTable());
				public List<UowRecord> withHandle(Handle h) {
				      return h.createQuery(sql)
				    		  .bind("id", id.toString())
				    		  .map(new UowRecordMapper()).list();
				    }
			 }
		    );

			if (unitsOfWork != null) {
				log.debug("found {} units of work for id {} on {}", unitsOfWork.size(), id.toString(), tables.getUnitOfWorkTable());
				for (UowRecord r : unitsOfWork){
					UnitOfWork uow = fromStringFunction.apply(r.uowData);
					log.debug(uow.toString());
					arh.add(uow);
				}
			} else {
				log.debug("found none unit of work for id {} on {}", id.toString(), tables.getUnitOfWorkTable());
			}

		} catch (Exception e) {
			log.error("error when loading {} from table {}", id.toString(), tables.getUnitOfWorkTable());
			e.printStackTrace();

		} finally {
		}
		
		return arh;
	}
	
	@AllArgsConstructor
	public static class UowRecord {
		String id;
		Integer version;
		String uowData;
	}
	
	public static class UowRecordMapper implements ResultSetMapper<UowRecord> {
		@Override
		public UowRecord map(int index, ResultSet r, StatementContext ctx)
				throws SQLException {
			String id = r.getString("id");
			Integer version = r.getBigDecimal("version").intValue();
			String uowData = new ClobMapperToString("uow_data").map(index, r, ctx); 
			return new UowRecord(id, version, uowData);
		}
	}

}