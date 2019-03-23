package com.effectiveosgi.rt.inspect.web.impl;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogService;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class LogEntryTypeAdapter extends TypeAdapter<LogEntry> {

	@Override
	public void write(JsonWriter out, LogEntry entry) throws IOException {
		out.beginObject();

		out.name("level");
		out.beginObject();
		out.name("code");
		out.value(entry.getLevel());
		out.name("name");
		out.value(toLevelString(entry.getLevel()));
		out.endObject();

		out.name("message");
		out.value(entry.getMessage());

		out.name("time");
		out.value(Instant.ofEpochMilli(entry.getTime()).toString());

		final Bundle bundle = entry.getBundle();
		if (bundle != null) {
			out.name("bundle");
			out.beginObject();
			out.name("id");
			out.value(bundle.getBundleId());
			out.name("bsn");
			out.value(bundle.getSymbolicName());
			out.name("version");
			out.value(bundle.getVersion().toString());
			out.endObject();
		}

		final ServiceReference<?> ref = entry.getServiceReference();
		if (ref != null) {
			out.name("service");
			out.beginObject();
			out.name("id");
			out.value((Long) ref.getProperty(Constants.SERVICE_ID));
			
			out.name("objectClass");
			final String[] objectClass = (String[]) ref.getProperty(Constants.OBJECTCLASS);
			out.value(Arrays.stream(objectClass).collect(Collectors.joining(",")));
			out.endObject();
		}

		final Throwable exception = entry.getException();
		if (exception != null) {
			out.name("exception");
			out.beginObject();

			out.name("type");
			out.value(exception.getClass().getName());

			out.name("message");
			out.value(exception.getMessage());

			out.name("stackTrace");
			out.beginArray();
			final StackTraceElement[] trace = exception.getStackTrace();
			for (final StackTraceElement element : trace) {
				out.beginObject();

				out.name("class");
				out.value(element.getClassName());

				out.name("method");
				out.value(element.getMethodName());

				out.name("file");
				out.value(element.getFileName());

				out.name("lineNumber");
				out.value(element.getLineNumber());

				out.endObject();
			}
			out.endArray();

			out.endObject();
		}
		
		out.endObject();
	}

	@Override
	public LogEntry read(JsonReader in) throws IOException {
		return null;
	}

	private static String toLevelString(int level) {
		final String result;
		switch(level) {
		case LogService.LOG_DEBUG:
			result = "DEBUG";
			break;
		case LogService.LOG_INFO:
			result = "INFO";
			break;
		case LogService.LOG_WARNING:
			result = "WARNING";
			break;
		case LogService.LOG_ERROR:
			result = "ERROR";
			break;
		default:
			result = "UNKNOWN";
			break;
		}
		return result;
	}
	
}
