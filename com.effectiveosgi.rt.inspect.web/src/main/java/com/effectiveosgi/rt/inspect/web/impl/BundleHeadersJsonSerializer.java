package com.effectiveosgi.rt.inspect.web.impl;

import java.lang.reflect.Type;
import java.util.Dictionary;
import java.util.Enumeration;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

class BundleHeadersJsonSerializer implements JsonSerializer<Dictionary<String,String>> {
	
	public static final Type TYPE = new TypeToken<Dictionary<String,String>>() {}.getType();
	
	public static void register(GsonBuilder gsonBuilder) {
		gsonBuilder.registerTypeAdapter(TYPE, new BundleHeadersJsonSerializer());
	}
	
	private BundleHeadersJsonSerializer() {}
	
	@Override
	public JsonElement serialize(Dictionary<String, String> headers, Type typeOfSrc, JsonSerializationContext context) {
		if (headers == null)
			return JsonNull.INSTANCE;

		JsonObject object = new JsonObject();
		for (Enumeration<String> keyEnum = headers.keys(); keyEnum.hasMoreElements(); ) {
			String key = keyEnum.nextElement();
			object.addProperty(key, headers.get(key));
		}
		
		return object;
	}

}
