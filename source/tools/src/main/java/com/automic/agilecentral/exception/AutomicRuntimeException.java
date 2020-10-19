package com.automic.agilecentral.exception;

/**
 * This exception is used to throw {@link RuntimeException}
 * */
public class AutomicRuntimeException extends RuntimeException {

    private static final long serialVersionUID = -1823674969631467447L;

    /**
     * @param message
     */
    public AutomicRuntimeException(String message) {
        super(message);
    }
}
