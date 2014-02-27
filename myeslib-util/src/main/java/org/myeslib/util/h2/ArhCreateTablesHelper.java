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
		
		log.warn(String.format("---- creating tables for event sourcing storage - %s", tables.getAggregateRootName()));
		
		dbi.inTransaction(new TransactionCallback<Integer>() {
			@Override
			public Integer inTransaction(Handle h, TransactionStatus ts)
					throws Exception {
				
				// this table has a row for each aggregate root type
				String createQueryModelSyncTable = "create table if not exists query_model_sync("
						+ "aggregateRootName varchar(60) not null, "
                        + "last_seq_number number(38), " 
					    + "constraint pk primary key (aggregateRootName))"; 
				log.info(createQueryModelSyncTable);
				h.execute(createQueryModelSyncTable);
				
				// insert aggregate
				String insertAggregateOnQueryModelSyncTable = String.format("insert into query_model_sync"
						+ "(aggregateRootName, last_seq_number) "
						+ "values ('%s', 0)", tables.getAggregateRootName());
				log.info(insertAggregateOnQueryModelSyncTable);
				h.execute(insertAggregateOnQueryModelSyncTable);

				// these are for aggregate root
				String createAggregateRootTable = String.format("create table if not exists %s("
										+ "id varchar(36) not null, "
				                        + "version number(38), " 
				                        + "last_update timestamp,"
									    + "constraint aggregate_pk primary key (id))", 
					    	    tables.getAggregateRootTable());
				log.info(createAggregateRootTable);
				h.execute(createAggregateRootTable);

				// TODO constraint id + version
				String createUnitOfWorkTable = String.format("create table if not exists %s("
										+ "id varchar(36) not null, "
				                        + "version number(38), " 
									    + "uow_data clob not null, "
				                        + "seq_number number(38), "
				                        + "constraint uow_pk primary key (id, version))", 
							    tables.getUnitOfWorkTable());

				log.info(createUnitOfWorkTable);
				h.execute(createUnitOfWorkTable);
				
				String createAggregateRootTrigger = String.format("create trigger before_update before insert on %s for each row "
										+ "	call \"org.myeslib.util.h2.ArhBeforeInsertTrigger\" ", 
								tables.getUnitOfWorkTable());
				
				log.info(createAggregateRootTrigger);
				h.execute(createAggregateRootTrigger);
	
				String createAggregateRootSequence = String.format("create sequence seq_%s",tables.getUnitOfWorkTable()) ;
				
				log.info(createAggregateRootSequence);
				h.execute(createAggregateRootSequence);
				
				return 0;
			}
		}) ;
	}

}
