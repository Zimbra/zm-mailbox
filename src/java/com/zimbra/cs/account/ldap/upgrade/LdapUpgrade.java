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

    private static final LdapUpgradePrinter printer = new LdapUpgradePrinter();
    
    private static String O_HELP = "h";
    private static String O_BUG = "b";
    private static String O_DESCRIBE ="d";
    private static String O_DESCRIBE_ALL ="da";
    private static String O_VERBOSE = "v";
    
    private static Options getAllOptions() {
        Options options = new Options ();
        options.addOption(O_HELP, "help", false, "print usage");
        options.addOption(O_VERBOSE, "verbose", false, "be verbose");
        options.addOption(O_BUG, "bug", true, "bug number this upgrade is for");
        options.addOption(O_DESCRIBE, "desc", true, "describe this upgrade task");
        options.addOption(O_DESCRIBE_ALL, "descAll", false, "describe all upgrade tasks");
        return options;
    }
    
    private static String getCommandUsage() {
        return LdapUpgrade.class.getCanonicalName() + " <options> [args]";
    }
    
    private static void usage() {
        usage(null, null, null);
    }
    
    static void usage(ParseException e, UpgradeOp upgradeOp, String errMsg) {
        
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
    
    static UpgradeOp getUpgradeOpUnitTest(String bug) throws ServiceException {
        return getUpgradeOp(bug,  printer);
    }
    
    private static UpgradeOp getUpgradeOp(String bug, LdapUpgradePrinter printer) 
    throws ServiceException {
        UpgradeTask upgradeTask = UpgradeTask.getTaskByBug(bug);
        
        if (upgradeTask == null) {
            printer.println("unrecognized bug number");
            System.exit(1);
        } 
        
        UpgradeOp upgradeOp = upgradeTask.getUpgradeOp();
        upgradeOp.setPrinter(printer);
        upgradeOp.setBug(bug);
        upgradeOp.setLdapProv(LdapProv.getInst());
        return upgradeOp;
    }
    
    // public for unittest
    public static void upgrade(String[] args) throws ServiceException {
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

        if (cl.hasOption(O_DESCRIBE)) {
            String bug = cl.getOptionValue(O_DESCRIBE);
            UpgradeOp upgradeOp = getUpgradeOp(bug, printer);
            upgradeOp.describe();
            System.exit(0);
        }
        
        if (cl.hasOption(O_DESCRIBE_ALL)) {
            for (UpgradeTask task : UpgradeTask.values()) {
                String bug = task.getBug();
                UpgradeOp upgradeOp = getUpgradeOp(bug, printer);
                upgradeOp.describe();
            }
            System.exit(0);
        }
        
        if (!cl.hasOption(O_BUG)) {
            usage();
            System.exit(1);
        }
        
        boolean verbose = cl.hasOption(O_VERBOSE);
        String bug = cl.getOptionValue(O_BUG);
        
        UpgradeOp upgradeOp = getUpgradeOp(bug, printer);
        upgradeOp.setVerbose(verbose);
                
        if (!upgradeOp.parseCommandLine(cl)) {
            System.exit(1);
        }
        
        upgradeOp.doUpgrade();
        
        printer.println("\n\n--------------");
        printer.println("done " + bug);
        printer.println("--------------");
    }
    
    /*
     * zmjava com.zimbra.cs.account.ldap.upgrade.LdapUpgrade -b <bug number>
     * 
     * or 
     * 
     * zmldapupgrade -b <bug number>
     */
    public static void main(String[] args) throws ServiceException {
        if (LdapClient.isLegacy()) {
            // delegate to the legacy package
            com.zimbra.cs.account.ldap.upgrade.legacy.LegacyLdapUpgrade.legacyMain(args);
        } else {
            upgrade(args);
        }
    }
}
