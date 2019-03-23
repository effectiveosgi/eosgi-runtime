package com.effectiveosgi.rt.inspect.web.impl;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

class BundleWiringJsonSerializer implements JsonSerializer<BundleWiring> {
	
	static final Class<BundleWiring> TYPE = BundleWiring.class;

	public static void register(GsonBuilder gson) {
		gson.registerTypeAdapter(TYPE, new BundleWiringJsonSerializer());
		BundleCapabilityJsonSerializer.register(gson);
		BundleRequirementJsonSerializer.register(gson);
	}
	
	private BundleWiringJsonSerializer() {}

	@Override
	public JsonElement serialize(BundleWiring wiring, Type typeOfSrc, JsonSerializationContext context) {
		if (wiring == null)
			return JsonNull.INSTANCE;
		JsonObject root = new JsonObject();
		
		// Map capabilities and requirements to actual wires, if they have them
		final Map<Capability, List<BundleWire>> capsToWires = new IdentityHashMap<>();
		wiring.getProvidedWires(null).stream().forEach(wire -> {
			capsToWires.compute(wire.getCapability(), (__, l) -> {
				if (l == null) l = new LinkedList<>();
				l.add(wire);
				return l;
			});
		});
		final Map<Requirement, BundleWire> reqsToWires = new IdentityHashMap<>();
		wiring.getRequiredWires(null).stream().forEach(wire -> {
			BundleWire existing = reqsToWires.put(wire.getRequirement(), wire);
			if (existing != null)
				System.err.printf("Requirement %s has multiple providers: %s and %s%n", wire.getRequirement(), wire.getProvider(), existing.getProvider());
		});
		
		// Iterate all the capabilities, including unused
		JsonArray providesArr = new JsonArray();
		root.add("provides", providesArr);
		wiring.getCapabilities(null).forEach(cap -> {
			JsonObject capObj = (JsonObject) context.serialize(cap, BundleCapabilityJsonSerializer.TYPE);
			providesArr.add(capObj);

			JsonArray consumersArr = new JsonArray();
			capObj.add("consumers", consumersArr);
			for (BundleWire consumerWire : capsToWires.getOrDefault(cap, Collections.emptyList())) {
				BundleRevision consumer = consumerWire.getRequirer();
				JsonObject consumerObj = new JsonObject();
				consumerObj.addProperty("id", consumer.getBundle().getBundleId());
				consumerObj.addProperty("bsn", consumer.getSymbolicName());
				consumerObj.addProperty("version", consumer.getVersion().toString());
				consumerObj.addProperty("revision", consumer.getBundle().adapt(BundleRevisions.class).getRevisions().indexOf(consumer));
				consumerObj.addProperty("location", consumer.getBundle().getLocation());
				consumersArr.add(consumerObj);
			}
		});
		
		// Iterate all the requirements, including the unsatisfied
		JsonArray requiresArr = new JsonArray();
		root.add("requires", requiresArr);
		wiring.getRequirements(null).forEach(req -> {
			JsonObject reqObj = (JsonObject) context.serialize(req, BundleRequirementJsonSerializer.TYPE);
			requiresArr.add(reqObj);

			BundleWire providerWire = reqsToWires.get(req);
			if (providerWire != null) {
				BundleRevision provider = providerWire.getProvider();
				JsonObject providerObj = new JsonObject();
				providerObj.addProperty("id", provider.getBundle().getBundleId());
				providerObj.addProperty("bsn", provider.getSymbolicName());
				providerObj.addProperty("version", provider.getVersion().toString());
				providerObj.addProperty("revision", provider.getBundle().adapt(BundleRevisions.class).getRevisions().indexOf(provider));
				providerObj.addProperty("location", provider.getBundle().getLocation());
				reqObj.add("provider", providerObj);
			}
		});
		
		return root;
	}
	
}
