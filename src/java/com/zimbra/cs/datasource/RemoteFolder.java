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

import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.mailclient.imap.ImapCapabilities;
import com.zimbra.cs.mailclient.imap.Mailbox;
import com.zimbra.cs.mailclient.imap.Flags;
import com.zimbra.cs.mailclient.imap.ImapConfig;
import com.zimbra.cs.mailclient.imap.Literal;
import com.zimbra.cs.mailclient.imap.ImapData;
import com.zimbra.cs.mailclient.CommandFailedException;
import com.zimbra.cs.mailclient.MailException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;

import javax.mail.internet.MimeMessage;
import javax.mail.MessagingException;
import java.io.IOException;
import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.List;
import java.util.Collections;

class RemoteFolder {
    private final ImapConnection connection;
    private final String name;
    private final boolean uidPlus;

    private static final Log LOG = ZimbraLog.datasource;
    
    RemoteFolder(ImapConnection connection, String name) {
        this.connection = connection;
        this.name = name;
        uidPlus = connection.hasCapability(ImapCapabilities.UIDPLUS);
    }

    public void create() throws IOException {
        LOG.info("Creating IMAP folder '%s'", name);
        try {
            connection.create(name);
        } catch (CommandFailedException e) {
            // OK if CREATE failed because mailbox already exists
            if (!exists()) throw e;
        }
    }

    public void delete() throws IOException {
        LOG.info("Deleting IMAP folder '%s'", name);
        try {
            connection.delete(name);
        } catch (CommandFailedException e) {
            // OK if DELETE failed because mailbox didn't exist
            if (exists()) throw e;
        }
    }

    public void renameTo(String newName) throws IOException {
        LOG.info("Renaming IMAP folder from '%s' to '%s'", name, newName);
        connection.rename(name, newName);
    }

    public long appendMessage(MimeMessage msg, Flags flags, Date date)
        throws IOException {
        ensureSelected();
        ImapConfig config = connection.getImapConfig();
        File tmp = null;
        OutputStream os = null;
        try {
            tmp = File.createTempFile("lit", null, config.getLiteralDataDir());
            os = new FileOutputStream(tmp);
            msg.writeTo(os);
            os.close();
            return connection.append(name, flags, date, new Literal(tmp));
        } catch (MessagingException e) {
            throw new MailException("Error appending message", e);
        } finally {
            if (os != null) os.close();
            if (tmp != null) tmp.delete();
        }
    }

    public void deleteMessages(List<Long> uids) throws IOException {
        ensureSelected();
        int size = uids.size();
        LOG.info("Deleting and expunging %d message(s) in IMAP folder '%s'",
                 size, name);
        for (int i = 0; i < size; i += 16) {
            String seq = ImapData.asSequenceSet(
                uids.subList(i, Math.min(size - i, 16)));
            connection.uidStore(seq, "+FLAGS.SILENT", "(\\Deleted)");
            // If UIDPLUS supported, then expunge deleted messages
            if (uidPlus) {
                connection.uidExpunge(seq);
            }
        }
        // If UIDPLUS not supported, then all we can do is expunge all messages
        // which unfortunately may end up removing some messages we ourselves
        // did not flag for deletion.
        if (!uidPlus) {
            connection.expunge();
        }
    }

    public List<Long> getUids(long startUid, long endUid) throws IOException {
        ensureSelected();
        if (endUid > 0 && startUid > endUid) {
            return Collections.emptyList();
        }
        String end = endUid > 0 ? String.valueOf(endUid) : "*";
        return connection.getUids(startUid + ":" + end);
    }

    public boolean exists() throws IOException {
        return !connection.list("", name).isEmpty();
    }
    
    public void ensureSelected() throws IOException {
        if (!isSelected()) {
            select();
        }
    }
    
    public Mailbox select() throws IOException {
        return connection.select(name);
    }
    
    public boolean isSelected() {
        Mailbox mb = connection.getMailbox();
        return mb != null && mb.getName().equals(name);
    }
}
