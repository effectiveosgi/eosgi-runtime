package com.effectiveosgi.rt.config.impl.yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.yaml.snakeyaml.Yaml;

import com.effectiveosgi.rt.config.ConfigFileReader;
import com.effectiveosgi.rt.config.ParsedRecord;
import com.effectiveosgi.rt.config.RecordIdentity;
import com.effectiveosgi.rt.config.impl.EntryImpl;
import com.effectiveosgi.rt.config.impl.util.Arrows;

public class YamlConfigReader implements ConfigFileReader {
	
	public static final String PATTERN = ".*\\.yaml";

	@Override
	public Stream<ParsedRecord> load(File artifact) throws IOException {
		FileInputStream stream = new FileInputStream(artifact);
		Yaml yaml = new Yaml();
		Iterable<Object> docs = yaml.loadAll(stream);
		return StreamSupport.stream(docs.spliterator(), false)
			.flatMap(this::filterNonMap)
			.flatMap(this::docToRecords)
			.onClose(() -> {
				try { stream.close(); } catch (IOException e) {}
			});
	}
	
	private Stream<Map<String, Object>> filterNonMap(Object o) {
		if (!(o instanceof Map)) {
			throw new IllegalArgumentException(String.format("Document was not an associative array, actual type was: %s", o != null ? o.getClass().getSimpleName() : "<null>"));
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) o;
		return Stream.of(map);
	}

	private Stream<ParsedRecord> docToRecords(Map<String, Object> doc) {
		return doc.entrySet().stream()
			.map(this::parseRecordContent)
			.map(Arrows.first(this::splitRecordId))
			.map(this::createRecord);
	}

	private Entry<String, Map<String, Object>> parseRecordContent(Entry<String, Object> recordEntry) {
		final Entry<String, Map<String, Object>> resultEntry;
		if (recordEntry.getValue() instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) recordEntry.getValue();
			resultEntry = new EntryImpl<>(recordEntry.getKey(), map);
		} else {
			throw new IllegalArgumentException(String.format("Record '%s' was not an associative array, actual type was: %s", recordEntry.getKey(), recordEntry.getValue() != null ? recordEntry.getValue().getClass().getSimpleName() : "<null>"));
		}
		return resultEntry;
	}

	private RecordIdentity splitRecordId(String key) {
		final RecordIdentity id;
		int index = key.indexOf('~');
		if (index < 0) {
			id = new RecordIdentity(key, null);
		} else {
			String first = key.substring(0, index);
			String second = key.substring(index+1);
			id = new RecordIdentity(second, first);
		}
		return id;
	}
	
	private ParsedRecord createRecord(Entry<RecordIdentity, Map<String, Object>> entry) {
		return new ParsedRecord(entry.getKey(), entry.getValue());
	}

}
