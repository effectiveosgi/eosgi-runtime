package com.effectiveosgi.rt.inspect.web.scr.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.Collection;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;

@Component(
		property = {
				HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN + "=/api/scr",
				HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME + "=SCR Runtime Service",
				HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN + "=/example/scr/*",
				HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX + "=/web",
		})
public class ScrServlet extends HttpServlet implements Servlet {

	private static final long serialVersionUID = 1L;

	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private static final Type dtoCollectionType = new TypeToken<Collection<ComponentDescriptionDTO>>() {}.getType();

	@Reference
	ServiceComponentRuntime scr;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Collection<ComponentDescriptionDTO> dtos = scr.getComponentDescriptionDTOs();

		try (PrintWriter out = new PrintWriter(resp.getOutputStream())) {
			JsonWriter jsonWriter = new JsonWriter(out);
			gson.toJson(dtos, dtoCollectionType, jsonWriter);
			out.flush();
		}
	}

}
