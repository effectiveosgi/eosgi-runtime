package com.effectiveosgi.rt.web.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

@Component(
	name = "com.effectiveosgi.rt.webresource",
	property = {
			HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN + "=/" + WebResourceConstants.WEBRESOURCE_NAMESPACE + "/*",
			HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME + "=/WebResources"
	})
public class WebResourceServlet extends HttpServlet implements Servlet {

	private static final long serialVersionUID = 1L;
	
	private BundleContext context;

	@Activate 
	void activate(BundleContext context) {
		this.context = context;
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String pathInfo = req.getPathInfo();
		if (pathInfo.startsWith("/"))
			pathInfo = pathInfo.substring(1);
		String[] splitPathInfo = pathInfo.split("/", 3);
		if (splitPathInfo.length < 3) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Not enough path segments");
			return;
		}
		
		String bsn = splitPathInfo[0];
		String versionStr = splitPathInfo[1];
		String resourcePath = splitPathInfo[2];
		
		Bundle bundle = findBundle(bsn, versionStr);
		if (bundle == null) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Bundle not found: " + bsn + ":" + versionStr);
			return;
		}

		List<BundleWire> candidates = new LinkedList<>();
		getWires(bundle).forEach(candidates::add);

		Set<Bundle> visited = new HashSet<>();
		while (!candidates.isEmpty()) {
			BundleWire candidate = candidates.remove(0);
			Bundle candidateBundle = candidate.getProvider().getBundle();
			visited.add(candidateBundle);
			Object rootPathObj = candidate.getCapability().getAttributes().get("root");
			String rootPath = (rootPathObj instanceof String) ? (String) rootPathObj : "";
			if (!rootPath.isEmpty() && !rootPath.endsWith("/"))
				rootPath += "/";
			String bundlePath = rootPath + resourcePath;
			URL resource = candidateBundle.getEntry(bundlePath);
			if (resource != null) {
				serve(resource, resp);
				return;
			}
			getWires(candidateBundle)
				// avoid looping infinitely in case cycles exist
				.filter(w -> !visited.contains(w.getProvider().getBundle()))
				.forEach(candidates::add);
		}
		
		String visitedStr = visited.stream()
				.map(b -> String.format("(%d) %s:%s", b.getBundleId(), b.getSymbolicName(), b.getVersion()))
				.collect(Collectors.joining(", "));
		resp.sendError(HttpServletResponse.SC_NOT_FOUND, String.format("Web resource path not found: '%s', visited bundles: %s", resourcePath, visitedStr));
	}
	
	private Stream<BundleWire> getWires(Bundle bundle) {
		List<BundleWire> wires = bundle.adapt(BundleWiring.class).getRequiredWires(WebResourceConstants.WEBRESOURCE_NAMESPACE);
		return wires != null ? wires.stream() : Stream.empty();
	}

	private void serve(URL resource, HttpServletResponse resp) throws IOException {
		try (
				InputStream in = resource.openStream(); 
				OutputStream out = resp.getOutputStream()
		) {
			byte[] tmp = new byte[1024];
			int bytesRead = in.read(tmp, 0, 1024);
			while (bytesRead >= 0) {
				out.write(tmp, 0, bytesRead);
				bytesRead = in.read(tmp, 0, 1024);
			}
			out.flush();
		}
	}

	private Bundle findBundle(String bsn, String versionStr) {
		return Arrays.stream(context.getBundles())
			.filter(b -> bsn.equals(b.getSymbolicName()) && versionStr.equals(b.getVersion().toString()))
			.findAny()
			.orElse(null);
	}

}
