/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2023 Synacor, Inc.
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
package com.zimbra.cs.db;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.query.InQuery;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.OperationContext;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Thread class to complete DB locator update per account
 */
public class LocatorUpgradeDaoWorker implements Callable<Boolean> {
    private final String accountPrimaryEmail;

    public LocatorUpgradeDaoWorker(String accountid) {
        this.accountPrimaryEmail = accountid;
    }

    /**
     * completes the locator update in DB for an account
     * @return
     * @throws Exception
     */
    @Override public Boolean call() throws Exception {
        try {
            Account account = Provisioning.getInstance().getAccountByName(this.accountPrimaryEmail.split("@")[0]);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(account.getId(), true);
            OperationContext ctx = new OperationContext(account, true);
            List<Integer> sentRAndRecievedToBeUpdated = mbox.getAllRecordsForSentAndRecieved(ctx, mbox,
                    this.accountPrimaryEmail, false);
            mbox.updateLocatorFieldZimbra10(ctx, sentRAndRecievedToBeUpdated);
            List<Integer> recordNumbersPending = mbox.getAllRecordsForSentAndRecieved(ctx, mbox,
                    this.accountPrimaryEmail, true);
            if (recordNumbersPending.size() == 0) {
                return true;
            } else {
                return false;
            }
        } catch (ServiceException e) {
            return false;
        }
    }
}
