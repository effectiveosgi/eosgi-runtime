package com.effectiveosgi.rt.command;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.Callable;

import org.apache.felix.service.command.CommandProcessor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

public class CommandArgsTracker extends ServiceTracker<Object, ServiceRegistration<Callable<Integer>>> {

	private final CommandProcessor processor;

	public CommandArgsTracker(BundleContext context, CommandProcessor processor) {
		super(context, createFilter(), null);
		this.processor = processor;
	}

	private static Filter createFilter() {
		try {
			final String filterStr = String.format("(&(%s=%d)(launcher.ready=true))", Constants.SERVICE_BUNDLEID, Constants.SYSTEM_BUNDLE_ID);
			return FrameworkUtil.createFilter(filterStr);
		} catch (InvalidSyntaxException e) {
			// Shouldn't happen
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public ServiceRegistration<Callable<Integer>> addingService(ServiceReference<Object> reference) {
		final String[] args;
		final Object argsObj = reference.getProperty("launcher.arguments");
		if (argsObj == null) {
			args = new String[0];
		} else if (argsObj instanceof String[]) {
			args = (String[]) argsObj;
		} else if (argsObj instanceof String) {
			args = new String[] { (String) argsObj };
		} else {
			return null;
		}

		final ServiceRegistration<Callable<Integer>> registration;
		if (args != null && args.length > 0) {
			final Callable<Integer> command;
			command = new CommandRunner(processor, args);
			final Dictionary<String, Object> callableProps = new Hashtable<>();
			callableProps.put("main.thread", true);
			
			registration = (ServiceRegistration<Callable<Integer>>)
				context.registerService(Callable.class.getName(), command, callableProps);
		} else {
			registration = null;
		}
		return registration;
	}

	@Override
	public void removedService(ServiceReference<Object> reference, ServiceRegistration<Callable<Integer>> registration) {
		registration.unregister();
	}

}