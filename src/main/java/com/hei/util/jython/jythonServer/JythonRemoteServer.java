package com.hei.util.jython.jythonServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.python.core.PyObject;
import org.python.util.InteractiveConsole;
import org.python.util.InteractiveInterpreter;

public class JythonRemoteServer {

	public static final int DEFAULT_PORT = 5518;

	private static JythonRemoteServer SINGLETON = null;

	private static final String STD_IN = "<stdin>";
	private static final String NEXT_PROMPT = ">>> ";
	private static final String CONT_PROMPT = "... ";
	private static final String WELCOME = InteractiveConsole.getDefaultBanner()
			+ "\n" + NEXT_PROMPT;

	private final InteractiveInterpreter _jython;
	private volatile Thread _serverThread;
	private final Object _runLock;

	public static JythonRemoteServer singleton() {
		if (SINGLETON == null) {
			SINGLETON = new JythonRemoteServer();
		}

		return SINGLETON;
	}

	private JythonRemoteServer() {
		_serverThread = null;
		_runLock = new Object();

		_jython = new InteractiveInterpreter();
		_jython.exec("0"); // Dummy exec in order to speed up response on first command
	}

	public synchronized void startServer() {
		startServer(null);
	}

	public synchronized void startServer(final PyObject locals) {
		startServer(locals, DEFAULT_PORT);
	}

	public synchronized void startServer(final int port) {
		startServer(null, DEFAULT_PORT);
	}

	public synchronized void startServer(final PyObject locals, final int port) {
		final boolean started = isStarted();
		if (started) {
			return;
		}

		if (locals != null) {
			_jython.setLocals(locals);
		}

		_serverThread = new Thread(new Runnable() {

			@Override
			public void run() {
				startServerImpl(port);
			}
		}, "JythonServer");
		_serverThread.setDaemon(true);
		_serverThread.start();
	}

	public boolean isStarted() {
		return _serverThread != null;
	}

	public synchronized void stopServer() {
		if (_serverThread != null) {
			_jython.cleanup();
			_serverThread.interrupt();
		}
	}

	private void startServerImpl(final int port) {

		try {
			final ServerSocket server = new ServerSocket(port);
			try {
				while (true) {
					final Socket connection = server.accept();
					handleConnection(connection);
				}
			} catch (final IOException e) {
				server.close();
				e.printStackTrace();
			}

		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public void handleConnection(final Socket connection) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					final OutputStream outputStream = connection.getOutputStream();
					final InputStream inputStream = connection.getInputStream();
					outputStream.write(WELCOME.getBytes());

					final StringBuilder codeBuilder = new StringBuilder();
					while (true) {
						try {
							final InputStreamReader inputStreamReader = new InputStreamReader(
									inputStream);
							final BufferedReader bufferedReader = new BufferedReader(
									inputStreamReader);
							final String line = bufferedReader.readLine();

							final boolean more;
							if (line != null) {
								synchronized (_runLock) {
									_jython.setOut(outputStream);
									_jython.setErr(outputStream);

									if (codeBuilder.length() > 0) {
										codeBuilder.append("\n");
									}
									codeBuilder.append(line);

									final String code = codeBuilder.toString();
									more = _jython.runsource(code, STD_IN);

									if (!more) {
										codeBuilder.setLength(0);
									}
								}
							} else {
								more = false;
							}

							final String prompt = more ? CONT_PROMPT
									: NEXT_PROMPT;
							final byte[] promptBytes = prompt.getBytes();
							outputStream.write(promptBytes);
							outputStream.flush();
						} catch (final IOException e) {
							break;
						}
					}

					inputStream.close();
					outputStream.close();
					connection.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}, "JythonConnection").start();
	}

	public static void main(final String[] args) {
		final JythonRemoteServer jythonServer = JythonRemoteServer.singleton();
		jythonServer.startServer();
		System.out.println(InteractiveConsole.getDefaultBanner());
		System.out.println("Please any key to end...");
		try {
			System.in.read();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		jythonServer.stopServer();
	}

}
