package com.hei.util.jythonRemote.api.message;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class ConnectionResponse extends JythonRemoteMessage {

	private static final int MSG_VERSION = 1;

	static final MessageDeserializer DESERIALIZER = new MessageDeserializer(
			MSG_VERSION) {

		@Override
		protected DeserializationResult derserialize(final byte[] bytes,
				final int offset, final int length)
				throws DeserializationException {
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

				final ConnectionResponse message = new ConnectionResponse(
						version, platform);
				final int remainingLen = msg.remaining();
				final byte[] remaining = new byte[remainingLen];
				msg.get(remaining);

				return new DeserializationResult(message, remaining);
			} catch (final BufferUnderflowException e) {
				throw new DeserializationException("Invalid message format", e);
			}
		}
	};

	private final String _version;
	private final String _platform;

	public ConnectionResponse(final String version, final String platform) {
		super(MessageType.ConnectionResponse, MSG_VERSION);
		_version = version;
		_platform = platform;
	}

	public String getVersion() {
		return _version;
	}

	public String getPlatform() {
		return _platform;
	}

	@Override
	protected byte[] serializeContent() {
		final byte[] version = _version.getBytes();
		final byte[] platform = _platform.getBytes();
		final int capacity = 4 + version.length + 4 + platform.length;

		final ByteBuffer msg = ByteBuffer.allocate(capacity);

		msg.putInt(version.length);
		msg.put(version);

		msg.putInt(platform.length);
		msg.put(platform);

		final byte[] bytes = msg.array();
		return bytes;
	}
}
