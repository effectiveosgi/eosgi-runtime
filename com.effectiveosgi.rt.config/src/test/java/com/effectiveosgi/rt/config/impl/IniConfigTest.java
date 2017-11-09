package com.effectiveosgi.rt.config.impl;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com.effectiveosgi.rt.config.ParsedRecord;

public class IniConfigTest {

	@Test
	public void testSingletonFile() throws Exception {
		List<ParsedRecord> records = load("org.example1");
		assertEquals(1, records.size());
		ParsedRecord record = records.get(0);
		assertEquals("org.example1", record.getId().getId());
		assertNull(record.getId().getFactoryId());
		assertEquals("bar", record.getProperties().get("foo"));
		assertEquals("baz", record.getProperties().get("bar"));
	}
	
	@Test
	public void testFactoryFile() throws Exception {
		List<ParsedRecord> records = load("org.example2");
		assertEquals(3, records.size());

		ParsedRecord record;

		record = records.get(0);
		assertEquals("first", record.getId().getId());
		assertEquals("org.example2", record.getId().getFactoryId());
		assertEquals("bar", record.getProperties().get("foo"));
		assertEquals("baz", record.getProperties().get("bar"));

		record = records.get(1);
		assertEquals("second", record.getId().getId());
		assertEquals("org.example2", record.getId().getFactoryId());

		record = records.get(2);
		assertEquals("third", record.getId().getId());
		assertEquals("org.example2", record.getId().getFactoryId());
		assertEquals("Guten Tag", record.getProperties().get("hello"));
		assertEquals("Auf Wiedersehen", record.getProperties().get("goodbye"));
	}
	
	@Test
	public void testMultiValueProperty() throws Exception {
		List<ParsedRecord> records = load("org.example3");
		assertEquals(1, records.size());
		ParsedRecord record = records.get(0);
		assertEquals("org.example3", record.getId().getId());
		assertNull(record.getId().getFactoryId());
		assertArrayEquals(new String[] { "bar", "baz", "wibble" }, (String[]) record.getProperties().get("foo"));
	}
	
	private static List<ParsedRecord> load(String pid) throws IOException {
		final IniConfigReader reader = new IniConfigReader();
		String path = pid + ".ini";
		try (InputStream stream = IniConfigTest.class.getClassLoader().getResourceAsStream(path)) {
			return reader.load(path, pid, stream).collect(Collectors.toList());
		}
	}
	
}
