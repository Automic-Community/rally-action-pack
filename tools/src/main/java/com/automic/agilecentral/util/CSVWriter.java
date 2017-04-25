package com.automic.agilecentral.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * This Class used to create csv file
 * 
 */
public class CSVWriter implements AutoCloseable {

    private static final int BUFFER_SIZE = 8 * 1024;
    private static final char DEFAULT_SEPARATOR = ',';
    private static final String QUOTE = "\"";
    private static final String DOUBLEQUOTE = "\"\"";

    private BufferedWriter bw = null;
    private FileWriter fw = null;

    public CSVWriter(String filePath, List<String> headers) throws IOException {
        fw = new FileWriter(filePath);
        bw = new BufferedWriter(fw, BUFFER_SIZE);
        writeLine(headers);
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
                sb.append(QUOTE).append(escapeData(value)).append(QUOTE);
            } else {
                sb.append(QUOTE).append(QUOTE);
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
        if (result.contains(QUOTE)) {
            result = result.replace(QUOTE, DOUBLEQUOTE);
        }
        return result;
    }

    public void flush() throws IOException {
        bw.flush();
    }

    @Override
    public void close() {
        close(bw);
        close(fw);        
    }
    
    private void close(Writer wr) {
        try {
            wr.close();
        } catch (IOException ie) {
            ConsoleWriter.writeln(ie);
        }
    }
}