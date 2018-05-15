/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.mailbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;
import com.zimbra.common.mailbox.Color;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbTag;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.MailItem.CustomMetadata.CustomMetadataList;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.session.Session;

/**
 * @since Jun 13, 2004
 */
public class Conversation extends MailItem {
    private   String     mEncodedSenders;
    protected SenderList mSenderList;

    Conversation(Mailbox mbox, UnderlyingData data) throws ServiceException {
        this(mbox, data, false);
    }

    Conversation(Mailbox mbox, UnderlyingData data, boolean skipCache) throws ServiceException {
        super(mbox, data, skipCache);
        init();
    }

    Conversation(Account acc, UnderlyingData data, int mailboxId)  throws ServiceException {
        super(acc, data, mailboxId);
        init();
    }

    private void init() throws ServiceException {
        if (type != Type.CONVERSATION.toByte() && type != Type.VIRTUAL_CONVERSATION.toByte()) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Returns the normalized subject of the conversation.  This is done by taking the {@code Subject:} header of the
     * first message and removing prefixes (e.g. {@code "Re:"}) and suffixes (e.g. {@code "(fwd)"}) and the like.
     *
     * @see ParsedMessage#normalizeSubject
     */
    String getNormalizedSubject() {
        return ParsedMessage.normalize(getSubject());
    }

    @Override
    public String getSender() {
        return "";
    }

    @Override
    public String getSortSubject() {
        return getNormalizedSubject();
    }

    /** Returns the number of messages in the conversation, as calculated from
     *  its list of children as fetched from the database.  This <u>should</u>
     *  always be equal to {@link MailItem#getSize()}; if it isn't, an error
     *  has occurred and been persisted to the database. */
    public int getMessageCount() {
        return (int) getSize();
    }

    @Override
    public int getInternalFlagBitmask() {
        return 0;
    }


    // do *not* make this public, as it'd skirt Mailbox-level synchronization and caching
    SenderList getSenderList() throws ServiceException {
        loadSenderList();
        return mSenderList;
    }

    void instantiateSenderList() {
        if (mSenderList != null && mSenderList.size() == getSize())
            return;

        mSenderList = null;
        // first, attempt to decode the existing sender list (if there is one)
        if (mEncodedSenders != null) {
            String encoded = mEncodedSenders;
            mEncodedSenders = null;
            try {
                // if the first message has been removed, this should throw
                //   an exception and force the list to be recalculated
                mSenderList = SenderList.parse(encoded);
                if (mSenderList.size() != getSize()) {
                    mSenderList = null;
                }
            } catch (Exception e) { }
        }
    }

    /** Makes certain the Conversation's {@link SenderList} is loaded and
     *  valid.  If it's not, first tries to instantiate it from the loaded
     *  metadata.  If that's either missing or invalid, recalculates the
     *  Conversation's metadata from its constituent messages and rewrites
     *  the database row.
     *
     * @return <tt>true</tt> if the metadata was recalculated, <tt>false</tt>
     *         if the SenderList was generated from existing data */
    boolean loadSenderList() throws ServiceException {
        instantiateSenderList();
        if (mSenderList != null)
            return false;

        // failed to parse or too few senders are listed -- have to recalculate
        //   (go through the Mailbox because we need to be in a transaction)
        recalculateMetadata();
        return true;
    }

    void recalculateCounts(List<Message> msgs) throws ServiceException {
        markItemModified(Change.TAGS | Change.FLAGS | Change.UNREAD);
        Set<String> tags = new HashSet<String>();
        int unreadCount = 0;
        state.setFlags(0);
        for (Message msg : msgs) {
            unreadCount += msg.getUnreadCount();
            state.setFlags(state.getFlags() | msg.getInternalFlagBitmask());
            for (String tag : msg.state.getTags()) {
                tags.add(tag);
            }
        }
        state.setUnreadCount(unreadCount);
        state.setTags(new Tag.NormalizedTags(tags));
    }

    private static final int RECALCULATE_CHANGE_MASK = Change.TAGS | Change.FLAGS  | Change.SENDERS | Change.SIZE |
            Change.UNREAD | Change.METADATA;

    SenderList recalculateMetadata() throws ServiceException {
        return recalculateMetadata(getMessages());
    }

    SenderList recalculateMetadata(List<Message> msgs) throws ServiceException {
        Collections.sort(msgs, new Message.SortDateAscending());

        markItemModified(RECALCULATE_CHANGE_MASK);

        mEncodedSenders = null;
        mSenderList = new SenderList(msgs);
        state.setSize(msgs.size());

        Set<String> tags = new HashSet<String>();
        int unreadCount = 0;
        state.setFlags(0);
        mExtendedData = null;
        for (Message msg : msgs) {
            super.addChild(msg);
            unreadCount += (msg.isUnread() ? 1 : 0);
            state.setFlags(state.getFlags() | msg.getInternalFlagBitmask());
            for (String tag : msg.state.getTags()) {
                tags.add(tag);
            }
            mExtendedData = MetadataCallback.duringConversationAdd(mExtendedData, msg);
        }
        state.setUnreadCount(unreadCount);
        state.setTags(new Tag.NormalizedTags(tags));

        // need to rewrite the overview metadata
        ZimbraLog.mailbox.debug("resetting metadata: cid=%d,size was=%d is=%d", mId, state.getSize(), mSenderList.size());
        saveData(new DbMailItem(mMailbox));
        return mSenderList;
    }

    /** Returns all the {@link Message}s in this conversation.  The messages
     *  are fetched from the {@link Mailbox}'s cache, if possible; if not,
     *  they're fetched from the database.  The returned messages are not
     *  guaranteed to be sorted in any way. */
    List<Message> getMessages() throws ServiceException {
        return getMessages(SortBy.NONE, -1);
    }

    /** Returns all the {@link Message}s in this conversation.  The messages
     *  are fetched from the {@link Mailbox}'s cache, if possible; if not,
     *  they're fetched from the database.
     *
     * @param sort the sort order for the messages
     * @param limit max number of messages to retrieve, or unlimited if -1
     */
    List<Message> getMessages(SortBy sort, int limit) throws ServiceException {
        List<Message> msgs = new ArrayList<Message>(getMessageCount());
        List<UnderlyingData> listData = DbMailItem.getByParent(this, sort, limit, false);
        for (UnderlyingData data : listData) {
            msgs.add(mMailbox.getMessage(data));
        }
        return msgs;
    }

    @Override
    boolean canAccess(short rightsNeeded) {
        return true;
    }

    @Override
    boolean canAccess(short rightsNeeded, Account authuser, boolean asAdmin) {
        return true;
    }

    @Override
    boolean isTaggable() {
        return false;
    }

    @Override
    boolean isCopyable() {
        return false;
    }

    @Override
    boolean isMovable() {
        return true;
    }

    @Override
    boolean isMutable() {
        return true;
    }

    @Override
    boolean canHaveChildren() {
        return true;
    }

    @Override
    boolean canParent(MailItem item) {
        return (item instanceof Message);
    }

    static Conversation create(Mailbox mbox, int id, Message... msgs) throws ServiceException {
        if (ZimbraLog.mailop.isDebugEnabled()) {
            StringBuilder msgIds = new StringBuilder();
            for (int i = 0; i < msgs.length; i++) {
                msgIds.append(i > 0 ? "," : "").append(msgs[i].getId());
            }
            ZimbraLog.mailop.debug("Adding Conversation: id=%d, message(s): %s.", id, msgIds);
        }

        assert(id != Mailbox.ID_AUTO_INCREMENT && msgs.length > 0);
        Arrays.sort(msgs, new Message.SortDateAscending());

        int date = 0, unread = 0, flags = 0;
        CustomMetadataList extended = null;
        Set<String> tags = new HashSet<String>();
        for (int i = 0; i < msgs.length; i++) {
            Message msg = msgs[i];
            if (msg == null) {
                throw ServiceException.FAILURE("null Message in list", null);
            }
            date = Math.max(date, msg.state.getDate());
            unread += msg.state.getUnreadCount();
            flags  |= msg.state.getFlags();
            for (String tag : msg.state.getTags()) {
                tags.add(tag);
            }
            extended = MetadataCallback.duringConversationAdd(extended, msg);
        }

        UnderlyingData data = new UnderlyingData();
        data.id = id;
        data.type = Type.CONVERSATION.toByte();
        data.folderId = Mailbox.ID_FOLDER_CONVERSATIONS;
        data.setSubject(msgs.length > 0 ? msgs[0].getSubject() : "");
        data.date = date;
        data.size = msgs.length;
        data.unreadCount = unread;
        data.setFlags(flags);
        data.setTags(new Tag.NormalizedTags(tags));
        data.metadata = encodeMetadata(DEFAULT_COLOR_RGB, 1, 1, extended, new SenderList(msgs));
        data.contentChanged(mbox);
        new DbMailItem(mbox).create(data);

        Conversation conv = new Conversation(mbox, data);
        conv.finishCreation(null);

        DbMailItem.setParent(msgs, conv);
        for (int i = 0; i < msgs.length; i++) {
            mbox.markItemModified(msgs[i], Change.PARENT);
            msgs[i].state.setParentId(id);
            msgs[i].metadataChanged();
        }
        return conv;
    }

    void open(String hash) throws ServiceException {
        DbMailItem.openConversation(hash, this);
    }

    void close(String hash) throws ServiceException {
        DbMailItem.closeConversation(hash, this);
    }

    @Override
    void detach() throws ServiceException {
        close(Mailbox.getHash(getNormalizedSubject()));
    }

    /** Updates the unread state of all messages in the conversation.
     *  Persists the change to the database and cache, and also updates
     *  the unread counts for the affected items' {@link Folder}s and
     *  {@link Tag}s appropriately.<p>
     *
     *  Messages in the conversation are omitted from this operation if
     *  one or more of the following applies:<ul>
     *     <li>The caller lacks {@link ACL#RIGHT_WRITE} permission on
     *         the <code>Message</code>.
     *     <li>The caller has specified a {@link MailItem.TargetConstraint}
     *         that explicitly excludes the <code>Message</code>.
     *     <li>The caller has specified the maximum change number they
     *         know about, and the (modification/content) change number on
     *         the <code>Message</code> is greater.</ul>
     *  As a result of all these constraints, no messages may actually be
     *  marked read/unread.
     *
     * @perms {@link ACL#RIGHT_WRITE} on all the messages */
    @Override
    void alterUnread(boolean unread) throws ServiceException {
        markItemModified(Change.UNREAD);

        boolean excludeAccess = false;

        // Decrement the in-memory unread count of each message.  Each message will
        // then implicitly decrement the unread count for its conversation, folder
        // and tags.
        TargetConstraint tcon = mMailbox.getOperationTargetConstraint();
        List<Integer> targets = new ArrayList<Integer>();
        for (Message msg : getMessages()) {
            // skip messages that don't need to be changed, or that the client can't modify, doesn't know about, or has explicitly excluded
            if (msg.isUnread() == unread ) {
                continue;
            } else if (!msg.canAccess(ACL.RIGHT_WRITE)) {
                excludeAccess = true;  continue;
            } else if (!msg.checkChangeID() || !TargetConstraint.checkItem(tcon, msg)) {
                continue;
            }

            int delta = unread ? 1 : -1;
            msg.updateUnread(delta, msg.isTagged(Flag.FlagInfo.DELETED) ? delta : 0);
            msg.metadataChanged();
            targets.add(msg.getId());
        }

        // mark the selected messages in this conversation as read in the database
        if (targets.isEmpty()) {
            if (excludeAccess) {
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions");
            }
        } else {
            DbMailItem.alterUnread(mMailbox, targets, unread);
        }
    }

    /** Tags or untags all messages in the conversation.  Persists the change
     *  to the database and cache.  If the conversation includes at least one
     *  unread {@link Message} whose tagged state is changing, updates the
     *  {@link Tag}'s unread count appropriately.<p>
     *
     *  Messages in the conversation are omitted from this operation if
     *  one or more of the following applies:<ul>
     *     <li>The caller lacks {@link ACL#RIGHT_WRITE} permission on
     *         the <code>Message</code>.
     *     <li>The caller has specified a {@link MailItem.TargetConstraint}
     *         that explicitly excludes the <code>Message</code>.
     *     <li>The caller has specified the maximum change number they
     *         know about, and the (modification/content) change number on
     *         the <code>Message</code> is greater.</ul>
     *  As a result of all these constraints, no messages may actually be
     *  tagged/untagged.
     *
     * @perms {@link ACL#RIGHT_WRITE} on all the messages */
    @Override
    void alterTag(Tag tag, boolean add) throws ServiceException {
        if (tag == null) {
            throw ServiceException.FAILURE("missing tag argument", null);
        }
        if (!add && !isTagged(tag)) {
            return;
        }
        if (tag.getId() == Flag.ID_UNREAD) {
            throw ServiceException.FAILURE("unread state must be set with alterUnread", null);
        }
        // don't let the user tag things as "has attachments" or "draft"
        if (tag instanceof Flag && ((Flag) tag).isSystemFlag()) {
            throw MailServiceException.CANNOT_TAG(tag, this);
        }
        markItemModified(tag instanceof Flag ? Change.FLAGS : Change.TAGS);

        TargetConstraint tcon = mMailbox.getOperationTargetConstraint();
        boolean excludeAccess = false;

        List<Message> msgs = getMessages();
        List<Integer> targets = new ArrayList<Integer>(msgs.size());
        for (Message msg : msgs) {
            // skip messages that don't need to be changed, or that the client can't modify, doesn't know about, or has explicitly excluded
            if (msg.isTagged(tag) == add) {
                continue;
            } else if (!msg.canAccess(ACL.RIGHT_WRITE)) {
                excludeAccess = true;  continue;
            } else if (!msg.checkChangeID() || !TargetConstraint.checkItem(tcon, msg)) {
                continue;
            } else if (add && !tag.canTag(msg)) {
                throw MailServiceException.CANNOT_TAG(tag, this);
            }

            targets.add(msg.getId());
            msg.tagChanged(tag, add);

            // since we're adding/removing a tag, the tag's unread count may change
            int delta = add ? 1 : -1;
            if (tag.trackUnread() && msg.isUnread()) {
                tag.updateUnread(delta, isTagged(Flag.FlagInfo.DELETED) ? delta : 0);
            }

            // if we're adding/removing the \Deleted flag, update the folder and tag "deleted" and "deleted unread" counts
            if (tag.getId() == Flag.ID_DELETED) {
                getFolder().updateSize(0, delta, 0);
                // note that Message.updateUnread() calls updateTagUnread()
                if (msg.isUnread()) {
                    msg.updateUnread(0, delta);
                }
            }
        }

        if (targets.isEmpty()) {
            if (excludeAccess) {
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions");
            }
        } else {
            if (ZimbraLog.mailop.isDebugEnabled()) {
                String operation = add ? "Setting" : "Unsetting";
                ZimbraLog.mailop.debug("%s %s for %s.  Affected ids: %s",
                    operation, getMailopContext(tag), getMailopContext(this), StringUtil.join(",", targets));
            }
            recalculateCounts(msgs);
            DbTag.alterTag(tag, targets, add);
        }
    }

    @Override
    protected void inheritedTagChanged(Tag tag, boolean add) throws ServiceException {
        if (tag == null || add == isTagged(tag)) {
            return;
        }
        markItemModified(tag instanceof Flag ? Change.FLAGS : Change.TAGS);
        if (add) {
            tagChanged(tag, add);
        } else {
            DbMailItem.completeConversation(mMailbox, mMailbox.getOperationConnection(), state.getUnderlyingData());
        }
    }

    protected void inheritedCustomDataChanged(Message msg, CustomMetadata custom) throws ServiceException {
        if (custom == null) {
            return;
        }
        markItemModified(Change.METADATA);
        if (!custom.isEmpty()) {
            mExtendedData = MetadataCallback.duringConversationAdd(mExtendedData, msg);
            saveMetadata();
        } else {
            recalculateMetadata();
        }
    }

    /** Moves all the conversation's {@link Message}s to a different
     *  {@link Folder}.  Persists the change to the database and the in-memory
     *  cache.  Updates all relevant unread counts, folder sizes, etc.<p>
     *
     *  Messages moved to the Trash folder are automatically marked read.
     *  Conversations moved to the Junk folder will not receive newly-delivered
     *  messages.<p>
     *
     *  Messages in the conversation are omitted from this operation if
     *  one or more of the following applies:<ul>
     *     <li>The caller lacks {@link ACL#RIGHT_WRITE} permission on
     *         the <code>Message</code>.
     *     <li>The caller has specified a {@link MailItem.TargetConstraint}
     *         that explicitly excludes the <code>Message</code>.
     *     <li>The caller has specified the maximum change number they
     *         know about, and the (modification/content) change number on
     *         the <code>Message</code> is greater.</ul>
     *  As a result of all these constraints, no messages may actually be
     *  moved.
     *
     * @perms {@link ACL#RIGHT_INSERT} on the target folder,
     *        {@link ACL#RIGHT_DELETE} on the messages' source folders */
    @Override
    boolean move(Folder target) throws ServiceException {
        if (!target.canContain(Type.MESSAGE)) {
            throw MailServiceException.CANNOT_CONTAIN();
        }
        markItemModified(Change.NONE);

        List<Message> msgs = getMessages();
        TargetConstraint tcon = mMailbox.getOperationTargetConstraint();
        boolean toTrash = target.inTrash();
        int oldUnread = 0;
        for (Message msg : msgs) {
            if (msg.isUnread()) {
                oldUnread++;
            }
        }
        // if mData.unread is wrong, what to do?  right now, always use the calculated value
        state.setUnreadCount(oldUnread);

        boolean excludeAccess = false;

        List<Integer> markedRead = new ArrayList<Integer>();
        List<Message> moved = new ArrayList<Message>();
        List<MailItem> indexUpdated = new ArrayList<MailItem>();

        for (Message msg : msgs) {
            Folder source = msg.getFolder();

            // skip messages that don't need to be moved, or that the client can't modify, doesn't know about, or has explicitly excluded
            if (source.getId() == target.getId()) {
                continue;
            } else if (!source.canAccess(ACL.RIGHT_DELETE)) {
                excludeAccess = true;  continue;
            } else if (target.getId() != Mailbox.ID_FOLDER_TRASH && target.getId() != Mailbox.ID_FOLDER_SPAM && !target.canAccess(ACL.RIGHT_INSERT)) {
                excludeAccess = true;  continue;
            } else if (!msg.checkChangeID() || !TargetConstraint.checkItem(tcon, msg)) {
                continue;
            }

            boolean isDeleted = msg.isTagged(Flag.FlagInfo.DELETED);
            if (msg.isUnread()) {
                if (!toTrash || msg.inTrash()) {
                    source.updateUnread(-1, isDeleted ? -1 : 0);
                    target.updateUnread(1, isDeleted ? 1 : 0);
                } else {
                    // unread messages moved from Mailbox to Trash need to be marked read:
                    //   update cached unread counts (message, conversation, folder, tags)
                    msg.updateUnread(-1, isDeleted ? -1 : 0);
                    //   note that we need to update this message in the DB
                    markedRead.add(msg.getId());
                }
            }

            // moved an item out of the spam folder, need to index it
            if (msg.inSpam() && !target.inSpam() && msg.getIndexStatus() != IndexStatus.NO) {
                indexUpdated.add(msg);
            }

            // if a draft is being moved to Trash then remove any "send-later" info from it
            if (toTrash && msg.isDraft()) {
                msg.setDraftAutoSendTime(0);
            }

            // handle folder message counts
            source.updateSize(-1, isDeleted ? -1 : 0, -msg.getTotalSize());
            target.updateSize(1, isDeleted ? 1 : 0, msg.getTotalSize());

            moved.add(msg);
        }

        // mark unread messages moved from Mailbox to Trash/Spam as read in the DB
        if (!markedRead.isEmpty()) {
            DbMailItem.alterUnread(target.getMailbox(), markedRead, false);
        }

        if (moved.isEmpty()) {
            if (excludeAccess)
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions");
        } else {
            // moving a conversation to spam closes it
            if (target.inSpam()) {
                detach();
            }
            if (ZimbraLog.mailop.isInfoEnabled()) {
                StringBuilder ids = new StringBuilder();
                for (int i = 0; i < moved.size(); i++) {
                    ids.append(i > 0 ? "," : "").append(moved.get(i).getId());
                }
                ZimbraLog.mailop.info("Moving %s to %s.  Affected message ids: %s.",
                    getMailopContext(this), getMailopContext(target), ids);
            }
            DbMailItem.setFolder(moved, target);
            for (Message msg : moved) {
                msg.folderChanged(target, 0);
            }

            if (!indexUpdated.isEmpty()) {
                for (MailItem msg : indexUpdated) {
                    mMailbox.index.add(msg);
                }
            }
        }

        return !moved.isEmpty();
    }

    /** please call this *after* adding the child row to the DB */
    @Override
    void addChild(MailItem child) throws ServiceException {
        if (!(child instanceof Message)) {
            throw MailServiceException.CANNOT_PARENT();
        }
        Message msg = (Message) child;

        super.addChild(msg);

        // update inherited flags
        int oldFlags = state.getFlags();
        state.setFlags(oldFlags | msg.getInternalFlagBitmask());
        if (state.getFlags() != oldFlags) {
            markItemModified(Change.FLAGS);
        }

        // update inherited tags
        String[] msgTags = msg.state.getTags();
        if (msgTags.length > 0) {
            Set<String> tags = Sets.newHashSet(state.getTags());
            int oldCount = tags.size();
            for (String msgTag : msgTags) {
                tags.add(msgTag);
            }
            if (tags.size() != oldCount) {
                markItemModified(Change.TAGS);
                state.setTags(new Tag.NormalizedTags(tags));
            }
        }

        // update unread counts
        if (msg.isUnread()) {
            markItemModified(Change.UNREAD);
            updateUnread(child.getUnreadCount(), child.isTagged(Flag.FlagInfo.DELETED) ? child.getUnreadCount() : 0);
        }

        markItemModified(Change.SIZE | Change.SENDERS | Change.METADATA);

        MetadataCallback.duringConversationAdd(mExtendedData, msg);

        // FIXME: this ordering is to work around the fact that when getSenderList has to
        //   recalc the metadata, it uses the already-updated DB message state to do it...
        state.setDate(mMailbox.getOperationTimestamp());
        contentChanged();

        if (!mMailbox.hasListeners(Session.Type.SOAP)) {
            instantiateSenderList();
            state.setSize(state.getSize() + 1);
            try {
                if (mSenderList != null) {
                    mSenderList.add(msg);
                }
            } catch (SenderList.RefreshException slre) {
                mSenderList = null;
            }
            saveMetadata();
        } else {
            boolean recalculated = loadSenderList();
            if (!recalculated) {
                state.setSize(state.getSize() + 1);
                try {
                    mSenderList.add(msg);
                    saveMetadata();
                } catch (SenderList.RefreshException slre) {
                    recalculateMetadata();
                }
            }
        }
    }

    @Override
    void removeChild(MailItem child) throws ServiceException {
        super.removeChild(child);

        // remove the last message and the conversation goes away
        if (getMessageCount() == 0) {
            delete();
            return;
        }

        markItemModified(Change.SIZE | Change.SENDERS);

        if (!mMailbox.hasListeners(Session.Type.SOAP)) {
            // update unread counts
            if (child.isUnread()) {
                markItemModified(Change.UNREAD);
                updateUnread(-child.getUnreadCount(), child.isTagged(Flag.FlagInfo.DELETED) ? -child.getUnreadCount() : 0);
            }

            // update inherited tags, if applicable
            if (child.state.getTags().length != 0 || child.state.getFlags() != 0) {
                int oldFlags = state.getFlags();
                int oldTagCount = state.getTags().length;

                DbMailItem.completeConversation(mMailbox, mMailbox.getOperationConnection(), state.getUnderlyingData());

                if (state.getFlags() != oldFlags) {
                    markItemModified(Change.FLAGS);
                }
                if (state.getTags().length != oldTagCount) {
                    markItemModified(Change.TAGS);
                }
            }

            mEncodedSenders = null;
            mSenderList = null;
            state.setSize(state.getSize() - 1);
            saveMetadata(null);
        } else {
            List<Message> msgs = getMessages();
            msgs.remove(child);
            recalculateMetadata(msgs);
        }
    }

    void merge(Conversation other) throws ServiceException {
        if (other == this) {
            return;
        }
        // make sure to add conversation to dirty list before mucking with it
        //   (the actual dirty mask gets applied during recalculateMetadata)
        markItemModified(Change.INTERNAL_ONLY);

        for (Message msg : other.getMessages()) {
            msg.markItemModified(Change.PARENT);
            msg.state.setParentId(mId);
            MetadataCallback.duringConversationAdd(mExtendedData, msg);
        }
        DbMailItem.reparentChildren(other, this);
        DbMailItem.changeOpenTargets(other, getId());

        recalculateMetadata();

        // delete the old conversation (must do this after moving the messages because of cascading delete)
        if (other instanceof VirtualConversation) {
            other.removeChild(((VirtualConversation) other).getMessage());
        } else {
            other.delete();
        }
    }

    /** Determines the set of {@link Message}s to be deleted from this
     *  <code>Conversation</code>.  Assembles a new {@link PendingDelete}
     *  object encapsulating the data on the items to be deleted.<p>
     *
     *  A message will be deleted unless:<ul>
     *     <li>The caller lacks {@link ACL#RIGHT_DELETE} permission on
     *         the <code>Message</code>.
     *     <li>The caller has specified a {@link MailItem.TargetConstraint}
     *         that explicitly excludes the <code>Message</code>.
     *     <li>The caller has specified the maximum change number they
     *         know about, and the (modification/content) change number on
     *         the <code>Message</code> is greater.</ul>
     *  As a result of all these constraints, no messages may actually be
     *  deleted.
     *
     * @throws ServiceException The following error codes are possible:<ul>
     *    <li><code>mail.MODIFY_CONFLICT</code> - if the caller specified a
     *        max change number and a modification check, and the modified
     *        change number of the <code>Message</code> is greater
     *    <li><code>service.FAILURE</code> - if there's a database
     *        failure fetching the message list</ul> */
    @Override
    PendingDelete getDeletionInfo() throws ServiceException {
        PendingDelete info = new PendingDelete();
        info.itemIds.add(getType(), mId, uuid);

        if (state.getSize() == 0) {
            return info;
        }

        List<Message> msgs = getMessages();
        TargetConstraint tcon = mMailbox.getOperationTargetConstraint();

        boolean excludeModify = false, excludeAccess = false;
        for (Message child : msgs) {
            // silently skip explicitly excluded messages, PERMISSION_DENIED messages, and MODIFY_CONFLICT messages
            if (!TargetConstraint.checkItem(tcon, child)) {
                continue;
            } else if (!child.canAccess(ACL.RIGHT_DELETE)) {
                excludeAccess = true;
            } else if (!child.checkChangeID()) {
                excludeModify = true;
            } else {
                info.add(child.getDeletionInfo());
            }
        }

        int totalDeleted = info.itemIds.size();
        if (totalDeleted == 1) {
            // if all messages have been excluded, some for "error" reasons, throw an exception
            if (excludeAccess) {
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions");
            } else if (excludeModify) {
                throw MailServiceException.MODIFY_CONFLICT();
            }
        }
        if (totalDeleted != msgs.size() + 1) {
            info.incomplete = true;
        }
        return info;
    }

    @Override
    void purgeCache(PendingDelete info, boolean purgeItem) throws ServiceException {
        // if *some* of the messages remain, recalculate the data based on this
        if (info.incomplete) {
            recalculateMetadata();
        }

        super.purgeCache(info, purgeItem);
    }


    @Override
    void decodeMetadata(String metadata) {
        // when a folder is deleted, DbMailItem.markDeletionTargets() nulls out metadata for all affected conversations
        //   in that case, leave mSenderList unset and fault it in as necessary
        if (metadata != null) {
            try {
                Metadata meta = new Metadata(metadata, mId);
                if (meta.containsKey(Metadata.FN_PARTICIPANTS)) {
                    decodeMetadata(meta);
                }
            } catch (ServiceException e) {
                ZimbraLog.mailbox.error("Failed to parse metadata id=%d,type=%s", mId, getType(), e);
            }
        }
    }

    @Override
    void decodeMetadata(Metadata meta) throws ServiceException {
        super.decodeMetadata(meta);
        mEncodedSenders = meta.get(Metadata.FN_PARTICIPANTS, null);
    }

    @Override
    Metadata encodeMetadata(Metadata meta) {
        String encoded = mEncodedSenders;
        if (encoded == null && mSenderList != null) {
            encoded = mSenderList.toString();
        }
        return encodeMetadata(meta, state.getColor(), state.getMetadataVersion(), state.getVersion(), mExtendedData, encoded);
    }

    static String encodeMetadata(Color color, int metaVersion, int version, CustomMetadataList extended, SenderList senders) {
        return encodeMetadata(new Metadata(), color, metaVersion, version, extended, senders.toString()).toString();
    }

    static Metadata encodeMetadata(Metadata meta, Color color, int metaVersion, int version, CustomMetadataList extended, String encodedSenders) {
        meta.put(Metadata.FN_PARTICIPANTS, encodedSenders);
        return MailItem.encodeMetadata(meta, color, null, metaVersion, version, extended);
    }

    /**
     * Overrides the default value of {@code true}, to handle the
     * {@code zimbraMailAllowReceiveButNotSendWhenOverQuota} account
     * attribute.
     */
    @Override
    protected boolean isQuotaCheckRequired() throws ServiceException {
        Account account = getMailbox().getAccount();
        return !account.isMailAllowReceiveButNotSendWhenOverQuota();
    }

    @Override
    public String toString() {
        return appendCommonMembers(MoreObjects.toStringHelper(this)).toString();
    }
}
