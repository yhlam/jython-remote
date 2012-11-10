package com.hei.util.jythonRemote.api;

public interface JythonEvaluator {

	public EvaluateResult evaluate(final String code);

	public String getPlatform();

	public String getVersion();
}
