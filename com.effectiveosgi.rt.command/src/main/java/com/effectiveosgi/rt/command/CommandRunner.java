package com.effectiveosgi.rt.command;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;

class CommandRunner implements Callable<Integer> {

	private final CommandProcessor processor;
	private final String program;
	private final InspectLevel inspectLevel;

	CommandRunner(CommandProcessor processor, InspectLevel inspectLevel, String[] args) {
		this.processor = processor;
		this.inspectLevel = inspectLevel;
		this.program = Arrays.stream(args)
			.map(arg -> {
				if (arg.contains(" ") || arg.contains("\\"))
					arg = "\"" + arg + "\"";
				return arg;
			})
			.collect(Collectors.joining(" "));
	}
	
	CommandRunner(CommandProcessor processor, InspectLevel inspectLevel, URI script) throws IOException {
		this.processor = processor;
		this.inspectLevel = inspectLevel;
	
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (InputStream in = script.toURL().openStream()) {
			final byte[] buf = new byte[1024];
			int read = in.read(buf);
			while (read >= 0) {
				out.write(buf, 0, read);
				read = in.read(buf);
			}
		}
		this.program = out.toString(StandardCharsets.UTF_8.name());
	}

	public Integer call() {
		final List<Thread> threads = new LinkedList<>();
		try (
			PipedInputStream in = new PipedInputStream();
			PipedOutputStream out = new PipedOutputStream();
			PipedOutputStream err = new PipedOutputStream();
			PrintStream printOut = new PrintStream(out)
		) {
			final PipeThread inPipeThread = new PipeThread("stdin", System.in, new PipedOutputStream(in));
			inPipeThread.start();
			threads.add(inPipeThread);

			final PipeThread outPipeThread = new PipeThread("stdout", new PipedInputStream(out), System.out);
			outPipeThread.start();
			threads.add(outPipeThread);

			final PipeThread errPipeThread = new PipeThread("stderr", new PipedInputStream(err), System.err);
			errPipeThread.start();
			threads.add(errPipeThread);

			final CommandSession session = processor.createSession(in, out, err);

			final Object result = session.execute(program);
			if (result != null && inspectLevel != InspectLevel.None) {
				final CharSequence formattedResult = session.format(result, inspectLevel.getConverterLevel());
				printOut.println(formattedResult);
			}

			out.flush();
			err.flush();
			return 0;
		} catch (Exception e) {
			e.printStackTrace();
			return 1;
		}
	}
}