package com.effectiveosgi.rt.inspect.web.impl;

import java.lang.reflect.Type;
import java.util.Collection;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

class CollectionJsonSerializer<T> implements JsonSerializer<Collection<? extends T>> {
	
	private final Class<? extends T> componentClass;

	CollectionJsonSerializer(Class<? extends T> componentClass) {
		this.componentClass = componentClass;
	}

	@Override
	public JsonElement serialize(Collection<? extends T> src, Type typeOfSrc, JsonSerializationContext context) {
		JsonArray array = new JsonArray();
		for (T item : src) {
			array.add(context.serialize(item, componentClass));
		}
		return array;
	}
	
}
