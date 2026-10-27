package app.sewain.exception;

public class MediaStorageException extends RuntimeException{
	public MediaStorageException() {
	}

	public MediaStorageException(String message) {
		super(message);
	}

	public MediaStorageException(String message, Throwable cause) {
		super(message, cause);
	}
}
