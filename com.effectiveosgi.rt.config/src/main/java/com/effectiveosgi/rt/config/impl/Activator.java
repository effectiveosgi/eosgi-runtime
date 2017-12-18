package com.effectiveosgi.rt.config.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.fileinstall.ArtifactInstaller;
import org.apache.felix.service.command.Converter;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.effectiveosgi.lib.osgi.LogServiceTracker;
import com.effectiveosgi.lib.osgi.SingletonServiceTracker;
import com.effectiveosgi.rt.config.ConfigFileReader;
import com.effectiveosgi.rt.config.impl.json.JsonConfigReader;
import com.effectiveosgi.rt.config.impl.yaml.YamlConfigReader;

public class Activator implements BundleActivator {

	private LogServiceTracker logTracker;
	private ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> configAdminTracker;

	private ServiceRegistration<ConfigFileReader> iniReaderReg;
	private ServiceRegistration<ConfigFileReader> jsonReaderReg;
	private ServiceRegistration<ConfigFileReader> yamlReaderReg;
	
	

	@Override
	public void start(BundleContext context) throws Exception {
		logTracker = new LogServiceTracker(context);
		logTracker.open();
		
		configAdminTracker = new SingletonServiceTracker<>(context, ConfigurationAdmin.class, new ServiceTrackerCustomizer<ConfigurationAdmin, ConfigurationAdmin>() {
			private ConfigurationAdmin configAdmin;
			private HierarchicalConfigInstaller configInstaller;
			private ServiceRegistration<ArtifactInstaller> configInstallerReg;
			private ServiceRegistration<?> commandsReg;
			@Override
			public synchronized ConfigurationAdmin addingService(ServiceReference<ConfigurationAdmin> reference) {
				Dictionary<String, Object> svcProps;

				configAdmin = context.getService(reference);
				
				// Create and register HierarchicalConfigInstaller
				configInstaller = new HierarchicalConfigInstaller(context, configAdmin, logTracker);
				configInstaller.open();

				svcProps = new Hashtable<>();
				svcProps.put("type", "hierarchical");
				svcProps.put("osgi.command.scope", "config");
				svcProps.put("osgi.command.function", new String[] { "install", "update", "uninstall" });
				configInstallerReg = context.registerService(ArtifactInstaller.class, configInstaller, svcProps);
				
				// Create and register ConfigurationCommands
				svcProps = new Hashtable<>();
				svcProps.put("osgi.command.scope", "config");
				svcProps.put("osgi.command.function", new String[] { "list", "info" });
				svcProps.put("osgi.converter.classes", Configuration.class.getName());
				
				try {
					// Test whether the optional import of org.apache.felix.service.command was wired
					context.getBundle().loadClass("org.apache.felix.service.command.Converter");
					commandsReg = context.registerService(Converter.class.getName(), new ConfigurationCommands(configAdmin), svcProps);
				} catch (ClassNotFoundException e) {
				}

				return configAdmin;
			}
			@Override
			public void modifiedService(ServiceReference<ConfigurationAdmin> reference, ConfigurationAdmin service) {
			}
			@Override
			public synchronized void removedService(ServiceReference<ConfigurationAdmin> reference, ConfigurationAdmin configAdmin) {
				if (this.configAdmin == configAdmin) {
					if (commandsReg != null) commandsReg.unregister();
					configInstallerReg.unregister();
					configInstaller.close();
					configAdmin = null;
				}
			}
		});
		configAdminTracker.open();
		
		Dictionary<String, Object> svcProps;

		svcProps = new Hashtable<>();
		svcProps.put(ConfigFileReader.PROP_FILE_PATTERN, IniConfigReader.PATTERN);
		iniReaderReg = context.registerService(ConfigFileReader.class, new IniConfigReader(), svcProps);

		svcProps = new Hashtable<>();
		svcProps.put(ConfigFileReader.PROP_FILE_PATTERN, JsonConfigReader.PATTERN);
		jsonReaderReg = context.registerService(ConfigFileReader.class, new JsonConfigReader(logTracker), svcProps);

		svcProps = new Hashtable<>();
		svcProps.put(ConfigFileReader.PROP_FILE_PATTERN, YamlConfigReader.PATTERN);
		yamlReaderReg = context.registerService(ConfigFileReader.class, new YamlConfigReader(), svcProps);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		yamlReaderReg.unregister();
		jsonReaderReg.unregister();
		iniReaderReg.unregister();
		configAdminTracker.close();
		logTracker.close();
	}

}
