/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008 Zimbra, Inc.
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

import com.zimbra.cs.mailclient.pop3.Pop3Connection;
import com.zimbra.cs.mailclient.pop3.Pop3Config;
import com.zimbra.cs.mailclient.pop3.Pop3Capabilities;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.db.DbPop3Message;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.DummySSLSocketFactory;
import com.zimbra.common.util.CustomSSLSocketFactory;
import com.zimbra.common.util.SystemUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.localconfig.LC;

import javax.mail.MessagingException;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Set;
import java.util.List;

public class Pop3Sync extends AbstractMailItemImport {
    private final Pop3Connection connection;
    private final Mailbox mbox;
    private final boolean attachmentsIndexingEnabled;
    private final byte[] buffer = new byte[4096];

    private static final boolean DEBUG = true;

    private static final Log LOG = ZimbraLog.datasource;
    static {
        if (DEBUG) LOG.setLevel(Log.Level.debug);
    }

    // Zimbra UID format is: item_id "." blob_digest
    private static final Pattern PATTERN_ZIMBRA_UID =
        Pattern.compile("(\\d+)\\.([^\\.]+)");

    public Pop3Sync(DataSource ds) throws ServiceException {
        super(ds);
        connection = new Pop3Connection(getPop3Config(ds));
        mbox = ds.getMailbox();
        attachmentsIndexingEnabled = mbox.attachmentsIndexingEnabled();
    }

    private static Pop3Config getPop3Config(DataSource ds) {
        Pop3Config config = new Pop3Config();
        config.setHost(ds.getHost());
        config.setPort(ds.getPort());
        config.setAuthenticationId(ds.getUsername());
        config.setTlsEnabled(LC.javamail_pop3_enable_starttls.booleanValue());
        config.setSslEnabled(ds.isSslEnabled());
        config.setDebug(DEBUG);
        config.setTrace(DEBUG);
        // config.setRawMode(true);
        if (LC.data_source_trust_self_signed_certs.booleanValue()) {
            config.setSSLSocketFactory(new DummySSLSocketFactory());
        } else {
            config.setSSLSocketFactory(new CustomSSLSocketFactory());
        }
        return config;
    }
    
    public synchronized String test() throws ServiceException {
        validateDataSource();
        try {
            connect();
        } catch (ServiceException e) {
            Throwable except = SystemUtil.getInnermostException(e);
            if (except == null) except = e;
            ZimbraLog.datasource.info("Error connecting to mail store: ", except);
            return except.toString();
        } finally {
            connection.close();
        }
        return null;
    }

    public synchronized void importData(boolean fullSync) throws ServiceException {
        validateDataSource();
        connect();
        try {
            if (dataSource.leaveOnServer()) {
                fetchAndRetainMessages();
            } else {
                fetchAndDeleteMessages();
            }
            connection.quit();
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw ServiceException.FAILURE(
                "Synchronization of POP3 folder failed", e);
        } finally {
            connection.close();
        }
    }

    private void connect() throws ServiceException {
        if (!connection.isClosed()) return;
        try {
            connection.connect();
        } catch (IOException e) {
            connection.close();
            throw ServiceException.FAILURE(
                "Unable to connect to IMAP server: " + dataSource, e);
        }
    }

    private void fetchAndDeleteMessages()
        throws ServiceException, MessagingException, IOException {
        int count = connection.getMessageCount();
        LOG.info("Found %d new messages on remote server", count);
        for (int i = 1; i <= count; i++) {
            InputStream is = connection.getMessage(i);
            try {
                addMessage(is, null);
            } finally {
                is.close();
            }
            connection.delete(i);
        }
    }

    private void fetchAndRetainMessages()
        throws ServiceException, MessagingException, IOException {
        if (!connection.hasCapability(Pop3Capabilities.UIDL)) {
            throw ServiceException.FAILURE(
                "POP3 server must support UIDL extension", null);
        }
        List<String> uids = connection.getMessageUids();
        Set<String> existingUids =
            DbPop3Message.getMatchingUids(mbox, dataSource, uids);
        int count = uids.size() - existingUids.size();
        LOG.info("Found %d new messages on remote server", count);
        if (count == 0) {
            return; // No new messages
        }
        if (poppingSelf(mbox, uids.get(0))) {
            throw ServiceException.INVALID_REQUEST(
                "User attempted to import messages from his own mailbox", null);
        }
        for (int i = 0; i < uids.size(); i++) {
            String uid = uids.get(i);
            if (!existingUids.contains(uid)) {
                LOG.debug("Fetching message with uid = %s", uid);
                InputStream is = connection.getMessage(i + 1);
                try {
                    addMessage(is, uid);
                } finally {
                    is.close();
                }
            }
        }
    }

    private void addMessage(InputStream is, String uid)
        throws ServiceException, MessagingException, IOException {
        File tmp = newTempFile(is);
        try {
            ParsedMessage pm = new ParsedMessage(tmp, null, attachmentsIndexingEnabled);
            int msgId = addMessage(pm, dataSource.getFolderId(), Flag.BITMASK_UNREAD);
            if (uid != null) {
                DbPop3Message.storeUid(mbox, dataSource.getId(), uid, msgId);
            }
        } finally {
            tmp.delete();
        }
    }

    private File newTempFile(InputStream is) throws IOException {
        File tmp = File.createTempFile("pop", null);
        tmp.deleteOnExit();
        try {
            OutputStream os = new FileOutputStream(tmp);
            try {
                int len;
                while ((len = is.read(buffer)) != -1) {
                    os.write(buffer, 0, len);
                }
            } finally {
                os.close();
            }
        } catch (IOException e) {
            tmp.delete();
            throw e;
        }
        return tmp;
    }

    private static boolean poppingSelf(Mailbox mbox, String uid)
        throws ServiceException {
        Matcher matcher = PATTERN_ZIMBRA_UID.matcher(uid);
        if (!matcher.matches()) {
            return false; // Not a Zimbra UID
        }
        // See if this UID comes from the specified mailbox. Popping from
        // another Zimbra mailbox is ok.
        int itemId;
        try {
            itemId = Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException e) {
            return false;
        }
        String digest = matcher.group(2);
        Message msg;
        try {
            msg = mbox.getMessageById(null, itemId);
        } catch (MailServiceException.NoSuchItemException e) {
            return false;
        }
        return digest.equals(msg.getDigest());
    }
}
