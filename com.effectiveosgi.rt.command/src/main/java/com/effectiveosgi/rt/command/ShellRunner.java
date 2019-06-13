package com.effectiveosgi.rt.command;

import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.BundleContext;

final class ShellRunner implements Callable<Integer> {

	private final BundleContext context;
	private final boolean quiet;

	public ShellRunner(BundleContext context, boolean quiet) {
		this.context = context;
		this.quiet = quiet;
	}

	@Override
	public Integer call() throws Exception {
		System.setProperty("gosh.args", quiet ? "--quiet" : "");
		final org.apache.felix.gogo.shell.Activator activator = new org.apache.felix.gogo.shell.Activator();
		activator.start(context);
		final Field executorField = activator.getClass().getDeclaredField("executor");
		executorField.setAccessible(true);
		final ExecutorService executor = (ExecutorService) executorField.get(activator);
		executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		return 0;
	}

}