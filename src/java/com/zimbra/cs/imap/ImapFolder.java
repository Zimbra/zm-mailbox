/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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
package com.zimbra.cs.imap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ArrayUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.imap.ImapFlagCache.ImapFlag;
import com.zimbra.cs.imap.ImapMessage.ImapMessageSet;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.SearchFolder;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.client.ZFolder;

/**
 * @since Apr 30, 2005
 */
public final class ImapFolder implements ImapSession.ImapFolderData, java.io.Serializable {
    // Update serialVersionUID when changing any instance members. Otherwise serialization won't work correctly.
    private static final long serialVersionUID = 3845968507901145794L;
    static final byte SELECT_READONLY  = 0x01;
    static final byte SELECT_CONDSTORE = 0x02;

    // attributes of the folder itself, irrespective of the session state
    private transient Mailbox mailbox;
    private transient ImapSession session;
    private transient ImapPath path;
    private transient SessionData sessionData;
    private transient Map<Integer, ImapMessage> messageIds;
    private transient ImapFlagCache flags;

    private final int folderId;
    private int uidValidity;
    private String query;
    private Set<MailItem.Type> typeConstraint = ImapHandler.ITEM_TYPES;
    private final List<ImapMessage> sequence = new ArrayList<ImapMessage>();
    private ImapFlagCache tags;   // operationally could be "transient", but that makes deserialization replay depend on magic

    // below this point are session-specific attributes of the folder SELECT state
    static class SessionData {
        ImapCredentials credentials;
        boolean writable;

        int lastSize;     // for EXISTS notifications
        int recentCount;  // for RECENT notifications
        int expungedCount;

        boolean tagsAreDirty;
        boolean notificationsSuspended;
        ImapMessageSet savedSearchResults;
        final Map<Integer, DirtyMessage> dirtyMessages = new ConcurrentSkipListMap<Integer, DirtyMessage>();

        SessionData(ImapPath path, byte params, ImapHandler handler) throws ServiceException {
            this.credentials = handler.getCredentials();
            this.writable = (params & SELECT_READONLY) == 0 && path.isWritable();
        }
        boolean hasNotifications() {
            return tagsAreDirty || !dirtyMessages.isEmpty() || expungedCount > 0;
        }
    }

    /** Initializes an empty ImapFolder from a {@link Folder}, specified by
     *  path.
     * @param path     The target folder's path.
     * @param params   Optional SELECT parameters (e.g. READONLY).
     * @param handler  The authenticated user's current IMAP session. */
    ImapFolder(ImapPath path, byte params, ImapHandler handler) throws ServiceException {
        this.path = path;
        Folder folder = (Folder) path.getFolder();
        this.folderId = folder.getId();
        // FIXME: Folder object may be stale since it's cached in ImapPath
        this.uidValidity = getUIDValidity(folder);
        if (folder instanceof SearchFolder) {
            this.query = ((SearchFolder) folder).getQuery();
            this.typeConstraint = getTypeConstraint((SearchFolder) folder);
        }

        if (handler != null) {
            this.sessionData = new SessionData(path, params, handler);
        }
        this.mailbox = folder.getMailbox();
        this.flags = ImapFlagCache.getSystemFlags(mailbox);
        this.tags = new ImapFlagCache();
    }

    void setInitialSize() {
        SessionData sdata = sessionData;
        if (sdata != null) {
            sdata.lastSize = sequence.size();
        }
    }

    @Override
    public void doEncodeState(Element imap) {
        SessionData sdata = sessionData;
        if (sdata != null) {
            ImapCredentials.EnabledHack[] hacks = sdata.credentials.getEnabledHacks();
            imap.addAttribute("hack", hacks == null ? null : Arrays.toString(hacks));
            imap.addAttribute("writable", isWritable());
            imap.addAttribute("dirty", sdata.dirtyMessages.size()).addAttribute("expunged", sdata.expungedCount);
        }
        imap.addAttribute("size", getSize());
        imap.addAttribute("folder", path.asImapPath()).addAttribute("query", query);
    }

    void setSession(ImapSession value) {
        assert(session == null || session == value || sessionData == null);
        session = value;
    }

    SessionData getSessionData() {
        return sessionData;
    }

    @Override
    public void endSelect() {
        sessionData = null;
    }

    /** Returns the selected folder's containing {@link Mailbox}. */
    public Mailbox getMailbox() {
        return mailbox;
    }

    /** Returns the {@link ImapCredentials} with which this ImapFolder was
     *  created. */
    ImapCredentials getCredentials() {
        SessionData sdata = sessionData;
        return sdata == null ? null : sdata.credentials;
    }

    /** Returns the selected folder's zimbra ID. */
    @Override
    public int getId() {
        return folderId;
    }

    /** Returns the number of messages in the folder.  Messages that have been
     *  received or deleted since the client was last notified are still
     *  included in this count. */
    @Override
    public int getSize() {
        return sequence.size();
    }

    /** Returns the number of messages in the folder that are considered
     *  \Recent.  These are messages that have been deposited in the folder
     *  since the last IMAP session that opened the folder. */
    int getRecentCount() {
        SessionData sdata = sessionData;
        return isVirtual()  || sdata == null ? 0 : sdata.recentCount;
    }

    @Override
    public boolean hasExpunges() {
        SessionData sdata = sessionData;
        return sdata != null && sdata.expungedCount > 0;
    }

    @Override
    public boolean hasNotifications() {
        SessionData sdata = sessionData;
        return sdata != null && sdata.hasNotifications();
    }

    /** Returns the search folder query associated with this IMAP folder, or
     *  {@code ""} if the SELECTed folder is not a search folder. */
    String getQuery() {
        return Strings.nullToEmpty(query);
    }

    /**
     * Constrain the search to the actually-requested types.
     */
    static Set<MailItem.Type> getTypeConstraint(SearchFolder search) {
        String typestr = search.getReturnTypes().toLowerCase();
        Set<MailItem.Type> types;
        if (!typestr.isEmpty()) {
            try {
                types = MailItem.Type.setOf(typestr);
            } catch (IllegalArgumentException e) {
                ZimbraLog.imap.warn("invalid item type: " + typestr, e);
                return EnumSet.noneOf(MailItem.Type.class);
            }
        } else {
            types = EnumSet.of(MailItem.Type.CONVERSATION);
        }

        if (types.remove(MailItem.Type.CONVERSATION)) {
            types.add(MailItem.Type.MESSAGE);
        }
        types.retainAll(ImapMessage.SUPPORTED_TYPES);
        return types;
    }

    /** Returns the types of items exposed in this IMAP folder.  Defaults to
     *  {@link ImapHandler#ITEM_TYPES} except for search folders. */
    Set<MailItem.Type> getTypeConstraint() {
        return typeConstraint;
    }

    /** Returns the folder's IMAP UID validity value.
     * @see #getUIDValidity(Folder) */
    int getUIDValidity() {
        return uidValidity;
    }

    int getCurrentMODSEQ() throws ServiceException {
        return mailbox.getFolderById(null, folderId).getImapMODSEQ();
    }

    /** Returns whether this folder is a "virtual" folder (i.e. a search
     *  folder).
     * @see #loadVirtualFolder(SearchFolder, Mailbox, OperationContext) */
    boolean isVirtual() {
        return query != null;
    }

    /** Returns whether this folder was opened for write. */
    @Override
    public boolean isWritable() {
        SessionData sdata = sessionData;
        return sdata == null ? false : sdata.writable;
    }

    public synchronized void traverse(Function<ImapMessage, Void> func) {
        int prevUid = -1;
        for (Iterator<ImapMessage> it = sequence.iterator(); it.hasNext();) {
            ImapMessage i4msg = it.next();
            if (i4msg.imapUid == prevUid) {
                ZimbraLog.imap.warn("duplicate UID %d in cached folder %d", prevUid, folderId);
                it.remove();
            } else {
                prevUid = i4msg.imapUid;
                func.apply(i4msg);
            }
        }
    }

    ImapPath getPath() {
        return path;
    }

    void updatePath(Folder folder) {
        path = new ImapPath(null, folder.getPath(), path.getCredentials());
    }

    String getQuotedPath() throws ServiceException {
        return '"' + path.asResolvedPath() + '"';
    }

    @Override
    public String toString() {
        return path.toString();
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
     *  folder's {@link #sequence} message list.  This retrieval is done via
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
            int targetUid = sequence.get(mid).imapUid;
            if (targetUid < uid) {
                low = mid + 1;
            } else if (targetUid > uid) {
                high = mid - 1;
            } else {
                return mid;  // key found
            }
        }
        return -(low + 1);  // key not found
    }

    /** Returns the ImapMessage with the given Zimbra item ID from the
     *  folder's {@link #sequence} message list. */
    synchronized ImapMessage getById(int id) {
        if (id <= 0 || getSize() == 0) {
            return null;
        }
        // leverage the fact that by default, the message's item id and its IMAP uid are identical
        int seq = uidSearch(id);
        if (seq >= 0 && seq < sequence.size()) {
            ImapMessage i4msg = sequence.get(seq);
            // slightly tricky: must check if message is expunged in order to catch the case of
            //   using the web client to move the message out of the folder and back in before
            //   the IMAP server can tell the client about the EXPUNGE from the first move
            if (i4msg != null && i4msg.msgId == id && !i4msg.isExpunged()) {
                return checkRemoved(i4msg);
            }
        }

        // if item id and IMAP uid differ, the message goes in the "mMessageIds" map
        if (messageIds == null) {
            // lookup miss means we need to generate the item-id-to-imap-message mapping
            messageIds = new HashMap<Integer, ImapMessage>();
            for (ImapMessage i4msg : sequence) {
                if (i4msg != null && i4msg.msgId != i4msg.imapUid) {
                    messageIds.put(i4msg.msgId, i4msg);
                }
            }
        }
        return checkRemoved(messageIds.get(new Integer(id)));
    }

    /** Returns the ImapMessage with the given IMAP UID from the folder's
     *  {@link #sequence} message list. */
    ImapMessage getByImapId(int uid) {
        return uid > 0 ? getBySequence(uidSearch(uid) + 1) : null;
    }

    /** Returns the ImapMessage with the given 1-based sequence number in the
     *  folder's {@link #sequence} message list. */
    ImapMessage getBySequence(int seq) {
        return getBySequence(seq, false);
    }

    /** Returns the ImapMessage with the given 1-based sequence number in the
     *  folder's {@link #sequence} message list. */
    ImapMessage getBySequence(int seq, boolean includeExpunged) {
        ImapMessage i4msg = seq > 0 && seq <= getSize() ? sequence.get(seq - 1) : null;
        return includeExpunged ? i4msg : checkRemoved(i4msg);
    }

    /** Returns the last ImapMessage in the folder's {@link #sequence}
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
     *  the folder's {@link #sequence} message list and inserted into the
     *  {@link #mMessageIds} hash (if the latter hash has been instantiated).
     * @return the passed-in ImapMessage. */
    synchronized ImapMessage cache(ImapMessage i4msg, boolean recent) {
        // provide the information missing from the DB search
        if (folderId == Mailbox.ID_FOLDER_SPAM) {
            i4msg.sflags |= ImapMessage.FLAG_SPAM | ImapMessage.FLAG_JUNKRECORDED;
        }
        if (recent) {
            i4msg.sflags |= ImapMessage.FLAG_RECENT;
            SessionData sdata = sessionData;
            if (sdata != null) {
                sdata.recentCount++;
            }
        }
        // update the folder information
        if (sequence.size() > 0 && sequence.get(sequence.size() - 1).imapUid == i4msg.imapUid) {
            ZimbraLog.imap.error("duplicate UID %d will be replaced", i4msg.imapUid, new Exception());
            sequence.set(sequence.size() - 1, i4msg);
        } else {
            sequence.add(i4msg);
        }
        setIndex(i4msg, sequence.size());
        // update the tag cache to include only the tags in the folder
        updateTagCache(i4msg);
        return i4msg;
    }

    void updateTagCache(ImapMessage i4msg) {
        if (!ArrayUtil.isEmpty(i4msg.tags)) {
            for (String tag : i4msg.tags) {
                if (tags.getByZimbraName(tag) == null) {
                    try {
                        tags.cache(new ImapFlag(mailbox.getTagByName(tag)));
                        setTagsDirty(true);
                    } catch (ServiceException e) {
                        ZimbraLog.imap.warn("could not fetch listed tag: %s", tag, e);
                    }
                }
            }
        }
    }

    private void setIndex(ImapMessage i4msg, int position) {
        i4msg.sequence = position;
        if (messageIds != null) {
            if (i4msg.msgId != i4msg.imapUid) {
                messageIds.put(new Integer(i4msg.msgId), i4msg);
            } else {
                messageIds.remove(new Integer(i4msg.msgId));
            }
        }
    }

    /** Cleans up all references to an ImapMessage from all the folder's data
     *  structures other than {@link #sequence}.  The {@link #sequence}
     *  cleanup must be done separately. */
    private void uncache(ImapMessage i4msg) {
        if (messageIds != null) {
            messageIds.remove(i4msg.msgId);
        }
        SessionData sdata = sessionData;
        if (sdata != null) {
            sdata.dirtyMessages.remove(new Integer(i4msg.imapUid));
            if ((i4msg.sflags & ImapMessage.FLAG_RECENT) != 0) {
                sdata.recentCount--;
            }
            if ((i4msg.sflags & ImapMessage.FLAG_EXPUNGED) != 0) {
                sdata.expungedCount--;
            }
        }
    }

    boolean areTagsDirty() {
        SessionData sdata = sessionData;
        return sdata == null ? false : sdata.tagsAreDirty;
    }

    void setTagsDirty(boolean dirty) {
        SessionData sdata = sessionData;
        if (sdata != null) {
            sdata.tagsAreDirty = dirty;
        }
    }

    ImapFlag cacheTag(Tag ltag) {
        assert !(ltag instanceof Flag);
        if (ltag instanceof Flag) {
            return null;
        }

        setTagsDirty(true);
        return tags.cache(new ImapFlag(ltag));
    }

    void dirtyTag(ImapFlag i4flag, int modseq, String newName) {
        setTagsDirty(true);
        if (getSize() == 0 || i4flag == null) {
            return;
        }

        for (ImapMessage i4msg : sequence) {
            if (i4msg != null && i4flag.matches(i4msg)) {
                dirtyMessage(i4msg, modseq);

                List<String> tags = Lists.newArrayList(i4msg.tags);
                tags.remove(i4flag.mName);
                if (newName != null) {
                    tags.add(newName);
                }
                i4msg.tags = tags.isEmpty() ? null : tags.toArray(new String[tags.size()]);
            }
        }
    }

    ImapFlag getFlagByName(String name) {
        ImapFlag i4flag = flags.getByImapName(name);
        return (i4flag != null ? i4flag : tags.getByImapName(name));
    }

    ImapFlag getTagByName(String name) {
        return flags.getByImapName(name);
    }

    List<String> getFlagList(boolean permanentOnly) {
        List<String> names = flags.listNames(permanentOnly);
        for (String tagname : tags.listNames(permanentOnly)) {
            if (flags.getByImapName(tagname) == null) {
                names.add(tagname);
            }
        }
        return names;
    }

    ImapFlagCache getTagset() {
        return tags;
    }

    void clearTagCache() {
        tags.clear();
    }

    static final class DirtyMessage {
        ImapMessage i4msg;
        int modseq;
        DirtyMessage(ImapMessage m, int s)  { i4msg = m;  modseq = s; }
    }

    boolean isMessageDirty(ImapMessage i4msg) {
        SessionData sdata = sessionData;
        return sdata == null ? false : sdata.dirtyMessages.containsKey(i4msg.imapUid);
    }

    void dirtyMessage(ImapMessage i4msg, int modseq) {
        SessionData sdata = sessionData;
        if (sdata == null) {
            return;
        }
        if (sdata.notificationsSuspended || i4msg != getBySequence(i4msg.sequence)) {
            return;
        }
        DirtyMessage dirty = sdata.dirtyMessages.get(i4msg.imapUid);
        if (dirty == null) {
            sdata.dirtyMessages.put(i4msg.imapUid, new DirtyMessage(i4msg, modseq));
        } else if (modseq > dirty.modseq) {
            dirty.modseq = modseq;
        }
    }

    DirtyMessage undirtyMessage(ImapMessage i4msg) {
        SessionData sdata = sessionData;
        if (sdata == null) {
            return null;
        }
        DirtyMessage dirty = sdata.dirtyMessages.remove(i4msg.imapUid);
        if (dirty != null) {
            dirty.i4msg.setAdded(false);
        }
        return dirty;
    }

    Iterator<DirtyMessage> dirtyIterator() {
        SessionData sdata = sessionData;
        return sdata == null ? Iterators.<DirtyMessage>emptyIterator() : sdata.dirtyMessages.values().iterator();
    }

    /** Empties the folder's list of modified/created messages. */
    void clearDirty()  {
        SessionData sdata = sessionData;
        if (sdata != null) {
            sdata.dirtyMessages.clear();
        }
    }

    boolean checkpointSize() {
        SessionData sdata = sessionData;
        if (sdata == null) {
            return false;
        }
        int last = sdata.lastSize;
        return last != (sdata.lastSize = getSize());
    }

    void disableNotifications() {
        SessionData sdata = sessionData;
        if (sdata != null) {
            sdata.notificationsSuspended = true;
        }
    }

    void enableNotifications() {
        SessionData sdata = sessionData;
        if (sdata != null) {
            sdata.notificationsSuspended = false;
        }
    }

    void saveSearchResults(ImapMessageSet i4set) {
        SessionData sdata = sessionData;
        if (sdata != null) {
            i4set.remove(null);
            sdata.savedSearchResults = i4set;
        }
    }

    ImapMessageSet getSavedSearchResults() {
        SessionData sdata = sessionData;
        if (sdata == null) {
            return new ImapMessageSet();
        }
        if (sdata.savedSearchResults == null) {
            sdata.savedSearchResults = new ImapMessageSet();
        }
        return sdata.savedSearchResults;
    }

    void markMessageExpunged(ImapMessage i4msg) {
        if (i4msg.isExpunged()) {
            return;
        }
        i4msg.setExpunged(true);

        SessionData sdata = sessionData;
        if (sdata != null) {
            if (sdata.savedSearchResults != null) {
                sdata.savedSearchResults.remove(i4msg);
            }
            sdata.expungedCount++;
        }
    }

    synchronized ImapMessageSet getAllMessages() {
        ImapMessageSet result = new ImapMessageSet();
        if (getSize() > 0) {
            result.addAll(sequence);
            result.remove(null);
        }
        return result;
    }

    synchronized ImapMessageSet getFlaggedMessages(ImapFlag i4flag) {
        ImapMessageSet result = new ImapMessageSet();
        if (i4flag != null && getSize() > 0) {
            for (ImapMessage i4msg : sequence) {
                if (i4msg != null && i4flag.matches(i4msg)) {
                    result.add(i4msg);
                }
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
        if (subseqStr == null || subseqStr.trim().isEmpty()) {
            return Collections.emptyList();
        }
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
                if (lower > upper)  {
                    int tmp = upper;
                    upper = lower;
                    lower = tmp;
                }
            }

            // add to list, merging with existing ranges if needed
            int insertpos = 0;
            for (int i = 0; i < normalized.size(); i++) {
                Pair<Integer, Integer> range = normalized.get(i);
                int lrange = range.getFirst(), urange = range.getSecond();
                if (lower > urange + 1) {
                    insertpos++;
                    continue;
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
        return getSubsequence(tag, subseqStr, byUID, isSEARCH, false);
    }

    ImapMessageSet getSubsequence(String tag, String subseqStr, boolean byUID, boolean isSEARCH, boolean isFETCH)
            throws ImapParseException {
        ImapMessageSet result = new ImapMessageSet();
        if (subseqStr == null || subseqStr.trim().isEmpty()) {
            return result;
        } else if (subseqStr.equals("$")) {
            return getSavedSearchResults();
        }
        for (Pair<Integer, Integer> range : normalizeSubsequence(subseqStr, byUID)) {
            int lower = range.getFirst(), upper = range.getSecond();
            if (!byUID && (lower < 1 || upper > getSize()) && !isSEARCH) {
                // 9: "The server should respond with a tagged BAD response to a command that uses a message
                //     sequence number greater than the number of messages in the selected mailbox.  This
                //     includes "*" if the selected mailbox is empty."
                throw new ImapParseException(tag, "invalid message sequence number: " + subseqStr);
            } else if (lower == upper) {
                // single message -- get it and add it (may be null)
                result.add(byUID ? getByImapId(lower) : getBySequence(lower, isFETCH));
            } else {
                // range of messages -- get them and add them (may be null)
                if (!byUID) {
                    upper = Math.min(getSize(), upper);
                    for (int seq = Math.max(0, lower); seq <= upper; seq++) {
                        result.add(getBySequence(seq, isFETCH));
                    }
                } else {
                    ImapMessage i4msg;
                    int start = uidSearch(lower), end = uidSearch(upper);
                    if (start < 0) {
                        start = -start - 1;
                    }
                    if (end < 0) {
                        end = -end - 2;
                    }
                    for (int seq = start; seq <= end; seq++) {
                        if ((i4msg = getBySequence(seq + 1)) != null) {
                            result.add(i4msg);
                        }
                    }
                }
            }
        }

        return result;
    }

    String cropSubsequence(String subseqStr, boolean byUID, int croplow, int crophigh) {
        if (croplow <= 0 && crophigh <= 0) {
            return subseqStr;
        }
        StringBuilder sb = new StringBuilder(subseqStr.length());
        for (Pair<Integer, Integer> range : normalizeSubsequence(subseqStr, byUID)) {
            int lower = range.getFirst(), upper = range.getSecond();
            if (croplow > 0 && upper < croplow) {
                continue;
            }
            if (crophigh > 0 && lower > crophigh) {
                continue;
            }
            if (croplow > 0) {
                lower = Math.max(lower, croplow);
            }
            if (crophigh > 0) {
                upper = Math.min(upper, crophigh);
            }
            sb.append(sb.length() == 0 ? "" : ",").append(lower).append(lower == upper ? "" : ":" + upper);
        }
        return sb.toString();
    }

    String invertSubsequence(String subseqStr, boolean byUID, Set<ImapMessage> i4set) {
        StringBuilder sb = new StringBuilder();

        Iterator<ImapMessage> i4it = i4set.iterator();
        Iterator<Pair<Integer, Integer>> itrange = normalizeSubsequence(subseqStr, byUID).iterator();
        if (!itrange.hasNext()) {
            return subseqStr;
        }
        Pair<Integer, Integer> range = itrange.next();
        int lower = range.getFirst();
        int upper = range.getSecond();
        int id = !i4it.hasNext() ? -1 : (byUID ? i4it.next().imapUid : i4it.next().sequence);

        while (lower != -1) {
            if (lower > upper) {
                // no valid values remaining in this range, so go to the next one
                if (!itrange.hasNext()) {
                    break;
                }
                range = itrange.next();
                lower = range.getFirst();
                upper = range.getSecond();
            } else if (id == -1 || id > upper) {
                // the remainder of the range qualifies, so serialize it and go to the next range
                sb.append(sb.length() == 0 ? "" : ",").append(lower).append(lower == upper ? "" : ":" + upper);
                if (!itrange.hasNext()) {
                    break;
                }
                range = itrange.next();  lower = range.getFirst();  upper = range.getSecond();
            } else if (id <= lower) {
                // the current ID is too low for this range, so fetch the next ID
                if (id == lower) {
                    lower++;
                }
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
        if (items == null || items.isEmpty()) {
            return "";
        }

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
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(start);
                if (start != last) {
                    sb.append(':').append(last);
                }
                last = start = next;
            } else {
                last = next;
            }
        } while (!done);
        return sb.toString();
    }

    static String encodeSubsequence(Collection<ImapMessage> items, boolean byUID) {
        if (items == null || items.isEmpty()) {
            return "";
        }

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
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(start);
                if (start != last) {
                    sb.append(':').append(last);
                }
                last = start = next;
            } else {
                last = next;
            }
        } while (!done);
        return sb.toString();
    }

    synchronized List<Integer> collapseExpunged(boolean byUID) {
        if (getSize() == 0) {
            return Collections.emptyList();
        }
        ZimbraLog.imap.debug("  ** iterating (collapseExpunged)");

        boolean trimmed = false;
        int seq = 1;
        List<Integer> removed = new ArrayList<Integer>();
        for (ListIterator<ImapMessage> lit = sequence.listIterator(); lit.hasNext(); seq++) {
            ImapMessage i4msg = lit.next();
            if (i4msg.isExpunged()) {
                ZimbraLog.imap.debug("  ** removing: %d", i4msg.msgId);
                // uncache() removes pointers to the message from mMessageIds;
                //   if the message appears again in sequence, it *must* be later and the
                //   subsequent call to setIndex() will correctly update the mMessageIds mapping
                uncache(i4msg);
                lit.remove();
                // note that we can't send expunge notifications for messages the client doesn't know about yet...
                if (!i4msg.isAdded()) {
                    removed.add(byUID ? i4msg.imapUid : seq);
                }
                seq--;
                trimmed = true;
            } else if (trimmed) {
                setIndex(i4msg, seq);
            }
        }
        return removed;
    }

    void restore(ImapSession sess, SessionData sdata) throws ImapSessionClosedException, ServiceException {
        session = sess;
        mailbox = session.getMailbox();
        if (mailbox == null) {
            throw new ImapSessionClosedException();
        }
        path = session.getPath();
        flags = ImapFlagCache.getSystemFlags(mailbox);
        // FIXME: NOT RESTORING sequence.msg.sflags PROPERLY -- need to serialize it!!!
        sessionData = sdata;
    }

    @Override
    public void handleTagDelete(int changeId, int tagId, Change chg) {
        dirtyTag(tags.uncache(tagId), changeId, null);
    }

    @Override
    public void handleTagRename(int changeId, Tag tag, Change chg) {
        dirtyTag(tags.uncache(tag.getId()), changeId, tag.getName());
        cacheTag(tag);
    }

    @Override
    public void handleItemDelete(int changeId, int itemId, Change chg) {
        ImapMessage i4msg = getById(itemId);
        if (i4msg != null) {
            markMessageExpunged(i4msg);
            ZimbraLog.imap.debug("  ** deleted (ntfn): %d", i4msg.msgId);
        }
    }

    @Override
    public void handleItemCreate(int changeId, MailItem item, ImapSession.AddedItems added) {
        int msgId = item.getId();
        // make sure this message hasn't already been detected in the folder
        if (getById(msgId) != null) {
            return;
        }

        ImapMessage i4msg = getByImapId(item.getImapUid());
        if (i4msg == null) {
            added.add(item);
        }
        ZimbraLog.imap.debug("  ** created (ntfn): %d", msgId);
    }

    @Override
    public void handleFolderRename(int changeId, Folder folder, Change chg) {
        updatePath(folder);
        // FIXME: can we change the folder's UIDVALIDITY?
        //        if not, how do we persist it for the session?
        // RFC 2180 3.4: "The server MAY allow the RENAME of a multi-accessed mailbox
        //                by simply changing the name attribute on the mailbox."
    }

    @Override
    public void handleItemUpdate(int changeId, Change chg, ImapSession.AddedItems added) {
        MailItem item = (MailItem) chg.what;
        boolean inFolder = isVirtual() || item.getFolderId() == folderId;

        ImapMessage i4msg = getById(item.getId());
        if (i4msg == null) {
            if (inFolder && !isVirtual()) {
                added.add(item);
                ZimbraLog.imap.debug("  ** moved (ntfn): %d", item.getId());
            }
        } else if (!inFolder && !isVirtual()) {
            markMessageExpunged(i4msg);
        } else if ((chg.why & Change.IMAP_UID) != 0) {
            // if the IMAP uid changed, need to bump it to the back of the sequence!
            markMessageExpunged(i4msg);
            if (!isVirtual()) {
                added.add(item);
            }
            ZimbraLog.imap.debug("  ** imap uid changed (ntfn): %d", item.getId());
        } else if ((chg.why & (Change.TAGS | Change.FLAGS | Change.UNREAD)) != 0) {
            i4msg.setPermanentFlags(item.getFlagBitmask(), item.getTags(), changeId, this);
        }
    }

    @Override
    public void handleAddedMessages(int changeId, ImapSession.AddedItems added) {
        boolean debug = ZimbraLog.imap.isDebugEnabled();

        added.sort();
        boolean recent = true;
        for (Session s : mailbox.getListeners(Session.Type.IMAP)) {
            // added messages are only \Recent if we're the first IMAP session notified about them
            ImapSession i4session = (ImapSession) s;
            if (i4session == session) {
                break;
            } else if (i4session.isWritable() && i4session.getFolderId() == folderId) {
                recent = false;
                break;
            }
        }

        if (added.numbered != null) {
            // if messages have acceptable UIDs, just add 'em
            StringBuilder addlog = debug ? new StringBuilder("  ** adding messages (ntfn):") : null;
            for (ImapMessage i4msg : added.numbered) {
                cache(i4msg, recent);
                if (debug) {
                    addlog.append(' ').append(i4msg.msgId);
                }
                i4msg.setAdded(true);
                dirtyMessage(i4msg, changeId);
            }
            if (debug) {
                ZimbraLog.imap.debug(addlog);
            }
        }

        if (added.unnumbered != null) {
            // 2.3.1.1: "Unique identifiers are assigned in a strictly ascending fashion in
            //           the mailbox; as each message is added to the mailbox it is assigned
            //           a higher UID than the message(s) which were added previously."
            List<Integer> renumber = new ArrayList<Integer>();
            StringBuilder chglog = debug ? new StringBuilder("  ** moved; changing imap uid (ntfn):") : null;
            for (ImapMessage i4msg : added.unnumbered) {
                renumber.add(i4msg.msgId);
                if (debug) {
                    chglog.append(' ').append(i4msg.msgId);
                }
            }
            try {
                if (debug) {
                    ZimbraLog.imap.debug(chglog);
                }
                // notification will take care of adding to mailbox
                getMailbox().resetImapUid(null, renumber);
            } catch (ServiceException e) {
                if (debug) {
                    ZimbraLog.imap.debug("  ** moved; imap uid change failed; msg hidden (ntfn): %d", renumber);
                }
            }
        }
    }

    @Override
    public void finishNotification(int changeId) {
    }

}
