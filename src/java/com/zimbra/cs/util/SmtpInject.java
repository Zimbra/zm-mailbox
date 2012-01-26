/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.util;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.zimbra.common.mime.shim.JavaMailInternetAddress;
import com.zimbra.common.net.SocketFactories;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.zmime.ZMimeMessage;
import com.zimbra.common.zmime.ZSharedFileInputStream;

/**
 * Simple command line SMTP client for testing purposes.
 */
public class SmtpInject {

    private static Log mLog = LogFactory.getLog(SmtpInject.class);

    private static Options mOptions = new Options();

    static {
        mOptions.addOption("h", "help",      false, "show help text");
        mOptions.addOption("f", "file",      true,  "rfc822/MIME formatted text file");
        mOptions.addOption("a", "address",   true,  "smtp server (default localhost)");
        mOptions.addOption("s", "sender",    true,  "envelope sender (mail from)");
        Option ropt = new Option("r", "recipient", true, "envelope recipients (rcpt to)");
        ropt.setArgs(Option.UNLIMITED_VALUES);
        mOptions.addOption(ropt);
        mOptions.addOption("T", "trace",     false, "trace server/client traffic");
        mOptions.addOption("t", "tls",       false, "use TLS");
        mOptions.addOption("A", "auth",      false, "use SMTP auth");
        mOptions.addOption("u", "username",  true,  "username for SMTP auth");
        mOptions.addOption("p", "password",  true,  "password for SMTP auth");
        mOptions.addOption("v", "verbose",   false, "show provided options");
    }

    private static void usage(String errmsg) {
        if (errmsg != null) {
            mLog.error(errmsg);
        }
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("SmtpInject [options]", mOptions);
        System.exit((errmsg == null) ? 0 : 1);
    }

    private static CommandLine parseArgs(String args[]) {
        StringBuffer gotCL = new StringBuffer("cmdline: ");
        for (int i = 0; i < args.length; i++) {
            gotCL.append("'").append(args[i]).append("' ");
        }
        //mLog.info(gotCL);

        CommandLineParser parser = new GnuParser();
        CommandLine cl = null;
        try {
            cl = parser.parse(mOptions, args);
        } catch (ParseException pe) {
            usage(pe.getMessage());
        }
        return cl;
    }

    public static void main(String[] args) {
        CliUtil.toolSetup();
        CommandLine cl = parseArgs(args);

        if (cl.hasOption("h")) {
            usage(null);
        }

        String file = null;
        if (!cl.hasOption("f")) {
            usage("no file specified");
        } else {
            file = cl.getOptionValue("f");
        }
        try {
            ByteUtil.getContent(new File(file));
        } catch (IOException ioe) {
            usage(ioe.getMessage());
        }

        String host = null;
        if (!cl.hasOption("a")) {
            usage("no smtp server specified");
        } else {
            host = cl.getOptionValue("a");
        }

        String sender = null;
        if (!cl.hasOption("s")) {
            usage("no sender specified");
        } else {
            sender = cl.getOptionValue("s");
        }

        String recipient = null;
        if (!cl.hasOption("r")) {
            usage("no recipient specified");
        } else {
            recipient = cl.getOptionValue("r");
        }

        boolean trace = false;
        if (cl.hasOption("T")) {
            trace = true;
        }

        boolean tls = false;
        if (cl.hasOption("t")) {
            tls = true;
        }

        boolean auth = false;
        String user = null;
        String password = null;
        if (cl.hasOption("A")) {
            auth = true;
            if (!cl.hasOption("u")) {
                usage("auth enabled, no user specified");
            } else {
                user = cl.getOptionValue("u");
            }
            if (!cl.hasOption("p")) {
                usage("auth enabled, no password specified");
            } else {
                password = cl.getOptionValue("p");
            }
        }

        if (cl.hasOption("v")) {
            mLog.info("SMTP server: " + host);
            mLog.info("Sender: " + sender);
            mLog.info("Recipient: " + recipient);
            mLog.info("File: " + file);
            mLog.info("TLS: " + tls);
            mLog.info("Auth: " + auth);
            if (auth) {
                mLog.info("User: " + user);
                char[] dummyPassword = new char[password.length()];
                Arrays.fill(dummyPassword, '*');
                mLog.info("Password: " + new String(dummyPassword));
            }
        }

        Properties props = System.getProperties();

        props.put("mail.smtp.host", host);

        if (auth) {
            props.put("mail.smtp.auth", "true");
        } else {
            props.put("mail.smtp.auth", "false");
        }

        if (tls) {
            props.put("mail.smtp.starttls.enable", "true");
        } else {
            props.put("mail.smtp.starttls.enable", "false");
        }

        // Disable certificate checking so we can test against
        // self-signed certificates
        props.put("mail.smtp.ssl.socketFactory", SocketFactories.dummySSLSocketFactory());

        Session session = Session.getInstance(props, null);
        session.setDebug(trace);

        try {
            // create a message
            MimeMessage msg = new ZMimeMessage(session, new ZSharedFileInputStream(file));
            InternetAddress[] address = { new JavaMailInternetAddress(recipient) };
            msg.setFrom(new JavaMailInternetAddress(sender));

            // attach the file to the message
            Transport transport = session.getTransport("smtp");
            transport.connect(null, user, password);
            transport.sendMessage(msg, address);

        } catch (MessagingException mex) {
            mex.printStackTrace();
            Exception ex = null;
            if ((ex = mex.getNextException()) != null) {
                ex.printStackTrace();
            }
            System.exit(1);
        }
    }
}
