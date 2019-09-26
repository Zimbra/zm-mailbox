/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

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
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;

import com.google.common.base.Charsets;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.util.BufferStream;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.ZimbraCookie;
import com.zimbra.common.zmime.ZMimeMessage;
import com.zimbra.common.zmime.ZSharedFileInputStream;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;
import com.zimbra.cs.service.formatter.ArchiveFormatter.ArchiveInputEntry;
import com.zimbra.cs.service.formatter.ArchiveFormatter.ArchiveInputStream;
import com.zimbra.cs.service.formatter.TarArchiveInputStream;
import com.zimbra.cs.service.mail.ItemAction;
import com.zimbra.cs.service.util.ItemData;

public class SpamExtract {

    private static Log LOG = LogFactory.getLog(SpamExtract.class);

    private static Options options = new Options();

    private static boolean verbose = false;

    private static int BATCH_SIZE = 25;

    private static int SLEEP_TIME = 100;

    static {
        options.addOption("s", "spam", false, "extract messages from configured spam mailbox");
        options.addOption("n", "notspam", false, "extract messages from configured notspam mailbox");
        options.addOption("m", "mailbox", true, "extract messages from specified mailbox");

        options.addOption("d", "delete", false, "delete extracted messages (default is to keep)");
        options.addOption("o", "outdir", true, "directory to store extracted messages");

        options.addOption("a", "admin", true, "admin user name for auth (default is zimbra_ldap_userdn)");
        options.addOption("p", "password", true, "admin password for auth (default is zimbra_ldap_password)");
        options.addOption("u", "url", true, "admin SOAP service url (default is target mailbox's server's admin service port)");

        options.addOption("q", "query", true, "search query whose results should be extracted (default is in:inbox)");
        options.addOption("r", "raw", false, "extract raw message (default: gets message/rfc822 attachments)");

        options.addOption("h", "help", false, "show this usage text");
        options.addOption("D", "debug", false, "enable debug level logging");
        options.addOption("v", "verbose", false, "be verbose while running");
    }

    private static void usage(String errmsg) {
        if (errmsg != null) {
            LOG.error(errmsg);
        }
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("zmspamextract [options] ",
            "where [options] are one of:", options,
            "SpamExtract retrieve messages that may have been marked as spam or not spam in the Zimbra Web Client.");
        System.exit((errmsg == null) ? 0 : 1);
    }

    private static CommandLine parseArgs(String args[]) {
        CommandLineParser parser = new GnuParser();
        CommandLine cl = null;
        try {
            cl = parser.parse(options, args);
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
            CliUtil.toolSetup("DEBUG");
        } else {
            CliUtil.toolSetup("INFO");
        }
        if (cl.hasOption('v')) {
            verbose = true;
        }

        boolean optDelete = cl.hasOption('d');

        if (!cl.hasOption('o')) {
            usage("must specify directory to extract messages to");
        }
        String optDirectory = cl.getOptionValue('o');
        File outputDirectory = new File(optDirectory);
        if (!outputDirectory.exists()) {
            LOG.info("Creating directory: " + optDirectory);
            outputDirectory.mkdirs();
            if (!outputDirectory.exists()) {
                LOG.error("could not create directory " + optDirectory);
                System.exit(2);
            }
        }

        String optAdminUser;
        if (cl.hasOption('a')) {
            optAdminUser = cl.getOptionValue('a');
        } else {
            optAdminUser = LC.zimbra_ldap_user.value();
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

        if (verbose) {
            LOG.info("Extracting from account " + account.getName());
        }

        Server server = Provisioning.getInstance().getServer(account);

        String optAdminURL;
        if (cl.hasOption('u')) {
            optAdminURL = cl.getOptionValue('u');
        } else {
            optAdminURL = getSoapURL(server, true);
        }
        String adminAuthToken = getAdminAuthToken(optAdminURL, optAdminUser, optAdminPassword);
        String authToken = getDelegateAuthToken(optAdminURL, account, adminAuthToken);
        BATCH_SIZE = Provisioning.getInstance().getLocalServer().getAntispamExtractionBatchSize();
        SLEEP_TIME = Provisioning.getInstance().getLocalServer().getAntispamExtractionBatchDelay();
        extract(authToken, account, server, optQuery, outputDirectory, optDelete, optRaw);
    }

    private static void extract(String authToken, Account account, Server server, String query, File outdir, boolean delete, boolean raw) throws ServiceException, HttpException, SoapFaultException, IOException {
        String soapURL = getSoapURL(server, false);

        URL restURL = getServerURL(server, false);
        HttpClientBuilder hc = HttpClientBuilder.create();   // CLI only, don't need conn mgr
        BasicCookieStore cookieStore = new BasicCookieStore();
        HttpGet gm = new HttpGet();
        hc.setRedirectStrategy(new DefaultRedirectStrategy());
        
        BasicClientCookie cookie = new BasicClientCookie(ZimbraCookie.COOKIE_ZM_AUTH_TOKEN, authToken);
        cookie.setDomain(restURL.getHost());
        cookie.setPath("/");
        cookie.setSecure(false);
        cookie.setExpiryDate(null);
        cookieStore.addCookie(cookie);
        hc.setDefaultCookieStore(cookieStore);

        HttpHost target = new HttpHost(restURL.getHost(), restURL.getPort(), null);
        
        SocketConfig config = SocketConfig.custom().setSoTimeout(60000).build();
        hc.setDefaultSocketConfig(config);


        if (verbose) {
            LOG.info("Mailbox requests to: " + restURL);
        }

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
            searchReq.addAttribute(MailConstants.A_SEARCH_TYPES, MailItem.Type.MESSAGE.toString());
            searchReq.addAttribute(MailConstants.A_QUERY_OFFSET, offset);
            searchReq.addAttribute(MailConstants.A_LIMIT, BATCH_SIZE);

            try {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(searchReq.prettyPrint());
                }
                Element searchResp = transport.invoke(searchReq, false, true, account.getId());
                if (LOG.isDebugEnabled()) {
                    LOG.debug(searchResp.prettyPrint());
                }

                StringBuilder deleteList = new StringBuilder();

                List<String> ids = new ArrayList<String>();
                for (Iterator<Element> iter = searchResp.elementIterator(MailConstants.E_MSG); iter.hasNext();) {
                    offset++;
                    Element e = iter.next();
                    String mid = e.getAttribute(MailConstants.A_ID);
                    if (mid == null) {
                        LOG.warn("null message id SOAP response");
                        continue;
                    }

                    LOG.debug("adding id %s", mid);
                    ids.add(mid);
                    if (ids.size() >= BATCH_SIZE || !iter.hasNext()) {
                        StringBuilder path = new StringBuilder(restURL.toString() + "/service/user/" + account.getName() + "/?fmt=tgz&list=" + StringUtils.join(ids, ","));
                        LOG.debug("sending request for path %s", path.toString());
                        List<String> extractedIds = extractMessages(hc, gm, path.toString(), outdir, raw);
                        if (ids.size() > extractedIds.size()) {
                            ids.removeAll(extractedIds);
                            LOG.warn("failed to extract %s", ids);
                        }
                        for (String id : extractedIds) {
                            deleteList.append(id).append(',');
                        }

                        ids.clear();
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
                            try {
                                Thread.sleep(SLEEP_TIME);
                            } catch (InterruptedException e) {
                            }
                        }
                    } catch (NumberFormatException nfe) {
                        LOG.warn("more flag from server not a number: " + more, nfe);
                    }
                }

                if (delete && deleteList.length() > 0) {
                    deleteList.deleteCharAt(deleteList.length()-1); // -1 removes trailing comma
                    Element msgActionReq = new Element.XMLElement(MailConstants.MSG_ACTION_REQUEST);
                    Element action = msgActionReq.addElement(MailConstants.E_ACTION);
                    action.addAttribute(MailConstants.A_ID, deleteList.toString());
                    action.addAttribute(MailConstants.A_OPERATION, ItemAction.OP_HARD_DELETE);

                    if (LOG.isDebugEnabled()) {
                        LOG.debug(msgActionReq.prettyPrint());
                    }
                    Element msgActionResp = transport.invoke(msgActionReq, false, true, account.getId());
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(msgActionResp.prettyPrint());
                    }
                    offset = 0; //put offset back to 0 so we always get top N messages even after delete
                }
            } finally {
                gm.releaseConnection();
            }

        }
        LOG.info("Total messages processed: " + totalProcessed);
    }

    private static Session mJMSession;

    private static String mOutputPrefix;

    static {
        Properties props = new Properties();
        props.setProperty("mail.mime.address.strict", "false");
        mJMSession = Session.getInstance(props);
        mOutputPrefix = Long.toHexString(System.currentTimeMillis());
    }

    private static int mExtractIndex;
    private static final int MAX_BUFFER_SIZE = 10 * 1024 * 1024;

    private static List<String> extractMessages(HttpClientBuilder hc, HttpGet gm, String path, File outdir, boolean raw) throws HttpException, IOException {
        List<String> extractedIds = new ArrayList<String>();
        HttpClient client = hc.build();
      
        if (LOG.isDebugEnabled()) {
            LOG.debug("Fetching " + path);
        }

        try {
            URI uri = new URI(path);
            gm.setURI(uri);
        } catch(URISyntaxException e) {
            LOG.warn("exception occurred in URI path", e);
        }
        HttpResponse httpResp = HttpClientUtil.executeMethod(client, gm);
        if (httpResp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new IOException("HTTP GET failed: " + gm.getRequestLine().getUri() + ": " + httpResp.getStatusLine().getStatusCode() + ": " + httpResp.getStatusLine().getReasonPhrase());
        }
        try (ArchiveInputStream tgzStream = new TarArchiveInputStream(
            new GZIPInputStream(httpResp.getEntity().getContent()), Charsets.UTF_8.name())) {

            ArchiveInputEntry entry = null;
            while ((entry = tgzStream.getNextEntry()) != null) {
                LOG.debug("got entry name %s", entry.getName());
                if (entry.getName().endsWith(".meta")) {
                    ItemData itemData = new ItemData(readArchiveEntry(tgzStream, entry));
                    UnderlyingData ud = itemData.ud;
                    entry = tgzStream.getNextEntry(); // .meta always followed
                                                      // by .eml
                    if (raw) {
                        // Write the message as-is.
                        File file = new File(outdir, mOutputPrefix + "-" + mExtractIndex++);
                        OutputStream os = null;
                        try {
                            os = new BufferedOutputStream(new FileOutputStream(file));
                            byte[] data = readArchiveEntry(tgzStream, entry);
                            ByteUtil.copy(new ByteArrayInputStream(data), true, os, false);
                            if (verbose) {
                                LOG.info("Wrote: " + file);
                            }
                            extractedIds.add(ud.id + "");
                        } catch (java.io.IOException e) {
                            String fileName = outdir + "/" + mOutputPrefix + "-" + mExtractIndex;
                            LOG.error("Cannot write to " + fileName, e);
                        } finally {
                            if (os != null) {
                                os.close();
                            }
                        }
                    } else {
                        // Write the attached message to the output directory.
                        BufferStream buffer = new BufferStream(entry.getSize(), MAX_BUFFER_SIZE);
                        buffer.setSequenced(false);
                        MimeMessage mm = null;
                        InputStream fis = null;
                        try {
                            byte[] data = readArchiveEntry(tgzStream, entry);
                            ByteUtil.copy(new ByteArrayInputStream(data), true, buffer, false);
                            if (buffer.isSpooled()) {
                                fis = new ZSharedFileInputStream(buffer.getFile());
                                mm = new ZMimeMessage(mJMSession, fis);
                            } else {
                                mm = new ZMimeMessage(mJMSession, buffer.getInputStream());
                            }
                            writeAttachedMessages(mm, outdir, entry.getName());
                            extractedIds.add(ud.id + "");
                        } catch (MessagingException me) {
                            LOG.warn("exception occurred fetching message", me);
                        } finally {
                            ByteUtil.closeStream(fis);
                        }

                    }
                }
            }
        }
        return extractedIds;
    }

    private static byte[] readArchiveEntry(ArchiveInputStream ais, ArchiveInputEntry aie)
    throws IOException {
        if (aie == null) {
            return null;
        }

        int dsz = (int) aie.getSize();
        byte[] data;

        if (dsz == 0) {
            return null;
        } else if (dsz == -1) {
            data = ByteUtil.getContent(ais.getInputStream(), -1, false);
        } else {
            data = new byte[dsz];
            if (ais.read(data, 0, dsz) != dsz) {
                throw new IOException("archive read err");
            }
        }
        return data;
    }

    private static void writeAttachedMessages(MimeMessage mm, File outdir, String msgUri)
    throws IOException, MessagingException {
        // Not raw - ignore the spam report and extract messages that are in attachments...
        if (!(mm.getContent() instanceof MimeMultipart)) {
            LOG.warn("Spam/notspam messages must have attachments (skipping " + msgUri + ")");
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
                if (msg instanceof MimeMessage) {
                    //bug 74435 clone into newMsg so our parser has a chance to handle headers which choke javamail
                    ZMimeMessage newMsg = new ZMimeMessage((MimeMessage) msg);
                    newMsg.writeTo(os);
                } else {
                    msg.writeTo(os);
                }
            } finally {
                os.close();
            }
            if (verbose) LOG.info("Wrote: " + file);
        }

        if (!foundAtleastOneAttachedMessage) {
            String msgid = mm.getHeader("Message-ID", " ");
            LOG.warn("message uri=" + msgUri + " message-id=" + msgid + " had no attachments");
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
        String file = admin ? AdminConstants.ADMIN_SERVICE_URI : AccountConstants.USER_SERVICE_URI;
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
            if (verbose) LOG.info("Auth request to: " + adminURL);
            if (LOG.isDebugEnabled()) LOG.debug(authReq.prettyPrint());
            Element authResp = transport.invokeWithoutSession(authReq);
            if (LOG.isDebugEnabled()) LOG.debug(authResp.prettyPrint());
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
            if (verbose) LOG.info("Delegate auth request to: " + adminURL);
            if (LOG.isDebugEnabled()) LOG.debug(daReq.prettyPrint());
            Element daResp = transport.invokeWithoutSession(daReq);
            if (LOG.isDebugEnabled()) LOG.debug(daResp.prettyPrint());
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
                LOG.error("only one of s, n or m options can be specified");
                return null;
            }
            name = conf.getAttr(Provisioning.A_zimbraSpamIsSpamAccount);
            if (name == null || name.length() == 0) {
                LOG.error("no account configured for spam");
                return null;
            }
        } else if (cl.hasOption('n')) {
            if (cl.hasOption('m')) {
                LOG.error("only one of s, n, or m options can be specified");
                return null;
            }
            name = conf.getAttr(Provisioning.A_zimbraSpamIsNotSpamAccount);
            if (name == null || name.length() == 0) {
                LOG.error("no account configured for ham");
                return null;
            }
        } else if (cl.hasOption('m')) {
            name = cl.getOptionValue('m');
            if (name.length() == 0) {
                LOG.error("illegal argument to m option");
                return null;
            }
        } else {
            LOG.error("one of s, n or m options must be specified");
            return null;
        }

        Account account = prov.get(AccountBy.name, name);
        if (account == null) {
            LOG.error("can not find account " + name);
            return null;
        }

        return account;
    }
}
