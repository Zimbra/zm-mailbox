/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.lmtpserver.utils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.lmtpserver.LmtpServer;
import com.zimbra.cs.lmtpserver.MinaLmtpServer;
import com.zimbra.cs.lmtpserver.LmtpConfig;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.server.Server;

public class StandAloneLmtpServer {

    private static Options mOptions = new Options();

    static {
        mOptions.addOption("p", "port", true, "listen port (required)");
        mOptions.addOption("a", "address", true, "bind address");
        mOptions.addOption("t", "threads", true, "threads in pool");
        mOptions.addOption("h", "help", false, "print usage");
        mOptions.addOption("u", "users", true, "properties file that lists users' maildir location");
    }

    private static void usage(String error) {
        if (error != null) {
            System.err.println("Error: " + error);
        }
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(StandAloneLmtpServer.class.getName(), mOptions);
        System.exit((error == null) ? 0 : 1);
    }

    private static CommandLine parseArgs(String args[]) {
        CommandLineParser parser = new GnuParser();
        CommandLine cl;
        try {
            cl = parser.parse(mOptions, args);
        } catch (ParseException pe) {
            usage(pe.getMessage());
            return null;
        }
        if (cl.hasOption("h")) {
            usage(null);
            return null;
        }
        return cl;
    }


    public static void main(String[] args) throws ServiceException {
        CliUtil.toolSetup();

        CommandLine cl = parseArgs(args);

        int port;
        String address;
        int threads;

        if (cl.hasOption("p")) {
            port = Integer.valueOf(cl.getOptionValue("p"));
            if (port <= 0) {
                usage("invalid port number: " + port);
                return;
            }
        } else {
            usage("port not specified");
            return;
        }

        if (cl.hasOption("a")) {
            address = cl.getOptionValue("a");
        } else {
            address = null;
        }

        if (cl.hasOption("t")) {
            threads = Integer.valueOf(cl.getOptionValue("t"));
            if (threads <= 0) {
                usage("invalid number of threads: " + threads);
                return;
            }
        } else {
            threads = 5;
        }

        Properties maildirMap = null;
        if (cl.hasOption("u")) {
            try {
                maildirMap = new Properties();
                maildirMap.load(new BufferedInputStream(new FileInputStream(cl.getOptionValue("u"))));
                maildirMap.list(System.out);
            } catch (IOException ioe) {
                usage(ioe.getMessage());
                return;
            }
        } else {
            usage("maildir map file not specified");
            return;
        }

        LmtpConfig config = new LmtpConfig();
        config.setBindAddress(address);
        config.setBindPort(port);
        config.setNumThreads(threads);
        config.setLmtpBackend(new MaildirBackend(maildirMap));
        
        Server lmtpServer;
        if (MinaLmtpServer.isEnabled()) {
            try {
                lmtpServer = new MinaLmtpServer(config);
            } catch (IOException e) {
                Zimbra.halt("failed to create MinaLmtpServer");
                return;
            }
        } else {
            lmtpServer = new LmtpServer(config);
        }
        
        try {
            lmtpServer.start();
        } catch (IOException e) {
            Zimbra.halt("failed to start LmtpServer");
        }

        try {
            while (true) {
                byte[] line = new byte[1024];
                System.in.read(line);
                String command = new String(line, 0, 5);
                if ("quit\n".equalsIgnoreCase(command)) {
                    break;
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        lmtpServer.shutdown(15);
    }
}
