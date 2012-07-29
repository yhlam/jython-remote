package com.hei.util.jythonRemote.api.message;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import com.hei.util.jythonRemote.api.JythonRemoteUtil.Prompt;

public class EvaluationResponse extends JythonRemoteMessage {

	public static final int MSG_VERSION = 1;

	static final MessageDeserializer DESERIALIZER = new MessageDeserializer(
			MSG_VERSION) {

		@Override
		protected DeserializationResult derserialize(final byte[] bytes,
				final int offset, final int length)
				throws DeserializationException {
			try {
				final ByteBuffer msg = ByteBuffer.wrap(bytes, offset, length);

				final int lineLen = msg.getInt();
				final byte[] lineBytes = new byte[lineLen];
				msg.get(lineBytes);
				final String line = new String(lineBytes);

				final int promptOrd = msg.getInt();
				final Prompt[] allPrompt = Prompt.values();
				if (allPrompt.length <= promptOrd) {
					throw new DeserializationException("Invalid prompt type "
							+ promptOrd);
				}

				final Prompt prompt = allPrompt[promptOrd];
				final EvaluationResponse message = new EvaluationResponse(line,
						prompt);

				final int remainingLen = msg.remaining();
				final byte[] remaining = new byte[remainingLen];
				msg.get(remaining);

				return new DeserializationResult(message, remaining);
			} catch (final BufferUnderflowException e) {
				throw new DeserializationException("Invalid message format", e);
			}
		}
	};

	private final String _response;
	private final Prompt _prompt;

	public EvaluationResponse(final String response, final Prompt prompt) {
		super(MessageType.EvaluationResponse, MSG_VERSION);
		_response = response;
		_prompt = prompt;
	}

	public String getResponse() {
		return _response;
	}

	public Prompt getPrompt() {
		return _prompt;
	}

	@Override
	protected byte[] serializeContent() {
		final byte[] line = _response.getBytes();
		final int capacity = 4 + line.length + 4;

		final ByteBuffer msg = ByteBuffer.allocate(capacity);

		msg.putInt(line.length);
		msg.put(line);

		final int prompt = _prompt.ordinal();
		msg.putInt(prompt);

		final byte[] bytes = msg.array();
		return bytes;
	}
}
