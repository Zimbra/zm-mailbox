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

package com.zimbra.cs.account.callback;

import java.util.Map;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbOutOfOffice;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Notification;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.ZimbraLog;

public class OutOfOfficeCallback implements AttributeCallback {

    private static final String KEY = OutOfOfficeCallback.class.getName();

    public void preModify(Map context, String attrName, Object value,
            Map attrsToModify, Entry entry, boolean isCreate) throws ServiceException {
    }

    /**
     * need to keep track in context on whether or not we have been called yet, only 
     * reset info once
     */

    public void postModify(Map context, String attrName, Entry entry, boolean isCreate) {
        if (!isCreate) {
            Object done = context.get(KEY);
            if (done == null) {
                context.put(KEY, KEY);
                ZimbraLog.misc.info("need to reset vacation info");
                if (entry instanceof Account) 
                    handleOutOfOffice((Account)entry);
            }
        }
    }

    private void handleOutOfOffice(Account account) {
        Connection conn = null;
        try {
            // clear the OOF database for this account
            Mailbox mbox = Mailbox.getMailboxByAccount(account);
            conn = DbPool.getConnection();
            DbOutOfOffice.clear(conn, mbox);
            conn.commit();
            ZimbraLog.misc.info("reset vacation info");
            //  Convenient place to prune old data, until we determine that this
            // needs to be a separate scheduled process.
            // TODO: only prune once a day?
            DbOutOfOffice.prune(conn, account.getTimeInterval(Provisioning.A_zimbraPrefOutOfOfficeCacheDuration, Notification.DEFAULT_OUT_OF_OFFICE_CACHE_DURATION_MILLIS));
            conn.commit();
        } catch (ServiceException e) {
            DbPool.quietRollback(conn);
        } finally {
            DbPool.quietClose(conn);
        }
    }
}
