package org.myeslib.util.jdbi;

import lombok.Getter;

@Getter
public class ArTablesMetadata {
	
	public ArTablesMetadata(String aggregateRootName) {
		this.aggregateRootName = aggregateRootName;
		this.aggregateRootTable = aggregateRootName.concat("_ar");
		this.unitOfWorkTable = aggregateRootName.concat("_uow");
	}
	
	private final String aggregateRootName;
	private final String aggregateRootTable;
	private final String unitOfWorkTable;

}
