package com.gameminers.visage.master.exception;

public class RenderFailedException extends Exception {
	private static final long serialVersionUID = 6835187728991896166L;

	public RenderFailedException() {
		super();
	}

	public RenderFailedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public RenderFailedException(String message, Throwable cause) {
		super(message, cause);
	}

	public RenderFailedException(String message) {
		super(message);
	}

	public RenderFailedException(Throwable cause) {
		super(cause);
	}

}
