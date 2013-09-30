/**
 * Copyright 2013 Tommi S.E. Laukkanen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.groom;

import org.apache.log4j.Logger;
import org.groom.shell.SystemCommandExecutor;
import org.vaadin.addons.sitekit.util.PropertiesUtil;

import java.util.*;

/**
 * Class which executes shell commands.
 *
 * @author Tommi S.E. Laukkanen
 */
public class Shell {
    /** The logger. */
    private static final Logger LOGGER = Logger.getLogger(Shell.class);

    /**
     * Executes requested shell command.
     *
     * @param cmd the shell command to execute
     */
    public static String execute(final String cmd, final String path) {
        LOGGER.debug("Executing shell command: " + cmd);
        try {

            List<String> commands = new ArrayList<String>();
            if (PropertiesUtil.getProperty("groom", "os").equals("windows")) {
                commands.add("cmd");
                commands.add("/c");
                commands.add(cmd);
            } else {
                commands.add("/bin/sh");
                commands.add("-c");
                commands.add(cmd);
            }

            SystemCommandExecutor commandExecutor = new SystemCommandExecutor(path, commands);
            commandExecutor.executeCommand();

            StringBuilder errorOutput = commandExecutor.getErrorOutput();
            if (errorOutput.length() > 0) {
                LOGGER.error(errorOutput);
            }
            StringBuilder standardOutput = commandExecutor.getStandardOutput();
            if (standardOutput == null) {
                return "";
            }

            return standardOutput.toString();
        } catch (final Throwable t) {
            LOGGER.error("Error executing shell command: " + cmd, t);
            return "";
        }
    }

}
