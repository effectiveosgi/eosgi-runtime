package com.effectiveosgi.rt.inspect.servlet.impl;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

import com.effectiveosgi.rt.nanoweb.NanoServlet;
import com.effectiveosgi.rt.nanoweb.NanoServletException;

@Component(
		property =  {
				HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN + "=/*",
				HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME + "=Inspector Application"
		})
public class InspectorAppServlet extends HttpServlet implements Servlet {

	private static final long serialVersionUID = 1L;
	
	@Reference(target = "(pattern=/inspector.*)")
	NanoServlet appServlet;
	
	@Reference(target = "(pattern=/api/bundles)")
	NanoServlet bundleApiServlet;
	
	@Reference(target = "(pattern=/api/scr)")
	NanoServlet scrApiServlet;
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String path = req.getPathInfo();
		final NanoServlet responder;
		if (path.startsWith("/inspector")) {
			responder = appServlet;
		} else if (path.startsWith("/api/bundles")) {
			responder = bundleApiServlet;
		} else if (path.startsWith("/api/scr")) {
			responder = scrApiServlet;
		} else {
			responder = null;
		}

		if (responder == null) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Not Found");
		} else {
			final NanoServlet.Session session = new NanoServlet.Session() {
				@Override
				public void putHeader(String name, String value) {
					resp.setHeader(name, value);
				}
				@Override
				public OutputStream getOutputStream() throws IOException {
					return resp.getOutputStream();
				}
			};
			try {
				responder.doGet(path, session);
			} catch (NanoServletException e) {
				resp.sendError(e.getCode(), e.getMessage());
			}
		}

	}
}
