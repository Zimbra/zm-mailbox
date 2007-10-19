/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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

package com.zimbra.cs.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.protocol.Protocol;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.CliUtil;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.service.mail.ItemAction;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.common.soap.*;

public class SpamExtract {

    private static Log mLog = LogFactory.getLog(SpamExtract.class);
    
    private static Options mOptions = new Options();

    static {
        mOptions.addOption("s", "spam", false, "extract messages from configured spam mailbox");
        mOptions.addOption("n", "notspam", false, "extract messages from configured notspam mailbox");
        mOptions.addOption("m", "mailbox", true, "extract messages from specified mailbox");
       
        mOptions.addOption("d", "delete", false, "delete extracted messages (default is to keep)");
        mOptions.addOption("o", "outdir", true, "directory to store extracted messages");

        mOptions.addOption("a", "admin", true, "admin user name for auth (default is zimbra_ldap_userdn)");
        mOptions.addOption("p", "password", true, "admin password for auth (default is zimbra_ldap_password)");
        mOptions.addOption("u", "url", true, "admin SOAP service url (default is target mailbox's server's admin service port)");
        
        mOptions.addOption("q", "query", true, "search query whose results should be extracted (default is in:inbox)");
        mOptions.addOption("r", "raw", false, "extract raw message (default: gets message/rfc822 attachments)");
        
        mOptions.addOption("h", "help", false, "show this usage text");
        mOptions.addOption("D", "debug", false, "enable debug level logging");
        mOptions.addOption("v", "verbose", false, "be verbose while running");
    }

    private static void usage(String errmsg) {
        if (errmsg != null) {
            mLog.error(errmsg);
        }
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("SpamExtract [options] ",
            "where [options] are one of:", mOptions,
            "SpamExtract retrieve messages that may have been marked as spam or not spam in the Zimbra Web Client.");
        System.exit((errmsg == null) ? 0 : 1);
    }

    private static CommandLine parseArgs(String args[]) {
        CommandLineParser parser = new GnuParser();
        CommandLine cl = null;
        try {
            cl = parser.parse(mOptions, args);
        } catch (ParseException pe) {
            usage(pe.getMessage());
        }

        if (cl.hasOption("h")) {
            usage(null);
        }
        return cl;
    }

    private static boolean mVerbose = false;
    
    public static void main(String[] args) throws ServiceException, HttpException, SoapFaultException, IOException {
        CommandLine cl = parseArgs(args);

        if (cl.hasOption('D')) {
            CliUtil.toolSetup("DEBUG");
        } else {
            CliUtil.toolSetup("INFO");
        }
        if (cl.hasOption('v')) {
            mVerbose = true;
        }
        
        boolean optDelete = cl.hasOption('d');

        if (!cl.hasOption('o')) {
            usage("must specify directory to extract messages to");
        }
        String optDirectory = cl.getOptionValue('o');
        File outputDirectory = new File(optDirectory);
        if (!outputDirectory.exists()) {
            mLog.info("Creating directory: " + optDirectory);
            outputDirectory.mkdirs();
            if (!outputDirectory.exists()) {
                mLog.error("could not create directory " + optDirectory);
                System.exit(2);
            }
        }

        String optAdminUser;
        if (cl.hasOption('a')) {
            optAdminUser = cl.getOptionValue('a'); 
        } else {
            optAdminUser = LdapUtil.dnToUid(LC.zimbra_ldap_userdn.value());
        }

        String optAdminPassword;
        if (cl.hasOption('p')) {
            optAdminPassword = cl.getOptionValue('p');
        } else {
            optAdminPassword = LC.zimbra_ldap_password.value();
        }
        
        String optQuery = "in:inbox";
        if (cl.hasOption('q')) {
            optQuery = cl.getOptionValue('q');
        }

        Account account = getAccount(cl);
        if (account == null) {
            System.exit(1);
        }

        boolean optRaw = cl.hasOption('r');
        
        if (mVerbose) mLog.info("Extracting from account " + account.getName());
        
        Server server = Provisioning.getInstance().getServer(account);

        String optAdminURL;
        if (cl.hasOption('u')) {
            optAdminURL = cl.getOptionValue('u');
        } else {
            optAdminURL = getSoapURL(server, true);
        }
        String adminAuthToken = getAdminAuthToken(optAdminURL, optAdminUser, optAdminPassword);
        String authToken = getDelegateAuthToken(optAdminURL, account, adminAuthToken);
        extract(authToken, account, server, optQuery, outputDirectory, optDelete, optRaw);
    }

    public static final String TYPE_MESSAGE = "message";

    private static void extract(String authToken, Account account, Server server, String query, File outdir, boolean delete, boolean raw) throws ServiceException, HttpException, SoapFaultException, IOException {
        String soapURL = getSoapURL(server, false);

        URL restURL = getServerURL(server, false);
        HttpClient hc = new HttpClient();
        HttpState state = new HttpState();
        GetMethod gm = new GetMethod();
        gm.setFollowRedirects(true);
        Cookie authCookie = new Cookie(restURL.getHost(), ZimbraServlet.COOKIE_ZM_AUTH_TOKEN, authToken, "/", -1, false);
        state.addCookie(authCookie);
        hc.setState(state);
        hc.getHostConfiguration().setHost(restURL.getHost(), restURL.getPort(), Protocol.getProtocol(restURL.getProtocol()));
        hc.getHttpConnectionManager().getParams().setConnectionTimeout(60000);
        hc.setTimeout(60000);

        if (mVerbose) mLog.info("Mailbox requests to: " + restURL);

        SoapHttpTransport transport = new SoapHttpTransport(soapURL);
        transport.setRetryCount(1);
        transport.setTimeout(0);
        transport.setAuthToken(authToken);

        int totalProcessed = 0;
        boolean haveMore = true;
        int offset = 0;
        while (haveMore) {
            Element searchReq = new Element.XMLElement(MailConstants.SEARCH_REQUEST);
            searchReq.addElement(MailConstants.A_QUERY).setText(query);
            searchReq.addAttribute(MailConstants.A_SEARCH_TYPES, TYPE_MESSAGE);
            searchReq.addAttribute(MailConstants.A_QUERY_OFFSET, offset);

            try {
                if (mLog.isDebugEnabled()) mLog.debug(searchReq.prettyPrint());
                Element searchResp = transport.invoke(searchReq, false, true, account.getId());
                if (mLog.isDebugEnabled()) mLog.debug(searchResp.prettyPrint());
                
                StringBuilder deleteList = new StringBuilder();

                for (Iterator<Element> iter = searchResp.elementIterator(MailConstants.E_MSG); iter.hasNext();) {
                    offset++;
                    Element e = iter.next();
                    String mid = e.getAttribute(MailConstants.A_ID);
                    if (mid == null) {
                        mLog.warn("null message id SOAP response");
                        continue;
                    }
                    String path = "/service/user/" + account.getName() + "/?id=" + mid;
                    if (extractMessage(hc, gm, path, outdir, raw)) {
                        deleteList.append(mid).append(',');
                    }
                    totalProcessed++;
                }
                
                haveMore = false;
                String more = searchResp.getAttribute(MailConstants.A_QUERY_MORE);
                if (more != null && more.length() > 0) {
                    try {
                        int m = Integer.parseInt(more);
                        if (m > 0) {
                            haveMore = true;
                        }
                    } catch (NumberFormatException nfe) {
                        mLog.warn("more flag from server not a number: " + more, nfe);
                    }
                }
                
                if (delete && deleteList.length() > 0) {
                    deleteList.deleteCharAt(deleteList.length()-1); // -1 removes trailing comma
                    Element msgActionReq = new Element.XMLElement(MailConstants.MSG_ACTION_REQUEST);
                    Element action = msgActionReq.addElement(MailConstants.E_ACTION);
                    action.addAttribute(MailConstants.A_ID, deleteList.toString());
                    action.addAttribute(MailConstants.A_OPERATION, ItemAction.OP_HARD_DELETE);
                    
                    if (mLog.isDebugEnabled()) mLog.debug(msgActionReq.prettyPrint());
                    Element msgActionResp = transport.invoke(msgActionReq, false, true, account.getId());
                    if (mLog.isDebugEnabled()) mLog.debug(msgActionResp.prettyPrint());
                }
            } finally {
                gm.releaseConnection();
            }

        }
        mLog.info("Total messages processed: " + totalProcessed);
    }

    private static Session mJMSession;
    
    private static String mOutputPrefix;
    
    static {
        Properties props = new Properties();
        props.setProperty("mail.mime.address.strict", "false");
        mJMSession = Session.getInstance(props);
        mOutputPrefix = Long.toHexString(System.currentTimeMillis());
    }
    
    private static boolean extractMessage(HttpClient hc, GetMethod gm, String path, File outdir, boolean raw) {
        try {
            extractMessage0(hc, gm, path, outdir, raw);
            return true;
        } catch (MessagingException me) {
            mLog.warn("exception occurred fetching message", me);
        } catch (IOException ioe) {
            mLog.warn("exception occurred fetching message", ioe);
        }
        return false;
    }        
    
    private static int mExtractIndex;
    
    private static void extractMessage0(HttpClient hc, GetMethod gm, String path, File outdir, boolean raw) throws IOException, MessagingException {
        gm.setPath(path);
        if (mLog.isDebugEnabled()) mLog.debug("Fetching " + path);
        hc.executeMethod(gm);
        if (gm.getStatusCode() != HttpStatus.SC_OK) {
            throw new IOException("HTTP GET failed: " + gm.getPath() + ": " + gm.getStatusCode() + ": " + gm.getStatusText());
        }
    
        InputStream is = null;
        MimeMessage mm = null;
        try {
            is = gm.getResponseBodyAsStream();
            mm = new MimeMessage(mJMSession, is);
        } finally {
            ByteUtil.closeStream(is);
        }
        
        if (raw) {
            File file = new File(outdir, mOutputPrefix + "-" + mExtractIndex++);
            OutputStream os = null;
            try {
                os = new BufferedOutputStream(new FileOutputStream(file)); 
                mm.writeTo(os);
            } finally {
                os.close();
            }
            if (mVerbose) mLog.info("Wrote: " + file);
            return;
        }

        // Not raw - ignore the spam report and extract messages that are in attachments...
        if (!(mm.getContent() instanceof MimeMultipart)) {
            mLog.warn("Spam/notspam messages must have attachments (skipping " + gm.getPath() + ")");
            return;
        }
        
        MimeMultipart mmp = (MimeMultipart)mm.getContent();
        int nAttachments  = mmp.getCount();
        boolean foundAtleastOneAttachedMessage = false;
        for (int i = 0; i < nAttachments; i++) {
            BodyPart bp = mmp.getBodyPart(i);
            if (!bp.isMimeType("message/rfc822")) {
                // Let's ignore all parts that are not messages.
                continue;
            }
            foundAtleastOneAttachedMessage = true;
            Part msg = (Part) bp.getContent(); // the actual message
            File file = new File(outdir, mOutputPrefix + "-" + mExtractIndex++);
            OutputStream os = null;
            try {
                os = new BufferedOutputStream(new FileOutputStream(file)); 
                msg.writeTo(os);
            } finally {
                os.close();
            }
            if (mVerbose) mLog.info("Wrote: " + file);
        }
        
        if (!foundAtleastOneAttachedMessage) {
            String msgid = mm.getHeader("Message-ID", " ");
            mLog.warn("message uri=" + gm.getPath() + " message-id=" + msgid + " had no attachments");
        }
    }
        
    public static URL getServerURL(Server server, boolean admin) throws ServiceException {
        String host = server.getAttr(Provisioning.A_zimbraServiceHostname);
        if (host == null) {
            throw ServiceException.FAILURE("invalid " + Provisioning.A_zimbraServiceHostname + " in server " + server.getName(), null);
        }

        String protocol = "http";
        String portAttr = Provisioning.A_zimbraMailPort;

        if (admin) {
            protocol = "https";
            portAttr = Provisioning.A_zimbraAdminPort;
        } else {
            String mode = server.getAttr(Provisioning.A_zimbraMailMode);
            if (mode == null) {
                throw ServiceException.FAILURE("null " + Provisioning.A_zimbraMailMode + " in server " + server.getName(), null);
            }
            if (mode.equalsIgnoreCase("https")) {
                protocol = "https";
                portAttr = Provisioning.A_zimbraMailSSLPort;
            }
            if (mode.equalsIgnoreCase("redirect")) {
                protocol = "https";
                portAttr = Provisioning.A_zimbraMailSSLPort;
            }
        }

        int port = server.getIntAttr(portAttr, -1);
        if (port < 1) {
            throw ServiceException.FAILURE("invalid " + portAttr + " in server " + server.getName(), null);
        }

        try {
            return new URL(protocol, host, port, "");
        } catch (MalformedURLException mue) {
            throw ServiceException.FAILURE("exception creating url (protocol=" + protocol + " host=" + host + " port=" + port + ")", mue);
        }
    }

    public static String getSoapURL(Server server, boolean admin) throws ServiceException {
        String url = getServerURL(server, admin).toString();
        String file = admin ? ZimbraServlet.ADMIN_SERVICE_URI : ZimbraServlet.USER_SERVICE_URI;
        return url + file;
    }

    public static String getAdminAuthToken(String adminURL, String adminUser, String adminPassword) throws ServiceException {
        SoapHttpTransport transport = new SoapHttpTransport(adminURL);
        transport.setRetryCount(1);
        transport.setTimeout(0);

        Element authReq = new Element.XMLElement(AdminConstants.AUTH_REQUEST);
        authReq.addAttribute(AdminConstants.E_NAME, adminUser, Element.Disposition.CONTENT);
        authReq.addAttribute(AdminConstants.E_PASSWORD, adminPassword, Element.Disposition.CONTENT);
        try {
            if (mVerbose) mLog.info("Auth request to: " + adminURL);
            if (mLog.isDebugEnabled()) mLog.debug(authReq.prettyPrint());
            Element authResp = transport.invokeWithoutSession(authReq);
            if (mLog.isDebugEnabled()) mLog.debug(authResp.prettyPrint());
            String authToken = authResp.getAttribute(AdminConstants.E_AUTH_TOKEN);
            return authToken;
        } catch (Exception e) {
            throw ServiceException.FAILURE("admin auth failed url=" + adminURL, e);
        }
    }

    public static String getDelegateAuthToken(String adminURL, Account account, String adminAuthToken) throws ServiceException {
        SoapHttpTransport transport = new SoapHttpTransport(adminURL);
        transport.setRetryCount(1);
        transport.setTimeout(0);
        transport.setAuthToken(adminAuthToken);

        Element daReq = new Element.XMLElement(AdminConstants.DELEGATE_AUTH_REQUEST);
        Element acctElem = daReq.addElement(AdminConstants.E_ACCOUNT);
        acctElem.addAttribute(AdminConstants.A_BY, AdminConstants.BY_ID);
        acctElem.setText(account.getId());
        try {
            if (mVerbose) mLog.info("Delegate auth request to: " + adminURL);
            if (mLog.isDebugEnabled()) mLog.debug(daReq.prettyPrint());
            Element daResp = transport.invokeWithoutSession(daReq);
            if (mLog.isDebugEnabled()) mLog.debug(daResp.prettyPrint());
            String authToken = daResp.getAttribute(AdminConstants.E_AUTH_TOKEN);
            return authToken;
        } catch (Exception e) {
            throw ServiceException.FAILURE("Delegate auth failed url=" + adminURL, e);
        }
    }

    private static Account getAccount(CommandLine cl) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Config conf;
        try {
            conf = prov.getConfig();
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("Unable to connect to LDAP directory", e);
        }

        String name = null;
        
        if (cl.hasOption('s')) {
            if (cl.hasOption('n') || cl.hasOption('m')) {
                mLog.error("only one of s, n or m options can be specified");
                return null;
            }
            name = conf.getAttr(Provisioning.A_zimbraSpamIsSpamAccount);
            if (name == null || name.length() == 0) {
                mLog.error("no account configured for spam");
                return null;
            }
        } else if (cl.hasOption('n')) {
            if (cl.hasOption('m')) {
                mLog.error("only one of s, n, or m options can be specified");
                return null;
            }
            name = conf.getAttr(Provisioning.A_zimbraSpamIsNotSpamAccount);
            if (name == null || name.length() == 0) {
                mLog.error("no account configured for ham");
                return null;
            }
        } else if (cl.hasOption('m')) {
            name = cl.getOptionValue('m');
            if (name.length() == 0) {
                mLog.error("illegal argument to m option");
                return null;
            }
        } else {
            mLog.error("one of s, n or m options must be specified");
            return null;
        }
        
        Account account = prov.get(AccountBy.name, name);
        if (account == null) {
            mLog.error("can not find account " + name);
            return null;
        }

        return account;
    }
}
