package com.effectiveosgi.rt.inspect.web.impl;

import java.lang.reflect.Type;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.framework.dto.ServiceReferenceDTO;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

final class ServiceReferenceDTOJsonSerializer implements JsonSerializer<ServiceReferenceDTO> {

	private final BundleContext context;

	ServiceReferenceDTOJsonSerializer(BundleContext context) {
		this.context = context;
	}

	@Override
	public JsonElement serialize(ServiceReferenceDTO dto, Type typeOfSrc, JsonSerializationContext jsonContext) {
		JsonObject obj = new JsonObject();
		
		obj.addProperty("id", dto.id);
		obj.addProperty("bundle", dto.bundle);
		
		Bundle bundle = context.getBundle(dto.bundle);
		obj.addProperty("bundleSymbolicName", bundle != null ? bundle.getSymbolicName() : "<missing>");
		obj.addProperty("bundleVersion", bundle != null ? bundle.getVersion().toString() : Version.emptyVersion.toString());

		return obj;
	}
}