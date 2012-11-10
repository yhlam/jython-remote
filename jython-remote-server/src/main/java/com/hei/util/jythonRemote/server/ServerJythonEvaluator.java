package com.hei.util.jythonRemote.server;

import java.io.ByteArrayOutputStream;

import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.util.InteractiveInterpreter;

import com.hei.util.jythonRemote.api.EvaluateResult;
import com.hei.util.jythonRemote.api.JythonEvaluator;
import com.hei.util.jythonRemote.api.JythonRemoteConstants.Prompt;

class ServerJythonEvaluator implements JythonEvaluator {
	private static final String STD_IN = "<stdin>";

	private final InteractiveInterpreter jython;
	private final Object lock;

	ServerJythonEvaluator() {
		jython = new InteractiveInterpreter();

		// Dummy exec in order to speed up response on first command
		jython.exec("0");

		lock = new Object();
	}

	public void setLocals(final PyObject locals) {
		synchronized (lock) {
			jython.setLocals(locals);
		}
	}

	public void cleanup() {
		synchronized (lock) {
			jython.cleanup();
		}
	}

	@Override
	public EvaluateResult evaluate(final String code) {
		final boolean more;
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		synchronized (lock) {
			jython.setOut(out);
			jython.setErr(out);

			more = jython.runsource(code, STD_IN);
		}

		final Prompt prompt = more ? Prompt.Continue : Prompt.NewLine;
		final String result = out.toString();
		final EvaluateResult evalResult = new EvaluateResult(prompt, result);

		return evalResult;
	}

	@Override
	public String getPlatform() {
		return PySystemState.platform.toString();
	}

	@Override
	public String getVersion() {
		return PySystemState.version.toString();
	}
}
