package com.effectiveosgi.rt.inspect.web.standalone;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Pattern;

import com.effectiveosgi.rt.inspect.web.impl.NanoServlet;
import com.effectiveosgi.rt.inspect.web.impl.NanoServletException;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class NanoServletServer extends NanoHTTPD {

	private final List<Entry<Pattern, NanoServlet>> registrations = new ArrayList<>();

	public NanoServletServer(InetSocketAddress address) {
		super(address.getHostString(), address.getPort());
	}

	public void addRegistration(Pattern pattern, NanoServlet servlet) {
		synchronized (registrations) {
			registrations.add(new AbstractMap.SimpleEntry<>(pattern, servlet));
		}
	}

	public void removeRegistration(Pattern pattern, NanoServlet servlet) {
		synchronized (registrations) {
			registrations.remove(new AbstractMap.SimpleEntry<>(pattern, servlet));
		}
	}

	@Override
	public Response serve(IHTTPSession session) {
		final String path = session.getUri();
		final Optional<NanoServlet> matched;
		synchronized (registrations) {
			matched = registrations.stream()
					.filter(e -> e.getKey().matcher(path).matches())
					.map(Entry::getValue)
					.findFirst();
		}
		final Map<String, String> responseHeaders = new HashMap<>();
		final Response response = matched.map(servlet -> {
			try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
				final String mimeType = servlet.doGet(path, new NanoServlet.Session() {
					@Override
					public OutputStream getOutputStream() throws IOException {
						return out;
					}

					@Override
					public void putHeader(String name, String value) {
						responseHeaders.put(name, value);
					}
				});
				final byte[] result = out.toByteArray();
				return newFixedLengthResponse(Status.OK, mimeType, new ByteArrayInputStream(result), result.length);
			} catch (NanoServletException e) {
				return newFixedLengthResponse(Status.lookup(e.getCode()), "text/plain", e.getMessage());
			} catch (Exception e) {
				return newFixedLengthResponse(Status.INTERNAL_ERROR, "text/plain", e.getMessage());
			}
		}).orElse(newFixedLengthResponse(Status.NOT_FOUND, "text/plain", "Not Found"));
		responseHeaders.forEach(response::addHeader);
		return response;
	}

}
