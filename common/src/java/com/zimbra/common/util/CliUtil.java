/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.common.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import jline.ConsoleReader;
import jline.ConsoleReaderInputStream;
import jline.History;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.net.SocketFactories;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

public class CliUtil {
    public static void toolSetup() {
        toolSetup("INFO"); 
    }

    public static void toolSetup(String defaultLogLevel) {
        toolSetup(defaultLogLevel, null, false);
    }

    public static void toolSetup(String defaultLogLevel, String logFile, boolean showThreads) {
        ZimbraLog.toolSetupLog4j(defaultLogLevel, logFile, showThreads);
        SocketFactories.registerProtocols();
        
        // Bug: 47051
        // for the CLI utilities we need to disable HTTP soap client timeout.
        LC.httpclient_soaphttptransport_so_timeout.setDefault(LC.cli_httpclient_soaphttptransport_so_timeout.longValue()); 
    }
    
    /**
     * Sets up the default value in local configuration cache for httpclient_soaphttptransport_so_timeout
     * for CLI utilities. Use cli_httpclient_soaphttptransport_so_timeout configuration key to 
     * override the default using local configuration file.
     */
    public static void setCliSoapHttpTransportTimeout() {
        LC.httpclient_soaphttptransport_so_timeout.setDefault(LC.cli_httpclient_soaphttptransport_so_timeout.longValue());
    }

    /**
     * Looks up an <tt>Option</tt> by its short or long name.  This workaround is necessary
     * because <tt>CommandLine.hasOption()</tt> doesn't support long option names.
     */
    public static Option getOption(CommandLine cl, String name) {
        for (Option opt : cl.getOptions()) {
            if (StringUtil.equal(opt.getOpt(), name) || StringUtil.equal(opt.getLongOpt(), name)) {
                return opt;
            }
        }
        return null;
    }

    /**
     * Returns <tt>true</tt> if either a short or long option with the given name was
     * specified on the command line.
     */
    public static boolean hasOption(CommandLine cl, String name) {
        return (getOption(cl, name) != null);
    }
    
    /**
     * Returns the value for the given option name.
     * @param cl command line
     * @param name either short or long option name
     */
    public static String getOptionValue(CommandLine cl, String name) {
        Option opt = getOption(cl, name);
        if (opt == null) {
            return null;
        }
        return opt.getValue();
    }
    
    /**
     * Turns on command line editing with JLine.  
     * @param histFilePath path to the history file, or {@code null} to not save history
     * @throws IOException if the history file is not writable or cannot be created
     */
    public static void enableCommandLineEditing(String histFilePath)
    throws IOException {
        File histFile = null;
        if (histFilePath != null) {
            histFile = new File(histFilePath);
            if (!histFile.exists()) {
                if (!histFile.createNewFile()) {
                    throw new IOException("Unable to create history file " + histFilePath);
                }
            }
            if (!histFile.canWrite()) {
                throw new IOException (histFilePath + " is not writable");
            }
        }
        ConsoleReader reader = new ConsoleReader();
        if (histFile != null) {
            reader.setHistory(new History(histFile));
        }
        ConsoleReaderInputStream.setIn(reader);
    }
    
    public static boolean confirm(String msg) {
        System.out.print(msg + " [Y]es, [N]o: ");
        BufferedReader in;
        try {
            in = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
            String line = StringUtil.readLine(in);
            if ("y".equalsIgnoreCase(line) || "yes".equalsIgnoreCase(line)) {
                return true;
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

}
