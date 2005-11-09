/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
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
 * Created on May 26, 2004
 */
package com.zimbra.soap;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.dom4j.DocumentException;

import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.SoapHttpTransport;

/**
 * @author schemers
 */
public class SoapTest {

	   public static void usage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(SoapTest.class.getName(), options); 
        System.exit(1);
    }
    
    public static void main(String args[]) 
       throws SoapFaultException, IOException, UnsupportedEncodingException, 
	   DocumentException {

        Zimbra.toolSetup();

        CommandLineParser parser = new GnuParser();
        Options options = new Options();
        
        options.addOption("h", "help", false, "print usage");
        options.addOption("r", "raw", false, "file is a raw soap message");
        options.addOption("d", "debug", false, "debug");
        
        Option fileOpt = new Option("f", "file", true, "input document");
        fileOpt.setArgName("request-document");
        fileOpt.setRequired(true);
        options.addOption(fileOpt);

        Option urlOpt = new Option("u", "uri", true, "uri of soap service");
        urlOpt.setArgName("soap-uri");
        urlOpt.setRequired(true);
        options.addOption(urlOpt);
        
        Option authOpt = new Option("a", "authToken", true, "authToken");
        authOpt.setArgName("auth-token");
        authOpt.setRequired(false);
        options.addOption(authOpt);
        
        Option idOpt = new Option("i", "id", true, "account id");
        idOpt.setArgName("account-id");
        idOpt.setRequired(false);
        options.addOption(idOpt);

        CommandLine cl = null;
        boolean err = false;
        try {
            cl = parser.parse(options, args);
        } catch (ParseException pe) {
            System.err.println("error: "+pe.getMessage());
            err = true;
        }
        
        if (err || cl.hasOption("help"))
            usage(options);

        boolean debug = cl.hasOption("d");
        boolean isRaw = cl.hasOption("r");
        String file = cl.getOptionValue("f");
        String uri = cl.getOptionValue("u");

        SoapHttpTransport trans = new SoapHttpTransport(uri);
        String authToken = cl.getOptionValue("a");
        String mailboxId = cl.getOptionValue("i");
        
        if (authToken != null || mailboxId != null) {
        	if (authToken != null)
        		trans.setAuthToken(authToken);
        	/*if (mailboxId != null)
        		lc.setMailboxId(Long.parseLong(mailboxId));
        		*/
        }

        String docStr = new String(ByteUtil.getContent(new File(file)), "utf-8");

        System.out.println("----");
        System.out.println(docStr);
        System.out.println("----");

		Element request = Element.parseXML(docStr);
        Element response;
        if (isRaw)
            response = trans.invokeRaw(request);
        else
            response = trans.invoke(request);

		System.out.println(response.prettyPrint());
		System.out.println("----");

		trans.shutdown();
    }
}
