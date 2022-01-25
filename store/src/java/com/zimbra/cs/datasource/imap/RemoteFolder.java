/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailclient.CommandFailedException;
import com.zimbra.cs.mailclient.imap.CAtom;
import com.zimbra.cs.mailclient.imap.CopyResult;
import com.zimbra.cs.mailclient.imap.FetchResponseHandler;
import com.zimbra.cs.mailclient.imap.Flags;
import com.zimbra.cs.mailclient.imap.ImapConnection;
import com.zimbra.cs.mailclient.imap.ImapData;
import com.zimbra.cs.mailclient.imap.ImapRequest;
import com.zimbra.cs.mailclient.imap.MailboxInfo;
import com.zimbra.cs.mailclient.imap.MailboxName;
import com.zimbra.cs.mailclient.imap.MessageData;
import com.zimbra.cs.mailclient.imap.ResponseText;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;

final class RemoteFolder {
    private final ImapConnection connection;
    private String path;
    private int deleted;
    
    /**
     * @see ZMailbox#PATH_SEPARATOR
     **/
    public final static String PATH_SEPARATOR = "/";
    private static final Log LOG = ZimbraLog.datasource;

    RemoteFolder(ImapConnection connection, String path) {
        this.connection = connection;
        this.path = path;
    }
   
    public void create() throws IOException {
        info("creating folder");
        try {
            connection.create(path);
        } catch (CommandFailedException e) {
            // OK if CREATE failed because mailbox already exists
            if (!exists()) throw e;
        }
    }

    public void delete() throws IOException {
        info("deleting folder");
        try {
            connection.delete(path);
        } catch (CommandFailedException e) {
            // OK if DELETE failed because mailbox didn't exist
            if (exists()) throw e;
        }
    }

    public RemoteFolder renameTo(String newName) throws IOException {
        info("renaming folder to '%s'", newName);

        // ZBUG-2192
        // execute only when last folder name's are different
        // don't change any parent folder names
        // using "path" as base name we are forming new folder name in such a way only last folder get's renamed at a time
        
        if (!isSameFolderName(newName)) {
            String baseNewPath = path;
            if (StringUtils.substringAfterLast(baseNewPath, PATH_SEPARATOR).isEmpty()
                    && StringUtils.substringAfterLast(newName, PATH_SEPARATOR).isEmpty()) {
                baseNewPath = newName;
            } else {
                baseNewPath = StringUtils.substringBeforeLast(baseNewPath, PATH_SEPARATOR) + PATH_SEPARATOR
                        + StringUtils.substringAfterLast(newName, PATH_SEPARATOR);
                if (StringUtils.endsWith(baseNewPath, PATH_SEPARATOR) || StringUtils.startsWith(baseNewPath, PATH_SEPARATOR)) {
                    baseNewPath = StringUtils.removeEnd(baseNewPath, PATH_SEPARATOR);
                    baseNewPath = StringUtils.removeStart(baseNewPath, PATH_SEPARATOR);
                }
            }
            connection.rename(path, baseNewPath);
        }
        return new RemoteFolder(connection, newName);
    }

    // ZBUG-2192
    // The helper method to make comparison of old and new folder name and
    // returns boolean value
    private boolean isSameFolderName(String newPathName) {

        String oldPathName = path.substring(path.lastIndexOf(PATH_SEPARATOR) + 1);
        newPathName = newPathName.substring(newPathName.lastIndexOf(PATH_SEPARATOR) + 1);

        return oldPathName.equals(newPathName);
	
    }
    
    public CopyResult copyMessage(long uid, String mbox) throws IOException {
        assert isSelected();
        String seq = String.valueOf(uid);
        ImapRequest req = connection.newUidRequest(CAtom.COPY, seq, new MailboxName(mbox));
        ResponseText rt = req.sendCheckStatus().getResponseText();
        if (rt.getCCode() == CAtom.COPYUID) {
            CopyResult cr = (CopyResult) rt.getData();
            // Bug 36373: If COPYUID result 0 then assume that message no longer exists.
            if (cr != null && cr.getToUids()[0] != 0) {
                return cr;
            }
        }
        return null; // Message not found
    }

    /**
     * Deletes and expunges messages for specified UIDs.
     *
     * @param uids the UIDs to be deleted and expunged
     * @throws java.io.IOException if an I/O error occurred
     */
    public void deleteMessages(List<Long> uids) throws IOException {
        assert isSelected();
        int size = uids.size();
        debug("deleting %d messages(s) from folder", size);
        for (int i = 0; i < size; i += 16) {
            String seq = ImapData.asSequenceSet(
                uids.subList(i, i + Math.min(size - i, 16)));
            connection.uidStore(seq, "+FLAGS.SILENT", "(\\Deleted)");
            // If UIDPLUS supported, then expunge deleted messages
            if (connection.hasUidPlus()) {
                connection.uidExpunge(seq);
            }
        }
        deleted += size;
    }

    public void deleteMessage(long uid) throws IOException {
        assert isSelected();
        debug("deleting message with uid %d", uid);
        String seq = String.valueOf(uid);
        connection.uidStore(seq, "+FLAGS.SILENT", "(\\Deleted)");
        // If UIDPLUS supported, then expunge deleted message
        if (connection.hasUidPlus()) {
            connection.uidExpunge(seq);
        }
        deleted++;
    }

    /**
     * Closes folder and optionally expunges deleted messages.
     *
     * @throws java.io.IOException if an I/O error occurred
     */
    public void close() throws IOException {
        if (deleted > 0 && !connection.hasUidPlus()) {
            connection.close_mailbox();
        }
    }

    public List<Long> getUids(long startUid, long endUid) throws IOException {
        assert isSelected();
        String end = endUid > 0 ? String.valueOf(endUid) : "*";
        List<Long> uids = connection.getUids(startUid + ":" + end);
        // If sequence is "<startUid>:*" and there are no messages with UID
        // greater than startUid, the the UID of the last message will always
        // be returned (RFC 3501 6.4.8). We want to make sure to exlude this
        // result.
        if (endUid <= 0 && uids.size() == 1 && uids.get(0) < startUid) {
            return Collections.emptyList();
        }
        //Yahoo sometimes returns out-of-range UIDs; bug 59773
        Iterator<Long> it = uids.iterator();
        while (it.hasNext()) {
            Long uid = it.next();
            if (uid < startUid || (endUid > 0 && uid > endUid)) {
                LOG.warn("UID FETCH %d:%d returned UID out of range: %d", startUid, endUid, uid);
                it.remove();
            }
        }
        // Sort UIDs in reverse order so we download latest messages first
        Collections.sort(uids, Collections.reverseOrder());
        return uids;
    }

    /*
     * Fetch message flags for specific UID sequence. Exclude messages which
     * have been flagged \Deleted.
     */
    public List<MessageData> getFlags(long startUid, long endUid)
        throws IOException {
        final List<MessageData> mds = new ArrayList<MessageData>();
        String end = endUid > 0 ? String.valueOf(endUid) : "*";
        connection.uidFetch(startUid + ":" + end, "FLAGS",
            new FetchResponseHandler() {
                public void handleFetchResponse(MessageData md) {
                    Flags flags = md.getFlags();
                    if (flags != null && !flags.isDeleted()) {
                        mds.add(md);
                    }
                }
            }
        );
        // If sequence is "<startUid>:*" and there are no messages with UID
        // greater than startUid, the the UID of the last message will always
        // be returned (RFC 3501 6.4.8). We want to make sure to exlude this
        // result.
        if (endUid <= 0 && mds.size() == 1 && mds.get(0).getUid() < startUid) {
            return Collections.emptyList();
        }
        return mds;
    }

    public boolean exists() throws IOException {
        return !connection.list("", path).isEmpty();
    }

    public MailboxInfo getMailboxInfo() {
        return connection.getMailboxInfo();
    }
    
    public MailboxInfo select() throws IOException {
        MailboxInfo mi = connection.select(path);
        // Bug 35554: If server does not provide UIDVALIDITY, then assume a value of 1
        if (mi.getUidValidity() <= 0) {
            mi.setUidValidity(1);
        }
        if (mi.getExists() == -1) {
            debug("Server did not provide EXISTS");
            mi.setExists(1);
        }
        return mi;
    }

    public MailboxInfo status() throws IOException {
        MailboxInfo mi = connection.status(path, "UIDVALIDITY", "UIDNEXT", "MESSAGES");
        // Bug 35554: If server does not provide UIDVALIDITY, then assume a value of 1
        if (mi.getUidValidity() <= 0) {
            mi.setUidValidity(1);
        }
        if (!path.equals(mi.getName())) {
            path = mi.getName();
        }
        if (mi.getExists() == -1) {
            debug("Server did not provide MESSAGES");
            mi.setExists(1);
        }
        return mi;
    }

    public boolean isSelected() {
        MailboxInfo mb = connection.getMailboxInfo();
        return mb != null && mb.getName().equals(path);
    }

    public String getPath() {
        return path;
    }
    
    
    public void debug(String fmt, Object... args) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(errmsg(String.format(fmt, args)));
        }
    }

   

    
	public void info(String fmt, Object... args) {
        LOG.info(errmsg(String.format(fmt, args)));
    }

    public void info(String msg, Throwable e) {
        LOG.info(errmsg(msg), e);
    }

    public void warn(String msg, Throwable e) {
        LOG.error(errmsg(msg), e);
    }

    public void warn(String fmt, Object... args) {
        LOG.warn(errmsg(String.format(fmt, args)));
    }

    public void error(String msg, Throwable e) {
        LOG.error(errmsg(msg), e);
    }

    private String errmsg(String s) {
        return String.format("Remote folder '%s': %s", getPath(), s);
    }
}
