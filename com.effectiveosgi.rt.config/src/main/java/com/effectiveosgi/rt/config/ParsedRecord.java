package com.effectiveosgi.rt.config;

import java.util.Collections;
import java.util.Map;

public final class ParsedRecord {

	private final RecordIdentity id;
	private final Map<String, ? extends Object> properties;

	public static ParsedRecord singleton(String id, Map<String, ? extends Object> properties) {
		return new ParsedRecord(new RecordIdentity(id, null), properties);
	}

	public static ParsedRecord factory(String id, String factoryId, Map<String, ? extends Object> properties) {
		return new ParsedRecord(new RecordIdentity(id, factoryId), properties);
	}

	public ParsedRecord(RecordIdentity id, Map<String, ? extends Object> properties) {
		assert id != null : "id cannot be null";
		assert properties != null : "properties cannot be null";
		this.id = id;
		this.properties = properties;
	}

	public RecordIdentity getId() {
		return id;
	}

	public Map<String, ? extends Object> getProperties() {
		return Collections.unmodifiableMap(properties);
	}

}
