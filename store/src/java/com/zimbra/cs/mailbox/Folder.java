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
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mailbox.ACLGrant;
import com.zimbra.common.mailbox.Color;
import com.zimbra.common.mailbox.FolderStore;
import com.zimbra.common.mailbox.ItemIdentifier;
import com.zimbra.common.mailbox.MailboxStore;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ArrayUtil;
import com.zimbra.common.util.ListUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbTag;
import com.zimbra.cs.imap.ImapSession;
import com.zimbra.cs.mailbox.MailItemState.AccessMode;
import com.zimbra.cs.mailbox.MailItem.CustomMetadata.CustomMetadataList;
import com.zimbra.cs.mailbox.cache.SharedState;
import com.zimbra.cs.mailbox.cache.SharedStateAccessor;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.soap.mail.type.RetentionPolicy;

/**
 * @since Aug 18, 2004
 */
public class Folder extends MailItem implements FolderStore, SharedState {
    public static class FolderOptions {
        private byte attributes;
        private Type defaultView = MailItem.Type.UNKNOWN;
        private int flags;
        private Color color = DEFAULT_COLOR_RGB;
        private String url;
        private Long date;
        private String uuid;
        private CustomMetadata custom;

        public FolderOptions setAttributes(byte attributes) {
            this.attributes = attributes;
            return this;
        }

        public byte getAttributes() {
            return attributes;
        }

        public FolderOptions setDefaultView(Type view) {
            if (view != null) {
                this.defaultView = view;
            }
            return this;
        }

        public Type getDefaultView() {
            return defaultView;
        }

        public FolderOptions setFlags(int flags) {
            this.flags = flags;
            return this;
        }

        public int getFlags() {
            return flags;
        }

        public FolderOptions setColor(byte bcolor) {
            return setColor(new Color(bcolor));
        }

        public FolderOptions setColor(Color color) {
            this.color = color;
            return this;
        }

        public Color getColor() {
            return color;
        }

        public FolderOptions setUrl(String url) {
            this.url = url;
            return this;
        }

        public String getUrl() {
            return url;
        }

        public FolderOptions setDate(Long date) {
            this.date = date;
            return this;
        }

        public Long getDate() {
            return date;
        }

        public FolderOptions setUuid(String uuid) {
            this.uuid = uuid;
            return this;
        }

        public String getUuid() {
            return uuid;
        }

        public FolderOptions setCustomMetadata(CustomMetadata custom) {
            this.custom = custom;
            return this;
        }

        public CustomMetadata getCustomMetadata() {
            return custom;
        }
    }

    public static final class SyncData {
        String url;
        String lastGuid;
        long   lastDate;
        boolean stop;

        SyncData(String url) {
            this.url = url;
        }

        SyncData(SyncData sd) {
            this(sd.url, sd.lastGuid, sd.lastDate);
        }

        SyncData(String url, String guid, long date) {
            this.url = url;
            this.lastGuid = guid == null ? null : guid.trim();
            this.lastDate = date;
        }

        public boolean alreadySeen(String guid, Date date) {
            if (date != null) {
                return lastDate >= date.getTime();
            } else if (stop) {
                return true;
            } else if (guid == null || lastGuid == null || !guid.trim().equalsIgnoreCase(lastGuid)) {
                return false;
            } else {
                return (this.stop = true);  // note the assignment
            }
        }

        public long getLastSyncDate() { return lastDate; }
    }

    public static final byte FOLDER_IS_IMMUTABLE      = 0x01;
    public static final byte FOLDER_DONT_TRACK_COUNTS = 0x02;

    protected byte    attributes;
    protected Type    defaultView;
    private SyncData  syncData;
    private boolean   activeSyncDisabled;
    private int webOfflineSyncDays;
    private ParentLocator parentLocator;

    Folder(Mailbox mbox, UnderlyingData ud) throws ServiceException {
        this(mbox, ud, false);
    }

    Folder(Mailbox mbox, UnderlyingData ud, boolean skipCache) throws ServiceException {
        super(mbox, ud, skipCache);
        parentLocator = LC.redis_cache_synchronize_folders_tags.booleanValue() ? new ReferencedParentLocator() : new DirectParentLocator();
        init();
    }

    Folder(Account acc, UnderlyingData data, int mailboxId) throws ServiceException {
        super(acc, data, mailboxId);
        init();
    }

    private void init() throws ServiceException {
        switch (getType()) {
            case FOLDER:
            case SEARCHFOLDER:
            case MOUNTPOINT:
                break;
            default:
                throw new IllegalArgumentException();
        }

        assert getState().getRetentionPolicy() != null; // Should have been set in decodeMetadata().
    }

    @Override public String getSender() {
        return "";
    }

    /** Returns the folder's absolute path.  Paths are UNIX-style with
     *  <tt>'/'</tt> as the path delimiter.  Paths are relative to the user
     *  root folder ({@link Mailbox#ID_FOLDER_USER_ROOT}), which has the path
     *  <tt>"/"</tt>.  So the Inbox's path is <tt>"/Inbox"</tt>, etc.
     * @throws ServiceException */
    @Override public String getPath() {
        if (mId == Mailbox.ID_FOLDER_ROOT || mId == Mailbox.ID_FOLDER_USER_ROOT) {
            return "/";
        }
        setupParent();
        String parentPath = parentLocator.getParent().getPath();
        return parentPath + (parentPath.equals("/") ? "" : "/") + getName();
    }

    /**
     * Returns the "hint" as to which view to use to display the folder's contents. For instance, if the default view
     * for the folder is {@link Type#APPOINTMENT}, the client would render the  contents using the calendar app.
     * Defaults to {@link Type#UNKNOWN}.
     */
    public Type getDefaultView() {
        return defaultView;
    }

    /** Returns the folder's set of special attributes.
     * @see #FOLDER_IS_IMMUTABLE
     * @see #FOLDER_DONT_TRACK_COUNTS */
    public byte getAttributes() {
        return attributes;
    }

    /** Returns the number of non-subfolder items in the folder.  (For
     *  example: messages, contacts, and appointments.)  <i>(Note that this
     *  is not recursive and thus does not include the items in the folder's
     *  subfolders.)</i> */
    public long getItemCount() {
        return getSize();
    }

    public int getDeletedCount() {
        return getState().getDeletedCount();
    }

    public int getDeletedUnreadCount() {
        return getState().getDeletedUnreadCount();
    }

    /** Returns the sum of the sizes of all items in the folder.  <i>(Note
     *  that this is not recursive and thus does not include the items in the
     *  folder's subfolders.)</i> */
    @Override
    public long getTotalSize() {
        return getState().getTotalSize();
    }

    /** Returns the URL the folder syncs to, or <tt>""</tt> if there is no
     *  such association.
     * @see #setUrl(String) */
    public String getUrl() {
        return (syncData == null || syncData.url == null ? "" : syncData.url);
    }

    /** Returns the URL the folder syncs to, or <tt>""</tt> if there is no
     *  such association.
     * @see #setUrl(String) */
    public SyncData getSyncData() {
        return syncData == null ? null : new SyncData(syncData);
    }

    /** Returns the date the folder was last sync'ed from an external
     *  data source. */
    public long getLastSyncDate() {
        return syncData == null ? 0 : syncData.lastDate;
    }

    private boolean writableImapSessionActive() {
        for (Session s : mMailbox.getNotificationPubSub().getSubscriber().getListeners(Session.Type.IMAP)) {
            ImapSession i4session = (ImapSession) s;
            if (i4session.getFolderItemIdentifier().id == mId && i4session.isWritable()) {
                return true;
            }
        }
        /*
         * For performance reasons, IMAP sessions and therefore WaitSets are not completely discarded when a
         * folder is no longer selected (in case it is re-selected soon) unless
         * <b>DebugConfig.imapTerminateSessionOnClose</b> is true (default value is false)
         * Therefore this code potentially returns false positives for selected folders:
         *
         *     if (WaitSetMgr.isMonitoringFolderForImap(mMailbox.getAccountId(), mId)) {
         *         return true;
         *     }
         *
         * Note that in the local case, isWriteable() returns false for such sessions which is why
         * this code works in the local case.
         *
         * In practice, each upstream IMAP server knows which folders are selected for the clients it
         * serves.
         * Therefore, this shortcoming doesn't matter as long as that server is the only one
         * which serves a particular mailbox.  If there are more IMAP servers, then ZCS-3248 will need fixing,
         * perhaps by enhancing the WaitSet code to know which folders are actually selected for IMAP.
         */
        return false;
    }

    /** Returns the last assigned item ID in the enclosing Mailbox the last
     *  time the folder was accessed via a read/write IMAP session.  If there
     *  is such a session already active, returns the last item ID in the
     *  Mailbox.  This value is used to calculate the \Recent flag when it
     *  has not already been cached. */
    public int getImapRECENTCutoff() {
        if (writableImapSessionActive()) {
            return mMailbox.getLastItemId();
        }
        return getState().getImapRECENTCutoff();
    }

    /** Returns the number of messages in the folder that would be considered
     *  \Recent in an IMAP session.  If there is currently a READ-WRITE IMAP
     *  session open on the folder, by definition all other IMAP connections
     *  will see no \Recent messages.  <i>(Note that as such, this method
     *  should <u>not</u> be called by IMAP sessions that have this folder
     *  selected.)</i>  Otherwise, it is the number of messages/chats/contacts
     *  added to the folder, moved to the folder, or edited in the folder
     *  since the last such IMAP session. */
    public int getImapRECENT() throws ServiceException {
        // no contents means no \Recent items (duh)
        if (getSize() == 0) {
            return 0;
        }
        // if there's a READ-WRITE IMAP session active on the folder, by definition there are no \Recent messages
        if (writableImapSessionActive()) {
            return 0;
        }
        // if no active sessions, use a cached value if possible
        int val = getState().getImapRECENT();
        if (val >= 0) {
            return val;
        }
        // final option is to calculate the number of \Recent messages
        markItemModified(Change.SIZE);
        int imapRecent = DbMailItem.countImapRecent(this, getImapRECENTCutoff());
        getState().setImapRECENT(imapRecent);
        return imapRecent;
    }

    /** Returns one higher than the IMAP ID of the last item added to the
     *  folder.  This is used as the UIDNEXT value when returning the folder
     *  via IMAP. */
    @Override
    public int getImapUIDNEXT() {
        return getState().getImapUIDNEXT();
    }

    private void setImapUIDNEXT(int val) {
        getState().setImapUIDNEXT(val);
    }

    /** Returns the change number of the last time
     *  (a) an item was inserted into the folder or
     *  (b) an item in the folder had its flags or tags changed.
     *  This data is used to enable IMAP client synchronization via the CONDSTORE extension. */
    @Override
    public int getImapMODSEQ() {
        return getState().getImapMODSEQ();
    }

    @Override
    public MailItem snapshotItem() throws ServiceException {
        return snapshotItem(true);
    }

    public MailItem snapshotItem(boolean snapshotParent) throws ServiceException {
        Folder parent = getParentFolder();
        Folder retVal = (Folder) super.snapshotItem();
        if (snapshotParent && (parent != null)) {
            retVal.getState().setParentFolder(parent);
        }
        return retVal;
    }

    /** Returns whether the folder is the Trash folder or any of its
     *  subfolders. */
    @Override public boolean inTrash() {
        if (mId <= Mailbox.HIGHEST_SYSTEM_ID) {
            return (mId == Mailbox.ID_FOLDER_TRASH);
        }
        setupParent();
        return getParentFolder().inTrash();
    }

    /** Returns whether the folder is the Junk folder. */
    @Override public boolean inSpam() {
        return (mId == Mailbox.ID_FOLDER_SPAM);
    }

    /**
     * Returns whether the folder is client-visible. Folders below the user root folder
     * ({@link Mailbox#ID_FOLDER_USER_ROOT}) are visible; all others are hidden.
     *
     * @see Mailbox#initialize()
     */
    @Override
    public boolean isHidden() {
        switch (mId) {
            case Mailbox.ID_FOLDER_USER_ROOT:
                return false;
            case Mailbox.ID_FOLDER_ROOT:
                return true;
            default:
                return getParentFolder().isHidden();
        }
    }

    /**
     * Returns true if specified folder is a descendant of this folder.
     * @param folder the folder whose ancestry is to be checked
     * @return true if the folder is a descendant, false otherwise
     * @throws ServiceException if an error occurred
     */
    public boolean isDescendant(Folder folder) throws ServiceException {
        while (true) {
            int parentId = folder.getFolderId();
            if (parentId == getId())
                return true;
            if (parentId == Mailbox.ID_FOLDER_ROOT)
                return false;
            folder = folder.getMailbox().getFolderById(null, parentId);
        }
    }

    /** Returns the subset of the requested access rights that the user has
     *  been granted on this folder.  The owner of the {@link Mailbox} has
     *  all rights on all items in the Mailbox, as do all admin accounts.
     *  All other users must be explicitly granted access.<p>
     *
     *  The set of rights that apply to a given folder is derived by starting
     *  at that folder and going up the folder hierarchy.  If we hit a folder
     *  that has a set of rights explicitly set on it, we stop and use those.
     *  If we hit a folder that doesn't inherit priviliges from its parent, we
     *  stop and treat it as if no rights are granted on the target folder.
     *  In other words, take the *first* (and only the first) of the following
     *  that exists, stopping at "do not inherit" folders:<ul>
     *    <li>the set of rights granted on the folder directly
     *    <li>the set of inherited rights granted on the folder's parent
     *    <li>the set of inherited rights granted on the folder's grandparent
     *    <li>...
     *    <li>the set of inherited rights granted on the mailbox's root folder
     *    <li>no rights at all</ul><p>
     *
     *  So if the folder hierarchy looks like this:<pre>
     *                   root  <- "read+write" granted to user A
     *                   /  \
     *                  V    W  <- "do not inherit" flag set
     *                 /    / \
     *                X    Y   Z  <- "read" granted to users A and B</pre>
     *  then user A has "write" rights on folders V and X, but not W, Y, and Z,
     *  user A has "read" rights on folders V, X, and Z but not W or Y, and
     *  user B has "read" rights on folder Z but not V, X, W, or Y.
     *
     * @param rightsNeeded  A set of rights (e.g. {@link ACL#RIGHT_READ}
     *                      and {@link ACL#RIGHT_DELETE}).
     * @param authuser      The user whose rights we need to query.
     * @see ACL */
    @Override short checkRights(short rightsNeeded, Account authuser, boolean asAdmin) throws ServiceException {
        if (rightsNeeded == 0)
            return rightsNeeded;
        // XXX: in Mailbox, authuser is set to null if authuser == owner.
        // the mailbox owner can do anything they want
        if (authuser == null || authuser.getId().equals(mMailbox.getAccountId()))
            return rightsNeeded;
        // check admin access
        if (AccessManager.getInstance().canAccessAccount(authuser, getAccount(), asAdmin))
            return rightsNeeded;

        return checkACL(rightsNeeded, authuser, asAdmin);
    }


    /** Returns the retention policy for this folder.  Does not return {@code null}. */
    public RetentionPolicy getRetentionPolicy() {
        return getState().getRetentionPolicy();
    }

    public void setRetentionPolicy(RetentionPolicy rp) throws ServiceException {
        if (!canAccess(ACL.RIGHT_ADMIN)) {
            throw ServiceException.PERM_DENIED("you do not have admin rights to folder " + getPath());
        }

        markItemModified(Change.RETENTION_POLICY);
        getState().setRetentionPolicy(rp == null ? new RetentionPolicy() : rp);
        saveMetadata();
    }

    /** Returns this folder's parent folder.  The root folder's parent is itself.
     * @see Mailbox#ID_FOLDER_ROOT */
    @Override
    public MailItem getParent() throws ServiceException {
        return getFolder();
    }

    /** Returns this folder's parent folder.  The root folder's parent is itself.
     * @see Mailbox#ID_FOLDER_ROOT */
    @Override
    Folder getFolder() throws ServiceException {
        Folder parent = getParentFolder();
        return parent != null ? parent : super.getFolder();
    }

    /** Tries as far as possible to ensure "parent" has a non-null value. */
    private void setupParent() {
        parentLocator.setupParent();
    }

    @Override
    public List<ACLGrant> getACLGrants() {
        ACL myACL = getEffectiveACL();
        if (null == myACL) {
            return Lists.newArrayListWithExpectedSize(0);
        }
        return Lists.newCopyOnWriteArrayList(myACL.getGrants());
    }

    /** Returns whether the folder contains any subfolders. */
    @Override
    public boolean hasSubfolders() {
        Map<String, Integer> subfolders = getSubfolders();
        return (subfolders != null && !subfolders.isEmpty());
    }

    /**
     * Returns the subfolder with the given name.  Name comparisons are case-insensitive.
     *
     * @param name  The folder name to search for.
     * @return The matching subfolder, or {@code null} if no such folder exists.
     */
    Folder findSubfolder(String name) {

        Map<String, Integer> subfolders = getSubfolders();
        if (name == null || subfolders == null || subfolders.isEmpty()) {
            return null;
        }
        name = StringUtil.trimTrailingSpaces(name);
        Integer subfolderId = subfolders.get(name.toLowerCase());
        try {
            return subfolderId == null ? null: mMailbox.getFolderById(subfolderId);
        } catch (ServiceException e) {
            ZimbraLog.mailbox.error("subfolder '%s' of folder '%s' does not exist", name, getName());
            return null;
        }
    }

    private static final class SortByName implements Comparator<Folder> {
        @Override
        public int compare(Folder f1, Folder f2) {
            String n1 = f1.getName();
            String n2 = f2.getName();
            return n1.compareToIgnoreCase(n2);
        }
    }

    private Map<String, Integer> getSubfolders() {
        return getState().getSubfolders();
    }

    private Folder getParentFolder()  {
        return parentLocator.getParent();
    }

    /**
     * Returns an unmodifiable list of the folder's subfolders sorted by name.  The sort is case-insensitive.
     */
    public List<Folder> getSubfolders(OperationContext octxt) throws ServiceException {
        Map<String, Integer> subfolders = getSubfolders();
        if (subfolders == null) {
            return Collections.emptyList();
        }
        ArrayList<Folder> visible = new ArrayList<Folder>();
        if (octxt == null || octxt.getAuthenticatedUser() == null) {
        	for (int subfolderId: subfolders.values()) {
        		try {
        			visible.add(mMailbox.getFolderById(subfolderId));
        		} catch (ServiceException e) {
        			ZimbraLog.mailbox.error("error while getting the folder by id: %s, ignoring to add", subfolderId, e);
        		}
        	}
        } else {
            for (int subfolderId : subfolders.values()) {
                Folder subfolder = mMailbox.getFolderById(subfolderId);
                if (subfolder.canAccess(ACL.RIGHT_READ, octxt.getAuthenticatedUser(), octxt.isUsingAdminPrivileges())) {
                    visible.add(subfolder);
                }
            }
        }
        Collections.sort(visible, new SortByName());
        return visible;
    }

    /** Returns a <tt>List</tt> that includes this folder and all its
     *  subfolders visible to the user in OperationContext.
     */
    public List<Folder> getSubfolderHierarchy(OperationContext octxt) throws ServiceException {
        ArrayList<Folder> subfolders = new ArrayList<Folder>();
        subfolders.add(this);
        List<Folder> visible = getSubfolders(octxt);
        for (Folder f : visible) {
            subfolders.add(f);
            subfolders.addAll(f.getSubfolderHierarchy(octxt));
        }
        return subfolders;
    }

    /** Returns a <tt>List</tt> that includes this folder and all its
     *  subfolders.  The tree traversal is done depth-first, so this folder
     *  is the first element in the list, followed by its children, then
     *  its grandchildren, etc. */
    public List<Folder> getSubfolderHierarchy() {
        return accumulateHierarchy(new ArrayList<Folder>());
    }

    private List<Folder> accumulateHierarchy(List<Folder> list) {
        list.add(this);
        Map<String, Integer> subfolders = getSubfolders();
        if (subfolders != null && !subfolders.isEmpty()) {
            for (int subfolderId : subfolders.values()) {
                Folder subfolder;
                try {
                    subfolder = mMailbox.getFolderById(subfolderId);
                    subfolder.accumulateHierarchy(list);
                } catch (ServiceException e) {
                    ZimbraLog.mailbox.error("error accumulating folder hierarchy", e);
                }
            }
        }
        return list;
    }

    public void setParentId(int parentId) {
        getState().setParentId(parentId);
    }

    public void setParent(Folder folder) {
        getState().setParentFolder(folder);
    }

    /** Updates the number of items in the folder and their total size.  Only
     *  "leaf node" items in the folder are summed; items in subfolders are
     *  included only in the size of the subfolder.
     * @param countDelta    The change in item count, negative or positive.
     * @param deletedDelta  The change in number of IMAP \Deleted items.
     * @param sizeDelta     The change in total size, negative or positive.*/
    void updateSize(int countDelta, int deletedDelta, long sizeDelta) throws ServiceException {
        if (!trackSize()) {
            return;
        }
        markItemModified(Change.SIZE);
        if (countDelta > 0) {
            updateUIDNEXT();
        }
        if (countDelta != 0) {
            updateHighestMODSEQ();
        }
        // reset the RECENT count unless it's just a change of \Deleted flags
        if (countDelta != 0 || sizeDelta != 0 || deletedDelta == 0) {
            getState().setImapRECENT(-1);
        }
        // if we go negative, that's OK!  just pretend we're at 0.
        FolderState fields = getState();
        fields.setSize(Math.max(0, getSize() + countDelta));
        fields.setTotalSize(Math.max(0, getTotalSize() + sizeDelta));
        fields.setDeletedCount((int) Math.min(Math.max(0, getDeletedCount() + deletedDelta), getSize()));
    }

    /** Sets the number of items in the folder and their total size.
     * @param count          The folder's new item count.
     * @param deletedCount   The folder's number of IMAP \Deleted items.
     * @param totalSize      The folder's new total size.
     * @param deletedUnread  The folder's number of unread \Deleted items. */
    public void setSize(long count, int deletedCount, long totalSize, int deletedUnread) throws ServiceException {
        if (!trackSize()) {
            return;
        }
        markItemModified(Change.SIZE);
        if (count > getSize()) {
            updateUIDNEXT();
        }
        if (count != getSize()) {
            updateHighestMODSEQ();
            getState().setImapRECENT(-1);
        }

        FolderState fields = getState();
        fields.getUnderlyingData().size = totalSize;
        fields.setTotalSize(totalSize);
        fields.setDeletedCount(deletedCount);
        fields.setDeletedUnreadCount(deletedUnread);
    }

    @Override protected void updateUnread(int delta, int deletedDelta) throws ServiceException {
        if (!trackUnread()) {
            return;
        }
        super.updateUnread(delta, deletedDelta);

        if (deletedDelta != 0) {
            markItemModified(Change.UNREAD);
            getState().setDeletedUnreadCount(Math.min(Math.max(0, getState().getDeletedUnreadCount() + deletedDelta), getUnreadCount()));
        }

        if (delta != 0) {
            updateHighestMODSEQ();
        }
    }

    /** Sets the folder's UIDNEXT item ID highwater mark to one more than
     *  the Mailbox's last assigned item ID. */
    void updateUIDNEXT() throws ServiceException {
        int uidnext = mMailbox.getLastItemId() + 1;
        if (trackImapStats() && getImapUIDNEXT() < uidnext) {
            markItemModified(Change.SIZE);
            setImapUIDNEXT(uidnext);
        }
    }

    /** Sets the folder's MODSEQ change ID highwater mark to the Mailbox's
     *  current change ID. */
    void updateHighestMODSEQ() throws ServiceException {
        int modseq = mMailbox.getOperationChangeID();
        if (trackImapStats() && getState().getImapMODSEQ() < modseq) {
            markItemModified(Change.SIZE);
            getState().setImapMODSEQ(modseq);
        }
    }

    /** Sets the folder's RECENT item ID highwater mark to the Mailbox's
     *  last assigned item ID. */
    void checkpointRECENT() throws ServiceException {
        if (getImapRECENTCutoff() == mMailbox.getLastItemId())
            return;

        markItemModified(Change.INTERNAL_ONLY);
        getState().setImapRECENT(0);
        getState().setImapRECENTCutoff(mMailbox.getLastItemId());
        saveFolderCounts(false);
    }

    /** Persists the folder's current unread/message counts and IMAP UIDNEXT
     *  value to the database.
     * @param initial  Whether this is the first time we're saving folder
     *                 counts, in which case we also initialize the IMAP
     *                 UIDNEXT and HIGHESTMODSEQ values. */
    protected void saveFolderCounts(boolean initial) throws ServiceException {
        FolderState fields = getState();
        if (initial) {
            fields.setImapUIDNEXT(mMailbox.getLastItemId() + 1);
            fields.setImapMODSEQ(mMailbox.getLastChangeID());
        }
        DbMailItem.persistCounts(this, encodeMetadata());
        if (ZimbraLog.mailbox.isDebugEnabled()) {
            ZimbraLog.mailbox.debug("\"%s\": updating folder counts (c%d/d%d/u%d/du%d/s%d)", getName(),
                    getSize(), fields.getDeletedCount(), getUnreadCount(), fields.getDeletedUnreadCount(), fields.getTotalSize());
        }
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
        return ((attributes & FOLDER_IS_IMMUTABLE) == 0);
    }
    @Override
    boolean isMutable() {
        return ((attributes & FOLDER_IS_IMMUTABLE) == 0);
    }

    @Override
    boolean canHaveChildren() {
        return true;
    }

    @Override
    public boolean isDeletable() {
        return ((attributes & FOLDER_IS_IMMUTABLE) == 0);
    }

    @Override
    boolean isLeafNode() {
        return false;
    }

    @Override
    boolean trackUnread() {
        return ((attributes & FOLDER_DONT_TRACK_COUNTS) == 0);
    }

    boolean trackSize() {
        return ((attributes & FOLDER_DONT_TRACK_COUNTS) == 0);
    }

    boolean trackImapStats() {
        return ((attributes & FOLDER_DONT_TRACK_COUNTS) == 0);
    }

    @Override
    boolean canParent(MailItem child) {
        return (child instanceof Folder);
    }

    /**
     * Returns whether the folder can contain the given item. We make the same checks as in {@link #canContain(byte)},
     * and we also make sure to avoid any cycles of folders.
     *
     * @param child the {@link MailItem} object to check
     */
    boolean canContain(MailItem child) {
        if (!canContain(child.getType())) {
            return false;
        } else if (child instanceof Folder) {
            // may not contain our parents or grandparents (c.f. Back to the Future)
            Folder folder = this;
            while (folder.getId() != Mailbox.ID_FOLDER_ROOT) {
                Folder parent = folder.getParentFolder();
                if (folder.getId() == child.getId()) {
                    ZimbraLog.mailop.warn("Attempting to place folder='%s' underneath itself", this);
                    return false;
                }
                if (parent == null) {
                    if (folder == this) {
                        ZimbraLog.mailop.warn("folder='%s' has null parent", this);
                    } else {
                        ZimbraLog.mailop.warn("folder='%s' has ancestor '%s' which has null parent",
                                this, folder);
                    }
                    folder.setupParent();
                    if (folder.getParentFolder() == null) {
                        return false;
                    }
                }
                folder = folder.getParentFolder();
            }
        }
        return true;
    }

    /**
     * Returns whether the folder can contain objects of the given type. In general, any folder can contain any object.
     * The exceptions are:
     * <ul>
     *  <li>The Tags folder can only contain {@link Tag}s (and vice versa)
     *  <li>The Conversations folder can only contain {@link Conversation}s (and vice versa)
     *  <li>The Spam folder can't have subfolders.
     * <ul>
     *
     * @param type the type of object, e.g. {@link MailItem#TYPE_TAG}
     */
    boolean canContain(Type type) {
        if ((type == Type.TAG || type == Type.SMARTFOLDER) != (mId == Mailbox.ID_FOLDER_TAGS)) {
            return false;
        } else if ((type == Type.CONVERSATION) != (mId == Mailbox.ID_FOLDER_CONVERSATIONS)) {
            return false;
        } else if (type == Type.FOLDER && !mMailbox.isChildFolderPermitted(mId)) {
            return false;
        }
        return true;
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
     * @param date        Date (timestamp) for the new folder (in millis
     *                    since Unix epoch).  If null, use the current time.
     * @param url         The (optional) url to sync folder contents to.
     * @param custom      An optional extra set of client-defined metadata.
     * @perms {@link ACL#RIGHT_INSERT} on the parent folder
     * @throws ServiceException   The following error codes are possible:<ul>
     *    <li><tt>mail.CANNOT_CONTAIN</tt> - if the target folder can't have
     *        subfolders
     *    <li><tt>mail.ALREADY_EXISTS</tt> - if a folder by that name already
     *        exists in the parent folder
     *    <li><tt>mail.INVALID_NAME</tt> - if the new folder's name is invalid
     *    <li><tt>service.FAILURE</tt> - if there's a database failure
     *    <li><tt>service.PERM_DENIED</tt> - if you don't have sufficient
     *        permissions</ul>
     * @see #validateItemName(String)
     * @see #canContain(byte)
     * @see #FOLDER_IS_IMMUTABLE
     * @see #FOLDER_DONT_TRACK_COUNTS
     */
    @SuppressWarnings("deprecation")
    static Folder create(int id, String uuid, Mailbox mbox, Folder parent, String name, byte attributes, Type view, int flags,
            Color color, Long date, String url, CustomMetadata custom)
    throws ServiceException {
        if (id != Mailbox.ID_FOLDER_ROOT) {
            if (parent == null || !parent.canContain(Type.FOLDER)) {
                throw MailServiceException.CANNOT_CONTAIN(parent, Type.FOLDER);
            }
            name = validateItemName(name);
            if (parent.findSubfolder(name) != null) {
                throw MailServiceException.ALREADY_EXISTS(name);
            } else if (!parent.canAccess(ACL.RIGHT_SUBFOLDER)) {
                throw ServiceException.PERM_DENIED("you do not have the required rights on the parent folder");
            }
        }
        if (view == Type.INVITE) {
            throw MailServiceException.INVALID_TYPE(view.toString());
        }

        UnderlyingData data = new UnderlyingData();
        data.uuid = uuid;
        data.id = id;
        data.type = Type.FOLDER.toByte();
        data.folderId = (id == Mailbox.ID_FOLDER_ROOT ? id : parent.getId());
        data.parentId = data.folderId;
        data.date = date == null ?  mbox.getOperationTimestamp() : (int) (date / 1000);
        data.setFlags((flags | Flag.toBitmask(mbox.getAccount().getDefaultFolderFlags())) & Flag.FLAGS_FOLDER);
        data.name = name;
        data.setSubject(name);
        data.metadata = encodeMetadata(color, 1, 1, custom, attributes, view, null, new SyncData(url), id + 1, 0,
                mbox.getOperationChangeID(), -1, 0, 0, 0, null, false, -1);
        data.contentChanged(mbox);
        ZimbraLog.mailop.debug("adding folder %s: id=%d, parentId=%d.", name, data.id, data.parentId);
        new DbMailItem(mbox).create(data);

        Folder folder = new Folder(mbox, data);
        folder.finishCreation(parent);
        return folder;
    }

    /**
     * Change the default view of this Folder.  Currently this call is only used during migration to correct a
     * folder created with wrong view.
     *
     * @param view the new default view of this folder
     */
    void setDefaultView(Type view) throws ServiceException {
        if (!isMutable()) {
            throw MailServiceException.IMMUTABLE_OBJECT(mId);
        }
        if (!canAccess(ACL.RIGHT_WRITE)) {
            throw ServiceException.PERM_DENIED("you do not have the required rights on the folder");
        }
        if (view == defaultView) {
            return;
        }
        markItemModified(Change.VIEW);
        defaultView = view;
        saveMetadata();
    }

    /** Sets the remote URL for the folder.  This can point to a remote
     *  calendar (<tt>.ics</tt> file), an RSS feed, etc.  Note that you
     *  cannot add a remote data source to an existing folder, as refreshing
     *  the linked content empties the folder.<p>
     *
     *  This is <i>not</i> used to mount other Zimbra users' folders; to do
     *  that, use a {@link Mountpoint}.
     *
     * @param url  The new URL for the folder, or <tt>null</tt> to remove the
     *             association with a remote object.
     * @perms {@link ACL#RIGHT_WRITE} on the folder
     * @throws ServiceException   The following error codes are possible:<ul>
     *    <li><tt>mail.CANNOT_SUBSCRIBE</tt> - if you're attempting to
     *        associate a URL with an existing, normal folder
     *    <li><tt>mail.IMMUTABLE_OBJECT</tt> - if the folder can't be modified
     *    <li><tt>service.FAILURE</tt> - if there's a database failure
     *    <li><tt>service.PERM_DENIED</tt> - if you don't have sufficient
     *        permissions</ul> */
    void setUrl(String url) throws ServiceException {
        if (url == null) {
            url = "";
        }
        if (getUrl().equals(url)) {
            return;
        }
        if (getUrl().isEmpty() && !url.isEmpty()) {
            throw MailServiceException.CANNOT_SUBSCRIBE(mId);
        }
        if (!isMutable()) {
            throw MailServiceException.IMMUTABLE_OBJECT(mId);
        }
        if (!canAccess(ACL.RIGHT_WRITE)) {
            throw ServiceException.PERM_DENIED("you do not have the required rights on the folder");
        }
        markItemModified(Change.URL);
        syncData = new SyncData(url);
        saveMetadata();
    }

    /**
     * Sets the number of days for which web client would sync the folder data for offline use.
     *
     * @param days
     * @throws ServiceException
     */
    void setWebOfflineSyncDays(int days) throws ServiceException {
        if (!canAccess(ACL.RIGHT_WRITE)) {
            throw ServiceException.PERM_DENIED("you do not have the required rights on the folder");
        }
        if (days < 0 || days > getAccount().getWebClientOfflineSyncMaxDays()) {
            throw ServiceException.INVALID_REQUEST("invalid web offline folder sync days: " + days, null);
        }
        markItemModified(Change.METADATA);
        webOfflineSyncDays = days;
        saveMetadata();
    }

    /**
     * Gets the number of days for which web client would sync the folder data for offline use.
     *
     * @return
     * @throws ServiceException if there's an error in getting the {@link Account} object corresponding
     *                          to the mailbox.
     */
    public int getWebOfflineSyncDays() throws ServiceException {
        int id = getId();
        if (webOfflineSyncDays < 0 &&
                (id == Mailbox.ID_FOLDER_INBOX || id == Mailbox.ID_FOLDER_SENT || id == Mailbox.ID_FOLDER_DRAFTS ||
                        id == Mailbox.ID_FOLDER_TRASH)) {
            // sync days property has not been set by the user on Inbox/Sent/Drafts/Trash
            return getAccount().getWebClientOfflineSyncMaxDays();
        } else if (webOfflineSyncDays < 0) {
            return 0;
        } else {
            return webOfflineSyncDays;
        }
    }

    /** Records the last-synced information for a subscribed folder.  If the
     *  folder does not have an associated URL, no action is taken and no
     *  exception is thrown.
     *
     * @param guid  The last synchronized remote item's GUID.
     * @param date  The last synchronized remote item's timestamp, or the last-modified time of the remote feed,
     *              whichever is more recent
     * @perms {@link ACL#RIGHT_WRITE} on the folder
     * @throws ServiceException   The following error codes are possible:<ul>
     *    <li><tt>mail.IMMUTABLE_OBJECT</tt> - if the folder can't be modified
     *    <li><tt>service.FAILURE</tt> - if there's a database failure
     *    <li><tt>service.PERM_DENIED</tt> - if you don't have sufficient
     *        permissions</ul> */
    void setSubscriptionData(String guid, long date) throws ServiceException {
        if (getUrl().isEmpty()) {
            return;
        }
        if (!isMutable()) {
            throw MailServiceException.IMMUTABLE_OBJECT(mId);
        }
        if (!canAccess(ACL.RIGHT_WRITE)) {
            throw ServiceException.PERM_DENIED("you do not have the required rights on the folder");
        }
        markItemModified(Change.URL);
        syncData = new SyncData(getUrl(), guid, date);
        saveMetadata();
    }

    void setSyncDate(long date) throws ServiceException {
        if (!canAccess(ACL.RIGHT_WRITE)) {
            throw ServiceException.PERM_DENIED("you do not have the required rights on the folder");
        }
        markItemModified(Change.URL);
        if (syncData == null) {
            syncData = new SyncData(null, null, date);
        } else {
            syncData.lastDate = date;
        }
        saveMetadata();
    }

    public boolean isActiveSyncDisabled() {
        return activeSyncDisabled;
    }

    public void setActiveSyncDisabled(boolean disableActiveSync) throws ServiceException {
        if (!canAccess(ACL.RIGHT_WRITE)) {
            throw ServiceException.PERM_DENIED("you do not have the required rights on the folder");
        }
        //doesn't work on system folders!!
        if (mId < Mailbox.FIRST_USER_ID) {
            ZimbraLog.mailop.warn("Cannot disable activesync access for system folder " + mId);
            return;
        }

        markItemModified(Change.DISABLE_ACTIVESYNC);
        this.activeSyncDisabled = disableActiveSync;
        saveMetadata();
    }

    /** Updates the unread state of all items in the folder.  Persists the
     *  change to the database and cache, and also updates the unread counts
     *  for the folder and the affected items' parents and {@link Tag}s
     *  appropriately.  <i>Note: Folders may only be marked read, not
     *  unread.</i>
     *
     * @perms {@link ACL#RIGHT_READ} on the folder,
     *        {@link ACL#RIGHT_WRITE} on all affected messages. */
    @Override void alterUnread(boolean unread) throws ServiceException {
        if (unread) {
            throw ServiceException.INVALID_REQUEST("folders can only be marked read", null);
        } else if (!canAccess(ACL.RIGHT_READ)) {
            throw ServiceException.PERM_DENIED("you do not have sufficient permissions on the folder");
        } else if (!isUnread()) {
            return;
        }

        // first, fault in all the conversations for the folder's unread messages
        //   so that we don't fetch them one by one during the updateUnread()
        List<UnderlyingData> unreaddata = DbMailItem.getUnreadMessages(this);
        if (canAccess(ACL.RIGHT_WRITE)) {
            Set<Integer> conversations = new HashSet<Integer>(unreaddata.size());
            for (UnderlyingData data : unreaddata) {
                if (data.parentId > 0) {
                    conversations.add(data.parentId);
                }
            }
            mMailbox.getItemById(conversations, Type.CONVERSATION);
        }

        // mark all messages in this folder as read in memory; this implicitly
        //   decrements the unread count for its conversation, folder and tags
        List<Integer> targets = new ArrayList<Integer>();
        for (UnderlyingData data : unreaddata) {
            Message msg = mMailbox.getMessage(data);
            if (msg.checkChangeID() || !msg.canAccess(ACL.RIGHT_WRITE)) {
                msg.updateUnread(-1, msg.isTagged(Flag.FlagInfo.DELETED) ? -1 : 0);
                msg.metadataChanged();
                targets.add(msg.getId());
            }
        }

        // mark all messages in this folder as read in the database
        if (ZimbraLog.mailop.isDebugEnabled() && targets.size() > 0) {
            String state = unread ? "unread" : "read";
            String context = getMailopContext(this);
            for (List<Integer> ids : ListUtil.split(targets, 200)) {
                ZimbraLog.mailop.debug("marking messages in %s as %s.  ids: %s", context, state, StringUtil.join(",", ids));
            }
        }
        DbMailItem.alterUnread(mMailbox, targets, unread);
    }

    /** Tags or untags a folder.  Persists the change to the database and
     *  cache.  In most cases, we call {@link MailItem#alterTag(Tag, boolean)}
     *  and the action will be performed on the folder's contents.  <i>Note:
     *  At present, user tags and non-folder-specific flags cannot be applied
     *  or removed on a folder.</i>  For folder-specific flags like
     *  {@link Mailbox#mSubscribedFlag}, the tagging or untagging applies to
     *  the <tt>Folder</tt> itself.<p>
     *
     *  You must use {@link #alterUnread} to change a folder's unread state.<p>
     *
     *  Note that clearing the "no inherit" flag on a folder enables permission
     *  inheritance and hence clears the folder's ACL as a side-effect.
     *
     * @perms {@link ACL#RIGHT_WRITE} on the folder */
    @Override
    void alterTag(Tag tag, boolean newValue) throws ServiceException {
        // folder flags are applied to the folder, not the contents
        if (!(tag instanceof Flag) || (((Flag) tag).toBitmask() & Flag.FLAGS_FOLDER) == 0) {
            super.alterTag(tag, newValue);
            return;
        }

        if (newValue == isTagged(tag)) {
            return;
        }
        boolean isNoInheritFlag = tag.getId() == Flag.ID_NO_INHERIT;
        if (!canAccess(isNoInheritFlag ? ACL.RIGHT_ADMIN : ACL.RIGHT_WRITE)) {
            throw ServiceException.PERM_DENIED("you do not have the necessary privileges on the folder");
        }
        ACL effectiveACL = isNoInheritFlag ? getEffectiveACL() : null;
        if (effectiveACL != null && effectiveACL.isEmpty()) {
            effectiveACL = null;
        }
        // change the tag on the Folder itself, not on its contents
        tagChanged(tag, newValue);

        if (ZimbraLog.mailop.isDebugEnabled()) {
            ZimbraLog.mailop.debug("setting " + getMailopContext(tag) + " for " + getMailopContext(this));
        }
        DbTag.alterTag(tag, Arrays.asList(mId), newValue);

        if (isNoInheritFlag) {
            if (!newValue && state.getRights() != null) {
                // clearing the "no inherit" flag sets inherit ON and thus must clear the item's ACL
                state.setRights(null);
                saveMetadata();
            } else if (newValue) {
                // setting the "no inherit" flag turns inherit OFF and thus must make a copy of the item's effective ACL
                //   note: can't just call MailItem.setPermissions() because at this point inherit is OFF and mRights is NULL, so delegated admin would fail
                state.setRights(effectiveACL);
                saveMetadata();
            }
            // this should rightfully go *before* the MailItem.rights changes, but that would trigger a recursive blowup (*sigh*)
            markItemModified(Change.ACL);
        }
    }

    /** Renames the item and optionally moves it.  Altering an item's case
     *  (e.g. from <tt>foo</tt> to <tt>FOO</tt>) is allowed.  Trailing
     *  whitespace is stripped from the name.  If you don't want the item to be
     *  moved, you must pass <tt>folder.getFolder()</tt> as the second parameter.
     *
     * @perms {@link ACL#RIGHT_WRITE} on the folder to rename it,
     *        {@link ACL#RIGHT_DELETE} on the folder and
     *        {@link ACL#RIGHT_INSERT} on the target folder to move it */
    @Override
    void rename(String name, Folder target) throws ServiceException {
        name = validateItemName(name);
        String oldName = getName();
        boolean renamed = !name.equals(oldName);
        Folder parent = getParentFolder();
        if (!renamed && target == parent)
            return;
        Folder oldParent = (target == parent ? parent : null);
        super.rename(name, target);
        if (oldParent != null) { //null if moved; super.rename() already removed it from old
            oldParent.subfolderRenamed(oldName, name);
        }
        if (state.getRights() != null) {
            queueForAclPush();
        }

        // for Folder objects rename also means the change in the contents.
        contentChanged();
    }

    private void subfolderRenamed(String oldName, String name) {
        Map<String, Integer> subfolders = getSubfolders();
        Integer child = subfolders.remove(oldName.toLowerCase());
        subfolders.put(name.toLowerCase(), child);
        getState().setSubfolders(subfolders);
    }

    /** Moves this folder so that it is a subfolder of <tt>target</tt>.
     *
     * @perms {@link ACL#RIGHT_INSERT} on the target folder,
     *        {@link ACL#RIGHT_DELETE} on the folder being moved */
    @Override
    boolean move(Folder target) throws ServiceException {
        markItemModified(Change.FOLDER | Change.PARENT);
        if (getFolderId() == target.getId()) {
            return false;
        } else if (!isMovable()) {
            throw MailServiceException.IMMUTABLE_OBJECT(mId);
        } else if (!canAccess(ACL.RIGHT_DELETE)) {
            throw ServiceException.PERM_DENIED("you do not have the required permissions");
        } else if (target.getId() != Mailbox.ID_FOLDER_TRASH && target.getId() != Mailbox.ID_FOLDER_SPAM && !target.canAccess(ACL.RIGHT_INSERT)) {
            throw ServiceException.PERM_DENIED("you do not have the required permissions");
        } else if (!target.canContain(this)) {
            throw MailServiceException.CANNOT_CONTAIN();
        }

        boolean fromTrash = inTrash();
        boolean toTrash = target.inTrash();
        if (!fromTrash && toTrash) { // moving this folder into Trash
            onSoftDelete();
        }

        // tell the folder's old and new parents
        getParentFolder().removeChild(this);
        target.addChild(this);

        ZimbraLog.mailop.info("moving %s to %s", getMailopContext(this), getMailopContext(target));

        // and update the folder's data (in memory and DB)
        DbMailItem.setFolder(this, target);
        state.setFolderId(target.getId());
        state.setParentId(target.getId());
        metadataChanged();

        if (state.getRights() != null) {
            queueForAclPush();
        }

        return true;
    }

    private void onSoftDelete() throws ServiceException {
        alterUnread(false);
        Map<String, Integer> subfolders = getSubfolders();
        if (subfolders != null) { // call on all sub folders recursively
            for (int subfolderId : subfolders.values()) {
                Folder subfolder = mMailbox.getFolderById(subfolderId);
                if (subfolder != null) {
                    subfolder.onSoftDelete();
                }
            }
        }
    }

    @Override
    void addChild(MailItem child) throws ServiceException {
        addChild(child, true);
    }

    void addChild(MailItem child, boolean newChild) throws ServiceException {
        if (child == null || !canParent(child)) {
            throw MailServiceException.CANNOT_CONTAIN();
        } else if (child == this) {
            if (mId != Mailbox.ID_FOLDER_ROOT) {
                throw MailServiceException.CANNOT_CONTAIN();
            }
        } else if (!(child instanceof Folder)) {
            super.addChild(child);
        } else {
            if (newChild) {
                markItemModified(Change.CHILDREN);
            }
            Folder subfolder = (Folder) child;
            Map<String, Integer> subfolders = getSubfolders();
            if (subfolders == null) {
                subfolders = new LinkedHashMap<String, Integer>();
            } else {
                Folder existing = findSubfolder(subfolder.getName());
                if (existing == child) {
                    return;
                }
                if (existing != null) {
                    throw MailServiceException.ALREADY_EXISTS(subfolder.getName());
                }
            }
            subfolders.put(subfolder.getName().toLowerCase(), subfolder.getId());
            getState().setSubfolders(subfolders);
            subfolder.parentLocator.setParent(this);
        }
    }

    @Override
    void removeChild(MailItem child) throws ServiceException {
        if (child == null) {
            throw MailServiceException.CANNOT_CONTAIN();
        } else if (!(child instanceof Folder)) {
            super.removeChild(child);
        } else {
            markItemModified(Change.CHILDREN);
            Folder subfolder = (Folder) child;
            Map<String, Integer> subfolders = getSubfolders();
            if (subfolders == null || subfolders.isEmpty()) {
                throw MailServiceException.IS_NOT_CHILD();
            }
            if (subfolders.remove(subfolder.getName().toLowerCase()) == null) {
                if (subfolders.containsValue(subfolder.getId())) {
                    //edge case when folder has been moved and renamed in same txn
                    //the rename happens before removal from 'old' folder, so lookup by name won't work
                    Iterator<Integer> it = subfolders.values().iterator();
                    while (it.hasNext()) {
                        if (subfolder.getId() == it.next()) {
                            it.remove();
                            break;
                        }
                    }
                } else {
                    throw MailServiceException.IS_NOT_CHILD();
                }
            }
            getState().setSubfolders(subfolders);
            subfolder.parentLocator.unsetParent();
        }
    }

    /** Deletes this folder and all its subfolders. */
    @Override
    void delete(boolean writeTombstones) throws ServiceException {
        if (hasSubfolders()) {
            List<Folder> allSubfolders = getSubfolderHierarchy();
            // walking the list in the reverse order
            // so that the leaf folders are deleted first.
            // the loop stops shorts of deleting the first
            // item which is the current folder.
            for (int i = allSubfolders.size() - 1; i > 0; i--) {
                Folder subfolder = allSubfolders.get(i);
                subfolder.delete(writeTombstones);
            }
        }
        ZimbraLog.mailbox.info("deleting folder id=%d,path=%s", getId(), getPath());
        super.delete(writeTombstones);

        if (state.getRights() != null) {
            queueForAclPush();
        }
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
     *    <li><tt>mail.MODIFY_CONFLICT</tt> - if the caller specified a
     *        max change number and a modification check, and the modified
     *        change number of a contained item is greater
     *    <li><tt>service.FAILURE</tt> - if there's a database failure
     *    <li><tt>service.PERM_DENIED</tt> - if you don't have
     *        sufficient permissions</ul> */
    @Override
    MailItem.PendingDelete getDeletionInfo() throws ServiceException {
        if (!canAccess(ACL.RIGHT_DELETE))
            throw ServiceException.PERM_DENIED("you do not have the required rights on the item");
        return DbMailItem.getLeafNodes(this);
    }

    @Override
    void propagateDeletion(PendingDelete info) throws ServiceException {
        if (info.incomplete) {
            info.cascadeIds = DbMailItem.markDeletionTargets(mMailbox,
                    info.itemIds.getIds(EnumSet.of(Type.MESSAGE, Type.CHAT)), info.modifiedIds);
        } else {
            info.cascadeIds = DbMailItem.markDeletionTargets(this, info.modifiedIds);
        }
        if (info.cascadeIds != null) {
            info.modifiedIds.removeAll(info.cascadeIds);
        }
        super.propagateDeletion(info);
    }

    @Override
    void purgeCache(PendingDelete info, boolean purgeItem) throws ServiceException {
        // when deleting a folder, need to purge conv cache!
        mMailbox.purge(Type.CONVERSATION);
        // fault modified conversations back in, thereby marking them dirty
        mMailbox.getItemById(ArrayUtil.toIntArray(info.modifiedIds), Type.CONVERSATION);
        // remove this folder from the cache if needed
        super.purgeCache(info, purgeItem);
    }

    /** @return the number of messages that were purged */
    static int purgeMessages(Mailbox mbox, Folder folder, long beforeDate, Boolean unread,
                             boolean useChangeDate, boolean deleteEmptySubfolders, Integer maxItems)
    throws ServiceException {
        if (beforeDate <= 0 || beforeDate >= mbox.getOperationTimestampMillis()) {
            return 0;
        }

        // get the full list of things that are being removed
        boolean allFolders = folder == null;
        List<Folder> folders = allFolders ? null : folder.getSubfolderHierarchy();
        // If a folder is moved to trash the change_time does not get updated on messages
        // within the folder. So, check the folder change_time first for the sub-folders.
        if (folders != null && useChangeDate) {
            Iterator<Folder> iter = folders.iterator();
            // skip the first folder which is top-level folder (e.g> Trash)
            if (iter.hasNext()) {
                iter.next();
            }
            // do not purge the sub-folder if it has been modified after the beforeDate
            while (iter.hasNext()) {
                Folder f = iter.next();
                if  (f.getChangeDate() >= beforeDate) {
                    ZimbraLog.purge.info("Skipping the recently modified/moved folder %s", f.getPath());
                    iter.remove();
                }
            }
        }
        PendingDelete info = DbMailItem.getLeafNodes(mbox, folders, (int) (beforeDate / 1000), allFolders, unread, useChangeDate, maxItems);
        delete(mbox, info, null, false, false);

        if (deleteEmptySubfolders) {
            // Iterate folder list in order of decreasing depth.
            for (int i = folders.size() - 1; i >= 1; i--) {
                Folder f = folders.get(i);
                long date = useChangeDate ? f.getChangeDate() : f.getDate();
                if (f.getItemCount() <= 0 && date < beforeDate) {
                    f.delete(false);
                }
            }
        }

        List<Integer> ids = info.itemIds.getIds(Type.MESSAGE);
        return (ids == null ? 0 : ids.size());
    }

    /** To be used for special situation such as migration. */
    void migrateDefaultView(MailItem.Type view) throws ServiceException {
        if (!canAccess(ACL.RIGHT_WRITE)) {
            throw ServiceException.PERM_DENIED("you do not have the required rights on the folder");
        }
        if (view == defaultView) {
            return;
        }
        markItemModified(Change.VIEW);
        defaultView = view;
        saveMetadata();
    }

    @Override
    void decodeMetadata(Metadata meta) throws ServiceException {
        super.decodeMetadata(meta);

        // avoid a painful data migration...
        Type view;
        switch (mId) {
            case Mailbox.ID_FOLDER_INBOX:
            case Mailbox.ID_FOLDER_SPAM:
            case Mailbox.ID_FOLDER_SENT:
            case Mailbox.ID_FOLDER_DRAFTS:
                view = Type.MESSAGE;
                break;
            case Mailbox.ID_FOLDER_CALENDAR:
                view = Type.APPOINTMENT;
                break;
            case Mailbox.ID_FOLDER_TASKS:
                view = Type.TASK;
                break;
            case Mailbox.ID_FOLDER_AUTO_CONTACTS:
            case Mailbox.ID_FOLDER_CONTACTS:
                view = Type.CONTACT;
                break;
            case Mailbox.ID_FOLDER_IM_LOGS:
                view = Type.MESSAGE;
                break;
            default:
                view = Type.UNKNOWN;
                break;
        }
        byte bview = (byte) meta.getLong(Metadata.FN_VIEW, -1);
        defaultView = bview >= 0 ? Type.of(bview) : view;

        FolderState fields = getState();
        attributes  = (byte) meta.getLong(Metadata.FN_ATTRS, 0);
        fields.setTotalSize(meta.getLong(Metadata.FN_TOTAL_SIZE, 0L), AccessMode.LOCAL_ONLY);
        fields.setImapUIDNEXT((int) meta.getLong(Metadata.FN_UIDNEXT, 0), AccessMode.LOCAL_ONLY);
        fields.setImapMODSEQ((int) meta.getLong(Metadata.FN_MODSEQ, 0), AccessMode.LOCAL_ONLY);
        fields.setImapRECENT((int) meta.getLong(Metadata.FN_RECENT, -1), AccessMode.LOCAL_ONLY);
        fields.setImapRECENTCutoff((int) meta.getLong(Metadata.FN_RECENT_CUTOFF, 0), AccessMode.LOCAL_ONLY);
        fields.setDeletedCount((int) meta.getLong(Metadata.FN_DELETED, 0), AccessMode.LOCAL_ONLY);
        fields.setDeletedUnreadCount((int) meta.getLong(Metadata.FN_DELETED_UNREAD, 0), AccessMode.LOCAL_ONLY);

        if (meta.containsKey(Metadata.FN_URL) || meta.containsKey(Metadata.FN_SYNC_DATE)) {
            syncData = new SyncData(meta.get(Metadata.FN_URL, null), meta.get(Metadata.FN_SYNC_GUID, null),
                    meta.getLong(Metadata.FN_SYNC_DATE, 0));
        }

        Metadata rp = meta.getMap(Metadata.FN_RETENTION_POLICY, true);
        if (rp != null) {
            fields.setRetentionPolicy(RetentionPolicyManager.retentionPolicyFromMetadata(rp, true), AccessMode.LOCAL_ONLY);
        } else {
            fields.setRetentionPolicy(new RetentionPolicy(), AccessMode.LOCAL_ONLY);
        }

        activeSyncDisabled = meta.getBool(Metadata.FN_DISABLE_ACTIVESYNC, false);

        webOfflineSyncDays = meta.getInt(Metadata.FN_WEB_OFFLINE_SYNC_DAYS, -1);
    }

    @Override
    Metadata encodeMetadata(Metadata meta) {
        FolderState fields = getState();
        Metadata m = encodeMetadata(meta, fields.getColor(), fields.getMetadataVersion(), fields.getVersion(), mExtendedData, attributes, defaultView, fields.getRights(), syncData,
                fields.getImapUIDNEXT(), fields.getTotalSize(), fields.getImapMODSEQ(), fields.getImapRECENT(), fields.getImapRECENTCutoff(), fields.getDeletedCount(),
                fields.getDeletedUnreadCount(), fields.getRetentionPolicy(), activeSyncDisabled, webOfflineSyncDays);
        return m;
    }

    private static String encodeMetadata(Color color, int metaVersion, int version, CustomMetadata custom, byte attributes, Type view,
            ACL rights, SyncData fsd, int uidnext, long totalSize, int modseq, int imapRecent, int imapRecentCutoff,
            int deleted, int deletedUnread, RetentionPolicy rp, boolean disableActiveSync, int webOfflineSyncdDays) {
        CustomMetadataList extended = (custom == null ? null : custom.asList());
        return encodeMetadata(new Metadata(), color, metaVersion, version, extended, attributes, view, rights, fsd, uidnext,
                              totalSize, modseq, imapRecent, imapRecentCutoff, deleted, deletedUnread, rp,
                              disableActiveSync, webOfflineSyncdDays).toString();
    }

    static Metadata encodeMetadata(Metadata meta, Color color, int metaVersion, int version, CustomMetadataList extended,
            byte attributes, Type view, ACL rights, SyncData fsd, int uidnext, long totalSize, int modseq,
            int imapRecent, int imapRecentCutoff, int deleted, int deletedUnread, RetentionPolicy rp,
            boolean disableActiveSync, int webOfflineSyncDays) {
        if (view != null && view != Type.UNKNOWN) {
            meta.put(Metadata.FN_VIEW, view.toByte());
        }
        if (attributes != 0) {
            meta.put(Metadata.FN_ATTRS, attributes);
        }
        if (totalSize > 0) {
            meta.put(Metadata.FN_TOTAL_SIZE, totalSize);
        }
        if (uidnext > 0) {
            meta.put(Metadata.FN_UIDNEXT, uidnext);
        }
        if (modseq > 0) {
            meta.put(Metadata.FN_MODSEQ, modseq);
        }
        if (imapRecent > 0) {
            meta.put(Metadata.FN_RECENT, imapRecent);
        }
        if (imapRecentCutoff > 0) {
            meta.put(Metadata.FN_RECENT_CUTOFF, imapRecentCutoff);
        }
        if (fsd != null && fsd.url != null && !fsd.url.isEmpty()) {
            meta.put(Metadata.FN_URL, fsd.url);
            meta.put(Metadata.FN_SYNC_GUID, fsd.lastGuid);
        }
        if (fsd != null && fsd.lastDate > 0) {
            meta.put(Metadata.FN_SYNC_DATE, fsd.lastDate);
        }
        if (deleted > 0) {
            meta.put(Metadata.FN_DELETED, deleted);
        }
        if (deletedUnread > 0) {
            meta.put(Metadata.FN_DELETED_UNREAD, deletedUnread);
        }
        if (rp != null && rp.isSet()) {
            meta.put(Metadata.FN_RETENTION_POLICY, RetentionPolicyManager.toMetadata(rp, true));
        }
        meta.put(Metadata.FN_DISABLE_ACTIVESYNC, disableActiveSync);
        if (webOfflineSyncDays >= 0) {
            meta.put(Metadata.FN_WEB_OFFLINE_SYNC_DAYS, webOfflineSyncDays);
        }

        return MailItem.encodeMetadata(meta, color, rights, metaVersion, version, extended);
    }

    protected static final String CN_NAME         = "n";
    protected static final String CN_ATTRIBUTES   = "attributes";
    private static final String CN_DELETED        = "deleted";
    private static final String CN_DELETED_UNREAD = "del_unread";

    @Override
    public String toString() {
        MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(this);
        FolderState state = getState();
        helper.add(CN_NAME, state.getName());
        appendCommonMembers(helper);
        Folder parent = getParentFolder();
        if (parent != null) {
            helper.add("parent", parent.getId());
        }
        helper.add(CN_DELETED, state.getDeletedCount());
        helper.add(CN_DELETED_UNREAD, state.getDeletedUnreadCount());
        helper.add(CN_ATTRIBUTES, attributes);
        return helper.toString();
    }

    @Override
    protected void checkItemCreationAllowed() throws ServiceException {
        // only system folders are allowed in external account mailbox
        if (isMutable()) {
            super.checkItemCreationAllowed();
        }
    }

    @Override
    protected long getMaxAllowedExternalShareLifetime(Account account) {
        return AccountUtil.getMaxExternalShareLifetime(account, getDefaultView());
    }

    @Override
    protected long getMaxAllowedInternalShareLifetime(Account account) {
        return AccountUtil.getMaxInternalShareLifetime(account, getDefaultView());
    }

    /**
     * Returns true if this folder is a shared folder.
     * @return
     */
    boolean isShare() {
        return isTagged(Flag.FlagInfo.NO_INHERIT);
    }

    /**
     * Returns true if there is a shared folder in this folder's subtree, not including this
     * folder itself.
     * @throws ServiceException
     */
    boolean containsShare() throws ServiceException {
        Map<String, Integer> subfolders = getSubfolders();
        if (subfolders != null) {
            for (Integer subfolderId : subfolders.values()) {
                Folder sub = mMailbox.getFolderById(subfolderId);
                if (sub.isShare() || sub.containsShare()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public ItemIdentifier getFolderItemIdentifier() {
        return ItemIdentifier.fromAccountIdAndItemId(mMailbox.getAccountId(), getId());
    }

    @Override
    public String getFolderIdAsString() {
        return Integer.toString(getId());
    }

    @Override
    public int getFolderIdInOwnerMailbox() {
        return getId();
    }

    @Override
    public boolean isInboxFolder() {
        return (mId == Mailbox.ID_FOLDER_INBOX);
    }

    @Override
    public boolean isSearchFolder() {
        return (this instanceof SearchFolder);
    }

    @Override
    public boolean isContactsFolder() {
        return (MailItem.Type.CONTACT == this.getDefaultView());
    }

    @Override
    public boolean isChatsFolder() {
        return (MailItem.Type.CHAT == this.getDefaultView());
    }

    @Override
    public boolean isSyncFolder() {
        return isTagged(Flag.FlagInfo.SYNCFOLDER);
    }

    @Override
    public boolean isIMAPSubscribed() {
        return isTagged(Flag.FlagInfo.SUBSCRIBED);
    }

    /**
     * Returns the IMAP UID Validity Value for the {@link Folder}.
     * This is the folder's <tt>MOD_CONTENT</tt> change sequence number.
     * @see Folder#getSavedSequence()
     **/
    @Override
    public int getUIDValidity() {
        return Math.max(getSavedSequence(), 1);
    }

    @Override
    public int getImapMessageCount() {
        return (int) getItemCount();
    }

    /** @return number of unread items in folder, including IMAP \Deleted items */
    @Override
    public int getImapUnreadCount() {
        return getUnreadCount();
    }

    /** Calendars, briefcases, etc. are not surfaced in IMAP. */
    @Override
    public boolean isVisibleInImap(boolean displayMailFoldersOnly) {
        switch (getDefaultView()) {
        case APPOINTMENT:
        case TASK:
        case WIKI:
        case DOCUMENT:
            return false;
        case CONTACT:
        case CHAT:
            return !displayMailFoldersOnly;
        default:
            return true;
        }
    }

    @Override
    public MailboxStore getMailboxStore() throws ServiceException {
        return getMailbox();
    }

    @Override
    public void attach(SharedStateAccessor accessor) {
        getState().setSharedStateAccessor(accessor);
    }


    @Override
    public void detatch() {
        getState().clearSharedStateAccessor();
    }

    @Override
    protected MailItemState initFieldCache(UnderlyingData data) {
        return new FolderState(data);
    }

    protected FolderState getState() {
        return (FolderState) state;
    }

    @Override
    public boolean isAttached() {
        return getState().hasSharedStateAccessor();
    }

    @Override
    public void sync() {
        getState().syncWithSharedState(this);
    }

    /**
     * Helper class used to look up a folder's parent or subfolders.
     * How this works depends on the FolderCache implementations
     * @author iraykin
     *
     */
    private abstract class ParentLocator {

        public abstract Folder getParent();

        protected abstract void setParent(Folder parent);

        public abstract void unsetParent();

        public void setupParent() {
            if (getParent() != null) {
                return;
            }
            try {
                ZimbraLog.mailbox.debug("setupParent() for folder id=%s name=%s. parent was null, re-getting.",
                        getId(), getName());
                setParent(getFolder());
            } catch (ServiceException e) {
                // Will end up throwing an NPE if call sites assume parent is non-null.
                // (close to release so don't want to change behavior)
                ZimbraLog.mailbox.warn("setupParent() Problem re-getting parent for folder id=%s name=%s %s",
                        getId(), getName(), e.getMessage());
            }
        }
    }

    private class DirectParentLocator extends ParentLocator {

        private Folder parent;

        @Override
        public Folder getParent() {
            return parent;
        }

        @Override
        public void unsetParent() {
            parent = null;

        }

        @Override
        protected void setParent(Folder parent) {
            this.parent = parent;

        }
    }

    private class ReferencedParentLocator extends ParentLocator {

        @Override
        public Folder getParent() {
            Integer parentFolderId = getState().getParentFolder();
            try {
                return parentFolderId == null ? null : mMailbox.getFolderById(parentFolderId);
            } catch (ServiceException e) {
                ZimbraLog.mailbox.error("unable to get parent folder with id=%d for folder %s", parentFolderId, getName());
                return null;
            }
        }

        @Override
        public void unsetParent() {
            getState().unsetParentFolderId();
        }

        @Override
        protected void setParent(Folder parent) {
            getState().setParentFolder(parent);

        }
    }
}
