/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

package com.zimbra.qa.unittest;


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.util.SharedByteArrayInputStream;

import org.apache.http.HttpException;
import org.dom4j.DocumentException;
import org.junit.internal.AssumptionViolatedException;
import org.junit.runner.JUnitCore;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.zimbra.client.ZAuthResult;
import com.zimbra.client.ZContact;
import com.zimbra.client.ZDataSource;
import com.zimbra.client.ZDateTime;
import com.zimbra.client.ZDocument;
import com.zimbra.client.ZEmailAddress;
import com.zimbra.client.ZFilterRule;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZGetInfoResult;
import com.zimbra.client.ZGetMessageParams;
import com.zimbra.client.ZGrant.GranteeType;
import com.zimbra.client.ZIdentity;
import com.zimbra.client.ZInvite;
import com.zimbra.client.ZInvite.ZAttendee;
import com.zimbra.client.ZInvite.ZClass;
import com.zimbra.client.ZInvite.ZComponent;
import com.zimbra.client.ZInvite.ZOrganizer;
import com.zimbra.client.ZInvite.ZParticipantStatus;
import com.zimbra.client.ZInvite.ZRole;
import com.zimbra.client.ZInvite.ZStatus;
import com.zimbra.client.ZInvite.ZTransparency;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMailbox.ContactSortBy;
import com.zimbra.client.ZMailbox.Options;
import com.zimbra.client.ZMailbox.OwnerBy;
import com.zimbra.client.ZMailbox.SharedItemBy;
import com.zimbra.client.ZMailbox.TrustedStatus;
import com.zimbra.client.ZMailbox.ZAppointmentResult;
import com.zimbra.client.ZMailbox.ZImportStatus;
import com.zimbra.client.ZMailbox.ZOutgoingMessage;
import com.zimbra.client.ZMailbox.ZOutgoingMessage.AttachedMessagePart;
import com.zimbra.client.ZMailbox.ZOutgoingMessage.MessagePart;
import com.zimbra.client.ZMessage;
import com.zimbra.client.ZMessage.ZMimePart;
import com.zimbra.client.ZMountpoint;
import com.zimbra.client.ZSearchHit;
import com.zimbra.client.ZSearchParams;
import com.zimbra.client.ZSearchResult;
import com.zimbra.client.ZTag;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.DistributionListBy;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.auth.twofactor.AuthenticatorConfig;
import com.zimbra.common.auth.twofactor.TOTPAuthenticator;
import com.zimbra.common.auth.twofactor.TwoFactorOptions;
import com.zimbra.common.lmtp.LmtpClient;
import com.zimbra.common.localconfig.ConfigException;
import com.zimbra.common.localconfig.KnownKey;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.localconfig.LocalConfig;
import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.Attribute;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.zmime.ZMimeMessage;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.client.LmcSession;
import com.zimbra.cs.client.soap.LmcAdminAuthRequest;
import com.zimbra.cs.client.soap.LmcAdminAuthResponse;
import com.zimbra.cs.client.soap.LmcAuthRequest;
import com.zimbra.cs.client.soap.LmcAuthResponse;
import com.zimbra.cs.client.soap.LmcSoapClientException;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.db.DbUtil;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedContact;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.rmgmt.RemoteCommands;
import com.zimbra.cs.rmgmt.RemoteMailQueue;
import com.zimbra.cs.rmgmt.RemoteMailQueue.QueueAttr;
import com.zimbra.cs.rmgmt.RemoteManager;
import com.zimbra.cs.service.admin.FlushCache;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.store.file.FileBlobStore;
import com.zimbra.cs.util.BuildInfo;
import com.zimbra.cs.util.JMSession;
import com.zimbra.qa.unittest.prov.soap.SoapTest;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.account.message.AuthRequest;
import com.zimbra.soap.account.message.AuthResponse;
import com.zimbra.soap.admin.message.GetAccountRequest;
import com.zimbra.soap.admin.message.GetAccountResponse;
import com.zimbra.soap.admin.message.GrantRightRequest;
import com.zimbra.soap.admin.message.GrantRightResponse;
import com.zimbra.soap.admin.message.QueryWaitSetRequest;
import com.zimbra.soap.admin.message.QueryWaitSetResponse;
import com.zimbra.soap.admin.message.ReloadLocalConfigRequest;
import com.zimbra.soap.admin.message.ReloadLocalConfigResponse;
import com.zimbra.soap.admin.type.Attr;
import com.zimbra.soap.admin.type.EffectiveRightsTargetSelector;
import com.zimbra.soap.admin.type.GranteeSelector;
import com.zimbra.soap.admin.type.GranteeSelector.GranteeBy;
import com.zimbra.soap.admin.type.RightModifierInfo;
import com.zimbra.soap.admin.type.SessionForWaitSet;
import com.zimbra.soap.admin.type.WaitSetInfo;
import com.zimbra.soap.admin.type.WaitSetSessionInfo;
import com.zimbra.soap.type.AccountSelector;
import com.zimbra.soap.type.TargetBy;
import com.zimbra.soap.type.TargetType;

import junit.framework.Assert;


/**
 * @author bburtin
 */
public class TestUtil extends Assert {
    public static int DEFAULT_WAIT = 200;
    public static final String DEFAULT_PASSWORD = "test123";
    public static boolean fromRunUnitTests = false; /* set if run from within RunUnitTestsRequest */
    private static boolean sIsCliInitialized = false;

    public static boolean accountExists(String userName) throws ServiceException {
        return AccountTestUtil.accountExists(userName);
    }

    public static boolean DLExists(String dlName) throws ServiceException {
        String address = getAddress(dlName);
        DistributionList dl = Provisioning.getInstance().get(DistributionListBy.name, address);
        return (dl != null);
    }

    /**
     * @return the <code>Account</code>, or <code>null</code> if account does not exist.
     * @throws ServiceException if name is invalid or can't determine the default domain
     */
    public static Account getAccount(String userName) throws ServiceException {
        return AccountTestUtil.getAccount(userName);
    }

    public static String getDomain() throws ServiceException {
        return AccountTestUtil.getDomain();
    }

    public static Mailbox getMailbox(String userName) throws ServiceException {
        return AccountTestUtil.getMailbox(userName);
    }

    public static String getAddress(String userName) throws ServiceException {
        return AccountTestUtil.getAddress(userName);
    }

    public static String getAddress(String userName, String domainName) {
        return AccountTestUtil.getAddress(userName, domainName);
    }

    public static String getSoapUrl() {
        return getSoapUrl(null);
    }

    public static String getSoapUrl(Server server) {
        try {
            return getBaseUrl(server) + AccountConstants.USER_SERVICE_URI;
        } catch (ServiceException e) {
            ZimbraLog.test.error("Unable to determine SOAP URL", e);
        }
        return null;
    }

    public static String getBaseUrl() throws ServiceException {
        return getBaseUrl(null);
    }

    public static String getBaseUrl(Server server) throws ServiceException {
        String scheme;
        Provisioning prov = Provisioning.getInstance();
        if(server == null) {
            server = prov.getLocalServer();
        }
        String hostname = server.getServiceHostname();
        int port;
        port = server.getIntAttr(Provisioning.A_zimbraMailSSLPort, 0);
        if (port > 0) {
            scheme = "https";
        } else {
            port = server.getIntAttr(Provisioning.A_zimbraMailPort, 0);
            scheme = "http";
        }
        return scheme + "://" + hostname + ":" + port;
    }

    public static String getAdminSoapUrl() {
        int port;
        String hostname = "localhost";
        try {
            port = Provisioning.getInstance().getLocalServer().getIntAttr(Provisioning.A_zimbraAdminPort, 0);
            hostname = Provisioning.getInstance().getLocalServer().getServiceHostname();
        } catch (ServiceException e) {
            ZimbraLog.test.error("Unable to get admin SOAP port", e);
            port = LC.zimbra_admin_service_port.intValue();
        }
        return "https://" + hostname + ":" + port + AdminConstants.ADMIN_SERVICE_URI;
    }

    public static LmcSession getSoapSession(String userName) throws ServiceException, LmcSoapClientException,
            IOException, SoapFaultException, HttpException {
        LmcAuthRequest auth = new LmcAuthRequest();
        auth.setUsername(getAddress(userName));
        auth.setPassword(DEFAULT_PASSWORD);
        LmcAuthResponse authResp = (LmcAuthResponse) auth.invoke(getSoapUrl());
        return authResp.getSession();
    }

    public static LmcSession getAdminSoapSession() throws Exception {
        // Authenticate
        LmcAdminAuthRequest auth = new LmcAdminAuthRequest();
        auth.setUsername(LC.zimbra_ldap_user.value());
        auth.setPassword(LC.zimbra_ldap_password.value());
        LmcAdminAuthResponse authResp = (LmcAdminAuthResponse) auth.invoke(getAdminSoapUrl());
        return authResp.getSession();
    }

    public static Message addMessage(Mailbox mbox, String subject) throws Exception {
        return addMessage(mbox, Mailbox.ID_FOLDER_INBOX, subject);
    }

    public static Message addMessage(Mailbox mbox, int folderId, String subject) throws Exception {
        return addMessage(mbox, folderId, subject, System.currentTimeMillis());
    }

    public static Message addMessage(Mailbox mbox, int folderId, String subject, long timestamp) throws Exception {
        String message = getTestMessage(subject, null, null, new Date(timestamp));
        ParsedMessage pm = new ParsedMessage(message.getBytes(), timestamp, false);
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(folderId).setFlags(Flag.BITMASK_UNREAD);
        return mbox.addMessage(null, pm, dopt, null);
    }

    public static Message addMessage(Mailbox mbox, ParsedMessage pm) throws Exception {
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX).setFlags(Flag.BITMASK_UNREAD);
        return mbox.addMessage(null, pm, dopt, null);
    }

    public static Message addMessage(Mailbox mbox, String recipient, String sender, String subject, String body,
            long timestamp) throws Exception {
        String message = getTestMessage(subject, recipient, sender, body, new Date(timestamp));
        ParsedMessage pm = new ParsedMessage(message.getBytes(), timestamp, false);
        return addMessage(mbox, pm);
    }

    public static String getTestMessage(String subject) throws ServiceException, MessagingException, IOException {
        return new MessageBuilder().withSubject(subject).create();
    }

    public static String getTestMessage(String subject, String recipient, String sender, Date date)
            throws ServiceException, MessagingException, IOException {
        return new MessageBuilder().withSubject(subject).withToRecipient(recipient).withFrom(sender).withDate(date)
                .create();
    }

    public static String getTestMessage(String subject, String recipient, String sender, String body, Date date)
            throws ServiceException, MessagingException, IOException {
        return new MessageBuilder().withSubject(subject).withToRecipient(recipient).withFrom(sender).withDate(date)
                .withBody(body).create();
    }

    public static Contact createContact(Mailbox mbox, int folderId, String emailAddr) throws ServiceException {
        return mbox.createContact(null,
                new ParsedContact(Collections.singletonMap(ContactConstants.A_email, emailAddr)), folderId, null);
    }

    public static Contact createContactInDefaultFolder(Mailbox mbox, String emailAddr) throws ServiceException {
        return createContact(mbox, Mailbox.ID_FOLDER_CONTACTS, emailAddr);
    }

    protected static String addDomainIfNecessary(String user) throws ServiceException {
        if (StringUtil.isNullOrEmpty(user) || user.contains("@")) {
            return user;
        }
        return String.format("%s@%s", user, getDomain());
    }

    public static boolean addMessageLmtp(String subject, String recipient, String sender) throws Exception {
        return addMessageLmtp(subject, new String[] { recipient }, sender);
    }

    public static boolean addMessageLmtp(String subject, String[] recipients, String sender) throws Exception {
        String message = getTestMessage(subject, recipients[0], sender, null);
        return addMessageLmtp(recipients, sender, message);
    }

    public static boolean addMessageLmtp(String subject, String[] recipients, String sender, String body)
            throws Exception {
        String message = getTestMessage(subject, recipients[0], sender, body, null);
        return addMessageLmtp(recipients, sender, message);
    }

    public static boolean addMessageLmtp(String[] recipients, String sender, String message) throws Exception {
        String[] recipWithDomain = new String[recipients.length];
        for (int i = 0; i < recipients.length; i++) {
            recipWithDomain[i] = addDomainIfNecessary(recipients[i]);
        }
        Provisioning prov = Provisioning.getInstance();
        LmtpClient lmtp = new LmtpClient("localhost", prov.getLocalServer().getIntAttr(
                Provisioning.A_zimbraLmtpBindPort, 7025));
        byte[] data = message.getBytes();
        String senderAddress = "";
        if (!StringUtil.isNullOrEmpty(sender)) {
            senderAddress = addDomainIfNecessary(sender);
        }
        boolean success = lmtp.sendMessage(new ByteArrayInputStream(data), recipWithDomain, senderAddress, "TestUtil",
                (long) data.length);
        lmtp.close();
        return success;
    }

    public static String addMessage(ZMailbox mbox, String subject) throws ServiceException, IOException,
            MessagingException {
        return addMessage(mbox, subject, Integer.toString(Mailbox.ID_FOLDER_INBOX));
    }

    public static String addMessage(ZMailbox mbox, String subject, String folderId) throws ServiceException,
            IOException, MessagingException {
        String message = getTestMessage(subject);
        return mbox.addMessage(folderId, null, null, 0, message, true);
    }

    public static String addMessage(ZMailbox mbox, String subject, String folderId, String flags)
            throws ServiceException, IOException, MessagingException {
        String message = getTestMessage(subject);
        return mbox.addMessage(folderId, flags, null, 0, message, true);
    }

    public static String addRawMessage(ZMailbox mbox, String rawMessage) throws ServiceException {
        return mbox.addMessage(Integer.toString(Mailbox.ID_FOLDER_INBOX), null, null, 0, rawMessage, true);
    }

    public static void sendMessage(ZMailbox senderMbox, String recipientName, String subject) throws Exception {
        String body = getTestMessage(subject);
        sendMessage(senderMbox, recipientName, subject, body);
    }

    public static void sendMessage(ZMailbox senderMbox, String recipientName, String subject, String body)
            throws Exception {
        sendMessage(senderMbox, recipientName, subject, body, null);
    }

    public static void sendMessage(ZMailbox senderMbox, String recipientName, String subject, String body,
            String attachmentUploadId) throws Exception {
        ZOutgoingMessage msg = getOutgoingMessage(recipientName, subject, body, attachmentUploadId);
        senderMbox.sendMessage(msg, null, false);
    }

    public static void saveDraftAndSendMessage(ZMailbox senderMbox, String recipient, String subject, String body,
            String attachmentUploadId) throws ServiceException {
        ZOutgoingMessage outgoingDraft = getOutgoingMessage(recipient, subject, body, attachmentUploadId);
        ZMessage draft = senderMbox.saveDraft(outgoingDraft, null, Integer.toString(Mailbox.ID_FOLDER_DRAFTS));

        ZOutgoingMessage outgoing = getOutgoingMessage(recipient, subject, body, null);
        if (attachmentUploadId != null) {
            AttachedMessagePart part = new AttachedMessagePart(draft.getId(), "2", null);
            outgoing.setMessagePartsToAttach(Arrays.asList(part));
        }
        senderMbox.sendMessage(outgoing, null, false);
    }

    public static ZOutgoingMessage getOutgoingMessage(String recipient, String subject, String body,
            String attachmentUploadId) throws ServiceException {
        ZOutgoingMessage msg = new ZOutgoingMessage();
        List<ZEmailAddress> addresses = new ArrayList<ZEmailAddress>();
        addresses.add(new ZEmailAddress(addDomainIfNecessary(recipient), null, null, ZEmailAddress.EMAIL_TYPE_TO));
        msg.setAddresses(addresses);
        msg.setSubject(subject);
        msg.setMessagePart(new MessagePart("text/plain", body));
        msg.setAttachmentUploadId(attachmentUploadId);
        return msg;
    }

    /**
     * Searches a mailbox and returns the id's of all matching items.
     */
    public static List<Integer> search(Mailbox mbox, String query, MailItem.Type type) throws ServiceException {
        return search(mbox, query, Collections.singleton(type));
    }

    /**
     * Searches a mailbox and returns the id's of all matching items.
     */
    public static List<Integer> search(Mailbox mbox, String query, Set<MailItem.Type> types) throws ServiceException {
        List<Integer> ids = new ArrayList<Integer>();
        try (ZimbraQueryResults r = mbox.index.search(new OperationContext(mbox), query, types,
            SortBy.DATE_DESC, 100)) {
            while (r.hasNext()) {
                ZimbraHit hit = r.getNext();
                ids.add(new Integer(hit.getItemId()));
            }
        } catch (IOException e) {
        }
        return ids;
    }

    public static List<ZimbraHit> searchForHits(Mailbox mbox, String query, MailItem.Type type) throws Exception {
        return searchForHits(mbox, query, Collections.singleton(type));
    }

    public static List<ZimbraHit> searchForHits(Mailbox mbox, String query, Set<MailItem.Type> types) throws Exception {
        List<ZimbraHit> hits = Lists.newArrayList();
        try (ZimbraQueryResults r = mbox.index.search(new OperationContext(mbox), query, types,
            SortBy.DATE_DESC, 100)) {
            while (r.hasNext()) {
                hits.add(r.getNext());
            }
        }
        return hits;
    }

    public static List<String> search(ZMailbox mbox, String query, String type) throws ServiceException {
        List<String> ids = new ArrayList<String>();
        ZSearchParams params = new ZSearchParams(query);
        params.setTypes(type);
        for (ZSearchHit hit : mbox.search(params).getHits()) {
            ids.add(hit.getId());
        }
        return ids;
    }

    public static List<ZMessage> search(ZMailbox mbox, String query) throws ServiceException {
        ZSearchParams params = new ZSearchParams(query);
        params.setTypes(ZSearchParams.TYPE_MESSAGE);

        return search(mbox, params);
    }

    public static List<ZMessage> search(ZMailbox mbox, ZSearchParams params) throws ServiceException {
        List<ZMessage> msgs = new ArrayList<ZMessage>();
        for (ZSearchHit hit : mbox.search(params).getHits()) {
            ZGetMessageParams msgParams = new ZGetMessageParams();
            msgParams.setId(hit.getId());
            msgs.add(mbox.getMessage(msgParams));
        }
        return msgs;
    }

    public static List<String> searchMessageIds(ZMailbox mbox, ZSearchParams params) throws ServiceException {
        List<String> msgsIds = new ArrayList<String>();
        ZSearchResult results = mbox.search(params);
        for (ZSearchHit hit : results.getHits()) {
            msgsIds.add(hit.getId());
        }
        return msgsIds;
    }

    /**
     * Gets the raw content of a message.
     */
    public static String getContent(ZMailbox mbox, String msgId) throws ServiceException {
        ZGetMessageParams msgParams = new ZGetMessageParams();
        msgParams.setId(msgId);
        msgParams.setRawContent(true);
        ZMessage msg = mbox.getMessage(msgParams);
        return msg.getContent();
    }

    public static byte[] getContent(ZMailbox mbox, String msgId, String name) throws ServiceException, IOException {
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
        if (StringUtil.equalIgnoreCase(mimeStructure.getName(), name)
                || StringUtil.equalIgnoreCase(mimeStructure.getFileName(), name)
                || StringUtil.equalIgnoreCase(mimeStructure.getPartName(), name)) {
            return mimeStructure;
        }
        return null;
    }

    /**
     * @return true if likely to have had things delayed in the deferred queue
     */
    private static boolean postfixFlushDeferredMailQueue(Server server, long waitMillis) {
        long start = System.currentTimeMillis();
        try {
            if (null == server) {
                server = Provisioning.getInstance().getLocalServer();
            }
            RemoteMailQueue rmq = RemoteMailQueue.getRemoteMailQueue(server, "deferred", true /* scan */);
            boolean stillScanning = rmq.waitForScan(waitMillis);
            if (stillScanning) {
                ZimbraLog.test.info("postfixFlushDeferredMailQueue - taking too long to scan queue.");
                return true;
            }
            RemoteMailQueue.SearchResult sr = rmq.search(null, 0, Integer.MAX_VALUE);
            if (sr.qitems.size() == 0) {
                ZimbraLog.test.info("postfixFlushDeferredMailQueue - deferred queue was empty.");
                return false;
            }
            String[] ids = new String[sr.qitems.size()];
            int i = 0;
            StringBuilder qinfo = new StringBuilder();
            for (Map<QueueAttr,String> qitem : sr.qitems) {
                qinfo.append("\nDEFERRED_POSTFIX_QUEUE_ENTRY:");
                ids[i++] = qitem.get(QueueAttr.id);
                for (QueueAttr qattr : QueueAttr.values()) {
                    String val = qitem.get(qattr);
                    if (!Strings.isNullOrEmpty(val)) {
                        qinfo.append(" ").append(qattr.name()).append("=").append(val);
                    }
                }
            }
            ZimbraLog.test.info("postfixFlushDeferredMailQueue - deferred queue:%s", qinfo);
            rmq.action(server, RemoteMailQueue.QueueAction.requeue, ids);
            ZimbraLog.test.info("postfixFlushDeferredMailQueue done requeue");
            // requeueing isn't enough on its own to get things moving
            RemoteManager rmgr = RemoteManager.getRemoteManager(server);
            rmgr.execute(RemoteCommands.FLUSHQUEUE);
            ZimbraLog.test.info("postfixFlushDeferredMailQueue finished [%s]",
                                ZimbraLog.elapsedTime(start, System.currentTimeMillis()));
        } catch (ServiceException e) {
            ZimbraLog.test.error("postfixFlushDeferredMailQueue - Problem getting 'deferred' mail queue", e);
        }
        return true;
    }

    private static List<ZMessage> helpWait4Msgs(ZMailbox mbox, String query, int numMsgsExpected, long timeout_millis)
    throws ServiceException
    {
        long start = System.currentTimeMillis();
        List<ZMessage> msgs = Lists.newArrayListWithExpectedSize(numMsgsExpected);
        while (timeout_millis > 0) {
            msgs = search(mbox, query);
            if ((numMsgsExpected > 0) && (msgs.size() == numMsgsExpected)) {
                ZimbraLog.test.debug("helpWait4Msgs succeeded - mbox='%s' query='%s' %s", mbox.getName(), query,
                        ZimbraLog.elapsedTime(start, System.currentTimeMillis()));
                return msgs;
            }
            if (msgs.size() > numMsgsExpected) {
                Assert.fail("Unexpected number of messages (" + msgs.size() + ") returned by query '" + query + "'");
            }
            try {
                if (timeout_millis > DEFAULT_WAIT) {
                    Thread.sleep(DEFAULT_WAIT);
                    timeout_millis = timeout_millis - DEFAULT_WAIT;
                } else {
                    Thread.sleep(timeout_millis);
                    timeout_millis = 0;

                }
            } catch (InterruptedException e) {
                ZimbraLog.test.debug("sleep got interrupted", e);
            }
        }
        ZimbraLog.test.debug("helpWait4Msgs finished for mbox='%s' query='%s' %s", mbox.getName(), query,
                ZimbraLog.elapsedTime(start, System.currentTimeMillis()));
        return msgs;
    }

    /**
     * @param numMsgsExpected
     *            - 0 means don't expect a message to arrive before timeout_millis
     * @param timeout_millis
     * @throws ServiceException
     */
    public static List<ZMessage> waitForMessages(ZMailbox mbox, String query, int numMsgsExpected, int timeout_millis)
            throws ServiceException {
        long start = System.currentTimeMillis();
        long finishBy = start + timeout_millis;
        List<ZMessage> msgs;
        // Just wait for a third of the time initially.  If hasn't arrived in that time, poke postfix to process
        // its queue and wait up to the rest of the time
        msgs = helpWait4Msgs(mbox, query, numMsgsExpected, timeout_millis / 3);
        if ((numMsgsExpected > 0) && (msgs.size() == numMsgsExpected)) {
            return msgs;
        }
        if (numMsgsExpected > 0) {
            /* One last try.  See if got stuck in Postfix mailq - seen mailq entries like this before now:
             * E980B183AF8     2622 Thu Mar 30 14:49:54  user1@example.com
             * (delivery temporarily suspended: connect to example.com[XXX.XXX.XXX.XXX]:7025: Connection refused)
             *                           user1@example.com
             * and:
             * DD410183B37     2619 Thu Mar 30 15:59:31  user1@example.com
             * (host example.com[XXX.XXX.XXX.XXX] said:
             *     451 4.0.0 Temporary message delivery failure try again (in reply to end of DATA command))
             *                           user1@example.com
             */
            if (postfixFlushDeferredMailQueue(AccountTestUtil.getServer(mbox.getName()),
                    finishBy - System.currentTimeMillis())) {
                ZimbraLog.test.debug("waitForMessages allowing another 5 seconds because of deferrals");
                finishBy += 5000;  // allow a bit more time if we spotted things getting deferred
            }
            msgs = helpWait4Msgs(mbox, query, numMsgsExpected, finishBy - System.currentTimeMillis());
            if ((numMsgsExpected > 0) && (msgs.size() == numMsgsExpected)) {
                return msgs;
            }
        }
        if (numMsgsExpected > 0) {
            Assert.fail(String.format("Message%s for query '%s' didn't arrive within %d millisecs.  %s",
                    (numMsgsExpected == 1) ? "" : "s", query, System.currentTimeMillis() - start,
                    "Either the MTA is not running or the test failed."));
        }
        return msgs;
    }

    public static ZMessage waitForMessage(ZMailbox mbox, String query) throws Exception {
        // Used to wait up to 10 secs but due to the way postfix sometimes works, can get longer delays
        // so increased the max wait time.
        List<ZMessage> msgs = waitForMessages(mbox, query, 1, 31000);
        return msgs.get(0);
    }

    public static QueryWaitSetResponse waitForSessions(int numExpectedSessions, int numExpectedFolderInterests, int timeout_millis, String wsID, Server server) throws Exception {
        QueryWaitSetResponse resp = null;
        while (timeout_millis > 0) {
            QueryWaitSetRequest req = new QueryWaitSetRequest(wsID);
            SoapTransport transport = getAdminSoapTransport(server);
            resp = JaxbUtil.elementToJaxb(transport.invoke(JaxbUtil.jaxbToElement(req)));
            List<WaitSetInfo> wsInfoList = resp.getWaitsets();
            assertFalse("Expecting to find a waitset", wsInfoList.isEmpty());
            assertEquals("Expecting to find only one waitset", 1, wsInfoList.size());
            WaitSetInfo wsInfo = wsInfoList.get(0);
            assertEquals("Found wrong waitset", wsID, wsInfo.getWaitSetId());
            List<SessionForWaitSet> sessions = wsInfo.getSessions();
            if(sessions != null && numExpectedSessions > 0) {
                if(sessions.size() == numExpectedSessions) {
                    int foundFolderInterests = 0;
                    for(SessionForWaitSet s : sessions) {
                        WaitSetSessionInfo sessionInfo = s.getWaitSetSession();
                        if(sessionInfo != null) {
                            foundFolderInterests +=s.getWaitSetSession().getFolderInterestsAsSet().size();
                        }
                    }
                    if(foundFolderInterests == numExpectedFolderInterests) {
                        return resp;
                    }
                }
            } else if((sessions == null || sessions.isEmpty()) && numExpectedSessions == 0) {
                return resp;
            }
            try {
                if (timeout_millis > 500) {
                    Thread.sleep(500);
                    timeout_millis = timeout_millis - 500;
                } else {
                    Thread.sleep(timeout_millis);
                    timeout_millis = 0;
                }
            } catch (InterruptedException e) {
                ZimbraLog.test.debug("sleep got interrupted", e);
            }
        }
        return resp;
    }
    /**
     * Returns a folder with the given path, or <code>null</code> if the folder doesn't exist.
     */
    public static Folder getFolderByPath(Mailbox mbox, String path) throws Exception {
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
     * Delete all messages, tags and folders in the user's mailbox whose subjects contain the given substring. For
     * messages, the subject must contain subjectString as a separate word. Tags and folders can have the string
     * anywhere in the name.
     */
    public static void deleteTestData(String userName, String subjectSubstring) throws ServiceException {
        ZMailbox mbox = getZMailbox(userName);

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
            if (ds.getName() != null && ds.getName().contains(subjectSubstring)) {
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

        ZMailbox adminMbox = getZMailboxAsAdmin(userName);
        adminMbox.emptyDumpster();
    }

    protected static void deleteMessages(ZMailbox mbox, String query) throws ServiceException {
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
     * Sets up the environment for command-line unit tests.
     */
    public static void cliSetup() throws ServiceException {
        if (!sIsCliInitialized) {
            if (TestUtil.fromRunUnitTests) {
                // Don't want to re-initialise log4j etc as results in redirecting away from mailbox.log
                CliUtil.toolSetup();
                Provisioning.setInstance(newSoapProvisioning());
            }
            SoapTransport.setDefaultUserAgent("Zimbra Unit Tests", BuildInfo.VERSION);
            sIsCliInitialized = true;
        }
    }

    public static SoapProvisioning newSoapProvisioning() throws ServiceException {
        SoapProvisioning sp = new SoapProvisioning();
        sp.soapSetURI("https://localhost:7071" + AdminConstants.ADMIN_SERVICE_URI);
        sp.soapZimbraAdminAuthenticate();
        return sp;
    }

    public static SoapProvisioning newDelegatedSoapProvisioning(String login, String password) throws ServiceException {
        SoapProvisioning sp = new SoapProvisioning();
        sp.soapSetURI("https://localhost:7071" + AdminConstants.ADMIN_SERVICE_URI);
        sp.soapAdminAuthenticate(login, password);
        return sp;
    }

    public static void runTest(Class<?> testClass) {
        JUnitCore junit = new JUnitCore();
        junit.addListener(new TestLogger());
        ZimbraLog.test.info("Starting unit test %s.", testClass.getName());
        junit.run(testClass);
    }

    public static ZMailbox getZMailbox(String username) throws ServiceException {
        return getZMailbox(username, null);
    }

    public static ZMailbox getZMailbox(String username, String password, String twoFactorCode, TrustedStatus trusted)
            throws ServiceException {
        ZMailbox.Options options = new ZMailbox.Options();
        options.setAccount(getAddress(username));
        options.setAccountBy(Key.AccountBy.name);
        options.setPassword(password);
        options.setUri(TestUtil.getSoapUrl(TestUtil.getAccount(username).getServer()));
        if (twoFactorCode != null) {
            options.setTwoFactorCode(twoFactorCode);
        }
        if (trusted == TrustedStatus.trusted) {
            options.setTrustedDevice(true);
        }
        return ZMailbox.getMailbox(options);
    }

    public static ZMailbox getZMailbox(String username, String twoFactorCode, TrustedStatus trusted)
            throws ServiceException {
        return getZMailbox(username, DEFAULT_PASSWORD, twoFactorCode, trusted);
    }

    public static ZMailbox getZMailbox(String username, String scratchCode) throws ServiceException {
        return getZMailbox(username, scratchCode, TrustedStatus.not_trusted);
    }

    public static ZMailbox getZMailboxAsAdmin(String username) throws ServiceException {
        ZAuthToken adminAuthToken = newSoapProvisioning().getAuthToken();
        ZMailbox.Options options = new ZMailbox.Options(adminAuthToken, getSoapUrl());
        options.setTargetAccount(getAddress(username));
        options.setTargetAccountBy(AccountBy.name);
        return ZMailbox.getMailbox(options);
    }

    /**
     * Creates an account for the given username, with password set to {@link #DEFAULT_PASSWORD}.
     */
    public static Account createAccount(String username) throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraMailHost, Provisioning.getInstance().getLocalServer().getServiceHostname());
        return createAccount(username, attrs);
    }

    /** Creates an account for the given username, and password. */
    public static Account createAccount(String username, String password, Map<String, Object> attrs)
    throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        String address = getAddress(username);
        return prov.createAccount(address, password, attrs);
    }

    /**
     * Creates an account for the given username, with password set to {@link #DEFAULT_PASSWORD}.
     */
    public static Account createAccount(String username, Map<String, Object> attrs) throws ServiceException {
        return createAccount(username, DEFAULT_PASSWORD, attrs);
    }

    /**
     * Creates a DL with a given address
     */
    public static DistributionList createDistributionList(String dlName) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        String address = getAddress(dlName);
        Map<String, Object> attrs = new HashMap<String, Object>();
        return prov.createDistributionList(address, attrs);
    }

    /**
     * Deletes the specified DL.
     */
    public static void deleteDistributionList(String listName) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();

        // If this code is running on the server, call SoapProvisioning explicitly
        // so that both the account and mailbox are deleted.
        if (!(prov instanceof SoapProvisioning)) {
            prov = newSoapProvisioning();
        }
        DistributionList dl = prov.get(DistributionListBy.name, getAddress(listName));
        if (dl != null) {
            prov.deleteDistributionList(dl.getId());
        }
    }

    /**
     * Deletes the account for the given username. Consider using {@link deleteAccountIfExists} as alternative
     * to reduce logging where the account may not exist.
     */
    public static void deleteAccount(String username) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();

        // If this code is running on the server, call SoapProvisioning explicitly
        // so that both the account and mailbox are deleted.
        if (!(prov instanceof SoapProvisioning)) {
            prov = newSoapProvisioning();
        }
        SoapProvisioning soapProv = (SoapProvisioning) prov;
        GetAccountRequest gaReq = new GetAccountRequest(AccountSelector.fromName(username), false,
                Lists.newArrayList(Provisioning.A_zimbraId));
        try {
            GetAccountResponse resp = soapProv.invokeJaxb(gaReq);
            if (resp != null) {
                String id = null;
                for (Attr attr : resp.getAccount().getAttrList()) {
                    if (Provisioning.A_zimbraId.equals(attr.getKey())) {
                        id = attr.getValue();
                        break;
                    }
                }
                if (null == id) {
                    ZimbraLog.test.error("GetAccountResponse for '%s' did not contain the zimbraId", username);
                    return;
                }
                prov.deleteAccount(id);
            }
        } catch (SoapFaultException sfe) {
            if (!sfe.getMessage().contains("no such account")) {
                ZimbraLog.test.error("GetAccountResponse for '%s' hit unexpected problem", username, sfe);
            }
        }
    }

    /**
     * Less chatty than deleteAccount if the account doesn't already exist.  Useful for cleanUp()
     * methods which are called before running a test to delete any accounts left over from previous
     * failed runs without spouting lots of logging to mailbox.log
     */
    public static void deleteAccountIfExists(String username) throws ServiceException {
        if (TestUtil.accountExists(username)) {
            deleteAccount(username);
        }
    }

    public static String getServerAttr(String attrName) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Server server = prov.getLocalServer();
        return server.getAttr(attrName, null);
    }

    public static void setServerAttr(String attrName, String attrValue) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Server server = prov.getLocalServer();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(attrName, attrValue);
        prov.modifyAttrs(server, attrs);
    }

    public static String getAccountAttr(String userName, String attrName) throws ServiceException {
        String accountName = getAddress(userName);
        Account account = Provisioning.getInstance().getAccount(accountName);
        return account.getAttr(attrName);
    }

    public static String[] getAccountMultiAttr(String userName, String attrName) throws ServiceException {
        String accountName = getAddress(userName);
        Account account = Provisioning.getInstance().getAccount(accountName);
        return account.getMultiAttr(attrName);
    }

    public static void setAccountAttr(String userName, String attrName, String attrValue) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(AccountBy.name, getAddress(userName));
        if (null == account) {
            throw ServiceException.FAILURE(
                    String.format("Trying to setAccountAttr(%s,%s,%s) on non-existent account %s",
                            userName, attrName, attrValue, userName), null);
        }
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(attrName, attrValue);
        prov.modifyAttrs(account, attrs);
    }

    public static void setAccountAttr(String userName, String attrName, String[] attrValues) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(AccountBy.name, getAddress(userName));
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(attrName, attrValues);
        prov.modifyAttrs(account, attrs);
    }

    public static String getConfigAttr(String attrName) throws ServiceException {
        return Provisioning.getInstance().getConfig().getAttr(attrName, "");
    }

    public static void setConfigAttr(String attrName, String attrValue) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Config config = prov.getConfig();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(attrName, attrValue);
        prov.modifyAttrs(config, attrs);
    }

    public static String getDomainAttr(String userName, String attrName) throws ServiceException {
        Account account = getAccount(userName);
        return Provisioning.getInstance().getDomain(account).getAttr(attrName);
    }

    public static void setDomainAttr(String userName, String attrName, Object attrValue) throws ServiceException {
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
        DataSource ds = prov.get(account, Key.DataSourceBy.name, dataSourceName);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(attrName, attrValue);
        prov.modifyAttrs(ds, attrs);
    }

    /**
     * Verifies that a message is tagged.
     */
    public static void verifyTag(ZMailbox mbox, ZMessage msg, String tagName) throws Exception {
        List<ZTag> tags = mbox.getTags(msg.getTagIds());
        for (ZTag tag : tags) {
            if (tag.getName().equals(tagName)) {
                return;
            }
        }
        Assert.fail("Message not tagged with " + tagName);
    }

    public static ZMessage getMessage(ZMailbox mbox, String query) throws Exception {
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

    public static ZFolder createFolder(ZMailbox mbox, String path) throws ServiceException {
        return createFolder(mbox, path, ZFolder.View.message);
    }

    public static ZFolder createFolder(ZMailbox mbox, String path, ZFolder.View view) throws ServiceException {
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

        return mbox.createFolder(parentId, name, view, null, null, null);
    }

    public static ZFolder createFolder(ZMailbox mbox, String parentId, String folderName) throws ServiceException {
        return mbox.createFolder(parentId, folderName, ZFolder.View.message, null, null, null);
    }

    /**
     * Creates a mountpoint between two mailboxes. The mountpoint gives the "to" user full rights on the folder.
     *
     * @param remoteMbox
     *            remote mailbox
     * @param remotePath
     *            remote folder path. Folder is created if it doesn't exist.
     * @param localMbox
     *            local mailbox
     * @param mountpointName
     *            the name of the mountpoint folder. The folder is created directly under the user root.
     */
    public static ZMountpoint createMountpoint(ZMailbox remoteMbox, String remotePath, ZMailbox localMbox,
            String mountpointName) throws ServiceException {
        ZFolder remoteFolder = remoteMbox.getFolderByPath(remotePath);
        if (remoteFolder == null) {
            remoteFolder = createFolder(remoteMbox, remotePath);
        }
        ZGetInfoResult remoteInfo = remoteMbox.getAccountInfo(true);
        remoteMbox.modifyFolderGrant(remoteFolder.getId(), GranteeType.all, null, "rwidx", null);
        return localMbox.createMountpoint(Integer.toString(Mailbox.ID_FOLDER_USER_ROOT), mountpointName, null, null,
                null, OwnerBy.BY_ID, remoteInfo.getId(), SharedItemBy.BY_ID, remoteFolder.getId(), false);
    }

    /**
     * Returns the data source with the given name.
     */
    public static ZDataSource getDataSource(ZMailbox mbox, String name) throws ServiceException {
        for (ZDataSource ds : mbox.getAllDataSources()) {
            if (ds.getName() != null && ds.getName().equals(name)) {
                return ds;
            }
        }
        return null;
    }

    /**
     * Imports data from the given data source and updates state on both the local and remote mailboxes.
     */
    public static void importDataSource(ZDataSource dataSource, ZMailbox localMbox, ZMailbox remoteMbox)
            throws Exception {
        importDataSource(dataSource, localMbox, remoteMbox, true);
    }

    /**
     * Imports data from the given data source and updates state on both the local and remote mailboxes.
     */
    public static void importDataSource(ZDataSource dataSource, ZMailbox localMbox, ZMailbox remoteMbox,
            boolean expectedSuccess) throws Exception {
        List<ZDataSource> dataSources = new ArrayList<ZDataSource>();
        dataSources.add(dataSource);
        localMbox.importData(dataSources);
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
        assertEquals("importDataSource status success value", expectedSuccess, status.getSuccess());
        if (!expectedSuccess) {
            assertNotNull("importDataSource status error value", status.getError());
        }

        // Get any state changes from the server
        localMbox.noOp();
        if (remoteMbox != null) {
            remoteMbox.noOp();
        }
    }

    private static SoapTransport getAdminSoapTransport(SoapHttpTransport transport,
            String adminName, String adminPassword)
                    throws SoapFaultException, IOException, ServiceException, HttpException {
        // Create auth element
        Element auth = new XMLElement(AdminConstants.AUTH_REQUEST);
        auth.addNonUniqueElement(AdminConstants.E_NAME).setText(adminName);
        auth.addNonUniqueElement(AdminConstants.E_PASSWORD).setText(adminPassword);

        // Authenticate and get auth token
        Element response =  transport.invoke(auth, false /* raw */, true /* noSession value */,
                null /* requestedAccountId */);
        String authToken = response.getElement(AccountConstants.E_AUTH_TOKEN).getText();
        transport.setAuthToken(authToken);
        transport.setAdmin(true);
        return transport;
    }

    /** Returns an authenticated transport for the <tt>zimbra</tt> account. 
     * @throws HttpException */
    public static SoapTransport getAdminSoapTransport()
            throws SoapFaultException, IOException, ServiceException, HttpException {
        return getAdminSoapTransport(new SoapHttpTransport(getAdminSoapUrl()),
                LC.zimbra_ldap_user.value(), LC.zimbra_ldap_password.value());
    }

    /** Returns an authenticated transport for the <tt>zimbra</tt> account on the target server. */
    public static SoapTransport getAdminSoapTransport(Server targetServer)
            throws SoapFaultException, IOException, ServiceException, HttpException {
        return getAdminSoapTransport(new SoapHttpTransport(URLUtil.getAdminURL(targetServer)),
                LC.zimbra_ldap_user.value(), LC.zimbra_ldap_password.value());
    }

    /**
     * Returns an authenticated transport for the <tt>adminName</tt> account.
     */
    public static SoapTransport getAdminSoapTransport(String adminName, String adminPassword)
            throws SoapFaultException, IOException, ServiceException, HttpException {
        return getAdminSoapTransport(new SoapHttpTransport(getAdminSoapUrl()), adminName, adminPassword);
    }

    /**
     * Assert the message contains the given sub-message, ignoring newlines. Used for comparing equality of two
     * messages, when one had <tt>Return-Path</tt> or other headers prepended.
     */
    public static void assertMessageContains(String message, String subMessage) throws IOException {
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

        String context = String.format("Could not find '%s' in message:\n%s", firstLine, message);
        assertTrue(context, foundFirstLine);

        while (true) {
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
        String context = String.format("Element %s, expected:\n%s\nactual:\n%s", expected.getName(), expectedDump,
                actualDump);
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
            return; // shuts up warnings in Eclipse
        }
        if (actual == null) {
            Assert.fail("expected was not null but actual was.");
            return; // shuts up warnings in Eclipse
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

    public static String getHeaderValue(ZMailbox mbox, ZMessage msg, String headerName) throws Exception {
        String content = msg.getContent();
        if (content == null) {
            content = getContent(mbox, msg.getId());
        }
        assertNotNull("Content was not fetched from the server", content);
        MimeMessage mimeMsg = new ZMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(
                content.getBytes()));
        return mimeMsg.getHeader(headerName, null);
    }

    public static ZAppointmentResult createAppointment(ZMailbox mailbox, String subject, String attendee,
            Date startDate, Date endDate) throws ServiceException {
        ZInvite invite = new ZInvite();
        ZInvite.ZComponent comp = new ZComponent();

        comp.setStatus(ZStatus.CONF);
        comp.setClassProp(ZClass.PUB);
        comp.setTransparency(ZTransparency.O);

        comp.setStart(new ZDateTime(startDate.getTime(), false, mailbox.getPrefs().getTimeZone()));
        comp.setEnd(new ZDateTime(endDate.getTime(), false, mailbox.getPrefs().getTimeZone()));
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

    public static void sendInviteReply(ZMailbox mbox, String inviteId, String organizer, String subject,
            ZMailbox.ReplyVerb replyVerb) throws ServiceException {
        organizer = addDomainIfNecessary(organizer);
        ZOutgoingMessage msg = getOutgoingMessage(organizer, subject, "Reply to appointment " + inviteId, null);
        mbox.sendInviteReply(inviteId, "0", replyVerb, true, null, null, msg);
    }

    public static ZFilterRule getFilterRule(ZMailbox mbox, String ruleName) throws ServiceException {
        for (ZFilterRule rule : mbox.getIncomingFilterRules(true).getRules()) {
            if (rule.getName().equals(ruleName)) {
                return rule;
            }
        }
        return null;
    }

    public static ZDocument createDocument(ZMailbox mbox, String folderId, String name, String contentType,
            byte[] content) throws ServiceException {
        return createDocument(mbox, folderId, name, contentType, content, false);
    }

    public static ZDocument createDocument(ZMailbox mbox, String folderId, String name, String contentType,
            byte[] content, boolean isNote) throws ServiceException {
        String attachId = mbox.uploadAttachment(name, content, contentType, 0);
        String docId = mbox.createDocument(folderId, name, attachId, isNote);
        return mbox.getDocument(docId);
    }

    public static ZIdentity getDefaultIdentity(ZMailbox mbox) throws ServiceException {
        for (ZIdentity ident : mbox.getIdentities()) {
            if (ident.getName().equalsIgnoreCase("DEFAULT")) {
                return ident;
            }
        }
        Assert.fail("Could not find default identity for " + mbox.getName());
        return null;
    }

    public static boolean checkLocalBlobs() {
        // some tests check disk for blob files. these tests only work with FileBlobStore
        return StoreManager.getInstance() instanceof FileBlobStore;
    }

    public static byte[] readInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int i = -1;
        while ((i = is.read()) >= 0) {
            baos.write(i);
        }
        return baos.toByteArray();
    }

    public static boolean bytesEqual(byte[] b1, InputStream is) throws IOException {
        return bytesEqual(b1, readInputStream(is));
    }

    public static boolean bytesEqual(byte[] b1, byte[] b2) {
        if (b1.length != b2.length) {
            return false;
        } else {
            for (int i = 0; i < b1.length; i++) {
                if (b1[i] != b2[i]) {
                    return false;
                }
            }
            return true;
        }
    }

    public static ZAuthResult testAuth(ZMailbox mbox, String account, String password) throws ServiceException {
        return testAuth(mbox, account, password, null);
    }

    public static ZAuthResult testAuth(ZMailbox mbox, String account, String password, String twoFactorCode)
            throws ServiceException {
        Options options = new Options();
        options.setAccount(account);
        options.setPassword(password);
        if (twoFactorCode != null) {
            options.setTwoFactorCode(twoFactorCode);
        }
        return mbox.authByPassword(options, password);
    }

    public static void updateMailItemChangeDateAndFlag(Mailbox mbox, int itemId, long changeDate, int flagValue)
            throws ServiceException {
        DbConnection conn = DbPool.getConnection(mbox);
        try {
            StringBuilder sql = new StringBuilder();
            sql.append("UPDATE ").append(DbMailItem.getMailItemTableName(mbox)).append(" SET change_date = ")
                    .append(changeDate);
            if (flagValue > 0) {
                sql.append(", flags = ").append(flagValue);
            }
            sql.append(" WHERE id = ").append(itemId);
            DbUtil.executeUpdate(conn, sql.toString());
        } finally {
            conn.commit();
            conn.closeQuietly();
        }
    }

    public static TOTPAuthenticator getDefaultAuthenticator() {
        final int WINDOW_SIZE = 30;
        final TwoFactorOptions.HashAlgorithm HASH_ALGORITHM = TwoFactorOptions.HashAlgorithm.SHA1;
        final TwoFactorOptions.CodeLength NUM_CODE_DIGITS = TwoFactorOptions.CodeLength.SIX;
        AuthenticatorConfig config = new AuthenticatorConfig();
        config.setHashAlgorithm(HASH_ALGORITHM);
        config.setNumCodeDigits(NUM_CODE_DIGITS);
        config.setWindowSize(WINDOW_SIZE);
        TOTPAuthenticator auth = new TOTPAuthenticator(config);
        return auth;
    }

    protected static void setLCValue(KnownKey key, String newValue) throws DocumentException, ConfigException,
            IOException, ServiceException {
        LocalConfig lc = new LocalConfig(null);
        if (newValue == null) {
            lc.remove(key.key());
        } else {
            lc.set(key.key(), newValue);
        }
        lc.save();
        SoapProvisioning prov = TestUtil.newSoapProvisioning();
        ReloadLocalConfigRequest req = new ReloadLocalConfigRequest();
        ReloadLocalConfigResponse resp = prov.invokeJaxb(req);
        assertNotNull("ReloadLocalConfigResponse", resp);
    }

    public static SoapTransport authUser(String acctName, String password) throws Exception {
        AccountSelector acct = new AccountSelector(com.zimbra.soap.type.AccountBy.name, acctName);
        SoapHttpTransport transport = new SoapHttpTransport(TestUtil.getSoapUrl());
        AuthRequest req = new AuthRequest(acct, password);
        AuthResponse resp = SoapTest.invokeJaxb(transport, req);
        transport.setAuthToken(resp.getAuthToken());
        return transport;
    }

    public static void grantRightToAdmin(SoapProvisioning adminSoapProv, TargetType targetType, String targetName,
            String granteeName, String rightName) throws ServiceException {
        GranteeSelector grantee = new GranteeSelector(com.zimbra.soap.type.GranteeType.usr, GranteeBy.name, granteeName);
        EffectiveRightsTargetSelector target = null;
        if (targetName == null) {
            target = new EffectiveRightsTargetSelector(targetType, null, null);
        } else {
            target = new EffectiveRightsTargetSelector(targetType, TargetBy.name, targetName);
        }

        RightModifierInfo right = new RightModifierInfo(rightName);
        GrantRightResponse grResp = adminSoapProv.invokeJaxb(new GrantRightRequest(target, grantee, right));
        assertNotNull("GrantRightResponse for " + right.getValue(), grResp);
    }

    public static class UserInfo {
        private final String name;
        private Account acct;
        private UserInfo(String acctName) {
            try {
                acctName = AccountTestUtil.getAddress(acctName);
            } catch (ServiceException e) {
            }
            name = acctName;
            acct = null;
        }

        protected Mailbox getMailbox() throws ServiceException {
            ensureAcctExists();
            return TestUtil.getMailbox(name);
        }

        protected ZMailbox getZMailbox() throws ServiceException {
            ensureAcctExists();
            return TestUtil.getZMailbox(name);
        }

        private void ensureAcctExists() throws ServiceException {
            if (null != acct) {
                return;
            }
            try {
                acct = getAccount(name);
            } catch (Exception se) {
                ZimbraLog.test.debug("ensureAcctExists getAccount exception '%s'", name, se);
            }
            if (null != acct) {
                return;
            }
            acct = create();
        }

        public Account create() throws ServiceException {
            acct = TestUtil.createAccount(name);
            return acct;
        }

        @Override
        public String toString() {
            return name;
        }

        public void cleanup() {
            if (null == acct) {
                return;  // Assumes only user UserInfo for creation/deletion of accounts
            }
            try {
                TestUtil.deleteAccount(name);
            } catch (Exception ex) {
                ZimbraLog.test.info("Exception thrown when deleting account '%s'", name, ex);
            }
            acct = null;
        }

        public static void cleanup(UserInfo[] users) {
            for (UserInfo user : users) {
                user.cleanup();
            }
        }
    }

    /** This a hacky way to get a more useful description when an assumption fails */
    public static void assumeTrue(String missive, Boolean testVal) {
        try {
            org.junit.Assume.assumeTrue(testVal);
        } catch (AssumptionViolatedException ave) {
            throw new AssumptionViolatedException(missive, null);
        }
    }

    public static void flushImapDaemonCache(Server imapd) throws Exception {
        // FIXME: This doesn't work any longer since the code was removed from FlushCache
    }
}
