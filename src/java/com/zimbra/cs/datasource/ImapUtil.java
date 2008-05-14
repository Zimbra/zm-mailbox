package com.zimbra.cs.datasource;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
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
import java.util.Set;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.io.IOException;
import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;

final class ImapUtil {
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
                if (res.getCode() == CAtom.FETCH) {
                    mds.add((MessageData) res.getData());
                    return true;
                }
                return false;
            }
        });
        return mds;
    }

    public static char getDelimiter(ImapConnection connection)
        throws IOException {
        List<ListData> ld = connection.list("", "");
        return ld.size() == 1 ? ld.get(0).getDelimiter() : 0;
    }
    
    public static List<ListData> listFolders(ImapConnection ic)
        throws IOException {
        List<ListData> list = ic.list("", "*");
        Set<String> names = new HashSet<String>(list.size());
        ListIterator<ListData> it = list.listIterator();
        while (it.hasNext()) {
            ListData ld = it.next();
            String name = fixFolderName(ld.getMailbox(), ld.getDelimiter());
            if (!names.add(name)) {
                ZimbraLog.datasource.warn(
                    "Skipping duplicate IMAP folder name: " + ld.getMailbox());
                it.remove();
            }
        }
        // Return list data sorted in descending order of mailbox name length.
        // This allows us to avoid problems if a parent mailbox is deleted
        // before its child.
        Collections.sort(list, new Comparator<ListData>() {
            public int compare(ListData ld1, ListData ld2) {
                return ld2.getMailbox().length() - ld1.getMailbox().length();
            }
        });
        return list;
    }

    /*
     * Workaround for bug 26483. Convert INBOX part of folder name to upper
     * case. This is needed to fix an issue where GMail's LIST command
     * sometimes returns the same folder name twice where INBOX differs
     * only in case.
     */
    private static String fixFolderName(String name, char separator) {
        int len = name.length();
        if (len < 5 || !name.substring(0, 5).equalsIgnoreCase("INBOX")) {
            return name;
        }
        if (len == 5) return "INBOX";
        return name.charAt(5) == separator ?
            "INBOX" + name.substring(5) : name;
    }
}
