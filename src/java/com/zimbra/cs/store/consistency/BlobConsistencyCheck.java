/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
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
package com.zimbra.cs.store.consistency;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.CommandLine;

import org.xml.sax.InputSource;

public class BlobConsistencyCheck {
    private final static String REPAIR_WARNING =
            "You have requested to repair the inconsistent BLOBs in your\n" +
            "zimbra installation.  This repair process will delete the\n" +
            "associated metadata for missing BLOBs, and update the volume\n" +
            "ID for incorrectly referenced BLOBs.  Zimbra mailboxd must\n" +
            "be shutdown.  If you are certain you wish to do this, and\n" +
            "have shutdown mailboxd then re-run this command with the -c\n" +
            "option.";
    final static String LOCAL_CONFIG = "/opt/zimbra/conf/localconfig.xml";
    final static String ZIMBRA_USER  = "zimbra";
    final static String JDBC_DRIVER  = "com.mysql.jdbc.Driver";

    final static String XPATH_EXPR =
            "/localconfig/key[@name = 'zimbra_mysql_password']/value";

    /**
     * "short option", "long option", "description", "has-arg", "required".
     */
    final static Object[][] OPTION_LIST = {
        { "z", "gzip",     "test compressed blobs",         false, false },
        { "p", "password", "zimbra user mysql password",    true,  false },
        { "h", "help",     "this help message",             false, false },
        //{ "r", "repair",   "repair/delete missing blobs",   false, false },
        { "f", "file",     "save/load report to/from file", true,  false },
        { "l", "load",     "display report from file",      true,  false },
        //{ "c", "force",    "required when repairing",       false, false },
    };

    public static void main(String[] args) throws Exception {
        Class.forName(JDBC_DRIVER);
        File reportFile = null;
        String mysqlPasswd = null;

        Options options = new Options();
        loadOptions(options);
        PosixParser parser = new PosixParser();
        CommandLine cmdLine = parser.parse(options, args);

        if (cmdLine.hasOption("h")) {
            HelpFormatter fmt = new HelpFormatter();
            fmt.printHelp("zmblobchk", options);
            return;
        }

        if (cmdLine.hasOption("p"))
            mysqlPasswd = cmdLine.getOptionValue("p");
        else
            mysqlPasswd = loadMysqlPassword();

        if (!cmdLine.hasOption("l") &&
                (mysqlPasswd == null || "".equals(mysqlPasswd.trim()))) {
            System.err.println(LOCAL_CONFIG +
                    ": no mysql password found, rerun with -p");
            return;
        }

        if (cmdLine.hasOption("r") && !cmdLine.hasOption("f")) {
            System.err.println("--repair requires --file to be specified");
            return;
        }
        if (!cmdLine.hasOption("f") && !cmdLine.hasOption("l")) {
            File tmpDir = new File(System.getProperty("java.io.tmpdir"));
            reportFile = File.createTempFile("zmblobc", ".rpt", tmpDir);
        } else if (cmdLine.hasOption("f")) {
            reportFile = new File(cmdLine.getOptionValue("f"));
        }

        if (cmdLine.hasOption("r") && !cmdLine.hasOption("c")) {
            // repair
            System.out.println(REPAIR_WARNING);
        } else if (cmdLine.hasOption("r") && cmdLine.hasOption("c")) {
            new BlobRepair(mysqlPasswd, reportFile).run();
        } else if (cmdLine.hasOption("l")) {
            // display report
            reportFile = new File(cmdLine.getOptionValue("l"));
            new ReportDisplay(reportFile).run();
        } else {
            // generate report
            new ReportGenerator(mysqlPasswd, reportFile, cmdLine.hasOption("z")).run();
        }
    }

    private static void loadOptions(Options options) {
        for (Object[] o : OPTION_LIST) {
            Option opt = new Option((String) o[0], (String) o[1],
                                    (Boolean) o[3], (String) o[2]);
            opt.setRequired((Boolean) o[4]);
            options.addOption(opt);
        }
    }

    private static String loadMysqlPassword() throws IOException {
        File config = new File(LOCAL_CONFIG);
        String passwd = null;
        if (config.exists() && config.isFile()) {
            FileInputStream in = null;
            try {
                in = new FileInputStream(config);
                InputSource src = new InputSource(in);
                XPathFactory xpf = XPathFactory.newInstance();
                XPath xp = xpf.newXPath();
                try {
                    passwd = xp.evaluate(XPATH_EXPR, src);
                }
                catch (XPathExpressionException e) {
                    e.printStackTrace();
                }
            } finally {
                if (in != null) in.close();
            }
        }
        return passwd;
    }
}
