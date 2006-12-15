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
package com.zimbra.cs.mailbox;

import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
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
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.db.DbPop3Message;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mime.ParsedMessage;


public class Pop3Import
implements MailItemImport {

    private static final int TIMEOUT = 10 * (int) Constants.MILLIS_PER_SECOND;
    
    // (item id).(blob digest)
    private static final Pattern PAT_ZIMBRA_UIDL = Pattern.compile("(\\d+)\\.([^\\.]+)");
    
    private static Session sSession;
    private static Session sSelfSignedCertSession;
    private static Log sLog = LogFactory.getLog(Pop3Import.class);
    private static FetchProfile UID_PROFILE;
    
    static {
        Properties props = new Properties();
        props.put("mail.pop3.connectiontimeout", TIMEOUT);
        sSession = Session.getInstance(props);
        
        props.put("mail.pop3.socketFactory.class", "com.zimbra.common.util.DummySSLSocketFactory");
        sSelfSignedCertSession = Session.getInstance(props);
        
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
            sLog.info("Testing connection to data source", e);
            error = e.getMessage();
        }
        return error;
    }
    
    public void importData(Account account, DataSource dataSource)
    throws ServiceException {
        try {
            fetchMessages(account, dataSource);
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("Importing data from " + dataSource, e);
        } catch (IOException e) {
            throw ServiceException.FAILURE("Importing data from " + dataSource, e);
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
    
    private Store getStore(String connectionType)
    throws NoSuchProviderException, ServiceException {
        if (connectionType.equals(DataSource.CT_CLEARTEXT)) {
            return sSession.getStore("pop3");
        } else if (connectionType.equals(DataSource.CT_SSL)) {
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
        ZimbraLog.mailbox.info("Importing POP3 messages from " + ds);
        
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
        
        sLog.debug("Retrieving " + msgs.length + " messages");

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

            // Add message to mailbox
            ParsedMessage pm = null;
            if (pop3Msg.getSentDate() != null) {
                // Set received date to the original message's date
                pm = new ParsedMessage(pop3Msg, pop3Msg.getSentDate().getTime(),
                    mbox.attachmentsIndexingEnabled());
            } else {
                pm = new ParsedMessage(pop3Msg, mbox.attachmentsIndexingEnabled());
            }
            com.zimbra.cs.mailbox.Message zimbraMsg =
                mbox.addMessage(null, pm, ds.getFolderId(), false, Flag.BITMASK_UNREAD, null);

            if (ds.leaveOnServer()) {
                DbPop3Message.storeUid(mbox, ds.getId(), folder.getUID(pop3Msg), zimbraMsg.getId());
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
