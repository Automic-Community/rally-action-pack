package com.automic.agilecentral.constants;

/**
 * Exception constants used in the application
 * 
 * @author shrutinambiar
 *
 */
public class ExceptionConstants {

    public static final String UNABLE_TO_FLUSH_STREAM = "Error while flushing stream";
    public static final String INVALID_ARGS = "Improper Args. Possible cause : %s";

    public static final String INVALID_INPUT_PARAMETER = "Invalid value for parameter [%s] : [%s]";

    public static final String INVALID_FILE = "Invalid file [%s], possibly file doesn't exists";
    public static final String GENERIC_ERROR_MSG = "System Error occured.";

    public static final String INVALID_FILE_PATH = "Invalid file path [%s], "
            + "possibly application can not write at specified path";

    private ExceptionConstants() {
    }
}
