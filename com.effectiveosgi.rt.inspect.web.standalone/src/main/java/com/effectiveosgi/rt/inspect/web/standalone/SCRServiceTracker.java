package com.effectiveosgi.rt.inspect.web.standalone;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.util.tracker.ServiceTracker;

import com.effectiveosgi.rt.inspect.web.impl.NanoServlet;
import com.effectiveosgi.rt.inspect.web.impl.SCRInspectorNanoServlet;

public class SCRServiceTracker extends ServiceTracker<ServiceComponentRuntime, NanoServlet> {
	
	// Don't use the class literal, to avoid NCDFE when the package import is unavailable
	private static final String SCR_FILTER = "(objectClass=org.osgi.service.component.runtime.ServiceComponentRuntime)";

	private final Consumer<NanoServlet> adding;
	private final Consumer<NanoServlet> removing;

	public SCRServiceTracker(BundleContext context, Consumer<NanoServlet> adding, Consumer<NanoServlet> removing) {
		super(context, buildFilter(), null);
		this.adding = adding;
		this.removing = removing;
	}

	private static Filter buildFilter() {
		try {
			return FrameworkUtil.createFilter(SCR_FILTER);
		} catch (InvalidSyntaxException e) {
			// Shouldn't happen
			throw new RuntimeException(e);
		}
	}

	@Override
	public NanoServlet addingService(ServiceReference<ServiceComponentRuntime> reference) {
		final ServiceComponentRuntime scr = context.getService(reference);
		SCRInspectorNanoServlet servlet = new SCRInspectorNanoServlet(context);
		servlet.setServiceComponentRuntime(scr);

		adding.accept(servlet);
		return servlet;
	}

	@Override
	public void removedService(ServiceReference<ServiceComponentRuntime> reference, NanoServlet servlet) {
		removing.accept(servlet);
		context.ungetService(reference);
	}
}
