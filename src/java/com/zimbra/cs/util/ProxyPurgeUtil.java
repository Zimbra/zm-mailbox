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
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.common.service.ServiceException;

/** @author mansoor peerbhoy 
 */
public class ProxyPurgeUtil
{
    static final String memcachedPort = "11211";
    
    public static void main (String[] args) throws ServiceException
    {
        CommandLine         commandLine;
        ArrayList<String>   servers;
        ArrayList<String>   accounts;
        String              outputformat;
        boolean             purge = false;
        Provisioning        prov;
        List<Server>        memcachedServers;
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
        /* -i (info) indicates that account route info should be printed
        -p (purge) indicates that account route info should be purged
        */
        purge = (commandLine.hasOption ("i") == false);

        /* parse the format string */
        if (commandLine.hasOption ("o")) {
            outputformat = commandLine.getOptionValue ("o");
        } else {
            outputformat = "[%1$s] %2$s -- %3$s";
        }
        
        purgeAccounts(servers, accounts, purge, outputformat);
    }
    
    /**
     * Purges or, prints all the routes for the accounts supplied. 
     * @param servers list of memcached servers supplied, if null the function gets all the  
     *                memcached servers from provisioning
     * @param accounts list of accounts (qualified or, unqualified)
     * @param purge true for the account routes purging, false for printing the routes
     * @param outputformat format of the output in case of printing
     * @throws ServiceException 
     */
    public static void purgeAccounts(List<String> servers, List<String> accounts, boolean purge, String outputformat) throws ServiceException {
        
        Provisioning prov = Provisioning.getInstance();
        
        // Some sanity checks. 
        if (accounts == null || accounts.isEmpty()) {
            System.err.println("No account supplied");
            System.exit(1);
        }
        
        if (!purge) {
            // the outputformat must be supplied. 
            if (outputformat == null || outputformat.length() == 0) {
                System.err.println("outputformat must be supplied for info");
                System.exit(1);
            }
        }
        
        if (servers == null) {
            List<Server> memcachedServers = prov.getAllServers(Provisioning.SERVICE_MEMCACHED);
            servers = new ArrayList<String> ();
            
            for (Iterator<Server> it=memcachedServers.iterator(); it.hasNext();) {
                Server s = it.next();
                String serverName = s.getAttr (Provisioning.A_zimbraServiceHostname, "localhost");
                String servicePort = s.getAttr (Provisioning.A_zimbraMemcachedBindPort, memcachedPort);
                servers.add (serverName + ":" + servicePort);
            }
            
        }
        
        // Connect to all memcached servers.
        int numServers = servers.size();
        ArrayList<ZimbraMemcachedClient> zmcs = new ArrayList<ZimbraMemcachedClient>();
        
        for (int i = 0; i < numServers; ++i) {
            ZimbraMemcachedClient zmc = new ZimbraMemcachedClient();
            zmc.connect(new String[] { servers.get(i) }, false, null, 0, 5000);
            zmcs.add(zmc);
        }
        
        for (String a: accounts) {
            // Bug 24463
            // The route keying in memcached is governed by the following rules: 
            // 1. if login name is fully qualified, use that as the route key
            // 2. otherwise, if memcache_entry_allow_unqualified is true, then use the bare login as the route key
            // 3. else, append the IP address of the proxy interface to the login and use that as the route key
            // 4. for the login store all the user's alias, append the ip address of the proxy interface. 
            //
            // For accounts authenticating without domain, NGINX internally suffixes @domain
            // to the login name, by first looking up an existing domain by the IP address of
            // the proxy interface where the connection came in. If no such domain is found,
            // then NGINX falls back to the default domain name specified by the config
            // attribute zimbraDefaultDomainName.
            // The IP to domain mapping is done based on the zimbraVirtualIPAddress attribute
            // of the domain (The IP-to-domain mapping is a many-to-one relationship.) 
            //
            // For the zmproxypurge utility if the account supplied (-a option) is:
            //    1. For fully qualified account with @domain; it will find all the virtual IP
            //        addresses for that domain and will delete all the entries on all memcached servers:
            //        i) with the user@domain (case 1 as described above) 
            //        ii) with just the user (case 2 as described above) 
            //        iii) with all the virtual IP addresses configured for the domain
            //        iv) find all the alias for the account and repeat (i) to (iii) 
            //    2. For the account supplied with the IP address; the utility will only try to
            //       purge the entries with the user@IP. 
            //    3. If there is a single domain and the account supplied is not fully qualified;
            //       the utility will append the default domain to that entry and will execute step 1.
            //       (In this case the provisioning lookup will return the correct domain)
                        
            ArrayList<String> routes = new ArrayList<String> ();
            
            // Lookup the account; at this point we don't whether the user is fully qualified.
            Account account = prov.get(Provisioning.AccountBy.name, a);
            if (account == null) {
                // In this case just purge the entries with the given account name as supplied.
                System.out.println("error looking up accout: " + a);
                routes.add("route:proto=http;user=" + a);
                routes.add("route:proto=imap;user=" + a);
                routes.add("route:proto=pop3;user=" + a);
            } else {
                String uid = account.getUid();
                routes.add("route:proto=http;id=" + account.getId());
                routes.add("route:proto=http;user=" + uid);
                routes.add("route:proto=imap;user=" + uid);
                routes.add("route:proto=pop3;user=" + uid);
                
                String domain = account.getDomainName();
                routes.add("route:proto=http;user=" + uid + "@" + domain);
                routes.add("route:proto=imap;user=" + uid + "@" + domain);
                routes.add("route:proto=pop3;user=" + uid + "@" + domain);
                routes.add("alias:user=" + uid + ";ip=" + domain);
                
                Domain d = prov.get(DomainBy.name, domain);
                String[] vips = d.getVirtualIPAddress();
                for (String vip : vips) {
                    // for each virtual ip add the routes to the list.
                    routes.add("route:proto=http;user=" + uid + "@" + vip);
                    routes.add("route:proto=imap;user=" + uid + "@" + vip);
                    routes.add("route:proto=pop3;user=" + uid + "@" + vip);
                    routes.add("alias:user=" + uid + ";ip=" + vip);
                }
                
                String[] aliases = account.getMailAlias();
                // for each alias add routes for it's domain and all virtual IPs for that domain
                // I think, all the http routes are stored by user id, or, uid or, uid@domain. 
                // I haven't found any alias in the http routes. Hence skipping it.
                for (String alias : aliases) {
                    routes.add("route:proto=imap;user=" + alias);
                    routes.add("route:proto=pop3;user=" + alias);
                    
                    routes.add("route:proto=imap;user=" + alias + "@" + domain);
                    routes.add("route:proto=pop3;user=" + alias + "@" + domain);
                    routes.add("alias:user=" + alias + ";ip=" + domain);
                    
                    for (String vip : vips) {
                        // for each virtual ip add the routes to the list. 
                        routes.add("route:proto=imap;user=" + alias + "@" + vip);
                        routes.add("route:proto=pop3;user=" + alias + "@" + vip);
                        routes.add("alias:user=" + alias + ";ip=" + vip);
                    }
                }
            }
 
            for (int i = 0; i < numServers; ++i) {
                ZimbraMemcachedClient zmc = zmcs.get(i);
                
                for (String route : routes) {
                    if (purge) {
                        // Note: there is no guarantee that all the routes will be present.
                        // We just try to purge all of them without waiting on ack.
                        System.out.println("Purging " + route + " on server " + servers.get(i));
                        zmc.remove(route, false);
                    } else {
                        String output = String.format(outputformat, servers.get(i), route, zmc.get(route));
                        System.out.println(output);
                    }
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

