/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.account.ldap.upgrade;

import java.io.PrintWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.ldap.LdapClient;

public class LdapUpgrade {

    private static String O_HELP = "h";
    private static String O_BUG = "b";
    private static String O_VERBOSE = "v";
    
    private static Options getAllOptions() {
        Options options = new Options ();
        options.addOption(O_HELP, "help", false, "print usage");
        options.addOption(O_VERBOSE, "verbose", false, "be verbose");
        options.addOption(O_BUG, "bug", true, "bug number this upgrade is for");
        
        return options;
    }
    
    private static String getCommandUsage() {
        return LdapUpgrade.class.getCanonicalName() + " <options> [args]";
    }
    
    private static void usage() {
        usage(null, null, null);
    }
    
    static void usage(ParseException e, UpgradeOp upgradeOp, String errMsg) {
        LdapUpgradePrinter printer = upgradeOp.printer;
        
        if (e != null) {
            printer.println("Error parsing command line arguments: " + e.getMessage());
        }
        
        if (errMsg != null) {
            printer.println(errMsg);
            printer.println();
        }

        Options opts = getAllOptions();
        PrintWriter pw = printer.getPrintWriter();
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(pw, formatter.getWidth(), getCommandUsage(),
                            null, opts, formatter.getLeftPadding(), formatter.getDescPadding(),
                            null);
        
        pw.flush();
        if (upgradeOp != null) {
            upgradeOp.usage(formatter);
        }
    }
    
    static void upgrade(String[] args) throws ServiceException {
        LdapUpgradePrinter printer = new LdapUpgradePrinter();
        
        printer.println("\n\n--------------");
        printer.print(LdapUpgrade.class.getCanonicalName() + " ");
        for (String arg : args) {
            printer.print(arg + " ");
        }
        printer.println();
        printer.println("--------------");
        
        CliUtil.toolSetup();
        
        CommandLine cl = null;
        try {
            CommandLineParser parser = new GnuParser();
            Options options = getAllOptions();
            cl = parser.parse(options, args);
            if (cl == null) {
                throw new ParseException("");
            }
        } catch (ParseException e) {
            usage(e, null, null);
            System.exit(1);
        }
        
        if (cl.hasOption(O_HELP)) {
            usage();
            System.exit(0);
        }

        if (!cl.hasOption(O_BUG)) {
            usage();
            System.exit(1);
        }
        
        String bug = cl.getOptionValue(O_BUG);
        boolean verbose = cl.hasOption(O_VERBOSE);
        
        UpgradeTask upgradeTask = UpgradeTask.getTaskByBug(bug);
        
        if (upgradeTask == null) {
            printer.println("unrecognized bug number");
            System.exit(1);
        } 
        
        LdapProv ldapProv = LdapProv.getInst();
        
        UpgradeOp upgradeOp = upgradeTask.getUpgradeOp();
        upgradeOp.setPrinter(printer);
        upgradeOp.setVerbose(verbose);
        upgradeOp.setBug(bug);
        upgradeOp.setLdapProv(ldapProv);
        
        if (!upgradeOp.parseCommandLine(cl)) {
            System.exit(1);
        }
        
        upgradeOp.doUpgrade();
        
        printer.println("\n\n--------------");
        printer.println("done " + bug);
        printer.println("--------------");
    }
    
    public static void main(String[] args) throws ServiceException {
        if (LdapClient.isLegacy()) {
            // delegate to the legacy package
            com.zimbra.cs.account.ldap.upgrade.legacy.LegacyLdapUpgrade.legacyMain(args);
        } else {
            upgrade(args);
        }
    }
}
