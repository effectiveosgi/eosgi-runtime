package com.effectiveosgi.rt.inspect.web.impl;

import java.lang.reflect.Type;

import org.osgi.framework.wiring.BundleRequirement;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

class BundleRequirementJsonSerializer implements JsonSerializer<BundleRequirement> {
	
	static final Class<BundleRequirement> TYPE = BundleRequirement.class;
	
	public static void register(GsonBuilder gson) {
		gson.registerTypeAdapter(TYPE, new BundleRequirementJsonSerializer());
	}
	
	private BundleRequirementJsonSerializer() {}

	@Override
	public JsonElement serialize(BundleRequirement req, Type typeOfSrc, JsonSerializationContext context) {
		if (req == null) return JsonNull.INSTANCE;
		
		JsonObject root = new JsonObject();
		root.addProperty("ns", req.getNamespace());
		root.add("attribs", context.serialize(req.getAttributes()));
		root.add("directives", context.serialize(req.getDirectives()));
		
		return root;
	}

}
