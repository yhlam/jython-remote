package com.hei.util.jythonRemote.api.message;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import com.hei.util.jythonRemote.api.message.JythonRemoteMessage.DeserializationException;
import com.hei.util.jythonRemote.api.message.JythonRemoteMessage.DeserializationResult;
import com.hei.util.jythonRemote.api.message.JythonRemoteMessage.MessageType;

public class JythonRemoteMessageReader {
	private static final Logger LOG = Logger.getLogger("JythonRemoteMessageReader");

	private final InputStream in;
	private volatile byte[] buffer;
	private volatile int offset;

	public JythonRemoteMessageReader(final InputStream in) {
		this(in, 1024);
	}

	public JythonRemoteMessageReader(final InputStream in, final int bufferSize) {
		this.in = in;
		this.buffer = new byte[bufferSize];
		this.offset = 0;
	}

	public JythonRemoteMessage readMessage() throws IOException {
		int read;
		while ((read = in.read(buffer, offset, buffer.length - offset)) > 0) {
			try {
				final DeserializationResult result = JythonRemoteMessage.deserialize(buffer, 0, offset + read);
				final byte[] remaining = result.getRemaining();
				System.arraycopy(remaining, 0, buffer, 0, remaining.length);
				offset = remaining.length;
				final JythonRemoteMessage message = result.getMessage();
				return message;
			} catch (final DeserializationException e) {
				offset += read;
				if (offset == buffer.length) {
					expandBuffer();
				}
			}
		}
		throw new IOException("Input stream closed");
	}

	public JythonRemoteMessage readMessage(final MessageType type) throws IOException {
		JythonRemoteMessage message;

		while ((message = readMessage()).getMsgType() != type) {
			LOG.warning("Ignored " + message + ". Because the message type is not  " + type);
		}

		return message;
	}

	private void expandBuffer() {
		final byte[] newBuffer = new byte[buffer.length * 2];
		System.arraycopy(buffer, 0, newBuffer, 0, offset);
		buffer = newBuffer;
	}

	public void close() throws IOException {
		in.close();
	}
}
