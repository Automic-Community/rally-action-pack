package com.automic.agilecentral.util;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;

import com.automic.agilecentral.constants.Constants;

/**
 * Common Utility class contains basic function(s) required by CA Agile Central actions.
 * 
 */
public class CommonUtil {

    private CommonUtil() {
    }

    /**
     * Method to format error message in the format "ERROR | message"
     *
     * @param message
     * @return formatted message
     */
    public static final String formatErrorMessage(final String message) {
        final StringBuilder sb = new StringBuilder();
        if (null != message && !message.isEmpty()) {
            sb.append("ERROR").append(" | ").append(message);
        }

        return sb.toString();
    }

    /**
     * Method to check if a String is not empty
     *
     * @param field
     * @return true if String is not empty else false
     */
    public static final boolean checkNotEmpty(String field) {
        return field != null && !field.isEmpty();
    }

    /**
     * Method to read environment value. If not defined then it returns the default value as specified.
     * 
     * @param value
     * @param defaultValue
     * @return
     */
    public static final String getEnvParameter(final String paramName, String defaultValue) {
        String val = System.getenv(paramName);
        if (val == null) {
            val = defaultValue;
        }
        return val;
    }

    /**
     *
     * Method to parse String containing numeric integer value. If string is not valid, then it returns the default
     * value as specified.
     *
     * @param value
     * @param defaultValue
     * @return numeric value
     */
    public static int parseStringValue(final String value, int defaultValue) {
        int i = defaultValue;
        if (checkNotEmpty(value)) {
            try {
                i = Integer.parseInt(value);
            } catch (final NumberFormatException nfe) {
                i = defaultValue;
            }
        }
        return i;
    }

    /**
     * Method to convert a stream into Json object
     * 
     * @param is
     *            input stream
     * @return {@link JsonObject}
     */
    public static final JsonObject jsonObjectResponse(InputStream is) {
        return Json.createReader(is).readObject();

    }

    /**
     * Method to beautify the jsonand write on the console
     * 
     * @param jsonObj
     */
    public static final String jsonPrettyPrinting(JsonObject jsonObj) {
        StringWriter stringWriter = new StringWriter();

        Map<String, Boolean> config = new HashMap<>();
        config.put(JsonGenerator.PRETTY_PRINTING, true);

        JsonWriterFactory writerFactory = Json.createWriterFactory(config);
        JsonWriter jsonWriter = writerFactory.createWriter(stringWriter);
        jsonWriter.writeObject(jsonObj);
        jsonWriter.close();

        return stringWriter.toString();
    }

    /**
     * Method to convert YES/NO values to boolean true or false
     * 
     * @param value
     * @return true if YES, 1
     */
    public static final boolean convert2Bool(String value) {
        boolean ret = false;
        if (checkNotEmpty(value)) {
            ret = Constants.YES.equalsIgnoreCase(value) || Constants.TRUE.equalsIgnoreCase(value)
                    || Constants.ONE.equalsIgnoreCase(value);
        }
        return ret;
    }

    public static final String formatDate(final String message) {
        final StringBuilder sb = new StringBuilder();

        return sb.toString();
    }

    /**
     *
     * Method to read the value as defined in environment. If value is not valid integer, then it returns the default
     * value as specified.
     *
     * @param paramName
     * @param defaultValue
     * @return parameter value
     */
    public static final int getEnvParameter(final String paramName, int defaultValue) {
        String val = System.getenv(paramName);
        int i;
        if (val != null) {
            try {
                i = Integer.parseInt(val);
            } catch (final NumberFormatException nfe) {
                i = defaultValue;
            }
        } else {
            i = defaultValue;
        }
        return i;
    }
}
