package com.effectiveosgi.rt.command;

import java.util.Hashtable;

import org.osgi.annotation.bundle.Header;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

@Header(name = Constants.BUNDLE_ACTIVATOR, value = "${@class}")
public class CommandsActivator implements BundleActivator {

	private ServiceTracker<?,?> tracker;
	private ServiceRegistration<Object> exampleCommandReg;

	@SuppressWarnings("serial")
	@Override
	public void start(BundleContext context) throws Exception {
		exampleCommandReg = context.registerService(Object.class, new Object() {
			@SuppressWarnings("unused") // called reflectively by Gogo
			public String hello(String name) {
				System.out.println("Generating message...");
				return "Hello " + name;
			}
		}, new Hashtable<String, Object>() {{
			put("osgi.command.scope", "example");
			put("osgi.command.function", "hello");
		}});

		tracker = new CommandProcessorTracker(context);
		tracker.open();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		exampleCommandReg.unregister();
		tracker.close();
	}

}
