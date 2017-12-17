package com.effectiveosgi.rt.config.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.fileinstall.ArtifactInstaller;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

class ConfigAdminToHierarchicalConfigInstallerRegistrationTracker extends ServiceTracker<ConfigurationAdmin, ConfigAdminToHierarchicalConfigInstallerRegistrationTracker.Registration> {

	private final LogService log;
	
	static class Registration {
		HierarchicalConfigInstaller configInstaller;
		ServiceRegistration<ArtifactInstaller> artifactInstallerRegistration;
	}

	ConfigAdminToHierarchicalConfigInstallerRegistrationTracker(BundleContext context, LogService log) {
		super(context, ConfigurationAdmin.class, null);
		this.log = log;
	}

	@Override
	public Registration addingService(ServiceReference<ConfigurationAdmin> reference) {
		ConfigurationAdmin configAdmin = context.getService(reference);

		Registration registration = new Registration();
		registration.configInstaller = new HierarchicalConfigInstaller(context, configAdmin, log);
		registration.configInstaller.open();

		Dictionary<String, Object> svcProps = new Hashtable<>();
		svcProps.put("type", "hierarchical");
		svcProps.put("osgi.command.scope", "config");
		svcProps.put("osgi.command.function", new String[] { "install", "update", "uninstall" });

		registration.artifactInstallerRegistration = context.registerService(ArtifactInstaller.class, registration.configInstaller, svcProps);
		return registration;
	}

	@Override
	public void removedService(ServiceReference<ConfigurationAdmin> reference, Registration registration) {
		try {
			registration.artifactInstallerRegistration.unregister();
		} catch (Exception e) {
			log.log(reference, LogService.LOG_ERROR, "Unexpected error during service unregistration", e);
		}
		registration.configInstaller.close();
		context.ungetService(reference);
	}

}
