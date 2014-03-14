
drop table if exists query_model_sync ;

create table  
	query_model_sync(aggregateRootName varchar(60) not null, 
                     last_seq_number number(38), 
					 constraint pk primary key (aggregateRootName)) ;
					 
insert into query_model_sync (aggregateRootName, last_seq_number) values ('inventory_item', 0) ;

drop table if exists inventory_item_ar ;

create table  
	inventory_item_ar(id varchar(36) not null, 
					  version number(38), 
				      last_update timestamp,
					  constraint aggregate_pk primary key (id)) ;

drop table if exists inventory_item_uow ;
					  
create table  
	inventory_item_uow (id varchar(36) not null, 
				        version number(38),  
						uow_data clob not null, 
				        seq_number number(38), 
				        constraint uow_pk primary key (id, version));

drop sequence if exists seq_inventory_item_uow ;

create sequence seq_inventory_item_uow ;

drop trigger if exists inventory_item_trigger ;

create trigger inventory_item_trigger before insert on inventory_item_uow for each row 
	call "org.myeslib.h2.InventoryItemOptimisticLockingTrigger" ;
	
	
	
	
	

				        