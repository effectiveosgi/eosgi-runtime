package com.effectiveosgi.rt.inspect.servlet.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.log.LogService;

import com.effectiveosgi.rt.inspect.web.impl.BundleInspectorNanoServlet;
import com.effectiveosgi.rt.inspect.web.impl.ConfigurationNanoServlet;
import com.effectiveosgi.rt.inspect.web.impl.LogNanoServlet;
import com.effectiveosgi.rt.inspect.web.impl.NanoServlet;
import com.effectiveosgi.rt.inspect.web.impl.NanoServletException;
import com.effectiveosgi.rt.inspect.web.impl.ResourcesNanoServlet;
import com.effectiveosgi.rt.inspect.web.impl.SCRInspectorNanoServlet;
import com.effectiveosgi.rt.inspect.web.impl.ServiceInspectorNanoServlet;

@Component(
		name = InspectorAppServlet.NAME,
		configurationPid = InspectorAppServlet.PID,
		property =  {
				HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN + "=/*",
				HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME + "=" + InspectorAppServlet.NAME,
				Constants.SERVICE_VENDOR + "=Effective OSGi RT"
		})
public class InspectorAppServlet extends HttpServlet implements Servlet {

	static final String PID = "com.effectiveosgi.rt.inspect.servlet";
	static final String NAME = "Effective OSGi RT Inspector Web";

	private static final long serialVersionUID = 1L;

	private final AtomicReference<ServiceComponentRuntime> scrRef = new AtomicReference<>(null);

	private SCRInspectorNanoServlet scrApiServlet;
	private NanoServlet[] servlets;
	
	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
	LogService log;

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
	void setSCR(ServiceComponentRuntime scr) {
		this.scrRef.set(scr);
	}
	void unsetSCR(ServiceComponentRuntime scr) {
		if (this.scrRef.compareAndSet(scr, null)) {
			if (scrApiServlet != null)
				scrApiServlet.setServiceComponentRuntime(scr);
		}
	}

	@Activate
	void activate(BundleContext context) {
		scrApiServlet = new SCRInspectorNanoServlet(context);
		scrApiServlet.setServiceComponentRuntime(scrRef.get());

		servlets = new NanoServlet[] {
				new BundleInspectorNanoServlet(context),
				new ServiceInspectorNanoServlet(context),
				new ConfigurationNanoServlet(context),
				new LogNanoServlet(context),
				scrApiServlet,
				new ResourcesNanoServlet(FrameworkUtil.getBundle(ResourcesNanoServlet.class))
		};
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		final String path = req.getPathInfo();
		final NanoServlet responder = Arrays.stream(servlets)
			.filter(s -> s.matchPath(path))
			.findFirst().orElse(null);
		if (responder == null) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Not Found");
			return;
		}

		resp.setHeader("Access-Control-Allow-Origin", "*");
		final NanoServlet.Session session = new NanoServlet.Session() {
			@Override
			public void putHeader(String name, String value) {
				resp.setHeader(name, value);
			}
			@Override
			public OutputStream getOutputStream() throws IOException {
				return resp.getOutputStream();
			}
		};

		try {
			responder.doGet(path, session);
		} catch (NanoServletException e) {
			resp.sendError(e.getCode(), e.getMessage());
		} catch (Exception e) {
			if (log != null)
				log.log(LogService.LOG_ERROR, "Error processing servlet response", e);
			resp.sendError(500, e.getMessage());
		}
	}

}
