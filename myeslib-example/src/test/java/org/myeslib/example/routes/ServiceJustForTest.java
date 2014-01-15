package org.myeslib.example.routes;

import org.myeslib.example.SampleCoreDomain.ItemDescriptionGeneratorService;

public class ServiceJustForTest implements ItemDescriptionGeneratorService {
	@Override
	public String generate() {
		return "a really nice description for this item so it can sell better ";
	}

}
