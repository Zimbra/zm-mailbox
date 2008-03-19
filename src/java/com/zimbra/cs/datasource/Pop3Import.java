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
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.CustomSSLSocketFactory;
import com.zimbra.common.util.DummySSLSocketFactory;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.SystemUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.db.DbPop3Message;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.SharedDeliveryContext;
import com.zimbra.cs.mime.ParsedMessage;


public class Pop3Import
implements MailItemImport {
    // (item id).(blob digest)
    private static final Pattern PAT_ZIMBRA_UIDL = Pattern.compile("(\\d+)\\.([^\\.]+)");
    
    private static Session sSession;
    private static Session sSelfSignedCertSession;
    private static FetchProfile UID_PROFILE;
    
    static {
    	
    	long timeout = LC.javamail_pop3_timeout.longValue() * Constants.MILLIS_PER_SECOND;
    	
        Properties props = new Properties();
        props.setProperty("mail.pop3.connectiontimeout", Long.toString(timeout));
        props.setProperty("mail.pop3.timeout", Long.toString(timeout));
        props.setProperty("mail.pop3s.connectiontimeout", Long.toString(timeout));
        props.setProperty("mail.pop3s.timeout", Long.toString(timeout));    	
		props.setProperty("mail.pop3s.socketFactory.class", CustomSSLSocketFactory.class.getName());
        props.setProperty("mail.pop3s.socketFactory.fallback", "false");
        if (LC.javamail_pop3_enable_starttls.booleanValue()) {
        	props.setProperty("mail.pop3.starttls.enable", "true");
        	props.setProperty("mail.pop3s.starttls.enable", "true");
        }
        sSession = Session.getInstance(props);
        if (LC.javamail_pop3_debug.booleanValue())
        	sSession.setDebug(true);
        
        Properties sscProps = new Properties();
        sscProps.setProperty("mail.pop3s.connectiontimeout", Long.toString(timeout));
        sscProps.setProperty("mail.pop3s.timeout", Long.toString(timeout));    	
        sscProps.setProperty("mail.pop3s.socketFactory.class", DummySSLSocketFactory.class.getName());
        sscProps.setProperty("mail.pop3s.socketFactory.fallback", "false");
        sSelfSignedCertSession = Session.getInstance(sscProps);
        if (LC.javamail_pop3_enable_starttls.booleanValue())
        	sscProps.setProperty("mail.pop3s.starttls.enable", "true");
        if (LC.javamail_pop3_debug.booleanValue())
        	sSelfSignedCertSession.setDebug(true);
        
        UID_PROFILE = new FetchProfile();
        UID_PROFILE.add(UIDFolder.FetchProfileItem.UID);
    }

    public String test(DataSource ds)
    throws ServiceException {
        String error = null;
        
        validateDataSource(ds);

        try {
            Store store = getStore(ds.getConnectionType());
            store.connect(ds.getHost(), ds.getPort(), ds.getUsername(), ds.getDecryptedPassword());
            store.close();
        } catch (MessagingException e) {
            ZimbraLog.datasource.info("Testing connection to data source", e);
            error = SystemUtil.getInnermostException(e).getMessage();
        }
        return error;
    }
    
    public void importData(Account account, DataSource dataSource)
    throws ServiceException {
        try {
            fetchMessages(account, dataSource);
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
        }
    }

    private void validateDataSource(DataSource ds)
    throws ServiceException {
        if (ds.getHost() == null) {
            throw ServiceException.FAILURE(ds + ": host not set", null);
        }
        if (ds.getPort() == null) {
            throw ServiceException.FAILURE(ds + ": port not set", null);
        }
        if (ds.getConnectionType() == null) {
            throw ServiceException.FAILURE(ds + ": connectionType not set", null);
        }
        if (ds.getUsername() == null) {
            throw ServiceException.FAILURE(ds + ": username not set", null);
        }
    }
    
    private Store getStore(DataSource.ConnectionType connectionType)
    throws NoSuchProviderException, ServiceException {
        if (connectionType == DataSource.ConnectionType.cleartext) {
            return sSession.getStore("pop3");
        } else if (connectionType == DataSource.ConnectionType.ssl) {
            if (LC.data_source_trust_self_signed_certs.booleanValue()) {
                return sSelfSignedCertSession.getStore("pop3s");
            } else {
                return sSession.getStore("pop3s");
            }
        } else {
            throw ServiceException.FAILURE("Invalid connectionType: " + connectionType, null);
        }
    }
    
    private void fetchMessages(Account account, DataSource ds)
    throws MessagingException, IOException, ServiceException {
        validateDataSource(ds);
        
        // Connect (USER, PASS, STAT)
        Store store = getStore(ds.getConnectionType());
        store.connect(ds.getHost(), ds.getPort(), ds.getUsername(), ds.getDecryptedPassword());
        POP3Folder folder = (POP3Folder) store.getFolder("INBOX");
        folder.open(Folder.READ_WRITE);

        Message[] msgs = folder.getMessages();

        // Bail out if there are no messages
        if (msgs.length == 0) {
            folder.close(false);
            store.close();
            return;
        }
        
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        
        // Fetch message UID's for reconciliation (UIDL)
        folder.fetch(msgs, UID_PROFILE);
        
        // Check the UID of the first message to make sure the account isn't trying
        // to POP itself
        String uid = folder.getUID(msgs[0]);
        if (poppingSelf(mbox, uid)) {
            folder.close(false);
            store.close();
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

            // Add message to mailbox.  Validate the timestamp to avoid out-of-range
            // error in the database (see bug 17031).
            ParsedMessage pm = null;
            Date sentDate = pop3Msg.getSentDate();
            if (sentDate != null && isValidDate(sentDate)) {
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
        }

        if (!ds.leaveOnServer()) {
            // Mark all messages for deletion (DELE)
            for (Message msg : msgs) {
                msg.setFlag(Flags.Flag.DELETED, true);
            }
        }
        
        // Expunge if necessary and disconnect (QUIT)
        folder.close(!ds.leaveOnServer());
        store.close();
    }

    // Make sure specified date is valid when represented as UNIX time
    // (seconds since the epoch).
    private static boolean isValidDate(Date date) {
        long time = date.getTime();
        return time >= 0 && time <= 0xffffffffL * 1000;
    }
    
    private int addMessage(Mailbox mbox, DataSource ds, ParsedMessage pm, int folderId) throws ServiceException, IOException {
    	com.zimbra.cs.mailbox.Message msg = null;
    	SharedDeliveryContext sharedDeliveryCtxt = new SharedDeliveryContext();
        if (folderId == Mailbox.ID_FOLDER_INBOX) {
        	try {
	            msg = RuleManager.getInstance().applyRules(mbox.getAccount(), mbox, pm, pm.getRawSize(), ds.getEmailAddress(), sharedDeliveryCtxt, Flag.BITMASK_UNREAD);
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
}
