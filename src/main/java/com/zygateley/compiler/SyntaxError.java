package com.zygateley.compiler;


public class SyntaxError extends Exception {
	static final long serialVersionUID = 666;

	public SyntaxError() {
		super("Syntax error");
	}

	public SyntaxError(String message) {
		super(message);
	}

	public SyntaxError(Throwable cause) {
		super(cause);
	}

	public SyntaxError(String message, Throwable cause) {
		super(message, cause);
	}

	public SyntaxError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
