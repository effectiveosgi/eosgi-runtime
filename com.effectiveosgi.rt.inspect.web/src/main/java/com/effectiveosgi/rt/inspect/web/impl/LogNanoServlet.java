package com.effectiveosgi.rt.inspect.web.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogReaderService;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;

public class LogNanoServlet implements NanoServlet {

	private static final String PATH_MATCH = "/api/log";
	private static final Type LOG_ENTRY_COLLECTION_TYPE = new TypeToken<Collection<LogEntry>>() {}.getType();

	private final BundleContext context;

	private final Gson gson;

	public LogNanoServlet(BundleContext context) {
		this.context = context;
		gson = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeAdapter(LogEntry.class, new LogEntryTypeAdapter())
			.create();
	}
	
	@Override
	public boolean matchPath(String path) {
		return PATH_MATCH.equalsIgnoreCase(path);
	}

	@Override
	public void doGet(String path, Session session) throws NanoServletException, IOException {
		Collection<LogEntry> logEntries = Collections.emptyList();
		final ServiceReference<LogReaderService> logReaderSvcRef = context.getServiceReference(LogReaderService.class);
		if (logReaderSvcRef != null) {
			final LogReaderService logReaderSvc = context.getService(logReaderSvcRef);
			if (logReaderSvc != null) {
				try {
					@SuppressWarnings("unchecked")
					final Enumeration<LogEntry> logEnum = logReaderSvc.getLog();
					logEntries = Collections.list(logEnum);
				} finally {
					context.ungetService(logReaderSvcRef);
				}
			}
		}

		session.putHeader("Content-Type", "application/json");
		try (PrintWriter out = new PrintWriter(session.getOutputStream())) {
			JsonWriter writer = new JsonWriter(out);
			gson.toJson(logEntries, LOG_ENTRY_COLLECTION_TYPE, writer);
			out.flush();
		}
	}

}
