package com.hei.util.jythonRemote.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class JythonRemoteClient {

	public static final int DEFAULT_PORT = 5518;
	
	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		startClient();
	}

	private static void startClient() {
		final Socket connection;
		try {
			connection = new Socket(InetAddress.getLocalHost(), DEFAULT_PORT);
		} catch (final IOException e) {
			System.err.println("Cannot reach the server");
			return;
		}

		final BufferedReader in = new BufferedReader(new InputStreamReader(
				System.in));
		new Thread(new Runnable() {

			public void run() {
				try {
					final OutputStream outputStream = connection.getOutputStream();
					String line;
					while ((line = in.readLine()) != null) {
						final String code = line + "\n";
						try {
							outputStream.write(code.getBytes());
							outputStream.flush();
						} catch (final IOException e) {
							System.err.println("Cannot reach the server");
						}
					}

					outputStream.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}, "StdInToSocketIn").start();

		try {
			final InputStream inputStream = connection.getInputStream();
			final InputStreamReader inputStreamReader = new InputStreamReader(
					inputStream);
			final BufferedReader bufferedReader = new BufferedReader(
					inputStreamReader);
			final char[] buffer = new char[1024];
			int read;
			while ((read = bufferedReader.read(buffer)) > 0) {
				final char[] str = new char[read];
				System.arraycopy(buffer, 0, str, 0, read);
				System.out.print(str);
			}

			connection.close();
		} catch (final IOException e) {
			System.out.println("Connection closed");
		}
	}
}