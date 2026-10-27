package app.sewain.exception;

public class ConflictException extends RuntimeException {
	public ConflictException(Object message) {
		super(String.valueOf(message));
	}
}
