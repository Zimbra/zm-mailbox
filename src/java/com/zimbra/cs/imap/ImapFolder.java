/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
import java.util.TreeMap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ArrayUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.imap.ImapFlagCache.ImapFlag;
import com.zimbra.cs.imap.ImapMessage.ImapMessageSet;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.SearchFolder;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.service.mail.Search;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.zclient.ZFolder;

public class ImapFolder implements Iterable<ImapMessage>, ImapSession.ImapFolderData, java.io.Serializable {
    private static final long serialVersionUID = -7279453727601658427L;

    static final byte SELECT_READONLY  = 0x01;
    static final byte SELECT_CONDSTORE = 0x02;

    // attributes of the folder itself, irrespective of the session state
    private transient Mailbox mMailbox;
    private transient ImapSession mSession;
    private transient ImapPath mPath;
    private final int mFolderId;
    private int mUIDValidityValue;

    private String mQuery;
    private byte[] mTypeConstraint = ImapHandler.ITEM_TYPES;
    
    private List<ImapMessage>                   mSequence;
    private transient Map<Integer, ImapMessage> mMessageIds;

    private transient ImapFlagCache mFlags;
    private ImapFlagCache mTags;   // operationally could be "transient", but that makes deserialization replay depend on magic

    // below this point are session-specific attributes of the folder SELECT state
    static class SessionData {
        ImapCredentials mCredentials;

        boolean mWritable;

        int mLastSize;     // for EXISTS notifications
        int mRecentCount;  // for RECENT notifications
        int mExpungedCount;

        boolean mTagsAreDirty;
        boolean mNotificationsSuspended;
        ImapMessageSet mSavedSearchResults;
        Map<Integer, DirtyMessage> mDirtyMessages = new TreeMap<Integer, DirtyMessage>();

        SessionData(ImapPath path, byte params, ImapHandler handler) throws ServiceException {
            mCredentials = handler.getCredentials();
            mWritable = (params & SELECT_READONLY) == 0 && path.isWritable();
        }
        boolean hasNotifications() {
            return mTagsAreDirty || !mDirtyMessages.isEmpty() || mExpungedCount > 0;
        }
    }
    private transient SessionData mSessionData;


    /** Initializes an empty ImapFolder from a {@link Folder}, specified by
     *  path.
     * @param name     The target folder's path.
     * @param params   Optional SELECT parameters (e.g. READONLY).
     * @param handler  The authenticated user's current IMAP session. */
    ImapFolder(ImapPath path, byte params, ImapHandler handler) throws ServiceException {
        mPath = path;
        Folder folder = (Folder) path.getFolder();
        mFolderId = folder.getId();
        // FIXME: Folder object may be stale since it's cached in ImapPath
        mUIDValidityValue = getUIDValidity(folder);
        if (folder instanceof SearchFolder) {
            mQuery = ((SearchFolder) folder).getQuery();
            mTypeConstraint = getTypeConstraint((SearchFolder) folder);
        }

        if (handler != null)
            mSessionData = new SessionData(path, params, handler);

        Mailbox mbox = mMailbox = folder.getMailbox();
        mFlags = ImapFlagCache.getSystemFlags(mbox);
        mTags = new ImapFlagCache(mbox, null);

        mSequence = new ArrayList<ImapMessage>();
    }

    void setInitialSize() {
        SessionData sdata = mSessionData;
        if (sdata != null)
            sdata.mLastSize = mSequence.size();
    }


    public void doEncodeState(Element imap) {
        SessionData sdata = mSessionData;
        if (sdata != null) {
            ImapCredentials.EnabledHack[] hacks = sdata.mCredentials.getEnabledHacks();
            imap.addAttribute("hack", hacks == null ? null : Arrays.toString(hacks));
            imap.addAttribute("writable", isWritable());
            imap.addAttribute("dirty", sdata.mDirtyMessages.size()).addAttribute("expunged", sdata.mExpungedCount);
        }

        if (mSequence != null)
            imap.addAttribute("size", getSize());
        imap.addAttribute("folder", mPath.asImapPath()).addAttribute("query", mQuery);
    }

    void setSession(ImapSession session) {
        assert(mSession == null || mSession == session || mSessionData == null);
        mSession = session;
    }

    SessionData getSessionData() {
        return mSessionData;
    }

    public void endSelect() {
        mSessionData = null;
    }

    /** Returns the selected folder's containing {@link Mailbox}. */
    public Mailbox getMailbox() {
        return mMailbox;
    }

    /** Returns the {@link ImapCredentials} with which this ImapFolder was
     *  created. */
    ImapCredentials getCredentials() {
        SessionData sdata = mSessionData;
        return sdata == null ? null : sdata.mCredentials;
    }

    /** Returns the selected folder's zimbra ID. */
    public int getId() {
        return mFolderId;
    }

    /** Returns the number of messages in the folder.  Messages that have been
     *  received or deleted since the client was last notified are still
     *  included in this count. */
    public int getSize() {
        return mSequence == null ? 0 : mSequence.size();
    }

    /** Returns the number of messages in the folder that are considered
     *  \Recent.  These are messages that have been deposited in the folder
     *  since the last IMAP session that opened the folder. */
    int getRecentCount() {
        SessionData sdata = mSessionData;
        return isVirtual()  || sdata == null ? 0 : sdata.mRecentCount;
    }

    public boolean hasExpunges() {
        SessionData sdata = mSessionData;
        return sdata != null && sdata.mExpungedCount > 0;
    }

    public boolean hasNotifications() {
        SessionData sdata = mSessionData;
        return sdata != null && sdata.hasNotifications();
    }

    /** Returns the search folder query associated with this IMAP folder, or
     *  <tt>""</tt> if the SELECTed folder is not a search folder. */
    String getQuery() {
        return mQuery == null ? "" : mQuery;
    }

    static byte[] getTypeConstraint(SearchFolder search) {
        // constrain the search to the actually-requested types
        List<Byte> types = new ArrayList<Byte>(1);
        String typestr = search.getReturnTypes().toLowerCase();
        if (typestr.equals(""))
            typestr = Search.DEFAULT_SEARCH_TYPES;
        for (String type : typestr.split("\\s+,\\s+")) {
            if (type.equals(MailboxIndex.SEARCH_FOR_CONVERSATIONS) || type.equals(MailboxIndex.SEARCH_FOR_MESSAGES))
                types.add(MailItem.TYPE_MESSAGE);
            else if (type.equals(MailboxIndex.SEARCH_FOR_CONTACTS))
                types.add(MailItem.TYPE_CONTACT);
            else if (type.equals(MailboxIndex.SEARCH_FOR_CHATS))
                types.add(MailItem.TYPE_CHAT);
        }
        return ArrayUtil.toByteArray(types);
    }

    /** Returns the types of items exposed in this IMAP folder.  Defaults to
     *  {@link ImapHandler#ITEM_TYPES} except for search folders. */
    byte[] getTypeConstraint() {
        return mTypeConstraint;
    }

    /** Returns the folder's IMAP UID validity value.
     * @see #getUIDValidity(Folder) */
    int getUIDValidity() {
        return mUIDValidityValue;
    }

    int getCurrentMODSEQ() throws ServiceException {
        return mMailbox.getFolderById(null, mFolderId).getImapMODSEQ();
    }

    /** Returns whether this folder is a "virtual" folder (i.e. a search
     *  folder).
     * @see #loadVirtualFolder(SearchFolder, Mailbox, OperationContext) */
    boolean isVirtual() {
        return mQuery != null;
    }

    /** Returns whether this folder was opened for write. */
    public boolean isWritable() {
        SessionData sdata = mSessionData;
        return sdata == null ? false : sdata.mWritable;
    }


    public Iterator<ImapMessage> iterator() {
        return mSequence.iterator();
    }


    ImapPath getPath() {
        return mPath;
    }

    void updatePath(Folder folder) {
        mPath = new ImapPath(null, folder.getPath(), mPath.getCredentials());
    }

    String getQuotedPath() throws ServiceException {
        return '"' + mPath.asResolvedPath() + '"';
    }

    @Override public String toString() {
        return mPath.toString();
    }


    /** Returns the UID Validity Value for the {@link Folder}.  This is the
     *  folder's <tt>MOD_CONTENT</tt> change sequence number.
     * @see Folder#getSavedSequence() */
    static int getUIDValidity(Folder folder) {
        return Math.max(folder.getSavedSequence(), 1);
    }

    static int getUIDValidity(ZFolder zfolder) {
        return zfolder.getContentSequence();
    }


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
     * @see Collections#binarySearch(List, Object) */
    private int uidSearch(int uid) {
        int low = 0, high = getSize() - 1;
        while (low <= high) {
            int mid = (low + high) >> 1;
            int targetUid = mSequence.get(mid).imapUid;
            if (targetUid < uid)       low = mid + 1;
            else if (targetUid > uid)  high = mid - 1;
            else                       return mid;  // key found
        }
        return -(low + 1);  // key not found
    }

    /** Returns the ImapMessage with the given Zimbra item ID from the
     *  folder's {@link #mSequence} message list. */
    ImapMessage getById(int id) {
        if (id <= 0 || getSize() == 0)
            return null;

        // leverage the fact that by default, the message's item id and its IMAP uid are identical
        int sequence = uidSearch(id);
        if (sequence >= 0 && sequence < mSequence.size()) {
            ImapMessage i4msg = mSequence.get(sequence);
            // slightly tricky: must check if message is expunged in order to catch the case of
            //   using the web client to move the message out of the folder and back in before
            //   the IMAP server can tell the client about the EXPUNGE from the first move
            if (i4msg != null && i4msg.msgId == id && !i4msg.isExpunged())
                return checkRemoved(i4msg);
        }

        // if item id and IMAP uid differ, the message goes in the "mMessageIds" map
        if (mMessageIds == null) {
            // lookup miss means we need to generate the item-id-to-imap-message mapping
            mMessageIds = new HashMap<Integer, ImapMessage>();
            for (ImapMessage i4msg : mSequence) {
                if (i4msg != null && i4msg.msgId != i4msg.imapUid)
                    mMessageIds.put(i4msg.msgId, i4msg);
            }
        }
        return checkRemoved(mMessageIds.get(new Integer(id)));
    }

    /** Returns the ImapMessage with the given IMAP UID from the folder's
     *  {@link #mSequence} message list. */
    ImapMessage getByImapId(int uid) {
        return uid > 0 ? getBySequence(uidSearch(uid) + 1) : null;
    }

    /** Returns the ImapMessage with the given 1-based sequence number in the
     *  folder's {@link #mSequence} message list. */
    ImapMessage getBySequence(int seq) {
        return checkRemoved(seq > 0 && seq <= getSize() ? (ImapMessage) mSequence.get(seq - 1) : null);
    }

    /** Returns the last ImapMessage in the folder's {@link #mSequence}
     *  message list.  This message corresponds to the "*" IMAP UID. */
    private ImapMessage getLastMessage() {
        return getBySequence(getSize());
    }

    /** Returns the passed-in ImapMessage, or <tt>null</tt> if the message has
     *  already been expunged.*/
    private ImapMessage checkRemoved(ImapMessage i4msg) {
        return (i4msg == null || i4msg.isExpunged() ? null : i4msg);
    }


    /** Adds the message to the folder.  Messages <b>must</b> be added in
     *  increasing IMAP UID order.  Added messages are appended to the end of
     *  the folder's {@link #mSequence} message list and inserted into the
     *  {@link #mMessageIds} hash (if the latter hash has been instantiated).
     * @return the passed-in ImapMessage. */
    ImapMessage cache(ImapMessage i4msg, boolean recent) {
        if (mSequence == null)
            return null;
        // provide the information missing from the DB search
        if (mFolderId == Mailbox.ID_FOLDER_SPAM)
            i4msg.sflags |= ImapMessage.FLAG_SPAM | ImapMessage.FLAG_JUNKRECORDED;
        if (recent) {
            i4msg.sflags |= ImapMessage.FLAG_RECENT;
            SessionData sdata = mSessionData;
            if (sdata != null)
                sdata.mRecentCount++;
        }
        // update the folder information
        mSequence.add(i4msg);
        setIndex(i4msg, mSequence.size());
        return i4msg;
    }

    private void setIndex(ImapMessage i4msg, int position) {
        i4msg.sequence = position;
        if (mMessageIds != null) {
            if (i4msg.msgId != i4msg.imapUid)
                mMessageIds.put(new Integer(i4msg.msgId), i4msg);
            else
                mMessageIds.remove(new Integer(i4msg.msgId));
        }
    }

    /** Cleans up all references to an ImapMessage from all the folder's data
     *  structures other than {@link #mSequence}.  The <tt>mSequence</tt>
     *  cleanup must be done separately. */
    private void uncache(ImapMessage i4msg) {
        if (mMessageIds != null)
            mMessageIds.remove(new Integer(i4msg.msgId));

        SessionData sdata = mSessionData;
        if (sdata != null) {
            sdata.mDirtyMessages.remove(new Integer(i4msg.imapUid));
            if ((i4msg.sflags & ImapMessage.FLAG_RECENT) != 0)
                sdata.mRecentCount--;
            if ((i4msg.sflags & ImapMessage.FLAG_EXPUNGED) != 0)
                sdata.mExpungedCount--;
        }
    }


    boolean areTagsDirty() {
        SessionData sdata = mSessionData;
        return sdata == null ? false : sdata.mTagsAreDirty;
    }

    void cleanTags() {
        SessionData sdata = mSessionData;
        if (sdata != null)
            sdata.mTagsAreDirty = false;
    }

    ImapFlag cacheTag(Tag ltag) {
        assert(!(ltag instanceof Flag));
        if (ltag instanceof Flag)
            return null;
        SessionData sdata = mSessionData;
        if (sdata != null)
            sdata.mTagsAreDirty = true;
        return mTags.cache(new ImapFlag(ltag.getName(), ltag, true));
    }

    void dirtyTag(int id, int modseq) {
        dirtyTag(id, modseq, false);
    }

    void dirtyTag(int id, int modseq, boolean removeTag) {
        SessionData sdata = mSessionData;
        if (sdata != null)
            sdata.mTagsAreDirty = true;
        if (getSize() == 0)
            return;
        long mask = 1L << Tag.getIndex(id);
        for (ImapMessage i4msg : mSequence) {
            if (i4msg != null && (i4msg.tags & mask) != 0) {
                dirtyMessage(i4msg, modseq);
                if (removeTag)
                    i4msg.tags &= ~mask;
            }
        }
    }

    ImapFlag getFlagByName(String name) {
        ImapFlag i4flag = mFlags.getByName(name);
        return (i4flag != null ? i4flag : mTags.getByName(name));
    }

    ImapFlag getTagByMask(long mask) {
        return mTags.getByMask(mask);
    }

    List<String> getFlagList(boolean permanentOnly) {
        List<String> names = mFlags.listNames(permanentOnly);
        for (String tagname : mTags.listNames(permanentOnly))
            if (mFlags.getByName(tagname) == null)
                names.add(tagname);
        return names;
    }

    ImapFlagCache getTagset() {
        return mTags;
    }

    void clearTagCache() {
        mTags.clear();
    }


    static final class DirtyMessage {
        ImapMessage i4msg;
        int modseq;
        DirtyMessage(ImapMessage m, int s)  { i4msg = m;  modseq = s; }
    }

    boolean isMessageDirty(ImapMessage i4msg) {
        SessionData sdata = mSessionData;
        return sdata == null ? false : sdata.mDirtyMessages.containsKey(i4msg.imapUid);
    }

    void dirtyMessage(ImapMessage i4msg, int modseq) {
        SessionData sdata = mSessionData;
        if (sdata == null)
            return;

        if (sdata.mNotificationsSuspended || i4msg != getBySequence(i4msg.sequence))
            return;

        DirtyMessage dirty = sdata.mDirtyMessages.get(i4msg.imapUid);
        if (dirty == null)
            sdata.mDirtyMessages.put(i4msg.imapUid, new DirtyMessage(i4msg, modseq));
        else if (modseq > dirty.modseq)
            dirty.modseq = modseq;
    }

    DirtyMessage undirtyMessage(ImapMessage i4msg) {
        SessionData sdata = mSessionData;
        if (sdata == null)
            return null;

        DirtyMessage dirty = sdata.mDirtyMessages.remove(i4msg.imapUid);
        if (dirty != null)
            dirty.i4msg.setAdded(false);
        return dirty;
    }

    Iterator<DirtyMessage> dirtyIterator() {
        SessionData sdata = mSessionData;
        return (sdata == null ? new ArrayList<DirtyMessage>(0) : sdata.mDirtyMessages.values()).iterator();
    }

    /** Empties the folder's list of modified/created messages. */
    void clearDirty()  {
        SessionData sdata = mSessionData;
        if (sdata != null)
            sdata.mDirtyMessages.clear();
    }


    boolean checkpointSize() {
        SessionData sdata = mSessionData;
        if (sdata == null)
            return false;

        int last = sdata.mLastSize;
        return last != (sdata.mLastSize = getSize());
    }

    void disableNotifications() {
        SessionData sdata = mSessionData;
        if (sdata != null)
            sdata.mNotificationsSuspended = true;
    }
    
    void enableNotifications() {
        SessionData sdata = mSessionData;
        if (sdata != null)
            sdata.mNotificationsSuspended = false;
    }


    void saveSearchResults(ImapMessageSet i4set) {
        SessionData sdata = mSessionData;
        if (sdata != null) {
            i4set.remove(null);
            sdata.mSavedSearchResults = i4set;
        }
    }

    ImapMessageSet getSavedSearchResults() {
        SessionData sdata = mSessionData;
        if (sdata == null)
            return new ImapMessageSet();

        if (sdata.mSavedSearchResults == null)
            sdata.mSavedSearchResults = new ImapMessageSet();
        return sdata.mSavedSearchResults;
    }

    void markMessageExpunged(ImapMessage i4msg) {
        if (i4msg.isExpunged())
            return;

        i4msg.setExpunged(true);

        SessionData sdata = mSessionData;
        if (sdata != null) {
            if (sdata.mSavedSearchResults != null)
                sdata.mSavedSearchResults.remove(i4msg);
            sdata.mExpungedCount++;
        }
    }


    ImapMessageSet getAllMessages() {
        ImapMessageSet result = new ImapMessageSet();
        if (getSize() > 0) {
            for (ImapMessage i4msg : mSequence) {
                if (i4msg != null)
                    result.add(i4msg);
            }
        }
        return result;
    }

    ImapMessageSet getFlaggedMessages(ImapFlag i4flag) {
        ImapMessageSet result = new ImapMessageSet();
        if (i4flag != null && getSize() > 0) {
            for (ImapMessage i4msg : mSequence) {
                if (i4msg != null && i4flag.matches(i4msg))
                    result.add(i4msg);
            }
        }
        return result;
    }


    private static int parseId(String id) {
        // valid values will always be positive ints, so force it there...
        try {
            return (int) Math.max(-1, Math.min(Integer.MAX_VALUE, Long.parseLong(id)));
        } catch (NumberFormatException nfe) {
            return Integer.MAX_VALUE;
        }
    }

    private List<Pair<Integer, Integer>> normalizeSubsequence(String subseqStr, boolean byUID) {
        if (subseqStr == null || subseqStr.trim().equals(""))
            return Collections.emptyList();

        ImapMessage i4msg = getLastMessage();
        int lastID = (i4msg == null ? (byUID ? Integer.MAX_VALUE : getSize()) : (byUID ? i4msg.imapUid : i4msg.sequence));

        List<Pair<Integer, Integer>> normalized = new ArrayList<Pair<Integer, Integer>>(5);
        for (String subset : subseqStr.split(",")) {
            int lower, upper;
            if (subset.indexOf(':') == -1) {
                // single item
                lower = upper = (subset.equals("*") ? lastID : parseId(subset));
            } else {
                // colon-delimited range
                String[] range = subset.split(":", 2);
                lower = (range[0].equals("*") ? lastID : parseId(range[0]));
                upper = (range[1].equals("*") ? lastID : parseId(range[1]));
                if (lower > upper)  { int tmp = upper; upper = lower; lower = tmp; }
            }

            // add to list, merging with existing ranges if needed
            int insertpos = 0;
            for (int i = 0; i < normalized.size(); i++) {
                Pair<Integer, Integer> range = normalized.get(i);
                int lrange = range.getFirst(), urange = range.getSecond();
                if (lower > urange + 1) {
                    insertpos++;  continue;
                } else if (upper < lrange - 1) {
                    break;
                } else {
                    normalized.remove(i--);
                    lower = Math.min(lower, lrange);  upper = Math.max(upper, urange);
                }
            }
            normalized.add(insertpos, new Pair<Integer, Integer>(lower, upper));
        }
        return normalized;
    }

    ImapMessageSet getSubsequence(String tag, String subseqStr, boolean byUID) throws ImapParseException {
        return getSubsequence(tag, subseqStr, byUID, false);
    }

    ImapMessageSet getSubsequence(String tag, String subseqStr, boolean byUID, boolean isSEARCH) throws ImapParseException {
        ImapMessageSet result = new ImapMessageSet();
        if (subseqStr == null || subseqStr.trim().equals(""))
            return result;
        else if (subseqStr.equals("$"))
            return getSavedSearchResults();

        for (Pair<Integer, Integer> range : normalizeSubsequence(subseqStr, byUID)) {
            int lower = range.getFirst(), upper = range.getSecond();
            if (!byUID && (lower < 1 || upper > getSize()) && !isSEARCH) {
                // 9: "The server should respond with a tagged BAD response to a command that uses a message
                //     sequence number greater than the number of messages in the selected mailbox.  This
                //     includes "*" if the selected mailbox is empty."
                throw new ImapParseException(tag, "invalid message sequence number: " + subseqStr);
            } else if (lower == upper) {
                // single message -- get it and add it (may be null)
                result.add(byUID ? getByImapId(lower) : getBySequence(lower));
            } else {
                // range of messages -- get them and add them (may be null)
                if (!byUID) {
                    upper = Math.min(getSize(), upper);
                    for (int seq = Math.max(0, lower); seq <= upper; seq++)
                        result.add(getBySequence(seq));
                } else {
                    ImapMessage i4msg;
                    int start = uidSearch(lower), end = uidSearch(upper);
                    if (start < 0)  start = -start - 1;
                    if (end < 0)    end = -end - 2;
                    for (int seq = start; seq <= end; seq++) {
                        if ((i4msg = getBySequence(seq + 1)) != null)
                            result.add(i4msg);
                    }
                }
            }
        }

        return result;
    }

    String cropSubsequence(String subseqStr, boolean byUID, int croplow, int crophigh) {
        if (croplow <= 0 && crophigh <= 0)
            return subseqStr;
        StringBuilder sb = new StringBuilder(subseqStr.length());
        for (Pair<Integer, Integer> range : normalizeSubsequence(subseqStr, byUID)) {
            int lower = range.getFirst(), upper = range.getSecond();
            if (croplow > 0 && upper < croplow)
                continue;
            if (crophigh > 0 && lower > crophigh)
                continue;
            if (croplow > 0)
                lower = Math.max(lower, croplow);
            if (crophigh > 0)
                upper = Math.min(upper, crophigh);
            sb.append(sb.length() == 0 ? "" : ",").append(lower).append(lower == upper ? "" : ":" + upper);
        }
        return sb.toString();
    }

    String invertSubsequence(String subseqStr, boolean byUID, Set<ImapMessage> i4set) {
        StringBuilder sb = new StringBuilder();

        Iterator<ImapMessage> i4it = i4set.iterator();
        Iterator<Pair<Integer, Integer>> itrange = normalizeSubsequence(subseqStr, byUID).iterator();
        if (!itrange.hasNext())
            return subseqStr;

        Pair<Integer, Integer> range = itrange.next();
        int lower = range.getFirst(), upper = range.getSecond();
        int id = !i4it.hasNext() ? -1 : (byUID ? i4it.next().imapUid : i4it.next().sequence);

        while (lower != -1) {
            if (lower > upper) {
                // no valid values remaining in this range, so go to the next one
                if (!itrange.hasNext())  break;
                range = itrange.next();  lower = range.getFirst();  upper = range.getSecond();
            } else if (id == -1 || id > upper) {
                // the remainder of the range qualifies, so serialize it and go to the next range
                sb.append(sb.length() == 0 ? "" : ",").append(lower).append(lower == upper ? "" : ":" + upper);
                if (!itrange.hasNext())  break;
                range = itrange.next();  lower = range.getFirst();  upper = range.getSecond();
            } else if (id <= lower) {
                // the current ID is too low for this range, so fetch the next ID
                if (id == lower)  lower++;
                id = !i4it.hasNext() ? -1 : (byUID ? i4it.next().imapUid : i4it.next().sequence);
            } else {
                // the current ID lies within this range, so serialize part and fetch the next ID
                sb.append(sb.length() == 0 ? "" : ",").append(lower).append(lower == id - 1 ? "" : ":" + (id - 1));
                lower = id + 1;
                id = !i4it.hasNext() ? -1 : (byUID ? i4it.next().imapUid : i4it.next().sequence);
            }
        }
        return sb.toString();
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


    List<Integer> collapseExpunged(boolean byUID) {
        if (getSize() == 0)
            return Collections.emptyList();

        boolean debug = ZimbraLog.imap.isDebugEnabled();
        if (debug)  ZimbraLog.imap.debug("  ** iterating (collapseExpunged)");

        // FIXME: need synchronization
        boolean trimmed = false;
        int seq = 1;
        List<Integer> removed = new ArrayList<Integer>();
        for (ListIterator<ImapMessage> lit = mSequence.listIterator(); lit.hasNext(); seq++) {
            ImapMessage i4msg = lit.next();
            if (i4msg.isExpunged()) {
                if (debug)  ZimbraLog.imap.debug("  ** removing: " + i4msg.msgId);
                // uncache() removes pointers to the message from mMessageIds;
                //   if the message appears again in mSequence, it *must* be later and the
                //   subsequent call to setIndex() will correctly update the mMessageIds mapping
                uncache(i4msg);
                lit.remove();
                // note that we can't send expunge notifications for messages the client doesn't know about yet...
                if (!i4msg.isAdded())
                    removed.add(byUID ? i4msg.imapUid : seq);
                seq--;
                trimmed = true;
            } else if (trimmed) {
                setIndex(i4msg, seq);
            }
        }
        return removed;
    }


    ImapFolder restore(ImapSession session, SessionData sdata) throws ServiceException {
        mSession = session;
        mMailbox = session.getMailbox();
        mPath = session.getPath();
        mFlags = ImapFlagCache.getSystemFlags(mMailbox);
        // FIXME: NOT RESTORING mSequence.msg.sflags PROPERLY -- need to serialize it!!!
        mSessionData = sdata;

        return this;
    }


    public void handleTagDelete(int changeId, int tagId) {
        mTags.uncache(1L << Tag.getIndex(tagId));
        dirtyTag(tagId, changeId, true);
    }

    public void handleTagCreate(int changeId, Tag tag) {
        cacheTag(tag);
    }

    public void handleTagRename(int changeId, Tag tag, Change chg) {
        mTags.uncache(tag.getBitmask());
        cacheTag(tag);
        dirtyTag(tag.getId(), changeId);
    }

    public void handleItemDelete(int changeId, int itemId) {
        ImapMessage i4msg = getById(itemId);
        if (i4msg != null) {
            markMessageExpunged(i4msg);
            if (ZimbraLog.imap.isDebugEnabled())
                ZimbraLog.imap.debug("  ** deleted (ntfn): " + i4msg.msgId);
        }
    }

    public void handleItemCreate(int changeId, MailItem item, ImapSession.AddedItems added) {
        int msgId = item.getId();
        // make sure this message hasn't already been detected in the folder
        if (getById(msgId) != null)
            return;
        ImapMessage i4msg = getByImapId(item.getImapUid());
        if (i4msg == null)
            added.add(item);

        if (ZimbraLog.imap.isDebugEnabled())
            ZimbraLog.imap.debug("  ** created (ntfn): " + msgId);
    }

    public void handleFolderRename(int changeId, Folder folder, Change chg) {
        updatePath(folder);
        // FIXME: can we change the folder's UIDVALIDITY?
        //        if not, how do we persist it for the session?
        // RFC 2180 3.4: "The server MAY allow the RENAME of a multi-accessed mailbox
        //                by simply changing the name attribute on the mailbox."
    }

    public void handleItemUpdate(int changeId, Change chg, ImapSession.AddedItems added) {
        MailItem item = (MailItem) chg.what;
        boolean inFolder = isVirtual() || (item.getFolderId() == mFolderId);

        ImapMessage i4msg = getById(item.getId());
        if (i4msg == null) {
            if (inFolder && !isVirtual()) {
                added.add(item);
                if (ZimbraLog.imap.isDebugEnabled())
                    ZimbraLog.imap.debug("  ** moved (ntfn): " + item.getId());
            }
        } else if (!inFolder && !isVirtual()) {
            markMessageExpunged(i4msg);
        } else if ((chg.why & Change.MODIFIED_IMAP_UID) != 0) {
            // if the IMAP uid changed, need to bump it to the back of the sequence!
            markMessageExpunged(i4msg);
            if (!isVirtual())
                added.add(item);
            if (ZimbraLog.imap.isDebugEnabled())
                ZimbraLog.imap.debug("  ** imap uid changed (ntfn): " + item.getId());
        } else if ((chg.why & (Change.MODIFIED_TAGS | Change.MODIFIED_FLAGS | Change.MODIFIED_UNREAD)) != 0) {
            i4msg.setPermanentFlags(item.getFlagBitmask(), item.getTagBitmask(), changeId, this);
        }
    }

    public void handleAddedMessages(int changeId, ImapSession.AddedItems added) {
        boolean debug = ZimbraLog.imap.isDebugEnabled();

        added.sort();
        boolean recent = true;
        for (Session s : mMailbox.getListeners(Session.Type.IMAP)) {
            // added messages are only \Recent if we're the first IMAP session notified about them
            ImapSession i4session = (ImapSession) s;
            if (i4session == mSession) {
                break;
            } else if (i4session.isWritable() && i4session.getFolderId() == mFolderId) {
                recent = false;  break;
            }
        }

        if (added.numbered != null) {
            // if messages have acceptable UIDs, just add 'em
            StringBuilder addlog = debug ? new StringBuilder("  ** adding messages (ntfn):") : null;
            for (ImapMessage i4msg : added.numbered) {
                cache(i4msg, recent);
                if (debug)  addlog.append(' ').append(i4msg.msgId);
                i4msg.setAdded(true);
                dirtyMessage(i4msg, changeId);
            }
            if (debug)  ZimbraLog.imap.debug(addlog);
        }

        if (added.unnumbered != null) {
            // 2.3.1.1: "Unique identifiers are assigned in a strictly ascending fashion in
            //           the mailbox; as each message is added to the mailbox it is assigned
            //           a higher UID than the message(s) which were added previously."
            List<Integer> renumber = new ArrayList<Integer>();
            StringBuilder chglog = debug ? new StringBuilder("  ** moved; changing imap uid (ntfn):") : null;
            for (ImapMessage i4msg : added.unnumbered) {
                renumber.add(i4msg.msgId);
                if (debug)  chglog.append(' ').append(i4msg.msgId);
            }
            try {
                if (debug)  ZimbraLog.imap.debug(chglog);
                // notification will take care of adding to mailbox
                getMailbox().resetImapUid(null, renumber);
            } catch (ServiceException e) {
                if (debug)  ZimbraLog.imap.debug("  ** moved; imap uid change failed; msg hidden (ntfn): " + renumber);
            }
        }
    }

    public void finishNotification(int changeId)  { }
}
