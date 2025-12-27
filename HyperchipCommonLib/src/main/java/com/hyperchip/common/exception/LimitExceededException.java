package com.hyperchip.common.exception;

public class LimitExceededException extends RuntimeException {
    public LimitExceededException() { super(); }
    public LimitExceededException(String message) { super(message); }
    public LimitExceededException(String message, Throwable cause) { super(message, cause); }
}
