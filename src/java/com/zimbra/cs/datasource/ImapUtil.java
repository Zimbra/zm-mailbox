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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailclient.imap.Literal;
import com.zimbra.cs.mailclient.imap.ImapConfig;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.mailclient.imap.Flags;
import com.zimbra.cs.mailclient.imap.ListData;
import com.zimbra.cs.mailclient.imap.ResponseHandler;
import com.zimbra.cs.mailclient.imap.ImapResponse;
import com.zimbra.cs.mailclient.imap.CAtom;
import com.zimbra.cs.mailclient.imap.MessageData;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.List;
import java.util.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.io.IOException;
import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;

public final class ImapUtil {
    public static void append(ImapConnection ic, String mbox, Message msg)
        throws ServiceException, IOException {
        append(ic, mbox, msg, false);
    }

    public static long appendUid(ImapConnection ic, String mbox, Message msg)
        throws ServiceException, IOException {
        return append(ic, mbox, msg, true);
    }
    
    private static long append(ImapConnection ic, String mailbox, Message msg,
                               boolean searchUid)
        throws ServiceException, IOException {
        ImapConfig config = (ImapConfig) ic.getConfig();
        File tmp = null;
        OutputStream os = null;
        MimeMessage mm = msg.getMimeMessage(false);
        long uid;
        try {
            tmp = File.createTempFile("lit", null, config.getLiteralDataDir());
            os = new FileOutputStream(tmp);
            mm.writeTo(os);
            os.close();
            Date date = mm.getReceivedDate();
            if (date == null) {
                date = mm.getSentDate();
            }
            Flags flags = FlagsUtil.zimbraToImapFlags(msg.getFlagBitmask());
            uid = ic.append(mailbox, flags, date, new Literal(tmp));
            if (uid <= 0 && searchUid) {
                // If server doesn't support UIDPLUS, search for UID of message
                if (!mailbox.equals(ic.getMailbox().getName())) {
                    ic.select(mailbox);
                }                                                             
                List<Long> uids = ic.uidSearch(
                    "ON", date, "HEADER", "Message-Id", mm.getMessageID());
                if (uids.size() == 1) {
                    uid = uids.get(0);
                }
            }
            return uid;
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("Error appending message", e);
        } finally {
            if (os != null) os.close();
            if (tmp != null) tmp.delete();
        }
    }

    public static List<MessageData> fetch(ImapConnection ic, String seq,
                                          Object param) throws IOException {
        final List<MessageData> mds = new ArrayList<MessageData>();
        ic.uidFetch(seq, param, new ResponseHandler() {
            public boolean handleResponse(ImapResponse res) {
                if (res.getCCode() == CAtom.FETCH) {
                    mds.add((MessageData) res.getData());
                    return true;
                }
                return false;
            }
        });
        return mds;
    }

    private static final String INBOX = "INBOX";
    private static final int INBOX_LEN = INBOX.length();

    // Used for sorting ListData lexicographically in reverse order. This
    // ensures that inferior mailboxes will be processed before their
    // parents, which avoids problems when deleting folders. Also, ignore
    // case of INBOX part when comparing INBOX or its inferiors.
    private static final Comparator<ListData> COMPARATOR =
        new Comparator<ListData>() {
            public int compare(ListData ld1, ListData ld2) {
                String name1 = getNormalizedName(ld1);
                String name2 = getNormalizedName(ld2);
                return name2.compareTo(name1);
            }
        };

    public static List<ListData> listFolders(ImapConnection ic)
        throws IOException {
        return sortFolders(ic.list("", "*"));
    }

    public static List<ListData> sortFolders(List<ListData> folders) {
        // Keep INBOX and inferiors separate so we can return them first
        ListData inbox = null;
        List<ListData> inboxInferiors = new ArrayList<ListData>();
        List<ListData> otherFolders = new ArrayList<ListData>();
        for (ListData ld : folders) {
            String name = ld.getMailbox();
            if (name.equalsIgnoreCase(INBOX)) {
                if (inbox == null) {
                    inbox = ld; // Ignore duplicate INBOX (fixes bug 26483)
                }
            } else if (isInboxInferior(ld)) {
                inboxInferiors.add(ld);
            } else {
                otherFolders.add(ld);
            }
        }
        List<ListData> sorted = new ArrayList<ListData>(folders.size());
        if (inbox != null) {
            sorted.add(inbox);
        }
        Collections.sort(inboxInferiors, COMPARATOR);
        sorted.addAll(inboxInferiors);
        Collections.sort(otherFolders, COMPARATOR);
        sorted.addAll(otherFolders);
        return sorted;
    }

    private static String getNormalizedName(ListData ld) {
        String name = ld.getMailbox();
        if (name.equalsIgnoreCase(INBOX)) {
            return INBOX;
        } else if (isInboxInferior(ld)) {
            return INBOX + name.substring(INBOX_LEN);
        } else {
            return name;
        }
    }
    
    /*
     * Returns true if specified ListData refers to am inferior of INBOX
     * (i.e. "INBOX/Foo").
     */
    private static boolean isInboxInferior(ListData ld) {
        String name = ld.getMailbox();
        return name.length() > INBOX_LEN &&
               name.substring(0, INBOX_LEN).equalsIgnoreCase(INBOX) &&
               name.charAt(INBOX_LEN) == ld.getDelimiter();
    }

}
