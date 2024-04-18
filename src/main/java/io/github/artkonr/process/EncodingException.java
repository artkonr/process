package io.github.artkonr.process;

public class EncodingException extends RuntimeException {

    public EncodingException(String message) {
        this(message, null);
    }

    public EncodingException(String message, Throwable cause) {
        super(message, cause);
    }

    public EncodingException(Throwable cause) {
        this(null, cause);
    }
}
