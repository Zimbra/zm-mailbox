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
package com.zimbra.cs.mailbox;

import com.google.common.base.Objects;
import com.zimbra.common.mailbox.Color;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.mailbox.MailItem.CustomMetadata.CustomMetadataList;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.session.PendingModifications.Change;

/**
 * @since Jul 3, 2005
 */
public class Mountpoint extends Folder {

    private String mOwnerId;
    private int    mRemoteId;
    private boolean mReminderEnabled;

    Mountpoint(Mailbox mbox, UnderlyingData ud) throws ServiceException {
        super(mbox, ud);
    }

    /** Returns the <code>zimbraId</code> of the remote shared item's
     *  mailbox's owner.
     *
     * @see Mailbox#getAccountId() */
    public String getOwnerId() {
        return mOwnerId;
    }

    /** Returns the numeric item id of the remote shared item pointed to by
     *  this <code>Mountpoint</code>.
     *
     * @see MailItem#getId() */
    public int getRemoteId() {
        return mRemoteId;
    }

    /** Returns the {@link ItemId} of the remote shared item referenced by
     *  this <code>Mountpoint</code>.
     *
     * @see Mountpoint#getOwnerId()
     * @see Mountpoint#getRemoteId() */
    public ItemId getTarget() {
        return new ItemId(mOwnerId, mRemoteId);
    }

    /** Returns true if reminders are enabled on the shared calendar.
     *
     * @return
     */
    public boolean isReminderEnabled() {
        return mReminderEnabled;
    }

    /** @return TRUE if this mountpoint points to its owner's mailbox */
    public boolean isLocal() {
        return (getOwnerId().equals(getMailbox().getAccountId()));
    }

    /** Grants the specified set of rights to the target and persists them
     *  to the database.  <i>Note: This function will always throw the
     *  <code>service.PERM_DENIED</code> {@link ServiceException} because
     *  <code>Mountpoint</code>s may not be re-shared.</i> */
    void grantAccess(String liquidId, byte type, short rights, boolean inherit) throws ServiceException {
        throw ServiceException.PERM_DENIED("you may not share the mounted folder " + getPath());
    }

    /** Removes the set of rights granted to the specified (id, type) pair
     *  and updates the database accordingly.  <i>Note: This function does
     *  nothing, as you cannot re-share a <code>Mountpoint</code> in the
     *  first place.</i>
     *
     * @see #grantAccess(String, byte, short, boolean) */
    @Override void revokeAccess(String liquidId) {
        return;
    }

    @Override boolean canHaveChildren() { return false; }
    @Override boolean trackUnread()     { return false; }
    @Override boolean canParent(MailItem child)  { return false; }
    @Override boolean canContain(MailItem child) { return false; }
    @Override boolean canContain(MailItem.Type type) { return false; }

    /** Creates a new Mountpoint pointing at a remote item and persists it
     *  to the database.  A real nonnegative item ID must be supplied from
     *  a previous call to {@link Mailbox#getNextItemId(int)}.
     *
     * @param id        The id for the new mountpoint.
     * @param parent    The folder to place the new mountpoint in.
     * @param name      The new mountpoint's name.
     * @param ownerId   The remote mailbox's owner's <code>zimbraId</code>.
     * @param remoteId  The remote item's numeric id.
     * @param view      The (optional) default object type for the folder.
     * @param flags     Folder flags (e.g. {@link Flag#BITMASK_CHECKED}).
     * @param color     The new mountpoint's color.
     * @param reminderEnabled Whether calendar reminders are enabled
     * @param custom    An optional extra set of client-defined metadata.
     * @perms {@link ACL#RIGHT_INSERT} on the parent folder
     * @throws ServiceException   The following error codes are possible:<ul>
     *    <li><code>mail.CANNOT_CONTAIN</code> - if the target folder
     *        can't contain mountpoints
     *    <li><code>mail.ALREADY_EXISTS</code> - if a folder by that name
     *        already exists in the parent folder
     *    <li><code>mail.INVALID_NAME</code> - if the new folder's name is
     *        invalid
     *    <li><code>mail.INVALID_REQUEST</code> - if the <code>ownerId</code>
     *        or <code>remoteId</code> are invalid
     *    <li><code>service.FAILURE</code> - if there's a database failure
     *    <li><code>service.PERM_DENIED</code> - if you don't have
     *        sufficient permissions</ul>
     * @see #validateItemName(String)
     * @see #canContain(byte) */
    static Mountpoint create(int id, Folder parent, String name, String ownerId, int remoteId, Type view, int flags,
            Color color, boolean reminderEnabled, CustomMetadata custom) throws ServiceException {
        if (parent == null || ownerId == null || remoteId <= 0) {
            throw ServiceException.INVALID_REQUEST("invalid parameters when creating mountpoint", null);
        }
        if (!parent.canAccess(ACL.RIGHT_INSERT)) {
            throw ServiceException.PERM_DENIED("you do not have sufficient permissions on the parent folder");
        }
        if (!parent.canContain(Type.MOUNTPOINT)) {
            throw MailServiceException.CANNOT_CONTAIN();
        }
        name = validateItemName(name);
        if (parent.findSubfolder(name) != null) {
            throw MailServiceException.ALREADY_EXISTS(name);
        }
        Mailbox mbox = parent.getMailbox();

        UnderlyingData data = new UnderlyingData();
        data.id = id;
        data.type = Type.MOUNTPOINT.toByte();
        data.folderId = parent.getId();
        data.parentId = data.folderId;
        data.date = mbox.getOperationTimestamp();
        data.setFlags(flags & Flag.FLAGS_FOLDER);
        data.name = name;
        data.setSubject(name);
        data.metadata = encodeMetadata(color, 1, custom, view, ownerId, remoteId, reminderEnabled);
        data.contentChanged(mbox);
        ZimbraLog.mailop.info("Adding Mountpoint %s: id=%d, parentId=%d, parentName=%s.",
                name, data.id, parent.getId(), parent.getName());
        new DbMailItem(mbox).create(data);

        Mountpoint mpt = new Mountpoint(mbox, data);
        mpt.finishCreation(parent);
        return mpt;
    }

    @Override
    void delete(boolean writeTombstones) throws ServiceException {
        if (!getFolder().canAccess(ACL.RIGHT_DELETE))
            throw ServiceException.PERM_DENIED("you do not have sufficient permissions on the parent folder");
        super.delete(writeTombstones);
    }


    @Override
    void decodeMetadata(Metadata meta) throws ServiceException {
        super.decodeMetadata(meta);
        mOwnerId = meta.get(Metadata.FN_ACCOUNT_ID);
        mRemoteId = (int) meta.getLong(Metadata.FN_REMOTE_ID);
        mReminderEnabled = meta.getBool(Metadata.FN_REMINDER_ENABLED, false);
    }

    @Override
    Metadata encodeMetadata(Metadata meta) {
        return encodeMetadata(meta, mRGBColor, mVersion, mExtendedData, attributes, defaultView, mOwnerId, mRemoteId, mReminderEnabled);
    }

    private static String encodeMetadata(Color color, int version, CustomMetadata custom, Type view, String owner,
            int remoteId, boolean reminderEnabled) {
        CustomMetadataList extended = (custom == null ? null : custom.asList());
        return encodeMetadata(new Metadata(), color, version, extended, (byte) 0, view, owner, remoteId, reminderEnabled).toString();
    }

    static Metadata encodeMetadata(Metadata meta, Color color, int version, CustomMetadataList extended, byte attrs,
            Type view, String owner, int remoteId, boolean reminderEnabled) {
        meta.put(Metadata.FN_ACCOUNT_ID, owner);
        meta.put(Metadata.FN_REMOTE_ID, remoteId);
        if (reminderEnabled)
            meta.put(Metadata.FN_REMINDER_ENABLED, reminderEnabled);
        return Folder.encodeMetadata(meta, color, version, extended, attrs, view, null, null, 0, 0, 0, 0, 0, 0, 0, null);
    }

    @Override
    public String toString() {
        Objects.ToStringHelper helper = Objects.toStringHelper(this);
        helper.add(CN_NAME, getName());
        appendCommonMembers(helper);
        helper.add(CN_ATTRIBUTES, attributes);
        helper.add("reminder", mReminderEnabled);
        return helper.toString();
    }

    /**
     * Enables/disables showing reminders for items in shared calendar.
     */
    void enableReminder(boolean enable) throws ServiceException {
        if (!isMutable()) {
            throw MailServiceException.IMMUTABLE_OBJECT(mId);
        }
        if (!canAccess(ACL.RIGHT_WRITE)) {
            throw ServiceException.PERM_DENIED("you do not have the required rights on the mountpoint");
        }
        if (mReminderEnabled == enable) {
            return;
        }
        markItemModified(Change.SHAREDREM);
        mReminderEnabled = enable;
        saveMetadata();
    }

    @Override
    protected void checkItemCreationAllowed() throws ServiceException {
        // check nothing
        // external account mailbox can have mountpoints
    }
}
