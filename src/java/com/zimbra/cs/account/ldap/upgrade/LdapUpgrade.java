/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009 Zimbra, Inc.
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
package com.zimbra.cs.account.ldap.upgrade;

import java.io.PrintWriter;
import java.util.Map;

import javax.naming.NamingException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapEntry;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.account.ldap.ZimbraLdapContext;

abstract class LdapUpgrade {
    
    protected String mBug;
    protected boolean mVerbose;
    protected LdapProvisioning mProv;
    
    LdapUpgrade(String bug, boolean verbose) throws ServiceException {
        mBug = bug;
        mVerbose = verbose;
        
        Provisioning prov = Provisioning.getInstance();
        if (!(prov instanceof LdapProvisioning))
            throw ServiceException.FAILURE("Provisioning is not instance of LdapProvisioning", null);
        else
            mProv = (LdapProvisioning)prov;
    };
    
    abstract void doUpgrade() throws ServiceException;
    
    boolean parseCommandLine(CommandLine cl) { return true; }
    void usage(HelpFormatter helpFormatter) {}
    
    static void modifyAttrs(Entry entry, ZimbraLdapContext initZlc, Map attrs) throws NamingException, ServiceException {
        ZimbraLdapContext zlc = initZlc;
        try {
            if (zlc == null)
                zlc = new ZimbraLdapContext(true);
            LdapUtil.modifyAttrs(zlc, ((LdapEntry)entry).getDN(), attrs, entry);
        } finally {
            if (initZlc == null)
                ZimbraLdapContext.closeContext(zlc);
        }
    }  
    
    abstract static class UpgradeVisitor {
        boolean mVerbose;
        LdapProvisioning mProv;
        ZimbraLdapContext mZlcForMod;
        
        UpgradeVisitor(LdapProvisioning prov, ZimbraLdapContext zlcForMod, boolean verbose) {
            mVerbose = verbose;
            mProv = prov;
            mZlcForMod = zlcForMod;
        }
        
        abstract void reportStat();
    }
    
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
        return "com.zimbra.cs.util.LdapUpgrade <options> [args]";
    }
    
    static void usage() {
        usage(null, null, null);
    }
    
    static void usage(ParseException e, LdapUpgrade ldapUpgrade, String errMsg) {
        if (e != null)
            System.out.println("Error parsing command line arguments: " + e.getMessage());
        
        if (errMsg != null) {
            System.out.println(errMsg);
            System.out.println();
        }

        Options opts = getAllOptions();
        PrintWriter pw = new PrintWriter(System.out, true);
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(pw, formatter.getWidth(), getCommandUsage(),
                            null, opts, formatter.getLeftPadding(), formatter.getDescPadding(),
                            null);
        
        pw.flush();
        if (ldapUpgrade != null)
            ldapUpgrade.usage(formatter);
    }
    
    /**
     * Usage: zmjava com.zimbra.cs.account.ldap.upgrade.LdapUpgrade -b <bug number> [-v]
     *
    * @param args
     */
    public static void main(String[] args) throws ServiceException {
        CliUtil.toolSetup();
        
        CommandLine cl = null;
        try {
            CommandLineParser parser = new GnuParser();
            Options options = getAllOptions();
            cl = parser.parse(options, args);
            if (cl == null)
                throw new ParseException("");
        } catch (ParseException e) {
            LdapUpgrade.usage(e, null, null);
            System.exit(1);
        }
        
        if (cl.hasOption(O_HELP)) {
            LdapUpgrade.usage();
            System.exit(1);
        }

        if (!cl.hasOption(O_BUG)) {
            LdapUpgrade.usage();
            System.exit(1);
        }
        
        boolean verbose = cl.hasOption(O_VERBOSE);
        
        String bug = cl.getOptionValue(O_BUG);
        LdapUpgrade upgrade = null;
        
        if ("14531".equalsIgnoreCase(bug)) {
            upgrade = new GalLdapFilterDef(bug, verbose);
        } else if ("18277".equalsIgnoreCase(bug)) {
            upgrade = new MigrateDomainAdmins(bug, verbose);
        } else if ("22033".equalsIgnoreCase(bug)) {
            upgrade = new SetZimbraCreateTimestamp(bug, verbose);
        } else if ("27075".equalsIgnoreCase(bug)) {
            // e.g. -b 27075 5.0.12
            upgrade = new SetCosAndGlobalConfigDefault(bug, verbose);
        } else if ("29978".equalsIgnoreCase(bug)) {
            upgrade = new DomainPublicServiceProtocolAndPort(bug, verbose);
//        } else if ("31284".equalsIgnoreCase(bug)) {
//            upgrade = new SetZimbraPrefFromDisplay(bug, verbose);
        } else if ("31694".equalsIgnoreCase(bug)) {
            upgrade = new MigrateZimbraMessageCacheSize(bug, verbose);
        } else if ("32557".equalsIgnoreCase(bug)) {
            upgrade = new DomainObjectClassAmavisAccount(bug, verbose);
        } else if ("32719".equalsIgnoreCase(bug)) {
            upgrade = new HsmPolicy(bug, verbose);
        } else if ("33814".equalsIgnoreCase(bug)) {
            // note: has to be run *before* running -b 27075
            upgrade = new MigrateZimbraMtaAuthEnabled(bug, verbose);
        } else {
            System.out.println("unrecognized bug number");
            System.exit(1);
        }
        
        if (!upgrade.parseCommandLine(cl)) {
            System.exit(1);
        }
        
        upgrade.doUpgrade();
        
        System.out.println("\n\n--------------");
        System.out.println("all done");
    }

}
