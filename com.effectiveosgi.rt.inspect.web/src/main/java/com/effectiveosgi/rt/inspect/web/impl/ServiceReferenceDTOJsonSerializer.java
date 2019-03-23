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

class ServiceReferenceDTOJsonSerializer implements JsonSerializer<ServiceReferenceDTO> {
	
	private final BundleContext context;

	ServiceReferenceDTOJsonSerializer(BundleContext context) {
		this.context = context;
	}

	@Override
	public JsonElement serialize(ServiceReferenceDTO dto, Type typeOfSrc, JsonSerializationContext jsonContext) {
		JsonObject obj = new JsonObject();

		// Add id
		obj.addProperty("id", dto.id);

		// Expand provider bundle info to include BSN and version
		addBundleInfo(dto.bundle, obj);

		return obj;
	}

	protected void addBundleInfo(long bundleId, JsonObject obj) {
		obj.addProperty("bundleId", bundleId);
		Bundle bundle = context.getBundle(bundleId);
		obj.addProperty("bundleSymbolicName", bundle != null ? bundle.getSymbolicName() : "<missing>");
		obj.addProperty("bundleVersion", bundle != null ? bundle.getVersion().toString() : Version.emptyVersion.toString());
	}
}