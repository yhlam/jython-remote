package com.hei.util.jythonRemote.api.message;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class EvaluationRequest extends JythonRemoteMessage {

	public static final int MSG_VERSION = 0;

	static final MessageDeserializer DESERIALIZER = new MessageDeserializer(MSG_VERSION) {

		@Override
		protected DeserializationResult derserialize(final byte[] bytes, final int offset, final int length) throws DeserializationException {
			try {
				final ByteBuffer msg = ByteBuffer.wrap(bytes, offset, length);

				final int lineLen = msg.getInt();
				final byte[] lineBytes = new byte[lineLen];
				msg.get(lineBytes);
				final String line = new String(lineBytes);

				final EvaluationRequest message = new EvaluationRequest(line);
				final int remainingLen = msg.remaining();
				final byte[] remaining = new byte[remainingLen];
				msg.get(remaining);

				return new DeserializationResult(message, remaining);
			} catch (final BufferUnderflowException e) {
				throw new DeserializationException("Invalid message format", e);
			}
		}
	};

	private final String line;

	public EvaluationRequest(final String line) {
		super(MessageType.EvaluationRequest, MSG_VERSION);
		this.line = line;
	}

	public String getLine() {
		return line;
	}

	@Override
	protected byte[] serializeContent() {
		final byte[] lineBytes = line.getBytes();
		final int capacity = 4 + lineBytes.length;

		final ByteBuffer msg = ByteBuffer.allocate(capacity);

		msg.putInt(lineBytes.length);
		msg.put(lineBytes);

		final byte[] bytes = msg.array();
		return bytes;
	}
}
