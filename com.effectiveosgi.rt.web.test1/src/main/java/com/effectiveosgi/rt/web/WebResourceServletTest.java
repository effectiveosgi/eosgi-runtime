package com.effectiveosgi.rt.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class WebResourceServletTest {

	private final BundleContext context = FrameworkUtil.getBundle(WebResourceServletTest.class).getBundleContext();

	@Test
	public void testFindWebResource() throws Exception {
		Bundle targetBundle = findBundle("com.effectiveosgi.rt.web.test1");
		String url = String.format("http://localhost:8080/osgi.enroute.webresource/%s/%s/testing.txt", targetBundle.getSymbolicName(), targetBundle.getVersion());

		try (InputStream stream = new URL(url).openStream()) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			String firstLine = reader.readLine();
			assertEquals("This file served from bundle com.effectiveosgi.rt.web.test1", firstLine);
		}
	}

	private Bundle findBundle(String bsn) {
		List<Bundle> matches = Arrays.stream(context.getBundles())
				.filter(b -> bsn.equals(b.getSymbolicName()))
				.collect(Collectors.toList());
		if (matches.isEmpty())
			fail("No bundles matching bsn " + bsn);
		else if (matches.size() > 1)
			fail("Multiple bundles matching bsn " + bsn);
		
		return matches.get(0);
	}

}
