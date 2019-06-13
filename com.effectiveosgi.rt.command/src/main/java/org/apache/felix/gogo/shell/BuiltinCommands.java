package org.apache.felix.gogo.shell;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.Converter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.effectiveosgi.rt.command.Motd;

public class BuiltinCommands {

	private final Set<ServiceRegistration<?>> regs = new HashSet<>();

	public void start(BundleContext context, CommandProcessor processor) {
		final Dictionary<String, Object> dict = new Hashtable<>();
		dict.put(CommandProcessor.COMMAND_SCOPE, "gogo");

		// register converters
		regs.add(context.registerService(Converter.class.getName(),
				new Converters(context.getBundle(0).getBundleContext()), null));

		// register commands

		dict.put(CommandProcessor.COMMAND_FUNCTION, Builtin.functions);
		regs.add(context.registerService(Builtin.class.getName(), new Builtin(), dict));

		dict.put(CommandProcessor.COMMAND_FUNCTION, Procedural.functions);
		regs.add(context.registerService(Procedural.class.getName(), new Procedural(), dict));

		dict.put(CommandProcessor.COMMAND_FUNCTION, Posix.functions);
		regs.add(context.registerService(Posix.class.getName(), new Posix(), dict));

		dict.put(CommandProcessor.COMMAND_FUNCTION, Telnet.functions);
		regs.add(context.registerService(Telnet.class.getName(), new Telnet(processor), dict));

		dict.put(CommandProcessor.COMMAND_FUNCTION, Motd.functions);
		regs.add(context.registerService(Motd.class.getName(), new Motd(context), dict));
	}

	public void stop() {
		regs.forEach(ServiceRegistration::unregister);
	}

}
