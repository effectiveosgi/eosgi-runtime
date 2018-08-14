package com.effectiveosgi.rt.inspect.web.standalone;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.effectiveosgi.rt.inspect.web.impl.ResourcesNanoServlet;
import com.effectiveosgi.rt.inspect.web.impl.BundleInspectorNanoServlet;
import com.effectiveosgi.rt.inspect.web.impl.SCRInspectorNanoServlet;
import com.effectiveosgi.rt.nanoweb.NanoServlet;
import com.effectiveosgi.rt.nanoweb.impl.NanoWebWhiteboardComponent;

public class InspectWebStandaloneActivator implements BundleActivator {

	private ResourcesNanoServlet appServlet;
	private ServiceRegistration<NanoServlet> appServletReg;

	private BundleInspectorNanoServlet bundleInspectorServlet;
	private ServiceRegistration<NanoServlet> bundleInspectorServletReg;

	private ServiceRegistration<NanoServlet> scrInspectorServletReg;

	private NanoWebWhiteboardComponent nanoweb;
	private SCRInspectorNanoServlet scrInspectorServlet;

	@Override
	public void start(BundleContext context) throws Exception {
		final NanoWebWhiteboardComponent nanoWeb = new NanoWebWhiteboardComponent();

		appServlet = new ResourcesNanoServlet();
		appServlet.activate(context);
		final Dictionary<String, Object> appServletProps = new Hashtable<>();
		appServletProps.put(NanoServlet.PROP_PATTERN, ResourcesNanoServlet.URI_PATTERN);
		appServletReg = context.registerService(NanoServlet.class, appServlet, appServletProps);
		nanoWeb.addService(appServlet, appServletReg.getReference());

		bundleInspectorServlet = new BundleInspectorNanoServlet();
		bundleInspectorServlet.activate(context);
		final Dictionary<String, Object> bundleInspectorServletProps = new Hashtable<>();
		bundleInspectorServletProps.put(NanoServlet.PROP_PATTERN, BundleInspectorNanoServlet.URI_PREFIX);
		bundleInspectorServletReg = context.registerService(NanoServlet.class, bundleInspectorServlet, bundleInspectorServletProps);
		nanoWeb.addService(bundleInspectorServlet, bundleInspectorServletReg.getReference());

		// The SCR inspector will fail to load if the relevant package imports
		// are not bound, e.g. because there is no SCR.
		try {
			scrInspectorServlet = new SCRInspectorNanoServlet();
			scrInspectorServlet.activate(context);
			final Dictionary<String, Object> scrInspectorServletProps = new Hashtable<>();
			scrInspectorServletProps.put(NanoServlet.PROP_PATTERN, SCRInspectorNanoServlet.URI_PREFIX);
			scrInspectorServletReg = context.registerService(NanoServlet.class, scrInspectorServlet, scrInspectorServletProps);
			nanoWeb.addService(scrInspectorServlet, scrInspectorServletReg.getReference());
		} catch (Exception e) {
			e.printStackTrace();
		}

		nanoWeb.activate(Collections.emptyMap());
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		nanoweb.deactivate();
		safeUnregister(scrInspectorServletReg);
		safeUnregister(bundleInspectorServletReg);
		safeUnregister(appServletReg);
	}

	private static final void safeUnregister(ServiceRegistration<?> reg) {
		if (reg != null) {
			try {
				reg.unregister();
			} catch (Exception e) {
				// swallow
			}
		}
	}
}
