/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.client.LmcSession;

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
    
    // common options
    
    public static final String O_AUTHTOKEN = "y";
    public static final String O_AUTHTOKENFILE = "Y";
    public static final String O_H = "h";
    public static final String O_HIDDEN = "hidden";
    public static final String O_S = "s";
    
    public static final Option OPT_AUTHTOKEN = new Option(O_AUTHTOKEN, "authtoken", true, "use auth token string (has to be in JSON format) from command line");
    public static final Option OPT_AUTHTOKENFILE = new Option(O_AUTHTOKENFILE, "authtokenfile", true, "read auth token (has to be in JSON format) from a file");


    private String mUser;
    private String mPassword;
    private String mHost;
    private int mPort;
    private boolean mAuth;
    private Options mOptions;
    private Options mHiddenOptions;
    
    private SoapTransport mTrans = null;
    private String mServerUrl;
    
    protected SoapCLI() throws ServiceException {
        // get admin username from local config
        mUser = LC.zimbra_ldap_user.value();
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
        mHiddenOptions = new Options();
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

        Options opts = getAllOptions();
        try {
            cl = clParser.parse(opts, args);
        } catch (ParseException e) {
            if (helpOptionSpecified(args)) {
                usage();
                return null;
            } else
                throw e;
        }
        if (cl.hasOption(O_H)) {
            boolean showHiddenOptions = cl.hasOption(O_HIDDEN);
            usage(null, showHiddenOptions);
            return null;
        }
        if (cl.hasOption(O_S))
            mHost = cl.getOptionValue(O_S);
        return cl;
    }

    /**
     * Returns an <tt>Options</tt> object that combines the standard options
     * and the hidden ones.
     */
    @SuppressWarnings("unchecked")
    private Options getAllOptions() {
        Options newOptions = new Options();
        Set<OptionGroup> groups = new HashSet<OptionGroup>();
        Options[] optionses =
            new Options[] { mOptions, mHiddenOptions };
        for (Options options : optionses) {
            for (Option opt : (Collection<Option>) options.getOptions()) {
                OptionGroup group = options.getOptionGroup(opt);
                if (group != null) {
                    groups.add(group);
                } else {
                    newOptions.addOption(opt);
                }
            }
        }
        
        for (OptionGroup group : groups) {
            newOptions.addOptionGroup(group);
        }
        return newOptions;
    }

    private boolean helpOptionSpecified(String[] args) {
        return
            args != null && args.length == 1 &&
            ("-h".equals(args[0]) || "--help".equals(args[0]));
    }
    
    /**
     * Authenticates using the username and password from the local config.
     * @throws IOException
     * @throws com.zimbra.common.soap.SoapFaultException
     * @throws ServiceException
     */
    protected LmcSession auth() throws SoapFaultException, IOException, ServiceException {
        URL url = new URL("https", mHost, mPort, AdminConstants.ADMIN_SERVICE_URI);
        mServerUrl = url.toExternalForm();
        SoapTransport trans = getTransport();
        mAuth = false;
        
        Element authReq = new Element.XMLElement(AdminConstants.AUTH_REQUEST);
        authReq.addAttribute(AdminConstants.E_NAME, mUser, Element.Disposition.CONTENT);
        authReq.addAttribute(AdminConstants.E_PASSWORD, mPassword, Element.Disposition.CONTENT);
        try {
            Element authResp = trans.invokeWithoutSession(authReq);
            String authToken = authResp.getAttribute(AdminConstants.E_AUTH_TOKEN);
            ZAuthToken zat = new ZAuthToken(null, authToken, null);
            trans.setAuthToken(authToken);
            mAuth = true;
            return new LmcSession(zat, null);
        } catch (UnknownHostException e) {
            // UnknownHostException's error message is not clear; rethrow with a more descriptive message
            throw new IOException("Unknown host: " + mHost);
        }
    }
    
    /**
     * Authenticates using the provided ZAuthToken
     * @throws IOException
     * @throws com.zimbra.common.soap.SoapFaultException
     * @throws ServiceException
     */
    protected LmcSession auth(ZAuthToken zAuthToken) throws SoapFaultException, IOException, ServiceException {
        if (zAuthToken == null)
            return auth();
            
        URL url = new URL("https", mHost, mPort, AdminConstants.ADMIN_SERVICE_URI);
        mServerUrl = url.toExternalForm();
        SoapTransport trans = getTransport();
        mAuth = false;
        
        Element authReq = new Element.XMLElement(AdminConstants.AUTH_REQUEST);
        zAuthToken.encodeAuthReq(authReq, true);
        try {
            Element authResp = trans.invokeWithoutSession(authReq);
            ZAuthToken zat = new ZAuthToken(authResp.getElement(AdminConstants.E_AUTH_TOKEN), true);
            trans.setAuthToken(zat);
            mAuth = true;
            return new LmcSession(zat, null);
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
        mHiddenOptions.addOption(null, O_HIDDEN, false, "Include hidden options in help output");
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
        usage(e, false);
    }

    protected void usage(ParseException e, boolean showHiddenOptions) {
        if (e != null) {
            System.err.println("Error parsing command line arguments: " + e.getMessage());
        }

        Options opts = showHiddenOptions ? getAllOptions() : mOptions;
        PrintWriter pw = new PrintWriter(System.err, true);
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(pw, formatter.getWidth(), getCommandUsage(),
                null, opts, formatter.getLeftPadding(), formatter.getDescPadding(),
                null);
        pw.flush();

        String trailer = getTrailer();
        if (trailer != null && trailer.length() > 0) {
            System.err.println();
            System.err.println(trailer);
        }
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
        if (mTrans == null)
            initTransport();
        return mTrans;
    }
    
    private void initTransport() {
        SoapHttpTransport trans = new SoapHttpTransport(mServerUrl);
        trans.setRetryCount(1);
        mTrans = trans;
    }
    
    /**
     * Set the SOAP transport read timeout
     * @return null if the SOAP client has not been authenticated.
     */
    public void setTransportTimeout(int newTimeout) {
        getTransport().setTimeout(newTimeout);
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

    protected Options getHiddenOptions() {
        return mHiddenOptions;
    }

    // helper for options that specify date/time

    private static final String[] DATETIME_FORMATS = {
        "yyyy/MM/dd HH:mm:ss",
        "yyyy/MM/dd HH:mm:ss SSS",
        "yyyy/MM/dd HH:mm:ss.SSS",
        "yyyy/MM/dd-HH:mm:ss-SSS",
        "yyyy/MM/dd-HH:mm:ss",
        "yyyyMMdd.HHmmss.SSS",
        "yyyyMMdd.HHmmss",
        "yyyyMMddHHmmssSSS",
        "yyyyMMddHHmmss"
    };
    public static final String CANONICAL_DATETIME_FORMAT = DATETIME_FORMATS[0];

    public static Date parseDatetime(String str) {
        for (String formatStr: DATETIME_FORMATS) {
            SimpleDateFormat fmt = new SimpleDateFormat(formatStr);
            fmt.setLenient(false);
            ParsePosition pp = new ParsePosition(0);
            Date d = fmt.parse(str, pp);
            if (d != null && pp.getIndex() == str.length())
                return d;
        }
        return null;
    }

    public static String getAllowedDatetimeFormatsHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append("Specify date/time in one of these formats:\n\n");
        Date d = new Date();
        for (String formatStr: DATETIME_FORMATS) {
            SimpleDateFormat fmt = new SimpleDateFormat(formatStr);
            String s = fmt.format(d);
            sb.append("    ").append(s).append("\n");
        }
        sb.append("\n");

        sb.append(
            "Specify year, month, date, hour, minute, second, and optionally millisecond.\n");
        sb.append(
            "Month/date/hour/minute/second are 0-padded to 2 digits, millisecond to 3 digits.\n");
        sb.append(
            "Hour must be specified in 24-hour format, and time is in local time zone.\n");
        return sb.toString();
    }
    
    public static ZAuthToken getZAuthToken(CommandLine cl) throws ServiceException, ParseException, IOException {
        if (cl.hasOption(SoapCLI.O_AUTHTOKEN) && cl.hasOption(SoapCLI.O_AUTHTOKENFILE)) {
            String msg = String.format("cannot specify both %s and %s options",
                    SoapCLI.O_AUTHTOKEN, SoapCLI.O_AUTHTOKENFILE);
            throw new ParseException(msg);
        }
        
        if (cl.hasOption(SoapCLI.O_AUTHTOKEN)) {
            return ZAuthToken.fromJSONString(cl.getOptionValue(SoapCLI.O_AUTHTOKEN));
        }
        
        if (cl.hasOption(SoapCLI.O_AUTHTOKENFILE)) {
            String authToken = StringUtil.readSingleLineFromFile(cl.getOptionValue(SoapCLI.O_AUTHTOKENFILE));
            return ZAuthToken.fromJSONString(authToken);
        } 
        
        return null;
    }
}
