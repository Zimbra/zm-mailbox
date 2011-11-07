/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011 Zimbra, Inc.
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

package com.zimbra.cs.account.callback;

import java.util.Map;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbOutOfOffice;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Notification;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

public class OutOfOfficeCallback extends AttributeCallback {

    @Override 
    public void preModify(CallbackContext context, String attrName, Object value,
            Map attrsToModify, Entry entry) {
    }

    @Override 
    public void postModify(CallbackContext context, String attrName, Entry entry) {
        
        if (context.isDoneAndSetIfNot(OutOfOfficeCallback.class)) {
            return;
        }
        
        if (!context.isCreate()) {
            ZimbraLog.misc.info("need to reset vacation info");
            if (entry instanceof Account) {
                handleOutOfOffice((Account)entry);
            }
        }
    }

    private void handleOutOfOffice(Account account) {
        try {
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

            DbConnection conn = null;
            try {
                // clear the OOF database for this account
                conn = DbPool.getConnection(mbox);
                DbOutOfOffice.clear(conn, mbox);
                conn.commit();
                ZimbraLog.misc.info("reset vacation info");

                // Convenient place to prune old data, until we determine that this
                //  needs to be a separate scheduled process.
                // TODO: only prune once a day?
                long interval = account.getTimeInterval(Provisioning.A_zimbraPrefOutOfOfficeCacheDuration, Notification.DEFAULT_OUT_OF_OFFICE_CACHE_DURATION_MILLIS);
                DbOutOfOffice.prune(conn, interval);
                conn.commit();
            } catch (ServiceException e) {
                DbPool.quietRollback(conn);
            } finally {
                DbPool.quietClose(conn);
            }
        } catch (ServiceException e) {
            ZimbraLog.misc.warn("error handling out-of-office", e);
        }
    }
}
