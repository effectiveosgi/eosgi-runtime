package com.effectiveosgi.rt.inspect.web.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import org.osgi.framework.Bundle;

public class ResourcesNanoServlet implements NanoServlet {
	
	static final String URI_PATH_PREFIX = "/inspector";
	public static final String URI_PATTERN = URI_PATH_PREFIX + ".*";
	static final String BUNDLE_PATH_PREFIX = "/static";
	private static final String REDIRECT = "bundles.html";

	private Bundle bundle;
	
	public ResourcesNanoServlet(Bundle bundle) {
		this.bundle = bundle;
	}
	
	@Override
	public void doGet(String path, NanoServlet.Session session) throws NanoServletException {
		String bundlePath = path.substring(URI_PATH_PREFIX.length());
		bundlePath = BUNDLE_PATH_PREFIX + (bundlePath.startsWith("/") ? "" : "/") + bundlePath;
		final URL entry = bundle.getEntry(bundlePath);
		if (entry == null)
			throw new NanoServletException(404, "Not Found: " + path);
		if (entry.getPath().endsWith("/")) {
			String redirect = path + (path.endsWith("/") ? "" : "/") + REDIRECT;
			session.putHeader("Location", redirect);
			throw new NanoServletException(307, String.format("Redirect to <a href='%s'>.", redirect));
		}

		session.putHeader("Content-Type", inferMimeType(bundlePath));
		try (
				InputStream in = entry.openStream();
		) {
			OutputStream out = session.getOutputStream();
			byte[] buffer = new byte[1024];
			int bytesRead = in.read(buffer, 0, 1024);
			while (bytesRead >= 0) {
				out.write(buffer, 0, bytesRead);
				bytesRead = in.read(buffer, 0, 1024);
			}
			out.flush();
		} catch (IOException e) {
			throw new NanoServletException(500, e.getMessage());
		}
	}

	private String inferMimeType(String bundlePath) {
		final String mimeType;
		if (bundlePath.endsWith(".html"))
			mimeType = "text/html";
		else if (bundlePath.endsWith(".js"))
			mimeType = "application/javascript";
		else
			mimeType = "text/plain";
		return mimeType;
	}
}
