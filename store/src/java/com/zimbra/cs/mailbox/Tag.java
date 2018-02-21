/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.base.CharMatcher;
import com.google.common.base.Objects;
import com.google.common.collect.Sets;
import com.zimbra.common.mailbox.Color;
import com.zimbra.common.mailbox.ZimbraTag;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ArrayUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbTag;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.soap.mail.type.RetentionPolicy;

/**
 * @since Jul 12, 2004
 */
public class Tag extends MailItem implements ZimbraTag {
    public static class NormalizedTags {
        private static final String[] NO_TAGS = new String[0];

        private String[] tags;

        NormalizedTags(Mailbox mbox, String[] tagsFromClient) throws ServiceException {
            this(mbox, tagsFromClient, true);
        }

        NormalizedTags(Mailbox mbox, String[] tagsFromClient, boolean create) throws ServiceException {
            this(mbox, tagsFromClient, create, false);
        }

        NormalizedTags(Mailbox mbox, String[] tagsFromClient, boolean create, boolean imapVisible) throws ServiceException {
            assert mbox.isTransactionActive() : "cannot instantiate NormalizedTags outside of a transaction";

            if (ArrayUtil.isEmpty(tagsFromClient)) {
                this.tags = NO_TAGS;
            } else {
                Set<Tag> tlist = Sets.newLinkedHashSet();
                for (String tag : tagsFromClient) {
                    try {
                        tlist.add(mbox.getTagByName(tag));
                        continue;
                    } catch (NoSuchItemException nsie) { }

                    if (create) {
                        try {
                            Tag newTag = mbox.createTagInternal(Mailbox.ID_AUTO_INCREMENT, tag, new Color(DEFAULT_COLOR), false);
                            if (imapVisible) {
                                newTag.setIsImapVisible(true);
                            }
                            tlist.add(newTag);
                            continue;
                        } catch (ServiceException e) {
                            if (!e.getCode().equals(MailServiceException.ALREADY_EXISTS)) {
                                throw e;
                            }
                        }
                    }
                }

                this.tags = new String[tlist.size()];
                int i = 0;
                for (Tag t : tlist) {
                    this.tags[i++] = t.getName();
                }
            }
        }

        public NormalizedTags(Collection<String> tagsFromDB) {
            this(tagsFromDB == null ? null : tagsFromDB.toArray(new String[tagsFromDB.size()]));
        }

        public NormalizedTags(String[] tagsFromDB) {
            this.tags = tagsFromDB == null ? NO_TAGS : tagsFromDB;
        }

        String[] getTags() {
            return tags;
        }

        @Override
        public String toString() {
            return tags.toString();
        }
    }

    // Note space reserved in the Flag number-space.
    // This is used for a pseudo non-existent tag.  Normally one defined for a mailbox of a shared folder, but
    // also used in searches for a tag which doesn't exist.
    public static final int NONEXISTENT_TAG = -32;

    private boolean isListed;

    // If this is true, an a newly-created unlisted tag will still be returned as a pending remote modification
    private boolean isImapVisible = false;

    private RetentionPolicy retentionPolicy;

    Tag(Mailbox mbox, UnderlyingData ud) throws ServiceException {
        this(mbox, ud, false);
    }

    Tag(Mailbox mbox, UnderlyingData ud, boolean skipCache) throws ServiceException {
        super(mbox, ud, skipCache);
        init();
    }

    Tag(Account acc, UnderlyingData data, int mailboxId) throws ServiceException {
        super(acc, data, mailboxId);
        init();
    }

    private void init() throws ServiceException {
        if (mData.type != Type.TAG.toByte() && mData.type != Type.FLAG.toByte() && mData.type != Type.SMARTFOLDER.toByte()) {
            throw new IllegalArgumentException();
        }
        if (retentionPolicy == null) {
            // Retention policy is initialized in Tag's encodeMetadata(), but
            // not in Flag's.
            retentionPolicy = new RetentionPolicy();
        }
    }

    @Override
    public String getSender() {
        return "";
    }

    public int getItemCount() {
        return (int) getSize();
    }

    /** Updates the number of items with the tag and their total size.
     *  <i>(Total size not currently tracked.)</i>
     * @param countDelta    The change in item count, negative or positive.
     * @param deletedDelta  The change in number of IMAP \Deleted items.*/
    void updateSize(int countDelta, int deletedDelta) throws ServiceException {
        int delta = countDelta - deletedDelta;
        if (delta == 0 || !trackUnread()) {
            return;
        }
        markItemModified(Change.SIZE);
        // if we go negative, that's OK!  just pretend we're at 0.
        mData.size = Math.max(0, mData.size + delta);
    }

    @Override
    protected void updateUnread(int delta, int deletedDelta) throws ServiceException {
        // we track unread un-\Deleted items only
        super.updateUnread(delta - deletedDelta, 0);
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
        return false;
    }

    @Override
    boolean isMutable() {
        return true;
    }

    @Override
    boolean canHaveChildren() {
        return false;
    }

    boolean canTag(MailItem item) {
        return item.isTaggable();
    }

    /** Returns the retention policy for this tag.  Does not return {@code null}. */
    public RetentionPolicy getRetentionPolicy() {
        return retentionPolicy;
    }

    public void setRetentionPolicy(RetentionPolicy rp) throws ServiceException {
        if (!canAccess(ACL.RIGHT_ADMIN)) {
            throw ServiceException.PERM_DENIED("you do not have admin rights to tag " + getName());
        }

        markItemModified(Change.RETENTION_POLICY);
        retentionPolicy = rp == null ? new RetentionPolicy() : rp;
        saveMetadata();
    }

    public boolean isListed() {
        return isListed;
    }

    void setListed() throws ServiceException {
        if (!isListed) {
            isListed = true;
            saveMetadata();
        }
    }

    static Tag create(Mailbox mbox, int id, String requestedName, Color color, boolean listed) throws ServiceException {
        String name = validateItemName(requestedName);
        try {
            // if we can successfully get a tag with that name, we've got a naming conflict
            mbox.getTagByName(name);
            throw MailServiceException.ALREADY_EXISTS(name);
        } catch (MailServiceException.NoSuchItemException nsie) {}

        UnderlyingData data = new UnderlyingData();
        data.id = id;
        data.type = Type.TAG.toByte();
        data.folderId = Mailbox.ID_FOLDER_TAGS;
        data.name = name;
        data.setSubject(name);
        data.metadata = encodeMetadata(color, 1, 1, null, listed);
        data.contentChanged(mbox);
        ZimbraLog.mailop.info("Adding Tag %s: id=%d.", name, data.id);
        DbTag.createTag(mbox, data, color, listed);

        Tag tag = new Tag(mbox, data);
        tag.finishCreation(null);
        return tag;
    }

    /**
     * Create a minimal Tag object but don't persist any equivalent of it to the mail store.
     * This is useful when searching for remote tags (e.g. By clicking on a remote tag on an item in a
     * shared folder - see Bug 77646)
     */
    public static Tag createPseudoRemoteTag(Mailbox mbox, String requestedName) throws ServiceException {
        String name = validateItemName(requestedName);
        try {
            // if we can successfully get a tag with that name, we've got a naming conflict
            mbox.getTagByName(name);
            throw MailServiceException.ALREADY_EXISTS(name);
        } catch (MailServiceException.NoSuchItemException nsie) {}

        UnderlyingData data = new UnderlyingData();
        data.id = NONEXISTENT_TAG; /* faked */
        data.type = Type.TAG.toByte();
        data.folderId = Mailbox.ID_FOLDER_TAGS;
        data.name = name;
        data.setSubject(name);
        data.metadata = null;
        // Need to keep this out of the cache so that the user is still able to add a local tag with the same name after
        // having clicked on a remote tag.
        data.setFlags(data.getFlags() | Flag.BITMASK_UNCACHED);
        Tag tag = new Tag(mbox, data);
        return tag;
    }


    /** Updates the unread state of all items with the tag.  Persists the
     *  change to the database and cache, and also updates the unread counts
     *  for the tag and the affected items' parents and {@link Folder}s
     *  appropriately.  <i>Note: Tags may only be marked read, not unread.</i>
     *
     * @perms {@link ACL#RIGHT_READ} on the folder,
     *        {@link ACL#RIGHT_WRITE} on all affected messages. */
    @Override
    void alterUnread(boolean unread) throws ServiceException {
        if (unread) {
            throw ServiceException.INVALID_REQUEST("tags can only be marked read", null);
        }

        // decrement the in-memory unread count of each message.  each message will
        // then implicitly decrement the unread count for its conversation, folder
        // and tags.
        List<Integer> targets = new ArrayList<Integer>();
        int delta = unread ? 1 : -1;
        for (UnderlyingData data : DbTag.getUnreadMessages(this)) {
            Message msg = mMailbox.getMessage(data);
            if (msg.checkChangeID() || !msg.canAccess(ACL.RIGHT_WRITE)) {
                msg.updateUnread(delta, msg.isTagged(Flag.FlagInfo.DELETED) ? delta : 0);
                msg.metadataChanged();
                targets.add(msg.getId());
            }
        }

        // Mark all messages with this tag as read in the database
        DbMailItem.alterUnread(mMailbox, targets, unread);
    }

    static final String FLAG_NAME_PREFIX = "\\";

    //extends flag prefix to avoid potential name collisions with existing tags
    static final String SMARTFOLDER_NAME_PREFIX = "\\\\";

    private static final CharMatcher INVALID_TAG_CHARS = CharMatcher.anyOf(":\\");

    static String validateItemName(String name) throws ServiceException {
        // reject invalid characters in the name
        if (name == null || name != StringUtil.stripControlCharacters(name) || INVALID_TAG_CHARS.matchesAnyOf(name)) {
            throw MailServiceException.INVALID_NAME(name);
        }
        // strip trailing whitespace and validate length of resulting name
        String trimmed = name.trim().replace('\t', ' ').replace('\r', ' ').replace('\n', ' ');
        if (trimmed.isEmpty() || trimmed.length() > MAX_NAME_LENGTH || trimmed.startsWith(FLAG_NAME_PREFIX)) {
            throw MailServiceException.INVALID_NAME(name);
        }
        return trimmed;
    }

    /** Overrides {@link MailItem#rename(String, Folder) to update filter rules
     *  if necessary. */
    @Override
    void rename(String name, Folder target) throws ServiceException {
        String originalName = getName(), newName = validateItemName(name);

        if (target.getId() != Mailbox.ID_FOLDER_TAGS) {
            throw MailServiceException.CANNOT_CONTAIN();
        } else if (originalName.equals(newName)) {
            return;
        } else if (!canAccess(ACL.RIGHT_WRITE)) {
            throw ServiceException.PERM_DENIED("you do not have the required rights on the item");
        }

        if (ZimbraLog.mailop.isDebugEnabled()) {
            ZimbraLog.mailop.debug("renaming " + getMailopContext(this) + " to " + newName);
        }

        // actually rename the tag
        markItemModified(Change.NAME);
        mData.name = newName;
        contentChanged();
        DbTag.renameTag(this);
        // dump entire item cache because tag names on cached items are now stale
        mMailbox.purge(Type.MESSAGE);
        // any folder that contains items might have seen some of its contents change
        touchAllFolders();
    }

    @Override
    void purgeCache(PendingDelete info, boolean purgeItem) throws ServiceException {
        ZimbraLog.mailop.debug("Removing %s from all items.", getMailopContext(this));
        // remove the tag from all items in the database
        DbTag.deleteTag(this);
        // any folder that contains items might have seen some of its contents change
        touchAllFolders();
        // dump entire item cache because tag names on cached items are now stale
        mMailbox.purge(Type.MESSAGE);
        // remove tag from tag cache
        super.purgeCache(info, purgeItem);
    }

    /** Updates the change highwater mark for all non-empty folders.  Renaming
     *  or deleting a tag may or may not touch items in any or all folders.
     *  Since we can't easily tell which folders would be affected, we just
     *  update the highwater mark for *all* folders. */
    void touchAllFolders() throws ServiceException {
        for (Folder folder : mMailbox.listAllFolders()) {
            if (folder.getItemCount() > 0) {
                folder.updateHighestMODSEQ();
            }
        }
    }

    /** Persists the tag's current unread count to the database. */
    protected void saveTagCounts() throws ServiceException {
        DbTag.persistCounts(this);
        ZimbraLog.mailbox.debug("\"%s\": updating tag counts (s%d/u%d)", getName(), (int) mData.size, mData.unreadCount);
    }

    @Override
    protected void saveMetadata(String ignored) throws ServiceException {
        metadataChanged();
        DbTag.saveMetadata(this);
    }

    @Override
    void decodeMetadata(Metadata meta) throws ServiceException {
        super.decodeMetadata(meta);

        isListed = meta.getBool(Metadata.FN_LISTED, false);

        Metadata rp = meta.getMap(Metadata.FN_RETENTION_POLICY, true);
        if (rp != null) {
            retentionPolicy = RetentionPolicyManager.retentionPolicyFromMetadata(rp, true);
        } else {
            retentionPolicy = new RetentionPolicy();
        }
    }

    @Override
    Metadata encodeMetadata(Metadata meta) {
        return encodeMetadata(meta, mRGBColor, mMetaVersion, mVersion, retentionPolicy, isListed);
    }

    public static String encodeMetadata(Color color, int metaVersion, int version, RetentionPolicy rp, boolean listed) {
        return encodeMetadata(new Metadata(), color, metaVersion, version, rp, listed).toString();
    }

    static Metadata encodeMetadata(Metadata meta, Color color, int metaVersion, int version, RetentionPolicy rp, boolean listed) {
        MailItem.encodeMetadata(meta, color, null, metaVersion, version, null);

        if (rp != null && rp.isSet()) {
            meta.put(Metadata.FN_RETENTION_POLICY, RetentionPolicyManager.toMetadata(rp, true));
        }
        meta.put(Metadata.FN_LISTED, listed);

        return meta;
    }

    @Override
    public String toString() {
        Objects.ToStringHelper helper = Objects.toStringHelper(this);
        appendCommonMembers(helper);
        return helper.toString();
    }

    @Override
    public int getTagId() {
        return getId();
    }

    @Override
    public String getTagName() {
        return getName();
    }

    public void setIsImapVisible(boolean visible) {
        isImapVisible = visible;
    }

    public boolean isImapVisible()  {
        return isImapVisible;
    }
}
