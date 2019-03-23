package com.effectiveosgi.rt.inspect.web.standalone;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import com.effectiveosgi.rt.inspect.web.impl.NanoServlet;
import com.effectiveosgi.rt.inspect.web.impl.NanoServletException;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class NanoServletServer extends NanoHTTPD {

	private final List<NanoServlet> servlets = new CopyOnWriteArrayList<>();

	public NanoServletServer(InetSocketAddress address) {
		super(address.getHostString(), address.getPort());
	}

	public void addServlet(NanoServlet servlet) {
		servlets.add(servlet);
	}

	public void removeServlet(NanoServlet servlet) {
		servlets.remove(servlet);
	}

	@Override
	public Response serve(IHTTPSession session) {
		final String path = session.getUri();
		final Optional<NanoServlet> matched;
		matched = servlets.stream()
			.filter(s -> s.matchPath(path))
			.findFirst();
		final Map<String, String> responseHeaders = new HashMap<>();
		if (!matched.isPresent()) {
			return newFixedLengthResponse(Status.NOT_FOUND, "text/plain", "Not Found");
		}
		NanoServlet servlet = matched.get();
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			servlet.doGet(path, new NanoServlet.Session() {
				@Override
				public OutputStream getOutputStream() throws IOException {
					return out;
				}
				@Override
				public void putHeader(String name, String value) {
					responseHeaders.put(name, value);
				}
			});
			responseHeaders.put("Access-Control-Allow-Origin", "*");
			String mimeType = responseHeaders.getOrDefault("Content-Type", "");
			final byte[] result = out.toByteArray();
			Response response = newFixedLengthResponse(Status.OK, mimeType, new ByteArrayInputStream(result), result.length);
			responseHeaders.forEach(response::addHeader);
			return response;
		} catch (NanoServletException e) {
			return newFixedLengthResponse(Status.lookup(e.getCode()), "text/plain", e.getMessage());
		} catch (Exception e) {
			return newFixedLengthResponse(Status.INTERNAL_ERROR, "text/plain", e.getMessage());
		}
	}

}
