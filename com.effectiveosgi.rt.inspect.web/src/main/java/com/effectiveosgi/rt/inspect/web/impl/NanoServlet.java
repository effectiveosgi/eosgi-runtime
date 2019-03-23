package com.effectiveosgi.rt.inspect.web.impl;

import java.io.IOException;
import java.io.OutputStream;

public interface NanoServlet {
	
	static interface Session {
		OutputStream getOutputStream() throws IOException;
		void putHeader(String name, String value);
	}

	boolean matchPath(String path);

	void doGet(String path, Session session) throws NanoServletException, IOException;

}
