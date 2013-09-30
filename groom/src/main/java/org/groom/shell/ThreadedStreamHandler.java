package org.groom.shell;

import org.apache.log4j.Logger;

import java.io.*;

/**
 * The threaded stream handler.
 */
class ThreadedStreamHandler extends Thread {

    /** The logger. */
    private static final Logger LOGGER = Logger.getLogger(ThreadedStreamHandler.class);

    InputStream inputStream;
    OutputStream outputStream;
    PrintWriter printWriter;
    StringBuilder outputBuffer = new StringBuilder();

    ThreadedStreamHandler(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    ThreadedStreamHandler(InputStream inputStream, OutputStream outputStream) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.printWriter = new PrintWriter(outputStream);
    }

    public void run() {

        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (outputBuffer.length() > 0) {
                    outputBuffer.append('\n');
                }
                outputBuffer.append(line);
            }
        } catch (IOException e) {
            LOGGER.error(e);
        } catch (Throwable t) {
            LOGGER.error(t);
        } finally {
            try {
                bufferedReader.close();
            } catch (IOException e) {
            }
        }
    }

    public StringBuilder getOutputBuffer() {
        return outputBuffer;
    }

}








