package com.effectiveosgi.rt.command;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.annotation.bundle.Requirement;
import org.osgi.annotation.bundle.Requirement.Cardinality;
import org.osgi.annotation.bundle.Requirement.Resolution;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

@Requirement(
		namespace = Motd.NAMESPACE,
		cardinality = Cardinality.MULTIPLE,
		resolution = Resolution.OPTIONAL,
		filter = "(" + Motd.PATH + "=*)"
)
public class Motd {
	
	public static final String[] functions = { "motd" };
	static final String NAMESPACE = "eosgi.rt.command.motd";
	static final String PATH = "path";
	private static final Random RANDOM = new Random();
	private final BundleContext context;
	
	public Motd(BundleContext context) {
		this.context = context;
	}
	
	public final void motd() throws IOException {
		final BundleWiring wiring = context.getBundle().adapt(BundleWiring.class);
		final List<String> motds = Optional.ofNullable(wiring.getRequiredWires(NAMESPACE))
			.map(Collection::stream).orElse(Stream.empty())
			.map(BundleWire::getCapability)
			.flatMap(cap -> {
				try {
					final String motdPath = (String) cap.getAttributes().get(PATH);
					final Bundle provider = cap.getRevision().getBundle();
					final URL entryPath = provider.getEntry(motdPath);

					String motd = readFully(entryPath);
					return Stream.of(motd);
				} catch (IOException e) {
					// Ignore this entry
					return Stream.empty();
				}
			})
			.collect(Collectors.toList());
		
		final String motd;
		if (motds.isEmpty()) {
			motd = readFully(context.getBundle().getEntry("motd"));
		} else {
			motd = motds.get(RANDOM.nextInt(motds.size()));
		}
		System.out.println(motd);
	}

	private static String readFully(final URL url) throws IOException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (InputStream in = url.openStream()) {
			byte[] buf = new byte[1024];
			int read = in.read(buf);
			while (read >= 0) {
				out.write(buf, 0, read);
				read = in.read(buf);
			}
		}
		return out.toString(StandardCharsets.UTF_8.name());
	}

}
