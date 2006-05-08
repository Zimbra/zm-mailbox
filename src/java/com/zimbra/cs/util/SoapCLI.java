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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.net.UnknownHostException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.client.LmcSession;
import com.zimbra.cs.localconfig.LC;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.admin.AdminService;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.SoapHttpTransport;
import com.zimbra.soap.SoapTransport;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * For command line interface utilities that are SOAP clients and need to authenticate with
 * the admin service using credentials from local configuration.
 * <p>
 * This class takes -h,--help for displaying usage, and -s,--server for target server hostname.
 * Subclass can provide additional options. The expected use is similar to the following:
 * <pre>
 *   MyUtil util = new MyUtil();
 *   try {
 *     util.setupCommandLineOptons();
 *     CommandLine cl = util.getCommandLine(args);
 *     if (cl != null) {
 *       if (cl.hasOption(...)) {
 *         util.auth();
 *         util.doMyThing();
 *       } else if (cl.hasOption(...)) {
 *         ...
 *       }
 *     }
 *   } catch (ParseException e) {
 *     util.usage(e);
 *   }
 *     
 * </pre>
 * 
 * @author kchen
 *
 */
public abstract class SoapCLI {
    
    protected static final String O_H = "h";
    protected static final String O_S = "s";

    private String mUser;
    private String mPassword;
    private String mHost;
    private int mPort;
    private boolean mAuth;
    private Options mOptions;
    
    private SoapTransport mTrans = null;
    private String mServerUrl;
    
    protected SoapCLI() throws ServiceException {
        // get admin username from local config
        String userDn = LC.zimbra_ldap_userdn.value();
        mUser = LdapUtil.dnToUid(userDn);
        // get password from localconfig
        mPassword = LC.zimbra_ldap_password.value();
        // host can be specified
        mHost = "localhost";
        // get admin port number from provisioning
        com.zimbra.cs.account.Config conf = null;
        try {
	        conf = Provisioning.getInstance().getConfig();
        } catch (ServiceException e) {
        	throw ServiceException.FAILURE("Unable to connect to LDAP directory", e);
        }
        mPort = conf.getIntAttr(Provisioning.A_zimbraAdminPort, 0);
        if (mPort == 0)
            throw ServiceException.FAILURE("Unable to get admin port number from provisioning", null);
        mOptions = new Options();
    }

    /**
     * Parses the command line arguments. If -h,--help is specified, displays usage and returns null.
     * @param args the command line arguments
     * @return
     * @throws ParseException
     */
    protected CommandLine getCommandLine(String[] args) throws ParseException {
        CommandLineParser clParser = new GnuParser();
        CommandLine cl = null;
        try {
            cl = clParser.parse(mOptions, args);
        } catch (ParseException e) {
            if (helpOptionSpecified(args)) {
                usage();
                return null;
            } else
                throw e;
        }
        if (cl.hasOption(O_H)) {
            usage();
            return null;
        }
        if (cl.hasOption(O_S))
            mHost = cl.getOptionValue(O_S);
        return cl;
    }

    private boolean helpOptionSpecified(String[] args) {
        return
            args != null && args.length == 1 &&
            ("-h".equals(args[0]) || "--help".equals(args[0]));
    }
    
    /**
     * Authenticates using the username and password from the local config.
     * @throws IOException
     * @throws SoapFaultException
     * @throws ServiceException
     */
    protected LmcSession auth() throws SoapFaultException, IOException, ServiceException {
        URL url = new URL("https", mHost, mPort, ZimbraServlet.ADMIN_SERVICE_URI);
        mServerUrl = url.toExternalForm();
        SoapHttpTransport trans = new SoapHttpTransport(mServerUrl);
        trans.setRetryCount(1);
        trans.setTimeout(0);
        mTrans = trans;
        mAuth = false;
        
        Element authReq = new Element.XMLElement(AdminService.AUTH_REQUEST);
        authReq.addAttribute(AdminService.E_NAME, mUser, Element.DISP_CONTENT);
        authReq.addAttribute(AdminService.E_PASSWORD, mPassword, Element.DISP_CONTENT);
        try {
            Element authResp = mTrans.invokeWithoutSession(authReq);
            String authToken = authResp.getAttribute(AdminService.E_AUTH_TOKEN);
            String sessionId = authResp.getAttribute(ZimbraSoapContext.E_SESSION_ID, null);
            mTrans.setAuthToken(authToken);
            if (sessionId != null) {
                mTrans.setSessionId(sessionId);
            }
            mAuth = true;
            return new LmcSession(authToken, sessionId);
        } catch (UnknownHostException e) {
            // UnknownHostException's error message is not clear; rethrow with a more descriptive message
            throw new IOException("Unknown host: " + mHost);
        }
    }

    /**
     * Sets up expected command line options. This class adds -h for help and -s for server.
     *
     */
    protected void setupCommandLineOptions() {
        Option s = new Option(O_S, "server", true, "Mail server hostname. Default is localhost.");
        mOptions.addOption(s);
        mOptions.addOption(O_H, "help", false, "Displays this help message.");
    }
    
    /**
     * Displays usage to stdout.
     *
     */
    protected void usage() {
        usage(null);
    }
    
    /**
     * Displays usage to stdout.
     * @param e parse error 
     */
    protected void usage(ParseException e) {
        if (e != null) {
            System.err.println("Error parsing command line arguments: " + e.getMessage());
        }

        PrintWriter pw = new PrintWriter(System.err, true);
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(pw, formatter.getWidth(), getCommandUsage(),
                null, mOptions, formatter.getLeftPadding(), formatter.getDescPadding(),
                "\n" + getTrailer());
        
    }

    /**
     * Returns the command usage. Since most CLI utilities are wrapped into shell script, the name of
     * the script should be returned.
     * @return
     */
    protected abstract String getCommandUsage();
    
    /**
     * Returns the trailer in the usage message. Subclass can add additional notes on the usage.
     * @return
     */
    protected String getTrailer() {
        return "";
    }
    
    /**
     * Returns whether this command line SOAP client has been authenticated.
     * @return
     */
    protected boolean isAuthenticated() {
        return mAuth;
    }
    
    /**
     * Returns the username.
     * @return
     */
    protected String getUser() {
        return mUser;
    }
    
    /**
     * Returns the target server hostname.
     * @return
     */
    protected String getServer() {
        return mHost;
    }
    
    /**
     * Returns the target server admin port number.
     * @return
     */
    protected int getPort() {
        return mPort;
    }
    
    /**
     * Gets the SOAP transport. 
     * @return null if the SOAP client has not been authenticated.
     */
    protected SoapTransport getTransport() {
        return mTrans;
    }
    
    protected String getServerUrl() {
        return mServerUrl;
    }
    
    /**
     * Gets the options that has been set up so far. 
     * @return 
     */
    protected Options getOptions() {
        return mOptions;
    }
}
