package com.automic.agilecentral.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * This Class used to create csv file
 * 
 */
public class CSVWriter implements AutoCloseable {

    private static final char DEFAULT_SEPARATOR = ',';
    private static final String QUOTE_CHARACTER = "\"";
    private static final String DOUBLEQUOTE_CHARACTER = "\"\"";

    private BufferedWriter bw = null;
    private FileWriter fw = null;

    public CSVWriter(String filePath, String[] headers) throws IOException {
        fw = new FileWriter(filePath);
        bw = new BufferedWriter(fw);
        writeLine(Arrays.asList(headers));
    }

    /**
     * Method to write line in csv file
     * 
     * @param values
     * @throws IOException
     */
    public void writeLine(List<String> values) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (CommonUtil.checkNotEmpty(value)) {
                sb.append(QUOTE_CHARACTER).append(escapeData(value)).append(QUOTE_CHARACTER);
            } else {
                sb.append(QUOTE_CHARACTER).append(QUOTE_CHARACTER);
            }
            sb.append(DEFAULT_SEPARATOR);

        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append("\n");
        bw.append(sb.toString());
    }

    private String escapeData(String value) {
        String result = value;
        if (result.contains(QUOTE_CHARACTER)) {
            result = result.replace(QUOTE_CHARACTER, DOUBLEQUOTE_CHARACTER);
        }
        return result;
    }

    public void flush() throws IOException {
        bw.flush();
    }

    @Override
    public void close() throws IOException {

        if (bw != null) {
            bw.close();
        }

        if (fw != null) {
            fw.close();
        }

    }

}