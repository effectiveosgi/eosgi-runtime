package com.effectiveosgi.rt.config.impl.json;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.codec.binary.Hex;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.log.LogService;

import com.effectiveosgi.rt.config.ConfigFileReader;
import com.effectiveosgi.rt.config.ParsedRecord;
import com.effectiveosgi.rt.config.RecordIdentity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

@Component(property = {
		ConfigFileReader.PROP_FILE_PATTERN + "=.*\\.json"
})
public class JsonConfigReader implements ConfigFileReader {

	/**
	 * Matches the :configurator:resource-version property.
	 */
	public static final int SUPPORTED_RESOURCE_VERSION = 1;

	private static final String PROP_PREFIX = ":configurator:";
	private static final String PROP_RESOURCE_VERSION = PROP_PREFIX + "resource-version";

	private final Gson gson = new GsonBuilder().setLenient().create();
	
	@Reference(cardinality = ReferenceCardinality.OPTIONAL)
	LogService log;
	
	@Override
	public Stream<ParsedRecord> load(File artifact) throws IOException {
		try (JsonReader jsonReader = gson.newJsonReader(new FileReader(artifact))) {
			final JsonElement rootElement = gson.fromJson(jsonReader, JsonElement.class);
			if (!rootElement.isJsonObject())
				throw new IOException(String.format("Invalid top-level JSON config object in file %s, must be an object", artifact.getAbsolutePath()));

			// Verify the :configurator:resource-version element
			final int resourceVersion;
			JsonObject rootObject = rootElement.getAsJsonObject();
			JsonElement resourceVersionElem = rootObject.get(PROP_RESOURCE_VERSION);
			if (resourceVersionElem != null)
				resourceVersion = resourceVersionElem.getAsInt();
			else
				resourceVersion = 1;
			if (resourceVersion > SUPPORTED_RESOURCE_VERSION)
				throw new IOException(String.format("Unsupported resource format version. Found %d, version %d is supported.", resourceVersion, SUPPORTED_RESOURCE_VERSION));

			// Read entries and transform to parsed records
			return rootObject.entrySet().stream()
				.filter(recordEntry -> {
					// Remove & warn on unsupported special keys
					if (recordEntry.getKey().startsWith(PROP_PREFIX) && !PROP_RESOURCE_VERSION.equals(recordEntry.getKey())) {
						log(LogService.LOG_WARNING, String.format("Ignoring unsupported special configuration key '%s' in resource level of %s", recordEntry.getKey(), artifact.getAbsolutePath()), null);;
						return false;
					}
					return true;
				})
				.flatMap(recordEntry -> {
					RecordIdentity recordId = parseEntryKey(recordEntry.getKey());
					JsonElement recordElem = recordEntry.getValue();
					try {
						// Transform the JsonElement entries into parsed Maps iff they are JsonObjects
						if (!recordElem.isJsonObject()) {
							log(LogService.LOG_WARNING, String.format("Ignoring unsupported record type %s for id %s: only object entries are supported", recordElem.getClass().getSimpleName(), recordId), null);
							return null;
						}
						JsonObject recordObj = recordElem.getAsJsonObject();

						// Transform the record content JsonObject into a Map
						Map<String, Object> parsedMap = recordObj.entrySet().stream()
							.filter(propEntry -> {
								// Remove & warn on unsupported special keys
								if (propEntry.getKey().startsWith(PROP_PREFIX)) {
									log(LogService.LOG_WARNING, String.format("Ignoring unsupported special configuration key '%s' in record %s in %s", propEntry.getKey(), recordEntry.getKey(), artifact.getAbsolutePath()), null);;
									return false;
								}
								return true;
							})
							.map(TypeUtils::parseEntry)
							.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (u,v) -> {throw new IllegalArgumentException("Duplicate key");}));
						return Stream.of(new ParsedRecord(recordId, parsedMap));
					} catch (IllegalArgumentException e) {
						throw new IllegalArgumentException(String.format("Error loading record %s: %s", recordId, e.getMessage()), e);
					}
				});
		}
	}
	
	private static RecordIdentity parseEntryKey(String key) {
		String[] parts = key.split("~", 2);
		return (parts.length > 1)
				? new RecordIdentity(parts[1], parts[0])
				: new RecordIdentity(parts[0], null);
	}

	public String getKey(JsonElement element) {
		String content = element.toString();
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			digest.update(content.getBytes(StandardCharsets.UTF_8));
			byte[] hashBytes = digest.digest();
			return Hex.encodeHexString(hashBytes);
		} catch (NoSuchAlgorithmException e) {
			// Shouldn't ever happen...
			throw new RuntimeException("Missing hash algorithm SHA-1", e);
		}
	}
	
	private void log(int level, String message, Throwable exception) {
		if (log != null) log.log(level, message, exception);
	}

	
}