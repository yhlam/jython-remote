package com.hei.util.jython;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class JythonServer {

	public static final int JYTHON_PORT = 8991;

	public static void main(final String[] args) {
		startServer();
	}

	private static void startServer() {
		final Jython jython = new Jython();

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					jython.interact(null, null);
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		}, "Jython Console").start();

		try {
			final ServerSocket server = new ServerSocket(JYTHON_PORT);
			try {
				while (true) {
					final Socket connection = server.accept();
					final OutputStream outputStream = connection
							.getOutputStream();
					jython.setOut(outputStream);

					jython.prepareRun();

					final InputStream inputStream = connection.getInputStream();
					jython.feedInput(inputStream);

					jython.waitIfRunning();

					connection.close();
				}
			} catch (final IOException e) {
				server.close();
				e.printStackTrace();
			}

		} // end try
		catch (final IOException e) {
			e.printStackTrace();
		}
	}

}
