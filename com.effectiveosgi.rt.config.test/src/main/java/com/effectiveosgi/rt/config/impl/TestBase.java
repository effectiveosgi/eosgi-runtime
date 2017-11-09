package com.effectiveosgi.rt.config.impl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.felix.fileinstall.ArtifactInstaller;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.osgi.framework.*;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationListener;

public class TestBase {

	private final BundleContext context = FrameworkUtil.getBundle(TestBase.class).getBundleContext();

	protected File configDir;

	private ServiceReference<ArtifactInstaller> installerRef;
	protected ArtifactInstaller installer;

	private ServiceReference<ConfigurationAdmin> configAdminRef;
	protected ConfigurationAdmin configAdmin;
	
	protected ConfigurationListener configListener;
	private ServiceRegistration<ConfigurationListener> configListenerReg;

	
	@BeforeClass
	public static void beforeAll() throws Exception {
		Thread.sleep(1000);
	}

	@Before
	public void setup() throws Exception {
		configDir = Files.createTempDirectory("load").toFile();
		configDir.deleteOnExit();

		configAdminRef = context.getServiceReference(ConfigurationAdmin.class);
		configAdmin = context.getService(configAdminRef);
		assertNotNull("ConfigurationAdmin service unavailable", configAdmin);

		Collection<ServiceReference<ArtifactInstaller>> installerRefs = context.getServiceReferences(ArtifactInstaller.class, "(type=hierarchical)");
		assertNotNull("ArtifactInstaller not registered", installerRefs);
		assertEquals("Should be one ArtifactInstaller with type=hierarchical", 1, installerRefs.size());
		installerRef = installerRefs.iterator().next();
		installer = context.getService(installerRef);
		assertNotNull("ArtifactInstaller unregistered", installer);

		Configuration[] configs = configAdmin.listConfigurations(null);
		boolean deleted = false;
		if (configs != null)
			for (Configuration config : configs) {
				config.delete();
				deleted = true;
			}
		if (deleted)
			Thread.sleep(1000);

		configListener = mock(ConfigurationListener.class);
		configListenerReg = context.registerService(ConfigurationListener.class, configListener, null);
	}

	@After
	public void shutdown() throws Exception {
		configListenerReg.unregister();
		context.ungetService(installerRef);
		context.ungetService(configAdminRef);
		recurseDelete(configDir);
	}

	private void recurseDelete(File dir) throws IOException {
		Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.deleteIfExists(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.deleteIfExists(dir);
				return FileVisitResult.CONTINUE;
			}
		});
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

    protected static Stream<Dictionary<String,Object>> streamCleanedConfigs(Configuration[] configs) {
		if (configs == null) configs = new Configuration[0];
		return Arrays.stream(configs)
				.map(Configuration::getProperties)
				.sorted(TestBase::orderDictionaries)
				.map(TestBase::removeGeneratedProperties);
	}

	protected static Set<Dictionary<String,Object>> cleanedConfigs(Configuration[] configs) {
		return streamCleanedConfigs(configs).collect(Collectors.toSet());
	}

    protected static void assertDeepConfigsEqual(String message, Collection<Dictionary<String,Object>> expected, Configuration[] configs) {
		List<Dictionary<String, Object>> expectedList = expected.stream()
				.sorted(TestBase::orderDictionaries)
				.collect(Collectors.toList());

		List<Dictionary<String, Object>> actualList = streamCleanedConfigs(configs).collect(Collectors.toList());

		boolean success = false;
        try {
			assertEquals(message + ": size mismatch", expectedList.size(), actualList.size());
			for (int i = 0; i < actualList.size(); i++) {
				assertDeepConfigEquals("item index " + i + " differs", expectedList.get(i), actualList.get(i));
			}
			success = true;
		} finally {
        	if (!success) {
        		System.out.println("EXPECTED:");
        		printDictionaries(" - ", expectedList);
				System.out.println("ACTUAL:");
				printDictionaries(" - ", actualList);
			}
		}
	}

	protected static <K,V> void assertDeepConfigEquals(String message, Dictionary<K,V> expected, Dictionary<K,V> actual) {
		Collections.list(expected.keys())
				.stream()
				.forEach(k -> {
					String m =  message + ": key " + k + " is not a match";
					V expectedVal = expected.get(k);
					V actualVal = actual.get(k);
					if (expectedVal.getClass().isArray())
						assertArraysEqual(m, expectedVal, actualVal);
					else
						assertEquals(m, expectedVal, actualVal);
				});
		Collections.list(actual.keys())
				.stream()
				.forEach(k -> {
					String m =  message + ": key " + k + " is not a match";
					V expectedVal = expected.get(k);
					V actualVal = actual.get(k);
					if (actualVal.getClass().isArray())
						assertArraysEqual(m, expectedVal, actualVal);
					else
						assertEquals(m, expectedVal, actualVal);
				});
	}

	protected static void assertArraysEqual(String message, Object expected, Object actual) {
		if (expected == null || !expected.getClass().isArray()) fail (message + ": expected is not an array");
		if (actual == null || !actual.getClass().isArray()) fail (message + ": actual is not an array");
		assertEquals(message + ": array lengths differ", Array.getLength(expected), Array.getLength(actual));
		for (int i = 0; i < Array.getLength(expected); i++) {
			assertEquals(message + ": index " + i + " differs", Array.get(expected, i), Array.get(actual, i));
		}
	}

	protected static int orderDictionaries(Dictionary<String, Object> left, Dictionary<String, Object> right) {
		int result = orderNullSafe(
				(String) left.get(Constants.SERVICE_PID),
				(String) right.get(Constants.SERVICE_PID));

		if (result != 0) return result;

		result = orderNullSafe(
				(String) left.get(HierarchicalConfigInstaller.PROP_IDENTITY),
				(String) right.get(HierarchicalConfigInstaller.PROP_IDENTITY));

		return result;
	}

	protected static <T extends Comparable<T>> int orderNullSafe(T left, T right) {
		if (left == null)
			return right != null ? 1 : 0;
		if (right == null)
			return left != null ? -1 : 0;

		return left.compareTo(right);
	}

	protected static int compareDictionariesOnRecordID(Dictionary<String, Object> left, Dictionary<String, Object> right) {
		String leftId = (String) left.get(HierarchicalConfigInstaller.PROP_IDENTITY);
		String rightId = (String) right.get(HierarchicalConfigInstaller.PROP_IDENTITY);
		if (leftId == null) {
			return rightId != null ? 1 : 0;
		}
		if (rightId == null) {
			return leftId != null ? -1 : 0;
		}
		return leftId.compareTo(rightId);
	}

	private static Dictionary<String, Object> removeGeneratedProperties(Dictionary<String, Object> dict) {
    		boolean factory = dict.get("service.factoryPid") != null;

        Dictionary<String, Object> copy = new Hashtable<>();
        for (Enumeration<String> e = dict.keys(); e.hasMoreElements(); ) {
            String key = e.nextElement();
            if (key.startsWith("_com.effectiveosgi.rt.config"))
            		continue;
            if (factory && "service.pid".equals(key))
            		continue;
            copy.put(key, dict.get(key));
        }
        return copy;
    }
    
    protected Configuration findFactoryConfig(String factoryPid, String recordId) throws IOException, InvalidSyntaxException {
    		Configuration[] configs = configAdmin.listConfigurations(String.format("(&(%s=%s)(%s=%s))",
    				ConfigurationAdmin.SERVICE_FACTORYPID, factoryPid,
    				HierarchicalConfigInstaller.PROP_IDENTITY, recordId));
    		if (configs == null || configs.length == 0)
    			return null;
    		if (configs.length > 1)
    			throw new IllegalArgumentException(String.format("Ambiguous config record for factoryPid=%s and recordId=%s. Something has gone wrong!", factoryPid, recordId));
    		return configs[0];
    }
    
    protected static void printDictionaries(String indent, Collection<? extends Dictionary<String, ? extends Object>> dicts) {
		for (Dictionary<String, ? extends Object> dict : dicts) {
			System.out.printf("Dictionary Size %d%n", dict.size());
			Collections.list(dict.keys())
					.stream()
					.sorted()
					.forEach(key -> {
						Object value = dict.get(key);
						Class<?> type = value.getClass();

						String valueStr;
						String typeStr;
						if (type.isArray()) {
							typeStr = type.getComponentType().getName() + "[]";
							StringBuilder sb = new StringBuilder().append("{");
							for (int i = 0; i < Array.getLength(value); i++) {
								if (i > 0) sb.append(",");
								Object entryValue = Array.get(value, i);
								sb.append(entryValue != null ? entryValue.toString() : "<null>");
							}
							valueStr = sb.append("}").toString();
						} else {
							typeStr = value.getClass().getName();
							valueStr = value != null ? value.toString() : "<null>";
						}
						System.out.printf("%s%s=%s (type=%s)%n", indent, key, valueStr, typeStr);
					});
		}
    }

}
