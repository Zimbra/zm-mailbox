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

package com.zimbra.qa.unittest;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.util.SharedByteArrayInputStream;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.testng.TestListenerAdapter;
import org.testng.TestNG;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.common.soap.Element.Attribute;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.DataSourceBy;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.client.LmcSession;
import com.zimbra.cs.client.soap.LmcAdminAuthRequest;
import com.zimbra.cs.client.soap.LmcAdminAuthResponse;
import com.zimbra.cs.client.soap.LmcAuthRequest;
import com.zimbra.cs.client.soap.LmcAuthResponse;
import com.zimbra.cs.client.soap.LmcSoapClientException;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.index.queryparser.ParseException;
import com.zimbra.cs.lmtpserver.utils.LmtpClient;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.util.BuildInfo;
import com.zimbra.cs.util.JMSession;
import com.zimbra.cs.zclient.ZContact;
import com.zimbra.cs.zclient.ZDataSource;
import com.zimbra.cs.zclient.ZDateTime;
import com.zimbra.cs.zclient.ZDocument;
import com.zimbra.cs.zclient.ZEmailAddress;
import com.zimbra.cs.zclient.ZFilterRule;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZGetInfoResult;
import com.zimbra.cs.zclient.ZGetMessageParams;
import com.zimbra.cs.zclient.ZInvite;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZMessage;
import com.zimbra.cs.zclient.ZMountpoint;
import com.zimbra.cs.zclient.ZSearchHit;
import com.zimbra.cs.zclient.ZSearchParams;
import com.zimbra.cs.zclient.ZTag;
import com.zimbra.cs.zclient.ZGrant.GranteeType;
import com.zimbra.cs.zclient.ZInvite.ZAttendee;
import com.zimbra.cs.zclient.ZInvite.ZClass;
import com.zimbra.cs.zclient.ZInvite.ZComponent;
import com.zimbra.cs.zclient.ZInvite.ZOrganizer;
import com.zimbra.cs.zclient.ZInvite.ZParticipantStatus;
import com.zimbra.cs.zclient.ZInvite.ZRole;
import com.zimbra.cs.zclient.ZInvite.ZStatus;
import com.zimbra.cs.zclient.ZInvite.ZTransparency;
import com.zimbra.cs.zclient.ZMailbox.ContactSortBy;
import com.zimbra.cs.zclient.ZMailbox.OwnerBy;
import com.zimbra.cs.zclient.ZMailbox.SharedItemBy;
import com.zimbra.cs.zclient.ZMailbox.ZAppointmentResult;
import com.zimbra.cs.zclient.ZMailbox.ZImportStatus;
import com.zimbra.cs.zclient.ZMailbox.ZOutgoingMessage;
import com.zimbra.cs.zclient.ZMailbox.ZOutgoingMessage.AttachedMessagePart;
import com.zimbra.cs.zclient.ZMailbox.ZOutgoingMessage.MessagePart;
import com.zimbra.cs.zclient.ZMessage.ZMimePart;

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
        if (userName.contains("@"))
            return userName;
        else
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
        return scheme + "://localhost:" + port + AccountConstants.USER_SERVICE_URI;
    }

    public static String getAdminSoapUrl() {
        int port;
        try {
            port = Provisioning.getInstance().getLocalServer().getIntAttr(Provisioning.A_zimbraAdminPort, 0);
        } catch (ServiceException e) {
            ZimbraLog.test.error("Unable to get admin SOAP port", e);
            port = LC.zimbra_admin_service_port.intValue();
        }
        return "https://localhost:" + port + AdminConstants.ADMIN_SERVICE_URI;
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

    public static Message addMessage(Mailbox mbox, String subject)
    throws Exception {
        return addMessage(mbox, Mailbox.ID_FOLDER_INBOX, subject, System.currentTimeMillis());
    }
    
    public static Message addMessage(Mailbox mbox, int folderId, String subject, long timestamp)
    throws Exception {
        String message = getTestMessage(subject, null, null, new Date(timestamp));
        ParsedMessage pm = new ParsedMessage(message.getBytes(), timestamp, false);
        return mbox.addMessage(null, pm, folderId, false, Flag.BITMASK_UNREAD, null);
    }
    
    private static String getTestMessage(String subject)
    throws ServiceException, MessagingException, IOException {
        return new MessageBuilder().withSubject(subject).create();
    }

    public static String getTestMessage(String subject, String recipient, String sender, Date date)
    throws ServiceException, MessagingException, IOException {
        return new MessageBuilder().withSubject(subject).withRecipient(recipient)
            .withFrom(sender).withDate(date).create();
    }

    static String addDomainIfNecessary(String user)
    throws ServiceException {
        if (user == null || user.contains("@")) {
            return user;
        }
        return String.format("%s@%s", user, getDomain());
    }
    
    public static boolean addMessageLmtp(String subject, String recipient, String sender)
    throws Exception {
        return addMessageLmtp(subject, new String[] { recipient }, sender);
    }

    public static boolean addMessageLmtp(String subject, String[] recipients, String sender)
    throws Exception {
        String message = getTestMessage(subject, recipients[0], sender, null);
        return addMessageLmtp(recipients, sender, message);
    }
    
    public static boolean addMessageLmtp(String[] recipients, String sender, String message)
    throws Exception {
        String[] recipWithDomain = new String[recipients.length];
        for (int i = 0; i < recipients.length; i++) {
            recipWithDomain[i] = addDomainIfNecessary(recipients[i]);
        }
        Provisioning prov = Provisioning.getInstance();
        LmtpClient lmtp = new LmtpClient("localhost", prov.getLocalServer().getIntAttr(Provisioning.A_zimbraLmtpBindPort, 7025));
        byte[] data = message.getBytes();
        boolean success = lmtp.sendMessage(new ByteArrayInputStream(data), recipWithDomain, addDomainIfNecessary(sender), "TestUtil", (long) data.length);
        lmtp.close();
        return success;
    }
    
    public static String addMessage(ZMailbox mbox, String subject)
    throws ServiceException, IOException, MessagingException {
        return addMessage(mbox, subject, Integer.toString(Mailbox.ID_FOLDER_INBOX));
    }
    
    public static String addMessage(ZMailbox mbox, String subject, String folderId)
    throws ServiceException, IOException, MessagingException {
        String message = getTestMessage(subject);
        return mbox.addMessage(folderId, null, null, 0, message, true);
    }

    public static String addMessage(ZMailbox mbox, String subject, String folderId, String flags)
    throws ServiceException, IOException, MessagingException {
        String message = getTestMessage(subject);
        return mbox.addMessage(folderId, flags, null, 0, message, true);
    }
    
    public static String addRawMessage(ZMailbox mbox, String rawMessage)
    throws ServiceException {
        return mbox.addMessage(Integer.toString(Mailbox.ID_FOLDER_INBOX), null, null, 0, rawMessage, true);
    }
    
    public static void sendMessage(ZMailbox senderMbox, String recipientName, String subject)
    throws Exception {
        String body = getTestMessage(subject);
        sendMessage(senderMbox, recipientName, subject, body);
    }
    
    public static void sendMessage(ZMailbox senderMbox, String recipientName, String subject, String body)
    throws Exception {
        sendMessage(senderMbox, recipientName, subject, body, null);
    }
    
    public static void sendMessage(ZMailbox senderMbox, String recipientName, String subject, String body, String attachmentUploadId)
    throws Exception {
        ZOutgoingMessage msg = getOutgoingMessage(recipientName, subject, body, attachmentUploadId);
        senderMbox.sendMessage(msg, null, false);
    }
    
    public static void saveDraftAndSendMessage(ZMailbox senderMbox, String recipient, String subject, String body, String attachmentUploadId)
    throws ServiceException {
        ZOutgoingMessage outgoingDraft = getOutgoingMessage(recipient, subject, body, attachmentUploadId);
        ZMessage draft = senderMbox.saveDraft(outgoingDraft, null, Integer.toString(Mailbox.ID_FOLDER_DRAFTS));
        
        ZOutgoingMessage outgoing = getOutgoingMessage(recipient, subject, body, null);
        if (attachmentUploadId != null) {
            AttachedMessagePart part = new AttachedMessagePart(draft.getId(), "2", null);
            outgoing.setMessagePartsToAttach(Arrays.asList(part));
        }
        senderMbox.sendMessage(outgoing, null, false);
    }
    
    public static ZOutgoingMessage getOutgoingMessage(String recipient, String subject, String body, String attachmentUploadId)
    throws ServiceException {
        ZOutgoingMessage msg = new ZOutgoingMessage();
        List<ZEmailAddress> addresses = new ArrayList<ZEmailAddress>();
        addresses.add(new ZEmailAddress(addDomainIfNecessary(recipient),
            null, null, ZEmailAddress.EMAIL_TYPE_TO));
        msg.setAddresses(addresses);
        msg.setSubject(subject);
        msg.setMessagePart(new MessagePart("text/plain", body));
        msg.setAttachmentUploadId(attachmentUploadId);
        return msg;
    }

    /**
     * Searches a mailbox and returns the id's of all matching items.
     */
    public static List<Integer> search(Mailbox mbox, String query, byte type)
    throws ServiceException, ParseException, IOException {
        return search(mbox, query, new byte[] { type });
    }
    
    /**
     * Searches a mailbox and returns the id's of all matching items.
     */
    public static List<Integer> search(Mailbox mbox, String query, byte[] types)
    throws ServiceException, ParseException, IOException {
        List<Integer> ids = new ArrayList<Integer>();
        ZimbraQueryResults r = mbox.search(new OperationContext(mbox), query, types, SortBy.DATE_DESCENDING, 100);
        while (r.hasNext()) {
            ZimbraHit hit = r.getNext();
            ids.add(new Integer(hit.getItemId()));
        }
        r.doneWithSearchResults();
        return ids;
    }
    
    
    public static List<String> search(ZMailbox mbox, String query, String type)
    throws ServiceException {
        List<String> ids = new ArrayList<String>();
        ZSearchParams params = new ZSearchParams(query);
        params.setTypes(type);
        for (ZSearchHit hit : mbox.search(params).getHits()) {
            ids.add(hit.getId());
        }
        return ids;
    }
    
    public static List<ZMessage> search(ZMailbox mbox, String query)
    throws ServiceException {
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
     * Gets the raw content of a message.
     */
    public static String getContent(ZMailbox mbox, String msgId)
    throws ServiceException {
        ZGetMessageParams msgParams = new ZGetMessageParams();
        msgParams.setId(msgId);
        msgParams.setRawContent(true);
        ZMessage msg = mbox.getMessage(msgParams);
        return msg.getContent();
    }
    
    public static byte[] getContent(ZMailbox mbox, String msgId, String name)
    throws ServiceException, IOException {
        ZMessage msg = mbox.getMessageById(msgId);
        ZMimePart part = getPart(msg, name);
        if (part == null) {
            return null;
        }
        return ByteUtil.getContent(mbox.getRESTResource("?id=" + msgId + "&part=" + part.getPartName()), 1024);
    }

    /**
     * Returns the mime part with a matching name, part name, or filename.
     */
    public static ZMimePart getPart(ZMessage msg, String name) {
        return getPart(msg.getMimeStructure(), name);
    }
    
    private static ZMimePart getPart(ZMimePart mimeStructure, String name) {
        for (ZMimePart child : mimeStructure.getChildren()) {
            ZMimePart part = getPart(child, name);
            if (part != null) {
                return part;
            }
        }
        if (StringUtil.equalIgnoreCase(mimeStructure.getName(), name) ||
            StringUtil.equalIgnoreCase(mimeStructure.getFileName(), name) ||
            StringUtil.equalIgnoreCase(mimeStructure.getPartName(), name)) {
            return mimeStructure;
        }
        return null;
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
        
        // Workaround for bug 31370
        deleteMessages(mbox, "subject: " + subjectSubstring);
        
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
        
        // Delete contacts
        for (ZContact contact : mbox.getAllContacts(null, ContactSortBy.nameAsc, false, null)) {
            String fullName = contact.getAttrs().get("fullName");
            if (fullName != null && fullName.contains(subjectSubstring)) {
                mbox.deleteContact(contact.getId());
            }
        }
        
        // Delete data sources
        List<ZDataSource> dataSources = mbox.getAllDataSources();
        for (ZDataSource ds : dataSources) {
            if (ds.getName().contains(subjectSubstring)) {
                mbox.deleteDataSource(ds);
            }
        }
        
        // Delete appointments
        List<String> ids = search(mbox, subjectSubstring, ZSearchParams.TYPE_APPOINTMENT);
        if (!ids.isEmpty()) {
            mbox.deleteItem(StringUtil.join(",", ids), null);
        }
        
        // Delete documents
        ids = search(mbox, subjectSubstring, ZSearchParams.TYPE_DOCUMENT);
        if (!ids.isEmpty()) {
            mbox.deleteItem(StringUtil.join(",", ids), null);
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

    private static boolean sIsCliInitialized = false;
    
    /**
     * Sets up the environment for command-line unit tests.
     */
    public static void cliSetup()
    throws ServiceException {
        if (!sIsCliInitialized) {
            CliUtil.toolSetup();
            SoapProvisioning sp = new SoapProvisioning();
            sp.soapSetURI("https://localhost:7071" + AdminConstants.ADMIN_SERVICE_URI);
            sp.soapZimbraAdminAuthenticate();
            Provisioning.setInstance(sp);
            SoapTransport.setDefaultUserAgent("Zimbra Unit Tests", BuildInfo.VERSION);
            sIsCliInitialized = true;
        }
    }

    public static void runTest(Class<?> testClass) {
        TestNG testng = TestUtil.newTestNG();
        ZimbraLog.test.info("Starting unit test %s.\nSee %s/index.html for results.",
            testClass.getName(), testng.getOutputDirectory());
        TestListenerAdapter listener = new TestListenerAdapter();
        testng.addListener(listener);
        testng.addListener(new TestLogger());
        
        Class<?>[] classArray = new Class<?>[1];
        classArray[0] = testClass;
        
        testng.setTestClasses(classArray);
        if (TestCase.class.isAssignableFrom(testClass)) {
            testng.setJUnit(true);
        }
        testng.run();
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
            sp.soapSetURI("https://localhost:7071" + AdminConstants.ADMIN_SERVICE_URI);
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
    
    public static String getAccountAttr(String userName, String attrName)
    throws ServiceException {
        String accountName = getAddress(userName);
        Account account = Provisioning.getInstance().getAccount(accountName);
        return account.getAttr(attrName);
    }
    
    public static String[] getAccountMultiAttr(String userName, String attrName)
    throws ServiceException {
        String accountName = getAddress(userName);
        Account account = Provisioning.getInstance().getAccount(accountName);
        return account.getMultiAttr(attrName);
    }
    
    public static void setAccountAttr(String userName, String attrName, String attrValue)
    throws ServiceException {
    	Provisioning prov = Provisioning.getInstance();
    	Account account = prov.get(AccountBy.name, getAddress(userName));
    	Map<String, Object> attrs = new HashMap<String, Object>();
    	attrs.put(attrName, attrValue);
    	prov.modifyAttrs(account, attrs);
    }
    
    public static void setAccountAttr(String userName, String attrName, String[] attrValues)
    throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(AccountBy.name, getAddress(userName));
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(attrName, attrValues);
        prov.modifyAttrs(account, attrs);
    }
    
    public static String getConfigAttr(String attrName)
    throws ServiceException {
        return Provisioning.getInstance().getConfig().getAttr(attrName, "");
    }
    
    public static void setConfigAttr(String attrName, String attrValue)
    throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Config config = prov.getConfig();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(attrName, attrValue);
        prov.modifyAttrs(config, attrs);
    }
    
    public static String getDomainAttr(String userName, String attrName)
    throws ServiceException {
        Account account = getAccount(userName);
        return Provisioning.getInstance().getDomain(account).getAttr(attrName);
    }
    
    public static void setDomainAttr(String userName, String attrName, Object attrValue)
    throws ServiceException {
        Account account = getAccount(userName);
        Domain domain = Provisioning.getInstance().getDomain(account);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(attrName, attrValue);
        Provisioning.getInstance().modifyAttrs(domain, attrs);
    }
    
    public static void setDataSourceAttr(String userName, String dataSourceName, String attrName, String attrValue)
    throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Account account = getAccount(userName);
        DataSource ds = prov.get(account, DataSourceBy.name, dataSourceName);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(attrName, attrValue);
        prov.modifyAttrs(ds, attrs);
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
        
        return mbox.createFolder(parentId, name, ZFolder.View.message, null, null, null);
    }
    
    public static ZFolder createFolder(ZMailbox mbox, String parentId, String folderName)
    throws ServiceException {
        return mbox.createFolder(parentId, folderName, ZFolder.View.message, null, null, null);
    }
    
    /**
     * Creates a mountpoint between two mailboxes.  The mountpoint gives the
     * "to" user full rights on the folder.
     * 
     * @param remoteMbox remote mailbox
     * @param remotePath remote folder path.  Folder is created if it doesn't exist.
     * @param localMbox local mailbox
     * @param mountpointName the name of the mountpoint folder.  The folder is created
     * directly under the user root.
     */
    public static ZMountpoint createMountpoint(ZMailbox remoteMbox, String remotePath,
                                               ZMailbox localMbox, String mountpointName)
    throws ServiceException {
        ZFolder remoteFolder = remoteMbox.getFolderByPath(remotePath);
        if (remoteFolder == null) {
            remoteFolder = createFolder(remoteMbox, remotePath);
        }
        ZGetInfoResult remoteInfo = remoteMbox.getAccountInfo(true);
        remoteMbox.modifyFolderGrant(
            remoteFolder.getId(), GranteeType.all, null, "rwidx", null);
        return localMbox.createMountpoint(Integer.toString(Mailbox.ID_FOLDER_USER_ROOT),
            mountpointName, null, null, null, OwnerBy.BY_ID, remoteInfo.getId(), SharedItemBy.BY_ID, remoteFolder.getId());
    }
    
    /**
     * Returns the data source with the given name.
     */
    public static ZDataSource getDataSource(ZMailbox mbox, String name)
    throws ServiceException {
        for (ZDataSource ds : mbox.getAllDataSources()) {
            if (ds.getName().equals(name)) {
                return ds;
            }
        }
        return null;
    }
    
    /**
     * Imports data from the given data source and updates state on both the local
     * and remote mailboxes.
     */
    public static void importDataSource(ZDataSource dataSource, ZMailbox localMbox, ZMailbox remoteMbox)
    throws Exception {
        importDataSource(dataSource, localMbox, remoteMbox, true);
    }
    
    /**
     * Imports data from the given data source and updates state on both the local
     * and remote mailboxes.
     */
    public static void importDataSource(ZDataSource dataSource, ZMailbox localMbox, ZMailbox remoteMbox, boolean expectedSuccess)
    throws Exception {
        List<ZDataSource> dataSources = new ArrayList<ZDataSource>();
        dataSources.add(dataSource);
        try {
            localMbox.importData(dataSources);
            assertTrue(expectedSuccess);
        } catch (SoapFaultException e) {
            if (expectedSuccess) {
                throw e;
            }
            return;
        }
        String type = dataSource.getType().toString();
        
        // Wait for import to complete
        ZImportStatus status;
        while (true) {
            Thread.sleep(500);

            status = null;
            for (ZImportStatus iter : localMbox.getImportStatus()) {
                if (iter.getId().equals(dataSource.getId())) {
                    status = iter;
                }
            }
            assertNotNull("No import status returned for data source " + dataSource.getName(), status);
            assertEquals("Unexpected data source type", type, status.getType());
            if (!status.isRunning()) {
                break;
            }
        }
        assertTrue("Import failed: " + status.getError(), status.getSuccess());
        
        // Get any state changes from the server 
        localMbox.noOp();
        if (remoteMbox != null) {
            remoteMbox.noOp();
        }
    }
    
    /**
     * Returns an authenticated transport for the <tt>admin</tt> account.
     */
    public static SoapTransport getAdminSoapTransport()
    throws SoapFaultException, IOException, ServiceException {
        SoapHttpTransport transport = new SoapHttpTransport(getAdminSoapUrl());
        
        // Create auth element
        Element auth = new XMLElement(AdminConstants.AUTH_REQUEST);
        auth.addElement(AdminConstants.E_NAME).setText(getAddress("admin"));
        auth.addElement(AdminConstants.E_PASSWORD).setText(DEFAULT_PASSWORD);
        
        // Authenticate and get auth token
        Element response = transport.invoke(auth);
        String authToken = response.getElement(AccountConstants.E_AUTH_TOKEN).getText();
        transport.setAuthToken(authToken);
        return transport;
    }

    /**
     * Assert the message contains the given sub-message, ignoring newlines.  Used
     * for comparing equality of two messages, when one had <tt>Return-Path</tt> or
     * other headers prepended.
     */
    public static void assertMessageContains(String message, String subMessage)
    throws IOException {
        BufferedReader msgReader = new BufferedReader(new StringReader(message));
        BufferedReader subReader = new BufferedReader(new StringReader(subMessage));
        String firstLine = subReader.readLine();
        String line;
        boolean foundFirstLine = false;
        
        while ((line = msgReader.readLine()) != null) {
            if (line.equals(firstLine)) {
                foundFirstLine = true;
                break;
            }
        }
        
        String context = String.format("Could not find '%s' in message:\n", firstLine, message);
        assertTrue(context, foundFirstLine);
        
        while(true) {
            line = msgReader.readLine();
            String subLine = subReader.readLine();
            if (line == null || subLine == null) {
                break;
            }
            assertEquals(subLine, line);
        }
        
    }
    
    /**
     * Asserts that two elements and all children in their hierarchy are equal. 
     */
    public static void assertEquals(Element expected, Element actual) {
        assertEquals(expected, actual, expected.prettyPrint(), actual.prettyPrint());
    }
    
    private static void assertEquals(Element expected, Element actual, String expectedDump, String actualDump) {
        assertEquals(expected.getName(), actual.getName());
        List<Element> expectedChildren = expected.listElements();
        List<Element> actualChildren = actual.listElements();
        String context = String.format("Element %s, expected:\n%s\nactual:\n%s", expected.getName(), expectedDump, actualDump);
        assertEquals(context + " children", getElementNames(expectedChildren), getElementNames(actualChildren));
        
        // Compare child elements
        for (int i = 0; i < expectedChildren.size(); i++) {
            assertEquals(expectedChildren.get(i), actualChildren.get(i), expectedDump, actualDump);
        }
        
        // Compare text
        assertEquals(expected.getTextTrim(), actual.getTextTrim());
        
        // Compare attributes
        Set<Attribute> expectedAttrs = expected.listAttributes();
        Set<Attribute> actualAttrs = actual.listAttributes();
        assertEquals(context + " attributes", getAttributesAsString(expectedAttrs), getAttributesAsString(actualAttrs));
    }
    
    /**
     * Asserts that two byte arrays are equal.
     */
    public static void assertEquals(byte[] expected, byte[] actual) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected == null) {
            Assert.fail("expected was null but actual was not.");
        }
        if (actual == null) {
            Assert.fail("expected was not null but actual was.");
        }
        assertEquals("Arrays have different length.", expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals("Data mismatch at byte " + i, expected[i], actual[i]);
        }
    }
    
    private static String getElementNames(List<Element> elements) {
        StringBuilder buf = new StringBuilder();
        for (Element e : elements) {
            if (buf.length() > 0) {
                buf.append(",");
            }
            buf.append(e.getName());
        }
        return buf.toString();
    }
    
    private static String getAttributesAsString(Set<Attribute> attrs) {
        Set<String> attrStrings = new TreeSet<String>();
        for (Attribute attr : attrs) {
            attrStrings.add(String.format("%s=%s", attr.getKey(), attr.getValue()));
        }
        return StringUtil.join(",", attrStrings);
    }
    /**    
     * Returns a new <tt>TestNG</tt> object that writes test results to
     * <tt>/opt/zimbra/test-output</tt>. 
     */
    public static TestNG newTestNG() {
        TestNG testng = new TestNG();
        testng.setOutputDirectory("/opt/zimbra/test-output");
        return testng;
    }
    
    public static String getHeaderValue(ZMailbox mbox, ZMessage msg, String headerName)
    throws Exception {
        String content = msg.getContent();
        if (content == null) {
            content = getContent(mbox, msg.getId());
        }
        assertNotNull("Content was not fetched from the server", content);
        MimeMessage mimeMsg = new MimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(content.getBytes()));
        return mimeMsg.getHeader(headerName, null);
    }
    
    public static ZAppointmentResult createAppointment(ZMailbox mailbox, String subject, String attendee, Date startDate, Date endDate)
    throws ServiceException {
        ZInvite invite = new ZInvite();
        ZInvite.ZComponent comp = new ZComponent();

        comp.setStatus(ZStatus.CONF);
        comp.setClassProp(ZClass.PUB);
        comp.setTransparency(ZTransparency.O);

        comp.setStart(new ZDateTime(startDate.getTime(), false, TimeZone.getDefault()));
        comp.setEnd(new ZDateTime(endDate.getTime(), false, TimeZone.getDefault()));
        comp.setName(subject);
        comp.setOrganizer(new ZOrganizer(mailbox.getName()));

        if (attendee != null) {
            attendee = addDomainIfNecessary(attendee);
            ZAttendee zattendee = new ZAttendee();
            zattendee.setAddress(attendee);
            zattendee.setRole(ZRole.REQ);
            zattendee.setParticipantStatus(ZParticipantStatus.NE);
            zattendee.setRSVP(true);
            comp.getAttendees().add(zattendee);
        }

        invite.getComponents().add(comp);

        ZOutgoingMessage m = null;
        if (attendee != null) {
            m = getOutgoingMessage(attendee, subject, "Test appointment", null);
        }

        return mailbox.createAppointment(ZFolder.ID_CALENDAR, null, m, invite, null);
    }
    
    public static void sendInviteReply(ZMailbox mbox, String inviteId, String organizer, String subject, ZMailbox.ReplyVerb replyVerb)
    throws ServiceException {
        organizer = addDomainIfNecessary(organizer);
        ZOutgoingMessage msg = getOutgoingMessage(organizer, subject, "Reply to appointment " + inviteId, null);
        mbox.sendInviteReply(inviteId, "0", replyVerb, true, null, null, msg);
    }
    
    public static ZFilterRule getFilterRule(ZMailbox mbox, String ruleName)
    throws ServiceException {
        for (ZFilterRule rule : mbox.getFilterRules(true).getRules()) {
            if (rule.getName().equals(ruleName)) {
                return rule;
            }
        }
        return null;
    }
    
    public static ZDocument createDocument(ZMailbox mbox, String folderId, String name, String contentType, byte[] content)
    throws ServiceException {
        String attachId = mbox.uploadAttachment(name, content, contentType, 0);
        String docId = mbox.createDocument(folderId, name, attachId);
        return mbox.getDocument(docId);
    }
}
