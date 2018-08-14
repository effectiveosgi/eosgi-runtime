package com.effectiveosgi.rt.nanoweb.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.URL;
import java.util.Hashtable;
import java.util.stream.Collectors;

import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

import com.effectiveosgi.rt.nanoweb.NanoServlet;
import com.effectiveosgi.rt.nanoweb.NanoServletException;

@SuppressWarnings("serial")
public class NanoWebWhiteboardTest {
	
	BundleContext context = FrameworkUtil.getBundle(NanoWebWhiteboardTest.class).getBundleContext();

	@Test(expected = FileNotFoundException.class)
	public void testNothingRegistered() throws Exception {
		readResponse("");
	}
	
	@Test
	public void testRegisterNanoServlet() throws Exception {
		// Precondition: 404
		try {
			readResponse("");
			fail("Expected 404 error");
		} catch (FileNotFoundException e) {}

		ServiceRegistration<NanoServlet> reg = context.registerService(NanoServlet.class, new NanoServlet() {
			@Override
			public String doGet(String path, NanoServlet.Session session) throws NanoServletException, IOException {
				try (PrintStream print = new PrintStream(session.getOutputStream())) {
					print.println("Hello World!");
				}
				return "text/plain";
			}
		}, new Hashtable<String, String>() {{
			put("pattern", "/hello");
		}});

		try {
			assertEquals("Hello World!", readResponse("/hello"));
		} finally {
			reg.unregister();
		}

		// Postcondition: 404
		try {
			readResponse("");
			fail("Expected 404 error");
		} catch (FileNotFoundException e) {}
	}

	@Test
	public void testRegisterNanoServletWithSamePatternButHigherRanking() throws Exception {
		// Precondition: 404
		try {
			readResponse("");
			fail("Expected 404 error");
		} catch (FileNotFoundException e) {}

		// Register first servlet
		final ServiceRegistration<NanoServlet> reg1 = context.registerService(NanoServlet.class, new NanoServlet() {
			@Override
			public String doGet(String path, NanoServlet.Session session) throws NanoServletException, IOException {
				try (PrintStream print = new PrintStream(session.getOutputStream())) {
					print.println("Hello World!");
				}
				return "text/plain";
			}
		}, new Hashtable<String, Object>() {{
			put("pattern", "/hello");
		}});

		try {
			// Check first servlet's message is returned
			assertEquals("Hello World!", readResponse("/hello"));

			// Register second servlet with same pattern and higher ranking
			final ServiceRegistration<NanoServlet> reg2 = context.registerService(NanoServlet.class, new NanoServlet() {
				@Override
				public String doGet(String path, NanoServlet.Session session) throws NanoServletException, IOException {
					try (PrintStream print = new PrintStream(session.getOutputStream())) {
						print.println("Goodbye World!");
					}
					return "text/plain";
				}
			}, new Hashtable<String, Object>() {{
				put("pattern", "/hello");
				put(Constants.SERVICE_RANKING, 1000);
			}});

			try {
				// Check second servlet's message is returned
				assertEquals("Goodbye World!", readResponse("/hello"));

				// Downgrade second servlet's ranking to zero. Now both servlets have equal ranking,
				// so we should choose the first servlet as it will have a lower service ID (because registered earlier).
				reg2.setProperties(new Hashtable<String, Object>() {{
					put("pattern", "/hello");
					put(Constants.SERVICE_RANKING, 0);
				}});

				// Check first servlet's message is returned
				assertEquals("Hello World!", readResponse("/hello"));
			} finally {
				reg2.unregister();
			}
		} finally {
			reg1.unregister();
		}

		// Postcondition: 404
		try {
			readResponse("");
			fail("Expected 404 error");
		} catch (FileNotFoundException e) {}
	}

	private String readResponse(String path) throws IOException {
		URL url = new URL("http", InetAddress.getLoopbackAddress().getHostName(), 8080, path);
		try (InputStream stream = url.openStream()) {
			return new BufferedReader(new InputStreamReader(stream)).lines().collect(Collectors.joining("\n"));
		}
	}

}
