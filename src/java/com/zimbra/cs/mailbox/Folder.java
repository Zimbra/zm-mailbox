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
 * Created on Aug 18, 2004
 */
package com.zimbra.cs.mailbox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;

/**
 * @author dkarp
 */
public class Folder extends MailItem {

    public static final class SyncData {
        String url;
        String lastGuid;
        long   lastDate;

        SyncData(String link)  { url = link; }
        SyncData(String link, String guid, long date) {
            url = link;  lastGuid = guid;  lastDate = date;
        }

        public boolean alreadySeen(String guid, long date)  { return lastDate >= date; }
    }

    public static final byte FOLDER_IS_IMMUTABLE      = 0x01;
    public static final byte FOLDER_DONT_TRACK_COUNTS = 0x02;

    protected byte    mAttributes;
    protected byte    mDefaultView;
    private List<Folder> mSubfolders;
    private Folder    mParent;
    private ACL       mRights;
    private SyncData  mSyncData;
    private int       mImapUIDNEXT;

    Folder(Mailbox mbox, UnderlyingData ud) throws ServiceException {
        super(mbox, ud);
        if (mData.type != TYPE_FOLDER && mData.type != TYPE_SEARCHFOLDER && mData.type != TYPE_MOUNTPOINT)
            throw new IllegalArgumentException();
    }


    /** Returns the folder's name.  Note that this is the folder's
     *  name (e.g. <code>"foo"</code>), not its absolute pathname
     *  (e.g. <code>"/baz/bar/foo"</code>).
     * 
     * @see #getPath() */
    public String getName() {
        return (mData.subject == null ? "" : mData.subject);
    }

    /** Returns the folder's absolute path.  Paths are UNIX-style with 
     *  <code>'/'</code> as the path delimiter.  Paths are relative to
     *  the user root folder ({@link Mailbox#ID_FOLDER_USER_ROOT}),
     *  which has the path <code>"/"</code>.  So the Inbox's path is
     *  <code>"/Inbox"</code>, etc. */
    public String getPath() {
        if (mId == Mailbox.ID_FOLDER_ROOT || mId == Mailbox.ID_FOLDER_USER_ROOT)
            return "/";
        String parentPath = mParent.getPath();
        return parentPath + (parentPath.equals("/") ? "" : "/") + getName();
    }

    /** Returns the "hint" as to which view to use to display the folder's
     *  contents.  For instance, if the default view for the folder is
     *  {@link MailItem#TYPE_APPOINTMENT}, the client would render the
     *  contents using the calendar app.<p>
     *  Defaults to {@link MailItem#TYPE_UNKNOWN}.*/
    public byte getDefaultView() {
        return mDefaultView;
    }

    /** Returns the folder's set of special attributes.
     * @see #FOLDER_IS_IMMUTABLE
     * @see #FOLDER_DONT_TRACK_COUNTS */
    public byte getAttributes() {
        return mAttributes;
    }

    /** Returns the URL the folder syncs to, or <code>""</code> if there
     *  is no such association.
     * @see #setUrl(String) */
    public String getUrl() {
        return (mSyncData == null || mSyncData.url == null ? "" : mSyncData.url);
    }

    /** Returns the URL the folder syncs to, or <code>""</code> if there
     *  is no such association.
     * @see #setUrl(String) */
    public SyncData getSyncData() {
        return mSyncData;
    }

    public int getImapUIDNEXT() {
        return mImapUIDNEXT;
    }

    /** Returns whether the folder is the Trash folder or any of its
     *  subfolders. */
    public boolean inTrash() {
        if (mId <= Mailbox.HIGHEST_SYSTEM_ID)
            return (mId == Mailbox.ID_FOLDER_TRASH);
        return mParent.inTrash();
    }

    /** Returns whether the folder is the Junk folder. */
    public boolean inSpam() {
        return (mId == Mailbox.ID_FOLDER_SPAM);
    }

    /** Returns whether the folder is client-visible.  Folders below
     *  the user root folder ({@link Mailbox#ID_FOLDER_USER_ROOT}) are
     *  visible; all others are hidden.
     * 
     * @see Mailbox#initialize() */
    public boolean isHidden() {
        switch (mId) {
            case Mailbox.ID_FOLDER_USER_ROOT:  return false;
            case Mailbox.ID_FOLDER_ROOT:       return true;
            default:                           return mParent.isHidden();
        }
    }


    /** Returns the subset of the requested access rights that the user has
     *  been granted on this folder.  The owner of the {@link Mailbox} has
     *  all rights on all items in the Mailbox, as do all admin accounts.
     *  All other users must be explicitly granted access.<p>
     * 
     *  The set of rights that apply to a given folder is derived by starting
     *  at that folder and going up the folder hierarchy, considering the first
     *  folder with explicitly set rights (whether or not they are inherited by
     *  the folder in question) and then stopping.  In other words, take the
     *  *first* (and only the first) of the following that exists:<ul>
     *    <li>the set of rights granted on the folder directly
     *    <li>the set of inherited rights granted on the folder's parent
     *    <li>the set of inherited rights granted on the folder's grandparent
     *    <li>...
     *    <li>the set of inherited rights granted on the mailbox's root folder
     *    <li>no rights at all</ul><p>
     * 
     *  So if the folder hierarchy looks like this:<pre>
     *           root  <- inherited "read+write" granted to user A
     *           /  \
     *          V    W  <- "read" granted to users A and B
     *         /    / \
     *        X    Y   Z</pre>
     *  then user A has "write" rights on folders V and X, but not W, Y, and Z,
     *  and user B has "read" rights on folder W but not V, X, Y, or Z.
     * 
     * @param rightsNeeded  A set of rights (e.g. {@link ACL#RIGHT_READ}
     *                      and {@link ACL#RIGHT_DELETE}).
     * @param authuser      The user whose rights we need to query.
     * @see ACL */
    short checkRights(short rightsNeeded, Account authuser) throws ServiceException {
        return checkRights(rightsNeeded, authuser, false);
    }

    private short checkRights(short rightsNeeded, Account authuser, boolean inheritedOnly) throws ServiceException {
        if (rightsNeeded == 0)
            return rightsNeeded;
        // XXX: in Mailbox, authuser is set to null if authuser == owner.
        // the mailbox owner can do anything they want
        if (authuser == null || authuser.getId().equals(mMailbox.getAccountId()))
            return rightsNeeded;
        // admin users (and the appropriate domain admins) can also do anything they want
        if (AccessManager.getInstance().canAccessAccount(authuser, getAccount()))
            return rightsNeeded;
        // check the ACLs to see if access has been explicitly granted
        Short granted = mRights != null ? mRights.getGrantedRights(authuser, inheritedOnly) : null;
        if (granted != null)
            return (short) (granted.shortValue() & rightsNeeded);
        // no ACLs apply; check parent folder for inherited rights
        return mId == Mailbox.ID_FOLDER_ROOT ? 0 : mParent.checkRights(rightsNeeded, authuser, true);
    }

    /** Grants the specified set of rights to the target and persists them
     *  to the database.
     * 
     * @param zimbraId  The zimbraId of the entry being granted rights.
     * @param type      The type of principal the grantee's ID refers to.
     * @param rights    A bitmask of the rights being granted.
     * @param inherit   Whether subfolders inherit these same rights.
     * @perms {@link ACL#RIGHT_ADMIN} on the folder
     * @throws ServiceException The following error codes are possible:<ul>
     *    <li><code>service.FAILURE</code> - if there's a database failure
     *    <li><code>service.PERM_DENIED</code> - if you don't have
     *        sufficient permissions</ul> */
    void grantAccess(String zimbraId, byte type, short rights, boolean inherit, String args) throws ServiceException {
        if (!canAccess(ACL.RIGHT_ADMIN))
            throw ServiceException.PERM_DENIED("you do not have admin rights to folder " + getPath());
        markItemModified(Change.MODIFIED_ACL);
        if (mRights == null)
            mRights = new ACL();
        mRights.grantAccess(zimbraId, type, rights, inherit, args);
        saveMetadata();
    }

    /** Removes the set of rights granted to the specified (id, type) pair
     *  and updates the database accordingly.
     * 
     * @param zimbraId  The zimbraId of the entry being revoked rights.
     * @perms {@link ACL#RIGHT_ADMIN} on the folder
     * @throws ServiceException The following error codes are possible:<ul>
     *    <li><code>service.FAILURE</code> - if there's a database failure
     *    <li><code>service.PERM_DENIED</code> - if you don't have
     *        sufficient permissions</ul> */
    void revokeAccess(String zimbraId) throws ServiceException {
        if (!canAccess(ACL.RIGHT_ADMIN))
            throw ServiceException.PERM_DENIED("you do not have admin rights to folder " + getPath());
        markItemModified(Change.MODIFIED_ACL);
        if (mRights == null || !mRights.revokeAccess(zimbraId))
            return;
        if (mRights.isEmpty())
            mRights = null;
        saveMetadata();
    }

    /** Replaces the folder's {@link ACL} with the supplied one and updates
     *  the database accordingly.
     * 
     * @param acl  The new ACL being applied (<code>null</code> is OK).
     * @perms {@link ACL#RIGHT_ADMIN} on the folder
     * @throws ServiceException The following error codes are possible:<ul>
     *    <li><code>service.FAILURE</code> - if there's a database failure
     *    <li><code>service.PERM_DENIED</code> - if you don't have
     *        sufficient permissions</ul> */
    void setPermissions(ACL acl) throws ServiceException {
        if (!canAccess(ACL.RIGHT_ADMIN))
            throw ServiceException.PERM_DENIED("you do not have admin rights to folder " + getPath());
        markItemModified(Change.MODIFIED_ACL);
        if (acl == null && mRights == null)
            return;
        mRights = acl;
        saveMetadata();
    }

    /** Returns a copy of the ACL on the folder, or <code>null</code> if
     *  one is not set. */
    public ACL getPermissions() {
        return mRights == null ? null : mRights.duplicate();
    }


    /** Returns whether the folder contains any subfolders. */
    public boolean hasSubfolders() {
        return (mSubfolders != null && !mSubfolders.isEmpty());
    }

    /** Returns the subfolder with the given name.  Name comparisons are
     *  case-insensitive.
     * 
     * @param name  The folder name to search for.
     * @return The matching subfolder, or <code>null</code> if no such
     *         folder exists. */
    Folder findSubfolder(String name) {
        if (name == null || mSubfolders == null)
            return null;
        for (Folder subfolder : mSubfolders)
            if (subfolder != null && name.equalsIgnoreCase(subfolder.getName()))
                return subfolder;
        return null;
    }

    private static final class SortByName implements Comparator<Folder> {
        public int compare(Folder f1, Folder f2) {
            String n1 = f1.getName();
            String n2 = f2.getName();
            return n1.compareToIgnoreCase(n2);
        }
    }

    /** Returns an unmodifiable list of the folder's subfolders sorted by
     *  name.  The sort is case-insensitive.
     * @throws ServiceException */
    public List<Folder> getSubfolders(Mailbox.OperationContext octxt) throws ServiceException {
        if (mSubfolders == null)
            return Collections.emptyList();

        Collections.sort(mSubfolders, new SortByName());
        if (octxt == null || octxt.authuser == null)
            return Collections.unmodifiableList(mSubfolders);

        ArrayList<Folder> visible = new ArrayList<Folder>();
        for (Folder subfolder : mSubfolders)
            if (subfolder.canAccess(ACL.RIGHT_READ, octxt.authuser))
                visible.add(subfolder);
        return visible;
    }

    /** Returns a <code>List</code> that includes this folder and all its
     *  subfolders.  The tree traversal is done depth-first, so this folder
     *  is the first element in the list, followed by its children, then
     *  its grandchildren, etc. */
    public List<Folder> getSubfolderHierarchy() {
        return accumulateHierarchy(new ArrayList<Folder>());
    }
    private List<Folder> accumulateHierarchy(List<Folder> list) {
        list.add(this);
        if (mSubfolders != null)
            for (Folder subfolder : mSubfolders)
                subfolder.accumulateHierarchy(list);
        return list;
    }

    /** Updates the number of items in the folder.  Only "leaf node" items in
     *  the folder are summed; items in subfolders are included only in the
     *  size of the subfolder.
     * @param delta  The change in item count, negative or positive. */
    void updateSize(int delta) {
        if (delta == 0 || !trackSize())
            return;
        // if we go negative, that's OK!  just pretend we're at 0.
        markItemModified(Change.MODIFIED_SIZE);
        mData.size = Math.max(0, mData.size + delta);
        if (delta > 0)
            mImapUIDNEXT = mMailbox.getLastItemId() + 1;
    }

    boolean isTaggable()       { return false; }
    boolean isCopyable()       { return false; }
    boolean isMovable()        { return ((mAttributes & FOLDER_IS_IMMUTABLE) == 0); }
    boolean isMutable()        { return ((mAttributes & FOLDER_IS_IMMUTABLE) == 0); }
    boolean isIndexed()        { return false; }
    boolean canHaveChildren()  { return true; }
    public boolean isDeletable()  { return ((mAttributes & FOLDER_IS_IMMUTABLE) == 0); }
    boolean isLeafNode()       { return false; }
    boolean trackUnread()      { return ((mAttributes & FOLDER_DONT_TRACK_COUNTS) == 0); }
    boolean trackSize()        { return ((mAttributes & FOLDER_DONT_TRACK_COUNTS) == 0); }

    boolean canParent(MailItem child)  { return (child instanceof Folder); }

    /** Returns whether the folder can contain the given item.  We make
     *  the same checks as in {@link #canContain(byte)}, and we also make
     *  sure to avoid any cycles of folders.
     * 
     * @param child  The {@link MailItem} object to check. */
    boolean canContain(MailItem child) {
        if (!canContain(child.getType())) {
            return false;
        } else if (child instanceof Folder) {
            // may not contain our parents or grandparents (c.f. Back to the Future)
            for (Folder folder = this; folder.getId() != Mailbox.ID_FOLDER_ROOT; folder = folder.mParent)
                if (folder.getId() == child.getId())
                    return false;
        }
        return true;
    }
    /** Returns whether the folder can contain objects of the given type.
     *  In general, any folder can contain any object.  The exceptions are
     *  that the Tags folder can only contain {@link Tag}s (and vice versa),
     *  the Conversations folder can only contain {@link Conversation}s
     *  (and vice versa), and the Spam folder can't have subfolders.
     * 
     * @param type  The type of object, e.g. {@link MailItem#TYPE_TAG}. */
    boolean canContain(byte type) {
        if ((type == TYPE_TAG) != (mId == Mailbox.ID_FOLDER_TAGS))
            return false;
        else if ((type == TYPE_CONVERSATION) != (mId == Mailbox.ID_FOLDER_CONVERSATIONS))
            return false;
        else if (type == TYPE_FOLDER && mId == Mailbox.ID_FOLDER_SPAM)
            return false;
        return true;
    }


    /** Creates a new Folder and persists it to the database.  A real
     *  nonnegative item ID must be supplied from a previous call to
     *  {@link Mailbox#getNextItemId(int)}.
     * 
     * @param id      The id for the new folder.
     * @param mbox    The {@link Mailbox} to create the folder in.
     * @param parent  The parent folder to place the new folder in.
     * @param name    The new folder's name.
     * @perms {@link ACL#RIGHT_INSERT} on the parent folder
     * @throws ServiceException   The following error codes are possible:<ul>
     *    <li><code>mail.CANNOT_CONTAIN</code> - if the target folder
     *        can't have subfolders
     *    <li><code>mail.ALREADY_EXISTS</code> - if a folder by that name
     *        already exists in the parent folder
     *    <li><code>mail.INVALID_NAME</code> - if the new folder's name is
     *        invalid
     *    <li><code>service.FAILURE</code> - if there's a database failure
     *    <li><code>service.PERM_DENIED</code> - if you don't have
     *        sufficient permissions</ul>
     * @see #validateFolderName(String)
     * @see #canContain(byte) */
    static Folder create(int id, Mailbox mbox, Folder parent, String name) throws ServiceException {
        return create(id, mbox, parent, name, (byte) 0, TYPE_UNKNOWN, 0, DEFAULT_COLOR, null);
    }

    /** Creates a new Folder with optional attributes and persists it
     *  to the database.  A real nonnegative item ID must be supplied
     *  from a previous call to {@link Mailbox#getNextItemId(int)}.
     * 
     * @param id          The id for the new folder.
     * @param mbox        The {@link Mailbox} to create the folder in.
     * @param parent      The parent folder to place the new folder in.
     * @param name        The new folder's name.
     * @param attributes  Any extra constraints on the folder.
     * @param view        The (optional) default object type for the folder.
     * @param flags       Folder flags (e.g. {@link Flag#BITMASK_CHECKED}).
     * @param color       The new folder's color.
     * @param url         The (optional) url to sync folder contents to.
     * @perms {@link ACL#RIGHT_INSERT} on the parent folder
     * @throws ServiceException   The following error codes are possible:<ul>
     *    <li><code>mail.CANNOT_CONTAIN</code> - if the target folder
     *        can't have subfolders
     *    <li><code>mail.ALREADY_EXISTS</code> - if a folder by that name
     *        already exists in the parent folder
     *    <li><code>mail.INVALID_NAME</code> - if the new folder's name is
     *        invalid
     *    <li><code>service.FAILURE</code> - if there's a database failure
     *    <li><code>service.PERM_DENIED</code> - if you don't have
     *        sufficient permissions</ul>
     * @see #validateFolderName(String)
     * @see #canContain(byte)
     * @see #FOLDER_IS_IMMUTABLE
     * @see #FOLDER_DONT_TRACK_COUNTS */
    static Folder create(int id, Mailbox mbox, Folder parent, String name, byte attributes, byte view, int flags, byte color, String url)
    throws ServiceException {
        if (id != Mailbox.ID_FOLDER_ROOT) {
            if (parent == null || !parent.canContain(TYPE_FOLDER))
                throw MailServiceException.CANNOT_CONTAIN();
            validateFolderName(name);
            if (parent.findSubfolder(name) != null)
                throw MailServiceException.ALREADY_EXISTS(name);
            if (!parent.canAccess(ACL.RIGHT_SUBFOLDER))
                throw ServiceException.PERM_DENIED("you do not have the required rights on the parent folder");
        }
        if (view != TYPE_UNKNOWN)
            validateType(view);

        UnderlyingData data = new UnderlyingData();
        data.id          = id;
        data.type        = TYPE_FOLDER;
        data.folderId    = (id == Mailbox.ID_FOLDER_ROOT ? id : parent.getId());
        data.parentId    = data.folderId;
        data.date        = mbox.getOperationTimestamp();
        data.flags       = flags & Flag.FLAGS_FOLDER;
        data.subject     = name;
        data.metadata    = encodeMetadata(color, attributes, view, null, new SyncData(url), id + 1);
        data.contentChanged(mbox);
        DbMailItem.create(mbox, data);

        Folder folder = new Folder(mbox, data);
        folder.finishCreation(parent);
        return folder;
    }

    /** Sets the remote URL for the folder.  This can point to a remote
     *  calendar (<code>.ics</code> file), an RSS feed, etc.  Note that you
     *  cannot add a remote data source to an existing folder, as refreshing
     *  the linked content empties the folder.<p>
     * 
     *  This is <i>not</i> used to mount other Zimbra users' folders; to do
     *  that, use a {@link Mountpoint}.
     * 
     * @param url  The new URL for the folder, or <code>null</code> to
     *             remove the association with a remote object.
     * @perms {@link ACL#RIGHT_WRITE} on the folder
     * @throws ServiceException   The following error codes are possible:<ul>
     *    <li><code>mail.CANNOT_SUBSCRIBE</code> - if you're attempting to
     *        associate a URL with an existing, normal folder
     *    <li><code>mail.IMMUTABLE_OBJECT</code> - if the folder can't be
     *        modified
     *    <li><code>service.FAILURE</code> - if there's a database failure
     *    <li><code>service.PERM_DENIED</code> - if you don't have
     *        sufficient permissions</ul> */
    void setUrl(String url) throws ServiceException {
        if (url == null)
            url = "";
        if (getUrl().equals(url))
            return;
        if (!getUrl().equals(""))
            throw MailServiceException.CANNOT_SUBSCRIBE(mId);
        if (!isMutable())
            throw MailServiceException.IMMUTABLE_OBJECT(mId);
        if (!canAccess(ACL.RIGHT_WRITE))
            throw ServiceException.PERM_DENIED("you do not have the required rights on the folder");

        markItemModified(Change.MODIFIED_URL);
        mSyncData = new SyncData(url);
        saveMetadata();
    }

    /** Records the last-synced information for a subscribed folder.  If the
     *  folder does not have an associated URL, no action is taken and no
     *  exception is thrown.
     * 
     * @param guid  The last synchronized remote item's GUID.
     * @param date  The last synchronized remote item's timestamp.
     * @perms {@link ACL#RIGHT_WRITE} on the folder
     * @throws ServiceException   The following error codes are possible:<ul>
     *    <li><code>mail.IMMUTABLE_OBJECT</code> - if the folder can't be
     *        modified
     *    <li><code>service.FAILURE</code> - if there's a database failure
     *    <li><code>service.PERM_DENIED</code> - if you don't have
     *        sufficient permissions</ul> */
    void setSubscriptionData(String guid, long date) throws ServiceException {
        if (getUrl().equals(""))
            return;
        if (!isMutable())
            throw MailServiceException.IMMUTABLE_OBJECT(mId);
        if (!canAccess(ACL.RIGHT_WRITE))
            throw ServiceException.PERM_DENIED("you do not have the required rights on the folder");

        markItemModified(Change.MODIFIED_URL);
        mSyncData = new SyncData(getUrl(), guid, date);
        saveMetadata();
    }

    /** Renames the folder in place.  Altering a folder's case (e.g.
     *  from <code>foo</code> to <code>FOO</code>) is allowed.
     * 
     * @param name  The new name for this folder.
     * @perms {@link ACL#RIGHT_WRITE} on the folder
     * @throws ServiceException   The following error codes are possible:<ul>
     *    <li><code>mail.IMMUTABLE_OBJECT</code> - if the folder can't be
     *        renamed
     *    <li><code>mail.ALREADY_EXISTS</code> - if a folder by that name
     *        already exists in the new parent folder
     *    <li><code>mail.INVALID_NAME</code> - if the new folder's name is
     *        invalid
     *    <li><code>service.FAILURE</code> - if there's a database failure
     *    <li><code>service.PERM_DENIED</code> - if you don't have
     *        sufficient permissions</ul>
     * @see #validateFolderName(String) */
    void rename(String name) throws ServiceException {
        rename(name, mParent);
    }
    /** Renames the folder and optionally moves it.  Altering a folder's
     *  case (e.g. from <code>foo</code> to <code>FOO</code>) is allowed.
     *  If you don't want the folder to be moved, you must pass 
     *  <code>folder.getParent()</code> as the second parameter.
     * 
     * @param name    The new name for this folder.
     * @param target  The new parent folder to move this folder to.
     * @perms {@link ACL#RIGHT_WRITE} on the folder to rename it,
     *        {@link ACL#RIGHT_DELETE} on the parent folder and
     *        {@link ACL#RIGHT_INSERT} on the target folder to move it
     * @throws ServiceException   The following error codes are possible:<ul>
     *    <li><code>mail.IMMUTABLE_OBJECT</code> - if the folder can't be
     *        renamed
     *    <li><code>mail.ALREADY_EXISTS</code> - if a folder by that name
     *        already exists in the new parent folder
     *    <li><code>mail.INVALID_NAME</code> - if the new folder's name is
     *        invalid
     *    <li><code>service.FAILURE</code> - if there's a database failure
     *    <li><code>service.PERM_DENIED</code> - if you don't have
     *        sufficient permissions</ul>
     * @see #validateFolderName(String)
     * @see #move(Folder) */
    void rename(String name, Folder target) throws ServiceException {
        validateFolderName(name);
        if (name.equals(mData.subject) && target == mParent)
            return;
        if (!isMutable())
            throw MailServiceException.IMMUTABLE_OBJECT(mId);
        boolean renamed = !name.equals(mData.subject);
        boolean moved   = target != mParent;

        if (moved &&!target.canAccess(ACL.RIGHT_INSERT))
            throw ServiceException.PERM_DENIED("you do not have the required rights on the target folder");
        if (moved && !mParent.canAccess(ACL.RIGHT_DELETE))
            throw ServiceException.PERM_DENIED("you do not have the required rights on the parent folder");
        if (renamed && !canAccess(ACL.RIGHT_WRITE))
            throw ServiceException.PERM_DENIED("you do not have the required rights on the folder");

        if (renamed) {
            String originalPath = getPath();
            markItemModified(Change.MODIFIED_NAME);
            Folder existingFolder = target.findSubfolder(name);
            if (existingFolder != null && existingFolder != this)
                throw MailServiceException.ALREADY_EXISTS(name);
            mData.subject = name;
            mData.date = mMailbox.getOperationTimestamp();
            saveSubject();
            updateRules(originalPath);
        }

        if (moved)
            move(target);
    }

    /**
     * Updates filter rules after a folder's path changes (move or rename).
     */
    private void updateRules(String originalPath)
    throws ServiceException {
        Account account = getAccount();
        RuleManager rm = RuleManager.getInstance();
        String rules = rm.getRules(account);
        if (rules != null) {
            // Assume that we always put quotes around folder paths.  Replace
            // any paths that start with this folder's original path.  This will
            // take care of rules for children affected by a parent's move or rename.
            String newPath = getPath();
            String newRules = rules.replace("\"" + originalPath, "\"" + newPath);
            if (!newRules.equals(rules)) {
                rm.setRules(account, newRules);
                ZimbraLog.mailbox.debug(
                    "Updated filter rules due to folder move or rename.  Old rules:\n" +
                    rules + ", new rules:\n" + newRules);
            }
        }
    }

    /** The regexp defining printable characters not permitted in folder
     *  names.  These are: ':', '/', '"', '\t', '\r', and '\n'. */
    private static final String INVALID_CHARACTERS = ".*[:/\"\t\r\n].*";
    /** The maximum length for a folder name.  This is not the maximum length
     *  of a <u>path</u>, just the maximum length of a single folder's name. */
    public static final int MAX_FOLDER_LENGTH = 128;

    /** Returns whether a proposed folder name is valid.  Folder names must
     *  be less than {@link #MAX_FOLDER_LENGTH} characters long, must contain
     *  non-whitespace characters, and may not contain any characters in
     *  {@link #INVALID_CHARACTERS} (':', '/', '"', '\t', '\r', '\n') or
     *  banned in XML.
     * 
     * @param name  The proposed folder name.
     * @throws ServiceException  The following error codes are possible:<ul>
     *    <li><code>mail.INVALID_NAME</code> - if the name is not acceptable
     *    </ul>
     * @see StringUtil#stripControlCharacters */
    protected static void validateFolderName(String name) throws ServiceException {
        if (name == null || name != StringUtil.stripControlCharacters(name))
            throw MailServiceException.INVALID_NAME(name);
        if (name.trim().equals("") || name.length() > MAX_FOLDER_LENGTH || name.matches(INVALID_CHARACTERS))
            throw MailServiceException.INVALID_NAME(name);
    }

    private void recursiveAlterUnread(boolean unread) throws ServiceException {
        alterUnread(unread);
        if (mSubfolders != null)
            for (Folder subfolder : mSubfolders)
                subfolder.recursiveAlterUnread(unread);
    }

    /** Updates the unread state of all items in the folder.  Persists the
     *  change to the database and cache, and also updates the unread counts
     *  for the folder and the affected items' parents and {@link Tag}s
     *  appropriately.  <i>Note: Folders may only be marked read, not
     *  unread.</i>
     * 
     * @perms {@link ACL#RIGHT_READ} on the folder,
     *        {@link ACL#RIGHT_WRITE} on all affected messages. */
    void alterUnread(boolean unread) throws ServiceException {
        if (unread)
            throw ServiceException.INVALID_REQUEST("folders can only be marked read", null);
        if (!canAccess(ACL.RIGHT_READ))
            throw ServiceException.PERM_DENIED("you do not have sufficient permissions on the folder");
        if (!isUnread())
            return;

        // decrement the in-memory unread count of each message.  each message will
        // then implicitly decrement the unread count for its conversation, folder
        // and tags.
        List<Integer> targets = new ArrayList<Integer>();
        boolean missed = false;
        for (UnderlyingData data : DbMailItem.getUnreadMessages(this)) {
            Message msg = mMailbox.getMessage(data);
            if (msg.checkChangeID() || !msg.canAccess(ACL.RIGHT_WRITE)) {
                msg.updateUnread(-1);
                msg.mData.metadataChanged(mMailbox);
                targets.add(msg.getId());
            } else {
                missed = true;
            }
        }

        // mark all messages in this folder as read in the database
        if (!missed)
            DbMailItem.alterUnread(this, unread);
        else
            DbMailItem.alterUnread(mMailbox, targets, unread);
    }

    /** Tags or untags a folder.  Persists the change to the database and
     *  cache.  In most cases, we call {@link MailItem#alterTag(Tag, boolean)}
     *  and the action will be performed on the folder's contents.  <i>Note:
     *  At present, user tags and non-folder-specific flags cannot be applied
     *  or removed on a folder.</i>  For folder-specific flags like
     *  {@link Mailbox#mSubscribeFlag}, the tagging or untagging applies to
     *  the <code>Folder</code> itself.<p>
     * 
     *  You must use {@link #alterUnread} to change a folder's unread state.
     * 
     * @perms {@link ACL#RIGHT_WRITE} on the folder */
    void alterTag(Tag tag, boolean add) throws ServiceException {
        // folder flags are applied to the folder, not the contents
        if (!(tag instanceof Flag) || !((Flag) tag).isFolderOnly()) {
            super.alterTag(tag, add);
            return;
        }

        if (!canAccess(ACL.RIGHT_WRITE))
            throw ServiceException.PERM_DENIED("you do not have the necessary privileges on the folder");
        if (add == isTagged(tag))
            return;

        // change the tag on the Folder itself, not on its contents
        markItemModified(tag instanceof Flag ? Change.MODIFIED_FLAGS : Change.MODIFIED_TAGS);
        tagChanged(tag, add);

        List<Integer> ids = new ArrayList<Integer>();
        ids.add(mId);
        DbMailItem.alterTag(tag, ids, add);
    }

    /** Moves this folder so that it is a subfolder of <code>folder</code>.
     * 
     * @perms {@link ACL#RIGHT_INSERT} on the target folder,
     *         {@link ACL#RIGHT_DELETE} on the source folder */
    protected void move(Folder folder) throws ServiceException {
        markItemModified(Change.MODIFIED_FOLDER | Change.MODIFIED_PARENT);
        if (mData.folderId == folder.getId())
            return;
        if (!isMovable())
            throw MailServiceException.IMMUTABLE_OBJECT(mId);
        if (!mParent.canAccess(ACL.RIGHT_DELETE) || !folder.canAccess(ACL.RIGHT_INSERT))
            throw ServiceException.PERM_DENIED("you do not have the required permissions");
        if (!folder.canContain(this))
            throw MailServiceException.CANNOT_CONTAIN();

        String originalPath = getPath();
        
        // moving a folder to the Trash marks its contents as read
        if (!inTrash() && folder.inTrash())
            recursiveAlterUnread(false);

        // tell the folder's old and new parents
        mParent.removeChild(this);
        folder.addChild(this);

        // and update the folder's data (in memory and DB)
        DbMailItem.setFolder(this, folder);
        mData.folderId = folder.getId();
        mData.parentId = folder.getId();
        mData.metadataChanged(mMailbox);
        
        updateRules(originalPath);
    }

    void addChild(MailItem child) throws ServiceException {
        if (child == null || !canParent(child)) {
            throw MailServiceException.CANNOT_CONTAIN();
        } else if (child == this) {
            if (mId != Mailbox.ID_FOLDER_ROOT)
                throw MailServiceException.CANNOT_CONTAIN();
        } else if (!(child instanceof Folder)) {
            super.addChild(child);
        } else {
            markItemModified(Change.MODIFIED_CHILDREN);
            Folder subfolder = (Folder) child;
            if (mSubfolders == null) {
                mSubfolders = new ArrayList<Folder>();
            } else {
                Folder existing = findSubfolder(subfolder.getName());
                if (existing == child)
                    return;
                else if (existing != null)
                    throw MailServiceException.ALREADY_EXISTS(subfolder.getName());
            }
            mSubfolders.add(subfolder);
            subfolder.mParent = this;
        }
    }

    void removeChild(MailItem child) throws ServiceException {
        if (child == null) {
            throw MailServiceException.CANNOT_CONTAIN();
        } else if (!(child instanceof Folder)) {
            super.removeChild(child);
        } else {
            markItemModified(Change.MODIFIED_CHILDREN);
            Folder subfolder = (Folder) child;
            if (mSubfolders == null)
                throw MailServiceException.IS_NOT_CHILD();
            int index = mSubfolders.indexOf(subfolder);
            if (index == -1)
                throw MailServiceException.IS_NOT_CHILD();
            mSubfolders.remove(index);
            subfolder.mParent = null;
        }
    }

    /** Deletes this folder and all its subfolders. */
    void delete(boolean childrenOnly, boolean writeTombstones) throws ServiceException {
        if (childrenOnly) {
            deleteSingleFolder(writeTombstones);
        } else {
            List<Folder> subfolders = getSubfolderHierarchy();
            for (int i = subfolders.size() - 1; i >= 0; i--) {
                Folder subfolder = subfolders.get(i);
                subfolder.deleteSingleFolder(writeTombstones);
            }
        }
    }

    void empty(boolean includeSubfolders) throws ServiceException {
        // kill all subfolders, if so requested
        if (includeSubfolders) {
            List<Folder> subfolders = getSubfolderHierarchy();
            // we DO NOT include *this* folder, the first in the list...
            for (int i = subfolders.size() - 1; i >= 1; i--) {
                Folder subfolder = subfolders.get(i);
                subfolder.deleteSingleFolder(true);
            }
        }

        // now we can empty *this* folder
        super.delete(DELETE_CONTENTS, true);
    }

    /** Deletes just this folder without affecting its subfolders. */
    void deleteSingleFolder(boolean writeTombstones) throws ServiceException {
        super.delete(hasSubfolders() ? DELETE_CONTENTS : DELETE_ITEM, writeTombstones);
    }

    /** Determines the set of items to be deleted.  Assembles a new
     *  {@link PendingDelete} object encapsulating the data on the items
     *  to be deleted.  This set of items will include the folder itself,
     *  but will exclude any subfolders.  If the caller has specified the
     *  maximum change number they know about, this set will also exclude
     *  any item for which the (modification/content) change number is
     *  greater.
     * 
     * @perms {@link ACL#RIGHT_DELETE} on the folder
     * @throws ServiceException The following error codes are possible:<ul>
     *    <li><code>mail.MODIFY_CONFLICT</code> - if the caller specified a
     *        max change number and a modification check, and the modified
     *        change number of a contained item is greater
     *    <li><code>service.FAILURE</code> - if there's a database failure
     *    <li><code>service.PERM_DENIED</code> - if you don't have
     *        sufficient permissions</ul> */
    MailItem.PendingDelete getDeletionInfo() throws ServiceException {
        if (!canAccess(ACL.RIGHT_DELETE))
            throw ServiceException.PERM_DENIED("you do not have the required rights on the item");
        return DbMailItem.getLeafNodes(this);
    }

    void propagateDeletion(PendingDelete info) throws ServiceException {
        if (info.incomplete)
            info.cascadeIds = DbMailItem.markDeletionTargets(mMailbox, info.itemIds.getIds(TYPE_MESSAGE));
        else
            info.cascadeIds = DbMailItem.markDeletionTargets(this);
        super.propagateDeletion(info);
    }

    void purgeCache(PendingDelete info, boolean purgeItem) throws ServiceException {
        // when deleting a folder, need to purge conv cache!
        mMailbox.purge(TYPE_CONVERSATION);
        super.purgeCache(info, purgeItem);
    }

    void uncacheChildren() throws ServiceException {
        if (mSubfolders != null)
            for (Folder subfolder : mSubfolders)
                mMailbox.uncache(subfolder);
    }


    static void purgeMessages(Mailbox mbox, Folder folder, int beforeDate) throws ServiceException {
        if (beforeDate <= 0 || beforeDate >= mbox.getOperationTimestamp())
            return;
        boolean allFolders = (folder == null);
        List<Folder> folders = (allFolders ? null : folder.getSubfolderHierarchy());

        // get the full list of things that are being removed
        PendingDelete info = DbMailItem.getLeafNodes(mbox, folders, beforeDate, allFolders);
        if (info.itemIds.isEmpty())
            return;
        mbox.markItemDeleted(info.itemIds.getAll());

        // update message counts
        for (Map.Entry<Integer, DbMailItem.LocationCount> entry : info.messages.entrySet()) {
            int folderID = entry.getKey();
            int msgCount = entry.getValue().count;
            mbox.getFolderById(folderID).updateSize(-msgCount);
        }

        // update the mailbox's size
        mbox.updateSize(-info.size);
        mbox.updateContactCount(-info.contacts);

        // update unread counts on folders and tags
        List<UnderlyingData> unreadData = DbMailItem.getById(mbox, info.unreadIds, TYPE_MESSAGE);
        for (UnderlyingData data : unreadData)
            mbox.getItem(data).updateUnread(-1);

        // remove the deleted item(s) from the mailbox's cache
        if (!info.itemIds.isEmpty()) {
            info.cascadeIds = DbMailItem.markDeletionTargets(mbox, info.itemIds.getIds(TYPE_MESSAGE));
            mbox.purge(TYPE_CONVERSATION);
        }

        // actually delete the items from the DB
        DbMailItem.delete(mbox, info.itemIds.getAll());
        mbox.markOtherItemDirty(info);

        // also delete any conversations whose messages have all been removed
        if (info.cascadeIds != null && !info.cascadeIds.isEmpty()) {
            DbMailItem.delete(mbox, info.cascadeIds);
            mbox.markItemDeleted(info.cascadeIds);
            info.itemIds.add(TYPE_CONVERSATION, info.cascadeIds);
        }

        // deal with index sharing
        if (!info.sharedIndex.isEmpty())
            DbMailItem.resolveSharedIndex(mbox, info);

        // write a deletion record for later sync
        if (mbox.isTrackingSync() && !info.itemIds.isEmpty())
            DbMailItem.writeTombstone(mbox, info.itemIds);

        // don't actually delete the blobs or index entries here; wait until after the commit
    }

    /** Persists the folder's current unread/message counts and IMAP UIDNEXT
     *  value to the database.
     * @param initial  Whether this is the first time we're saving folder
     *                 counts, in which case we also initialize the IMAP
     *                 UIDNEXT value. */
    protected void saveFolderCounts(boolean initial) throws ServiceException {
        if (initial)
            mImapUIDNEXT = mMailbox.getLastItemId() + 1;
        DbMailItem.persistCounts(this, encodeMetadata());
    }


    void decodeMetadata(Metadata meta) throws ServiceException {
        super.decodeMetadata(meta);

        // avoid a painful data migration...
        byte view = TYPE_UNKNOWN;
        switch (mId) {
            case Mailbox.ID_FOLDER_INBOX:
            case Mailbox.ID_FOLDER_SPAM:
            case Mailbox.ID_FOLDER_SENT:
            case Mailbox.ID_FOLDER_DRAFTS:    view = MailItem.TYPE_MESSAGE;      break;
            case Mailbox.ID_FOLDER_CALENDAR:  view = MailItem.TYPE_APPOINTMENT;  break;
            case Mailbox.ID_FOLDER_AUTO_CONTACTS:
            case Mailbox.ID_FOLDER_CONTACTS:  view = MailItem.TYPE_CONTACT;      break;
        }
        mDefaultView = (byte) meta.getLong(Metadata.FN_VIEW, view);
        mAttributes  = (byte) meta.getLong(Metadata.FN_ATTRS, 0);
        mImapUIDNEXT = (int) meta.getLong(Metadata.FN_UIDNEXT, 0);

        if (meta.containsKey(Metadata.FN_URL))
            mSyncData = new SyncData(meta.get(Metadata.FN_URL), meta.get(Metadata.FN_SYNC_GUID, null), meta.getLong(Metadata.FN_SYNC_DATE, 0));

        MetadataList mlistACL = meta.getList(Metadata.FN_RIGHTS, true);
        if (mlistACL != null)
            if ((mRights = new ACL(mlistACL)).isEmpty())
                mRights = null;
    }

    Metadata encodeMetadata(Metadata meta) {
        return encodeMetadata(meta, mColor, mAttributes, mDefaultView, mRights, mSyncData, mImapUIDNEXT);
    }
    private static String encodeMetadata(byte color, byte attributes, byte hint, ACL rights, SyncData fsd, int uidnext) {
        return encodeMetadata(new Metadata(), color, attributes, hint, rights, fsd, uidnext).toString();
    }
    static Metadata encodeMetadata(Metadata meta, byte color, byte attributes, byte hint, ACL rights, SyncData fsd, int uidnext) {
        if (hint != TYPE_UNKNOWN)
            meta.put(Metadata.FN_VIEW, hint);
        if (attributes != 0)
            meta.put(Metadata.FN_ATTRS, attributes);
        if (uidnext > 0)
            meta.put(Metadata.FN_UIDNEXT, uidnext);
        if (rights != null)
            meta.put(Metadata.FN_RIGHTS, rights.encode());
        if (fsd != null && fsd.url != null && !fsd.url.equals("")) {
            meta.put(Metadata.FN_URL, fsd.url);
            meta.put(Metadata.FN_SYNC_GUID, fsd.lastGuid);
            if (fsd.lastDate > 0)
                meta.put(Metadata.FN_SYNC_DATE, fsd.lastDate);
        }
        return MailItem.encodeMetadata(meta, color);
    }


    private static final String CN_ATTRIBUTES = "attributes";

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("folder: {");
        sb.append("n:\"").append(getName()).append("\", ");
        appendCommonMembers(sb).append(", ");
        sb.append(CN_ATTRIBUTES).append(": ").append(mAttributes);
        sb.append("}");
        return sb.toString();
    }
}
