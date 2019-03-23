package com.effectiveosgi.rt.inspect.web.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.FrameworkWiring;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;

public class BundleInspectorNanoServlet implements NanoServlet {
	
	private static final String PATH_PATTERN = "/api/bundles";
	private static final Type BUNDLES_RETURN_TYPE = new TypeToken<List<? extends Bundle>>() {}.getType();

	private final CollectionJsonSerializer<Bundle> bundlesSerializer = new CollectionJsonSerializer<>(Bundle.class);
	private final BundleContext context;
	private final Gson gson;
	
	public BundleInspectorNanoServlet(BundleContext context) {
		this.context = context;
		GsonBuilder gsonBuilder = new GsonBuilder()
				.setPrettyPrinting()
				.registerTypeAdapter(BundleJsonSerializer.TYPE, new BundleJsonSerializer())
				.registerTypeAdapter(BUNDLES_RETURN_TYPE, bundlesSerializer);
		BundleHeadersJsonSerializer.register(gsonBuilder);
		BundleWiringJsonSerializer.register(gsonBuilder);
		VersionJsonSerializer.register(gsonBuilder);
		this.gson = gsonBuilder.create();
	}

	@Override
	public boolean matchPath(String path) {
		return PATH_PATTERN.equalsIgnoreCase(path);
	}

	@Override
	public void doGet(String path, NanoServlet.Session session) throws NanoServletException, IOException {
		FrameworkWiring wiring = context.getBundle(0).adapt(FrameworkWiring.class);
		List<Bundle> bundles = Stream
			.concat(
				Arrays.stream(context.getBundles()), // The currently installed bundles
				wiring.getRemovalPendingBundles().stream()) // Add the uninstalled bundles
			.sorted()
			.distinct()
			.collect(Collectors.toList());
		
		session.putHeader("Content-Type", "application/json");
		try (PrintWriter out = new PrintWriter(session.getOutputStream())) {
			JsonWriter writer = new JsonWriter(out);
			gson.toJson(bundles, BUNDLES_RETURN_TYPE, writer);
			out.flush();
		}
	}
	
}
