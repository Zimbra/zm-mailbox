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
package com.zimbra.cs.datasource;

import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;

import com.sun.mail.pop3.POP3Folder;
import com.sun.mail.pop3.POP3Message;
import com.sun.mail.pop3.POP3Store;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.CustomSSLSocketFactory;
import com.zimbra.common.util.DummySSLSocketFactory;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.db.DbPop3Message;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.SharedDeliveryContext;
import com.zimbra.cs.mime.ParsedMessage;


public class Pop3Import extends AbstractMailItemImport {
    private POP3Store store;
    
    // (item id).(blob digest)
    private static final Pattern PAT_ZIMBRA_UIDL = Pattern.compile("(\\d+)\\.([^\\.]+)");
    
    private static final int MAX_MESSAGE_MEMORY_SIZE =
        LC.data_source_max_message_memory_size.intValue();

    private static final long TIMEOUT = LC.javamail_pop3_timeout.longValue() * Constants.MILLIS_PER_SECOND;

    private static final boolean DEBUG =
        Boolean.getBoolean("ZimbraJavamailDebug") || LC.javamail_pop3_debug.booleanValue();

    private static final Session SESSION;
    private static final FetchProfile UID_PROFILE;
    
    static {
        Properties props = new Properties();
        props.setProperty("mail.pop3.connectiontimeout", Long.toString(TIMEOUT));
        props.setProperty("mail.pop3.timeout", Long.toString(TIMEOUT));
        props.setProperty("mail.pop3s.connectiontimeout", Long.toString(TIMEOUT));
        props.setProperty("mail.pop3s.timeout", Long.toString(TIMEOUT));
        props.setProperty("mail.pop3s.socketFactory.class",
            LC.data_source_trust_self_signed_certs.booleanValue() ?
                DummySSLSocketFactory.class.getName() : CustomSSLSocketFactory.class.getName());
        props.setProperty("mail.pop3s.socketFactory.fallback", "false");
        if (LC.javamail_pop3_enable_starttls.booleanValue()) {
        	props.setProperty("mail.pop3.starttls.enable", "true");
        }
        props.setProperty("mail.pop3.max.message.memory.size",
                          String.valueOf(MAX_MESSAGE_MEMORY_SIZE));
        props.setProperty("mail.pop3s.max.message.memory.size",
                          String.valueOf(MAX_MESSAGE_MEMORY_SIZE));
        if (DEBUG) {
            props.setProperty("mail.debug", "true");
        }
        
        SESSION = Session.getInstance(props);
        
        if (DEBUG) {
            SESSION.setDebug(true);
        }

        UID_PROFILE = new FetchProfile();
        UID_PROFILE.add(UIDFolder.FetchProfileItem.UID);
    }

    public Pop3Import(DataSource ds) throws ServiceException {
        super(ds);
        store = getStore(ds);
    }
    
    public void importData(boolean fullSync) throws ServiceException {
        validateDataSource();
        connect();
        try {
            fetchMessages();
        } catch (MessagingException e) {
            // Only send the Java class name back to the client if there's no message
            String message = e.getMessage();
            if (message == null) {
                message = e.toString();
            }
            throw ServiceException.FAILURE(message, e);
        } catch (IOException e) {
            // Only send the Java class name back to the client if there's no message
            String message = e.getMessage();
            if (message == null) {
                message = e.toString();
            }
            throw ServiceException.FAILURE(message, e);
        } finally {
            disconnect();
        }
    }

    private static POP3Store getStore(DataSource ds) throws ServiceException {
        DataSource.ConnectionType type = ds.getConnectionType();
        String provider = getProvider(type);
        if (provider == null) {
            throw ServiceException.FAILURE("Invalid connection type: " + type, null);
        }
        try {
            return (POP3Store) SESSION.getStore(provider);
        } catch (NoSuchProviderException e) {
            throw ServiceException.FAILURE("Unknown provider: " + provider, e);
        }
    }

    private static String getProvider(DataSource.ConnectionType type) {
        switch (type) {
        case cleartext:
            return "pop3";
        case ssl:
            return "pop3s";
        default:
            return null;
        }
    }
    
    private void fetchMessages() throws MessagingException, IOException, ServiceException {
        DataSource ds = getDataSource();
        POP3Folder folder = (POP3Folder) store.getFolder("INBOX");
        folder.open(Folder.READ_WRITE);

        Message[] msgs = folder.getMessages();

        // Bail out if there are no messages
        if (msgs.length == 0) {
            folder.close(false);
            return;
        }
        
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(ds.getAccount());
        
        // Fetch message UID's for reconciliation (UIDL)
        folder.fetch(msgs, UID_PROFILE);
        
        // Check the UID of the first message to make sure the account isn't trying
        // to POP itself
        String uid = folder.getUID(msgs[0]);
        if (poppingSelf(mbox, uid)) {
            folder.close(false);
            throw ServiceException.INVALID_REQUEST(
                "User attempted to import messages from his own mailbox", null);
        }
        
        ZimbraLog.datasource.debug("Found %d messages on remote server", msgs.length);

        Set<String> uidsToFetch = null;
        if (ds.leaveOnServer()) {
            uidsToFetch = getUidsToFetch(mbox, ds, folder);
        }

        // Fetch message bodies (RETR)
        for (int i = 0; i < msgs.length; i++) {
            POP3Message pop3Msg = (POP3Message) msgs[i];
            if (ds.leaveOnServer()) {
                // Skip this message if we've already downloaded it
                uid = folder.getUID(pop3Msg);
                if (!uidsToFetch.contains(uid)) {
                    continue;
                }
            }

            try {
                // Add message to mailbox.  Validate the timestamp to avoid out-of-range
                // error in the database (see bug 17031).
                ParsedMessage pm;
                Date sentDate = pop3Msg.getSentDate();
                if (sentDate != null) {
                    // Set received date to the original message's date
                    pm = new ParsedMessage(pop3Msg, sentDate.getTime(),
                        mbox.attachmentsIndexingEnabled());
                } else {
                    // Otherwise use current time if date invalid
                    pm = new ParsedMessage(pop3Msg, mbox.attachmentsIndexingEnabled());
                }

                int msgid = addMessage(mbox, ds, pm, ds.getFolderId());

                if (ds.leaveOnServer()) {
                    DbPop3Message.storeUid(mbox, ds.getId(), folder.getUID(pop3Msg), msgid);
                }
            } finally {
                pop3Msg.dispose();
            }
        }

        if (!ds.leaveOnServer()) {
            // Mark all messages for deletion (DELE)
            for (Message msg : msgs) {
                msg.setFlag(Flags.Flag.DELETED, true);
            }
        }
        
        // Expunge if necessary and disconnect (QUIT)
        folder.close(!ds.leaveOnServer());
    }

    private int addMessage(Mailbox mbox, DataSource ds, ParsedMessage pm, int folderId) throws ServiceException, IOException {
    	com.zimbra.cs.mailbox.Message msg = null;
    	SharedDeliveryContext sharedDeliveryCtxt = new SharedDeliveryContext();
        if (folderId == Mailbox.ID_FOLDER_INBOX) {
        	try {
	            msg = RuleManager.getInstance().applyRules(mbox.getAccount(), mbox, pm, pm.getRawSize(), ds.getEmailAddress(), sharedDeliveryCtxt);
	            if (msg == null)
	            	 return 0; //null if DISCARD
        	} catch (Throwable t) {
        		ZimbraLog.datasource.warn("failed applying filter rules", t);
        	}
        }
        if (msg == null)
        	msg = mbox.addMessage(null, pm, folderId, false, Flag.BITMASK_UNREAD, null);
        return msg.getId();
    }
    
    private Set<String> getUidsToFetch(Mailbox mbox, DataSource ds, POP3Folder folder)
    throws MessagingException, ServiceException {
        Set<String> uidsToFetch = new HashSet<String>();
        for (Message msg : folder.getMessages()) {
            String uid = folder.getUID(msg);
            if (uid != null) {
                uidsToFetch.add(uid);
            }
        }
        
        // Remove UID's of messages that we already downloaded
        Set<String> existingUids =
            DbPop3Message.getMatchingUids(mbox, ds, uidsToFetch);
        uidsToFetch.removeAll(existingUids);
        return uidsToFetch;
    }

    private boolean poppingSelf(Mailbox mbox, String uid)
    throws ServiceException {
        Matcher matcher = PAT_ZIMBRA_UIDL.matcher(uid);
        if (!matcher.matches()) {
            // Not our UID
            return false;
        }
        
        // See if this UID comes from the current mailbox.  Popping from
        // another Zimbra mailbox is ok.
        int itemId = Integer.parseInt(matcher.group(1));
        String digest = matcher.group(2);
        com.zimbra.cs.mailbox.Message msg = null;
        
        try {
            msg = mbox.getMessageById(null, itemId);
        } catch (NoSuchItemException e) {
            return false;
        }
        
        if (!StringUtil.equal(digest, msg.getDigest())) {
            return false;
        }
        
        return true;
    }

    public Store getStore() {
        return store;
    }
}
