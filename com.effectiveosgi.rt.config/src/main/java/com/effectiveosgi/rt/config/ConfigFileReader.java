package com.effectiveosgi.rt.config;

import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface ConfigFileReader {

	/**
	 * A Service property for file name patterns, which must be of type String or
	 * String Array.
	 */
	static final String PROP_FILE_PATTERN = "patterns";

	Stream<ParsedRecord> load(File artifact) throws IOException;

}