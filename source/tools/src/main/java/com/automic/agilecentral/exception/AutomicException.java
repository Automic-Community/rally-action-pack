package com.automic.agilecentral.exception;

/**
 * Exception class thrown to indicate that error has occurred while processing request. It could either be <li>
 * <ul>
 * Business exception for invalid inputs to Actions
 * </ul>
 * <ul>
 * Technical exception to denote errors while communicating with CA Agile Central(Rally) API
 * </ul>
 * </li>
 *
 */
public class AutomicException extends Exception {

    private static final long serialVersionUID = -2593992984664372683L;

    /**
     * Constructor that takes an error message
     *
     * @param message
     */
    public AutomicException(String message) {
        super(message);
    }
}
