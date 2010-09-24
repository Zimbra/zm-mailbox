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

package com.zimbra.common.localconfig;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.dom4j.DocumentException;

import com.zimbra.common.util.RandomPassword;

public class Main {
    
    private static Options mOptions = new Options();
    
    private static void usage() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(Main.class.getName() + " [options] [args]",
                            "where [options] are:", mOptions, "");
        System.exit(0);
    }

    private static void error(String errmsg, Exception e) {
        Logging.error(errmsg, e);
        System.exit(1);
    }

    private static void checkCompatibleOptions(String mainOption, String compatibleOptions, CommandLine cl) {
        Option[] opts = cl.getOptions();
        for (int i = 0; i < opts.length; i++) {
            String clOption = opts[i].getOpt();
            if (!mainOption.equals(clOption) && compatibleOptions.indexOf(clOption) == -1) {
                if (mainOption.equals("")) {
                    error("invalid option '" + clOption + "'", null);
                } else {
                    error("option '" + clOption + "' can not be used with option '" + mainOption + "'", null);
                }
            }
        }
    }

    static {
        mOptions.addOption("c", "config",  true,  "File in which configuration is stored.");
        mOptions.addOption("p", "path",    false, "Show which configuration file will be used.");
        mOptions.addOption("e", "edit",    false, "Edit configuration file, changing keys and values specified. [args] is in key=value form.");
        mOptions.addOption("r", "random",  false, "Used with the edit option, sets specified key to random password string");
        mOptions.addOption("d", "default", false, "Show default values for keys listed in [args].");
        mOptions.addOption("n", "changed", false, "Show values for only those keys listed in [args] that have been changed from their defaults.");
        mOptions.addOption("i", "info",    false, "Show documentation for keys listed in [args].");
        mOptions.addOption("x", "expand",  false, "Expand values.");
        mOptions.addOption("s", "show",    false, "Force display of password strings.");
        mOptions.addOption("f", "force",   false, "Allow editing of keys whose change is known to be potentially dangerous.");
        mOptions.addOption("m", "format",  true,  "Show values in one of these formats: plain (default), xml, shell, export, nokey.");
        mOptions.addOption("q", "quiet",   false, "Suppress logging.");
        mOptions.addOption("u", "unset",   false, "Remove a configuration key.  If this is a key with compiled in defaults, set its value to the empty string.");
        mOptions.addOption("h", "help",    false, "Show this usage information.");
    }

    public static void main(String args[]) {
        Logging.setUseZimbraLog(false);
        
        CommandLine cl = null;
        CommandLineParser parser = new GnuParser();
        try {
            cl = parser.parse(mOptions, args);
        } catch (ParseException pe) {
            Logging.error("Failed to parse command line: " + pe);
            System.exit(1);
        }

        if (cl.hasOption("q")) {
            Logging.setQuietMode(true);
        }
        
        if (cl.hasOption("h")) {
            usage();
        }
        
        // info/docs
        if (cl.hasOption("i")) {
            checkCompatibleOptions("i", "q", cl);
            LocalConfig.printDoc(System.out, cl.getArgs());
            return;
        }

        LocalConfig lc = null;
        try {
            lc = new LocalConfig(cl.getOptionValue("c"));
        } catch (DocumentException de) {
            error("failed when reading config file", de);
        } catch (ConfigException ce) {
            error("failed with error in config file" , ce);
        }
        
        // edit
        if (cl.hasOption("e")) { 
            checkCompatibleOptions("e", "qfrc", cl);
            String[] av = cl.getArgs();
            if (av == null || av.length == 0) {
                error("insufficient arguments", null);
            }
            for (int i = 0; i < av.length; i++) {
                String key = null;
                String value = null;
                if (cl.hasOption("r")) {
                    key = av[i];
                    value = RandomPassword.generate();
                } else {
                    int eqidx = av[i].indexOf("=");
                    if (eqidx <= 0) { 
                        // <= 0 also catches first char being =, ie no key specified
                        error("argument '" + av[i] + "' not in key=value form", null);
                    }
                    key = av[i].substring(0, eqidx);
                    value = av[i].substring(eqidx + 1, av[i].length());
                }
                if (KnownKey.needForceToEdit(key) && !cl.hasOption("f")) {
                   error("can not edit key " + key, null);
                }
                lc.set(key, value);
            }
            try {
                lc.save();
            } catch (Exception e) {
                error("save to " + lc.getConfigFile() + " failed", e);
            }
            return;
        }

        // unset
        if (cl.hasOption("u")) { 
            checkCompatibleOptions("u", "qfc", cl);
            String[] av = cl.getArgs();
            if (av == null || av.length == 0) {
                error("insufficient arguments", null);
            }
            for (int i = 0; i < av.length; i++) {
                String key = av[i];
                if (!lc.isSet(key)) {
                    error("key " + key + " is not set", null);
                }
                lc.remove(key);
            }
            try {
                lc.save();
            } catch (Exception e) {
                error("save to " + lc.getConfigFile() + " failed", e);
            }
            return;
        }

        
        // show path
        if (cl.hasOption("p")) {
            checkCompatibleOptions("p", "qc", cl);
            System.out.println(lc.getConfigFile());
            return;
        }
        
        // print values
        String format = cl.getOptionValue("m");
        ConfigWriter cwriter = null;
        try {
            cwriter = ConfigWriter.getInstance(format, cl.hasOption("x"), !cl.hasOption("s"));
        } catch (ConfigException iae) {
            error("failed to create writer " + format, iae);
        }
        
        try {
            // changed
            if (cl.hasOption("n")) {
                checkCompatibleOptions("n", "qscmx", cl);
                lc.printChanged(System.out, cwriter, cl.getArgs());
                return;
            }
            
            // default 
            if (cl.hasOption("d")) {
                checkCompatibleOptions("d", "qscmx", cl);
                lc.printDefaults(System.out, cwriter, cl.getArgs());
                return;
            } 
            
            // current
            checkCompatibleOptions("", "qscmx", cl);
            lc.print(System.out, cwriter, cl.getArgs());
        } catch (Exception e) {
            error("exception occurred when printing", e);
        }
    }   
}
