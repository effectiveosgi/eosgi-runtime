package com.effectiveosgi.rt.config.impl.yaml;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.effectiveosgi.rt.config.ParsedRecord;
import com.effectiveosgi.rt.config.RecordIdentity;

public class YamlReaderTest {
	
	private File configDir;

	@Before
	public void setup() throws Exception {
		configDir = Files.createTempDirectory("load").toFile();
		configDir.deleteOnExit();
	}

	@After
	public void shutdown() throws Exception {
		Files.walkFileTree(configDir.toPath(), new SimpleFileVisitor<Path>() {
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.deleteIfExists(file);
				return FileVisitResult.CONTINUE;
			}
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.deleteIfExists(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	@Test
	public void testReadRecords() throws Exception {
		File configFile = writeConfigFile(YamlReaderTest.class.getClassLoader().getResourceAsStream("sample1.yaml"), "sample1.yaml");
		List<ParsedRecord> records = new YamlConfigReader().load(configFile).collect(Collectors.toList());
		assertEquals(3, records.size());
		
		Iterator<ParsedRecord> recordIter = records.iterator();
		ParsedRecord record;

		record = recordIter.next();
		assertEquals(new RecordIdentity("org.example", null), record.getId());
		assertEquals("bar", record.getProperties().get("foo"));
		assertEquals(Arrays.asList(new String[] { "Ein", "Zwei", "Drei" }), record.getProperties().get("Deutsch"));
		assertEquals(Arrays.asList(new String[] { "Ein", "Zwei", "Drei" }), record.getProperties().get("German"));
		assertEquals(Arrays.asList(new Integer[] { 1, 2, 3 }), record.getProperties().get("numbers"));
		assertEquals(123, record.getProperties().get("intVal"));
		assertEquals(2_147_483_648L, record.getProperties().get("longVal"));
		assertEquals(123.0d, record.getProperties().get("floatVal"));
		assertEquals(123.0d, record.getProperties().get("anotherFloat"));
		
		record = recordIter.next();
		assertEquals(new RecordIdentity("one", "org.example.server"), record.getId());
		assertEquals("0.0.0.0", record.getProperties().get("host"));
		assertEquals(8080, record.getProperties().get("port"));
		
		record = recordIter.next();
		assertEquals(new RecordIdentity("two", "org.example.server"), record.getId());
		assertTrue((Boolean) record.getProperties().get("useSsl"));
		assertFalse((Boolean) record.getProperties().get("logRequests"));
	}

	protected File writeConfigFile(String content, String fileName) throws IOException {
		return writeConfigFile(new ByteArrayInputStream(content.getBytes()), fileName);
	}

	protected File writeConfigFile(InputStream stream, String fileName) throws IOException {
		File file = new File(configDir, fileName);
		Files.copy(stream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
		file.deleteOnExit();
		return file;
	}
}
