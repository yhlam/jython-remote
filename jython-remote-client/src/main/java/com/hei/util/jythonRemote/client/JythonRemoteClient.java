package com.hei.util.jythonRemote.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

public class JythonRemoteClient {

	public static final int DEFAULT_PORT = 5518;

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
		OptionBuilder.withDescription("Server Port (default: 5518)");
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

			int port = DEFAULT_PORT;
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
			final String ipStr = ip == null ? "localhost" : ip;
			System.err.println("Cannot reach the server " + ipStr + ":" + port);
			return;
		}

		final BufferedReader in = new BufferedReader(new InputStreamReader(
				System.in));
		new Thread(new Runnable() {

			public void run() {
				try {
					final OutputStream outputStream = connection.getOutputStream();
					String line;
					while ((line = in.readLine()) != null) {
						final String code = line + "\n";
						try {
							outputStream.write(code.getBytes());
							outputStream.flush();
						} catch (final IOException e) {
							System.err.println("Cannot reach the server");
						}
					}

					outputStream.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}, "StdInToSocketIn").start();

		try {
			final InputStream inputStream = connection.getInputStream();
			final InputStreamReader inputStreamReader = new InputStreamReader(
					inputStream);
			final BufferedReader bufferedReader = new BufferedReader(
					inputStreamReader);
			final char[] buffer = new char[1024];
			int read;
			while ((read = bufferedReader.read(buffer)) > 0) {
				final char[] str = new char[read];
				System.arraycopy(buffer, 0, str, 0, read);
				System.out.print(str);
			}

			connection.close();
		} catch (final IOException e) {
			System.out.println("Connection closed");
		}
	}
}
