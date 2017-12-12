package com.effectiveosgi.rt.inspect.web.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

@Component(
		property =  {
				HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN + "=/inspector/*",
				HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME + "=Inspector Application"
		})
public class AppServlet extends HttpServlet implements Servlet {

	private static final long serialVersionUID = 1L;
	private static final String RESOURCE_PATH_KEY = "%RESOURCE_PATH%";

	private Bundle bundle;
	
	@Activate
	void activate(BundleContext context) {
		this.bundle = context.getBundle();
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		final String resourcePath = "osgi.enroute.webresource/" + bundle.getSymbolicName() + "/" + bundle.getVersion().toString();
		try (
				BufferedReader reader = new BufferedReader(new InputStreamReader(bundle.getEntry("app.html").openStream()));
				PrintStream out = new PrintStream(resp.getOutputStream())
		) {
			String line;
			while ((line = reader.readLine()) != null) {
				String replaced = line.replace(RESOURCE_PATH_KEY, resourcePath);
				out.println(replaced);
			}
			out.flush();
		}
	}
}
