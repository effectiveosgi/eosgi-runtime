package com.effectiveosgi.rt.inspect.web.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleContext;
import org.osgi.framework.dto.ServiceReferenceDTO;
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

	private static final Type returnType = new TypeToken<Map<String, Collection<Object>>>() {}.getType();

	private Gson gson;
	
	@Reference
	ServiceComponentRuntime scr;
	
	@Activate
	void activate(BundleContext context) {
		this.gson = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeAdapter(ServiceReferenceDTO.class, new ServiceReferenceDTOJsonSerializer(context))
			.create();
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String pathInfo = req.getPathInfo();
		if ("/scr".equals(pathInfo))
			doGetScr(req, resp);
		else {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, req.getRequestURI());
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
			
			gson.toJson(result, returnType, jsonWriter);
			out.flush();
		}
	}

}
