package com.effectiveosgi.rt.bootstrap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
		name = "cache",
		description = "Manage Enroute cache",
		mixinStandardHelpOptions = true)
public class Cache implements Callable<Integer> {

	static final String PROP_CACHE_PATH = "cache.dir";
	static final Supplier<String> DEFAULT_CACHE_PATH = () -> new StringBuilder()
			.append(System.getProperty("user.home"))
			.append(File.separatorChar)
			.append(".enroute")
			.append(File.separatorChar)
			.append("cache")
			.toString();

	private final Path cacheDir;

	@Option(names = {"--clean"}, description = "Clean caches")
	private boolean clean;

	public Cache() throws IOException {
		this(new Config());
	}

	public Cache(Config config) {
		this.cacheDir = Paths.get(config.getProperty(PROP_CACHE_PATH).orElseGet(DEFAULT_CACHE_PATH));
	}

	public Path resolve(String path) {
		return cacheDir.resolve(path);
	}

	@Override
	public Integer call() throws Exception {
		if (clean) {
			if (Files.exists(cacheDir)) {
				Files.walk(cacheDir)
					.map(Path::toFile)
					.sorted((o1, o2) -> -o1.compareTo(o2))
					.forEach(File::delete);
				System.out.println("Removed cache directory: " + cacheDir);
			}
		} else {
			System.out.println("Cache directory (--clean to delete):" + cacheDir);
		}
		return 0;
	}

}
