/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
/*
 * Created on Sep 27, 2005
 */
package com.zimbra.cs.service.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;

public class ItemId {
    private static final char ACCOUNT_DELIMITER = ':';
    private static final char PART_DELIMITER    = '-';

    private String mAccountId;
    private int    mId;
    private int    mSubpartId = -1;

    public ItemId(MailItem item) {
        this(item.getMailbox(), item.getId());
    }

    public ItemId(Mailbox mbox, int id) {
        this(mbox.getAccountId(), id);
    }

    public ItemId(String acctId, int id) {
        mAccountId = acctId;  mId = id;
    }

    public ItemId(MailItem item, int subId) {
        this(item.getMailbox().getAccountId(), item.getId(), subId);
    }

    public ItemId(String acctId, int id, int subId) {
        mAccountId = acctId;  mId = id;  mSubpartId = subId;
    }

    public ItemId(String encoded, ZimbraSoapContext zsc) throws ServiceException {
        this(encoded, zsc.getRequestedAccountId());
    }

    public ItemId(String encoded, String defaultAccountId) throws ServiceException {
        if (encoded == null || encoded.equals(""))
            throw ServiceException.INVALID_REQUEST("empty/missing item ID", null);

        // strip off the account id, if present
        int delimiter = encoded.indexOf(ACCOUNT_DELIMITER);
        if (delimiter == 0 || delimiter == encoded.length() - 1)
            throw ServiceException.INVALID_REQUEST("malformed item ID: " + encoded, null);
        if (delimiter != -1)
            mAccountId = encoded.substring(0, delimiter);
        else if (defaultAccountId != null)
            mAccountId = defaultAccountId;
        encoded = encoded.substring(delimiter + 1);

        // break out the appointment sub-id, if present
        delimiter = encoded.indexOf(PART_DELIMITER);
        if (delimiter == encoded.length() - 1)
            throw ServiceException.INVALID_REQUEST("malformed item ID: " + encoded, null);
        try {
            if (delimiter > 0) {
                mSubpartId = Integer.parseInt(encoded.substring(delimiter + 1));
                if (mSubpartId < 0)
                    throw ServiceException.INVALID_REQUEST("malformed item ID: " + encoded, null);
                encoded = encoded.substring(0, delimiter);
            }
            mId = Integer.parseInt(encoded);
        } catch (NumberFormatException nfe) {
            throw ServiceException.INVALID_REQUEST("malformed item ID: " + encoded, nfe);
        }
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

    /** Returns whether the item belongs to the specified {@link Mailbox}.  If
     *  <code>mbox</code> is <tt>null</tt> and/or this ItemId was unqualified,
     *  returns <tt>true</tt>. */
    public boolean belongsTo(Mailbox mbox) {
        return mbox == null || mAccountId == null || mAccountId.equals(mbox.getAccountId());
    }


    @Override public String toString() {
        return toString((String) null);
    }

    public String toString(Account authAccount) {
        return toString(authAccount == null ? null : authAccount.getId());
    }

    public String toString(ItemIdFormatter ifmt) {
        return toString(ifmt == null ? null : ifmt.getAuthenticatedId());
    }

    public String toString(String authAccountId) {
        StringBuffer sb = new StringBuffer();
        if (mAccountId != null && mAccountId.length() > 0 && !mAccountId.equals(authAccountId))
            sb.append(mAccountId).append(ACCOUNT_DELIMITER);
        sb.append(mId);
        if (hasSubpart())
            sb.append(PART_DELIMITER).append(mSubpartId);
        return sb.toString();
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
                                Account targetAcct = Provisioning.getInstance().get(Provisioning.AccountBy.id, targetAccountId);
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
                    ZimbraLog.calendar.warn(
                            "Ignorable permission error " + ifmt.formatItemId(folderId), e);
                } else if (ecode.equals(MailServiceException.NO_SUCH_FOLDER)) {
                    // shared calendar folder was deleted by the owner
                    ZimbraLog.calendar.warn(
                            "Ignoring deleted folder " + ifmt.formatItemId(folderId));
                } else {
                    throw e;
                }
            }
        }
        return foldersMap;
    }

    public static void main(String[] args) {
        ItemId foo = null;
        try {
            foo = new ItemId("34480-508bc90b-d85e-45d6-bca2-7c34b7c407cb:34479", (String) null);
        } catch (ServiceException e) {
            e.printStackTrace();
        }
        System.out.println(foo.toString());
    }
}
