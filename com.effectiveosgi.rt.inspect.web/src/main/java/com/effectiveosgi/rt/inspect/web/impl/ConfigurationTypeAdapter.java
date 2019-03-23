package com.effectiveosgi.rt.inspect.web.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class ConfigurationTypeAdapter extends TypeAdapter<Configuration> {
	
	private final BundleContext context;

	public ConfigurationTypeAdapter(BundleContext context) {
		this.context = context;
	}

	@Override
	public void write(JsonWriter out, Configuration config) throws IOException {
		out.beginObject();

		out.name("pid");
		out.value(config.getPid());

		out.name("factoryPid");
		out.value(config.getFactoryPid());

		out.name("changeCount");
		out.value(config.getChangeCount());

		writeBoundBundle(out, config);

		final Dictionary<String, Object> props = config.getProperties();
		out.name("current"); out.value(props != null);
		if (props != null) {
			out.name("properties");
			out.beginObject();
			for (final Enumeration<String> keys = props.keys(); keys.hasMoreElements(); ) {
				final String key = keys.nextElement();
				out.name(key);
				final Object value = props.get(key);
				if (value == null)
					out.nullValue();
				else if (value instanceof Number)
					out.value((Number) value);
				else
					out.value(value.toString());
			}
			out.endObject();
		}
		out.endObject();
	}

	private void writeBoundBundle(JsonWriter out, Configuration config) throws IOException {
		out.name("binding");

		final String location = config.getBundleLocation();
		if (location == null)
			out.value("unbound");
		else if (location.equals("?"))
			out.value("any");
		else {
			out.value(location);
			final Bundle bundle = Arrays.stream(context.getBundles())
				.filter(b -> location.equals(b.getLocation()))
				.findAny().orElse(null);
			if (bundle != null) {
				out.name("boundBundle");
				out.beginObject();

				out.name("id");
				out.value(bundle.getBundleId());

				out.name("bsn");
				out.value(bundle.getSymbolicName());

				out.name("version");
				out.value(bundle.getVersion().toString());

				out.endObject();
			}
		}
	}

	@Override
	public Configuration read(JsonReader in) throws IOException {
		return null;
	}

}
