/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.SearchFolder;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZMountpoint;
import com.zimbra.cs.zclient.ZSearchFolder;

public class ImapPath implements Comparable<ImapPath> {
    enum Scope { UNPARSED, NAME, CONTENT, REFERENCE };

    static Charset FOLDER_ENCODING_CHARSET;
        static {
            try {
                FOLDER_ENCODING_CHARSET = Charset.forName("imap-utf-7");
            } catch (Exception e) {
                ZimbraLog.imap.error("could not load imap-utf-7 charset (perhaps zimbra-charset.jar is not in the jetty endorsed directory)", e);
                FOLDER_ENCODING_CHARSET = Charset.forName("utf-8");
            }
        }

    static final String NAMESPACE_PREFIX = "/home/";

    private ImapCredentials mCredentials;
    private String mOwner;
    private String mPath;
    private ItemId mItemId;
    private Scope mScope = Scope.CONTENT;
    private transient Object mMailbox;
    private transient Object mFolder;
    private transient ImapPath mReferent;

    /** Takes a user-supplied IMAP mailbox path and converts it to a Zimbra
     *  folder pathname.  Applies all special, hack-specific folder mappings.
     *  Does <b>not</b> do IMAP-UTF-7 decoding; this is assumed to have been
     *  already done by the appropriate method in {@link ImapRequest}.
     *  
     * @param imapPath   The client-provided logical IMAP pathname.
     * @param creds      The authenticated user's login credentials.
     * @see #exportPath(String, ImapCredentials) */
    ImapPath(String imapPath, ImapCredentials creds) {
        this(imapPath, creds, Scope.CONTENT);
    }

    ImapPath(String imapPath, ImapCredentials creds, Scope scope) {
        mCredentials = creds;
        mPath = imapPath;
        mScope = scope;

        if (imapPath.toLowerCase().startsWith(NAMESPACE_PREFIX)) {
            imapPath = imapPath.substring(NAMESPACE_PREFIX.length());
            if (!imapPath.equals("") && !imapPath.startsWith("/")) {
                int slash = imapPath.indexOf('/');
                mOwner = (slash == -1 ? imapPath : imapPath.substring(0, slash)).toLowerCase();
                mPath = (slash == -1 ? "" : imapPath.substring(slash));
            }
        }

        while (mPath.startsWith("/"))
            mPath = mPath.substring(1);
        while (mPath.endsWith("/"))
            mPath = mPath.substring(0, mPath.length() - 1);

        // Windows Mobile 5 hack: server must map "Sent Items" to "Sent"
        String lcname = mPath.toLowerCase();
        if (creds != null && creds.isHackEnabled(ImapCredentials.EnabledHack.WM5)) {
            if (lcname.startsWith("sent items") && (lcname.length() == 10 || lcname.charAt(10) == '/'))
                mPath = "Sent" + mPath.substring(10);
        }
    }

    ImapPath(String owner, String zimbraPath, ImapCredentials creds) {
        mCredentials = creds;
        mOwner = owner == null ? null : owner.toLowerCase();
        mPath = zimbraPath.startsWith("/") ? zimbraPath.substring(1) : zimbraPath;
    }

    ImapPath(ImapPath other) {
        mCredentials = other.mCredentials;
        mOwner = other.mOwner;
        mPath = other.mPath;
        mMailbox = other.mMailbox;
        mFolder = other.mFolder;
        mItemId = other.mItemId;
    }

    ImapPath(String owner, Folder folder, ImapCredentials creds) {
        this(owner, folder.getPath(), creds);
        mMailbox = folder.getMailbox();
        mFolder = folder;
        mItemId = new ItemId(folder);
    }

    ImapPath(String owner, Folder folder, ImapPath mountpoint) throws ServiceException {
        this(mountpoint);
        (mReferent = new ImapPath(owner, folder, mCredentials)).mScope = Scope.REFERENCE;
        int start = mountpoint.getReferent().mPath.length() + 1;
        mPath = mountpoint.mPath + "/" + mReferent.mPath.substring(start == 1 ? 0 : start);
    }

    ImapPath(String owner, ZFolder zfolder, ImapCredentials creds) throws ServiceException {
        this(owner, zfolder.getPath(), creds);
        mMailbox = zfolder.getMailbox();
        mFolder = zfolder;
        mItemId = new ItemId(zfolder.getId(), creds == null ? null : creds.getAccountId());
    }

    ImapPath(String owner, ZFolder zfolder, ImapPath mountpoint) throws ServiceException {
        this(mountpoint);
        (mReferent = new ImapPath(owner, zfolder, mCredentials)).mScope = Scope.REFERENCE;
        int start = mountpoint.getReferent().mPath.length() + 1;
        mPath = mountpoint.mPath + "/" + mReferent.mPath.substring(start == 1 ? 0 : start);
    }


    public boolean isEquivalent(ImapPath other) {
        if (!mPath.equalsIgnoreCase(other.mPath))
            return false;
        if (mOwner == other.mOwner || (mOwner != null && mOwner.equalsIgnoreCase(other.mOwner)))
            return true;
        try {
            Account acct = getOwnerAccount(), otheracct = other.getOwnerAccount();
            return (acct == null || otheracct == null ? false : acct.getId().equalsIgnoreCase(otheracct.getId()));
        } catch (ServiceException e) {
            return false;
        }
    }

    @Override public boolean equals(Object obj) {
        if (!(obj instanceof ImapPath))
            return super.equals(obj);
        return asImapPath().equalsIgnoreCase(((ImapPath) obj).asImapPath());
    }

    @Override public int compareTo(ImapPath o) {
        return asImapPath().compareToIgnoreCase(o.asImapPath());
    }

    @Override public int hashCode() {
        return (mOwner == null ? 0 : mOwner.toUpperCase().hashCode()) ^ mPath.toUpperCase().hashCode() ^ (mCredentials == null ? 0 : mCredentials.hashCode());
    }

    ImapPath canonicalize() throws ServiceException {
        getFolder();

        String path;
        if (mFolder instanceof Folder)
            path = ((Folder) mFolder).getPath();
        else
            path = ((ZFolder) mFolder).getPath();

        while (path.startsWith("/"))
            path = path.substring(1);
        while (path.endsWith("/"))
            path = path.substring(0, path.length() - 1);

        int excess = mPath.length() - path.length();
        if (mReferent == this || excess == 0)
            mPath = path;
        else
            mPath = path + "/" + mReferent.canonicalize().mPath.substring(mReferent.mPath.length() - excess + 1);
        return this;
    }


    String getOwner() {
        return mOwner;
    }

    ImapCredentials getCredentials() {
        return mCredentials;
    }

    boolean belongsTo(Mailbox mbox) throws ServiceException {
        return belongsTo(mbox.getAccountId());
    }

    boolean belongsTo(ImapCredentials creds) throws ServiceException {
        return belongsTo(creds.getAccountId());
    }

    boolean belongsTo(String accountId) throws ServiceException {
        String ownerId = getOwnerAccountId();
        return ownerId != null && ownerId.equalsIgnoreCase(accountId);
    }

    String getOwnerAccountId() throws ServiceException {
        if (useReferent())
            return getReferent().getOwnerAccountId();

        if (mMailbox instanceof Mailbox)
            return ((Mailbox) mMailbox).getAccountId();
        else if (mOwner == null && mCredentials != null)
            return mCredentials.getAccountId();
        else if (mOwner == null)
            return null;
        Account acct = getOwnerAccount();
        return acct == null ? null : acct.getId();
    }

    Account getOwnerAccount() throws ServiceException {
        if (useReferent())
            return getReferent().getOwnerAccount();
        else if (mMailbox instanceof Mailbox)
            return ((Mailbox) mMailbox).getAccount();
        else if (mOwner != null)
            return Provisioning.getInstance().get(AccountBy.name, mOwner);
        else if (mCredentials != null)
            return Provisioning.getInstance().get(AccountBy.id, mCredentials.getAccountId());
        else
            return null;
    }

    boolean onLocalServer() throws ServiceException {
        Account acct = getOwnerAccount();
        return acct != null && Provisioning.onLocalServer(acct);
    }

    Object getOwnerMailbox() throws ServiceException {
        return getOwnerMailbox(true);
    }

    Object getOwnerMailbox(boolean traverse) throws ServiceException {
        if (useReferent())
            return mReferent.getOwnerMailbox();

        if (mMailbox == null) {
            Account target = getOwnerAccount();
            if (target == null)
                mMailbox = null;
            else if (Provisioning.onLocalServer(target))
                mMailbox = MailboxManager.getInstance().getMailboxByAccount(target);
            else if (mCredentials == null)
                mMailbox = null;
            else
                mMailbox = getOwnerZMailbox();
        }
        return mMailbox;
    }

    ZMailbox getOwnerZMailbox() throws ServiceException {
        if (useReferent())
            return getReferent().getOwnerZMailbox();

        if (mMailbox instanceof ZMailbox)
            return (ZMailbox) mMailbox;
        if (mCredentials == null)
            return null;

        Account target = getOwnerAccount();
        if (target == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(getOwner());
        Account acct = Provisioning.getInstance().get(AccountBy.id, mCredentials.getAccountId());
        if (acct == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(mCredentials.getUsername());

        try {
            ZMailbox.Options options = new ZMailbox.Options(AuthProvider.getAuthToken(acct).getEncoded(), AccountUtil.getSoapUri(target));
            options.setTargetAccount(target.getName());
            options.setNoSession(true);
            return ZMailbox.getMailbox(options);
        } catch (AuthTokenException ate) {
            throw ServiceException.FAILURE("error generating auth token", ate);
        }
    }

    private OperationContext getContext() throws ServiceException {
        return (mCredentials == null ? null : mCredentials.getContext());
    }

    Object getFolder() throws ServiceException {
        if (useReferent())
            return getReferent().getFolder();

        if (mFolder == null) {
            Object mboxobj = getOwnerMailbox();
            if (mboxobj instanceof Mailbox) {
                Folder folder = ((Mailbox) mboxobj).getFolderByPath(getContext(), asZimbraPath());
                mFolder = folder;
                mItemId = new ItemId(folder);
            } else if (mboxobj instanceof ZMailbox) {
                ZFolder zfolder = ((ZMailbox) mboxobj).getFolderByPath(asZimbraPath());
                mFolder = zfolder;
                if (zfolder == null)
                    throw MailServiceException.NO_SUCH_FOLDER(asImapPath());
                mItemId = new ItemId(zfolder.getId(), mCredentials == null ? null : mCredentials.getAccountId());
            } else {
                throw AccountServiceException.NO_SUCH_ACCOUNT(getOwner());
            }
        }
        return mFolder;
    }

    boolean useReferent() throws ServiceException {
        if (getReferent() == this)
            return false;
        if (mScope == Scope.CONTENT)
            return true;

        // if we're here, we should be at NAME scope -- return whether the original path pointed at a mountpoint
        assert(mScope == Scope.NAME);
        assert(mFolder != null);
        assert(mReferent != null);

        ItemId iidBase;
        if (mFolder instanceof Mountpoint)
            iidBase = ((Mountpoint) mFolder).getTarget();
        else if (mFolder instanceof ZMountpoint)
            iidBase = new ItemId(((ZMountpoint) mFolder).getCanonicalRemoteId(), (String) null);
        else
            return false;
        return !iidBase.equals(mReferent.mItemId);
    }

    ImapPath getReferent() throws ServiceException {
        if (mReferent != null)
            return mReferent;

        // while calculating, use the base 
        mReferent = this;

        // only follow the authenticated user's own mountpoints
        if (mScope == Scope.REFERENCE || mScope == Scope.UNPARSED || !belongsTo(mCredentials))
            return mReferent;

        ItemId iidRemote;
        String subpathRemote = null;

        Object mboxobj = getOwnerMailbox();
        if (mboxobj instanceof Mailbox) {
            try {
                if (mFolder == null) {
                    Pair<Folder,String> resolved = ((Mailbox) mboxobj).getFolderByPathLongestMatch(getContext(), Mailbox.ID_FOLDER_USER_ROOT, asZimbraPath());
                    subpathRemote = resolved.getSecond();

                    boolean isMountpoint = resolved.getFirst() instanceof Mountpoint;
                    if (isMountpoint || resolved.getSecond() == null) {
                        mFolder = resolved.getFirst();
                        mItemId = new ItemId(resolved.getFirst());
                    }
                    if (!isMountpoint)
                        return mReferent;
                } else if (!(mFolder instanceof Mountpoint)) {
                    return mReferent;
                }

                // somewhere along the specified path is a visible mountpoint owned by the user
                iidRemote = ((Mountpoint) mFolder).getTarget();
            } catch (ServiceException e) {
                return mReferent;
            }
        } else if (mboxobj instanceof ZMailbox) {
            String accountId = mCredentials == null ? null : mCredentials.getAccountId();
            if (mFolder == null) {
                ZMailbox zmbx = (ZMailbox) mboxobj;
                String path = asZimbraPath();
                try {
                    for (int index = path.length(); index != -1; index = path.lastIndexOf('/', index - 1)) {
                        ZFolder zfolder = zmbx.getFolderByPath(path.substring(0, index));
                        if (zfolder != null) {
                            subpathRemote = path.substring(Math.min(path.length(), index + 1));
    
                            if (zfolder instanceof ZMountpoint || subpathRemote.isEmpty()) {
                                mFolder = zfolder;
                                mItemId = new ItemId(zfolder.getId(), accountId);
                            }
                            break;
                        }
                    }
                } catch (ServiceException e) {}
            }

            if (!(mFolder instanceof ZMountpoint))
                return mReferent;

            // somewhere along the specified path is a visible mountpoint owned by the user
            iidRemote = new ItemId(((ZMountpoint) mFolder).getCanonicalRemoteId(), accountId);
        } else {
            return mReferent;
        }

        // don't allow mountpoints that point at the same mailbox (as it can cause infinite loops)
        if (belongsTo(iidRemote.getAccountId()))
            return mReferent;

        Account target = Provisioning.getInstance().get(AccountBy.id, iidRemote.getAccountId());
        if (target == null)
            return mReferent;

        String owner = mCredentials != null && mCredentials.getAccountId().equalsIgnoreCase(target.getId()) ? null : target.getName();
        if (Provisioning.onLocalServer(target)) {
            try {
                Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(target);
                Folder folder = mbox.getFolderById(getContext(), iidRemote.getId());
                if (subpathRemote == null)
                    mReferent = new ImapPath(owner, folder, mCredentials);
                else
                    (mReferent = new ImapPath(owner, folder.getPath() + (folder.getPath().equals("/") ? "" : "/") + subpathRemote, mCredentials)).mMailbox = mbox;
            } catch (ServiceException e) {
            }
        } else {
            Account acct = mCredentials == null ? null : Provisioning.getInstance().get(AccountBy.id, mCredentials.getAccountId());
            if (acct == null)
                return mReferent;
            try {
                ZMailbox.Options options = new ZMailbox.Options(AuthProvider.getAuthToken(acct).getEncoded(), AccountUtil.getSoapUri(target));
                options.setTargetAccount(target.getName());
                options.setNoSession(true);
                ZMailbox zmbx = ZMailbox.getMailbox(options);
                ZFolder zfolder = zmbx.getFolderById(iidRemote.toString(mCredentials.getAccountId()));
                if (zfolder == null)
                    return mReferent;
                if (subpathRemote == null)
                    mReferent = new ImapPath(owner, zfolder, mCredentials);
                else
                    (mReferent = new ImapPath(owner, zfolder.getPath() + (zfolder.getPath().equals("/") ? "" : "/") + subpathRemote, mCredentials)).mMailbox = zmbx;
            } catch (AuthTokenException ate) {
                throw ServiceException.FAILURE("error generating auth token", ate);
            } catch (ServiceException e) {
            }
        }

        if (mReferent != this)
            mReferent.mScope = Scope.REFERENCE;
        return mReferent;
    }

    short getFolderRights() throws ServiceException {
        if (getFolder() instanceof Folder) {
            Folder folder = (Folder) getFolder();
            return folder.getMailbox().getEffectivePermissions(getContext(), folder.getId(), folder.getType());
        } else {
            ZFolder zfolder = (ZFolder) getFolder();
            String rights = zfolder.getEffectivePerms();
            return rights == null ? ~0 : ACL.stringToRights(rights);
        }
    }


    boolean isCreatable() {
        String path = mPath.toLowerCase();
        return !path.matches("\\s*notebook\\s*(/.*)?") &&
               !path.matches("\\s*contacts\\s*(/.*)?") &&
               !path.matches("\\s*calendar\\s*(/.*)?");
    }

    /** Returns whether the server can return the <tt>READ-WRITE</tt> response
     *  code when the folder referenced by this path is <tt>SELECT</tt>ed. */
    boolean isWritable() throws ServiceException {
        // RFC 4314 5.2: "The server SHOULD include a READ-WRITE response code in the tagged OK
        //                response if at least one of the "i", "e", or "shared flag rights" is
        //                granted to the current user."
        return isWritable(ACL.RIGHT_DELETE) || isWritable(ACL.RIGHT_INSERT) || isWritable(ACL.RIGHT_WRITE);
    }

    /** Returns <tt>true</tt> if all of the specified rights have been granted
     *  on the folder referenced by this path to the authenticated user. */
    boolean isWritable(short rights) throws ServiceException {
        if (!isSelectable())
            return false;

        if (mFolder instanceof Folder) {
            Folder folder = (Folder) mFolder;
            if (folder instanceof SearchFolder || folder.getDefaultView() == MailItem.TYPE_CONTACT)
                return false;
        } else {
            ZFolder zfolder = (ZFolder) mFolder;
            if (zfolder instanceof ZSearchFolder || zfolder.getDefaultView() == ZFolder.View.contact)
                return false;
        }

        // note that getFolderRights() operates on the referent folder...
        if (rights == 0)
            return true;
        return (getFolderRights() & rights) == rights;
    }

    boolean isSelectable() throws ServiceException {
        if (!isVisible())
            return false;

        if (mFolder instanceof Folder) {
            Folder folder = (Folder) mFolder;
            if (folder.getId() == Mailbox.ID_FOLDER_USER_ROOT)
                return false;
            if (folder.isTagged(Flag.ID_FLAG_DELETED))
                return false;
        } else {
            ZFolder zfolder = (ZFolder) mFolder;
            if (new ItemId(zfolder.getId(), (String) null).getId() == Mailbox.ID_FOLDER_USER_ROOT)
                return false;
            if (zfolder.isIMAPDeleted())
                return false;
        }
        return (mReferent == this ? true : mReferent.isSelectable());
    }

    boolean isVisible() throws ServiceException {
        if (mCredentials != null) {
            if (mCredentials.isFolderHidden(this))
                return false;

            if (mCredentials.isHackEnabled(ImapCredentials.EnabledHack.WM5)) {
                String lcname = mPath.toLowerCase();
                if (lcname.startsWith("sent items") && (lcname.length() == 10 || lcname.charAt(10) == '/'))
                    return false;
            }
        }

        try {
            getFolder();
        } catch (ServiceException e) {
            if (ServiceException.PERM_DENIED.equals(e.getCode()))
                return false;
            throw e;
        }

        // you cannot access your own mailbox via the /home/username mechanism
        if (mOwner != null && belongsTo(mCredentials))
            return false;

        if (mFolder instanceof Folder) {
            Folder folder = (Folder) mFolder;
            // hide all system folders and the user root folder
            if (folder.isHidden())
                return false;
            if (folder.getId() == Mailbox.ID_FOLDER_USER_ROOT && mScope != Scope.REFERENCE)
                return false;
            // calendars, briefcases, etc. are not surfaced in IMAP
            byte view = folder.getDefaultView();
            if (view == MailItem.TYPE_APPOINTMENT || view == MailItem.TYPE_TASK || view == MailItem.TYPE_WIKI || view == MailItem.TYPE_DOCUMENT)
                return false;
            // hide subfolders of trashed mountpoints
            if (mReferent != this && folder.inTrash() && !((Mountpoint) folder).getTarget().equals(mReferent.asItemId()))
                return false;
            // hide other users' mountpoints and mountpoints that point to the same mailbox
            if (folder instanceof Mountpoint && mReferent == this && mScope != Scope.UNPARSED)
                return false;
            // search folder visibility depends on an account setting
            if (folder instanceof SearchFolder)
                return ((SearchFolder) folder).isImapVisible() && ImapFolder.getTypeConstraint((SearchFolder) folder).length > 0;
        } else {
            ZFolder zfolder = (ZFolder) mFolder;
            // the mailbox root folder is not visible
            if (asItemId().getId() == Mailbox.ID_FOLDER_USER_ROOT && mScope != Scope.REFERENCE)
                return false;
            // calendars, briefcases, etc. are not surfaced in IMAP
            ZFolder.View view = zfolder.getDefaultView();
            if (view == ZFolder.View.appointment || view == ZFolder.View.task || view == ZFolder.View.wiki || view == ZFolder.View.document)
                return false;
            // hide other users' mountpoints and mountpoints that point to the same mailbox
            if (zfolder instanceof ZMountpoint && mReferent == this && mScope != Scope.UNPARSED)
                return false;
            // hide all remote searchfolders
            if (zfolder instanceof ZSearchFolder)
                return false;
        }
        return (mReferent == this ? true : mReferent.isVisible());
    }


    String asZimbraPath() {
        return mPath;
    }

    String asResolvedPath() throws ServiceException {
        return getReferent().mPath;
    }

    ItemId asItemId() throws ServiceException {
        if (useReferent())
            return getReferent().mItemId;

        if (mItemId == null)
            getFolder();
        return mItemId;
    }

    @Override public String toString() {
        return asImapPath();
    }

    String asImapPath() {
        String path = mPath, lcpath = path.toLowerCase();
        // make sure that the Inbox is called "INBOX", regardless of how we capitalize it
        if (lcpath.startsWith("inbox") && (lcpath.length() == 5 || lcpath.charAt(5) == '/')) {
            path = "INBOX" + path.substring(5);
        } else if (mCredentials != null && mCredentials.isHackEnabled(ImapCredentials.EnabledHack.WM5)) {
            if (lcpath.startsWith("sent") && (lcpath.length() == 4 || lcpath.charAt(4) == '/'))
                path = "Sent Items" + path.substring(4);
        }

        if (mOwner != null && !mOwner.equals(""))
            path = NAMESPACE_PREFIX + mOwner + (path.equals("") ? "" : "/") + path;
        return path;
    }

    /** Formats a folder path as an IMAP-UTF-7 quoted-string.  Applies all
     *  special hack-specific path transforms. */
    String asUtf7String() {
        return asUtf7String(asImapPath());
    }

    /** Formats a folder path as an IMAP-UTF-7 quoted-string. */
    static String asUtf7String(String imapPath) {
        ByteBuffer bb = FOLDER_ENCODING_CHARSET.encode(imapPath);
        byte[] content = new byte[bb.limit() + 2];
        content[0] = '"';
        System.arraycopy(bb.array(), 0, content, 1, content.length - 2);
        content[content.length - 1] = '"';
        return new String(content).replaceAll("\\\\", "\\\\\\\\");
    }
}
