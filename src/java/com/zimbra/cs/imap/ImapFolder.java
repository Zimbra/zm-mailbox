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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Apr 30, 2005
 */
package com.zimbra.cs.imap;

import java.io.UnsupportedEncodingException;
import java.util.*;

import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mailbox.*;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.ZimbraLog;


class ImapFolder implements Iterable<ImapMessage> {
    private int     mFolderId;
    private String  mPath;
    private boolean mWritable;
    private String  mQuery;

    private List<ImapMessage>         mSequence = new ArrayList<ImapMessage>();
    private Map<Integer, ImapMessage> mUIDs     = new HashMap<Integer, ImapMessage>();

    private int mUIDValidityValue;
    private int mFirstUnread = -1;
    private int mLastSize    = 0;

    private boolean          mNotificationsSuspended;
    private Set<ImapMessage> mDirtyMessages = new TreeSet<ImapMessage>();

    /** Initializes an ImapFolder from a {@link Folder}, specified by path.
     *  Search folders are treated as folders containing all messages matching
     *  the search.
     * @param name     The target folder's path.
     * @param select   Whether the user wants to open the folder for writing.
     * @param mbox     The mailbox in which the target folder is found.
     * @param session  The authenticated user's current IMAP session.
     * @see #loadVirtualFolder(SearchFolder, Mailbox, OperationContext) */
    ImapFolder(String name, boolean select, Mailbox mbox, ImapSession session) throws ServiceException {
        name = importPath(name, session);

        boolean debug = ZimbraLog.imap.isDebugEnabled();
        if (debug)  ZimbraLog.imap.debug("  ** loading folder: " + name);

        OperationContext octxt = session.getContext();
        Folder folder = mbox.getFolderByPath(octxt, name);
        if (!isFolderSelectable(folder, session))
            throw ServiceException.PERM_DENIED("cannot select folder: /" + name);
        mPath = name;
        mFolderId = folder.getId();
        if (folder instanceof SearchFolder) {
            String types = ((SearchFolder) folder).getReturnTypes().toLowerCase();
            if (types.indexOf("conversation") != -1 || types.indexOf("message") != -1)
                mQuery = ((SearchFolder) folder).getQuery();
            else
                mQuery = "item:none";
        }
        mWritable = select && isFolderWritable(folder, session);
        mUIDValidityValue = getUIDValidity(folder);

        // fetch visible items from database
        List<ImapMessage> i4list;
        if (isVirtual())
            i4list = loadVirtualFolder(octxt, (SearchFolder) folder);
        else
            i4list = mbox.openImapFolder(octxt, folder.getId());
        Collections.sort(i4list);

        // check messages for imapUid <= 0 and assign new IMAP IDs if necessary
        List<ImapMessage> unnumbered = new ArrayList<ImapMessage>();
        List<Integer> renumber = new ArrayList<Integer>();
        while (!i4list.isEmpty() && i4list.get(0).imapUid <= 0) {
            ImapMessage i4msg = i4list.remove(0);
            unnumbered.add(i4msg);  renumber.add(i4msg.msgId);
        }
        if (!renumber.isEmpty()) {
            List<Integer> newIds = mbox.resetImapUid(octxt, renumber);
            for (int i = 0; i < newIds.size(); i++)
                unnumbered.get(i).imapUid = newIds.get(i);
            i4list.addAll(unnumbered);
        }

        // and create our lists and hashes of items
        StringBuilder added = debug ? new StringBuilder("  ** added: ") : null;
        for (ImapMessage i4msg : i4list) {
            cache(i4msg);
            if (mFirstUnread == -1 && (i4msg.flags & Flag.BITMASK_UNREAD) != 0)
                mFirstUnread = i4msg.sequence;
            if (debug)  added.append(' ').append(i4msg.msgId);
        }
        if (debug)  ZimbraLog.imap.debug(added);

        mLastSize = mSequence.size();
    }

    /** Fetches the messages contained within a search folder.  When a search
     *  folder is IMAP-visible, it appears in folder listings, is SELECTable
     *  READ-ONLY, and appears to have all matching messages as its contents.
     *  If it is not visible, it will be completely hidden from all IMAP
     *  commands.
     * @param octxt   Encapsulation of the authenticated user.
     * @param search  The search folder being exposed. */
    private List<ImapMessage> loadVirtualFolder(OperationContext octxt, SearchFolder search) throws ServiceException {
        Mailbox mbox = search.getMailbox();
        List<ImapMessage> i4list = new ArrayList<ImapMessage>();
        try {
            ZimbraQueryResults zqr = mbox.search(octxt, mQuery, ImapHandler.ITEM_TYPES, MailboxIndex.SortBy.DATE_ASCENDING, 1000);
            int i = 0, hitIds[] = new int[100];
            Arrays.fill(hitIds, Mailbox.ID_AUTO_INCREMENT);
            try {
                for (ZimbraHit hit = zqr.getFirstHit(); hit != null || i > 0; ) {
                    if (hit == null || i == 100) {
                        for (MailItem item : mbox.getItemById(octxt, hitIds, MailItem.TYPE_MESSAGE))
                            if (item != null)
                                i4list.add(new ImapMessage(item));
                        Arrays.fill(hitIds, Mailbox.ID_AUTO_INCREMENT);
                        i = 0;
                    }
                    if (hit != null) {
                        hitIds[i++] = hit.getItemId();
                        hit = zqr.getNext();
                    }
                }
            } finally {
                zqr.doneWithSearchResults();
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw ServiceException.FAILURE("failure opening searchfolder", e);
        }
        return i4list;
    }

    /** Reinitializes an ImapFolder for the purposes of EXAMINE/SELECTing the
     *  folder that's already EXAMINE/SELECTed.  Does not reload the contents
     *  of the folder from the Mailbox, but instead cleans up the existing data
     *  structures.  Cannot be called on virtual folders, which must be re-read
     *  manually.
     * @param select   Whether the user wants to open the folder for writing.
     * @param mbox     The mailbox in which the target folder is found.
     * @param session  The authenticated user's current IMAP session.
     * @see #ImapFolder(String, boolean, Mailbox, ImapSession) */
    void reopen(boolean select, Mailbox mbox, ImapSession session) throws ServiceException {
        if (isVirtual())
            throw ServiceException.INVALID_REQUEST("cannot reopen virtual folders", null);
        OperationContext octxt = session.getContext();
        Folder folder = mbox.getFolderByPath(octxt, mPath);
        if (!isFolderSelectable(folder, session))
            throw ServiceException.PERM_DENIED("cannot select folder: /" + mPath);
        if (folder.getId() != mFolderId)
            throw ServiceException.INVALID_REQUEST("folder IDs do not match (was " + mFolderId + ", is " + folder.getId() + ')', null);
        mWritable = select && isFolderWritable(folder, session);
        mUIDValidityValue = getUIDValidity(folder);

        mNotificationsSuspended = false;
        mDirtyMessages.clear();
        collapseExpunged();

        mFirstUnread = -1;
        for (ImapMessage i4msg : mSequence) {
            if (mFirstUnread == -1 && (i4msg.flags & Flag.BITMASK_UNREAD) != 0)
                mFirstUnread = i4msg.sequence;
            i4msg.setAdded(false);
            i4msg.setGhost(false);
        }

        mLastSize = mSequence.size();
    }

    /** Returns the selected folder's zimbra ID. */
    int getId()           { return mFolderId; }
    /** Returns the number of messages in the folder.  Messages that have been
     *  received or deleted since the client was last notified are still
     *  included in this count. */
    int getSize()         { return mSequence.size(); }
    /** Returns the search folder query associated with this IMAP folder, or
     *  <code>""</code> if the SELECTed folder is not a search folder. */
    String getQuery()     { return mQuery == null ? "" : mQuery; }
    /** Returns the folder's IMAP UID validity value.
     * @see #getUIDValidity(Folder) */
    int getUIDValidity()  { return mUIDValidityValue; }
    /** Returns the "sequence number" of the first unread message in the
     *  folder, or -1 if none are unread.  This is <b>only</b> valid
     *  immediately after the folder is initialized and is not updated
     *  as messages are marked read and unread. */
    int getFirstUnread()  { return mFirstUnread; }
    /** Returns whether this folder is a "virtual" folder (i.e. a search
     *  folder).
     * @see #loadVirtualFolder(SearchFolder, Mailbox, OperationContext) */
    boolean isVirtual()   { return mQuery != null; }
    /** Returns whether this folder was opened for write. */
    boolean isWritable()  { return mWritable; }

    public Iterator<ImapMessage> iterator()  { return mSequence.iterator(); }

    static boolean isFolderWritable(Folder folder, ImapSession session) {
        return isFolderSelectable(folder, session) && !(folder instanceof SearchFolder) &&
               folder.getDefaultView() != MailItem.TYPE_CONTACT;
    }
    static boolean isFolderSelectable(Folder folder, ImapSession session) {
        return isFolderVisible(folder, session) && !folder.isTagged(folder.getMailbox().mDeletedFlag);
    }
    static boolean isFolderVisible(Folder folder, ImapSession session) {
        if (session.isHackEnabled(ImapSession.EnabledHack.WM5)) {
            String lcname = folder.getPath().substring(1).toLowerCase();
            if (lcname.startsWith("sent items") && (lcname.length() == 10 || lcname.charAt(10) == '/'))
                return false;
        }
        return folder != null &&
               folder.getDefaultView() != MailItem.TYPE_APPOINTMENT &&
               folder.getDefaultView() != MailItem.TYPE_WIKI &&
               folder.getDefaultView() != MailItem.TYPE_DOCUMENT &&
               folder.getId() != Mailbox.ID_FOLDER_USER_ROOT &&
               (!(folder instanceof SearchFolder) || ((SearchFolder) folder).isImapVisible());
    }

    static boolean isPathCreatable(String path) {
        return path != null &&
               !path.toLowerCase().matches("\\s*notebook\\s*(/.*)?") &&
               !path.toLowerCase().matches("\\s*contacts\\s*(/.*)?") &&
               !path.toLowerCase().matches("\\s*calendar\\s*(/.*)?");
    }

    /** Formats a folder path as an IMAP-UTF-7 quoted-string.  Applies all
     *  special hack-specific path transforms.
     * @param path     The Zimbra-local folder pathname.
     * @param session  The authenticated user's current session.
     * @see #importPath(String, ImapSession) */
    static String exportPath(String path, ImapSession session) {
        if (path.startsWith("/") && path.length() > 1)
            path = path.substring(1);
        String lcname = path.toLowerCase();
        // make sure that the Inbox is called "INBOX", regardless of how we capitalize it
        if (lcname.startsWith("inbox") && (lcname.length() == 5 || lcname.charAt(5) == '/'))
            path = "INBOX" + path.substring(5);
        else if (session.isHackEnabled(ImapSession.EnabledHack.WM5))
            if (lcname.startsWith("sent") && (lcname.length() == 4 || lcname.charAt(4) == '/'))
                path = "Sent Items" + path.substring(4);
        return path;
    }

    static String formatPath(String path, ImapSession session) {
        path = exportPath(path, session);
        try {
            path = '"' + new String(path.getBytes("imap-utf-7"), "US-ASCII") + '"';
        } catch (UnsupportedEncodingException e) {
            path = '"' + path + '"';
        }
        return path.replaceAll("\\\\", "\\\\\\\\");
    }

    /** Takes a user-supplied IMAP mailbox path and converts it to a Zimbra
     *  folder pathname.  Applies all special, hack-specific folder mappings.
     *  Does <b>not</b> do IMAP-UTF-7 decoding; this is assumed to have been
     *  already done by the appropriate method in {@link ImapRequest}.
     * @param path     The client-provided logical IMAP pathname.
     * @param session  The authenticated user's current session.
     * @see #exportPath(String, ImapSession) */
    static String importPath(String path, ImapSession session) {
        if (path.startsWith("/") && path.length() > 1)
            path = path.substring(1);
        String lcname = path.toLowerCase();
        // Windows Mobile 5 hack: server must map "Sent Items" to "Sent"
        if (session.isHackEnabled(ImapSession.EnabledHack.WM5))
            if (lcname.startsWith("sent items") && (lcname.length() == 10 || lcname.charAt(10) == '/'))
                path = "Sent" + path.substring(10);
        return path;
    }

    String getPath()                { return mPath; }
    String getQuotedPath()          { return '"' + mPath + '"'; }
    void updatePath(Folder folder)  { mPath = folder.getPath(); }

    static int getUIDValidity(Folder folder) { return Math.max(folder.getSavedSequence(), 1); }

    ImapMessage getById(int id)        { if (id <= 0) return null;   return checkRemoved(mUIDs.get(new Integer(-id))); }
    ImapMessage getByImapId(int uid)   { if (uid <= 0) return null;  return checkRemoved(mUIDs.get(new Integer(uid))); }
    ImapMessage getBySequence(int seq) { return checkRemoved(seq > 0 && seq <= mSequence.size() ? (ImapMessage) mSequence.get(seq - 1) : null); }
    ImapMessage getLastMessage()       { return getBySequence(mSequence.size()); }
    private ImapMessage checkRemoved(ImapMessage i4msg)  { return (i4msg == null || i4msg.isExpunged() ? null : i4msg); }

    ImapMessage cache(ImapMessage i4msg) {
        // provide the information missing from the DB search
        i4msg.sflags |= mFolderId == Mailbox.ID_FOLDER_SPAM ? (byte) (ImapMessage.FLAG_SPAM | ImapMessage.FLAG_JUNKRECORDED) : 0;
        i4msg.parent = this;
        // update the folder information
        mSequence.add(i4msg);
        setIndex(i4msg, mSequence.size());
        return i4msg;
    }
    void setIndex(ImapMessage i4msg, int position) {
        i4msg.sequence = position;
        mUIDs.put(new Integer(i4msg.imapUid), i4msg);
        mUIDs.put(new Integer(-i4msg.msgId), i4msg);
    }
    void uncache(ImapMessage i4msg) {
        mUIDs.remove(new Integer(i4msg.imapUid));
        mUIDs.remove(new Integer(-i4msg.msgId));
        mDirtyMessages.remove(i4msg);
    }
    void unghost(ImapMessage i4msg, int msgId) {
        if (i4msg.isGhost()) {
            mUIDs.remove(new Integer(-i4msg.msgId));
            i4msg.msgId = msgId;
            mUIDs.put(new Integer(-i4msg.msgId), i4msg);
            i4msg.setGhost(false);
        }
    }
    
    boolean checkpointSize()  { int last = mLastSize;  return last != (mLastSize = getSize()); }

    void disableNotifications()  { mNotificationsSuspended = true; }
    void enableNotifications()   { mNotificationsSuspended = false; }

    void dirtyMessage(ImapMessage i4msg) {
        if (!mNotificationsSuspended && i4msg == getBySequence(i4msg.sequence))
            mDirtyMessages.add(i4msg);
    }
    void undirtyMessage(ImapMessage i4msg) {
        if (mDirtyMessages.remove(i4msg))
            i4msg.setAdded(false);
    }
    Iterator<ImapMessage> dirtyIterator()  { return mDirtyMessages.iterator(); }
    void clearDirty()                      { mDirtyMessages.clear(); }

    void dirtyTag(int id) {
        dirtyTag(id, false);
    }
    void dirtyTag(int id, boolean removeTag) {
        long mask = 1L << Tag.getIndex(id);
        for (ImapMessage i4msg : mSequence)
            if (i4msg != null && (i4msg.tags & mask) != 0) {
                mDirtyMessages.add(i4msg);
                if (removeTag)
                	i4msg.tags &= ~mask;
            }
    }

    private static class NullableComparator implements Comparator<ImapMessage> {
        public int compare(ImapMessage o1, ImapMessage o2) {
            if (o1 == null)       return o2 == null ? 0 : -1;
            else if (o2 == null)  return 1;
            return (o1.sequence < o2.sequence ? -1 : (o1.sequence == o2.sequence ? 0 : 1));
        }
    }

    Set<ImapMessage> getFlaggedMessages(ImapSession.ImapFlag i4flag) {
        if (i4flag == null || mSequence.size() == 0)
            return Collections.emptySet();
        TreeSet<ImapMessage> result = new TreeSet<ImapMessage>(new NullableComparator());
        for (ImapMessage i4msg : mSequence)
            if (i4msg != null && (i4msg.sflags & i4flag.mBitmask) != 0)
                result.add(i4msg);
        return result;
    }

    private int parseId(String id) {
        // valid values will always be positive ints, so force it there...
        return (int) Math.max(-1, Math.min(Integer.MAX_VALUE, Long.parseLong(id)));
    }
    Set<ImapMessage> getSubsequence(String subseqStr, boolean byUID) {
        if (subseqStr == null || subseqStr.trim().equals("") || mSequence.size() == 0)
            return Collections.emptySet();
        ImapMessage i4msg = getLastMessage();
        int lastID = (i4msg == null ? (byUID ? Integer.MAX_VALUE : mSequence.size()) : (byUID ? i4msg.imapUid : i4msg.sequence));

        TreeSet<ImapMessage> result = new TreeSet<ImapMessage>(new NullableComparator());
        for (String subset : subseqStr.split(","))
            if (subset.indexOf(':') == -1) {
                // single message -- get it and add it (may be null)
                int id = (subset.equals("*") ? lastID : parseId(subset));
                result.add(byUID ? getByImapId(id) : getBySequence(id));
            } else {
                // colon-delimited range -- get them and add them (may be null)
                String[] range = subset.split(":", 2);
                int lower = (range[0].equals("*") ? lastID : parseId(range[0]));
                int upper = (range[1].equals("*") ? lastID : parseId(range[1]));
                if (lower > upper)  { int tmp = upper; upper = lower; lower = tmp; }
                if (!byUID) {
                    upper = Math.min(lastID, upper);
                    for (int seq = Math.max(0, lower); seq <= upper; seq++)
                        result.add(getBySequence(seq));
                } else
                    for (int seq = 0; seq < mSequence.size(); seq++)
                        if ((i4msg = getBySequence(seq + 1)) != null && i4msg.imapUid >= lower && i4msg.imapUid <= upper)
                            result.add(i4msg);
                        else if (i4msg != null && i4msg.imapUid > upper)
                            break;
            }

        return result;
    }
    static String encodeSubsequence(List<Integer> items) {
        if (items == null || items.size() == 0)
            return "";
        StringBuilder sb = new StringBuilder();
        int start = -1, last = -1;
        boolean done;
        Iterator<Integer> it = items.iterator();
        do {
            done = !it.hasNext();
            int next = done ? -1 : it.next();
            if (last == -1) {
                last = start = next;
            } else if (done || next != last + 1) {
                if (sb.length() > 0)
                    sb.append(',');
                sb.append(start);
                if (start != last)
                    sb.append(':').append(last);
                last = start = next;
            } else {
                last = next;
            }
        } while (!done);
        return sb.toString();
    }

    List<Integer> collapseExpunged() {
        // FIXME: need synchronization
        boolean debug = ZimbraLog.imap.isDebugEnabled();
        List<Integer> removed = new ArrayList<Integer>();
        boolean trimmed = false;
        int seq = 1;
        if (debug)  ZimbraLog.imap.debug("  ** iterating (collapseExpunged)");
        for (ListIterator lit = mSequence.listIterator(); lit.hasNext(); seq++) {
            ImapMessage i4msg = (ImapMessage) lit.next();
            if (i4msg.isExpunged()) {
                if (debug)  ZimbraLog.imap.debug("  ** removing: " + i4msg.msgId);
                // uncache() removes pointers to the message from mUIDs;
                //   if the message appears again in mSequence, it *must* be later
                //   and the subsequent call to setIndex() will correctly set the mUIDs mapping
                uncache(i4msg);
                lit.remove();
                removed.add(seq--);
                trimmed = true;
            } else if (trimmed)
                setIndex(i4msg, seq);
        }
        return removed;
    }
}
