package com.automic.agilecentral.util;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * This Class used to create csv file
 * 
 * @author anuragupadhyay
 *
 */
public class CSVUtils {

    private static final char DEFAULT_SEPARATOR = ',';

    /**
     * Method to write line in csv file
     * 
     * @param w
     * @param values
     * @throws IOException
     */
    public static void writeLine(Writer w, List<String> values) throws IOException {
        writeLine(w, values, DEFAULT_SEPARATOR, '"');
    }

    private static String escapeData(String value) {

        String result = value;
        if (result.contains("\"")) {
            result = result.replace("\"", "\"\"");
        }
        return result;

    }

    /**
     * Method to write line in csv file
     * 
     * @param w
     * @param values
     * @param separators
     * @param customQuote
     * @throws IOException
     */
    public static void writeLine(Writer w, List<String> values, char separators, char customQuote) throws IOException {

        if (separators == ' ') {
            separators = DEFAULT_SEPARATOR;
        }

        StringBuilder sb = new StringBuilder();
        for (String value : values) {

            sb.append(customQuote).append(escapeData(value)).append(customQuote);
            sb.append(separators);

        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append("\n");
        w.append(sb.toString());

    }

}