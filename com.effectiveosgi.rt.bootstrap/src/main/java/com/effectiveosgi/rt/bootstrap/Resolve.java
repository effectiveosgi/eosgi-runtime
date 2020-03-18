package com.effectiveosgi.rt.bootstrap;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "resolve", description = "Index and resolve an OSGi assembly module")
public class Resolve implements Callable<Integer> {

	@ParentCommand
	private EnrouteCommand parent;

	@Option(names = {"-f", "--file"}, description = "Project directory (default current)", defaultValue = ".")
	private String projectPath;

	private final Config config;
	private final Cache cache;

	public Resolve() throws IOException {
		config = new Config();
		cache = new Cache(config);
	}

	@Override
	public Integer call() throws Exception {
		final List<String> args = new LinkedList<>();
		args.add("--update-snapshots");
		args.add("--also-make");
		args.add("--projects");
		args.add(projectPath.toString());
		args.add("bnd-indexer:index");
		args.add("bnd-indexer:index@test-index");
		args.add("bnd-resolver:resolve");
		args.add("package");

		final Maven maven = new Maven(config, cache, parent.isDebug());
		return maven.run(Collections.emptyMap(), args);
	}

}
