/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.datasource.imap;

import com.zimbra.cs.datasource.SyncUtil;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.mailclient.imap.Flags;
import com.zimbra.cs.mailclient.imap.Literal;
import com.zimbra.cs.mailclient.imap.AppendResult;
import com.zimbra.cs.mailclient.imap.MessageData;
import com.zimbra.cs.mailclient.imap.Envelope;
import com.zimbra.cs.mailclient.imap.MailboxInfo;
import com.zimbra.cs.mailclient.imap.ImapRequest;
import com.zimbra.cs.mailclient.imap.CAtom;
import com.zimbra.cs.mailclient.imap.MailboxName;
import com.zimbra.cs.mailclient.imap.ResponseText;
import com.zimbra.cs.mailclient.imap.ImapData;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.common.service.ServiceException;

import javax.mail.internet.MimeMessage;
import javax.mail.internet.MailDateFormat;
import javax.mail.MessagingException;
import java.util.List;
import java.util.Date;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;

public class ImapAppender {
    private final ImapConnection connection;
    private final String mailbox;
    private boolean hasAppendUid;
    private MailDateFormat mdf;

    public ImapAppender(ImapConnection connection, String mailbox) {
        this.connection = connection;
        this.mailbox = mailbox;
        hasAppendUid = connection.hasUidPlus();
    }

    public ImapAppender setHasAppendUid(boolean hasAppendUid) {
        this.hasAppendUid = hasAppendUid;
        return this;
    }

    public long appendMessage(Message msg) throws IOException, ServiceException {
        return append(new MessageInfo(msg));
    }

    public long appendMessage(File file, Flags flags) throws IOException, ServiceException {
        return append(new MessageInfo(getData(file), flags));
    }

    public long appendMessage(byte[] b, Flags flags) throws IOException, ServiceException {
        return append(new MessageInfo(getData(b), flags));
    }

    private long append(MessageInfo mi) throws IOException, ServiceException {
        InputStream is = mi.data.getInputStream();
        Literal lit = new Literal(is, mi.data.getSize());
        try {
            return hasAppendUid ? append(mi, lit) : appendSlow(mi, lit);
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("Parsing error", e);
        } finally {
            is.close();
        }
    }

    private static Data getData(final File file) {
        return new Data() {
            public InputStream getInputStream() throws IOException {
                return new FileInputStream(file);
            }
            public int getSize() {
                return (int) file.length();
            }
        };
    }
    
    private long append(MessageInfo mi, Literal data) throws IOException {
        ImapRequest req = connection.newRequest(CAtom.APPEND, new MailboxName(mailbox));
        if (mi.flags != null) {
            req.addParam(mi.flags);
        }
        if (mi.date != null) {
            req.addParam(mi.date);
        }
        req.addParam(data);
        ResponseText rt = req.sendCheckStatus().getResponseText();
        if (rt != null && rt.getCCode() == CAtom.APPENDUID) {
            AppendResult ar = (AppendResult) rt.getData();
            if (ar.getUid() > 0) {
                return ar.getUid();
            }
        }
        throw req.failed("APPENDUID supported but UID missing from result");
    }

    // Slow APPEND for servers lacking UIDPLUS capability
    private long appendSlow(MessageInfo mi, Literal lit)
        throws IOException, MessagingException {
        MailboxInfo mb = connection.getMailboxInfo();
        if (mdf == null) {
            mdf = new MailDateFormat();
        }
        if (mb == null || !mailbox.equals(mb.getName())) {
            connection.select(mailbox);
        }
        long startUid = getUidNext();
        connection.append(mailbox, mi.flags, mi.date, lit);
        if (mi.mm.getSentDate() == null || mi.mm.getMessageID() == null) {
            // "Date" and "Message-ID" headers are required to find message
            return -1;
        }
        // Check new messages for the one we just appended
        long endUid = getUidNext() - 1;
        if (startUid <= endUid) {
            List<Long> found = findUids(startUid + ":" + endUid, mi);
            if (found.size() == 1) {
                return found.get(0);
            }
        }
        // If not found then server must have de-duped the message. This
        // is certainly possible with GMail. Search through the entire mailbox
        // for matching message and hope this is not too slow.
        List<Long> uids;
        try {
            // bug 45385: Temporarily increase timeout to 10 minutes in case of slow search.
            // Not pretty, but hopefully we never get here since most servers now support
            // UIDPLUS or don't de-dup messages.
            connection.setReadTimeout(10 * 60);
            uids = connection.uidSearch(getSearchParams(mi));
        } finally {
            connection.setReadTimeout(connection.getConfig().getReadTimeout());
        }
        Iterator<Long> it = uids.iterator();
        while (it.hasNext()) {
            List<Long> found = findUids(nextSeq(it, 5), mi);
            // TODO What if we find more than one match?
            if (found.size() > 0) {
                return found.get(0);
            }
        }
        // If still not found, then give up :(
        return -1;
    }

    private Object[] getSearchParams(MessageInfo mi) throws MessagingException {
        List<Object> params = new ArrayList<Object>();
        String subj = mi.mm.getSubject();
        if (subj != null) {
            ImapData data = ImapData.asString(subj);
            if (data.isLiteral()) {
                params.add("CHARSET");
                params.add("UTF-8");
            }
            params.add("SUBJECT");
            params.add(data);
        }
        params.add("SENTON");
        Date date = mi.mm.getSentDate();
        params.add(String.format("%td-%tb-%tY", date, date, date));
        return params.toArray();
    }

    private String nextSeq(Iterator<Long> it, int count) {
        assert it.hasNext();
        StringBuilder sb = new StringBuilder();
        sb.append(it.next());
        while (--count > 0 && it.hasNext()) {
            sb.append(',').append(it.next());
        }
        return sb.toString();
    }

    private List<Long> findUids(String seq, final MessageInfo mi)
        throws IOException, MessagingException {
        final List<Long> uids = new ArrayList<Long>(1);
        Map<Long, MessageData> mds = connection.uidFetch(seq, "(RFC822.SIZE ENVELOPE)");
        for (MessageData md : mds.values()) {
            if (matches(mi, md)) {
                uids.add(md.getUid());
            }
        }
        return uids;
    }

    private boolean matches(MessageInfo mi, MessageData md)
        throws IOException, MessagingException {
        // Message size must match
        if (mi.data.getSize() == md.getRfc822Size()) {
            // Message-ID, and optional Subject must match
            Envelope env = md.getEnvelope();
            if (env != null) {
                String subj = mi.mm.getSubject();
                return mi.mm.getMessageID().equals(env.getMessageId()) &&
                       (subj == null || subj.equals(env.getSubject()));
            }
        }
        return false;
    }

    private long getUidNext() throws IOException {
        return connection.status(mailbox, "UIDNEXT").getUidNext();
    }


    private static Data getData(final byte[] b) {
        return new Data() {
            public InputStream getInputStream() throws IOException {
                return new ByteArrayInputStream(b);
            }
            public int getSize() {
                return b.length;
            }
        };
    }

    private static class MessageInfo {
        Data data;
        MimeMessage mm;
        Flags flags;
        Date date;

        MessageInfo(Message msg) throws ServiceException {
            final MailboxBlob mblob = msg.getBlob();
            data = new Data() {
                public InputStream getInputStream() throws IOException {
                    return StoreManager.getInstance().getContent(mblob);
                }
                public int getSize() throws IOException {
                    return (int) mblob.getSize();
                }
            };
            mm = msg.getMimeMessage(false);
            flags = SyncUtil.zimbraToImapFlags(msg.getFlagBitmask());
            date = SyncUtil.getInternalDate(msg, mm);
        }

        MessageInfo(Data data, Flags flags) throws ServiceException, IOException {
            this.data = data;
            InputStream is = data.getInputStream();
            try {
                mm = new MimeMessage(null, is);
                this.flags = flags;
                date = mm.getReceivedDate();
                if (date == null) {
                    date = new Date(System.currentTimeMillis());
                }
            } catch (MessagingException e) {
                throw ServiceException.FAILURE("Unable to parse message", e);
            } finally {
                is.close();
            }
        }
    }

    private static interface Data {
        InputStream getInputStream() throws IOException;
        int getSize() throws IOException;
    }
}
