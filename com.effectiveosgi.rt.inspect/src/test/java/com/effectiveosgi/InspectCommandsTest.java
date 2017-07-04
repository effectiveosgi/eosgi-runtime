package com.effectiveosgi;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.osgi.service.cm.Configuration;

public class InspectCommandsTest {

    @Test
    public void testRepeats() {
        assertEquals("----------", ComponentCommands.repeat(10, '-'));
        assertEquals("---", ComponentCommands.repeatForDigits(999, '-'));
        assertEquals("----", ComponentCommands.repeatForDigits(1000, '-'));
    }
    
    @Test
    public void testCompareConfigs() {
    	// factory before non-factory
    	assertEquals(1, ComponentCommands.compareConfigurations(createMockConfig(null, ""), createMockConfig("", ""))); 
    	assertEquals(-1, ComponentCommands.compareConfigurations(createMockConfig("", ""), createMockConfig(null, ""))); 
    	assertEquals(0, ComponentCommands.compareConfigurations(createMockConfig(null, ""), createMockConfig(null, "")));
    	
    	// compare factory first
    	assertEquals(-1, ComponentCommands.compareConfigurations(createMockConfig("aaa", "yyy"), createMockConfig("bbb", "xxx"))); 
    	assertEquals(1, ComponentCommands.compareConfigurations(createMockConfig("bbb", "xxx"), createMockConfig("aaa", "yyy")));
    	
    	// compare pid second
    	assertEquals(-1, ComponentCommands.compareConfigurations(createMockConfig("aaa", "xxx"), createMockConfig("aaa", "yyy")));
    	assertEquals(1, ComponentCommands.compareConfigurations(createMockConfig("aaa", "yyy"), createMockConfig("aaa", "xxx")));
    	assertEquals(0, ComponentCommands.compareConfigurations(createMockConfig("aaa", "xxx"), createMockConfig("aaa", "xxx")));
    }

	private Configuration createMockConfig(String factoryPid, String pid) {
		Configuration mock = mock(Configuration.class);
		when(mock.getFactoryPid()).thenReturn(factoryPid);
		when(mock.getPid()).thenReturn(pid);
		return mock;
	}

}
