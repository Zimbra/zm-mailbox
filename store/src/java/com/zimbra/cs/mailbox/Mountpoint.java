/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import com.google.common.base.MoreObjects;
import com.zimbra.common.mailbox.Color;
import com.zimbra.common.mailbox.ItemIdentifier;
import com.zimbra.common.mailbox.MountpointStore;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.mailbox.MailItem.CustomMetadata.CustomMetadataList;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.session.PendingModifications.Change;

/**
 * @since Jul 3, 2005
 */
public class Mountpoint extends Folder implements MountpointStore {

    private String mOwnerId;
    private int    mRemoteId;
    private String mRemoteUuid;
    private boolean mReminderEnabled;

    Mountpoint(Mailbox mbox, UnderlyingData ud) throws ServiceException {
        this(mbox, ud, false);
    }

    Mountpoint(Mailbox mbox, UnderlyingData ud, boolean skipCache) throws ServiceException {
        super(mbox, ud, skipCache);
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

    /** Returns the UUID of the remote shared item pointed to by
     *  this <code>Mountpoint</code>.
     */
    public String getRemoteUuid() {
        return mRemoteUuid;
    }

    /** Returns the {@link ItemId} of the remote shared item referenced by
     *  this <code>Mountpoint</code>.
     *
     * @see Mountpoint#getOwnerId()
     * @see Mountpoint#getRemoteId() */
    public ItemId getTarget() {
        return new ItemId(mOwnerId, mRemoteId);
    }

    @Override
    public ItemIdentifier getTargetItemIdentifier() {
        return getTarget().toItemIdentifier();
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
    static Mountpoint create(int id, String uuid, Folder parent, String name, String ownerId, int remoteId, String remoteUuid,
            Type view, int flags, Color color, boolean reminderEnabled, CustomMetadata custom) throws ServiceException {
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
        data.uuid = uuid;
        data.id = id;
        data.type = Type.MOUNTPOINT.toByte();
        data.folderId = parent.getId();
        data.parentId = data.folderId;
        data.date = mbox.getOperationTimestamp();
        data.setFlags(flags & Flag.FLAGS_FOLDER);
        data.name = name;
        data.setSubject(name);
        data.metadata = encodeMetadata(color, 1, 1, custom, view, ownerId, remoteId, remoteUuid, reminderEnabled);
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
        mRemoteUuid = meta.get(Metadata.FN_REMOTE_UUID, null);
        mReminderEnabled = meta.getBool(Metadata.FN_REMINDER_ENABLED, false);
    }

    @Override
    Metadata encodeMetadata(Metadata meta) {
        return encodeMetadata(meta, mRGBColor, mMetaVersion, mVersion, mExtendedData, attributes, defaultView, mOwnerId, mRemoteId, mRemoteUuid, mReminderEnabled);
    }

    private static String encodeMetadata(Color color, int metaVersion, int version, CustomMetadata custom, Type view, String owner,
            int remoteId, String remoteUuid, boolean reminderEnabled) {
        CustomMetadataList extended = (custom == null ? null : custom.asList());
        return encodeMetadata(new Metadata(), color, metaVersion, version, extended, (byte) 0, view, owner, remoteId, remoteUuid, reminderEnabled).toString();
    }

    static Metadata encodeMetadata(Metadata meta, Color color, int metaVersion, int version, CustomMetadataList extended, byte attrs,
            Type view, String owner, int remoteId, String remoteUuid, boolean reminderEnabled) {
        meta.put(Metadata.FN_ACCOUNT_ID, owner);
        meta.put(Metadata.FN_REMOTE_ID, remoteId);
        meta.put(Metadata.FN_REMOTE_UUID, remoteUuid);
        if (reminderEnabled)
            meta.put(Metadata.FN_REMINDER_ENABLED, reminderEnabled);
        return Folder.encodeMetadata(meta, color, metaVersion, version, extended, attrs, view, null, null, 0, 0, 0, 0, 0, 0, 0, null, false, -1);
    }

    @Override
    public String toString() {
        MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(this);
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

    /** Sets the folder's UIDNEXT item ID highwater mark to one more than
     *  the Mailbox's last assigned item ID. */
    /**
     * Sets the owner and item id of the remote folder this mountpoint points to.
     * @param ownerId account id of the owner of the remote folder
     * @param remoteId numeric item id of the remote folder
     * @throws ServiceException
     */
    void setRemoteInfo(String ownerId, int remoteId) throws ServiceException {
        if (mOwnerId.equalsIgnoreCase(ownerId) && mRemoteId == remoteId) {
            // no change
            return;
        }
        mOwnerId = ownerId;
        mRemoteId = remoteId;
        markItemModified(Change.METADATA);
        saveMetadata();
    }

    @Override
    protected void checkItemCreationAllowed() throws ServiceException {
        // check nothing
        // external account mailbox can have mountpoints
    }
    
    /**
     * Creates an interim new Mountpoint pointing at a remote item sub-folder.
     * It does not persists in the database.
     * This is a temporary holder for sub-folder of remote shared item for active-sync
     * @param id
     * @param uuid
     * @param parent
     * @param name
     * @param ownerId
     * @param remoteId
     * @param remoteUuid
     * @param view
     * @param flags
     * @param color
     * @param reminderEnabled
     * @param custom
     * @throws ServiceException
     */
    public static Mountpoint createInterimMountPointForActiveSync(int id, String uuid, Folder parent, String name, String ownerId, int remoteId, String remoteUuid,
            Type view, int flags, Color color, boolean reminderEnabled, CustomMetadata custom) throws ServiceException {
        if (parent == null || ownerId == null || remoteId <= 0) {
            throw ServiceException.INVALID_REQUEST("invalid parameters when creating mountpoint", null);
        }
        if (!parent.canAccess(ACL.RIGHT_INSERT)) {
            throw ServiceException.PERM_DENIED("you do not have sufficient permissions on the parent folder");
        }
        name = validateItemName(name);
        if (parent.findSubfolder(name) != null) {
            throw MailServiceException.ALREADY_EXISTS(name);
        }
        Mailbox mbox = parent.getMailbox();
        UnderlyingData data = new UnderlyingData();
        data.uuid = uuid;
        data.id = id;
        data.type = Type.MOUNTPOINT.toByte();
        data.folderId = parent.getId();
        data.parentId = data.folderId;
        data.date = mbox.getOperationTimestamp();
        data.setFlags(flags & Flag.FLAGS_FOLDER);
        data.name = name;
        data.setSubject(name);
        data.metadata = encodeMetadata(color, 1, 1, custom, view, ownerId, remoteId, remoteUuid, reminderEnabled);
        data.contentChanged(mbox);
        ZimbraLog.mailop.info("Adding Mountpoint %s: id=%d, parentId=%d, parentName=%s.",
                name, data.id, parent.getId(), parent.getName());
        Mountpoint mpt = new Mountpoint(mbox, data);
        return mpt;
    }

}
