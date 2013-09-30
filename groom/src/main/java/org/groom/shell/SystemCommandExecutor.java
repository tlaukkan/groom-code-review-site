package org.groom.shell;

import org.vaadin.addons.sitekit.util.PropertiesUtil;

import java.io.*;
import java.util.List;

/**
 * System shell command executor.
 */
public class SystemCommandExecutor {
    private final String path;
    private List<String> commandInformation;
    private ThreadedStreamHandler inputStreamHandler;
    private ThreadedStreamHandler errorStreamHandler;

    public SystemCommandExecutor(final String path, final List<String> commandInformation) {
        this.path = path;
        this.commandInformation = commandInformation;
    }

    public void executeCommand()
            throws IOException, InterruptedException {
        try {
            ProcessBuilder pb = new ProcessBuilder(commandInformation);
            pb.directory(new File(PropertiesUtil.getProperty("groom", "repository-path") + "/" + path));
            Process process = pb.start();

            OutputStream stdOutput = process.getOutputStream();
            InputStream inputStream = process.getInputStream();
            InputStream errorStream = process.getErrorStream();
            inputStreamHandler = new ThreadedStreamHandler(inputStream, stdOutput);
            errorStreamHandler = new ThreadedStreamHandler(errorStream);

            inputStreamHandler.start();
            errorStreamHandler.start();
            process.waitFor();
            inputStreamHandler.interrupt();
            errorStreamHandler.interrupt();
            inputStreamHandler.join();
            errorStreamHandler.join();
        } catch (IOException e) {
            throw e;
        } catch (InterruptedException e) {
            throw e;
        }
    }

    public StringBuilder getStandardOutput() {
        return inputStreamHandler.getOutputBuffer();
    }

    public StringBuilder getErrorOutput() {
        return errorStreamHandler.getOutputBuffer();
    }

}






