package com.hei.util.jythonRemote.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
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

import com.hei.util.jythonRemote.api.JythonRemoteUtil;
import com.hei.util.jythonRemote.api.JythonRemoteUtil.Prompt;
import com.hei.util.jythonRemote.api.message.EvaluationResponse;
import com.hei.util.jythonRemote.api.message.EvaluationRequest;
import com.hei.util.jythonRemote.api.message.ConnectionResponse;
import com.hei.util.jythonRemote.api.message.JythonRemoteMessage;
import com.hei.util.jythonRemote.api.message.JythonRemoteMessage.MessageType;
import com.hei.util.jythonRemote.api.message.JythonRemoteMessageReader;

public class JythonRemoteClient {

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
		OptionBuilder.withDescription("Server Port (default: "
				+ JythonRemoteUtil.DEFAULT_PORT + ")");
		OptionBuilder.withLongOpt("port");
		final Option portOpt = OptionBuilder.create('p');

		final Option helpOpt = new Option("h", "help", false,
				"print this message");

		final Options options = new Options();
		options.addOption(hostOpt);
		options.addOption(portOpt);
		options.addOption(helpOpt);

		final CommandLineParser parser = new PosixParser();
		try {
			final CommandLine line = parser.parse(options, args);

			if (line.hasOption('h')) {
				final HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("jython-remote-client", options, true);
				return;
			}

			final String host = line.getOptionValue('i');

			int port = JythonRemoteUtil.DEFAULT_PORT;
			if (line.hasOption('p')) {
				final String portStr = line.getOptionValue("port");
				try {
					port = Integer.parseInt(portStr);
				} catch (final NumberFormatException e) {
					System.err.println("Invalid port number " + portStr);
					return;
				}
			}

			startClient(host, port);
		} catch (final ParseException e) {
			final HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("jython-remote-client", options, true);
		}
	}

	private static void startClient(final String ip, final int port) {
		final String ipStr = ip == null ? "localhost" : ip;

		final Socket connection;
		try {
			final InetAddress addr;
			if (ip != null) {
				try {
					addr = InetAddress.getByName(ip);
				} catch (final UnknownHostException e) {
					System.err.println("Unknown host " + ip);
					return;
				}
			} else {
				addr = InetAddress.getLocalHost();
			}

			connection = new Socket(addr, port);
		} catch (final IOException e) {
			System.err.println("Cannot reach the server " + ipStr + ":" + port);
			return;
		}

		final ConsoleReader reader;
		try {
			final OutputStreamWriter out = new OutputStreamWriter(System.out);
			final InputStream bindings = getBindings();
			reader = new ConsoleReader(System.in, out, bindings);
		} catch (final IOException e) {
			System.err.println("Cannot start the console");
			try {
				connection.close();
			} catch (final IOException ex) {
			}
			return;
		}

		final InputStream inputStream;
		final OutputStream outputStream;
		try {
			inputStream = connection.getInputStream();
			outputStream = connection.getOutputStream();
		} catch (final IOException e1) {
			System.err.println("Cannot reach the server " + ipStr + ":" + port);
			try {
				connection.close();
			} catch (final IOException e) {
			}
			return;
		}

		final JythonRemoteMessageReader msgReader = new JythonRemoteMessageReader(
				inputStream);

		ConnectionResponse connMsg;
		try {
			while (true) {
				final JythonRemoteMessage message = msgReader.nextMessage();
				final MessageType msgType = message.getMsgType();
				if (msgType != MessageType.ConnectionResponse) {
					continue;
				}

				connMsg = (ConnectionResponse) message;
				break;
			}
		} catch (final IOException e) {
			System.err.println("Cannot reach the server " + ipStr + ":" + port);
			try {
				connection.close();
			} catch (final IOException ex) {
			}
			return;
		}

		final String version = connMsg.getVersion();
		final String platform = connMsg.getPlatform();

		System.out.println("Connected to " + ipStr + ":" + port);
		System.out.println("Jython " + version + " on " + platform);

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
				try {
					connection.close();
				} catch (final IOException e) {
				}
				return;
			}

			final String line;
			try {
				line = reader.readLine(promptStr);
			} catch (final IOException e) {
				System.err.println("Cannot read the console");
				try {
					connection.close();
				} catch (final IOException ex) {
				}
				return;
			}

			final EvaluationRequest evaluationRequest = new EvaluationRequest(line);
			final byte[] submitBytes = evaluationRequest.serialize();
			try {
				outputStream.write(submitBytes);
				outputStream.flush();
			} catch (final IOException e) {
				System.err.println("Cannot reach the server " + ipStr + ":"
						+ port);
				try {
					connection.close();
				} catch (final IOException ex) {
				}
				return;
			}

			try {
				JythonRemoteMessage message;
				MessageType msgType;
				do {
					message = msgReader.nextMessage();
					msgType = message.getMsgType();
				} while (msgType != MessageType.EvaluationResponse);

				final EvaluationResponse castedMsg = (EvaluationResponse) message;
				final String response = castedMsg.getResponse();
				System.out.print(response);
				prompt = castedMsg.getPrompt();
			} catch (final IOException e) {
				System.err.println("Cannot reach the server " + ipStr + ":"
						+ port);
				try {
					connection.close();
				} catch (final IOException ex) {
				}
				return;
			}
		}
	}

	/**
	 * Return the JLine bindings file.
	 * 
	 * This handles loading the user's custom keybindings (normally JLine does)
	 * so it can fallback to Jython's (which disable tab completition) when the
	 * user's are not available.
	 * 
	 * @return an InputStream of the JLine bindings file.
	 */
	protected static InputStream getBindings() {
		final String userBindings = new File(System.getProperty("user.home"),
				".jlinebindings.properties").getAbsolutePath();
		final File bindingsFile = new File(System.getProperty(
				"jline.keybindings", userBindings));

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
		return JythonRemoteClient.class
				.getResourceAsStream("jline-keybindings.properties");
	}
}
