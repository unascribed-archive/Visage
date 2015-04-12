package com.gameminers.visage.master.exception;

public class NoSlavesAvailableException extends Exception {
	private static final long serialVersionUID = -1249873765125395486L;
	public NoSlavesAvailableException() {
		super();
	}

	public NoSlavesAvailableException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public NoSlavesAvailableException(String message, Throwable cause) {
		super(message, cause);
	}

	public NoSlavesAvailableException(String message) {
		super(message);
	}

	public NoSlavesAvailableException(Throwable cause) {
		super(cause);
	}

}
