package com.effectiveosgi.rt.command;

import java.util.Optional;

import org.osgi.annotation.bundle.Header;
import org.osgi.annotation.bundle.Requirement;
import org.osgi.annotation.bundle.Requirement.Cardinality;
import org.osgi.annotation.bundle.Requirement.Resolution;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

@Header(name = Constants.BUNDLE_ACTIVATOR, value = "${@class}")
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
