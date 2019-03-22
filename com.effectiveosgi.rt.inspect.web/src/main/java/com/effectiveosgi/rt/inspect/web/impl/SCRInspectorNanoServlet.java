package com.effectiveosgi.rt.inspect.web.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.osgi.framework.BundleContext;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;

public class SCRInspectorNanoServlet implements NanoServlet {
	
	public static final String URI_PREFIX = "/api/scr";

	private static final Type SCR_RETURN_TYPE = new TypeToken<Map<String, Collection<Object>>>() {}.getType();

	private final Gson gson;
	private final ServiceComponentRuntime scr;
	
	public SCRInspectorNanoServlet(BundleContext context, ServiceComponentRuntime scr) {
		this.scr = scr;
		this.gson = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeAdapter(ServiceReferenceDTO.class, new ServiceReferenceDTOJsonSerializer(context))
			.create();
	}
	
	@Override
	public String doGet(String path, NanoServlet.Session session) throws NanoServletException, IOException {
		Map<String, Object> result = new HashMap<>();
		
		try {
			List<ComponentConfigurationDTO> configDtos = scr.getComponentDescriptionDTOs().stream()
					.flatMap(dto -> scr.getComponentConfigurationDTOs(dto).stream())
					.sorted((a, b) -> (int) (a.id - b.id))
					.collect(Collectors.toList());
			result.put("configured", configDtos);
			
			List<ComponentDescriptionDTO> unconfigured = scr.getComponentDescriptionDTOs().stream()
					.filter(dto -> scr.getComponentConfigurationDTOs(dto).isEmpty())
					.sorted((a,b) -> a.name.compareTo(b.name))
					.collect(Collectors.toList());
			result.put("unconfigured", unconfigured);
			
		} catch (NoClassDefFoundError e) {
			throw new NanoServletException(503, "Unavailable");
		}

		try (PrintWriter out = new PrintWriter(session.getOutputStream())) {
			JsonWriter jsonWriter = new JsonWriter(out);
			
			gson.toJson(result, SCR_RETURN_TYPE, jsonWriter);
			out.flush();
			return "application/json";
		}
	}
	
}
