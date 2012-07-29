package com.hei.util.jythonRemote.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.util.InteractiveConsole;
import org.python.util.InteractiveInterpreter;

import com.hei.util.jythonRemote.api.JythonRemoteUtil;
import com.hei.util.jythonRemote.api.JythonRemoteUtil.Prompt;
import com.hei.util.jythonRemote.api.message.EvaluationResponse;
import com.hei.util.jythonRemote.api.message.EvaluationRequest;
import com.hei.util.jythonRemote.api.message.ConnectionResponse;
import com.hei.util.jythonRemote.api.message.JythonRemoteMessage;
import com.hei.util.jythonRemote.api.message.JythonRemoteMessage.MessageType;
import com.hei.util.jythonRemote.api.message.JythonRemoteMessageReader;

public class JythonRemoteServer {

	private static final String STD_IN = "<stdin>";
	private static JythonRemoteServer SINGLETON = null;

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
		startServer(locals, JythonRemoteUtil.DEFAULT_PORT);
	}

	public synchronized void startServer(final int port) {
		startServer(null, JythonRemoteUtil.DEFAULT_PORT);
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

			public void run() {
				try {
					final OutputStream outputStream = connection.getOutputStream();
					final InputStream inputStream = connection.getInputStream();
					final ByteArrayOutputStream out = new ByteArrayOutputStream();

					final String version = PySystemState.version.toString();
					final String platform = PySystemState.platform.toString();
					final ConnectionResponse connectionResponse = new ConnectionResponse(
							version, platform);
					final byte[] responseBytes = connectionResponse.serialize();
					outputStream.write(responseBytes);
					outputStream.flush();

					final JythonRemoteMessageReader msgReader = new JythonRemoteMessageReader(
							inputStream);

					final StringBuilder codeBuilder = new StringBuilder();
					while (true) {
						try {
							JythonRemoteMessage message;
							MessageType type;
							do {
								message = msgReader.nextMessage();
								type = message.getMsgType();
							} while (type != MessageType.EvaluationRequest);

							final EvaluationRequest cmdMsg = (EvaluationRequest) message;
							final String line = cmdMsg.getLine();

							final boolean more;
							synchronized (_runLock) {
								_jython.setOut(out);
								_jython.setErr(out);

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

							final Prompt prompt = more ? Prompt.Continue
									: Prompt.NewLine;

							final String response = out.toString();
							out.reset();

							final EvaluationResponse commandResponse = new EvaluationResponse(
									response, prompt);
							final byte[] msgBytes = commandResponse.serialize();
							outputStream.write(msgBytes);
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
