package com.hei.util.jython;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class JythonClient {

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		startClient();
	}

	private static void startClient() {
		try {
			final BufferedReader in = new BufferedReader(new InputStreamReader(
					System.in));
			System.out.print(">>>");
			while (true) {
				final String line = in.readLine() + "\n";

				final Socket connection = new Socket(
						InetAddress.getLocalHost(), JythonServer.JYTHON_PORT);
				final OutputStream outputStream = connection.getOutputStream();
				outputStream.write(line.getBytes());

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
			}
		} catch (final UnknownHostException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}
}
