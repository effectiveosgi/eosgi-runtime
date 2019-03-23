package com.effectiveosgi.rt.inspect.web.impl;

import java.lang.reflect.Type;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.dto.ServiceReferenceDTO;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.reflect.TypeToken;

class ServiceReferenceDTOExpandedJsonSerializer extends ServiceReferenceDTOJsonSerializer {
	
	private static final Type PROPERTIES_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

	ServiceReferenceDTOExpandedJsonSerializer(BundleContext context) {
		super(context);
	}

	@Override
	public JsonElement serialize(ServiceReferenceDTO dto, Type typeOfSrc, JsonSerializationContext jsonContext) {
		JsonObject obj = (JsonObject) super.serialize(dto, typeOfSrc, jsonContext);

		// Expand using bundles to include BSN and version
		final JsonArray usingBundlesArray = new JsonArray();
		if (dto.usingBundles != null) {
			for (long usingBundleId : dto.usingBundles) {
				JsonObject usingBundleObj = new JsonObject();
				addBundleInfo(usingBundleId, usingBundleObj);
				usingBundlesArray.add(usingBundleObj);
			}
		}
		obj.add("usingBundles", usingBundlesArray);
		
		// Add properties map using default serialization of Map<String,Object>
		final JsonElement propertiesElem = dto.properties != null
				? jsonContext.serialize(dto.properties, PROPERTIES_TYPE)
				: new JsonObject();
		obj.add("properties", propertiesElem);

		return obj;
	}
}