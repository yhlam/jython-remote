package com.hei.util.jythonRemote.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

import com.hei.util.jythonRemote.api.EvaluateResult;
import com.hei.util.jythonRemote.api.JythonRemoteConstants.Prompt;
import com.hei.util.jythonRemote.api.message.ConnectionResponse;
import com.hei.util.jythonRemote.api.message.EvaluationRequest;
import com.hei.util.jythonRemote.api.message.EvaluationResponse;
import com.hei.util.jythonRemote.api.message.JythonRemoteMessage;
import com.hei.util.jythonRemote.api.message.JythonRemoteMessage.MessageType;
import com.hei.util.jythonRemote.api.message.JythonRemoteMessageReader;
import com.hei.util.jythonRemote.api.message.JythonRemoteMessageWriter;

class ConnectionHandler extends Thread {
	private final Socket connection;
	private final ServerJythonEvaluator jython;
	private final JythonRemoteMessageReader msgReader;
	private final JythonRemoteMessageWriter msgWriter;

	public ConnectionHandler(final Socket connection, final ServerJythonEvaluator jython) throws IOException {
		super("ConnectionHandler-" + connection.getInetAddress());
		this.connection = connection;
		this.jython = jython;

		final InputStream inputStream = connection.getInputStream();
		this.msgReader = new JythonRemoteMessageReader(inputStream);

		final OutputStream outputStream = connection.getOutputStream();
		this.msgWriter = new JythonRemoteMessageWriter(outputStream);

		this.start();
	}

	@Override
	public void run() {
		final InetAddress clientAddr = connection.getInetAddress();
		JythonRemoteServer.getLogger().info("Connected to " + clientAddr.getHostAddress());

		try {

			final String version = jython.getVersion();
			final String platform = jython.getPlatform();
			final ConnectionResponse connectionResponse = new ConnectionResponse(version, platform);
			msgWriter.writeMessage(connectionResponse);

			final StringBuilder codeBuilder = new StringBuilder();
			while (true) {
				try {
					JythonRemoteMessage message;
					MessageType type;
					do {
						message = msgReader.readMessage();
						type = message.getMsgType();
					} while (type != MessageType.EvaluationRequest);

					final EvaluationRequest cmdMsg = (EvaluationRequest) message;
					final String line = cmdMsg.getLine();
					codeBuilder.append(line).append('\n');

					final String code = codeBuilder.toString();

					final EvaluateResult result = jython.evaluate(code);
					final String response = result.getResult();
					final Prompt prompt = result.getPrompt();

					final EvaluationResponse evalResponese = new EvaluationResponse(response, prompt);
					msgWriter.writeMessage(evalResponese);

					// reset codeBuilder
					if (prompt == Prompt.NewLine) {
						codeBuilder.setLength(0);
					}
				} catch (final IOException e) {
					break;
				}
			}

			msgReader.close();
			msgWriter.close();
			connection.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}
}
