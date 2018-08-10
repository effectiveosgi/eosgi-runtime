package com.effectiveosgi.rt.inspect.web.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;

@Component(
		property = {
				HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN + "=/api/*",
				HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME + "=Inspector Runtime Service"
		})
public class InspectorServlet extends HttpServlet implements Servlet {

	private static final long serialVersionUID = 1L;

	private static final Type SCR_RETURN_TYPE = new TypeToken<Map<String, Collection<Object>>>() {}.getType();
	private static final Type BUNDLES_RETURN_TYPE = new TypeToken<List<? extends Bundle>>() {}.getType();

	private final CollectionJsonSerializer<Bundle> bundlesSerializer = new CollectionJsonSerializer<>(Bundle.class);
	private Gson gson;
	
	@Reference
	ServiceComponentRuntime scr;

	private BundleContext context;
	
	@Activate
	void activate(BundleContext context) {
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
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String pathInfo = req.getPathInfo();
		if ("/bundles".equals(pathInfo))
			doGetBundles(req, resp);
		else if ("/scr".equals(pathInfo))
			doGetScr(req, resp);
		else {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, req.getRequestURI());
		}
	}

	private void doGetBundles(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		FrameworkWiring wiring = context.getBundle(0).adapt(FrameworkWiring.class);
		
		List<Bundle> bundles = Stream
			.concat(
				Arrays.stream(context.getBundles()), // The currently installed bundles
				wiring.getRemovalPendingBundles().stream()) // Add the uninstalled bundles
			.sorted()
			.distinct()
			.collect(Collectors.toList());
		
		try (PrintWriter out = new PrintWriter(resp.getOutputStream())) {
			JsonWriter writer = new JsonWriter(out);
			gson.toJson(bundles, BUNDLES_RETURN_TYPE, writer);
			out.flush();
		}
	}

	private void doGetScr(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		Map<String, Object> result = new HashMap<>();
		
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
		
		try (PrintWriter out = new PrintWriter(resp.getOutputStream())) {
			JsonWriter jsonWriter = new JsonWriter(out);
			
			gson.toJson(result, SCR_RETURN_TYPE, jsonWriter);
			out.flush();
		}
	}

}
