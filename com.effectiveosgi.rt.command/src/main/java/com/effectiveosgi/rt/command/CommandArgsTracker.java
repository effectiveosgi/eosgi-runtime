package com.effectiveosgi.rt.command;

import java.io.IOException;
import java.net.URI;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Optional;
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

class CommandArgsTracker extends ServiceTracker<Object, ServiceRegistration<Callable<Integer>>> {

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

		try {
			final Callable<Integer> runner;
			final ControlOptions opts = ControlOptions.parseArgs(args);

			if (opts.isHelp()) {
				runner = new Callable<Integer>() {
					@Override
					public Integer call() throws Exception {
						opts.usage();
						return 1;
					}
				};
			} else {
				final InspectLevel inspectLevel = opts.getInspectLevel()
						.orElse(InspectLevel.Basic);
				final String[] commandArgs = opts.getRemainingArgs();
				final Optional<URI> optScript = opts.getScript();

				if (optScript.isPresent()) {
					runner = new CommandRunner(processor, inspectLevel, optScript.get());
				} else if (commandArgs.length > 0) {
					runner = new CommandRunner(processor, inspectLevel, commandArgs);
				} else if (opts.isNoShell()) {
					return null;
				} else {
					runner = new ShellRunner(context, processor, opts.isQuiet());
				}
			}

			return registerCommand(runner);
		} catch (IllegalArgumentException | IOException e) {
			return registerCommand(new Callable<Integer>() {
				@Override
				public Integer call() throws Exception {
					e.printStackTrace();
					return 1;
				}
			});
		}
	}

	private ServiceRegistration<Callable<Integer>> registerCommand(final Callable<Integer> command) {
		final Dictionary<String, Object> callableProps = new Hashtable<>();
		callableProps.put("main.thread", true);
		@SuppressWarnings("unchecked")
		final ServiceRegistration<Callable<Integer>> registration = (ServiceRegistration<Callable<Integer>>)
			context.registerService(Callable.class.getName(), command, callableProps);
		return registration;
	}

	@Override
	public void removedService(ServiceReference<Object> reference, ServiceRegistration<Callable<Integer>> registration) {
		registration.unregister();
	}

}