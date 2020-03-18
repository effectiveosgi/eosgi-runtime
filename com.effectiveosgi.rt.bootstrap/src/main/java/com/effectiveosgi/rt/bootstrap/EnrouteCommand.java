package com.effectiveosgi.rt.bootstrap;

import java.util.concurrent.Callable;

import org.osgi.annotation.bundle.Header;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Header(name = "Main-Class", value = "${@class}")
@Command(
		name = "enroute",
		mixinStandardHelpOptions = true,
		subcommands = {
			Generate.class,
			Resolve.class,
			Cache.class,
			Config.class
		},
		version = "Enroute Command Wrapper 0.0.1")
public class EnrouteCommand implements Callable<Integer> {

	@Option(names = {"-X", "--debug"}, description = "Enable debugging.")
	private boolean debug;

	public Integer call() throws Exception {
		throw new IllegalArgumentException("Command not specified");
	}

	public static void main(String[] args) {
		final EnrouteCommand cmd = new EnrouteCommand();
		final CommandLine cmdLine = new CommandLine(cmd);
		cmdLine.setCaseInsensitiveEnumValuesAllowed(true);
		cmdLine.setExecutionExceptionHandler((e, c, p) -> {
			System.err.printf("ERROR: %s%n", e.getMessage());
			c.usage(System.err);
			if (cmd.debug) {
				e.printStackTrace(System.err);
			}
			return 1;
		});
		final int status = cmdLine.execute(args);
		System.exit(status);
	}

	public boolean isDebug() {
		return debug;
	}

}
