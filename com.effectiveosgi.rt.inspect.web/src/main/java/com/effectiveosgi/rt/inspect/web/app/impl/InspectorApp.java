package com.effectiveosgi.rt.inspect.web.app.impl;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

@Component(
		service = InspectorApp.class,
		property = {
				HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN + "=/inspector/*",
				HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX + "=/web"
		})
public class InspectorApp {
}
