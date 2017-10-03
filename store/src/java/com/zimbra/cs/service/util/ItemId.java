/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
/*
 * Created on Sep 27, 2005
 */
package com.zimbra.cs.service.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.mailbox.FolderStore;
import com.zimbra.common.mailbox.ItemIdentifier;
import com.zimbra.common.mailbox.MailboxStore;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;

public class ItemId implements java.io.Serializable {
    private static final long serialVersionUID = -9044615129495573523L;

    private final String mAccountId;
    private final int    mId;
    private int    mSubpartId = -1;

    public ItemId(MailItem item) {
        this(item.getAccountId(), item.getId());
    }

    public ItemId(Mailbox mbox, int id) {
        this(mbox.getAccountId(), id);
    }

    public ItemId(String acctId, int id) {
        mAccountId = acctId;  mId = id;
    }

    public ItemId(MailItem item, int subId) {
        this(item.getAccountId(), item.getId(), subId);
    }

    public ItemId(String acctId, int id, int subId) {
        mAccountId = acctId;  mId = id;  mSubpartId = subId;
    }

    public ItemId(ItemIdentifier itemIdentifier) {
        mAccountId = itemIdentifier.accountId;
        mId = itemIdentifier.id;
        mSubpartId = itemIdentifier.subPartId;
    }

    public ItemId(String encoded, ZimbraSoapContext zsc) throws ServiceException {
        this(encoded, zsc.getRequestedAccountId());
    }

    public ItemId(FolderStore folder, String defaultAccountId) throws ServiceException {
        this(new ItemIdentifier(folder.getFolderIdAsString(), defaultAccountId));
    }

    public ItemId(String encoded, String defaultAccountId) throws ServiceException {
        this(new ItemIdentifier(encoded, defaultAccountId));
    }

    public static ItemId createFromEncoded(String encoded, String defaultAccountId) throws ServiceException {
        return new ItemId(encoded, defaultAccountId);
    }

    public String getAccountId()  { return mAccountId; }
    public int getId()            { return mId; }
    public int getSubpartId()     { return mSubpartId; }

    public boolean hasSubpart()   { return mSubpartId >= 0; }

    /** Returns whether this item is on the local server. */
    public boolean isLocal() throws ServiceException {
        if (mAccountId == null)
            return true;
        Account acctTarget = Provisioning.getInstance().get(AccountBy.id, mAccountId);
        if (acctTarget == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(mAccountId);
        return DocumentHandler.getLocalHost().equalsIgnoreCase(acctTarget.getAttr(Provisioning.A_zimbraMailHost));
    }

    /** Returns whether the item belongs to the mailbox owned by the passed-in
     *  {@link Account}.  If <code>acct</code> is <tt>null</tt> and/or this
     *  ItemId was unqualified, returns <tt>true</tt>. */
    public boolean belongsTo(Account acct) {
        return acct == null || mAccountId == null || mAccountId.equals(acct.getId());
    }

    /** Returns whether the item belongs to the mailbox owned by the passed-in
     *  account.  If <code>acctId</code> is <tt>null</tt> and/or this ItemId
     *  was unqualified, returns <tt>true</tt>. */
    public boolean belongsTo(String acctId) {
        return acctId == null || mAccountId == null || mAccountId.equals(acctId);
    }

    /** Returns whether the item belongs to the specified {@link MailboxStore}.  If
     *  <code>mbox</code> is <tt>null</tt> and/or this ItemId was unqualified,
     *  returns <tt>true</tt>. */
    public boolean belongsTo(MailboxStore mbox) {
        try {
            return mbox == null || mAccountId == null || mAccountId.equals(mbox.getAccountId());
        } catch (ServiceException e) {
            return false;
        }
    }

    public ItemIdentifier toItemIdentifier() {
        return new ItemIdentifier(mAccountId, mId, mSubpartId);
    }

    @Override
    public String toString() {
        return toItemIdentifier().toString();
    }

    public String toString(Account authAccount) {
        return toString(authAccount == null ? null : authAccount.getId());
    }

    public String toString(ItemIdFormatter ifmt) {
        return toString(ifmt == null ? null : ifmt.getAuthenticatedId());
    }

    public String toString(String authAccountId) {
        return toItemIdentifier().toString(authAccountId);
    }

    @Override public boolean equals(Object that) {
        if (this == that)
            return true;
        else if (!(that instanceof ItemId))
            return false;

        ItemId other = (ItemId) that;
        if (mAccountId == other.mAccountId || (mAccountId != null && mAccountId.equalsIgnoreCase(other.mAccountId)))
            return (other.mId == this.mId && other.mSubpartId == this.mSubpartId);
        else
            return false;
    }

    @Override public int hashCode() {
        return (mAccountId == null ? 0 : mAccountId.hashCode()) ^ mId;
    }

    // groups folders by account id
    // value is list of folder id integers
    // mountpoints in local account are fully resolved to real folder in target account
    public static Map<String, List<Integer>> groupFoldersByAccount(
            OperationContext octxt, Mailbox mbox, List<ItemId> folderIids)
    throws ServiceException {
        Map<String, List<Integer>> foldersMap = new HashMap<String, List<Integer>>();
        for (ItemId iidFolder : folderIids) {
            String targetAccountId = iidFolder.getAccountId();
            int folderId = iidFolder.getId();
            try {
                if (mbox.getAccountId().equals(targetAccountId)) {
                    boolean isMountpoint = true;
                    int hopCount = 0;
                    // resolve local mountpoint to a real folder; deal with possible mountpoint chain
                    while (isMountpoint && hopCount < ZimbraSoapContext.MAX_HOP_COUNT) {
                        Folder folder = mbox.getFolderById(octxt, folderId);
                        isMountpoint = folder instanceof Mountpoint;
                        if (isMountpoint) {
                            Mountpoint mp = (Mountpoint) folder;
                            folderId = mp.getRemoteId();
                            if (!mp.isLocal()) {
                                // done resolving if pointing to a different account
                                targetAccountId = mp.getOwnerId();
                                Account targetAcct = Provisioning.getInstance().get(Key.AccountBy.id, targetAccountId);
                                if (targetAcct == null) {
                                    throw MailServiceException.NO_SUCH_MOUNTPOINT(
                                            mp.getId(), mp.getOwnerId(), mp.getRemoteId(),
                                            AccountServiceException.NO_SUCH_ACCOUNT(targetAccountId));
                                }
                                break;
                            }
                            hopCount++;
                        }
                    }
                    if (hopCount >= ZimbraSoapContext.MAX_HOP_COUNT)
                        throw MailServiceException.TOO_MANY_HOPS(iidFolder);
                }
                List<Integer> folderList = foldersMap.get(targetAccountId);
                if (folderList == null) {
                    folderList = new ArrayList<Integer>();
                    foldersMap.put(targetAccountId, folderList);
                }
                folderList.add(folderId);
            } catch (ServiceException e) {
                String ecode = e.getCode();
                ItemIdFormatter ifmt = new ItemIdFormatter(targetAccountId, targetAccountId, false);
                if (ecode.equals(ServiceException.PERM_DENIED)) {
                    // share permission was revoked
                    ZimbraLog.calendar.warn("Ignorable permission error %s", ifmt.formatItemId(folderId), e);
                } else if (ecode.equals(MailServiceException.NO_SUCH_FOLDER)) {
                    // shared calendar folder was deleted by the owner
                    ZimbraLog.calendar.warn("Ignoring deleted folder %s", ifmt.formatItemId(folderId));
                } else {
                    throw e;
                }
            }
        }
        return foldersMap;
    }

    // ZCS-6695 Deserialization protection
    private final void readObject(ObjectInputStream in) throws java.io.IOException {
        throw new IOException("Cannot be deserialized");
    }

    public static void main(String[] args) {
        ItemId foo = null;
        try {
            foo = new ItemId("34480-508bc90b-d85e-45d6-bca2-7c34b7c407cb:34479", (String) null);
            System.out.println(foo.toString());
        } catch (ServiceException e) {
            e.printStackTrace();
        }
    }
}
