package com.effectiveosgi.rt.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import com.effectiveosgi.lib.OS;
import com.effectiveosgi.lib.OSType;

public class Maven {

	static final String PROP_MAVEN_EMBED = "maven.embedded";
	static final boolean DEFAULT_MAVEN_EMBED = false;

	static final String PROP_MAVEN_WRAPPER_DISABLED = "mvn.wrapper.disabled";
	static final boolean DEFAULT_MAVEN_WRAPPER_DISABLED = false;

	static final String PROP_MAVEN_VERSION = "maven.embedded.version";
	static final String DEFAULT_MAVEN_VERSION = "3.6.3";

	static final String PROP_MAVEN_MIRROR_URL = "maven.mirror.url";
	static final String DEFAULT_MAVEN_MIRROR_URL = "http://mirrorservice.org/sites/ftp.apache.org/maven/maven-3";

	private final Config config;
	private final Cache cacheDir;
	private final boolean debug;

	public Maven(Config config, Cache cacheDir, boolean debug) {
		this.config = config;
		this.cacheDir = cacheDir;
		this.debug = debug;
	}

	private String getMavenExePath() throws IOException {
		// Check if we should use the mvnw wrapper executable in the current dir
		boolean mvnwDisabled = config.getProperty(PROP_MAVEN_WRAPPER_DISABLED)
				.map(Boolean::valueOf)
				.orElse(DEFAULT_MAVEN_WRAPPER_DISABLED);
		if (!mvnwDisabled) {
			final String mvnwName = OS.CURRENT.getType() == OSType.Windows
					? "mvnw.bat"
							: "mvnw";
			Path mvnwPath = Paths.get(mvnwName).toAbsolutePath();
			if (Files.exists(mvnwPath)) {
				return mvnwPath.toString();
			}
		}

		// Check if we should download and use an embedded Maven
		boolean embed = config.getProperty(PROP_MAVEN_EMBED)
			.map(Boolean::valueOf)
			.orElse(DEFAULT_MAVEN_EMBED);
		if (embed) {
			Path mavenDir = downloadMaven();
			Path mvnExePath = mavenDir.resolve("bin/mvn");
			return mvnExePath.toString();
		}

		// Fall back to the mvn command from the PATH
		return "mvn";
	}

	private Path downloadMaven() throws IOException {
		// Infer Maven directory
		final String mavenVersion = config.getProperty(PROP_MAVEN_VERSION).orElse(DEFAULT_MAVEN_VERSION);
		final Path mavenDir = cacheDir.resolve(String.format("apache-maven-%s", mavenVersion));
		if (Files.isDirectory(mavenDir)) {
			return mavenDir;
		}

		// Get download URL
		final String mirrorUrl = config.getProperty(PROP_MAVEN_MIRROR_URL).orElse(DEFAULT_MAVEN_MIRROR_URL);
		final String downloadUrlStr = String.format("%s/%s/binaries/apache-maven-%s-bin.zip",
				mirrorUrl, mavenVersion, mavenVersion);
		final URL url = new URL(downloadUrlStr);

		// Download to temp file and expand
		final Path tempFile = Files.createTempFile("mvn-" + mavenVersion, ".zip");
		System.out.printf("Downloading %s%n", url);
		try (InputStream in = url.openStream()) {
			Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
			System.out.println("Extracting...");
			try (ZipFile zip = new ZipFile(tempFile.toFile())) {
				for (Enumeration<ZipArchiveEntry> zipEntries = zip.getEntries(); zipEntries.hasMoreElements(); ) {
					final ZipArchiveEntry zipEntry = zipEntries.nextElement();
					final Path targetPath = cacheDir.resolve(zipEntry.getName());

					if (zipEntry.isDirectory()) {
						Files.createDirectories(targetPath);
					} else {
						Files.createDirectories(targetPath.getParent());
						Files.copy(zip.getInputStream(zipEntry), targetPath);
					}

					final PosixFileAttributeView targetPosixAttribs = Files.getFileAttributeView(targetPath, PosixFileAttributeView.class);
					final Set<PosixFilePermission> permissions = getPosixPermissionsAsSet(zipEntry.getUnixMode());
					if (!permissions.isEmpty() && targetPosixAttribs != null) {
						targetPosixAttribs.setPermissions(permissions);
					}
					System.out.printf("Unzipped: %s perms=%s%n", zipEntry.getName(), permissions);
				}
			}
		} finally {
			Files.delete(tempFile);
		}
		if (!Files.isDirectory(mavenDir)) {
			throw new IOException("Unzipping Maven archive did not produce expected directory: " + mavenDir);
		}
		return mavenDir;
	}

	public int run(Map<String, String> props, String... args) throws IOException {
		return run(props, Arrays.asList(args));
	}

	public int run(Map<String, String> props, List<String> args) throws IOException {
		final List<String> commandLine = new LinkedList<>();
		commandLine.add(getMavenExePath());
		if (debug) {
			commandLine.add("-X");
		}
		props.forEach((k, v) -> commandLine.add(String.format("-D%s=%s", k, v)));
		commandLine.addAll(args);

		if (debug) {
			System.out.printf("Executing: %s%n", commandLine.stream()
					.map(s -> s.matches("\\s+") ? String.format("\"%s\"", s) : s)
					.collect(Collectors.joining(" ")));
		}

		final Process process = new ProcessBuilder(commandLine)
			.inheritIO()
			.start();
		try {
			return process.waitFor();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt(); // Restore interrupt state
			throw new IOException("Interrupted waiting for Maven process to complete", e);
		}
	}

	public static Set<PosixFilePermission> getPosixPermissionsAsSet(int mode) {
		Set<PosixFilePermission> permissionSet = new HashSet<>();
		if ((mode & 0400) > 0) {
			permissionSet.add(PosixFilePermission.OWNER_READ);
		}
		if ((mode & 0200) > 0) {
			permissionSet.add(PosixFilePermission.OWNER_WRITE);
		}
		if ((mode & 0100) > 0) {
			permissionSet.add(PosixFilePermission.OWNER_EXECUTE);
		}
		if ((mode & 0040) > 0) {
			permissionSet.add(PosixFilePermission.GROUP_READ);
		}
		if ((mode & 0020) > 0) {
			permissionSet.add(PosixFilePermission.GROUP_WRITE);
		}
		if ((mode & 0010) > 0) {
			permissionSet.add(PosixFilePermission.GROUP_EXECUTE);
		}
		if ((mode & 0004) > 0) {
			permissionSet.add(PosixFilePermission.OTHERS_READ);
		}
		if ((mode & 0002) > 0) {
			permissionSet.add(PosixFilePermission.OTHERS_WRITE);
		}
		if ((mode & 0001) > 0) {
			permissionSet.add(PosixFilePermission.OTHERS_EXECUTE);
		}
		return permissionSet;
	}
}
