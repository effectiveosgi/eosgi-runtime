package com.effectiveosgi.rt.command;

import org.apache.felix.gogo.shell.BuiltinCommands;
import org.apache.felix.service.command.CommandProcessor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

class CommandProcessorTracker extends ServiceTracker<CommandProcessor, CommandArgsTracker> {
	

	private BuiltinCommands builtinCommands;

	CommandProcessorTracker(BundleContext context) {
		super(context, CommandProcessor.class, null);
	}

	@Override
	public CommandArgsTracker addingService(ServiceReference<CommandProcessor> reference) {
		final CommandProcessor processor = context.getService(reference);
		
		builtinCommands = new BuiltinCommands();
		builtinCommands.start(context, processor);
	
		CommandArgsTracker argsTracker = new CommandArgsTracker(context, processor);
		argsTracker.open();
		return argsTracker;
	}

	@Override
	public void removedService(ServiceReference<CommandProcessor> reference, CommandArgsTracker tracker) {
		tracker.close();
		builtinCommands.stop();
		context.ungetService(reference);
	}

}