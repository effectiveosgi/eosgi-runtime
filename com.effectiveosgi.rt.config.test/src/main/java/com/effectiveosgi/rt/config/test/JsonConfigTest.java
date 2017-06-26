package com.effectiveosgi.rt.config.test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.felix.fileinstall.ArtifactInstaller;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import static org.junit.Assert.*;

@SuppressWarnings("serial")
public class JsonConfigTest {

    private final BundleContext context = FrameworkUtil.getBundle(JsonConfigTest.class).getBundleContext();

    private File configDir;

    private ServiceReference<ArtifactInstaller> installerRef;
    private ArtifactInstaller installer;

    private ServiceReference<ConfigurationAdmin> configAdminRef;
    private ConfigurationAdmin configAdmin;


    @Before
    public void setup() throws Exception {
        configDir = Files.createTempDirectory("load").toFile();
        configDir.deleteOnExit();

        configAdminRef = context.getServiceReference(ConfigurationAdmin.class);
        configAdmin = context.getService(configAdminRef);
        assertNotNull("ConfigurationAdmin service unavailable", configAdmin);

        Collection<ServiceReference<ArtifactInstaller>> installerRefs = context.getServiceReferences(ArtifactInstaller.class, "(type=json)");
        assertNotNull("ArtifactInstaller not registered", installerRefs);
        assertEquals("Should be one ArtifactInstaller with type=json", 1, installerRefs.size());
        installerRef = installerRefs.iterator().next();
        installer = context.getService(installerRef);
        assertNotNull("ArtifactInstaller unregistered", installer);

        Configuration[] configs = configAdmin.listConfigurations(null);
        boolean deleted = false;
        if (configs != null) for (Configuration config : configs) {
            config.delete();
            deleted = true;
        }
        if (deleted)
            Thread.sleep(1000);
    }

    @After
    public void shutdown() throws Exception {
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


    @Test
    public void testZeroConfigsWhenNoConfigFileExists() throws Exception {
        assertConfigsEqual("configs should be empty", Collections.emptySet(), configAdmin.listConfigurations(null));
    }

    @Test
    public void testInstallJsonFileWithOneConfig() throws Exception {
        Dictionary<String,Object> expected = new Hashtable<String,Object>() {{
            put("service.pid", "org.example");
            put("foo", "bar");
        }};
        final File jsonFile = writeConfigFile("{'foo':'bar'}", "org.example.json");
        assertTrue(installer.canHandle(jsonFile));

        installer.install(jsonFile);
        Thread.sleep(1000);
        assertConfigsEqual(Collections.singleton(expected), configAdmin.listConfigurations(null));

        installer.uninstall(jsonFile);
        Thread.sleep(1000);
        assertConfigsEqual("configs should be empty after json file uninstall", Collections.emptySet(), configAdmin.listConfigurations(null));
    }

    @Test
    public void testJsonTypes() throws Exception {
        Dictionary<String,Object> expected = new Hashtable<String,Object>() {{
            put("service.pid", "org.example");
            put("string", "string");
            put("number", 0.0d);
            put("float", 0.1d);
            put("boolean", true);
        }};
        final File jsonFile = writeConfigFile("{'string':'string', 'number' : 0, 'float' : 0.1, 'boolean' : true }", "org.example.json");
        assertTrue(installer.canHandle(jsonFile));

        installer.install(jsonFile);
        Thread.sleep(1000);
        assertConfigsEqual(Collections.singleton(expected), configAdmin.listConfigurations(null));

        installer.uninstall(jsonFile);
        Thread.sleep(1000);
        assertConfigsEqual("configs should be empty after json file uninstall", Collections.emptySet(), configAdmin.listConfigurations(null));
    }

    @Test
    public void testInstallAndUpdateJsonFileWithMultipleEntries() throws Exception {
        Set<Dictionary<String,Object>> expected = new HashSet<>();
        expected.add(new Hashtable<String,Object>(){{
            put("service.factoryPid", "org.example");
            put("a","b");
        }});
        expected.add(new Hashtable<String,Object>(){{
            put("service.factoryPid", "org.example");
            put("b","c");
        }});

        File jsonFile = writeConfigFile("[{'a':'b'},{'b':'c'}]", "org.example.json");
        assertTrue(installer.canHandle(jsonFile));

        // Initial Install
        installer.install(jsonFile);
        Thread.sleep(1000);
        assertConfigsEqual(expected, configAdmin.listConfigurations(null));

        // Update -> change one record
        expected = new HashSet<>();
        expected.add(new Hashtable<String,Object>(){{
            put("a","x");
            put("service.factoryPid", "org.example");
        }});
        expected.add(new Hashtable<String,Object>(){{
            put("b","c");
            put("service.factoryPid", "org.example");
        }});
        writeConfigFile("[{'a':'x'},{'b':'c'}]", "org.example.json");
        installer.update(jsonFile);
        Thread.sleep(1000);
        assertConfigsEqual(expected, configAdmin.listConfigurations(null));

        // Uninstall
        installer.uninstall(jsonFile);
        Thread.sleep(1000);
        assertConfigsEqual("configs should be empty after json file uninstall", Collections.emptySet(), configAdmin.listConfigurations(null));
    }

    private File writeConfigFile(String content, String fileName) throws IOException {
        File jsonFile = new File(configDir, fileName);
        Files.copy(new ByteArrayInputStream(content.getBytes()), jsonFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        jsonFile.deleteOnExit();
        return jsonFile;
    }

    private void assertConfigsEqual(Collection<Dictionary<String,Object>> expected, Configuration[] configs) {
        assertConfigsEqual(null, expected, configs);
    }

    private void assertConfigsEqual(String message, Collection<Dictionary<String,Object>> expected, Configuration[] configs) {
        if (configs == null) configs = new Configuration[0];
        assertEquals(message, expected.size(), configs.length);
        Set<Dictionary<String, Object>> dicts = Arrays.stream(configs)
                .map(Configuration::getProperties)
                .map(this::removeGeneratedServicePid)
                .collect(Collectors.toSet());
        assertEquals(message, expected, dicts);
    }

    private Dictionary<String, Object> removeGeneratedServicePid(Dictionary<String, Object> dict) {
        if (dict.get("service.factoryPid") == null)
            return dict;

        Dictionary<String, Object> copy = new Hashtable<>();
        for (Enumeration<String> e = dict.keys(); e.hasMoreElements(); ) {
            String key = e.nextElement();
            if (!"service.pid".equals(key) && !"_com.effectiveosgi.rt.config.json.record_hash".equals(key))
                copy.put(key, dict.get(key));
        }
        return copy;
    }
}
