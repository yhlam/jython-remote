package com.hei.util.jythonRemote.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import org.python.core.PyObject;
import org.python.util.InteractiveConsole;

import com.hei.util.jythonRemote.api.JythonRemoteConstants;

public class JythonRemoteServer {
	private static final Logger LOG = Logger.getLogger("JythonRemoteServer");

	public static Logger getLogger() {
		return LOG;
	}

	private static JythonRemoteServer SINGLETON = null;

	private final ServerJythonEvaluator jython;
	private volatile Thread serverThread;

	public static JythonRemoteServer singleton() {
		if (SINGLETON == null) {
			SINGLETON = new JythonRemoteServer();
		}

		return SINGLETON;
	}

	private JythonRemoteServer() {
		serverThread = null;
		jython = new ServerJythonEvaluator();
	}

	public synchronized void startServer() {
		startServer(null);
	}

	public synchronized void startServer(final PyObject locals) {
		startServer(locals, JythonRemoteConstants.DEFAULT_PORT);
	}

	public synchronized void startServer(final int port) {
		startServer(null, JythonRemoteConstants.DEFAULT_PORT);
	}

	public synchronized void startServer(final PyObject locals, final int port) {
		final boolean started = isStarted();
		if (started) {
			return;
		}

		if (locals != null) {
			jython.setLocals(locals);
		}

		serverThread = new Thread(new Runnable() {

			@Override
			public void run() {
				startServerImpl(port);
			}
		}, "JythonServer");
		serverThread.setDaemon(true);
		serverThread.start();
	}

	public boolean isStarted() {
		return serverThread != null;
	}

	public synchronized void stopServer() {
		if (serverThread != null) {
			jython.cleanup();
			serverThread.interrupt();
		}
	}

	private void startServerImpl(final int port) {
		final ServerSocketFactory serverSocketFactory = SSLServerSocketFactory.getDefault();

		final ServerSocket server;
		try {
			server = serverSocketFactory.createServerSocket(port);
		} catch (final IOException e) {
			LOG.log(Level.SEVERE, "Failed to create server socket", e);
			return;
		}

		try {
			while (true) {
				final Socket connection = server.accept();

				// Need to re-check the property in case it is set during runtime
				final boolean noKeyStore = System.getProperty("javax.net.ssl.keyStore") == null;
				if (noKeyStore) {
					final SSLSocket sslConn = (SSLSocket) connection;
					final String[] supportedCipherSuites = sslConn.getSupportedCipherSuites();
					sslConn.setEnabledCipherSuites(supportedCipherSuites);
				}

				handleConnection(connection);
			}
		} catch (final IOException e) {
			try {
				server.close();
			} catch (final IOException closeErr) {
				// Ignore exception for close
			}

			LOG.log(Level.SEVERE, "Failed to accept connection", e);
		}

	}

	public void handleConnection(final Socket connection) {
		try {
			new ConnectionHandler(connection, jython);
		} catch (final IOException e) {
			LOG.severe("Connection closed");
		}
	}

	public static void main(final String[] args) {
		final JythonRemoteServer jythonServer = JythonRemoteServer.singleton();
		jythonServer.startServer();
		System.out.println(InteractiveConsole.getDefaultBanner());
		final boolean noKeyStore = System.getProperty("javax.net.ssl.keyStore") == null;
		if (noKeyStore) {
			System.out.println("Warining: No keystore is provided. Anonymous cipher suites are enabled, they are vulnerable to \"man-in-the-middle\" attacks. "
					+ "Please specify the keystore in the javax.net.ssl.keyStore system property");
		}

		System.out.println("Press <enter> to to exit.");
		try {
			System.in.read();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		jythonServer.stopServer();
	}

}
