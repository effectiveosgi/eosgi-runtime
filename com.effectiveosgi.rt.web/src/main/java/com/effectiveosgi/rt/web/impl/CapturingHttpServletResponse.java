package com.effectiveosgi.rt.web.impl;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Random;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class CapturingHttpServletResponse extends HttpServletResponseWrapper {
	
	private static final Random random = new Random();
	private static final Base64.Encoder base64Enc = Base64.getUrlEncoder().withoutPadding();

	private final HttpServletResponse response;
	private final Path tempPath;
	private final Path finalFilePath;

	CapturingHttpServletResponse(HttpServletResponse response, Path tempDir, Path finalFilePath) {
		super(response);
		this.response = response;
		this.finalFilePath = finalFilePath;

		byte[] tempFileNameBytes = new byte[100];
		random.nextBytes(tempFileNameBytes);
		String tempFileName = base64Enc.encodeToString(tempFileNameBytes);

		this.tempPath = tempDir.resolve(tempFileName + ".tmp");
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		ServletOutputStream original = response.getOutputStream();
		Files.createFile(tempPath);
		FileOutputStream capture = new FileOutputStream(tempPath.toFile());
		return new CapturingServletOutputStream(original, capture);
	}

	private class CapturingServletOutputStream extends ServletOutputStream {
		private ServletOutputStream original;
		private OutputStream capture;
		public CapturingServletOutputStream(ServletOutputStream original, OutputStream capture) {
			this.original = original;
			this.capture = capture;
		}
		@Override
		public boolean isReady() {
			return original.isReady();
		}
		@Override
		public void setWriteListener(WriteListener listener) {
			original.setWriteListener(listener);
		}
		@Override
		public void write(int b) throws IOException {
			original.write(b);
			capture.write(b);
		}
		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			original.write(b, off, len);
			capture.write(b, off, len);
		}
		@Override
		public void close() throws IOException {
			original.close();
			capture.close();
			
			try {
				Files.move(tempPath, finalFilePath, StandardCopyOption.ATOMIC_MOVE);
			} catch (FileAlreadyExistsException e) {
				CachingFilter.log.warn("Failed to atomically move temp file {} to cache file {}... possibly cached by another thread??", tempPath, finalFilePath, e);
				Files.deleteIfExists(tempPath);
			}
		}
		@Override
		public void flush() throws IOException {
			original.flush();
			capture.flush();
		}
	}
}