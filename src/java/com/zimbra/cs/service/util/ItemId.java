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
 * Created on Sep 27, 2005
 */
package com.zimbra.cs.service.util;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;
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

    public ItemId(String encoded, ZimbraSoapContext lc) throws ServiceException {
        if (encoded == null || encoded.equals(""))
            throw ServiceException.INVALID_REQUEST("empty/missing item ID", null);

        // strip off the account id, if present
        int delimiter = encoded.indexOf(ACCOUNT_DELIMITER);
        if (delimiter == 0 || delimiter == encoded.length() - 1)
            throw ServiceException.INVALID_REQUEST("malformed item ID: " + encoded, null);
        if (delimiter != -1)
            mAccountId = encoded.substring(0, delimiter);
        else if (lc != null)
            mAccountId = lc.getRequestedAccountId();
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

    public boolean isLocal() throws ServiceException {
        if (mAccountId == null)
            return true;
        Account acctTarget = Provisioning.getInstance().getAccountById(mAccountId);
        if (acctTarget == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(mAccountId);
        return DocumentHandler.LOCAL_HOST.equalsIgnoreCase(acctTarget.getAttr(Provisioning.A_zimbraMailHost));
    }

    public boolean belongsTo(Account acct) {
        return acct == null || mAccountId == null || mAccountId.equals(acct.getId());
    }
    public boolean belongsTo(String acctId) {
        return acctId == null || mAccountId == null || mAccountId.equals(acctId);
    }
    public boolean belongsTo(Mailbox mbox) {
        return mbox == null || mAccountId == null || mAccountId.equals(mbox.getAccountId());
    }

    public String toString()  { return toString((String) null); }
    public String toString(Account authAccount) {
        return toString(authAccount == null ? null : authAccount.getId());
    }
    public String toString(ZimbraSoapContext lc) {
        return toString(lc == null ? null : lc.getAuthtokenAccountId());
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
}
