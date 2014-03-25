package org.myeslib.core.data.test;

import java.util.UUID;

import lombok.Value;

import org.myeslib.core.Command;

@SuppressWarnings("serial")
@Value
public class CommandJustForTest implements Command{
	UUID id;
	Long version;
}
