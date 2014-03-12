
-- drop table if exists query_model_sync ;

create table  
	query_model_sync(aggregateRootName varchar(60) not null, 
                     last_seq_number number(38), 
					 constraint pk primary key (aggregateRootName)) ;
					 
insert into query_model_sync (aggregateRootName, last_seq_number) values ('inventory_item', 0) ;

-- drop table if exists inventory_item_ar ;

create table  
	inventory_item_ar(id varchar(36) not null, 
					  version number(38), 
				      last_update timestamp,
					  constraint aggregate_pk primary key (id)) ;

-- drop table if exists inventory_item_uow ;
					  
create table  
	inventory_item_uow (id varchar(36) not null, 
				        version number(38),  
						uow_data clob not null, 
				        seq_number number(38), 
				        constraint uow_pk primary key (id, version)) ;

-- drop sequence if exists seq_inventory_item_uow ;

create sequence seq_inventory_item_uow ;

-- drop trigger if exists inventory_item_trigger ;

create trigger inventory_item_trigger
  BEFORE INSERT 
  ON inventory_item_uow
  FOR EACH ROW
  -- Optionally restrict this trigger to fire only when really needed
  -- WHEN (new.qname_id is null)
  DECLARE
    v_last_version inventory_item_uow.version%TYPE;
   v_next_seq_no inventory_item_uow.seq_number%TYPE;
BEGIN

  BEGIN
    SELECT version
      INTO v_last_version
      FROM inventory_item_ar
     WHERE id = :NEW.ID;
  EXCEPTION
    WHEN NO_DATA_FOUND THEN
         v_last_version := 0;
  END;
  
  IF v_last_version = 0 AND :new.version <> 1 THEN
    raise_application_error(-20001, 'Versão deve ser =1');
  END IF;
  
  IF v_last_version = 0 AND :new.version = 1 THEN
     insert into inventory_item_ar (id, version, last_update) values (:new.id, :new.version, CURRENT_TIMESTAMP) ;
  END IF;
  
  IF v_last_version <> 0 AND :new.version <> v_last_version +1 THEN
    raise_application_error(-20001, 'Próxima versão deve ser igual a ' || (v_last_version+1));
  END IF;
  
  IF v_last_version <> 0 AND :new.version = v_last_version +1 THEN
    update inventory_item_ar set version = :new.version, last_update = CURRENT_TIMESTAMP where id = :new.id ;
  END IF;
  
  SELECT seq_inventory_item_uow.nextval INTO v_next_seq_no FROM DUAL;

  :new.seq_number := v_next_seq_no;
  
END inventory_item_trigger;

