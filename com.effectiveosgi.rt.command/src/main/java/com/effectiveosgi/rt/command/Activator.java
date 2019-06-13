package com.effectiveosgi.rt.command;

import java.util.Optional;

import org.osgi.annotation.bundle.Capability;
import org.osgi.annotation.bundle.Header;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

@Header(name = Constants.BUNDLE_ACTIVATOR, value = "${@class}")
@Capability(
		namespace = "org.apache.felix.gogo",
		attribute = {
				"org.apache.felix.gogo=shell.implementation",
				"version:Version=1.0.0"
		}
)

public class Activator implements BundleActivator {

	private static final String PROP_DEBUG = "eosgi.rt.command.debug";

	private Optional<ServiceRegistration<?>> exampleCommandsReg = Optional.empty();
	private ServiceTracker<?,?> tracker;

	@Override
	public void start(BundleContext context) throws Exception {
		if (Boolean.getBoolean(PROP_DEBUG)) {
			exampleCommandsReg = Optional.of(new ExampleCommands().register(context));
		}
		tracker = new CommandProcessorTracker(context);
		tracker.open();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		exampleCommandsReg.ifPresent(ServiceRegistration::unregister);
		tracker.close();
	}

}
