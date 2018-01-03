package com.effectiveosgi;

import java.util.Dictionary;
import java.util.Hashtable;

import com.effectiveosgi.lib.osgi.LogServiceTracker;
import org.apache.felix.service.command.Converter;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.effectiveosgi.lib.osgi.SingletonServiceTracker;

public class Activator implements BundleActivator {

	private LogServiceTracker logTracker;
	private SingletonServiceTracker<ServiceComponentRuntime, ServiceRegistration<Converter>> tracker;

	@Override
	public void start(BundleContext context) throws Exception {
		logTracker = new LogServiceTracker(context);
		logTracker.open();

		tracker = new SingletonServiceTracker<>(context, ServiceComponentRuntime.class, new ServiceTrackerCustomizer<ServiceComponentRuntime, ServiceRegistration<Converter>>() {
			@Override
			public ServiceRegistration<Converter> addingService(ServiceReference<ServiceComponentRuntime> reference) {
				ServiceComponentRuntime scr = context.getService(reference);
				ComponentCommands commands = new ComponentCommands(context, scr, logTracker);

				Dictionary<String, Object> props = new Hashtable<>();
				props.put("osgi.command.scope", "comp");
				props.put("osgi.command.function", new String[] { "list", "info" });
				props.put(Converter.CONVERTER_CLASSES, new String[] {
						ComponentDescriptionDTO.class.getName(),
						ComponentConfigurationDTO.class.getName()
				});
				return context.registerService(Converter.class, commands, props);
			}
			@Override
			public void modifiedService(ServiceReference<ServiceComponentRuntime> reference, ServiceRegistration<Converter> service) {
			}

			@Override
			public void removedService(ServiceReference<ServiceComponentRuntime> reference, ServiceRegistration<Converter> registration) {
				registration.unregister();
			}
		});
		tracker.open();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		tracker.close();
		logTracker.close();
	}

}
