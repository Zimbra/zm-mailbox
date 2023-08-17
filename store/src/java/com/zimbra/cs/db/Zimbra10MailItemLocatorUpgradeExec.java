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

import com.zimbra.client.ZMailbox;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.query.InQuery;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.soap.admin.message.Zimbra10LocatorUpgradeRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * central class for updating the locator in mail_item
 */
public class Zimbra10MailItemLocatorUpgradeExec {
    /**
     * recieves the request from porter application
     * @param locatorUpdateRequestequest
     * @return
     * @throws ServiceException
     */
    public boolean upgrade(Zimbra10LocatorUpgradeRequest locatorUpdateRequestequest) throws ServiceException {
        if (locatorUpdateRequestequest.isUpdateAllMailBoxes()) {
            //TODO placeholder forallmailboxes

        } else if (null != locatorUpdateRequestequest.getMboxNumbers() && locatorUpdateRequestequest.getMboxNumbers()
                .size() > 1) {
            List<Integer> mboxNumbers = locatorUpdateRequestequest.getMboxNumbers();
            for (Integer mboxNumber : mboxNumbers) {
                Mailbox mbox = MailboxManager.getInstance().getMailboxById(mboxNumber);
                OperationContext ctx = new OperationContext(mbox);
                return upgrade(mbox.getAllSendersList(ctx));
            }
        } else if (null != locatorUpdateRequestequest.getAccounts() && locatorUpdateRequestequest.getAccounts()
                .size() > 1) {
            return upgrade(locatorUpdateRequestequest.getAccounts());
        }
        return false;
    }
    private boolean upgrade(List<String> accounts) throws ServiceException {
        try {
            List<Future<Boolean>> resultFlags = new ArrayList<Future<Boolean>>();
            for (String accountId : accounts) {
                LocatorUpgradeDaoWorker daoWorker = new LocatorUpgradeDaoWorker(accountId);
                Future<Boolean> resultFlag = LocatorUpdateExecutor.executor.submit(daoWorker);
                resultFlags.add(resultFlag);
            }
            for (Future<Boolean> resultFlag : resultFlags) {
                if (!resultFlag.get()) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
