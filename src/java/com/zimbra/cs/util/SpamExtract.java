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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.client.LmcSession;
import com.zimbra.cs.localconfig.LC;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.admin.AdminService;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.SoapHttpTransport;

/* zmspamextract --spam|--notspam [--keep] --directory */

public class SpamExtract {

    private static Log mLog = LogFactory.getLog(SpamExtract.class);
    
    private static Options mOptions = new Options();

    static {
        mOptions.addOption("s", "spam", false, "extract messages marked spam");
        mOptions.addOption("n", "notspam", false, "extract messages marked notspam");
        mOptions.addOption("k", "keep", false, "keep messages on server (default is to delete)");
        mOptions.addOption("d", "directory", true, "directory to store extracted messages");
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

    public static void main(String[] args) throws ServiceException, HttpException, SoapFaultException, IOException {
        CommandLine cl = parseArgs(args);

        if (cl.hasOption('D')) {
            Zimbra.toolSetup("DEBUG");
        } else if (cl.hasOption('v')) {
            Zimbra.toolSetup("INFO");
        } else {
            Zimbra.toolSetup("WARN");
        }
        
        if (cl.hasOption('s') && cl.hasOption('n')) {
            usage("specify only one of spam or notspam options");
        }
        if (!cl.hasOption('s') && !cl.hasOption('n')) {
            usage("must specify one of spam or notspam options");
        }

        boolean optSpam = true;
        if (cl.hasOption('n')) {
            optSpam = false;
        } else if (cl.hasOption('s')) {
            optSpam = true;
        }

        boolean optKeep = cl.hasOption('k');

        if (!cl.hasOption('d')) {
            usage("must specify directory to extract messages to");
        }

        String optDirectory = cl.getOptionValue('d');
        File dir = new File(optDirectory);
        if (!dir.exists()) {
            if (mLog.isInfoEnabled()) mLog.info("directory " + optDirectory + " does not exist, will create");
            dir.mkdirs();
            if (!dir.exists()) {
                mLog.error("could not create directory " + optDirectory);
                System.exit(2);
            }
        }

        if (mLog.isInfoEnabled()) mLog.info("Extracting type: " + (optSpam ? "spam" : "ham"));
        
        Account spamAccount = getSpamAccount(optSpam);
        if (spamAccount == null) {
            System.exit(1);
        }
        
        if (mLog.isInfoEnabled()) mLog.info("Configured account: " + spamAccount.getName());
        
        Server server = spamAccount.getServer();

        String adminAuthToken = getAdminAuthToken(server);

        LmcSession session = new LmcSession(adminAuthToken, null);

        extract(adminAuthToken, spamAccount, server, "inbox", dir);
    }

    public static final String TYPE_MESSAGE = "message";

    private static void extract(String authToken, Account account, Server server, String folder, File outdir) throws ServiceException, HttpException, SoapFaultException, IOException {
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
        hc.getHttpConnectionManager().getParams().setConnectionTimeout(30000);
        if (mLog.isInfoEnabled()) mLog.info("Mailbox requests to: " + restURL);
        
        boolean haveMore = true;
        int offset = 0;
        while (haveMore) {
            SoapHttpTransport transport = new SoapHttpTransport(soapURL);
            transport.setRetryCount(1);
            transport.setTimeout(0);
            transport.setAuthToken(authToken);

            Element searchReq = new Element.XMLElement(MailService.SEARCH_REQUEST);
            String queryText = "in:" + folder;
            searchReq.setText(queryText);
            searchReq.addAttribute(MailService.A_SEARCH_TYPES, TYPE_MESSAGE);
            searchReq.addAttribute(MailService.A_QUERY_OFFSET, offset);

            try {
                if (mLog.isDebugEnabled()) mLog.debug("Searching " + queryText);
                if (mLog.isDebugEnabled()) mLog.debug(searchReq.prettyPrint());
                Element searchResp = transport.invoke(searchReq, false, true, true, account.getId());
                if (mLog.isDebugEnabled()) mLog.debug(searchResp.prettyPrint());
                
                for (Iterator<Element> iter = searchResp.elementIterator(MailService.E_MSG); iter.hasNext();) {
                    offset++;
                    Element e = iter.next();
                    String mid = e.getAttribute(MailService.A_ID);
                    if (mid == null) {
                        mLog.warn("null message id SOAP response");
                        continue;
                    }
                    String path = "/service/user/" + account.getName() + "/" + folder + "?id=" + mid;
                    extractMessage(hc, gm, offset, path, outdir);
                }
                
                haveMore = false;
                String more = searchResp.getAttribute(MailService.A_QUERY_MORE);
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
                
            } finally {
                gm.releaseConnection();
            }
        }
    }

    private static Session mJMSession;
    
    private static String mOutputPrefix;
    
    static {
        Properties props = new Properties();
        props.setProperty("mail.mime.address.strict", "false");
        mJMSession = Session.getInstance(props);
        mOutputPrefix = Long.toHexString(System.currentTimeMillis());
    }
    
    private static void extractMessage(HttpClient hc, GetMethod gm, int n, String path, File outdir) {
        try {
            extractMessage0(hc, gm, n, path, outdir);
        } catch (MessagingException me) {
            mLog.warn("message not in zimbra spam report expected format", me);
        } catch (Exception e) {
            mLog.warn("exception occurred fetching message", e);
        }
    }        
    
    private static void extractMessage0(HttpClient hc, GetMethod gm, int n, String path, File outdir) throws HttpException, IOException, MessagingException {
        gm.setPath(path);
        if (mLog.isDebugEnabled()) mLog.debug("Fetching " + path);
        hc.executeMethod(gm);
        if (gm.getStatusCode() != HttpStatus.SC_OK) {
            mLog.warn("Fetch status: " + gm.getStatusLine());
        }
        InputStream is = gm.getResponseBodyAsStream();
        MimeMessage mm = new MimeMessage(mJMSession, is);
        MimeMultipart mmp = (MimeMultipart)mm.getContent(); // multipart/mixed 
        BodyPart bp = mmp.getBodyPart(1); // message/rfc822 part
        Part msg = (Part) bp.getContent(); // the actual message
        File file = new File(outdir, mOutputPrefix + "-" + n);
        OutputStream os = new BufferedOutputStream(new FileOutputStream(file)); 
        msg.writeTo(os);
        os.close();
        if (mLog.isInfoEnabled()) mLog.info("Wrote: " + file);
    }
    
    public static URL getServerURL(Server server, boolean admin) throws ServiceException {
        String host = server.getAttr(Provisioning.A_zimbraServiceHostname);
        if (host == null) {
            throw ServiceException.FAILURE("invalid "
                    + Provisioning.A_zimbraServiceHostname + " in server "
                    + server.getName(), null);
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

    public static String getAdminAuthToken(Server server)
            throws ServiceException {
        String adminUser = LdapUtil.dnToUid(LC.zimbra_ldap_userdn.value());
        String adminPassword = LC.zimbra_ldap_password.value();

        String url = getSoapURL(server, true);
        SoapHttpTransport transport = new SoapHttpTransport(url);
        transport.setRetryCount(1);
        transport.setTimeout(0);

        Element authReq = new Element.XMLElement(AdminService.AUTH_REQUEST);
        authReq.addAttribute(AdminService.E_NAME, adminUser,
                Element.DISP_CONTENT);
        authReq.addAttribute(AdminService.E_PASSWORD, adminPassword,
                Element.DISP_CONTENT);
        try {
            if (mLog.isInfoEnabled()) mLog.info("Auth request to: " + url);
            if (mLog.isDebugEnabled()) mLog.debug(authReq.prettyPrint());
            Element authResp = transport.invokeWithoutSession(authReq);
            if (mLog.isDebugEnabled()) mLog.debug(authResp.prettyPrint());
            String authToken = authResp.getAttribute(AdminService.E_AUTH_TOKEN);
            return authToken;
        } catch (Exception e) {
            throw ServiceException.FAILURE("admin auth failed url=" + url, e);
        }
    }

    private static Account getSpamAccount(boolean spam) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Config conf;
        try {
            conf = prov.getConfig();
        } catch (ServiceException e) {
            throw ServiceException.FAILURE(
                    "Unable to connect to LDAP directory", e);
        }

        String name = conf.getAttr(
                spam ? Provisioning.A_zimbraSpamIsSpamAccount
                        : Provisioning.A_zimbraSpamIsNotSpamAccount, null);
        if (name == null || name.length() == 0) {
            mLog.error("no account configured for " + (spam ? "spam" : "notspam"));
            return null;
        }

        Account account = prov.getAccountByName(name);
        if (account == null) {
            mLog.error("can not locate " + (spam ? "spam" : "notspam") + " account: " + name);
            return null;
        }

        return account;
    }
}
