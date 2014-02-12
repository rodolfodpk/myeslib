package org.myeslib.util.h2;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.myeslib.util.jdbi.ArTablesMetadata;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.TransactionCallback;
import org.skife.jdbi.v2.TransactionStatus;

@AllArgsConstructor
@Slf4j
public class ArhCreateTablesHelper {

	private final ArTablesMetadata tables;
	private final DBI dbi;
	
	public void createTables() {
		
		log.warn(String.format("checking if table %s exists for map storage", tables));
		
		dbi.inTransaction(new TransactionCallback<Integer>() {
			@Override
			public Integer inTransaction(Handle h, TransactionStatus ts)
					throws Exception {

				String sql1 = String.format("create table if not exists %s(id varchar(36) not null, "
				                        + "version number(38), " 
									    + "constraint aggregate_pk primary key (id))", 
					    	    tables.getAggregateRootTable());
				log.info(sql1);
				h.execute(sql1);

				// TODO constraint id + version
				String sql2 = String.format("create table if not exists %s(id varchar(36) not null, "
				                        + "version number(38), " 
									    + "uow_data clob not null) ", 
							    tables.getUnitOfWorkTable());

				log.info(sql2);
				h.execute(sql2);
				
				String sql3 = String.format("create trigger before_update before insert on %s for each row "
										+ "	call \"org.myeslib.util.h2.BeforeInsertTrigger\" ", 
								tables.getUnitOfWorkTable());
				
				log.info(sql3);
				h.execute(sql3);
	
				return 0;
			}
		}) ;
	}

}
