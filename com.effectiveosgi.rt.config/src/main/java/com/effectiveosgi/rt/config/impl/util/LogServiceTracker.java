package com.effectiveosgi.rt.config.impl.util;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

public class LogServiceTracker extends ServiceTracker<LogService, LogService> implements LogService {

	public LogServiceTracker(BundleContext context) {
		super(context, LogService.class, null);
	}

	@Override
	public void log(int level, String message) {
		log(null, level, message, null);
	}

	@Override
	public void log(int level, String message, Throwable exception) {
		log (null, level, message, exception);
	}

	@Override
	public void log(@SuppressWarnings("rawtypes") ServiceReference sr, int level, String message) {
		log(sr, level, message, null);
	}

	@Override
	public void log(@SuppressWarnings("rawtypes") ServiceReference sr, int level, String message, Throwable exception) {
		LogService log = getService();
		if (log != null) log.log(sr, level, message, exception);
	}

}
