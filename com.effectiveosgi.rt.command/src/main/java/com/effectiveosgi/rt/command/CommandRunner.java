package com.effectiveosgi.rt.command;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Converter;

class CommandRunner implements Callable<Integer> {

	private final CommandProcessor processor;
	private final String[] args;

	CommandRunner(CommandProcessor processor, String[] args) {
		this.processor = processor;
		this.args = args;
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
			final String commandLine = Arrays.stream(args)
					.map(arg -> {
						if (arg.contains(" "))
							arg = "\"" + arg + "\"";
						return arg;
					})
					.collect(Collectors.joining(" "));

			final Object result = session.execute(commandLine);
			if (result != null) {
				final CharSequence formattedResult = session.format(result, Converter.INSPECT);
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