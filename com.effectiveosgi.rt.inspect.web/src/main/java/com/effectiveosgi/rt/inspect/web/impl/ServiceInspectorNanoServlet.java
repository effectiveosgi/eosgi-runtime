package com.effectiveosgi.rt.inspect.web.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.osgi.framework.BundleContext;
import org.osgi.framework.dto.ServiceReferenceDTO;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;

public class ServiceInspectorNanoServlet implements NanoServlet {
	
	private static final String PATH_PATTERN = "/api/services";
	private static final Type SERVICES_RETURN_TYPE = new TypeToken<List<? extends ServiceReferenceDTO>>() {}.getType();
	
	private final BundleContext context;
	private final Gson gson;

	public ServiceInspectorNanoServlet(BundleContext context) {
		this.context = context;
		final GsonBuilder gsonBuilder = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeAdapter(ServiceReferenceDTO.class, new ServiceReferenceDTOExpandedJsonSerializer(context))
			;
		this.gson = gsonBuilder.create();
	}

	@Override
	public boolean matchPath(String path) {
		return PATH_PATTERN.equalsIgnoreCase(path);
	}

	@Override
	public void doGet(String path, Session session) throws NanoServletException, IOException {
		List<ServiceReferenceDTO> refs = Arrays.stream(context.getBundles())
			.map(b -> b.adapt(ServiceReferenceDTO[].class))
			.flatMap(Arrays::stream)
			.sorted(Comparator.comparingLong(dto -> dto.id))
			.collect(Collectors.toList());

		session.putHeader("Content-Type", "application/json");
		try (PrintWriter out = new PrintWriter(session.getOutputStream())) {
			JsonWriter writer = new JsonWriter(out);
			gson.toJson(refs, SERVICES_RETURN_TYPE, writer);
			out.flush();
		}
	}

}
