package com.automic.agilecentral.validator;

import java.io.File;

import com.automic.agilecentral.constants.ExceptionConstants;
import com.automic.agilecentral.exception.AutomicException;
import com.automic.agilecentral.util.CommonUtil;

/**
 * This class provides common validations as required by action(s).
 *
 */

public final class AgileCentralValidator {

    private AgileCentralValidator() {
    }

    public static final void checkNotEmpty(String parameter, String parameterName) throws AutomicException {
        if (!CommonUtil.checkNotEmpty(parameter)) {
            throw new AutomicException(String.format(ExceptionConstants.INVALID_INPUT_PARAMETER, parameterName,
                    parameter));
        }
    }

    public static final void checkNotNull(Object parameter, String parameterName) throws AutomicException {
        if (parameter != null) {
            throw new AutomicException(String.format(ExceptionConstants.INVALID_INPUT_PARAMETER, parameterName,
                    parameter));
        }
    }

    public static void lessThan(int value, int lessThan, String parameterName) throws AutomicException {
        if (value < lessThan) {
            String errMsg = String.format(ExceptionConstants.INVALID_INPUT_PARAMETER, parameterName, value);
            throw new AutomicException(errMsg);
        }
    }

    public static final void checkFileExists(File file) throws AutomicException {
        if (!(file.exists() && file.isFile())) {
            throw new AutomicException(String.format(ExceptionConstants.INVALID_FILE, file));
        }
    }
}
