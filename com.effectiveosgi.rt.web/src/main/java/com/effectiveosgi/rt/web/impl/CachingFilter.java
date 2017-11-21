package com.effectiveosgi.rt.web.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@interface CachingFilterConfig {
	boolean enabled() default true;
}


@Component(
		name = "com.effectiveosgi.rt.webresource.cache",
		property = {
				HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN + "=/" + WebResourceConstants.WEBRESOURCE_NAMESPACE + "/*"
		})
public class CachingFilter implements Filter {

	static final Logger log = LoggerFactory.getLogger(CachingFilter.class);

	private static final Base64.Encoder base64Enc = Base64.getUrlEncoder().withoutPadding();

	private final Duration maxAge = Duration.ofMinutes(1);
	private Path cacheDir;

	private CachingFilterConfig config;

	@Activate
	void activate(CachingFilterConfig config) throws Exception {
		this.config = config;
		this.cacheDir = Files.createTempDirectory("cache-");
		
		if (!config.enabled()) {
			log.warn("Caching filter disabled for /" + WebResourceConstants.WEBRESOURCE_NAMESPACE + "/* namespace -- NOT RECOMMENDED except during development.");
		}
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
		if (!config.enabled()) {
			chain.doFilter(req, resp);
			return;
		}

		final HttpServletRequest request = (HttpServletRequest) req;
		final HttpServletResponse response = (HttpServletResponse) resp;

		final String path = request.getPathInfo();
		final Path cacheFilePath = cacheDir.resolve(hash(path));
		final File cacheFile = cacheFilePath.toFile();
		
		if (cacheFile.exists()) {
			Duration cacheFileAge = Duration.ofMillis(System.currentTimeMillis() - cacheFile.lastModified());
			if (cacheFileAge.compareTo(maxAge) >= 0) {
				log.info("Cache file {} has exceeded maxium age, deleting", cacheFilePath);
				Files.deleteIfExists(cacheFilePath);
			} else {
				log.debug("Serving path {} from cache file {}", path, cacheFilePath);
				serveFile(cacheFile, resp);
				return;
			}
		}
		log.debug("Caching path {} to cache file {}", path, cacheFilePath);
		HttpServletResponseWrapper responseWrapper = new CapturingHttpServletResponse(response, cacheDir, cacheFilePath);
		chain.doFilter(request, responseWrapper);
	}
	
	private void serveFile(File cacheFile, ServletResponse resp) throws IOException {
		try (FileInputStream in = new FileInputStream(cacheFile); OutputStream out = resp.getOutputStream()) {
			byte[] tmp = new byte[1024];
			int bytesRead = in.read(tmp, 0, tmp.length);
			while (bytesRead >= 0) {
				out.write(tmp, 0, bytesRead);
				bytesRead = in.read(tmp, 0, tmp.length);
			}
		}
	}


	@Override
	public void init(FilterConfig config) throws ServletException {
	}

	@Override
	public void destroy() {
	}
	
	private static String hash(String input) throws IOException {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA1");
			digest.update(input.getBytes(StandardCharsets.UTF_8));
			byte[] hashBytes = digest.digest();
			return base64Enc.encodeToString(hashBytes);
		} catch (NoSuchAlgorithmException e) {
			throw new IOException("", e);
		}
	}

}
