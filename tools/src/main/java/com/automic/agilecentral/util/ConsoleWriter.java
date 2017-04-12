package com.automic.agilecentral.util;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import com.automic.agilecentral.constants.ExceptionConstants;

/**
 * This class writes content to standard console
 * 
 * @author sumitsamson
 *
 */
public final class ConsoleWriter {
    private static final ByteWriter WRITER = new ByteWriter(System.out);

    /**
     * Method to write a newline to console
     * 
     */
    public static final void newLine() {
        write(System.lineSeparator());
    }

    /**
     * Method to write an Object to console and followed by newline.
     *
     * @param content
     */
    public static final void writeln(Object content) {
        String temp = content != null ? content.toString() : "null";
        write(temp);
        newLine();
    }

    /**
     * Method to to log the exception trace.
     * 
     * @param content
     */
    public static final void writeln(Throwable content) {
        StringWriter sw = new StringWriter(4 * 1024);
        PrintWriter pw = new PrintWriter(sw);
        content.printStackTrace(pw);
        pw.flush();
        write(sw.toString());
        newLine();
    }

    /**
     * Method to write string to console
     *
     * @param content
     */
    public static final void write(String content) {
        String temp = content != null ? content : "null";
        WRITER.write(temp);
    }

    /**
     * Method to flush to console
     * 
     */
    public static final void flush() {
        WRITER.flush();
    }

    /**
     * 
     * Inner class for writing messages.
     *
     */
    private static class ByteWriter {

        private static final int IO_BUFFER_SIZE = 4 * 1024;
        private BufferedOutputStream bos = null;

        public ByteWriter(OutputStream output) {
            bos = new BufferedOutputStream(output, IO_BUFFER_SIZE);
        }

        /**
         * Method to write specific part of byte array to Stream
         *
         * @param bytes
         * @param offset
         * @param length
         */
        public void write(byte[] bytes, int offset, int length) {
            try {
                bos.write(bytes, offset, length);
            } catch (IOException e) {
                System.out.println(new String(bytes, offset, length));
                e.printStackTrace();
            }
        }

        /**
         * Method to write bytes to Stream
         *
         * @param bytes
         */
        public void write(byte[] bytes) {
            write(bytes, 0, bytes.length);
        }

        /**
         * Method to write a String to stream
         *
         * @param field
         */
        public void write(String field) {
            write(field.getBytes(StandardCharsets.UTF_8));
        }

        /**
         * Method to flush to stream
         */
        public void flush() {
            if (bos != null) {
                try {
                    bos.flush();
                } catch (IOException e) {
                    System.out.println(ExceptionConstants.UNABLE_TO_FLUSH_STREAM);
                    e.printStackTrace();
                }
            }
        }
    }
}
