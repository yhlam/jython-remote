package com.hei.util.jython;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.python.util.InteractiveConsole;

public class JythonServer {

	public static final int JYTHON_PORT = 8991;

	private static JythonServer SINGLETON = null;
	private static final String NEXT_PROMPT = ">>> ";
	private static final String CONT_PROMPT = "... ";

	private volatile Thread _serverThread;

	public static JythonServer singleton() {
		if (SINGLETON == null) {
			SINGLETON = new JythonServer();
		}

		return SINGLETON;
	}

	private JythonServer() {
		_serverThread = null;
	}

	public synchronized void startServer() {
		final boolean started = isStarted();
		if (started) {
			return;
		}

		_serverThread = new Thread(new Runnable() {

			@Override
			public void run() {
				startServerImpl();
			}
		}, "JythonServer");
		_serverThread.isDaemon();
		_serverThread.start();
	}

	public boolean isStarted() {
		return _serverThread != null;
	}

	public synchronized void stopServer() {
		if (_serverThread != null) {
			_serverThread.interrupt();
		}
	}

	private void startServerImpl() {
		System.setProperty("python.cachedir", "/home/hei/.jython-cache");
		final InteractiveConsole jython = new InteractiveConsole();

		try {
			final ServerSocket server = new ServerSocket(JYTHON_PORT);
			try {
				while (true) {
					final Socket connection = server.accept();
					final OutputStream outputStream = connection
							.getOutputStream();
					jython.setOut(outputStream);
					jython.setErr(outputStream);

					final InputStream inputStream = connection.getInputStream();
					final InputStreamReader inputStreamReader = new InputStreamReader(
							inputStream);
					final BufferedReader bufferedReader = new BufferedReader(
							inputStreamReader);
					final String line = bufferedReader.readLine();

					final boolean more;
					if (line != null) {
						more = jython.push(line);
					} else {
						more = false;
					}

					final String prompt = more ? CONT_PROMPT : NEXT_PROMPT;
					final byte[] promptBytes = prompt.getBytes();
					outputStream.write(promptBytes);

					inputStream.close();
					outputStream.close();
					connection.close();
				}
			} catch (final IOException e) {
				server.close();
				e.printStackTrace();
			}

		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

}
