/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
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
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author dkarp
 */
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

    /**
     * @return TRUE if this item is on the local server
     * @throws ServiceException
     */
    public boolean isLocal() throws ServiceException {
        if (mAccountId == null)
            return true;
        Account acctTarget = Provisioning.getInstance().get(AccountBy.id, mAccountId);
        if (acctTarget == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(mAccountId);
        return DocumentHandler.LOCAL_HOST.equalsIgnoreCase(acctTarget.getAttr(Provisioning.A_zimbraMailHost));
    }

    /**
     * @param acct
     * @return TRUE if the item is in the passed-in account
     */
    public boolean belongsTo(Account acct) {
        return acct == null || mAccountId == null || mAccountId.equals(acct.getId());
    }
    /**
     * @param acctId
     * @return TRUE if the item is in the passed-in account
     */
    public boolean belongsTo(String acctId) {
        return acctId == null || mAccountId == null || mAccountId.equals(acctId);
    }
    /**
     * @param mbox
     * @return TRUE if the item is in the passed-in mailbox
     */
    public boolean belongsTo(Mailbox mbox) {
        return mbox == null || mAccountId == null || mAccountId.equals(mbox.getAccountId());
    }

    @Override
    public String toString() {
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
    
    @Override
    public boolean equals(Object that) {
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

    @Override
    public int hashCode() {
        return (mAccountId == null ? 0 : mAccountId.hashCode()) ^ mId;
    }

    // groups folders by account id
    // local account has key == null
    // value is list of folder id integers
    // mountpoints are fully resolved to real folder ids for local account
    public static Map<String, List<Integer>> groupFoldersByAccount(
            OperationContext octxt, Mailbox mbox, List<ItemId> folderIids)
    throws ServiceException {
        Map<String, List<Integer>> foldersMap = new HashMap<String, List<Integer>>();
        for (ItemId iidFolder : folderIids) {
            int folderId = iidFolder.getId();
            String targetAccountId = iidFolder.getAccountId();
            if (mbox.getAccountId().equals(targetAccountId))
                targetAccountId = null;
            if (targetAccountId == null) {
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
                    throw ServiceException.TOO_MANY_HOPS();
            }
            List<Integer> folderList = foldersMap.get(targetAccountId);
            if (folderList == null) {
                folderList = new ArrayList<Integer>();
                foldersMap.put(targetAccountId, folderList);
            }
            folderList.add(folderId);
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
