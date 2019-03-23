package com.effectiveosgi.rt.inspect.web.impl;

import java.io.IOException;
import java.io.PrintWriter;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;

public class ConfigurationNanoServlet implements NanoServlet {

	private static final String PATH_MATCH = "/api/config";

	private final BundleContext context;
	private final Gson gson;

	public ConfigurationNanoServlet(BundleContext context) {
		this.context = context;
		gson = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeAdapter(Configuration.class, new ConfigurationTypeAdapter(context))
			.create();
	}
	
	@Override
	public boolean matchPath(String path) {
		return PATH_MATCH.equalsIgnoreCase(path);
	}

	@Override
	public void doGet(String path, Session session) throws NanoServletException, IOException {
		Configuration[] configs = null;
		final ServiceReference<ConfigurationAdmin> configAdminRef = context.getServiceReference(ConfigurationAdmin.class);
		if (configAdminRef != null) {
			final ConfigurationAdmin configAdmin = context.getService(configAdminRef);
			if (configAdmin != null) {
				try {
					configs = configAdmin.listConfigurations(null);
				} catch (InvalidSyntaxException e) {
					// Shouldn't happen
					throw new NanoServletException(500, "Internal error: " + e.getMessage());
				} finally {
					context.ungetService(configAdminRef);
				}
			}
		}
		if (configs == null) configs = new Configuration[0];

		session.putHeader("Content-Type", "application/json");
		try (PrintWriter out = new PrintWriter(session.getOutputStream())) {
			JsonWriter writer = new JsonWriter(out);
			gson.toJson(configs, Configuration[].class, writer);
			out.flush();
		}
	}

}
