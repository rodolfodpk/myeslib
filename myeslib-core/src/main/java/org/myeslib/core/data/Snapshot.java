package org.myeslib.core.data;

import java.io.Serializable;

import org.myeslib.core.AggregateRoot;

import lombok.Value;

@SuppressWarnings("serial")
@Value
public class Snapshot<A extends AggregateRoot> implements Serializable {
	
	private A aggregateInstance;
	private Long version;

}
