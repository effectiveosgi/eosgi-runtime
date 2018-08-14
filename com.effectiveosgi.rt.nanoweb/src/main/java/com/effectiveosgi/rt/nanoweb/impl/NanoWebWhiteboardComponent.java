package com.effectiveosgi.rt.nanoweb.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.effectiveosgi.rt.nanoweb.NanoServlet;
import com.effectiveosgi.rt.nanoweb.NanoServletException;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;

@Component
public class NanoWebWhiteboardComponent {

	private static final String PROP_HOST = "host";

	private static final String PROP_PORT = "port";

	public static final int DEFAULT_PORT = 8080;

	private NanoHTTPD httpd;
	
	private static class Registration {
		final NanoServlet servlet;
		final ServiceReference<NanoServlet> ref;
		final Pattern pattern;
		Registration(NanoServlet servlet, ServiceReference<NanoServlet> ref) {
			this.servlet = servlet;
			this.ref = ref;

			long serviceId = (Long) ref.getProperty(Constants.SERVICE_ID);
			final Object patternObj = ref.getProperty(NanoServlet.PROP_PATTERN);
			if (patternObj == null || !(patternObj instanceof String)) {
				throw new IllegalArgumentException(String.format("%s service id %d from bundle %s:%s is missing mandatory property %s (or has incorrect type)",
						NanoServlet.class.getSimpleName(), serviceId, ref.getBundle().getSymbolicName(), ref.getBundle().getVersion(), NanoServlet.PROP_PATTERN));
			}
			final String patternStr = (String) patternObj;
			try {
				this.pattern = Pattern.compile(patternStr);
			} catch (PatternSyntaxException e) {
				throw new IllegalArgumentException(String.format("%s property on %s service id %d from bundle %s:%s has invalid syntax: %s",
						NanoServlet.PROP_PATTERN, NanoServlet.class.getSimpleName(), serviceId, ref.getBundle().getSymbolicName(), ref.getBundle().getVersion(), patternStr), e);
			}
		}
	}

	private final List<Registration> registrations = new ArrayList<>();

	@Activate
	public void activate(Map<String, Object> configProps) throws Exception {
		final int port;
		final Object portObj = configProps.get(PROP_PORT);
		if (portObj != null) {
			if (portObj instanceof Number) {
				port = ((Number) portObj).intValue();
			} else if (portObj instanceof String) {
				port = Integer.parseInt((String) portObj);
			} else {
				throw new IllegalArgumentException(String.format("Unexpected type for %s property: %s", PROP_PORT, portObj.getClass().getName()));
			}
		} else {
			port = DEFAULT_PORT;
		}

		final String hostName;
		final Object hostObj = configProps.get(PROP_HOST);
		if (hostObj != null) {
			if (hostObj instanceof String) {
				hostName = (String) hostObj;
			} else {
				throw new IllegalArgumentException(String.format("Unexpected type for %s property: %s", PROP_HOST, hostObj.getClass().getName()));
			}
		} else {
			hostName = null;
		}

		httpd = new NanoHTTPD(hostName, port) {
			@Override
			public Response serve(IHTTPSession session) {
				String path = session.getUri();
				Optional<Registration> matched;
				synchronized (registrations) {
					matched = registrations.stream()
						.filter(r -> r.pattern.matcher(path).matches())
						// Inverted sort to make higher ranked / lower service ID services come first
						.sorted((r1, r2) -> r2.ref.compareTo(r1.ref))
						.findFirst();
				}
				final Map<String, String> responseHeaders = new HashMap<>();
				final Response response = matched.map(r -> {
					try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
						final String mimeType = r.servlet.doGet(path, new NanoServlet.Session() {
							@Override
							public OutputStream getOutputStream() throws IOException {
								return out;
							}
							@Override
							public void putHeader(String name, String value) {
								responseHeaders.put(name, value);
							}
						});
						byte[] result = out.toByteArray();
						return newFixedLengthResponse(Status.OK, mimeType, new ByteArrayInputStream(result), result.length);
					} catch (NanoServletException e) {
						return newFixedLengthResponse(Status.lookup(e.getCode()), "text/plain", e.getMessage());
					} catch (Exception e) {
						return newFixedLengthResponse(Status.INTERNAL_ERROR, "text/plain", e.getMessage());
					}
				})
				.orElse(newFixedLengthResponse(Status.NOT_FOUND, "text/plain", "Not Found"));
				responseHeaders.forEach(response::addHeader);
				return response;
			}
		};
		httpd.start(NanoHTTPD.SOCKET_READ_TIMEOUT, true);
	}

	@Deactivate
	public void deactivate() {
		httpd.stop();
	}
	
	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
	public void addService(NanoServlet servlet, ServiceReference<NanoServlet> ref) {
		Registration registration = new Registration(servlet, ref);
		synchronized (registrations) {
			registrations.add(registration);
		}
	}
	public void removeService(NanoServlet servlet, ServiceReference<NanoServlet> ref) {
		synchronized (registrations) {
			registrations.removeIf(r -> r.ref == ref);
		}
	}
}
