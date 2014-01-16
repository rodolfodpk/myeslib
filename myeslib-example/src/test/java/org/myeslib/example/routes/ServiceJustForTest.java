package org.myeslib.example.routes;

import java.util.UUID;

import org.myeslib.example.SampleCoreDomain.ItemDescriptionGeneratorService;

public class ServiceJustForTest implements ItemDescriptionGeneratorService {
	@Override
	public String generate(UUID id) {
		return "a really nice description for this item ";
	}

}
