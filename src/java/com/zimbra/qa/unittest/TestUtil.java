/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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

package com.zimbra.qa.unittest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestResult;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.client.LmcSession;
import com.zimbra.cs.client.soap.LmcAdminAuthRequest;
import com.zimbra.cs.client.soap.LmcAdminAuthResponse;
import com.zimbra.cs.client.soap.LmcAuthRequest;
import com.zimbra.cs.client.soap.LmcAuthResponse;
import com.zimbra.cs.client.soap.LmcSoapClientException;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.lmtpserver.utils.LmtpClient;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.zclient.ZDataSource;
import com.zimbra.cs.zclient.ZEmailAddress;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZGetMessageParams;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZMessage;
import com.zimbra.cs.zclient.ZSearchHit;
import com.zimbra.cs.zclient.ZSearchParams;
import com.zimbra.cs.zclient.ZTag;
import com.zimbra.cs.zclient.ZMailbox.ZImportStatus;
import com.zimbra.cs.zclient.ZMailbox.ZOutgoingMessage;
import com.zimbra.cs.zclient.ZMailbox.ZOutgoingMessage.MessagePart;

/**
 * @author bburtin
 */
public class TestUtil
extends Assert {
    
    public static final String DEFAULT_PASSWORD = "test123";

    public static boolean accountExists(String userName)
    throws ServiceException {
        String address = getAddress(userName);
        Account account = Provisioning.getInstance().get(AccountBy.name, address);
        return (account != null);
    }
    
    public static Account getAccount(String userName)
    throws ServiceException {
        String address = getAddress(userName);
        return Provisioning.getInstance().get(AccountBy.name, address);
    }

    public static String getDomain()
    throws ServiceException {
        Config config = Provisioning.getInstance().getConfig();
        String domain = config.getAttr(Provisioning.A_zimbraDefaultDomainName, null);
        assert(domain != null && domain.length() > 0);
        return domain;
    }

    public static Mailbox getMailbox(String userName)
    throws ServiceException {
        Account account = getAccount(userName);
        return MailboxManager.getInstance().getMailboxByAccount(account);
    }

    public static String getAddress(String userName)
    throws ServiceException {
        return userName + "@" + getDomain();
    }

    public static String getSoapUrl() {
        String scheme;
        int port;
        try {
            port = Provisioning.getInstance().getLocalServer().getIntAttr(Provisioning.A_zimbraMailPort, 0);
            if (port > 0) {
                scheme = "http";
            } else {
                port = Provisioning.getInstance().getLocalServer().getIntAttr(Provisioning.A_zimbraMailSSLPort, 0);
                scheme = "https";
            }
        } catch (ServiceException e) {
            ZimbraLog.test.error("Unable to get user SOAP port", e);
            port = 80;
            scheme = "http";
        }
        return scheme + "://localhost:" + port + ZimbraServlet.USER_SERVICE_URI;
    }

    public static String getAdminSoapUrl() {
        int port;
        try {
            port = Provisioning.getInstance().getLocalServer().getIntAttr(Provisioning.A_zimbraAdminPort, 0);
        } catch (ServiceException e) {
            ZimbraLog.test.error("Unable to get admin SOAP port", e);
            port = LC.zimbra_admin_service_port.intValue();
        }
        return "https://localhost:" + port + ZimbraServlet.ADMIN_SERVICE_URI;
    }

    public static LmcSession getSoapSession(String userName)
    throws ServiceException, LmcSoapClientException, IOException, SoapFaultException
    {
        LmcAuthRequest auth = new LmcAuthRequest();
        auth.setUsername(getAddress(userName));
        auth.setPassword(DEFAULT_PASSWORD);
        LmcAuthResponse authResp = (LmcAuthResponse) auth.invoke(getSoapUrl());
        return authResp.getSession();
    }

    public static LmcSession getAdminSoapSession()
    throws Exception
    {
        // Authenticate
        LmcAdminAuthRequest auth = new LmcAdminAuthRequest();
        auth.setUsername(getAddress("admin"));
        auth.setPassword(DEFAULT_PASSWORD);
        LmcAdminAuthResponse authResp = (LmcAdminAuthResponse) auth.invoke(getAdminSoapUrl());
        return authResp.getSession();
    }

    private static String[] MESSAGE_TEMPLATE_LINES = {
        "From: Jeff Spiccoli <${SENDER}@${DOMAIN}>",
        "To: Test User 1 <${RECIPIENT}@${DOMAIN}>",
        "Subject: ${SUBJECT}",
        "Date: ${DATE}",
        "Content-Type: text/plain",
        "",
        "Dude,",
        "",
        "All I need are some tasty waves, a cool buzz, and I'm fine.",
        "",
        "Jeff"
    };

    private static String MESSAGE_TEMPLATE = StringUtil.join("\n", MESSAGE_TEMPLATE_LINES);

    public static Message addMessage(Mailbox mbox, String subject)
    throws Exception {
        return addMessage(mbox, Mailbox.ID_FOLDER_INBOX, subject, System.currentTimeMillis());
    }
    
    public static Message addMessage(Mailbox mbox, int folderId, String subject, long timestamp)
    throws Exception {
        String message = getTestMessage(subject, null, null, new Date(timestamp));
        ParsedMessage pm = new ParsedMessage(message.getBytes(), timestamp, false);
        pm.analyze();
        return mbox.addMessage(null, pm, folderId, false, Flag.BITMASK_UNREAD, null);
    }
    
    private static String getDateHeaderValue(Date date) {
        return String.format("%1$ta, %1$td %1$tb %1$tY %1$tH:%1$tM:%1$tS %1$tz (%1$tZ)", date);
    }

    private static String getTestMessage(String subject)
    throws ServiceException {
        return getTestMessage(subject, null, null, null);
    }

    public static String getTestMessage(String subject, String recipient, String sender, Date date)
    throws ServiceException {
        if (recipient == null) {
            recipient = "user1";
        }
        if (sender == null) {
            sender = "jspiccoli";
        }
        if (date == null) {
            date = new Date();
        }
        
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("SUBJECT", subject);
        vars.put("DOMAIN", getDomain());
        vars.put("SENDER", sender);
        vars.put("RECIPIENT", recipient);
        vars.put("DATE", getDateHeaderValue(date));
        return StringUtil.fillTemplate(MESSAGE_TEMPLATE, vars);
    }

    public static String getTestMessage(String subject, String body, String recipient, String sender, Date date)
    throws ServiceException {
        if (recipient == null) {
            recipient = "user1";
        }
        if (sender == null) {
            sender = "jspiccoli";
        }
        if (date == null) {
            date = new Date();
        }
        
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("SUBJECT", subject);
        vars.put("DOMAIN", getDomain());
        vars.put("SENDER", sender);
        vars.put("RECIPIENT", recipient);
        vars.put("DATE", getDateHeaderValue(date));
        return StringUtil.fillTemplate(MESSAGE_TEMPLATE, vars);
    }
    
    public static void addMessageLmtp(String subject, String recipient, String sender)
    throws Exception {
        addMessageLmtp(subject, new String[] { recipient }, sender);
    }

    public static void addMessageLmtp(String subject, String[] recipients, String sender)
    throws Exception {
        String message = getTestMessage(subject, recipients[0], sender, null);
        LmtpClient lmtp = new LmtpClient("localhost", 7025);
        byte[] data = message.getBytes();
        lmtp.sendMessage(new ByteArrayInputStream(data), recipients, sender, "TestUtil", (long) data.length);
        lmtp.close();
    }
    
    public static String addMessage(ZMailbox mbox, int messageNum, String subject)
    throws ServiceException {
        return addMessage(mbox, messageNum, subject, Integer.toString(Mailbox.ID_FOLDER_INBOX));
    }
    
    public static String addMessage(ZMailbox mbox, int messageNum, String subject, String folderId)
    throws ServiceException {
        String message = getTestMessage(subject);
        return mbox.addMessage(folderId, null, null, 0, message, true);
    }

    public static void sendMessage(ZMailbox senderMbox, String recipientName, String subject, String body)
    throws Exception {
        sendMessage(senderMbox, recipientName, subject, body, null);
    }
    
    public static void sendMessage(ZMailbox senderMbox, String recipientName, String subject, String body, String attachmentUploadId)
    throws Exception {
        ZOutgoingMessage msg = new ZOutgoingMessage();
        List<ZEmailAddress> addresses = new ArrayList<ZEmailAddress>();
        addresses.add(new ZEmailAddress(TestUtil.getAddress(recipientName),
            null, null, ZEmailAddress.EMAIL_TYPE_TO));
        msg.setAddresses(addresses);
        msg.setSubject(subject);
        msg.setMessagePart(new MessagePart("text/plain", body));
        msg.setAttachmentUploadId(attachmentUploadId);
        senderMbox.sendMessage(msg, null, false);
    }

    /**
     * Searches a mailbox and returns the id's of all matching items.
     */
    public static List<Integer> search(Mailbox mbox, String query, byte type)
    throws Exception {
        ZimbraLog.test.debug("Running search: '" + query + "', type=" + type);
        byte[] types = new byte[1];
        types[0] = type;

        List<Integer> ids = new ArrayList<Integer>();
        ZimbraQueryResults r = mbox.search(new Mailbox.OperationContext(mbox), query, types, MailboxIndex.SortBy.DATE_DESCENDING, 100);
        while (r.hasNext()) {
            ZimbraHit hit = r.getNext();
            ids.add(new Integer(hit.getItemId()));
        }
        return ids;

    }
    

    public static List<ZMessage> search(ZMailbox mbox, String query)
    throws Exception {
        List<ZMessage> msgs = new ArrayList<ZMessage>();
        ZSearchParams params = new ZSearchParams(query);
        params.setTypes(ZSearchParams.TYPE_MESSAGE);

        for (ZSearchHit hit : mbox.search(params).getHits()) {
            ZGetMessageParams msgParams = new ZGetMessageParams();
            msgParams.setId(hit.getId());
            msgs.add(mbox.getMessage(msgParams));
        }
        return msgs;
    }
    
    public static ZMessage waitForMessage(ZMailbox mbox, String query)
    throws Exception {
        for (int i = 1; i <= 20; i++) {
            List<ZMessage> msgs = search(mbox, query);
            if (msgs.size() == 1) {
                return msgs.get(0);
            }
            if (msgs.size() > 1) {
                Assert.fail("Unexpected number of messages (" + msgs.size() + ") returned by query '" + query + "'");
            }
            Thread.sleep(500);
        }
        Assert.fail("Message for query '" + query + "' never arrived.  Either the MTA is not running or the test failed.");
        return null;
    }

    /**
     * Returns a folder with the given path, or <code>null</code> if the folder
     * doesn't exist.
     */
    public static Folder getFolderByPath(Mailbox mbox, String path)
    throws Exception {
        Folder folder = null;
        try {
            folder = mbox.getFolderByPath(null, path);
        } catch (MailServiceException e) {
            if (e.getCode() != MailServiceException.NO_SUCH_FOLDER) {
                throw e;
            }
        }
        return folder;
    }

    /**
     * Delete all messages, tags and folders in the user's mailbox
     * whose subjects contain the given substring.  For messages, the
     * subject must contain subjectString as a separate word.  Tags
     * and folders can have the string anywhere in the name. 
     */
    public static void deleteTestData(String userName, String subjectSubstring)
    throws ServiceException {
        ZMailbox mbox = TestUtil.getZMailbox(userName);
        
        deleteMessages(mbox, "is:anywhere " + subjectSubstring);
        
        // Workaround for bug 15160 (is:anywhere is busted)
        deleteMessages(mbox, "in:trash " + subjectSubstring);
        deleteMessages(mbox, "in:junk " + subjectSubstring);
        deleteMessages(mbox, "in:sent " + subjectSubstring);
        
        // Delete tags
        for (ZTag tag : mbox.getAllTags()) {
            if (tag.getName().contains(subjectSubstring)) {
                mbox.deleteTag(tag.getId());
            }
        }
        
        // Delete folders
        for (ZFolder folder : mbox.getAllFolders()) {
            if (folder.getName().contains(subjectSubstring)) {
                mbox.deleteFolder(folder.getId());
            }
        }
    }

    private static void deleteMessages(ZMailbox mbox, String query)
    throws ServiceException {
        // Delete messages
        ZSearchParams params = new ZSearchParams(query);
        params.setTypes(ZSearchParams.TYPE_MESSAGE);
        List<ZSearchHit> hits = mbox.search(params).getHits();
        if (hits.size() > 0) {
            List<String> ids = new ArrayList<String>();
            for (ZSearchHit hit : hits) {
                ids.add(hit.getId());
            }
            mbox.deleteMessage(StringUtil.join(",", ids));
        }
    }

    /**
     * Runs a test and writes the output to the logfile.
     */
    public static TestResult runTest(Test t) {
        return runTest(t, (OutputStream)null);
    }
    
    /**
     * Sets up the environment for command-line unit tests.
     */
    static void cliSetup()
    throws ServiceException {
        CliUtil.toolSetup();
        SoapProvisioning sp = new SoapProvisioning();
        sp.soapSetURI("https://localhost:7071" + ZimbraServlet.ADMIN_SERVICE_URI);
        sp.soapZimbraAdminAuthenticate();
        Provisioning.setInstance(sp);
    }

    /**
     * Runs a test and writes the output to the specified
     * <code>OutputStream</code>.
     */
    public static TestResult runTest(Test t, Element parent) {
        ZimbraLog.test.debug("Starting unit test suite");

        long suiteStart = System.currentTimeMillis();
        TestResult result = new TestResult();
        ZimbraTestListener listener = new ZimbraTestListener(parent);
        result.addListener(listener);
        t.run(result);

        double seconds = (double) (System.currentTimeMillis() - suiteStart) / 1000;
        String msg = String.format(
            "Unit test suite finished in %.2f seconds.  %d errors, %d failures.",
            seconds, result.errorCount(), result.failureCount());
        ZimbraLog.test.info(msg);

        return result;
    }
    
    /**
     * Runs a test and writes the output to the specified
     * <code>OutputStream</code>.
     */
    public static TestResult runTest(Test t, OutputStream outputStream) {
        ZimbraLog.test.debug("Starting unit test suite");

        long suiteStart = System.currentTimeMillis();
        TestResult result = new TestResult();
        ZimbraTestListener listener = new ZimbraTestListener(null);
        result.addListener(listener);
        t.run(result);

        double seconds = (double) (System.currentTimeMillis() - suiteStart) / 1000;
        String msg = String.format(
            "Unit test suite finished in %.2f seconds.  %d errors, %d failures.",
            seconds, result.errorCount(), result.failureCount());
        ZimbraLog.test.info(msg);

        if (outputStream != null) {
            try {
                outputStream.write(msg.getBytes());
                outputStream.write('\n');
            } catch (IOException e) {
                ZimbraLog.test.error(e.toString());
            }
        }

        return result;
    }
    
    
    public static ZMailbox getZMailbox(String username)
    throws ServiceException {
        ZMailbox.Options options = new ZMailbox.Options();
        options.setAccount(getAddress(username));
        options.setAccountBy(AccountBy.name);
        options.setPassword(DEFAULT_PASSWORD);
        options.setUri(getSoapUrl());
        return ZMailbox.getMailbox(options);
    }
    
    /**
     * Creates an account for the given username, with
     * password set to {@link #DEFAULT_PASSWORD}.
     */
    public static Account createAccount(String username)
    throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        String address = getAddress(username);
        return prov.createAccount(address, DEFAULT_PASSWORD, null);
    }
    
    /**
     * Deletes the account for the given username.
     */
    public static void deleteAccount(String username)
    throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        
        // If this code is running on the server, call SoapProvisioning explicitly
        // so that both the account and mailbox are deleted.
        if (!(prov instanceof SoapProvisioning)) {
            SoapProvisioning sp = new SoapProvisioning();
            sp.soapSetURI("https://localhost:7071" + ZimbraServlet.ADMIN_SERVICE_URI);
            sp.soapZimbraAdminAuthenticate();
            prov = sp;
        }
        Account account = prov.get(AccountBy.name, getAddress(username));
        if (account != null) {
            prov.deleteAccount(account.getId());
        }
    }
    
    public static String getServerAttr(String attrName)
    throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Server server = prov.getLocalServer();
        return server.getAttr(attrName, null);
    }
    
    public static void setServerAttr(String attrName, String attrValue)
    throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Server server = prov.getLocalServer();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(attrName, attrValue);
        prov.modifyAttrs(server, attrs);
    }
    
    /**
     * Verifies that a message is tagged.
     */
    public static void verifyTag(ZMailbox mbox, ZMessage msg, String tagName)
    throws Exception {
        List<ZTag> tags = mbox.getTags(msg.getTagIds());
        for (ZTag tag : tags) {
            if (tag.getName().equals(tagName)) {
                return;
            }
        }
        Assert.fail("Message not tagged with " + tagName);
    }

    public static ZMessage getMessage(ZMailbox mbox, String query)
    throws Exception {
        List<ZMessage> results = search(mbox, query);
        String errorMsg = String.format("Unexpected number of messages returned by query '%s'", query);
        Assert.assertEquals(errorMsg, 1, results.size());
        return results.get(0);
    }
    
    /**
     * Verifies that a message is flagged.
     */
    public static void verifyFlag(ZMailbox mbox, ZMessage msg, ZMessage.Flag flag) {
        String flags = msg.getFlags();
        String errorMsg = String.format("Flag %s not found in %s", flag.getFlagChar(), msg.getFlags());
        Assert.assertTrue(errorMsg, flags.indexOf(flag.getFlagChar()) >= 0);
    }
    
    public static ZFolder createFolder(ZMailbox mbox, String path)
    throws ServiceException {
        String parentId = Integer.toString(Mailbox.ID_FOLDER_USER_ROOT);
        String name = null;
        int idxLastSlash = path.lastIndexOf('/');
        
        if (idxLastSlash < 0) {
            name = path;
        } else if (idxLastSlash == 0) {
            name = path.substring(1);
        } else {
            String parentPath = path.substring(0, idxLastSlash);
            name = path.substring(idxLastSlash + 1);
            ZFolder parent = mbox.getFolderByPath(parentPath);
            if (parent == null) {
                String msg = String.format("Creating folder %s: parent %s does not exist", name, parentPath);
                throw ServiceException.FAILURE(msg, null);
            }
            parentId = parent.getId();
        }
        
        return mbox.createFolder(parentId, name, null, null, null, null);
    }
    
    /**
     * Imports data from the given data source and updates state on both the local
     * and remote mailboxes.
     */
    public static void importDataSource(ZDataSource dataSource, ZMailbox localMbox, ZMailbox remoteMbox)
    throws Exception {
        List<ZDataSource> dataSources = new ArrayList<ZDataSource>();
        dataSources.add(dataSource);
        localMbox.importData(dataSources);
        String type = dataSource.getType().toString();
        
        // Wait for import to complete
        ZImportStatus status = null;
        while (true) {
            Thread.sleep(500);
            List<ZImportStatus> statusList = localMbox.getImportStatus();
            assertEquals("Unexpected number of imports running", 1, statusList.size());
            status = statusList.get(0);
            assertEquals("Unexpected data source type", type, status.getType());
            if (!status.isRunning()) {
                break;
            }
        }
        assertTrue("Import failed: " + status.getError(), status.getSuccess());
        
        // Get any state changes from the server 
        localMbox.noOp();
        remoteMbox.noOp();
    }

}
