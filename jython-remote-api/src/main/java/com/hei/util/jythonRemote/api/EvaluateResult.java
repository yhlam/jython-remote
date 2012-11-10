package com.hei.util.jythonRemote.api;

import com.hei.util.jythonRemote.api.JythonRemoteConstants.Prompt;

public final class EvaluateResult {
	private final Prompt prompt;
	private final String result;

	public EvaluateResult(final Prompt prompt, final String result) {
		this.prompt = prompt;
		this.result = result;
	}

	public Prompt getPrompt() {
		return prompt;
	}

	public String getResult() {
		return result;
	}
}