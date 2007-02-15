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
import java.util.Map;
import java.util.Properties;

import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.db.DbImapMessage;
import com.zimbra.cs.db.ImapMessage;
import com.zimbra.cs.mime.ParsedMessage;


public class ImapImport
implements MailItemImport {

    private static final long TIMEOUT = 20 * Constants.MILLIS_PER_SECOND;
    
    private static Session sSession;
    private static Session sSelfSignedCertSession;
    private static Log sLog = LogFactory.getLog(ImapImport.class);
    private static FetchProfile FETCH_PROFILE;
    
    static {
        Properties props = new Properties();
        props.setProperty("mail.imap.connectiontimeout", Long.toString(TIMEOUT));
        props.setProperty("mail.imap.timeout", Long.toString(TIMEOUT));
        sSession = Session.getInstance(props);
        
        props.put("mail.imap.socketFactory.class", com.zimbra.common.util.DummySSLSocketFactory.class.getName());
        sSelfSignedCertSession = Session.getInstance(props);
        
        FETCH_PROFILE = new FetchProfile();
        FETCH_PROFILE.add(UIDFolder.FetchProfileItem.UID);
        FETCH_PROFILE.add(UIDFolder.FetchProfileItem.FLAGS);
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
            importDataInternal(account, dataSource);
        } catch (MessagingException e) {
            throw ServiceException.FAILURE(e.getMessage(), e);
        } catch (IOException e) {
            throw ServiceException.FAILURE(e.getMessage(), e);
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
            return sSession.getStore("imap");
        } else if (connectionType == DataSource.ConnectionType.ssl) {
            if (LC.data_source_trust_self_signed_certs.booleanValue()) {
                return sSelfSignedCertSession.getStore("imaps");
            } else {
                return sSession.getStore("imaps");
            }
        } else {
            throw ServiceException.FAILURE("Invalid connectionType: " + connectionType, null);
        }
    }

    private void importDataInternal(Account account, DataSource ds)
    throws MessagingException, IOException, ServiceException {
        validateDataSource(ds);
        
        // Connect (USER, PASS, STAT)
        Store store = getStore(ds.getConnectionType());
        store.connect(ds.getHost(), ds.getPort(), ds.getUsername(), ds.getDecryptedPassword());
        IMAPFolder folder = (IMAPFolder) store.getFolder("INBOX");
        folder.open(Folder.READ_ONLY);

        Message[] msgs = folder.getMessages();
        sLog.debug("Found %d messages on remote server", msgs.length);

        if (msgs.length > 0) {
            // Fetch message UID's for reconciliation (UIDL)
            folder.fetch(msgs, FETCH_PROFILE);
        }
        
        // TODO: Check the UID of the first message to make sure the user isn't
        // importing his own mailbox.
        /*
        long uid = folder.getUID(msgs[0]);
        if (isOwnUid(mbox, uid)) {
            folder.close(false);
            store.close();
            throw ServiceException.INVALID_REQUEST(
                "User attempted to import messages from his own mailbox", null);
        }
        */

        // Insert new messages
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        Map<Long, ImapMessage> storedMsgs = DbImapMessage.getImapMessages(mbox, ds);
        
        for (int i = 0; i < msgs.length; i++) {
            IMAPMessage imapMsg = (IMAPMessage) msgs[i];
            long uid = folder.getUID(imapMsg);
            
            if (storedMsgs.containsKey(uid)) {
                // We already know about this message.
                ImapMessage storedMsg = storedMsgs.remove(uid);
                int appliedFlags = applyFlags(imapMsg, storedMsg.getFlags());
                if (appliedFlags != storedMsg.getFlags()) {
                    // Flags changed
                    mbox.setTags(null, storedMsg.getItemId(), MailItem.TYPE_MESSAGE, appliedFlags, MailItem.TAG_UNCHANGED);
                }
            } else {
                // Add message to mailbox
                ParsedMessage pm = null;
                if (imapMsg.getSentDate() != null) {
                    // Set received date to the original message's date
                    pm = new ParsedMessage(imapMsg, imapMsg.getSentDate().getTime(),
                        mbox.attachmentsIndexingEnabled());
                } else {
                    pm = new ParsedMessage(imapMsg, mbox.attachmentsIndexingEnabled());
                }
                int flags = applyFlags(imapMsg, 0);
                com.zimbra.cs.mailbox.Message zimbraMsg =
                    mbox.addMessage(null, pm, ds.getFolderId(), false, flags, null);

                DbImapMessage.storeImapMessage(
                    mbox, ds.getId(), folder.getUID(imapMsg), zimbraMsg.getId());
            }
        }
        
        // Remaining UID's are not on the remote server.  Delete local copies.
        if (storedMsgs.size() > 0) {
            int[] idArray = new int[storedMsgs.size()];
            int i = 0;
            for (ImapMessage imapMsg: storedMsgs.values()) {
                idArray[i++] = imapMsg.getItemId();
            }
            mbox.delete(null, idArray, MailItem.TYPE_MESSAGE, null);
        }
        
        // Expunge if necessary and disconnect (QUIT)
        folder.close(false);
        store.close();
    }
    
    private static final Flags.Flag[] IMAP_FLAGS = {
        Flags.Flag.ANSWERED,
        Flags.Flag.DELETED,
        Flags.Flag.DRAFT,
        Flags.Flag.FLAGGED,
        Flags.Flag.SEEN
    };
    
    private static final int[] ZIMBRA_FLAG_BITMASKS = {
        Flag.BITMASK_REPLIED,
        Flag.BITMASK_DELETED,
        Flag.BITMASK_DRAFT,
        Flag.BITMASK_FLAGGED,
        Flag.BITMASK_UNREAD
    };
    
    /**
     * Applies the flags from a JavaMail message to the given flag bitmask. 
     */
    private int applyFlags(Message remoteMsg, int flags)
    throws MessagingException {
        for (int i = 0; i < IMAP_FLAGS.length; i++) {
            Flags.Flag imapFlag = IMAP_FLAGS[i];
            boolean isSet = remoteMsg.isSet(imapFlag);
            if (imapFlag == Flags.Flag.SEEN) {
                // IMAP uses "seen", we use "unread"
                isSet = !isSet;
            }
            if (isSet) {
                flags |= ZIMBRA_FLAG_BITMASKS[i];
            } else {
                flags &= ~ZIMBRA_FLAG_BITMASKS[i];
            }
        }
        
        return flags;
    }
}
