/*
***** BEGIN LICENSE BLOCK *****
Version: ZPL 1.1

The contents of this file are subject to the Zimbra Public License
Version 1.1 ("License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.zimbra.com/license

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
the License for the specific language governing rights and limitations
under the License.

The Original Code is: Zimbra Collaboration Suite.

The Initial Developer of the Original Code is Zimbra, Inc.  Portions
created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
Reserved.

Contributor(s): 

***** END LICENSE BLOCK *****
*/

/*
 * Created on 2004. 6. 17.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.zimbra.cs.tools;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * @author jhahm
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class SendSocketCommand {

    public static void usage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(SendSocketCommand.class.getName(), options); 
        System.exit(1);
    }

    private static CommandLine parseCmdlineArgs(String args[], Options options) {
        CommandLineParser parser = new GnuParser();

        options.addOption("h", "host", true, "hostname (default localhost)");
        options.addOption("p", "port", true, "TCP port number (required)");
        options.addOption("c", "command", true, "command to send (required)");

        CommandLine cl = null;
        boolean err = false;
        try {
            cl = parser.parse(options, args);
        } catch (ParseException pe) {
            System.err.println("error: " + pe.getMessage());
            err = true;
        }
        
        if (err || !cl.hasOption("p") || !cl.hasOption("c"))
            usage(options);

        return cl;
    }
	
	public static void main(String args[]) {

        // command line argument parsing
        Options options = new Options();
        CommandLine cl = parseCmdlineArgs(args, options);

        String command = cl.getOptionValue("c", null);
        String hostname = cl.getOptionValue("h", "localhost");
        int port = 0;
        try {
        	port = Integer.parseInt(cl.getOptionValue("p", "0"));
    	} catch (NumberFormatException e) {
    	}
		
    	if (command == null || command.length() == 0 ||
    		hostname == null || hostname.length() == 0 ||
			port <= 0) {
    		usage(options);
    	}

    	// connect and send command
		Socket s = null;
		try {
			s = new Socket(InetAddress.getByName(hostname), port);
		} catch (UnknownHostException e) {
			System.err.println("Bad address: " + hostname);
			System.exit(2);
		} catch (IOException e) {
			System.err.println("Unable to connect to " + hostname + ":" + port);
			System.exit(3);
		}
		PrintWriter out = null;
		try {
			out = new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
		} catch (IOException e) {
			System.err.println("Unable to send command");
			System.exit(4);
		}
		out.println(command);
        out.flush();
        out.close();
    }
}
