package com.hei.util.jythonRemote.api.message;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class ConnectionResponse extends JythonRemoteMessage {

	private static final int MSG_VERSION = 1;

	static final MessageDeserializer DESERIALIZER = new MessageDeserializer(MSG_VERSION) {

		@Override
		protected DeserializationResult derserialize(final byte[] bytes, final int offset, final int length) throws DeserializationException {
			try {
				final ByteBuffer msg = ByteBuffer.wrap(bytes, offset, length);

				final int versionLen = msg.getInt();
				final byte[] versionBytes = new byte[versionLen];
				msg.get(versionBytes);
				final String version = new String(versionBytes);

				final int platformLen = msg.getInt();
				final byte[] platformBytes = new byte[platformLen];
				msg.get(platformBytes);
				final String platform = new String(platformBytes);

				final ConnectionResponse message = new ConnectionResponse(version, platform);
				final int remainingLen = msg.remaining();
				final byte[] remaining = new byte[remainingLen];
				msg.get(remaining);

				return new DeserializationResult(message, remaining);
			} catch (final BufferUnderflowException e) {
				throw new DeserializationException("Invalid message format", e);
			}
		}
	};

	private final String version;
	private final String platform;

	public ConnectionResponse(final String version, final String platform) {
		super(MessageType.ConnectionResponse, MSG_VERSION);
		this.version = version;
		this.platform = platform;
	}

	public String getVersion() {
		return version;
	}

	public String getPlatform() {
		return platform;
	}

	@Override
	protected byte[] serializeContent() {
		final byte[] versionBytes = version.getBytes();
		final byte[] platformBytes = platform.getBytes();
		final int capacity = 4 + versionBytes.length + 4 + platformBytes.length;

		final ByteBuffer msg = ByteBuffer.allocate(capacity);

		msg.putInt(versionBytes.length);
		msg.put(versionBytes);

		msg.putInt(platformBytes.length);
		msg.put(platformBytes);

		final byte[] bytes = msg.array();
		return bytes;
	}
}
