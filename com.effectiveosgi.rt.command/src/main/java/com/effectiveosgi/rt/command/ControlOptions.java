package com.effectiveosgi.rt.command;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.felix.gogo.options.Option;
import org.apache.felix.gogo.options.Options;

class ControlOptions {

	private static final String[] USAGE = {
		"Effective OSGi Command Bridge",
		"",
		"Usage: [OPTIONS] -- [COMMANDS]",
		"  -? --help             Show help",
		"  -d --detail=DETAIL    Output detail (none|basic|full)",
		"  -n --noshell          Don't start a shell when no command is passed",
		"  -s --script=FILE      Run the specified script file",
		"  -q --quiet            Don't display message-of-the-day"
	};
	private static final Set<String> SWITCHES_REQUIRING_PARAMS = new HashSet<>(Arrays.asList(
			"-d", "-s"
	));
	private static final Option OPTIONS = Options.compile(USAGE);

	private final Option options;
	private final List<String> remainingArgs;

	public static ControlOptions parseArgs(String... args) throws IllegalArgumentException {
		final List<String> controlArgs = new ArrayList<>(args.length);

		int argIndex;
		for (argIndex = 0; argIndex < args.length; argIndex++) {
			final String arg = args[argIndex];
			// If the marker "--" is seen, all subsequent arguments are not interpreted
			if ("--".equals(arg)) {
				argIndex++; // Skip over this arg
				break;
			}
			// If an arg does not start with "-", it is the beginning of the command
			if (!arg.startsWith("-")) {
				break;
			}

			controlArgs.add(arg);
			if (SWITCHES_REQUIRING_PARAMS.contains(arg)) {
				argIndex ++;
				if (argIndex >= args.length) {
					throw OPTIONS.usageError(String.format("option '%s' requires an argumentt", arg));
				}
				controlArgs.add(args[argIndex]);
			}
		}

		final Option opt = OPTIONS.parse(controlArgs.toArray(new String[controlArgs.size()]));

		final List<String> remainingArgs;
		if (argIndex < args.length) {
			remainingArgs = new ArrayList<>(args.length - argIndex);
			while (argIndex < args.length)
				remainingArgs.add(args[argIndex++]);
		} else {
			remainingArgs = Collections.emptyList();
		}
		
		return new ControlOptions(opt, remainingArgs);
	}

	public ControlOptions(Option options, List<String> remainingArgs) {
		this.options = options;
		this.remainingArgs = remainingArgs;
	}

	public String[] getRemainingArgs() {
		return remainingArgs.toArray(new String[remainingArgs.size()]);
	}

	public boolean isHelp() {
		return options.isSet("help");
	}

	public boolean isNoShell() {
		return options.isSet("noshell");
	}

	public boolean isQuiet() {
		return options.isSet("quiet");
	}

	public Optional<URI> getScript() {
		if (!options.isSet("script"))
			return Optional.empty();
		
		final URI scriptUri = new File(".").toURI().resolve(options.get("script"));
		return Optional.of(scriptUri);
	}

	public Optional<InspectLevel> getInspectLevel() throws IllegalArgumentException {
		if (!options.isSet("detail"))
			return Optional.empty();
		final InspectLevel level = InspectLevel.fromStringIgnoreCase(options.get("detail"))
			.orElseThrow(() -> options.usageError("Unexpected argument for 'detail' option"));
		return Optional.of(level);
	}

	public void usage() {
		OPTIONS.usage();
	}

	public IllegalArgumentException usageError(String error) {
		return OPTIONS.usageError(error);
	}

}
