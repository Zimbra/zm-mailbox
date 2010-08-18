/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.dom4j.DocumentException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.ZimbraLog;

public abstract class LocalConfigUpgrade {
    private static String O_HELP = "h";
    private static String O_BUG = "b";
    private static String O_CONFIG = "c";
    private static String O_TAG = "t";
    
    private static Options getAllOptions() {
        Options options = new Options();
        options.addOption(O_HELP, "help", false, "print usage");
        options.addOption(O_CONFIG, "config", true, "path to localconfig.xml");
        options.addOption(O_TAG, "tag", true, "backup and tag with this suffix");
        Option bugOpt = new Option(O_BUG, "bug", true,
                "bug number this upgrade is for (multiple allowed)");
        bugOpt.setArgs(Option.UNLIMITED_VALUES);
        options.addOption(bugOpt);
        return options;
    }
    
    private static String getCommandUsage() {
        return "com.zimbra.common.localconfig.LocalConfigUpgrade <options>";
    }
    
    static void usage(String error) {
        if (error != null)
            System.out.println("Error: " + error);
        Options opts = getAllOptions();
        PrintWriter pw = new PrintWriter(System.out, true);
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(pw, formatter.getWidth(), getCommandUsage(),
                            null, opts, formatter.getLeftPadding(), formatter.getDescPadding(),
                            null);
        pw.flush();
        if (error != null) {
            System.exit(1);
        } else {
            System.exit(0);
        }
    }

    private final String mBug;
    private final String mShortName;
    
    LocalConfigUpgrade(String bug, String shortName) {
	mBug = bug;
	mShortName = shortName;
	sUpgrades.put(bug, this);
    }
    
    protected abstract void upgrade(LocalConfig lc) throws ConfigException;

    private static final boolean TRACE = false;
    
    private static boolean hasOption(String option, String value) {
	if (TRACE) System.out.println("hasOption: option=" + option);
	if (TRACE) System.out.println("hasOption: value=" + value);
	
	// option is in the middle
	String mre = "^.*\\s+\\Q" + value + "\\E\\s+.*$";
	if (option.matches(mre)) {
	    if (TRACE) System.out.println("hasOption: matched mre=" + mre);
	    return true;
	}
	
	// option is in the end
	String ere = "^.*\\s+\\Q" + value + "\\E\\s*$";
	if (option.matches(ere)) {
	    if (TRACE) System.out.println("hasOption: matched ere=" + ere);
	    return true;
	}

	// option is in the beginning
	String bre = "^\\s*\\Q" + value + "\\E\\s+.*$";
	if (option.matches(bre)) {
	    if (TRACE) System.out.println("hasOption: matched bre=" + bre);
	    return true;
	}
	
	if (TRACE) System.out.println("hasOption: matched none");
	return false;
    }
    
    private static boolean hasOptionThatBeginsWith(String option, String value) {
	if (TRACE) System.out.println("hasOptionBegins: option=" + option);
	if (TRACE) System.out.println("hasOptionBegins: value=" + value);
	
	// at middle or end
	String moere = "^.*\\s+\\Q" + value + "\\E.*$";
	if (option.matches(moere)) {
	    if (TRACE) System.out.println("hasOptionBegins: matched moere=" + moere);
	    return true;
	}
	
	// at beginning
	String ere = "^\\s*\\Q" + value + "\\E.*$";
	if (option.matches(ere)) {
	    if (TRACE) System.out.println("hasOptionBegins: matched ere=" + ere);
	    return true;
	}
	
	if (TRACE) System.out.println("hasOptionBegins: matched none");
	return false;
    }
    
    private static String appendOptionIfNotPresent(String mjo, String option) {
	if (hasOption(mjo, option)) {
	    return mjo;
	}
	return mjo + " " + option; 
    }
    
    private static String prependOption(String mjo, String option) {
	if (hasOption(mjo, option)) {
	    return mjo;
	}
	return option + " " + mjo; 
    }
    
    private static String appendOptionIfNoOptionBeginsWith(String mjo, String match, String option) {
	if (hasOptionThatBeginsWith(mjo, match)) {
	    return mjo;
	}
	return mjo + " " + option;
    }
    
    private static String removeOption(String mjo, String option) {
	// option is in the middle
	mjo = mjo.replaceAll("\\s+\\Q" + option + "\\E\\s+", " ");
	
	// option is in the end
	mjo = mjo.replaceAll("\\s+\\Q" + option + "\\E\\s*$", "");
	
	// option is in the beginning
	mjo = mjo.replaceAll("^\\s*\\Q" + option + "\\E\\s+", "");
	
	// only option is present
	mjo = mjo.replaceAll("^\\s*\\Q" + option + "\\E\\s*$", "");
	return mjo;
    }

    private static String removeOptionWithValue(String mjo, String option) {
        return mjo.replaceAll("\\s*\\Q" + option + "=\\E\\w+", "");
    }

    private static class LocalConfigUpgradeSwitchToCMS extends LocalConfigUpgrade {
	
	LocalConfigUpgradeSwitchToCMS(String bug) {
	    super(bug, "SwitchToCMS");
	}
	
	public static void main(String[] args) {
	    StringBuilder sb = new StringBuilder();
	    for (int i = 0; i < args.length; i++) {
		sb.append(args[i]);
		if (i < (args.length-1)) {
		    sb.append(' ');
		}
	    }
	    String mjo = sb.toString();
	    System.out.println("before: " + mjo);
	    System.out.println("after: " + new LocalConfigUpgradeSwitchToCMS("testbug").modify(mjo));
	}
	
	@Override
	protected void upgrade(LocalConfig lc) throws ConfigException {
	    String key = LC.mailboxd_java_options.key();
	    String mjo = lc.getRaw(key);
	    System.out.println("Old " + key + "=" + mjo);
	    mjo = modify(mjo);
	    System.out.println("New " + key + "=" + mjo);
	    lc.set(key, mjo);
	}
	
	private String modify(String mjo) {
	    mjo = removeOption(mjo, "-XX:+UseParallelGC");
	    mjo = removeOptionWithValue(mjo, "-XX:NewRatio");

	    mjo = appendOptionIfNotPresent(mjo, "-XX:+UseConcMarkSweepGC");
	    mjo = appendOptionIfNotPresent(mjo, "-verbose:gc");
	    mjo = appendOptionIfNotPresent(mjo, "-XX:+PrintGCDetails");
	    mjo = appendOptionIfNotPresent(mjo, "-XX:+PrintGCTimeStamps");
	    mjo = appendOptionIfNotPresent(mjo, "-XX:+PrintGCApplicationStoppedTime");
	    
	    mjo = appendOptionIfNoOptionBeginsWith(mjo, "-XX:PermSize=", "-XX:PermSize=128m");
	    mjo = appendOptionIfNoOptionBeginsWith(mjo, "-XX:MaxPermSize=", "-XX:MaxPermSize=128m");
	    mjo = appendOptionIfNoOptionBeginsWith(mjo, "-XX:SoftRefLRUPolicyMSPerMB=", "-XX:SoftRefLRUPolicyMSPerMB=1"); 
	    return mjo;
	}
    }
    
    private static class LocalConfigUpgradeSwitchToServerJVM extends LocalConfigUpgrade {
	LocalConfigUpgradeSwitchToServerJVM(String bug) {
	    super(bug, "SwitchToServerJVM");
	}
	
	public static void main(String[] args) {
	    StringBuilder sb = new StringBuilder();
	    for (int i = 0; i < args.length; i++) {
		sb.append(args[i]);
		if (i < (args.length-1)) {
		    sb.append(' ');
		}
	    }
	    String mjo = sb.toString();
	    System.out.println("before: " + mjo);
	    System.out.println("after: " + new LocalConfigUpgradeSwitchToServerJVM("testme").modify(mjo));
	}
	
	@Override
	protected void upgrade(LocalConfig lc) throws ConfigException {
	    String key = LC.mailboxd_java_options.key();
	    String mjo = lc.getRaw(key);
	    System.out.println("Old " + key + "=" + mjo);
	    mjo = modify(mjo);
	    System.out.println("New " + key + "=" + mjo);
	    lc.set(key, mjo);
	}

	private String modify(String mjo) {
	    mjo = removeOption(mjo, "-client");
	    mjo = removeOption(mjo, "-server");
	    mjo = prependOption(mjo, "-server");
	    return mjo;
	}
    }
    
    private static class LocalConfigUpgradeUnsetMailboxdJavaHome extends LocalConfigUpgrade {
	public LocalConfigUpgradeUnsetMailboxdJavaHome(String bug) {
	    super(bug, "UnsetMailboxdJavaHome");
	}
	
	@Override
	protected void upgrade(LocalConfig lc) {
	    lc.remove("mailboxd_java_home");
	}
    }
    
    private final static HashMap<String,LocalConfigUpgrade> sUpgrades;
    
    static {
	sUpgrades = new HashMap<String,LocalConfigUpgrade>();
	new LocalConfigUpgradeSwitchToCMS("37842");
	new LocalConfigUpgradeSwitchToServerJVM("37844");
	new LocalConfigUpgradeUnsetMailboxdJavaHome("37802");
    }
    
    public static void main(String[] args) throws ServiceException {
        CliUtil.toolSetup();
        
        CommandLine cl = null;
        try {
            CommandLineParser parser = new GnuParser();
            Options options = getAllOptions();
            cl = parser.parse(options, args);
            if (cl == null)
                throw new ParseException("");
        } catch (ParseException pe) {
            usage(pe.getMessage());
        }
        
        if (cl.hasOption(O_HELP)) {
            usage(null);
        }

        if (!cl.hasOption(O_BUG)) {
            usage("no bug specified");
        }

        if (!cl.hasOption(O_TAG)) {
            usage("no backup suffix tag specified");
        }
        
        LocalConfig lc = null;
        try {
            lc = new LocalConfig(cl.getOptionValue("c"));
            lc.backup(cl.getOptionValue("t"));
        } catch (DocumentException de) {
            ZimbraLog.misc.error("failed reading config file", de);
            System.exit(1);
        } catch (ConfigException ce) {
            ZimbraLog.misc.error("failed reading config file" , ce);
            System.exit(2);
        } catch (IOException ioe) {
            ZimbraLog.misc.error("failed to backup config file" , ioe);
            System.exit(3);
	}

        String[] bugs = cl.getOptionValues(O_BUG);
        for (String bug : bugs) {
            if (!sUpgrades.containsKey(bug)) {
        	ZimbraLog.misc.warn("local config upgrade can't handle bug " + bug);
        	continue;
            }
            
            LocalConfigUpgrade lcu = sUpgrades.get(bug);
            System.out.println("== Running local config upgrade for bug " + lcu.mBug + " (" + lcu.mShortName + ")");
            try {
		lcu.upgrade(lc);
		System.out.println("== Done local config upgrade for bug " + lcu.mBug + " (" + lcu.mShortName + ")");
	    } catch (ConfigException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
        }
        
        try {
            lc.save();
        } catch (IOException ioe) {
            ZimbraLog.misc.error("failed writing config file", ioe);
            System.exit(1);
        } catch (ConfigException ce) {
            ZimbraLog.misc.error("failed writing config file", ce);
            System.exit(1);
        }
    }
}
