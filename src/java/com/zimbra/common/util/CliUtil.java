/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.common.util;

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
}
