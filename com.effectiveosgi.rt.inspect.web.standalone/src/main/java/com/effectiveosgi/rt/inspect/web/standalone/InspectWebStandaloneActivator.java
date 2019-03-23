package com.effectiveosgi.rt.inspect.web.standalone;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.stream.Stream;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.effectiveosgi.rt.inspect.web.impl.BundleInspectorNanoServlet;
import com.effectiveosgi.rt.inspect.web.impl.LogNanoServlet;
import com.effectiveosgi.rt.inspect.web.impl.ResourcesNanoServlet;
import com.effectiveosgi.rt.inspect.web.impl.ServiceInspectorNanoServlet;

public class InspectWebStandaloneActivator implements BundleActivator {

	private static final String DEFAULT_HOST = "0.0.0.0";
	private static final String SYS_PROP_PORT = "inspect.web.standalone.port";
	private static final String SYS_PROP_HOST = "inspect.web.standalone.host";
	private static final int DEFAULT_PORT = 8080;

	private NanoServletServer server;
	private ServiceTracker<?, ?> scrTracker;

	@Override
	public void start(BundleContext context) throws Exception {
		// Get server host and port from system props
		final int port = Optional.ofNullable(System.getProperty(SYS_PROP_PORT))
				.map(Integer::parseInt)
				.orElse(DEFAULT_PORT);
		final String host = Optional.ofNullable(System.getProperty(SYS_PROP_HOST))
				.orElse(DEFAULT_HOST);

		// Create the server
		final NanoServletServer server = new NanoServletServer(new InetSocketAddress(InetAddress.getByName(host), port));

		// Register app and bundle inspector servlets
		Stream.of(
				new ResourcesNanoServlet(context.getBundle()),
				new BundleInspectorNanoServlet(context),
				new ServiceInspectorNanoServlet(context),
				new LogNanoServlet(context))
			.forEach(server::addServlet);

		// Open a tracker to register the SCR servlet if SCR is present
		scrTracker = new SCRServiceTracker(context, server::addServlet, server::removeServlet);
		scrTracker.open();

		server.start();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		scrTracker.close();
		server.stop();
	}
}
