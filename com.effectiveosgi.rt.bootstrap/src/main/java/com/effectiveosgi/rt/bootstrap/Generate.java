package com.effectiveosgi.rt.bootstrap;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import picocli.CommandLine.Command;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.TypeConversionException;

@Command(
		name = "generate",
		description = "Generate a new project or module",
		mixinStandardHelpOptions = true)
public class Generate implements Callable<Integer> {

	private static final String PROP_ENROUTE_VERSION = "enroute.version";
	private static final String DEFAULT_ENROUTE_VERSION = "7.0.0";

	static enum ArchetypeId {
		Project("project"),
		Application("application"),
		Api("api"),
		BareProject("project-bare"),
		RestComponent("rest-component"),
		DSComponent("ds-component"),
		BundleTest("bundle-test");
		final String id;
		private ArchetypeId(String id) {
			this.id = id;
		}
		public String getId() {
			return id;
		}
	}

	static class ArchetypeIdConverter implements ITypeConverter<ArchetypeId> {
		@Override
		public ArchetypeId convert(String value) throws Exception {
			return Stream.of(ArchetypeId.values())
					.filter(v -> v.getId().equalsIgnoreCase(value))
					.findFirst()
					.orElseThrow(() -> new TypeConversionException("Unknown type: " + value));
		}
	}

	private final Config config;
	private final Cache cache;

	@ParentCommand
	private EnrouteCommand parent;

	public Generate() throws IOException {
		config = new Config();
		cache = new Cache(config);
	}

	@Option(names = {"-t", "--type"}, required = true, description = "Type of project to create", converter = ArchetypeIdConverter.class)
	private ArchetypeId type;

	@Option(names = {"--enrouteVersion"}, description = "Enroute Version", paramLabel = "version")
	private String enrouteVersion;

	@Parameters(index = "0")
	private String groupId;
	@Parameters(index = "1")
	private String artifactId;

	@Override
	public Integer call() throws Exception {
		final String enrouteVersion = Optional.ofNullable(this.enrouteVersion)
				.orElseGet(() -> config.getProperty(PROP_ENROUTE_VERSION)
						.orElse(DEFAULT_ENROUTE_VERSION));
		final String packageName = groupId.replace('-', '.');

		final Map<String, String> mavenProps = new LinkedHashMap<>();
		mavenProps.put("interactiveMode", Boolean.FALSE.toString());
		mavenProps.put("archetypeGroupId", "org.osgi.enroute.archetype");
		mavenProps.put("archetypeArtifactId", type.getId());
		mavenProps.put("archetypeVersion", enrouteVersion);
		mavenProps.put("groupId", groupId);
		mavenProps.put("artifactId", artifactId);
		mavenProps.put("package", packageName);

		final Maven maven = new Maven(config, cache, parent.isDebug());
		return maven.run(mavenProps, "org.apache.maven.plugins:maven-archetype-plugin:3.0.1:generate");
	}

}
