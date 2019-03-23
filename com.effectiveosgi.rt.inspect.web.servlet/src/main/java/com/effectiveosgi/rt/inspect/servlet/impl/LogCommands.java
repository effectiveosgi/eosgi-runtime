package com.effectiveosgi.rt.inspect.servlet.impl;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogService;

@Component(
		service = Object.class,
		immediate = true,
		property = {
			"osgi.command.scope=log",
			"osgi.command.function=debug",
			"osgi.command.function=info",
			"osgi.command.function=warn",
			"osgi.command.function=error"
		})
public class LogCommands {

	@Reference
	LogService log;
	
	public void debug(String message, String exception) {
		log.log(LogService.LOG_DEBUG, message, new Exception(exception));
	}
	public void debug(String message) {
		log.log(LogService.LOG_DEBUG, message);
	}

	public void warn(String message, String exception) {
		log.log(LogService.LOG_WARNING, message, new Exception(exception));
	}
	public void warn(String message) {
		log.log(LogService.LOG_WARNING, message);
	}

	public void error(String message, String exception) {
		log.log(LogService.LOG_ERROR, message, new Exception(exception));
	}
	public void error(String message) {
		log.log(LogService.LOG_ERROR, message);
	}
}
