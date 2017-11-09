package com.effectiveosgi.rt.config.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.File;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.service.cm.ConfigurationEvent;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("serial")
public class IniConfigTest extends TestBase {
	
	@Captor
	ArgumentCaptor<ConfigurationEvent> eventCaptor;

    @Test
    public void testInstallIniFileWithOneConfig() throws Exception {
	    	Dictionary<String,Object> expected = new Hashtable<String,Object>() {{
            put("service.pid", "org.example");
            put("foo", "bar");
        }};
        final File iniFile = writeConfigFile("foo=bar", "org.example.ini");
        assertTrue(installer.canHandle(iniFile));

        installer.install(iniFile);
        Thread.sleep(1000);
        assertEquals( Collections.singleton(expected), cleanedConfigs(configAdmin.listConfigurations(null)));

        installer.uninstall(iniFile);
        Thread.sleep(1000);
        assertEquals("configs should be empty after json file uninstall", Collections.emptySet(), cleanedConfigs(configAdmin.listConfigurations(null)));
        
        
        // Verify config events
        verify(configListener, times(2)).configurationEvent(eventCaptor.capture());
        Iterator<ConfigurationEvent> configEvents = eventCaptor.getAllValues().iterator();
        {
	        	ConfigurationEvent ev = configEvents.next();
	        	assertEquals("org.example", ev.getPid());
	        	assertEquals(ConfigurationEvent.CM_UPDATED, ev.getType());
        }
        {
	        	ConfigurationEvent ev = configEvents.next();
	        assertEquals("org.example", ev.getPid());
	        assertEquals(ConfigurationEvent.CM_DELETED, ev.getType());
        }
        verifyNoMoreInteractions(configListener);
    }

    @Test
    public void testInstallAndUpdateIniFileWithMultipleNamedEntries() throws Exception {
        Set<Dictionary<String,Object>> expected = new HashSet<>();
        expected.add(new Hashtable<String,Object>(){{
            put("service.factoryPid", "org.example");
            put("a","b");
        }});
        expected.add(new Hashtable<String,Object>(){{
            put("service.factoryPid", "org.example");
            put("b","c");
        }});

        File iniFile = writeConfigFile(""
        		+ "[one]\n"
        		+ "a=b\n"
        		+ "[two]\n"
        		+ "b=c", "org.example.ini");
        assertTrue(installer.canHandle(iniFile));

        // Initial Install -> 2 update events
        installer.install(iniFile);
        Thread.sleep(1000);
        assertEquals(expected, cleanedConfigs(configAdmin.listConfigurations(null)));
        
        // Get the PID of the records in order to check them later
        String pidOne = findFactoryConfig("org.example", "one").getPid();
        String pidTwo = findFactoryConfig("org.example", "two").getPid();

        // Update -> change 1 record -> 1 update event
        expected = new HashSet<>();
        expected.add(new Hashtable<String,Object>(){{
            put("a","x");
            put("service.factoryPid", "org.example");
        }});
        expected.add(new Hashtable<String,Object>(){{
            put("b","c");
            put("service.factoryPid", "org.example");
        }});
        writeConfigFile(""
        		+ "[one]\n"
        		+ "a=x\n"
        		+ "[two]\n"
        		+ "b=c", "org.example.ini");
        installer.update(iniFile);
        Thread.sleep(1000);
        assertEquals(expected, cleanedConfigs(configAdmin.listConfigurations(null)));

        // Uninstall -> 2 delete events
        installer.uninstall(iniFile);
        Thread.sleep(1000);
        assertEquals("configs should be empty after ini file uninstall", Collections.emptySet(), cleanedConfigs(configAdmin.listConfigurations(null)));

        // Verify config events
        verify(configListener, times(5)).configurationEvent(eventCaptor.capture());
        Iterator<ConfigurationEvent> configEvents = eventCaptor.getAllValues().iterator();
        {
        		// Initial update
	        	ConfigurationEvent ev = configEvents.next();
	        	assertEquals("org.example", ev.getFactoryPid());
	        	assertTrue(pidOne.equals(ev.getPid()) || pidTwo.equals(ev.getPid()));
	        	assertEquals(ConfigurationEvent.CM_UPDATED, ev.getType());
        }
        {
        		// Initial update
	        	ConfigurationEvent ev = configEvents.next();
	        	assertEquals("org.example", ev.getFactoryPid());
	        	assertTrue(pidOne.equals(ev.getPid()) || pidTwo.equals(ev.getPid()));
	        	assertEquals(ConfigurationEvent.CM_UPDATED, ev.getType());
	    }
        {
        		// Second update of record one
	        	ConfigurationEvent ev = configEvents.next();
	        	assertEquals("org.example", ev.getFactoryPid());
	        	assertEquals(pidOne, ev.getPid());
	        	assertEquals(ConfigurationEvent.CM_UPDATED, ev.getType());
	    }
        {
	    		// Delete
	        	ConfigurationEvent ev = configEvents.next();
	        	assertEquals("org.example", ev.getFactoryPid());
	        	assertEquals(ConfigurationEvent.CM_DELETED, ev.getType());
	    }
        {
	    		// Delete
	        	ConfigurationEvent ev = configEvents.next();
	        	assertEquals("org.example", ev.getFactoryPid());
	        	assertEquals(ConfigurationEvent.CM_DELETED, ev.getType());
	    }

        verifyNoMoreInteractions(configListener);
    }
    
    @Test
    public void testAddToMultipleEntries() throws Exception {
        Set<Dictionary<String,Object>> expected = new HashSet<>();
        expected.add(new Hashtable<String,Object>(){{
            put("service.factoryPid", "org.example");
            put("a","b");
        }});
        expected.add(new Hashtable<String,Object>(){{
            put("service.factoryPid", "org.example");
            put("b","c");
        }});

        File iniFile = writeConfigFile(""
        		+ "[one]\n"
        		+ "a=b\n"
        		+ "[two]\n"
        		+ "b=c", "org.example.ini");
        assertTrue(installer.canHandle(iniFile));

        // Initial Install -> 2 update events
        installer.install(iniFile);
        Thread.sleep(1000);
        assertEquals(expected, cleanedConfigs(configAdmin.listConfigurations(null)));
        
        // Get the PID of the records in order to check them later
        String pidOne = findFactoryConfig("org.example", "one").getPid();
        String pidTwo = findFactoryConfig("org.example", "two").getPid();

        // Update -> add 1 record -> 1 update event
        expected = new HashSet<>();
        expected.add(new Hashtable<String,Object>(){{
            put("a","b");
            put("service.factoryPid", "org.example");
        }});
        expected.add(new Hashtable<String,Object>(){{
            put("b","c");
            put("service.factoryPid", "org.example");
        }});
        expected.add(new Hashtable<String,Object>(){{
            put("c","d");
            put("service.factoryPid", "org.example");
        }});
        writeConfigFile(""
        		+ "[one]\n"
        		+ "a=b\n"
        		+ "[two]\n"
        		+ "b=c\n"
        		+ "[three]\n"
        		+ "c=d", "org.example.ini");
        installer.update(iniFile);
        Thread.sleep(1000);
        assertEquals( expected, cleanedConfigs(configAdmin.listConfigurations(null)));
        String pidThree = findFactoryConfig("org.example", "three").getPid();

        // Uninstall -> 3 delete events
        installer.uninstall(iniFile);
        Thread.sleep(1000);
        assertEquals("configs should be empty after ini file uninstall", Collections.emptySet(), cleanedConfigs(configAdmin.listConfigurations(null)));

        // Verify config events
        verify(configListener, times(6)).configurationEvent(eventCaptor.capture());
        Iterator<ConfigurationEvent> configEvents = eventCaptor.getAllValues().iterator();
        {
        		// Initial update
	        	ConfigurationEvent ev = configEvents.next();
	        	assertEquals("org.example", ev.getFactoryPid());
	        	assertTrue(pidOne.equals(ev.getPid()) || pidTwo.equals(ev.getPid()));
	        	assertEquals(ConfigurationEvent.CM_UPDATED, ev.getType());
        }
        {
        		// Initial update
	        	ConfigurationEvent ev = configEvents.next();
	        	assertEquals("org.example", ev.getFactoryPid());
	        	assertTrue(pidOne.equals(ev.getPid()) || pidTwo.equals(ev.getPid()));
	        	assertEquals(ConfigurationEvent.CM_UPDATED, ev.getType());
	    }
        {
        		// Addition of record three
	        	ConfigurationEvent ev = configEvents.next();
	        	assertEquals("org.example", ev.getFactoryPid());
	        	assertEquals(pidThree, ev.getPid());
	        	assertEquals(ConfigurationEvent.CM_UPDATED, ev.getType());
	    }
        {
	    		// Delete
	        	ConfigurationEvent ev = configEvents.next();
	        	assertEquals("org.example", ev.getFactoryPid());
	        	assertEquals(ConfigurationEvent.CM_DELETED, ev.getType());
	    }
        {
	    		// Delete
	        	ConfigurationEvent ev = configEvents.next();
	        	assertEquals("org.example", ev.getFactoryPid());
	        	assertEquals(ConfigurationEvent.CM_DELETED, ev.getType());
	    }
        {
	    		// Delete
	        	ConfigurationEvent ev = configEvents.next();
	        	assertEquals("org.example", ev.getFactoryPid());
	        	assertEquals(ConfigurationEvent.CM_DELETED, ev.getType());
	    }

        verifyNoMoreInteractions(configListener);
    }

    
    @Test
    public void testRemoveFromMultipleEntries() throws Exception {
        Set<Dictionary<String,Object>> expected = new HashSet<>();
        expected.add(new Hashtable<String,Object>(){{
            put("service.factoryPid", "org.example");
            put("a","b");
        }});
        expected.add(new Hashtable<String,Object>(){{
            put("service.factoryPid", "org.example");
            put("b","c");
        }});

        File iniFile = writeConfigFile(""
        		+ "[one]\n"
        		+ "a=b\n"
        		+ "[two]\n"
        		+ "b=c", "org.example.ini");
        assertTrue(installer.canHandle(iniFile));

        // Initial Install -> 2 update events
        installer.install(iniFile);
        Thread.sleep(1000);
        assertEquals(expected, cleanedConfigs(configAdmin.listConfigurations(null)));
        
        // Get the PID of the records in order to check them later
        String pidOne = findFactoryConfig("org.example", "one").getPid();
        String pidTwo = findFactoryConfig("org.example", "two").getPid();

        // Update -> remove 1 record -> 1 delete event
        expected = new HashSet<>();
        expected.add(new Hashtable<String,Object>(){{
            put("a","b");
            put("service.factoryPid", "org.example");
        }});
        writeConfigFile(""
        		+ "[one]\n"
        		+ "a=b", "org.example.ini");
        installer.update(iniFile);
        Thread.sleep(1000);
        assertEquals(expected, cleanedConfigs(configAdmin.listConfigurations(null)));
        
        // Update -> add 1 record -> 1 update event
        expected = new HashSet<>();
        expected.add(new Hashtable<String,Object>(){{
            put("a","b");
            put("service.factoryPid", "org.example");
        }});
        expected.add(new Hashtable<String,Object>(){{
            put("b","c");
            put("service.factoryPid", "org.example");
        }});
        writeConfigFile(""
        		+ "[one]\n"
        		+ "a=b\n"
        		+ "[two]\n"
        		+ "b=c\n", "org.example.ini");
        installer.update(iniFile);
        Thread.sleep(1000);
        assertEquals(expected, cleanedConfigs(configAdmin.listConfigurations(null)));
        String pidTwoAgain = findFactoryConfig("org.example", "two").getPid();

        // Uninstall -> 2 delete events
        installer.uninstall(iniFile);
        Thread.sleep(1000);
        assertEquals("configs should be empty after ini file uninstall", Collections.emptySet(), cleanedConfigs(configAdmin.listConfigurations(null)));

        // Verify config events
        verify(configListener, times(6)).configurationEvent(eventCaptor.capture());
        Iterator<ConfigurationEvent> configEvents = eventCaptor.getAllValues().iterator();
        {
        		// Initial update
	        	ConfigurationEvent ev = configEvents.next();
	        	assertEquals("org.example", ev.getFactoryPid());
	        	assertTrue(pidOne.equals(ev.getPid()) || pidTwo.equals(ev.getPid()));
	        	assertEquals(ConfigurationEvent.CM_UPDATED, ev.getType());
        }
        {
        		// Initial update
	        	ConfigurationEvent ev = configEvents.next();
	        	assertEquals("org.example", ev.getFactoryPid());
	        	assertTrue(pidOne.equals(ev.getPid()) || pidTwo.equals(ev.getPid()));
	        	assertEquals(ConfigurationEvent.CM_UPDATED, ev.getType());
	    }
        {
        		// Deletion of record two
	        	ConfigurationEvent ev = configEvents.next();
	        	assertEquals("org.example", ev.getFactoryPid());
	        	assertEquals(pidTwo, ev.getPid());
	        	assertEquals(ConfigurationEvent.CM_DELETED, ev.getType());
	    }
        {
	    		// Update of record two (second version)
	        	ConfigurationEvent ev = configEvents.next();
	        	assertEquals("org.example", ev.getFactoryPid());
	        	assertTrue(pidTwoAgain.equals(ev.getPid()) || pidTwo.equals(ev.getPid()));
	        	assertEquals(ConfigurationEvent.CM_UPDATED, ev.getType());
        }
        {
	    		// Delete
	        	ConfigurationEvent ev = configEvents.next();
	        	assertEquals("org.example", ev.getFactoryPid());
	        	assertEquals(ConfigurationEvent.CM_DELETED, ev.getType());
	    }
        {
	    		// Delete
	        	ConfigurationEvent ev = configEvents.next();
	        	assertEquals("org.example", ev.getFactoryPid());
	        	assertEquals(ConfigurationEvent.CM_DELETED, ev.getType());
	    }

        verifyNoMoreInteractions(configListener);
    }
}
