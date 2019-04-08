package com.effectiveosgi.rt.command;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

class PipeThread extends Thread {

	private final String name;
	private final Reader reader;
	private final Writer writer;
	
	PipeThread(String name, InputStream in, OutputStream out) {
		this(name, new InputStreamReader(in), new OutputStreamWriter(out));
	}

	PipeThread(String name, Reader reader, Writer writer) {
		super("Printer Thread: " + name);
		this.name = name;
		this.reader = reader;
		this.writer = writer;
	}

	@Override
	public void run() {
		try {
			char[] buf = new char[1024];
			int bytesRead;
			while ((bytesRead = reader.read(buf, 0, buf.length)) >= 0) {
				writer.write(buf, 0, bytesRead);
			}
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
