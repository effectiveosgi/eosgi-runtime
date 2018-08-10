package com.effectiveosgi.rt.inspect.web.impl;

import java.lang.reflect.Type;

import org.osgi.framework.wiring.BundleCapability;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

class BundleCapabilityJsonSerializer implements JsonSerializer<BundleCapability> {

	static final Class<BundleCapability> TYPE = BundleCapability.class;

	public static void register(GsonBuilder gson) {
		gson.registerTypeAdapter(TYPE, new BundleCapabilityJsonSerializer());
	}
	
	private BundleCapabilityJsonSerializer() {}

	@Override
	public JsonElement serialize(BundleCapability cap, Type typeOfSrc, JsonSerializationContext context) {
		if (cap == null) return JsonNull.INSTANCE;
		
		JsonObject root = new JsonObject();
		root.addProperty("ns", cap.getNamespace());
		root.add("attribs", context.serialize(cap.getAttributes()));
		root.add("directives", context.serialize(cap.getDirectives()));
		
		return root;
	}

}
