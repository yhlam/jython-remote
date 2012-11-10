package com.hei.util.jythonRemote.api.message;

import java.io.IOException;
import java.io.OutputStream;

public class JythonRemoteMessageWriter {
	private final OutputStream out;

	public JythonRemoteMessageWriter(final OutputStream out) {
		this.out = out;
	}

	public void writeMessage(final JythonRemoteMessage msg) throws IOException {
		final byte[] bytes = msg.serialize();
		out.write(bytes);
		out.flush();
	}

	public void close() throws IOException {
		out.close();
	}
}
