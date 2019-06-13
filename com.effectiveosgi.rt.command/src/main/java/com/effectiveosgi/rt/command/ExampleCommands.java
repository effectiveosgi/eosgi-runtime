package com.effectiveosgi.rt.command;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

class ExampleCommands {

	@SuppressWarnings("serial")
	public ServiceRegistration<?> register(BundleContext context) {
		return context.registerService(Object.class, new ExampleCommands(), new Hashtable<String, Object>() {{
			put("osgi.command.scope", "example");
			put("osgi.command.function", new String[] {
					"hello",
					"pwd",
					"resolve"
			});
		}});
	}

	public String hello(String name) {
		System.out.println("Generating message...");
		return "Hello " + name;
	}

	public File pwd() throws IOException {
		return new File(".").getCanonicalFile();
	}

	public File resolve(String path) throws IOException {
		return new File(path).getCanonicalFile();
	}

}