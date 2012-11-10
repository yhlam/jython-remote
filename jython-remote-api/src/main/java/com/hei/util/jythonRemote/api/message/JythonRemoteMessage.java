package com.hei.util.jythonRemote.api.message;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public abstract class JythonRemoteMessage {

	protected static final byte[] HEADER = "JYRE".getBytes();
	private static final Map<MessageType, MessageDeserializer> DESERIALIZERS;

	static {
		DESERIALIZERS = new HashMap<JythonRemoteMessage.MessageType, JythonRemoteMessage.MessageDeserializer>();
		DESERIALIZERS.put(MessageType.ConnectionResponse, ConnectionResponse.DESERIALIZER);
		DESERIALIZERS.put(MessageType.EvaluationRequest, EvaluationRequest.DESERIALIZER);
		DESERIALIZERS.put(MessageType.EvaluationResponse, EvaluationResponse.DESERIALIZER);
		DESERIALIZERS.put(MessageType.Authentication, AuthMessage.DESERIALIZER);
	}

	public static DeserializationResult deserialize(final byte[] msg) throws DeserializationException {
		return deserialize(msg, 0, msg.length);
	}

	public static DeserializationResult deserialize(final byte[] msg, final int offset, final int length) throws DeserializationException {
		final ByteBuffer buffer = ByteBuffer.wrap(msg, offset, length);

		final byte[] header = new byte[HEADER.length];
		try {
			buffer.get(header);
		} catch (final BufferUnderflowException e) {
			throw new DeserializationException("Not header, not a JythonRemote message", e);
		}

		final boolean isJythonRemoteMessage = Arrays.equals(HEADER, header);
		if (!isJythonRemoteMessage) {
			throw new DeserializationException("Invalid header, not a JythonRemote message");
		}

		final int msgTypeOrd;
		try {
			msgTypeOrd = buffer.getInt();
		} catch (final BufferUnderflowException e) {
			throw new DeserializationException("Missing message type", e);
		}
		final MessageType[] allTypes = MessageType.values();
		if (msgTypeOrd >= allTypes.length) {
			throw new DeserializationException("No message type for ID=" + msgTypeOrd);
		}

		final MessageType messageType = allTypes[msgTypeOrd];

		final MessageDeserializer deserializer = DESERIALIZERS.get(messageType);
		if (deserializer == null) {
			throw new DeserializationException("No deserializer for " + messageType);
		}

		final int targetVersion = deserializer.getMsgVersion();
		final int version;
		try {
			version = buffer.getInt();
		} catch (final BufferUnderflowException e) {
			throw new DeserializationException("Missing message version", e);
		}

		if (targetVersion != version) {
			throw new DeserializationException("Incompatible version " + version + " of " + messageType);
		}

		final int headerOffset = HEADER.length + 8;
		final int contentLen = length - headerOffset;
		final DeserializationResult result = deserializer.derserialize(msg, headerOffset, contentLen);
		return result;
	}

	protected final MessageType msgType;
	protected final int msgVersion;

	protected JythonRemoteMessage(final MessageType msgType, final int msgVersion) {
		this.msgType = msgType;
		this.msgVersion = msgVersion;
	}

	public byte[] serialize() {
		final byte[] content = serializeContent();
		final ByteBuffer msg = ByteBuffer.allocate(HEADER.length + 8 + content.length);

		msg.put(HEADER);
		final int msgTypeId = msgType.ordinal();
		msg.putInt(msgTypeId);
		msg.putInt(msgVersion);
		msg.put(content);

		final byte[] bytes = msg.array();
		return bytes;
	}

	protected abstract byte[] serializeContent();

	public int getMsgVersion() {
		return msgVersion;
	}

	public MessageType getMsgType() {
		return msgType;
	}

	/**
	 * Do not reorder
	 */
	public static enum MessageType {
		ConnectionResponse, EvaluationRequest, EvaluationResponse, Authentication
	}

	public static class DeserializationException extends IOException {
		private static final long serialVersionUID = 1L;

		public DeserializationException() {
			super();
		}

		public DeserializationException(final String msg) {
			super(msg);
		}

		public DeserializationException(final Throwable throwable) {
			super(throwable);
		}

		public DeserializationException(final String msg, final Throwable throwable) {
			super(msg, throwable);
		}
	}

	protected static abstract class MessageDeserializer {
		protected final int msgVersion;

		protected MessageDeserializer(final int msgVersion) {
			this.msgVersion = msgVersion;
		}

		protected abstract DeserializationResult derserialize(byte[] bytes, int offset, int length) throws DeserializationException;

		public int getMsgVersion() {
			return msgVersion;
		}
	}

	public static class DeserializationResult {
		private final JythonRemoteMessage message;
		private final byte[] remaining;

		public DeserializationResult(final JythonRemoteMessage message, final byte[] remaining) {
			this.message = message;
			this.remaining = remaining;
		}

		public JythonRemoteMessage getMessage() {
			return message;
		}

		public byte[] getRemaining() {
			return remaining;
		}
	}
}
