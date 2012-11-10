package com.hei.util.jythonRemote.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import com.hei.util.jythonRemote.api.EvaluateResult;
import com.hei.util.jythonRemote.api.JythonEvaluator;
import com.hei.util.jythonRemote.api.message.ConnectionResponse;
import com.hei.util.jythonRemote.api.message.EvaluationRequest;
import com.hei.util.jythonRemote.api.message.EvaluationResponse;
import com.hei.util.jythonRemote.api.message.JythonRemoteMessage.MessageType;
import com.hei.util.jythonRemote.api.message.JythonRemoteMessageReader;
import com.hei.util.jythonRemote.api.message.JythonRemoteMessageWriter;

public class ClientJythonEvaluator implements JythonEvaluator {

	private static final Logger LOG = Logger.getLogger("ClientJythonEvaluator");

	public static Logger getLogger() {
		return LOG;
	}

	private final String ip;
	private final int port;
	private final boolean anonymous;
	private final Socket connection;
	private final JythonRemoteMessageReader msgReader;
	private final JythonRemoteMessageWriter msgWriter;
	private final String platform;
	private final String version;

	public ClientJythonEvaluator(final String ip, final int port, final boolean anonymous) throws UnknownHostException, IOException {
		this.ip = ip;
		this.port = port;
		this.anonymous = anonymous;

		this.connection = createConnection();

		final InputStream inputStream = connection.getInputStream();
		this.msgReader = new JythonRemoteMessageReader(inputStream);

		final OutputStream outputStream = connection.getOutputStream();
		this.msgWriter = new JythonRemoteMessageWriter(outputStream);

		try {
			final ConnectionResponse connectMsg = (ConnectionResponse) this.msgReader.readMessage(MessageType.ConnectionResponse);
			this.platform = connectMsg.getPlatform();
			this.version = connectMsg.getVersion();
		} catch (final IOException e) {
			disconnect();
			throw e;
		}
	}

	private Socket createConnection() throws UnknownHostException, IOException {
		final Socket connection;
		final SocketFactory socketFactory = SSLSocketFactory.getDefault();
		final InetAddress addr;
		if (ip != null) {
			addr = InetAddress.getByName(ip);
		} else {
			addr = InetAddress.getLocalHost();
		}

		connection = socketFactory.createSocket(addr, port);

		if (anonymous) {
			final SSLSocket sslConn = (SSLSocket) connection;
			final String[] supportedCipherSuites = sslConn.getSupportedCipherSuites();
			sslConn.setEnabledCipherSuites(supportedCipherSuites);
		}
		return connection;
	}

	public void disconnect() {
		try {
			connection.close();
		} catch (final IOException ex) {
		}
	}

	@Override
	public String getPlatform() {
		return platform;
	}

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public EvaluateResult evaluate(final String code) {
		final EvaluationRequest evaluationRequest = new EvaluationRequest(code);
		try {
			msgWriter.writeMessage(evaluationRequest);
			final EvaluationResponse evalResponse = (EvaluationResponse) msgReader.readMessage(MessageType.EvaluationResponse);
			final EvaluateResult evaluateResult = evalResponse.toEvaluateResult();
			return evaluateResult;
		} catch (final IOException e) {
			disconnect();
			throw new RuntimeException(e);
		}
	}

	public String getIP() {
		return ip;
	}

	public int getPort() {
		return port;
	}

	public boolean enabledAnonymous() {
		return anonymous;
	}
}
