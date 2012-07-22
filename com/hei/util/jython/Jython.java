package com.hei.util.jython;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.python.core.Py;
import org.python.core.PyFile;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.util.InteractiveConsole;

public class Jython extends InteractiveConsole {
	private final Object _runCodeLock;
	private volatile boolean _running;

	private final PipedInputStream _inputStream;
	private final PipedOutputStream _inputStreamPipe;
	private final Object _feedInputLock;
	private InputStream _inputStreamSrc;

	static {
		System.setProperty("python.cachedir", "/home/hei/.jython-cache");
	}

	public Jython() {
		super();

		_runCodeLock = new Object();
		_running = false;

		_inputStream = new PipedInputStream();
		_inputStreamPipe = new PipedOutputStream();
		try {
			_inputStream.connect(_inputStreamPipe);
			setIn(_inputStream);
		} catch (final IOException e) {
			e.printStackTrace();
		}

		_feedInputLock = new Object();
		_inputStreamSrc = null;
		startFeedInputThread();
	}

	public void prepareRun() {
		synchronized (_runCodeLock) {
			_running = true;
		}
	}

	@Override
	public String raw_input(final PyObject prompt) {
		return raw_input(prompt, Py.getSystemState().stdin);
	}

	@Override
	public String raw_input(final PyObject prompt, final PyObject file) {
		return raw_input_impl(prompt, file);
	}

	private String raw_input_impl(final PyObject prompt, final PyObject file) {
		final PyObject stdout = Py.getSystemState().stdout;
		Py.print(stdout, prompt);

		synchronized (_runCodeLock) {
			_running = false;
			_runCodeLock.notify();
		}

		final String data = readline(file).toString();
		if (data.endsWith("\n")) {
			return data.substring(0, data.length() - 1);
		}
		if (data.length() == 0) {
			throw Py.EOFError("raw_input()");
		}

		return data;
	}

	private static PyString readline(final PyObject file) {
		if (file instanceof PyFile) {
			return ((PyFile) file).readline();
		}
		final PyObject ret = file.invoke("readline");
		if (!(ret instanceof PyString)) {
			throw Py.TypeError("object.readline() returned non-string");
		}
		return ((PyString) ret);
	}

	public void waitIfRunning() {
		synchronized (_runCodeLock) {
			while (_running) {
				try {
					_runCodeLock.wait();
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void startFeedInputThread() {
		final Thread feedInputThread = new Thread(new Runnable() {

			@Override
			public void run() {
				while (true) {
					final byte[] bytes = new byte[1024];
					int read;

					synchronized (_feedInputLock) {
						while (_inputStreamSrc == null) {
							try {
								_feedInputLock.wait();
							} catch (final InterruptedException e) {
								e.printStackTrace();
							}
						}

						try {
							while ((read = _inputStreamSrc.read(bytes)) > 0) {
								_inputStreamPipe.write(bytes, 0, read);
							}
						} catch (final IOException e) {
							// connection closed
						}
					}
				}

			}
		}, "FeedInput");
		feedInputThread.setDaemon(true);
		feedInputThread.start();
	}

	public void feedInput(final InputStream in) {
		synchronized (_feedInputLock) {
			_inputStreamSrc = in;
			_feedInputLock.notify();
		}
	}
}