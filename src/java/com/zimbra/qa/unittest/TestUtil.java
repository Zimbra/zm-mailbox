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

package com.zimbra.qa.unittest;

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
import com.zimbra.common.soap.SoapFaultException;
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
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZGetMessageParams;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZMessage;
import com.zimbra.cs.zclient.ZSearchHit;
import com.zimbra.cs.zclient.ZSearchParams;
import com.zimbra.cs.zclient.ZTag;

/**
 * @author bburtin
 */
public class TestUtil {
    
    public static final String DEFAULT_PASSWORD = "test123";

    public static boolean accountExists(String userName)
    throws ServiceException {
        String address = getAddress(userName);
        Account account = Provisioning.getInstance().get(AccountBy.name, address);
        return account != null;
    }
    
    public static Account getAccount(String userName)
    throws ServiceException {
        String address = getAddress(userName);
        Account account = Provisioning.getInstance().get(AccountBy.name, address);
        if (account == null) {
            throw new IllegalArgumentException("Could not find account for '" + address + "'");
        }
        return account;
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
        "Jeff",
        "",
        "(${SUBJECT} ${MESSAGE_NUM})"
    };

    private static String MESSAGE_TEMPLATE = StringUtil.join("\n", MESSAGE_TEMPLATE_LINES);

    public static Message insertMessage(Mailbox mbox, int messageNum, String subject)
    throws Exception {
        String message = getTestMessage(messageNum, subject);
        ParsedMessage pm = new ParsedMessage(message.getBytes(), System.currentTimeMillis(), false);
        pm.analyze();
        return mbox.addMessage(null, pm, Mailbox.ID_FOLDER_INBOX, false, Flag.BITMASK_UNREAD, null);
    }
    
    private static String getDateHeaderValue() {
        return String.format("%1$ta, %1$td %1$tb %1$tY %1$tH:%1$tM:%1$tS %1$tz (%1$tZ)", new Date());
    }

    private static String getTestMessage(int messageNum, String subject)
    throws ServiceException {
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("MESSAGE_NUM", new Integer(messageNum));
        vars.put("SUBJECT", subject);
        vars.put("DOMAIN", getDomain());
        vars.put("SENDER", "jspiccoli");
        vars.put("RECIPIENT", "user1");
        vars.put("DATE", getDateHeaderValue());
        return StringUtil.fillTemplate(MESSAGE_TEMPLATE, vars);
    }

    private static String getTestMessage(int messageNum, String subject, String recipient, String sender)
    throws ServiceException {
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("MESSAGE_NUM", new Integer(messageNum));
        vars.put("SUBJECT", subject);
        vars.put("DOMAIN", getDomain());
        vars.put("SENDER", sender);
        vars.put("RECIPIENT", recipient);
        vars.put("DATE", getDateHeaderValue());
        return StringUtil.fillTemplate(MESSAGE_TEMPLATE, vars);
    }

    public static void insertMessageLmtp(int messageNum, String subject, String recipient, String sender)
    throws Exception {
        String message = getTestMessage(messageNum, subject, recipient, sender);
        LmtpClient lmtp = new LmtpClient("localhost", 7025);
        List<String> recipients = new ArrayList<String>();
        recipients.add(recipient);
        lmtp.sendMessage(message.getBytes(), recipients, sender, "TestUtil");
        lmtp.close();
    }
    
    public static String insertMessage(ZMailbox mbox, int messageNum, String subject)
    throws ServiceException {
        return insertMessage(mbox, messageNum, subject, Integer.toString(Mailbox.ID_FOLDER_INBOX));
    }
    
    public static String insertMessage(ZMailbox mbox, int messageNum, String subject, String folderId)
    throws ServiceException {
        String message = getTestMessage(messageNum, subject);
        return mbox.addMessage(folderId, null, null, 0, message, true);
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
        return runTest(t, null);
    }

    /**
     * Runs a test and writes the output to the specified
     * <code>OutputStream</code>.
     */
    public static TestResult runTest(Test t, OutputStream outputStream) {
        ZimbraLog.test.debug("Starting unit test suite");

        long suiteStart = System.currentTimeMillis();
        TestResult result = new TestResult();
        ZimbraTestListener listener = new ZimbraTestListener();
        result.addListener(listener);
        t.run(result);

        double seconds = (double) (System.currentTimeMillis() - suiteStart) / 1000;
        String msg = String.format(
            "Unit test suite finished in %.2f seconds.  %d errors, %d failures.\n%s",
            seconds, result.errorCount(), result.failureCount(), listener.getSummary());
        ZimbraLog.test.info(msg);

        if (outputStream != null) {
            try {
                outputStream.write(msg.getBytes());
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
    
    public static SoapProvisioning getSoapProvisioning()
    throws ServiceException {
        SoapProvisioning sp = new SoapProvisioning();
        sp.soapSetURI("https://localhost:7071" + ZimbraServlet.ADMIN_SERVICE_URI);
        sp.soapZimbraAdminAuthenticate();
        return sp;
    }

    /**
     * Creates an account for the given username, with
     * password set to {@link #DEFAULT_PASSWORD}.
     */
    public static Account createAccount(String username)
    throws ServiceException {
        Provisioning prov = getSoapProvisioning();
        String address = getAddress(username);
        return prov.createAccount(address, DEFAULT_PASSWORD, null);
    }
    
    /**
     * Deletes the account for the given username.
     */
    public static void deleteAccount(String username)
    throws ServiceException {
        Provisioning prov = getSoapProvisioning();
        Account account = getAccount(username);
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
}
