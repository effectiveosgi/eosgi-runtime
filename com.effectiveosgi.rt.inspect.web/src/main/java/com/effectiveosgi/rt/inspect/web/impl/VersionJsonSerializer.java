package com.effectiveosgi.rt.inspect.web.impl;

import java.lang.reflect.Type;

import org.osgi.framework.Version;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

class VersionJsonSerializer implements JsonSerializer<Version> {
	
	public static final Class<Version> TYPE = Version.class;

	public static void register(GsonBuilder builder) {
		builder.registerTypeAdapter(TYPE, new VersionJsonSerializer());
	}
	
	private VersionJsonSerializer() {}

	@Override
	public JsonElement serialize(Version version, Type typeOfSrc, JsonSerializationContext context) {
		if (version == null) return JsonNull.INSTANCE;
		return new JsonPrimitive(version.toString());
	}

}
