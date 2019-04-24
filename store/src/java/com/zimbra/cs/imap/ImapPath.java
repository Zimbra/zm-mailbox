/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.imap;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mailbox.ExistingParentFolderStoreAndUnmatchedPart;
import com.zimbra.common.mailbox.FolderStore;
import com.zimbra.common.mailbox.ItemIdentifier;
import com.zimbra.common.mailbox.MailboxStore;
import com.zimbra.common.mailbox.MountpointStore;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.SystemUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.util.AccountUtil;

public class ImapPath implements Comparable<ImapPath> {
    enum Scope { UNPARSED, NAME, CONTENT, REFERENCE };

    protected static Charset FOLDER_ENCODING_CHARSET;
    static {
        try {
            FOLDER_ENCODING_CHARSET = Charset.forName("imap-utf-7");
        } catch (Exception e) {
            ZimbraLog.imap.error(
                "could not load imap-utf-7 charset (perhaps zimbra-charset.jar is not in the jetty endorsed directory)",
                e);
            FOLDER_ENCODING_CHARSET = Charset.forName("utf-8");
        }
    }

    protected static final String NAMESPACE_PREFIX = "/home/";

    private final ImapCredentials mCredentials;
    private String mOwner;
    private String mPath;
    private ItemId mItemId;
    private Scope mScope = Scope.CONTENT;
    private transient ImapMailboxStore imapMboxStore;
    private transient ImapFolderStore imapFolderStore;  // Note - demand initialized
    private transient FolderStore folder;
    private transient ImapPath mReferent;

    /** Takes a user-supplied IMAP mailbox path and converts it to a Zimbra
     *  folder pathname.  Applies all special, hack-specific folder mappings.
     *  Does <b>not</b> do IMAP-UTF-7 decoding; this is assumed to have been
     *  already done by the appropriate method in {@link ImapRequest}.
     *
     * @param imapPath   The client-provided logical IMAP pathname.
     * @param creds      The authenticated user's login credentials.
     * @see #exportPath(String, ImapCredentials) */
    public ImapPath(String imapPath, ImapCredentials creds) {
        this(imapPath, creds, Scope.CONTENT);
    }

    ImapPath(String imapPath, ImapCredentials creds, Scope scope) {
        mCredentials = creds;
        mPath = imapPath;
        mScope = scope;

        Pair<String,String> homeOwnerAndPath = getHomeOwnerAndPath(imapPath,
                false /* allowWildcardOwner - to avoid trying to instantiate accounts with * and % in name */);
        if (homeOwnerAndPath != null) {
            mOwner = homeOwnerAndPath.getFirst();
            mPath = homeOwnerAndPath.getSecond();
        }
        while (mPath.startsWith("/")) {
            mPath = mPath.substring(1);
        }
        while (mPath.endsWith("/")) {
            mPath = mPath.substring(0, mPath.length() - 1);
        }

        // Windows Mobile 5 hack: server must map "Sent Items" to "Sent"
        String lcname = mPath.toLowerCase();
        if (creds != null && creds.isHackEnabled(ImapCredentials.EnabledHack.WM5) &&
                lcname.startsWith("sent items") && (lcname.length() == 10 || lcname.charAt(10) == '/')) {
            mPath = "Sent" + mPath.substring(10);
        }
    }

    ImapPath(String owner, String zimbraPath, ImapCredentials creds) {
        mCredentials = creds;
        mOwner = owner == null ? null : owner.toLowerCase();
        mPath = zimbraPath.startsWith("/") ? zimbraPath.substring(1) : zimbraPath;
    }

    protected static ImapPath get(String owner, String zimbraPath, ImapCredentials creds,
            ImapMailboxStore imapMailboxStore) throws ServiceException {
        ImapPath ipath = new ImapPath (owner, zimbraPath, creds);
        ipath.imapMboxStore = imapMailboxStore;
        return ipath;
    }

    ImapPath(ImapPath other) {
        mCredentials = other.mCredentials;
        mOwner = other.mOwner;
        mPath = other.mPath;
        imapMboxStore = other.imapMboxStore;
        folder = other.folder;
        mItemId = other.mItemId;
        this.imapFolderStore = other.imapFolderStore;
    }

    ImapPath(String owner, FolderStore folderStore, ImapCredentials creds) throws ServiceException {
        this(owner, folderStore.getPath(), creds);
        imapMboxStore = ImapMailboxStore.get(folderStore.getMailboxStore(), accountIdFromCredentials());
        this.folder = folderStore;
        mItemId = new ItemId(folderStore.getFolderIdAsString(), accountIdFromCredentials());
    }

    ImapPath(String owner, FolderStore folderStore, ImapPath mountpoint) throws ServiceException {
        this(mountpoint);
        (mReferent = new ImapPath(owner, folderStore, mCredentials)).mScope = Scope.REFERENCE;
        int start = mountpoint.getReferent().mPath.length() + 1;
        mPath = mountpoint.mPath + "/" + mReferent.mPath.substring(start == 1 ? 0 : start);
    }

    public boolean isEquivalent(ImapPath other) {
        if (!mPath.equalsIgnoreCase(other.mPath)) {
            return false;
        } else if (mOwner == other.mOwner || (mOwner != null && mOwner.equalsIgnoreCase(other.mOwner))) {
            return true;
        }
        try {
            Account acct = getOwnerAccount();
            Account otheracct = other.getOwnerAccount();
            return (acct == null || otheracct == null ? false : acct.getId().equalsIgnoreCase(otheracct.getId()));
        } catch (ServiceException e) {
            return false;
        }
    }

    @Override public boolean equals(Object obj) {
        if (!(obj instanceof ImapPath)) {
            return super.equals(obj);
        }
        return asImapPath().equalsIgnoreCase(((ImapPath) obj).asImapPath());
    }

    @Override public int compareTo(ImapPath o) {
        return asImapPath().compareToIgnoreCase(o.asImapPath());
    }

    @Override public int hashCode() {
        return (mOwner == null ? 0 : mOwner.toUpperCase().hashCode()) ^ mPath.toUpperCase().hashCode() ^ (mCredentials == null ? 0 : mCredentials.hashCode());
    }

    @VisibleForTesting
    public ImapPath canonicalize() throws ServiceException {
        getFolder();

        String path = folder.getPath();

        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        int excess = mPath.length() - path.length();
        if (mReferent == this || excess == 0) {
            mPath = path;
        } else {
            mPath = path + "/" + mReferent.canonicalize().mPath.substring(mReferent.mPath.length() - excess + 1);
        }
        return this;
    }

    protected String getOwner() {
        return mOwner;
    }

    protected ImapCredentials getCredentials() {
        return mCredentials;
    }

    protected boolean belongsTo(ImapCredentials creds) throws ServiceException {
        return creds != null && belongsTo(creds.getAccountId());
    }

    protected boolean belongsTo(String accountId) throws ServiceException {
        String ownerId = getOwnerAccountId();
        return ownerId != null && ownerId.equalsIgnoreCase(accountId);
    }

    protected String getOwnerAccountId() throws ServiceException {
        if (useReferent()) {
            return getReferent().getOwnerAccountId();
        }

        if (mOwner == null && mCredentials != null) {
            return mCredentials.getAccountId();
        } else if (mOwner == null) {
            return null;
        }
        Account acct = getOwnerAccount();
        return acct == null ? null : acct.getId();
    }

    protected Account getOwnerAccount() throws ServiceException {
        if (useReferent()) {
            return getReferent().getOwnerAccount();
        } else if (mOwner != null) {
            return Provisioning.getInstance().get(AccountBy.name, mOwner);
        } else if (mCredentials != null) {
            return Provisioning.getInstance().get(AccountBy.id, mCredentials.getAccountId());
        } else {
            return null;
        }
    }

    protected boolean onLocalServer() throws ServiceException {
        if(LC.imap_always_use_remote_store.booleanValue() ||
                !ImapDaemon.isRunningImapInsideMailboxd()) {
            return false;
        }
        Account acct = getOwnerAccount();
        return acct != null && Provisioning.onLocalServer(acct);
    }

    protected MailboxStore getOwnerMailbox() throws ServiceException {
        ImapMailboxStore store = getOwnerImapMailboxStore();
        return (null == store) ? null : store.getMailboxStore();
    }

    protected ImapMailboxStore getOwnerImapMailboxStore() throws ServiceException {
        return getOwnerImapMailboxStore(!onLocalServer());
    }

    /**
     * sets up `mboxStore` and returns its value.
     * @param forceRemote - return either a RemoteImapMailboxStore or null if true
     */
    protected ImapMailboxStore getOwnerImapMailboxStore(boolean forceRemote) throws ServiceException {
        if (useReferent()) {
            return mReferent.getOwnerImapMailboxStore(forceRemote);
        }
        Account target = getOwnerAccount();
        if (forceRemote || (Provisioning.canUseLocalIMAP(target) && !Provisioning.onLocalServer(target))) {
            imapMboxStore = getRemoteImapMailboxStoreForOwner();
        }
        if (imapMboxStore == null) {
            if (target == null) {
                imapMboxStore = null;
            } else if (Provisioning.onLocalServer(target)) {
                Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(target);
                imapMboxStore = ImapMailboxStore.get(mbox);
            } else if (mCredentials == null) {
                imapMboxStore = null;
            } else {
                imapMboxStore = getRemoteImapMailboxStoreForOwner();
            }
        }
        return imapMboxStore;
    }

    private ImapMailboxStore getRemoteImapMailboxStoreForOwner() throws ServiceException {
        ZMailbox zmbox = getOwnerZMailbox();
        return (null == zmbox) ? null : ImapMailboxStore.get(zmbox, this.getOwnerAccountId());
    }

    private ZMailbox getZMailboxForAccount(Account target) throws ServiceException {
        Account acct = mCredentials == null ? null :
            Provisioning.getInstance().get(AccountBy.id, mCredentials.getAccountId());
        if (acct == null) {
            throw AccountServiceException.NO_SUCH_ACCOUNT(mCredentials.getUsername());
        }
        if (acct.getId().equals(target.getId())) {
            return (ZMailbox) mCredentials.getMailbox();
        }
        try {
            ZMailbox.Options options = new ZMailbox.Options(AuthProvider.getAuthToken(acct).getEncoded(),
                    AccountUtil.getSoapUri(target));
            /* getting by ID avoids failed GetInfo SOAP requests trying to determine ID before auth setup. */
            options.setTargetAccountBy(AccountBy.id);
            options.setTargetAccount(target.getId());
            options.setUserAgent("zclient-imap-onBehalfOf", SystemUtil.getProductVersion());
            options.setNoSession(true);
            options.setAlwaysRefreshFolders(true);
            ZMailbox zmbx = ZMailbox.getMailbox(options);
            zmbx.setAccountId(target.getId()); /* need this when logging in using another user's auth */
            zmbx.setName(target.getName()); /* need this when logging in using another user's auth */
            zmbx.setAuthName(acct.getName());
            return zmbx;
        } catch (AuthTokenException ate) {
            throw ServiceException.FAILURE("error generating auth token", ate);
        }
    }

    private ZMailbox getOwnerZMailbox() throws ServiceException {
        if (useReferent()) {
            return getReferent().getOwnerZMailbox();
        }

        if (imapMboxStore instanceof RemoteImapMailboxStore) {
            return ((RemoteImapMailboxStore) imapMboxStore).getZMailbox();
        } else if (mCredentials == null) {
            return null;
        }

        Account target = getOwnerAccount();
        if (target == null) {
            throw AccountServiceException.NO_SUCH_ACCOUNT(getOwner());
        }
        return getZMailboxForAccount(target);
    }

    private String accountIdFromCredentials() {
        return (mCredentials == null) ? null : mCredentials.getAccountId();
    }

    private OperationContext getContext() throws ServiceException {
        return (mCredentials == null ? null : mCredentials.getContext());
    }

    @VisibleForTesting
    public FolderStore getFolder() throws ServiceException {
        if (useReferent()) {
            return getReferent().getFolder();
        }

        if (mCredentials != null && mCredentials.isFolderHidden(this)) {
            throw MailServiceException.NO_SUCH_FOLDER(asImapPath());
        }

        if (folder == null) {
            MailboxStore mboxStore = getOwnerMailbox();
            if (null == mboxStore) {
                throw AccountServiceException.NO_SUCH_ACCOUNT(getOwner());
            }
            String zimbraPath = asZimbraPath();
            folder = mboxStore.getFolderByPath(getContext(), zimbraPath);
            if (folder == null) {
                // If the folder is not found, it's possible that it was created
                // by a non-imap client. Issue a noOp request to get latest changes and try again.
                mboxStore.noOp();
                folder = mboxStore.getFolderByPath(getContext(), zimbraPath);
            }
            if (folder == null) {
                throw MailServiceException.NO_SUCH_FOLDER(asImapPath());
            }
            mItemId = new ItemId(folder.getFolderIdAsString(), accountIdFromCredentials());
        }
        return folder;
    }

    protected ImapFolderStore getImapFolderStore() throws ServiceException {
        if (imapFolderStore != null) {
            return imapFolderStore;
        }
        // for LIST *, LIST % and LIST %/% we do not need an instance of ImapFolderStore
        if (mPath.indexOf("*") >= 0 || mPath.indexOf("%") >= 0) {
            return null;
        }
        if (useReferent()) {
            synchronized(this) {
                imapFolderStore = getReferent().getImapFolderStore();
            }
            return imapFolderStore;
        }
        synchronized(this) {
            imapFolderStore = ImapFolderStore.get(getFolder());
        }
        return imapFolderStore;
    }

    protected boolean useReferent() throws ServiceException {
        if (getReferent() == this) {
            return false;
        } else if (mScope == Scope.CONTENT) {
            return true;
        }

        // if we're here, we should be at NAME scope -- return whether the original path pointed at a mountpoint
        assert(mScope == Scope.NAME);
        assert(folder != null);
        assert(mReferent != null);

        if (folder instanceof MountpointStore) {
            ItemId iidBase;
            iidBase = new ItemId(((MountpointStore)folder).getTargetItemIdentifier());
            return !iidBase.equals(mReferent.mItemId);
        } else {
            return false;
        }
    }

    /**
     * @return If the folder is a mountpoint (i.e. an accepted share), may return an ImapPath representing
     *         that, otherwise, the value is this.
     */
    @VisibleForTesting
    public ImapPath getReferent() throws ServiceException {
        if (mReferent != null) {
            return mReferent;
        }

        // while calculating, use the base
        mReferent = this;

        // only follow the authenticated user's own mountpoints
        if (mScope == Scope.REFERENCE || mScope == Scope.UNPARSED || !belongsTo(mCredentials)) {
            return mReferent;
        }

        ImapMailboxStore ownerImapMailboxStore = getOwnerImapMailboxStore();
        if (null == ownerImapMailboxStore) {
            return mReferent;
        }

        ItemId iidRemote;
        String subpathRemote = null;

        if (folder == null) {
            try {
                ExistingParentFolderStoreAndUnmatchedPart info =
                        ownerImapMailboxStore.getMailboxStore().getParentFolderStoreAndUnmatchedPart(
                                getContext(), asZimbraPath());
                subpathRemote = info.unmatchedPart;
                if (info.parentFolderStore instanceof MountpointStore || Strings.isNullOrEmpty(subpathRemote)) {
                    folder = info.parentFolderStore;
                    mItemId = new ItemId(ItemIdentifier.fromOwnerAndFolder(accountIdFromCredentials(), folder));
                }
                if (!(info.parentFolderStore instanceof MountpointStore)) {
                    return mReferent;
                }
            } catch (ServiceException e) {
                return mReferent;
            }
        }

        if (!(folder instanceof MountpointStore)) {
            return mReferent;
        }
        // somewhere along the specified path is a visible mountpoint owned by the user
        iidRemote = new ItemId(((MountpointStore) folder).getTargetItemIdentifier());

        // don't allow mountpoints that point at the same mailbox (as it can cause infinite loops)
        if (belongsTo(iidRemote.getAccountId())) {
            return mReferent;
        }

        Account target = Provisioning.getInstance().get(AccountBy.id, iidRemote.getAccountId());
        if (target == null) {
            return mReferent;
        }

        ImapMailboxStore imapMailboxStore = setupMailboxStoreForTarget(target, iidRemote);
        if (null == imapMailboxStore) {
            return mReferent;
        }
        FolderStore fldr = imapMailboxStore.getMailboxStore().getFolderById(
                getContext(), Integer.toString(iidRemote.getId()));
        if (fldr == null) {
            return mReferent;
        }
        String owner = getOwner(target);
        if (Strings.isNullOrEmpty(subpathRemote)) {
            mReferent = new ImapPath(owner, fldr, mCredentials);
        } else {
            mReferent = ImapPath.get(owner, fldr.getPath() +
                    (fldr.getPath().equals("/") ? "" : "/") + subpathRemote, mCredentials, imapMailboxStore);
        }

        if (mReferent != this) {
            mReferent.mScope = Scope.REFERENCE;
        }
        return mReferent;
    }

    private String getOwner(Account target) {
        return mCredentials != null && mCredentials.getAccountId().equalsIgnoreCase(target.getId()) ? null
                : target.getName();
    }

    private ImapMailboxStore setupMailboxStoreForTarget(Account target, ItemId iidRemote)
            throws ServiceException {
        ImapMailboxStore imapMailboxStore = null;
        // if both target and owner are on local server and using local imap
        if (Provisioning.onLocalServer(target) && onLocalServer()) {
            try {
                MailboxStore mbox = MailboxManager.getInstance().getMailboxByAccount(target);
                imapMailboxStore = ImapMailboxStore.get(mbox, target.getId());
            } catch (ServiceException se) {
                ZimbraLog.imap.debug("Unexpected exception", se);
            }
        } else {
            Account acct = mCredentials == null ? null :
                Provisioning.getInstance().get(AccountBy.id, mCredentials.getAccountId());
            if (acct == null) {
                return null;
            }
            try {
                ZMailbox zmbx = getZMailboxForAccount(target);
                ZFolder zfolder = zmbx.getFolderById(iidRemote.toString(mCredentials.getAccountId()));
                if (zfolder == null) {
                    return null;
                }
                imapMailboxStore = ImapMailboxStore.get(zmbx);
            } catch (ServiceException e) {
                ZimbraLog.imap.debug("Unexpected exception", e);
            }
        }
        return imapMailboxStore;
    }

    protected short getFolderRights() throws ServiceException {
        if (getFolder() instanceof Folder) {
            Folder fldr = (Folder) getFolder();
            return fldr.getMailbox().getEffectivePermissions(getContext(), fldr.getId(), fldr.getType());
        } else {
            ZFolder zfolder = (ZFolder) getFolder();
            String rights = zfolder.getEffectivePerms();
            return rights == null ? ~0 : ACL.stringToRights(rights);
        }
    }

    protected boolean isCreatable() {
        String path = mPath.toLowerCase();
        return !path.matches("\\s*notebook\\s*(/.*)?") &&
               !path.matches("\\s*contacts\\s*(/.*)?") &&
               !path.matches("\\s*calendar\\s*(/.*)?");
    }

    /** Returns whether the server can return the <tt>READ-WRITE</tt> response
     *  code when the folder referenced by this path is <tt>SELECT</tt>ed. */
    protected boolean isWritable() throws ServiceException {
        // RFC 4314 5.2: "The server SHOULD include a READ-WRITE response code in the tagged OK
        //                response if at least one of the "i", "e", or "shared flag rights" is
        //                granted to the current user."
        return isWritable(ACL.RIGHT_DELETE) || isWritable(ACL.RIGHT_INSERT) || isWritable(ACL.RIGHT_WRITE);
    }

    /** Returns <tt>true</tt> if all of the specified rights have been granted
     *  on the folder referenced by this path to the authenticated user. */
    protected boolean isWritable(short rights) throws ServiceException {
        if (!isSelectable()) {
            return false;
        }
        getImapFolderStore();
        if (imapFolderStore == null) {
            return false;
        }
        FolderStore fstore = imapFolderStore.getFolderStore();
        if (fstore.isSearchFolder() || fstore.isContactsFolder()) {
            return false;
        }
        // note that getFolderRights() operates on the referent folder...
        if (rights == 0) {
            return true;
        }
        return (getFolderRights() & rights) == rights;
    }

    protected boolean isSelectable() throws ServiceException {
        getImapFolderStore();
        if (imapFolderStore == null || !isVisible() || imapFolderStore.isUserRootFolder() || imapFolderStore.isIMAPDeleted()) {
            return false;
        }
        return (mReferent == this ? true : mReferent.isSelectable());
    }

    protected boolean isVisible() throws ServiceException {
        boolean isMailFolders = Provisioning.getInstance().getLocalServer().isImapDisplayMailFoldersOnly();
        /** "folder" CAN be null when this is called - relying on getFolder() to fill in the details later
         * e.g. in the case of ". RENAME nonExistentA nonExistentB" */
        if (folder != null && !(folder.isVisibleInImap(isMailFolders))) {
            return false;
        }
        if (mCredentials != null && mCredentials.isFolderHidden(this)) {
            return false;
        }

        try {
            getFolder();
        } catch (ServiceException e) {
            if (ServiceException.PERM_DENIED.equals(e.getCode())) {
                return false;
            }
            throw e;
        }

        if (folder.isHidden()) {
            return false;
        }
        boolean okPath = isValidImapPath();
        if (!okPath) {
            return false;
        }
        return mReferent == this ? true : mReferent.isVisible();
    }

    /**
     * Mostly checking that the path doesn't clash with any paths we don't want to expose via IMAP.
     * Separated out from isVisible() to aid IMAP LSUB command support.
     */
    protected boolean isValidImapPath() throws ServiceException {
        if (mCredentials != null && mCredentials.isHackEnabled(ImapCredentials.EnabledHack.WM5)) {
            String lcname = mPath.toLowerCase();
            if (lcname.startsWith("sent items") && (lcname.length() == 10 || lcname.charAt(10) == '/'))
                return false;
        }
        try {
            // you cannot access your own mailbox via the /home/username mechanism
            if (mOwner != null && belongsTo(mCredentials)) {
                return false;
            }
            getFolder();
            if (folder !=null) {
                /* the mailbox root folder is not visible (although note that if a whole mailbox has been
                 * shared then the mountpoint used to represent that uses the user root folder as its
                 * target and that SHOULD be allowed  - ZBUG-795) */
                if (mItemId.getId() == Mailbox.ID_FOLDER_USER_ROOT && mScope != Scope.REFERENCE) {
                    return false;
                }
                // hide spam folder unless anti-spam feature is enabled.
                if (asItemId().getId() == Mailbox.ID_FOLDER_SPAM && !getOwnerAccount().isFeatureAntispamEnabled()) {
                    return false;
                }
                boolean isMailFolders = Provisioning.getInstance().getLocalServer().isImapDisplayMailFoldersOnly();
                // calendars, briefcases, etc. are not surfaced in IMAP
                if (!(folder.isVisibleInImap(isMailFolders))) {
                    return false;
                }
                // hide subfolders of trashed mountpoints
                if (mReferent != this && folder.inTrash()) {
                    ItemId targID = new ItemId(((MountpointStore) folder).getTargetItemIdentifier());
                    if (targID.equals(mReferent.asItemId())) {
                        return false;
                    }
                }
            }
        } catch (NoSuchItemException ignore) {
            // 6.3.9.  LSUB Command
            //   The server MUST NOT unilaterally remove an existing mailbox name from the subscription list even if a
            //   mailbox by that name no longer exists.
        } catch (AccountServiceException ase) {
            if (!AccountServiceException.NO_SUCH_ACCOUNT.equals(ase.getCode())) {
                throw ase;
            }
        } catch (ServiceException se) {
            if (ServiceException.PERM_DENIED.equals(se.getCode())) {
                // Path probably OK.  For subscriptions, don't disallow path for possibly temporary permissions issue
                return true;
            }
            throw se;
        }
        return mReferent == this ? true : mReferent.isValidImapPath();
    }

    /**
     * @param imapPath
     * @param allowWildcardOwner
     * @return If imapPath represents a path in the /home namespace, then return the owner and path sub-components
     * otherwise return null
     */
    protected static Pair<String,String> getHomeOwnerAndPath(String imapPath, boolean allowWildcardOwner) {
        String owner = null;
        String path = null;

        if (!imapPath.toLowerCase().startsWith(NAMESPACE_PREFIX)) {
            return null;
        }
        String imapPathNoPrefix = imapPath.substring(NAMESPACE_PREFIX.length());
        if (imapPathNoPrefix.isEmpty() || imapPathNoPrefix.startsWith("/")) {
            return null;
        }
        int slash = imapPathNoPrefix.indexOf('/');
        owner = (slash == -1 ? imapPathNoPrefix : imapPathNoPrefix.substring(0, slash)).toLowerCase();
        if (!allowWildcardOwner && ((owner.indexOf("*") >= 0) || (owner.indexOf("%") >= 0))) {
            return null;
        }
        path = (slash == -1 ? "" : imapPathNoPrefix.substring(slash));
        return new Pair<String,String>(owner, path);
    }

    protected String asZimbraPath() {
        return mPath;
    }

    protected String asResolvedPath() throws ServiceException {
        return getReferent().mPath;
    }

    protected ItemId asItemId() throws ServiceException {
        if (useReferent()) {
            return getReferent().mItemId;
        }

        if (mItemId == null) {
            getFolder();
        }
        return mItemId;
    }

    @Override
    public String toString() {
        return asImapPath();
    }

    protected String asImapPath() {
        String path = mPath;
        String lcpath = path.toLowerCase();
        // make sure that the Inbox is called "INBOX", regardless of how we capitalize it
        if (lcpath.startsWith("inbox") && (lcpath.length() == 5 || lcpath.charAt(5) == '/')) {
            path = "INBOX" + path.substring(5);
        } else if (mCredentials != null && mCredentials.isHackEnabled(ImapCredentials.EnabledHack.WM5) &&
                lcpath.startsWith("sent") && (lcpath.length() == 4 || lcpath.charAt(4) == '/')) {
            path = "Sent Items" + path.substring(4);
        }

        if (mOwner != null && !mOwner.equals("")) {
            path = NAMESPACE_PREFIX + mOwner + ("".equals(path) ? "" : "/") + path;
        }
        return path;
    }

    /** Formats a folder path as an IMAP-UTF-7 quoted-string.  Applies all
     *  special hack-specific path transforms. */
    protected String asUtf7String() {
        return asUtf7String(asImapPath());
    }

    /** Formats a folder path as an IMAP-UTF-7 quoted-string. */
    protected static String asUtf7String(String imapPath) {
        ByteBuffer bb = FOLDER_ENCODING_CHARSET.encode(imapPath);
        byte[] content = new byte[bb.limit() + 2];
        content[0] = '"';
        System.arraycopy(bb.array(), 0, content, 1, content.length - 2);
        content[content.length - 1] = '"';
        return new String(content).replaceAll("\\\\", "\\\\\\\\");
    }

}
