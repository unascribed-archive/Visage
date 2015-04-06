package blue.lapis.lapitar2.master;

public class RenderFailedException extends Exception {

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
