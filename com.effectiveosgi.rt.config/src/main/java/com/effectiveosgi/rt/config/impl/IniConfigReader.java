package com.effectiveosgi.rt.config.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.ini4j.Config;
import org.ini4j.Ini;
import org.ini4j.Profile.Section;

import com.effectiveosgi.rt.config.ConfigFileReader;
import com.effectiveosgi.rt.config.ParsedRecord;

public class IniConfigReader implements ConfigFileReader {
	
	public static final String PATTERN = ".*\\.ini";

	private static final String SUFFIX_INI = ".ini";
	
	Stream<ParsedRecord> load(String location, String filePid, InputStream stream) throws IOException {
		final Stream<ParsedRecord> result;
		
		Config iniConfig = new Config();
		iniConfig.setGlobalSection(true);
		Ini ini = new Ini();
		ini.setConfig(iniConfig);
		ini.load(stream);
		
		Set<String> keys = ini.keySet();
		if (keys.isEmpty()) {
			throw new IOException("Could not parse any records from " + location + ", even the global/root section.");
		} else if (keys.size() == 1 && keys.iterator().next().equals(Config.DEFAULT_GLOBAL_SECTION_NAME)) {
			Section singletonSection = ini.get(Config.DEFAULT_GLOBAL_SECTION_NAME);
			result = Stream.of(ParsedRecord.singleton(filePid, processValues(singletonSection)));
		} else {
			// NB: Ini#entrySet does NOT preserve file order, keySet does.
			return ini.keySet().stream()
					.filter(k -> !Config.DEFAULT_GLOBAL_SECTION_NAME.equals(k))
					.map(k -> ParsedRecord.factory(k, filePid, processValues(ini.get(k))));
		}
		return result;
	}
	
	@Override
	public Stream<ParsedRecord> load(File artifact) throws IOException {
		final String filePid = getPid(artifact);
		try (FileInputStream stream = new FileInputStream(artifact)) {
			return load(artifact.getAbsolutePath(), filePid, stream);
		}
	}

	private String getPid(File artifact) {
		String fileName = artifact.getName();
		return fileName.substring(0, fileName.length() - SUFFIX_INI.length());
	}
	
	private static Map<String, Object> processValues(Section section) {
		Map<String, Object> converted = new HashMap<>();
		for (String key : section.keySet()) {
			int length = section.length(key);
			if (length > 1) {
				String[] array = new String[length];
				for (int i = 0; i < length; i++)
					array[i] = section.fetch(key, i);
				converted.put(key, array);
			} else if (length == 1){
				String val = section.fetch(key, 0);
				converted.put(key, val);
			}
		}
		return converted;
	}
	
}
