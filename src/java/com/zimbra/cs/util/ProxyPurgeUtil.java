/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.memcached.ZimbraMemcachedClient;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.ParseException;
import java.util.*;
import java.io.*;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.common.service.ServiceException;

/** @author mansoor peerbhoy 
 */
public class ProxyPurgeUtil
{
    public static void main (String[] args) throws ServiceException
    {
        CommandLine         commandLine;
        ArrayList<String>   servers;
        ArrayList<String>   accounts;
        ArrayList<ZimbraMemcachedClient> zmcs;
        String              outputformat;
        int                 numServers;
        boolean             purge = false;
        Provisioning        prov;
        List<Server>        memcachedServers;
        final String        memcachedPort = "11211";
        String              logLevel = "ERROR";

        /* Parse the command-line arguments, and display usage if necessary */
        try {
            commandLine = parseCommandLine (args);
        } catch (ParseException pe) {
            commandLine = null;
        }

        if ((commandLine == null) || 
            commandLine.hasOption ("h") || 
            commandLine.hasOption ("u")
           ) {
            usage ();
            System.exit (1);
        }

        if (commandLine.hasOption("v")) { logLevel = "DEBUG"; }
        ZimbraLog.toolSetupLog4j (logLevel, null, false);

        /* Initialize the logging system and the zimbra environment */
        prov = Provisioning.getInstance();

        /* Get the list of servers running the memcached service
           this is equivalent to the $(zmprov gamcs) command
         */
        memcachedServers = prov.getAllServers(Provisioning.SERVICE_MEMCACHED);
        servers = new ArrayList <String> ();

        for (Iterator<Server> it=memcachedServers.iterator(); it.hasNext();) {
            Server s = it.next();
            String serverName = s.getAttr (Provisioning.A_zimbraServiceHostname, "localhost");
            String servicePort = s.getAttr (Provisioning.A_zimbraMemcachedBindPort, memcachedPort);
            servers.add (serverName + ":" + servicePort);
        }

        accounts = getAccounts (commandLine);
        servers.addAll (getCacheServers (commandLine));

        if (servers.size() == 0) {
            System.err.println ("No memcached servers found, and none specified (--help for help)");
            System.exit (1);
        }

        if (accounts.size() == 0) {
            System.err.println ("No accounts specified (--help for help)");
            System.exit (1);
        }

        /* Assume purge unless `-i' is specified */
        purge = (commandLine.hasOption ("i") == false);

        numServers = servers.size();

        // Connect to all memcached servers.
        zmcs = new ArrayList<ZimbraMemcachedClient>();
        for (int i = 0; i < numServers; ++i) {
            ZimbraMemcachedClient zmc = new ZimbraMemcachedClient();
            zmc.connect(new String[] { servers.get(i) }, false, null, 0, 5000);
            zmcs.add(zmc);
        }

        /* parse the format string */
        if (commandLine.hasOption ("o")) {
            outputformat = commandLine.getOptionValue ("o");
        } else {
            outputformat = "[%1$s] %2$s -- %3$s";
        }

        /* -i (info) indicates that account route info should be printed
           -p (purge) indicates that account route info should be purged
         */
        for (String a: accounts) {
            Account account = prov.get(Provisioning.AccountBy.name, a);
            for (int i = 0; i < numServers; ++i) {
                ZimbraMemcachedClient zmc = zmcs.get(i);
                String httpKey = "route:proto=http;" + (account == null ? "user=" + a :
                	"id=" + account.getId());
                String imapKey = "route:proto=imap;user=" + a;
                String pop3Key = "route:proto=pop3;user=" + a;
                if (purge) {
                    zmc.remove(httpKey, false);
                    zmc.remove(imapKey, false);
                    zmc.remove(pop3Key, false);
                } else {
                    Formatter httpf, imapf, pop3f;
                    String server = servers.get(i);

                    httpf = new Formatter ();
                    httpf.format (outputformat, server, httpKey, zmc.get(httpKey));
                    System.out.println (httpf.toString());
                    imapf = new Formatter ();
                    imapf.format (outputformat, server, imapKey, zmc.get(imapKey));
                    System.out.println (imapf.toString());
                    pop3f = new Formatter ();
                    pop3f.format (outputformat, server, pop3Key, zmc.get(pop3Key));
                    System.out.println (pop3f.toString());
                }
            }
        }

        for (ZimbraMemcachedClient zmc : zmcs) {
            zmc.disconnect(ZimbraMemcachedClient.DEFAULT_TIMEOUT);
        }
    }

    /** Extract the account names
     *  @param  commandLine Command Line object (org.apache.commons.cli.CommandLine)
     *  @return ArrayList containing the account names specified on the command line (-a or -L)
     */
    static ArrayList<String> getAccounts (CommandLine commandLine)
    {
        ArrayList<String>   accounts = new ArrayList<String> ();
        String[]            values = commandLine.getOptionValues ("a");
        String              filename = commandLine.getOptionValue ("L");

        /* Start off with any account specified with `-a' */
        if (values != null) {
            for (String u: commandLine.getOptionValues ("a")) {
                accounts.add (u);
            }
        }

        /* Other accounts may be read from a list file specified with -L 
           Each line of input will contain one account name
         */
        if (filename != null) {
            BufferedReader br;
            if (filename == "-") {
                br = new BufferedReader (new InputStreamReader (System.in));
            }
            else {
                try {
                    br = new BufferedReader (new FileReader (filename));
                } catch (FileNotFoundException e) {
                    br = null;
                    System.err.println ("File not found: " + filename);
                }
            }

            if (br != null) {
                String s;
                do {
                    try { s = br.readLine(); }
                    catch (IOException e) { 
                        s = null; 
                    }

                    if (s != null) {
                        accounts.add (s);
                    }
                } while (s != null);

                try { br.close (); }
                catch (IOException e) { }
            }
        }

        return accounts;
    }

    /** Extract the account names
     *  @param  commandLine     Command Line object (org.apache.commons.cli.CommandLine)
     *  @return An ArrayList containing the server names specified in the command line
     */
    static ArrayList<String> getCacheServers (CommandLine commandLine)
    {
        ArrayList<String>   servers = new ArrayList<String> ();
        String[]            values = commandLine.getArgs ();

        if (values != null) {
            for (String s: commandLine.getArgs ()) {
                servers.add (s);
            }
        }

        return servers;
    }

    /** Parse command line arguments
        @param  args    An array of strings representing the command line arguments
        @return         An instance of org.apache.commons.cli.CommandLine
        @throws         ParseException
     */
    static CommandLine parseCommandLine (String[] args)
        throws ParseException
    {
        CommandLineParser   parser;
        Options             options;

        parser = new GnuParser ();
        options = new Options ();

        options.addOption ("h", "help", false, "print usage");
        options.addOption ("u", "usage", false, "print usage");
        options.addOption ("v", "verbose", false, "be verbose");
        options.addOption ("i", "info", false, "display route information");
        options.addOption ("a", "account", true, "account name");
        options.addOption ("L", "list", true, "file containing list of accounts, one per line");
        options.addOption ("o", "output", true, "format for displaying routing information");

        return parser.parse (options, args);
    }

    /** Display program usage */
    static void usage ()
    {
        System.err.println (" ");
        System.err.println (" Purges POP/IMAP routing information from one or more memcached servers");
        System.err.println (" Available Memcached servers are discovered by the 'zmprov gamcs' function, others can be specified");
        System.err.println (" If necessary, please specify additional memcached servers in the form of server:port at the end of the command line");
        System.err.println (" ");
        System.err.println (" Usage: zmproxypurge [-v] [-i] -a account [-L accountlist] [cache1 [cache2 ... ]]");
        System.err.println ("  -h, --help     Display help");
        System.err.println ("  -v, --verbose  Be verbose");
        System.err.println ("  -i, --info     Just display account routing information, don't purge (dry run)");
        System.err.println ("  -a, --account  Account name");
        System.err.println ("  -L, --list     File containing list of accounts, one per line");
        System.err.println ("                 Use `-' to indicate standard input");
        System.err.println ("  -o, --output   Format to be used to print routing information with -i");
        System.err.println ("                 Three fields are displayed by default:");
        System.err.println ("                 . the cache server");
        System.err.println ("                 . the account name");
        System.err.println ("                 . the route information");
        System.err.println ("                 Default format is `[%1$s] %2$s -- %3$s'");
        System.err.println ("  cacheN         (Optional) Additional memcache server, of the form server:port");
        System.err.println (" ");
    }

}

