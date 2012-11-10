package com.hei.util.jythonRemote.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.UnknownHostException;

import jline.ConsoleReader;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import com.hei.util.jythonRemote.api.EvaluateResult;
import com.hei.util.jythonRemote.api.JythonRemoteConstants;
import com.hei.util.jythonRemote.api.JythonRemoteConstants.Prompt;

public class JythonRemoteClient {

	private static final String EXIT_CMD = "\\exit";

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		OptionBuilder.withArgName("host");
		OptionBuilder.hasArg();
		OptionBuilder.withDescription("Server IP (default: localhost)");
		OptionBuilder.withLongOpt("ip");
		final Option hostOpt = OptionBuilder.create('i');

		OptionBuilder.withArgName("port");
		OptionBuilder.hasArg();
		OptionBuilder.withDescription("Server Port (default: " + JythonRemoteConstants.DEFAULT_PORT + ")");
		OptionBuilder.withLongOpt("port");
		final Option portOpt = OptionBuilder.create('p');

		final Option anonOpt = new Option("a", "anonymous", false, "Enable anonymous cipher suites");

		final Option helpOpt = new Option("h", "help", false, "print this message");

		final Options options = new Options();
		options.addOption(hostOpt);
		options.addOption(portOpt);
		options.addOption(anonOpt);
		options.addOption(helpOpt);

		final CommandLineParser parser = new PosixParser();
		try {
			final CommandLine line = parser.parse(options, args);

			if (line.hasOption('h')) {
				final HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("jython-remote-client", options, true);
				return;
			}

			final String host = line.getOptionValue('i', "localhost");

			int port = JythonRemoteConstants.DEFAULT_PORT;
			if (line.hasOption('p')) {
				final String portStr = line.getOptionValue("port");
				try {
					port = Integer.parseInt(portStr);
				} catch (final NumberFormatException e) {
					System.err.println("Invalid port number " + portStr);
					return;
				}
			}

			final boolean anon = line.hasOption('a');

			startClient(host, port, anon);
		} catch (final ParseException e) {
			final HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("jython-remote-client", options, true);
		}
	}

	private static void startClient(final String ip, final int port, final boolean anon) {
		final ConsoleReader reader;
		try {
			final OutputStreamWriter out = new OutputStreamWriter(System.out);
			final InputStream bindings = getBindings();
			reader = new ConsoleReader(System.in, out, bindings);
		} catch (final IOException e) {
			System.err.println("Cannot initalize the console");
			return;
		}

		final ClientJythonEvaluator jython;
		try {
			jython = new ClientJythonEvaluator(ip, port, anon);
		} catch (final UnknownHostException e) {
			System.err.println("Unknown host " + ip);
			return;
		} catch (final IOException e) {
			System.err.println("Cannot reach the server " + ip + ":" + port);
			return;
		}

		final String version = jython.getVersion();
		final String platform = jython.getPlatform();

		System.out.println("Connected to " + ip + ":" + port);
		System.out.println("Jython " + version + " on " + platform);

		readEvalPrintLoop(reader, jython);

		jython.disconnect();
	}

	private static void readEvalPrintLoop(final ConsoleReader reader, final ClientJythonEvaluator jython) {
		final String ip = jython.getIP();
		final int port = jython.getPort();

		Prompt prompt = Prompt.NewLine;
		while (true) {
			String promptStr;
			switch (prompt) {
			case NewLine:
				promptStr = ">>> ";
				break;
			case Continue:
				promptStr = "... ";
				break;
			default:
				System.err.println("Unexpected prompt type: " + prompt);
				return;
			}

			String line;
			try {
				line = reader.readLine(promptStr);
			} catch (final IOException e) {
				System.err.println("Cannot read the console");
				return;
			}

			if (EXIT_CMD.equals(line)) {
				return;
			}

			try {
				final EvaluateResult evalResult = jython.evaluate(line);
				final String result = evalResult.getResult();
				System.out.print(result);
				prompt = evalResult.getPrompt();
			} catch (final RuntimeException e) {
				System.err.println("Cannot reach the server " + ip + ":" + port);
				return;
			}
		}
	}

	/**
	 * Return the JLine bindings file.
	 * 
	 * This handles loading the user's custom keybindings (normally JLine does)
	 * so it can fallback to Jython's (which disable tab completition) when the user's are not available.
	 * 
	 * @return an InputStream of the JLine bindings file.
	 */
	protected static InputStream getBindings() {
		final String userBindings = new File(System.getProperty("user.home"), ".jlinebindings.properties").getAbsolutePath();
		final File bindingsFile = new File(System.getProperty("jline.keybindings", userBindings));

		try {
			if (bindingsFile.isFile()) {
				try {
					return new FileInputStream(bindingsFile);
				} catch (final FileNotFoundException fnfe) {
					// Shouldn't really ever happen
					fnfe.printStackTrace();
				}
			}
		} catch (final SecurityException se) {
			// continue
		}
		return JythonRemoteClient.class.getResourceAsStream("jline-keybindings.properties");
	}
}
