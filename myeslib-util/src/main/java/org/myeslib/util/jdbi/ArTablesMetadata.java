package org.myeslib.util.jdbi;

import lombok.Getter;

import com.google.inject.Inject;
import com.google.inject.name.Named;

@Getter
public class ArTablesMetadata {
	
	@Inject
	public ArTablesMetadata(@Named("aggregateRootName") String aggregateRootName) {
		this.aggregateRootName = aggregateRootName;
		this.aggregateRootTable = aggregateRootName.concat("_ar");
		this.unitOfWorkTable = aggregateRootName.concat("_uow");
	}
	
	private final String aggregateRootName;
	private final String aggregateRootTable;
	private final String unitOfWorkTable;

}
