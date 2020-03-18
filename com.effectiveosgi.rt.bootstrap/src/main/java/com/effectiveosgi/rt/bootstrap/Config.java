package com.effectiveosgi.rt.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Enumeration;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
		name = "config",
		description = "Manage Enroute configuration",
		mixinStandardHelpOptions = true)
public class Config implements Callable<Integer> {

	private static final Path CONFIG_PATH = Paths.get(System.getProperty("user.home"), ".enroute", "config.properties");

	@Option(names = {"-l", "--list"}, description = "List config properties")
	private boolean list;
	@Parameters(paramLabel = "properties", description = "One or more properties: name[=value]")
	private String[] properties;

	private final Properties config;

	public Config() throws IOException {
		config = loadConfig();
	}

	@Override
	public Integer call() throws Exception {
		if (list) {
			listConfigs();
		} else {
			if (properties == null || properties.length == 0) {
				throw new IllegalArgumentException("No properties specified");
			}
			boolean modified = false;
			for (String propertyStr : properties) {
				String[] propertyStrArr = propertyStr.split("=", 2);
				if (propertyStrArr.length == 1) {
					printProperty(propertyStrArr[0]);
				} else {
					setProperty(propertyStrArr[0], propertyStrArr[1]);
					modified = true;
				}
			}
			if (modified) {
				save();
			}
		}
		return 0;
	}

	public Optional<String> getProperty(String propertyName) {
		return Optional.ofNullable(config.getProperty(propertyName, null));
	}

	public void setProperty(String propertyName, String value) {
		config.put(propertyName, value);
	}

	private void printProperty(String propertyName) {
		Optional<String> prop = getProperty(propertyName);
		if (prop.isPresent()) {
			System.out.printf("%s = %s%n", propertyName, prop.get());
		} else {
			System.err.printf("undefined: %s%n", propertyName);
		}
	}

	private void listConfigs() throws IOException {
		@SuppressWarnings("unchecked")
		Enumeration<String> names = (Enumeration<String>) config.propertyNames();
		while (names.hasMoreElements()) {
			final String name = names.nextElement();
			System.out.printf("%s = %s%n", name, config.getProperty(name));
		}
	}

	private static Properties loadConfig() throws IOException {
		final Properties props = new Properties();
		if (Files.exists(CONFIG_PATH)) {
			try (InputStream in = Files.newInputStream(CONFIG_PATH, StandardOpenOption.CREATE)) {
				props.load(in);
			}
		}
		return props;
	}

	public void save() throws IOException {
		try (OutputStream out = Files.newOutputStream(CONFIG_PATH, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
			config.store(out, "Enroute Configuration");
		}
	}

}
