package com.effectiveosgi.rt.inspect.web.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
	
	private static final String PATH_PATTERN = "/api/scr";
	private static final Type SCR_RETURN_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

	private final Gson gson;
	private volatile Optional<ServiceComponentRuntime> scr = Optional.empty();
	
	public SCRInspectorNanoServlet(BundleContext context) {
		this.gson = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeAdapter(ServiceReferenceDTO.class, new ServiceReferenceDTOExpandedJsonSerializer(context))
			.create();
	}

	@Override
	public boolean matchPath(String path) {
		return PATH_PATTERN.equalsIgnoreCase(path);
	}

	@Override
	public void doGet(String path, NanoServlet.Session session) throws NanoServletException, IOException {
		Map<String, Object> result = new HashMap<>();
		final SCRStatusDTO statusDto = new SCRStatusDTO();
		final List<ComponentConfigurationDTO> configured;
		final List<ComponentDescriptionDTO> unconfigured;
		if (scr.isPresent()) {
			statusDto.available = true;
			final Collection<ComponentDescriptionDTO> descriptionDTOs = scr.get().getComponentDescriptionDTOs();
			configured = descriptionDTOs.stream()
				.flatMap(dto -> scr.get().getComponentConfigurationDTOs(dto).stream())
				.sorted(Comparator.comparingLong(dto -> dto.id))
				.collect(Collectors.toList());
			unconfigured = descriptionDTOs.stream()
				.filter(dto -> scr.get().getComponentConfigurationDTOs(dto).isEmpty())
				.collect(Collectors.toList());
		} else {
			statusDto.available = false;
			configured = Collections.emptyList();
			unconfigured = Collections.emptyList();
		}
		result.put("status", statusDto);
		result.put("configured", configured);
		result.put("unconfigured", unconfigured);

		session.putHeader("Content-Type", "application/json");
		try (PrintWriter out = new PrintWriter(session.getOutputStream())) {
			JsonWriter jsonWriter = new JsonWriter(out);
			
			gson.toJson(result, SCR_RETURN_TYPE, jsonWriter);
			out.flush();
		}
	}

	public void setServiceComponentRuntime(ServiceComponentRuntime scr) {
		this.scr = Optional.ofNullable(scr);
	}

}
