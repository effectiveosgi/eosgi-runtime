package com.effectiveosgi.rt.config.impl.util;

import java.util.SortedSet;
import java.util.TreeSet;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class SingletonServiceTracker<S,T> extends ServiceTracker<S, T> {
	
	private final SortedSet<ServiceReference<S>> rankedServices = new TreeSet<>();
	private final ServiceTrackerCustomizer<S, T> customizer;

	private ServiceReference<S> currentRef = null;
	private T currentService = null;
	
	public SingletonServiceTracker(BundleContext context, Class<S> clazz, ServiceTrackerCustomizer<S, T> customizer) {
		super(context, clazz, null);
		if (customizer == null) throw new IllegalArgumentException("Customizer may not be null");
		this.customizer = customizer;
	}
	
	@Override
	public T addingService(ServiceReference<S> reference) {
		synchronized (rankedServices) {
			rankedServices.add(reference);
			if (currentRef == null) {
				currentRef = reference;
				currentService = customizer.addingService(currentRef);
			}
		}
		return currentService;
	}
	
	@Override
	public void modifiedService(ServiceReference<S> reference, T service) {
		if (reference.equals(currentRef)) {
			customizer.modifiedService(currentRef, service);
		}
	}
	
	@Override
	public void removedService(ServiceReference<S> reference, T ignore) {
		synchronized (rankedServices) {
			rankedServices.remove(reference);
			if (currentRef != null && currentRef.equals(reference)) {
				ServiceReference<S> oldRef = currentRef;
				T oldService = currentService;
				
				if (rankedServices.isEmpty()) {
					currentRef = null;
					currentService = null;
					customizer.removedService(oldRef, oldService);
				} else {
					currentRef = rankedServices.last();
					currentService = customizer.addingService(currentRef);
					customizer.removedService(oldRef, oldService);
				}
			}
		}
	}
	
}
