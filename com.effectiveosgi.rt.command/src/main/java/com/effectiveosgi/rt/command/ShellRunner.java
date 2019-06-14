package com.effectiveosgi.rt.command;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.osgi.framework.BundleContext;

final class ShellRunner implements Callable<Integer> {

	private final BundleContext context;
	private final boolean quiet;
	private final CommandProcessor processor;

	public ShellRunner(BundleContext context, CommandProcessor processor, boolean quiet) {
		this.context = context;
		this.processor = processor;
		this.quiet = quiet;
	}
	
	@Override
	public Integer call() throws Exception {
		final Callable<Integer> startShellJob = () -> {
			final CommandSession session = processor.createSession(new FileInputStream(FileDescriptor.in), new FileOutputStream(FileDescriptor.out), new FileOutputStream(FileDescriptor.err));
			try
			{
				// wait for gosh command to be registered
				for (int i = 0; (i < 100) && session.get("gogo:gosh") == null; ++i)
				{
					TimeUnit.MILLISECONDS.sleep(10);
				}
				
				String commandline = "gosh --login --noshutdown";
				if (quiet) commandline += " --quiet";
				session.execute(commandline);
				return 0;
			}
			catch (InterruptedException e)
			{
				// Ok, back off...
				Thread.currentThread().interrupt();
				return 1;
			}
			catch (Exception e)
			{
				Object loc = session.get(".location");
				if (null == loc || !loc.toString().contains(":"))
				{
					loc = "gogo";
				}
				
				System.err.println(loc + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
				e.printStackTrace();
				return 1;
			}
			finally
			{
				if (session != null) {
					session.close();
				}
				Thread.currentThread().interrupt();
			}
		};
		final ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "Effective OSGi Gogo Shell"));
		int result = executor.submit(startShellJob).get();
		executor.shutdownNow();
		try {
			if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
				System.err.println("Thread pool failed to terminate.");
			}
		} catch (InterruptedException e) {
			System.err.println("Interrupted waiting for thread pool to terminate");
		}
		return result;
	}

}