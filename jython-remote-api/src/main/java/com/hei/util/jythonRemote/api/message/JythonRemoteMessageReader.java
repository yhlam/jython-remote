package com.hei.util.jythonRemote.api.message;

import java.io.IOException;
import java.io.InputStream;

import com.hei.util.jythonRemote.api.message.JythonRemoteMessage.DeserializationException;
import com.hei.util.jythonRemote.api.message.JythonRemoteMessage.DeserializationResult;

public class JythonRemoteMessageReader {
	private final InputStream _in;
	private volatile byte[] _buffer;
	private volatile int _offset;

	public JythonRemoteMessageReader(final InputStream in) {
		this(in, 1024);
	}

	public JythonRemoteMessageReader(final InputStream in, final int bufferSize) {
		_in = in;
		_buffer = new byte[bufferSize];
		_offset = 0;
	}

	public JythonRemoteMessage nextMessage() throws IOException {
		int read;
		while ((read = _in.read(_buffer, _offset, _buffer.length - _offset)) > 0) {
			try {
				final DeserializationResult result = JythonRemoteMessage
						.deserialize(_buffer, 0, _offset + read);
				final byte[] remaining = result.getRemaining();
				System.arraycopy(remaining, 0, _buffer, 0, remaining.length);
				_offset = remaining.length;
				final JythonRemoteMessage message = result.getMessage();
				return message;
			} catch (final DeserializationException e) {
				_offset += read;
				if (_offset >= _buffer.length) {
					final byte[] newBuffer = new byte[_buffer.length * 2];
					System.arraycopy(_buffer, 0, newBuffer, 0, _offset);
					_buffer = newBuffer;
				}
			}
		}
		throw new IOException("Input stream closed");
	}
}
