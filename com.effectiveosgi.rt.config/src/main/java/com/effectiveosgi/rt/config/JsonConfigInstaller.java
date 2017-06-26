package com.effectiveosgi.rt.config;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.codec.binary.Hex;
import org.apache.felix.fileinstall.ArtifactInstaller;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.log.LogService;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

@Component(
		name = JsonConfigInstaller.PID,
		property = {
				"type=json"
		})
public class JsonConfigInstaller implements ArtifactInstaller {
	
	public static final String SUFFIX_JSON = ".json";
	
	static final String PID = "com.effectiveosgi.rt.config.json";
	static final String PROP_HASH = "_" + PID + ".record_hash";

	private static final Type TYPE_RECORD = new TypeToken<Map<String,Object>>(){}.getType();

	private final Gson gson = new GsonBuilder().setLenient().create();

	@Reference(cardinality = ReferenceCardinality.OPTIONAL)
	LogService log;
	
	@Reference
	ConfigurationAdmin configAdmin;

	@Override
	public boolean canHandle(File artifact) {
		return artifact.getName().toLowerCase().endsWith(SUFFIX_JSON);
	}

	@Override
	public void install(File artifact) throws Exception {
		log(LogService.LOG_INFO, "Installing artifact " + artifact.getAbsolutePath(), null);
		loadConfig(artifact);
	}


	@Override
	public void update(File artifact) throws Exception {
		log(LogService.LOG_INFO, "Updating artifact " + artifact.getAbsolutePath(), null);
		loadConfig(artifact);
	}

	@Override
	public void uninstall(File artifact) throws Exception {
		log(LogService.LOG_INFO, "Uninstalling artifact" + artifact.getAbsolutePath(), null);
		deleteConfigs(artifact);
	}
	
	private void loadConfig(File artifact) throws IOException, IllegalArgumentException {
		String fileName = artifact.getName();
		String pid = fileName.substring(0, fileName.length() - SUFFIX_JSON.length());
		
		try (JsonReader jsonReader = gson.newJsonReader(new FileReader(artifact))) {
			JsonElement rootElement = gson.fromJson(jsonReader, JsonElement.class);
			if (rootElement.isJsonObject()) {
				Map<String, Object> record = gson.fromJson(rootElement, TYPE_RECORD);
				loadSingletonConfig(pid, record);
			} else if (rootElement.isJsonArray()) {
				Map<String, Map<String, Object>> records = parseArrayToRecords(artifact, rootElement);
				loadFactoryConfig(pid, records);
			} else {
				throw new IllegalArgumentException(String.format("Invalid top-level JSON config object in file %s, must be an object or an array", artifact.getAbsolutePath()));
			}
		}
	}

	private Map<String, Map<String, Object>> parseArrayToRecords(File artifact, JsonElement rootElement) {
		Map<String, Map<String, Object>> records = new HashMap<>();
		int index = 0;
		for (JsonElement element : rootElement.getAsJsonArray()) {
			if (element.isJsonObject()) {
				String hash = hash(element.toString());
				Map<String, Object> record = gson.fromJson(element, TYPE_RECORD);
				if (records.putIfAbsent(hash, record) != null) {
					log(LogService.LOG_WARNING, String.format("Entry index %d in JSON config array file is a duplicate, ignoring", index, artifact.getAbsolutePath()), null);
				}
			} else {
				throw new IllegalArgumentException(String.format("Invalid entry in JSON config array at index %d of file %s: must be a JSON object", index, artifact.getAbsolutePath()));
			}
			index ++;
		}
		return records;
	}
	
	private String hash(String content) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			byte[] hashBytes = digest.digest(content.getBytes(Charset.forName("UTF-8")));
			return Hex.encodeHexString(hashBytes);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Can't happen!?", e);
		}
	}
	
	private void loadSingletonConfig(String pid, Map<String, Object> record) throws IOException {
		Configuration config = configAdmin.getConfiguration(pid, "?");
		Dictionary<String, Object> newProps = new Hashtable<>(record);
		Dictionary<String, Object> existingProps = config.getProperties();
		if (existingProps == null || !existingProps.equals(newProps)) {
			config.update(newProps);
		}
	}

	private void loadFactoryConfig(String factoryPid, Map<String, Map<String, Object>> updatedRecords) throws IOException {
		Set<String> existingHashes = new HashSet<>();
		try {
			// First delete any configurations with hashes not in the map
			Configuration[] existingConfigs = configAdmin.listConfigurations(String.format("(service.factoryPid=%s)", factoryPid));
			if (existingConfigs != null) for (Configuration existingConfig : existingConfigs) {
				Object hash = existingConfig.getProperties().get(PROP_HASH);
				if (!updatedRecords.containsKey(hash)) {
					existingConfig.delete();
				} else if (hash instanceof String){
					existingHashes.add((String) hash);
				}
			}
			
			// Now create new ones that have hashes we didn't see above
			for (Entry<String, Map<String, Object>> updatedEntry : updatedRecords.entrySet()) {
				String hash = updatedEntry.getKey();
				if (!existingHashes.contains(hash)) {
					Dictionary<String, Object> dict = new Hashtable<>(updatedEntry.getValue());
					dict.put(PROP_HASH, hash);
					Configuration config = configAdmin.createFactoryConfiguration(factoryPid, "?");
					config.update(dict);
				}
			}
		} catch (InvalidSyntaxException e) {
			// Can't happen?
			throw new IllegalArgumentException("Invalid configuration filter", e);
		}

	}

	private void deleteConfigs(File artifact) {
		String fileName = artifact.getName();
		String pid = fileName.substring(0, fileName.length() - SUFFIX_JSON.length());

		try {
			String filter = String.format("(|(service.pid=%s)(service.factoryPid=%s))", pid, pid);
			Configuration[] configs = configAdmin.listConfigurations(filter);
			if (configs != null) for (Configuration config : configs) {
				try {
					config.delete();
				} catch (Exception e) {
					log(LogService.LOG_ERROR, String.format("Failed to delete config %s for file %s", printConfigId(config), artifact.getAbsoluteFile()), e);
				}
			}
		} catch (IOException | InvalidSyntaxException e) {
			log(LogService.LOG_ERROR, "Error listing configurations for file " + artifact.getAbsolutePath(), null);
		}
	}

	private String printConfigId(Configuration config) {
		return config.getFactoryPid() == null ? String.format("pid=%s", config.getPid()) : String.format("factoryPid=%s, pid=%s", config.getFactoryPid(), config.getPid());
	}

	private void log(int level, String message, Throwable exception) {
		if (log != null) log.log(level, message, exception);
	}

}
