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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.zimbra.cs.imap.ImapFlagCache.ImapFlag;
import com.zimbra.cs.imap.ImapMessage.ImapMessageSet;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.SearchFolder;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.service.mail.Search;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

class ImapFolder implements Iterable<ImapMessage> {
    private int      mFolderId;
    private ImapPath mPath;
    private boolean  mWritable;
    private String   mQuery;

    private List<ImapMessage>         mSequence   = new ArrayList<ImapMessage>();
    private Map<Integer, ImapMessage> mMessageIds = new HashMap<Integer, ImapMessage>();

    private int mUIDValidityValue;
    private int mInitialUIDNEXT = -1;
    private int mInitialFirstUnread = -1;
    private int mLastSize = 0;

    private Mailbox          mMailbox;
    private ImapSession      mSession;
    private boolean          mNotificationsSuspended;
    private Set<ImapMessage> mDirtyMessages = new TreeSet<ImapMessage>();
    private ImapMessageSet   mSavedSearchResults;

    /** Initializes an ImapFolder from a {@link Folder}, specified by path.
     *  Search folders are treated as folders containing all messages matching
     *  the search.
     * @param name     The target folder's path.
     * @param select   Whether the user wants to open the folder for writing.
     * @param session  The authenticated user's current IMAP session.
     * @see #loadVirtualFolder(SearchFolder, OperationContext) */
    ImapFolder(ImapPath path, boolean select, ImapSession session) throws ServiceException {
        boolean debug = ZimbraLog.imap.isDebugEnabled();
        if (debug)  ZimbraLog.imap.debug("  ** loading folder: " + path);

        mPath = path;

        OperationContext octxt = session.getContext();
        mMailbox = (Mailbox) path.getOwnerMailbox();
        Folder folder = mMailbox.getFolderByPath(octxt, mPath.asZimbraPath());
        if (!mPath.isSelectable())
            throw ServiceException.PERM_DENIED("cannot select folder: " + path);

        mFolderId = folder.getId();
        if (folder instanceof SearchFolder) {
            String types = ((SearchFolder) folder).getReturnTypes().toLowerCase();
            if (types.equals(""))
                types = Search.DEFAULT_SEARCH_TYPES;
            if (types.indexOf("conversation") != -1 || types.indexOf("message") != -1)
                mQuery = ((SearchFolder) folder).getQuery();
            else
                mQuery = "item:none";
        }
        mWritable = select && mPath.isWritable();
        mUIDValidityValue = getUIDValidity(folder);
        mInitialUIDNEXT = folder.getImapUIDNEXT();

        // fetch visible items from database
        List<ImapMessage> i4list;
        if (isVirtual())
            i4list = loadVirtualFolder(octxt, (SearchFolder) folder);
        else
            i4list = mMailbox.openImapFolder(octxt, folder.getId());
        Collections.sort(i4list);

        // check messages for imapUid <= 0 and assign new IMAP IDs if necessary
        List<ImapMessage> unnumbered = new ArrayList<ImapMessage>();
        List<Integer> renumber = new ArrayList<Integer>();
        while (!i4list.isEmpty() && i4list.get(0).imapUid <= 0) {
            ImapMessage i4msg = i4list.remove(0);
            unnumbered.add(i4msg);  renumber.add(i4msg.msgId);
        }
        if (!renumber.isEmpty()) {
            List<Integer> newIds = mMailbox.resetImapUid(octxt, renumber);
            for (int i = 0; i < newIds.size(); i++)
                unnumbered.get(i).imapUid = newIds.get(i);
            i4list.addAll(unnumbered);
        }

        // and create our lists and hashes of items
        StringBuilder added = debug ? new StringBuilder("  ** added: ") : null;
        for (ImapMessage i4msg : i4list) {
            cache(i4msg);
            if (mInitialFirstUnread == -1 && (i4msg.flags & Flag.BITMASK_UNREAD) != 0)
                mInitialFirstUnread = i4msg.sequence;
            if (debug)  added.append(' ').append(i4msg.msgId);
        }
        if (debug)  ZimbraLog.imap.debug(added);

        mLastSize = mSequence.size();
        mSession = session;
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
     * @param session  The authenticated user's current IMAP session.
     * @see #ImapFolder(String, boolean, ImapSession) */
    void reopen(boolean select, ImapSession session) throws ServiceException {
        if (isVirtual())
            throw ServiceException.INVALID_REQUEST("cannot reopen virtual folders", null);

        OperationContext octxt = session.getContext();
        Folder folder = mMailbox.getFolderByPath(octxt, mPath.asZimbraPath());
        if (!mPath.isSelectable())
            throw ServiceException.PERM_DENIED("cannot select folder: " + mPath);
        if (folder.getId() != mFolderId)
            throw ServiceException.INVALID_REQUEST("folder IDs do not match (was " + mFolderId + ", is " + folder.getId() + ')', null);

        mWritable = select && mPath.isWritable();
        mUIDValidityValue = getUIDValidity(folder);
        mInitialUIDNEXT = folder.getImapUIDNEXT();

        mNotificationsSuspended = false;
        mDirtyMessages.clear();
        collapseExpunged();

        mInitialFirstUnread = -1;
        for (ImapMessage i4msg : mSequence) {
            if (mInitialFirstUnread == -1 && (i4msg.flags & Flag.BITMASK_UNREAD) != 0)
                mInitialFirstUnread = i4msg.sequence;
            i4msg.setAdded(false);
        }

        mLastSize = mSequence.size();
        mSavedSearchResults = null;
        mSession = session;
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

    /** Returns an indicator for determining whether a folder has had items
     *  inserted since the last check.  This is <b>only</b> valid immediately
     *  after the folder is initialized and is not updated as messages are
     *  subsequently added.
     * @see Folder#getImapUIDNEXT() */
    int getInitialUIDNEXT()  { return mInitialUIDNEXT; }

    /** Returns the "sequence number" of the first unread message in the
     *  folder, or -1 if none are unread.  This is <b>only</b> valid
     *  immediately after the folder is initialized and is not updated
     *  as messages are marked read and unread. */
    int getFirstUnread()  { return mInitialFirstUnread; }

    /** Returns the active {@link ImapSession} in which this ImapFolder was
     *  created. */
    ImapSession getSession()  { return mSession; }

    /** Returns whether this folder is a "virtual" folder (i.e. a search
     *  folder).
     * @see #loadVirtualFolder(SearchFolder, Mailbox, OperationContext) */
    boolean isVirtual()   { return mQuery != null; }

    /** Returns whether this folder was opened for write. */
    boolean isWritable()  { return mWritable; }


    public Iterator<ImapMessage> iterator()  { return mSequence.iterator(); }


    ImapPath getPath()              { return mPath; }
    String getQuotedPath()          { return '"' + mPath.asZimbraPath() + '"'; }
    void updatePath(Folder folder)  { mPath = new ImapPath(null, folder.getPath(), mPath.getSession()); }


    /** Returns the UID Validity Value for the {@link Folder}.  This is the
     *  folder's <code>MOD_CONTENT</code> change sequence number.
     * @see Folder#getSavedSequence() */
    static int getUIDValidity(Folder folder)    { return Math.max(folder.getSavedSequence(), 1); }
    static int getUIDValidity(ZFolder zfolder)  { return zfolder.getContentSequence(); }


    /** Retrieves the index of the ImapMessage with the given IMAP UID in the
     *  folder's {@link #mSequence} message list.  This retrieval is done via
     *  binary search rather than direct lookup.
     * @return index of the search key, if it is contained in the list;
     *         otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
     *         <i>insertion point</i> is defined as the point at which the
     *         key would be inserted into the list: the index of the first
     *         element greater than the key, or <tt>list.size()</tt>, if all
     *         elements in the list are less than the specified key.  Note
     *         that this guarantees that the return value will be &gt;= 0 if
     *         and only if the key is found.
     * @see Collections#binarySearch(java.util.List, T) */
    private int uidSearch(int uid) {
        int low = 0, high = mSequence.size() - 1;
        while (low <= high) {
            int mid = (low + high) >> 1;
            int targetUid = mSequence.get(mid).imapUid;
            if (targetUid < uid)      low = mid + 1;
            else if (targetUid> uid)  high = mid - 1;
            else                      return mid;  // key found
        }
        return -(low + 1);  // key not found
    }

    /** Returns the ImapMessage with the given Zimbra item ID from the
     *  folder's {@link #mSequence} message list. */
    ImapMessage getById(int id)        { if (id <= 0) return null;   return checkRemoved(mMessageIds.get(new Integer(id))); }

    /** Returns the ImapMessage with the given IMAP UID from the folder's
     *  {@link #mSequence} message list. */
    ImapMessage getByImapId(int uid)   { if (uid <= 0) return null;  return getBySequence(uidSearch(uid) + 1); }

    /** Returns the ImapMessage with the given 1-based sequence number in the
     *  folder's {@link #mSequence} message list. */
    ImapMessage getBySequence(int seq) { return checkRemoved(seq > 0 && seq <= mSequence.size() ? (ImapMessage) mSequence.get(seq - 1) : null); }

    /** Returns the last ImapMessage in the folder's {@link #mSequence}
     *  message list.  This message corresponds to the "*" IMAP UID. */
    private ImapMessage getLastMessage()  { return getBySequence(mSequence.size()); }

    /** Returns the passed-in ImapMessage, or <coode>null</code> if the
     *  message has already been expunged.*/
    private ImapMessage checkRemoved(ImapMessage i4msg)  { return (i4msg == null || i4msg.isExpunged() ? null : i4msg); }


    ImapMessage cache(ImapMessage i4msg) {
        // provide the information missing from the DB search
        i4msg.sflags |= mFolderId == Mailbox.ID_FOLDER_SPAM ? (byte) (ImapMessage.FLAG_SPAM | ImapMessage.FLAG_JUNKRECORDED) : 0;
        // update the folder information
        mSequence.add(i4msg);
        setIndex(i4msg, mSequence.size());
        return i4msg;
    }

    void setIndex(ImapMessage i4msg, int position) {
        i4msg.sequence = position;
        mMessageIds.put(new Integer(i4msg.msgId), i4msg);
    }

    void uncache(ImapMessage i4msg) {
        mMessageIds.remove(new Integer(i4msg.msgId));
        mDirtyMessages.remove(i4msg);
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
        for (ImapMessage i4msg : mSequence) {
            if (i4msg != null && (i4msg.tags & mask) != 0) {
                mDirtyMessages.add(i4msg);
                if (removeTag)
                	i4msg.tags &= ~mask;
            }
        }
    }


    void saveSearchResults(ImapMessageSet i4set) {
        i4set.remove(null);
        mSavedSearchResults = i4set;
    }

    ImapMessageSet getSavedSearchResults() {
        if (mSavedSearchResults == null)
            mSavedSearchResults = new ImapMessageSet();
        return mSavedSearchResults;
    }

    void markMessageExpunged(ImapMessage i4msg) {
        if (mSavedSearchResults != null)
            mSavedSearchResults.remove(i4msg);
        i4msg.setExpunged(true);
    }


    ImapMessageSet getAllMessages() {
        ImapMessageSet result = new ImapMessageSet();
        if (!mSequence.isEmpty()) {
            for (ImapMessage i4msg : mSequence)
                if (i4msg != null)
                    result.add(i4msg);
        }
        return result;
    }

    ImapMessageSet getFlaggedMessages(ImapFlag i4flag) {
        ImapMessageSet result = new ImapMessageSet();
        if (i4flag != null && !mSequence.isEmpty()) {
            for (ImapMessage i4msg : mSequence)
                if (i4msg != null && i4flag.matches(i4msg))
                    result.add(i4msg);
        }
        return result;
    }


    private int parseId(String id) {
        // valid values will always be positive ints, so force it there...
        return (int) Math.max(-1, Math.min(Integer.MAX_VALUE, Long.parseLong(id)));
    }

    ImapMessageSet getSubsequence(String subseqStr, boolean byUID) {
        ImapMessageSet result = new ImapMessageSet();
        if (subseqStr == null || subseqStr.trim().equals("") || mSequence.isEmpty())
            return result;
        else if (subseqStr.equals("$"))
            return getSavedSearchResults();

        ImapMessage i4msg = getLastMessage();
        int lastID = (i4msg == null ? (byUID ? Integer.MAX_VALUE : mSequence.size()) : (byUID ? i4msg.imapUid : i4msg.sequence));

        for (String subset : subseqStr.split(",")) {
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
                } else {
                    int start = uidSearch(lower), end = uidSearch(upper);
                    if (start < 0)  start = -start - 1;
                    if (end < 0)    end = -end - 2;
                    for (int seq = start; seq <= end; seq++)
                        if ((i4msg = getBySequence(seq + 1)) != null)
                            result.add(i4msg);
                }
            }
        }

        return result;
    }

    static String encodeSubsequence(List<Integer> items) {
        if (items == null || items.isEmpty())
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

    static String encodeSubsequence(Collection<ImapMessage> items, boolean byUID) {
        if (items == null || items.isEmpty())
            return "";
        StringBuilder sb = new StringBuilder();
        int start = -1, last = -1;
        boolean done;
        Iterator<ImapMessage> it = items.iterator();
        do {
            done = !it.hasNext();
            int next = done ? -1 : (byUID ? it.next().imapUid : it.next().sequence);
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
