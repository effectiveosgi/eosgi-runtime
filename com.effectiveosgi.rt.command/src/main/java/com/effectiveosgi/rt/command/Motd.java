package com.effectiveosgi.rt.command;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
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
	
	private static final String MISSING_RESOURCE_MESSAGE
			= "Bundle ''{0}'' tried to provide a message-of-the-day, but\n"
			+ "resource path ''{1}'' was missing! SAD :-(";
	private static final String RESOURCE_READ_ERROR
			= "Bundle ''{0}'' tried to provide a message-of-the-day, but\n"
			+ "there was an error reading from path ''{1}''...\n";
	
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
			.map(cap -> {
				final String motdPath = (String) cap.getAttributes().get(PATH);
				final Bundle provider = cap.getRevision().getBundle();
				final URL entry = provider.getEntry(motdPath);
				if (entry != null) {
					try {
						return readFully(entry);
					} catch (IOException e) {
						ByteArrayOutputStream buf = new ByteArrayOutputStream();
						e.printStackTrace(new PrintStream(buf));
						return MessageFormat.format(RESOURCE_READ_ERROR, provider.getSymbolicName(), motdPath)
								+ new String(buf.toByteArray(), StandardCharsets.UTF_8);
					}
				}
				return MessageFormat.format(MISSING_RESOURCE_MESSAGE, provider.getSymbolicName(), motdPath);
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
		return new String(out.toByteArray(), StandardCharsets.UTF_8);
	}

}
