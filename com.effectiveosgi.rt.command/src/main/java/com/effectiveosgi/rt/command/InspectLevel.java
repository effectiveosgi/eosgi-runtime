package com.effectiveosgi.rt.command;

import java.util.Arrays;
import java.util.Optional;

import org.apache.felix.service.command.Converter;

enum InspectLevel {

	None(Integer.MIN_VALUE),
	Basic(Converter.LINE),
	Full(Converter.INSPECT);

	public static Optional<InspectLevel> fromStringIgnoreCase(String name) {
		return Arrays.stream(InspectLevel.values())
			.filter(l -> l.name().equalsIgnoreCase(name))
			.findFirst();
	}

	private int converterLevel;

	private InspectLevel(int converterLevel) {
		this.converterLevel = converterLevel;
	}
	
	public int getConverterLevel() {
		return converterLevel;
	}
}
