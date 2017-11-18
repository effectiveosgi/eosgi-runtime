package com.effectiveosgi.rt.config.impl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.osgi.service.cm.Configuration;

public class ConfigurationCommandsTest {

	@Test
	public void testCompareConfigs() {
		// factory before non-factory
		assertEquals(1, ConfigurationCommands.compareConfigurations(createMockConfig(null, ""), createMockConfig("", ""))); 
		assertEquals(-1, ConfigurationCommands.compareConfigurations(createMockConfig("", ""), createMockConfig(null, ""))); 
		assertEquals(0, ConfigurationCommands.compareConfigurations(createMockConfig(null, ""), createMockConfig(null, "")));
		
		// compare factory first
		assertEquals(-1, ConfigurationCommands.compareConfigurations(createMockConfig("aaa", "yyy"), createMockConfig("bbb", "xxx"))); 
		assertEquals(1, ConfigurationCommands.compareConfigurations(createMockConfig("bbb", "xxx"), createMockConfig("aaa", "yyy")));
		
		// compare pid second
		assertEquals(-1, ConfigurationCommands.compareConfigurations(createMockConfig("aaa", "xxx"), createMockConfig("aaa", "yyy")));
		assertEquals(1, ConfigurationCommands.compareConfigurations(createMockConfig("aaa", "yyy"), createMockConfig("aaa", "xxx")));
		assertEquals(0, ConfigurationCommands.compareConfigurations(createMockConfig("aaa", "xxx"), createMockConfig("aaa", "xxx")));
	}

	private Configuration createMockConfig(String factoryPid, String pid) {
		Configuration mock = mock(Configuration.class);
		when(mock.getFactoryPid()).thenReturn(factoryPid);
		when(mock.getPid()).thenReturn(pid);
		return mock;
	}

}
