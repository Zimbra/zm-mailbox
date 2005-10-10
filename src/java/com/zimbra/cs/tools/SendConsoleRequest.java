/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Feb 18, 2005
 */
package com.zimbra.cs.tools;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.net.URL;

import org.apache.commons.cli.Option;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.account.AccountService;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraContext;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.SoapHttpTransport;
import com.zimbra.soap.SoapTransport;
import com.zimbra.cs.service.mail.*;
import com.zimbra.cs.servlet.ZimbraServlet;


/**
 * @author tim
 *
 * General-Purpose Util for sending the Console SOAP request -- hook for 
 * testing/debugging type things...
 * 
 */
public class SendConsoleRequest {
    public static void usage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(SendConsoleRequest.class.getName(), options); 
        System.exit(1);
    }

    private static CommandLine parseCmdlineArgs(String args[], Options options) {
        CommandLineParser parser = new GnuParser();

        Option ropt = new Option("r", "reindex", true, "MailboxId or email address or ALL");
        ropt.setArgs(Option.UNLIMITED_VALUES);
        options.addOption(ropt);
        options.addOption("h", "hostname", true, "Hostname");
        options.addOption("t", "port", true, "Port Number");
        
        Option u = new Option("u", "username", true, "Admin username");
        u.setRequired(true);
        options.addOption(u);
        Option p = new Option("p", "password", true, "Admin password");
        p.setRequired(true);
        options.addOption(p);
        
        CommandLine cl = null;
        boolean err = false;
        try {
            cl = parser.parse(options, args);
        } catch (ParseException pe) {
            System.err.println("error: " + pe.getMessage());
            err = true;
        }
        
        if (err ) {
            usage(options);
        }

        return cl;
    }
    
    private SoapTransport mTrans = null;
    
    private void auth(String name, String pwd, String host, int port)
    throws SoapFaultException, IOException, ServiceException {
        URL src = new URL("http", host, port, ZimbraServlet.USER_SERVICE_URI);
        mTrans = new SoapHttpTransport(src.toExternalForm());

        // gonna go with XML here; may want to switch to a binary protocol at some point
        Element request = new Element.XMLElement(AccountService.AUTH_REQUEST);
        request.addAttribute(AccountService.E_ACCOUNT, name, Element.DISP_CONTENT);
        request.addAttribute(AccountService.E_PASSWORD, pwd, Element.DISP_CONTENT);
        Element response = mTrans.invokeWithoutSession(request);

        System.out.println(response.prettyPrint());

        // get the auth token out, no default, must be present or a service exception is thrown
        String authToken = response.getAttribute(AccountService.E_AUTH_TOKEN);
        // get the session id, if not present, default to null
        String sessionId = response.getAttribute(ZimbraContext.E_SESSION_ID, null);

        // set the auth token and session id in the transport for future requests to use
        mTrans.setAuthToken(authToken);
        if (sessionId != null)
            mTrans.setSessionId(sessionId);
    }
    
    private void reIndex(String mbox) throws SoapFaultException, IOException {
        // gonna go with XML here; may want to switch to a binary protocol at some point
        Element req = new Element.XMLElement(MailService.CONSOLE_REQUEST);
        req.addAttribute(MailService.A_NAME, "reindex");
        req.addElement(MailService.E_PARAM).addAttribute(MailService.A_NAME, "id").setText(mbox);

        Element resp = mTrans.invokeWithoutSession(req);
        System.out.println(resp.prettyPrint());
    }
    
    
    
    public static void main(String args[]) 
    {
        Zimbra.toolSetup();
        
        // command line argument parsing
        Options options = new Options();
        CommandLine cl = parseCmdlineArgs(args, options);

        String[] reindex = cl.getOptionValues("r");
        String hostname = cl.getOptionValue("h", "localhost");
        
        String user = cl.getOptionValue("u");
        String pwd = cl.getOptionValue("p");
        
        int port = 0;
        try {
            port = Integer.parseInt(cl.getOptionValue("p", "0"));
        } catch (NumberFormatException e) {
        }
        if (port == 0) {
            port = 7070;
        }
        
        if (hostname == null || hostname.length() == 0 ||
            port <= 0) {
            usage(options);
        }
        
        SendConsoleRequest main = new SendConsoleRequest();
        
        try {
            main.auth(user, pwd, hostname, port);
            
            if (reindex.length > 0) {
                for (int i = 0; i < reindex.length; i++) {
                    main.reIndex(reindex[i]);
                }
            }
            
        } catch(SoapFaultException e) {
            System.err.println("Caught SoapFaultException: "+e.toString());
            e.printStackTrace();
        } catch(ServiceException e) {
            System.err.println("Caught ServiceException: "+e.toString());
        } catch(IOException e) {
            System.err.println("Caught IOException: "+e.toString());
        }
        
    }
}
