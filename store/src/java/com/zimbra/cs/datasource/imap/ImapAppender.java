/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2012, 2013, 2014, 2016, 2022 Synacor, Inc.
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
package com.zimbra.cs.datasource.imap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MailDateFormat;
import javax.mail.internet.MimeMessage;
import javax.mail.util.SharedByteArrayInputStream;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.zmime.ZMimeMessage;
import com.zimbra.common.zmime.ZSharedFileInputStream;
import com.zimbra.cs.datasource.SyncUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailclient.imap.AppendResult;
import com.zimbra.cs.mailclient.imap.CAtom;
import com.zimbra.cs.mailclient.imap.Envelope;
import com.zimbra.cs.mailclient.imap.Flags;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.mailclient.imap.ImapRequest;
import com.zimbra.cs.mailclient.imap.Literal;
import com.zimbra.cs.mailclient.imap.MailboxInfo;
import com.zimbra.cs.mailclient.imap.MailboxName;
import com.zimbra.cs.mailclient.imap.MessageData;
import com.zimbra.cs.mailclient.imap.ResponseText;
import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.store.StoreManager;

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

    @VisibleForTesting
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
            @Override
            public InputStream getInputStream() throws IOException {
                return new ZSharedFileInputStream(file);
            }

            @Override
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
    private long appendSlow(MessageInfo mi, Literal lit) throws IOException, MessagingException {
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
        try {
            connection.select(mailbox); //exchange doesn't give accurate UIDNEXT unless mbox is selected again.
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

                // bug 64062 : let's try 2 minutes. hopefully search shouldn't take too long now that we use msg id rather than subject
                connection.setReadTimeout(2 * 60);
                uids = connection.uidSearch(getSearchParams(mi));
            } finally {
                connection.setReadTimeout(connection.getConfig().getReadTimeout());
            }
            Iterator<Long> it = uids.iterator();
            while (it.hasNext()) {
                List<Long> found = findUids(nextSeq(it, 5), mi);
                if (found.size() > 0) {
                    if (found.size() > 1) {
                        ZimbraLog.imap_client.warn("found more than one (%d)"+
                                "matching UID during appendSlow. Probably a leftover dupe from earlier bugs?",found.size());
                        if (ZimbraLog.imap_client.isDebugEnabled()) {
                            ZimbraLog.imap_client.debug("potential duplicate ids = %s",Joiner.on(',').join(found));
                        }
                    }
                    return found.get(0);
                }
            }
        } catch(Exception e) {
            //if this is a real exception (e.g. network went down) next command will fail regardless.
            //otherwise, don't allow appendSlow to create loop
            ZimbraLog.imap_client.warn("Dedupe search in appendSlow failed.",e);
        }
        //this usually is OK, and actually the way Exchange has been working due to size check in matches()
        //we delete the local tracker and allow next sync to get the current version of message
        ZimbraLog.imap_client.warn("append slow failed to find appended message id");
        // If still not found, then give up :(
        return -1;
    }

    private Object[] getSearchParams(MessageInfo mi) throws MessagingException {
        List<Object> params = new ArrayList<Object>();
        params.add("SENTON");
        Date date = mi.mm.getSentDate();
        params.add(String.format("%td-%tb-%tY", date, date, date));
        String mId = mi.mm.getMessageID();
        params.add("HEADER");
        params.add("message-id");
        params.add(mId);
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

    private List<Long> findUids(String seq, final MessageInfo mi) throws IOException, MessagingException {
        final List<Long> uids = new ArrayList<Long>(1);
        Map<Long, MessageData> mds = connection.uidFetch(seq, "(RFC822.SIZE ENVELOPE)");
        for (MessageData md : mds.values()) {
            if (matches(mi, md)) {
                uids.add(md.getUid());
            }
        }
        return uids;
    }

    private boolean matches(MessageInfo mi, MessageData md) throws MessagingException {
        //bug 64062 Exchange misreports RFC 822 size unless configured with: Set-ImapSettings -EnableExactRFC822Size:$true
        //Message-ID, and optional Subject must match
        Envelope env = md.getEnvelope();
        if (env != null) {
            String subj = mi.mm.getSubject();
            return mi.mm.getMessageID().equals(env.getMessageId()) && (subj == null || subj.equals(env.getSubject()));
        }
        return false;
    }

    private long getUidNext() throws IOException {
        return connection.status(mailbox, "UIDNEXT").getUidNext();
    }

    private static Data getData(final byte[] b) {
        return new Data() {
            @Override
            public InputStream getInputStream() throws IOException {
                return new SharedByteArrayInputStream(b);
            }

            @Override
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
                @Override
                public InputStream getInputStream() throws IOException {
                    return StoreManager.getReaderSMInstance(mblob.getLocator()).getContent(mblob);
                }

                @Override
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
                mm = new ZMimeMessage(null, is);
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
