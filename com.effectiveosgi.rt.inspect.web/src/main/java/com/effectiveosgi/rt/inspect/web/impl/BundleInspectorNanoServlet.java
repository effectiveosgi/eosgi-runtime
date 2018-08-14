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
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import com.effectiveosgi.rt.nanoweb.NanoServlet;
import com.effectiveosgi.rt.nanoweb.NanoServletException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;

@Component(property = NanoServlet.PROP_PATTERN + "=" + BundleInspectorNanoServlet.URI_PREFIX)
public class BundleInspectorNanoServlet implements NanoServlet {
	
	public static final String URI_PREFIX = "/api/bundles";

	private static final Type BUNDLES_RETURN_TYPE = new TypeToken<List<? extends Bundle>>() {}.getType();

	private final CollectionJsonSerializer<Bundle> bundlesSerializer = new CollectionJsonSerializer<>(Bundle.class);
	private Gson gson;
	
	BundleContext context;

	@Activate
	public void activate(BundleContext context) {
		this.context = context;
		GsonBuilder gsonBuilder = new GsonBuilder()
				.setPrettyPrinting()
				.registerTypeAdapter(ServiceReferenceDTO.class, new ServiceReferenceDTOJsonSerializer(context))
				.registerTypeAdapter(BundleJsonSerializer.TYPE, new BundleJsonSerializer(context))
				.registerTypeAdapter(BUNDLES_RETURN_TYPE, bundlesSerializer);
			BundleHeadersJsonSerializer.register(gsonBuilder);
			BundleWiringJsonSerializer.register(gsonBuilder);
			VersionJsonSerializer.register(gsonBuilder);
			this.gson = gsonBuilder.create();
	}
	
	@Override
	public String doGet(String path, NanoServlet.Session session) throws NanoServletException, IOException {
		FrameworkWiring wiring = context.getBundle(0).adapt(FrameworkWiring.class);
		List<Bundle> bundles = Stream
			.concat(
				Arrays.stream(context.getBundles()), // The currently installed bundles
				wiring.getRemovalPendingBundles().stream()) // Add the uninstalled bundles
			.sorted()
			.distinct()
			.collect(Collectors.toList());
		
		try (PrintWriter out = new PrintWriter(session.getOutputStream())) {
			JsonWriter writer = new JsonWriter(out);
			gson.toJson(bundles, BUNDLES_RETURN_TYPE, writer);
			out.flush();
			return "application/json";
		}
	}
	
}
