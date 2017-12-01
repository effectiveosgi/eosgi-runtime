package com.effectiveosgi.rt.inspect.web.impl;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Dictionary;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.wiring.dto.BundleRevisionDTO;
import org.osgi.framework.wiring.dto.BundleWiringDTO;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

class BundleJsonSerializer implements JsonSerializer<Bundle> {
	
	public static final Type TYPE = Bundle.class;

	private static final String PROP_LAST_MODIFIED_ISO = "lastModifiedISO";

	private static final Type REVISION_ARRAY_TYPE = new TypeToken<BundleRevisionDTO[]>() {}.getType();
	private static final Type WIRING_ARRAY_TYPE = new TypeToken<BundleWiringDTO[]>() {}.getType();

	private final BundleContext context;

	BundleJsonSerializer(BundleContext context) {
		this.context = context;
	}

	@Override
	public JsonElement serialize(Bundle bundle, Type typeOfSrc, JsonSerializationContext jsonContext) {

		BundleDTO bundleDto = bundle.adapt(BundleDTO.class);

		if (bundleDto == null) { // Uninstalled bundles return a null DTO but we would still like to know something about them!
			bundleDto = new BundleDTO();
	        bundleDto.id = bundle.getBundleId();
	        bundleDto.lastModified = bundle.getLastModified();
	        bundleDto.state = bundle.getState();
	        bundleDto.symbolicName = bundle.getSymbolicName();
	        bundleDto.version = "" + bundle.getVersion();
		}

		final JsonObject object = (JsonObject) jsonContext.serialize(bundleDto, BundleDTO.class);

		// Add the ISO-8601 formatted date/time
		String dateTime = DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(bundle.getLastModified()));
		object.addProperty(PROP_LAST_MODIFIED_ISO, dateTime);
		
		// Add the bundle headers
		Dictionary<String, String> headers = bundle.getHeaders();
		object.add("headers", jsonContext.serialize(headers, BundleHeadersJsonSerializer.TYPE));
		
		return object;
	}

}
