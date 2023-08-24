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

import com.google.common.collect.Lists;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.soap.admin.message.Zimbra10LocatorUpgradeRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * central class for updating the locator in mail_item
 */
public class Zimbra10MailItemLocatorUpgradeExec {
    /**
     * recieves the request from porter application
     *
     * @param locatorUpdateRequest
     * @return
     * @throws ServiceException
     */

    public boolean upgrade(Zimbra10LocatorUpgradeRequest locatorUpdateRequest)
            throws ServiceException {
      LocatorUpdateExecutor.pool.prestartAllCoreThreads();
        if (locatorUpdateRequest.isUpdateAllMailBoxes()) {
            ZimbraLog.account.info("updating locator for all mailboxes");
            List<Mailbox> allLoadedMailboxes = MailboxManager.getInstance().getAllLoadedMailboxes();
            boolean isAllMailboxesUpdated = upgradeMailboxes(allLoadedMailboxes);
            ZimbraLog.account.info("updated locator for all mailboxes ");
            return isAllMailboxesUpdated;
        } else if (null != locatorUpdateRequest.getMboxNumbers()) {
            ZimbraLog.account.info("updating locator for the mailboxes %s ", locatorUpdateRequest.getMboxNumbers());
            List<Integer> mboxNumbers = locatorUpdateRequest.getMboxNumbers();
            List<Mailbox> mailBoxes = Lists.newArrayList();
            for (Integer mboxNumber : mboxNumbers) {
                if (null != mboxNumber) {
                    mailBoxes.add(MailboxManager.getInstance().getMailboxById(mboxNumber));
                }
            }
            return upgradeMailboxes(mailBoxes);
        } else if (null != locatorUpdateRequest.getAccounts()) {
            return upgrade(locatorUpdateRequest.getAccounts());
        }
        LocatorUpdateExecutor.executor.shutdown();
        return false;
    }

    boolean upgradeMailboxes(List<Mailbox> mailboxes) throws ServiceException {
        Account account = Provisioning.getInstance().getAccount("admin");
        for (Mailbox mbox : mailboxes) {
            if (null != mbox) {
                OperationContext ctx = new OperationContext(account, true);
                ZimbraLog.account.info("updating locator for mailboxe %s ", mbox);
                if (!upgrade(mbox.getAllSendersList(ctx))) {
                    return false;
                }
                ZimbraLog.account.info("updated locator for mailbox %s ", mbox);

            }
        }
        ZimbraLog.account.info("updated locator for mailboxes %s ", mailboxes);

        return true;
    }

    public boolean upgrade(List<String> accounts) throws ServiceException {
        try {
            ZimbraLog.account.info("updating locator for accounts %s ", accounts);
            List<Future<Boolean>> resultFlags = new ArrayList<Future<Boolean>>();
            for (String accountId : accounts) {
                if (null != accountId) {
                    LocatorUpgradeDaoWorker daoWorker = new LocatorUpgradeDaoWorker(accountId);
                    Future<Boolean> resultFlag = LocatorUpdateExecutor.executor.submit(daoWorker);
                    resultFlags.add(resultFlag);
                }
            }
            for (Future<Boolean> resultFlag : resultFlags) {
               /* if (!resultFlag.get()) {
                    return false;
                }*/
                while(!resultFlag.isDone()){
                    ZimbraLog.account.info("continuning processing for %s", resultFlag.toString());
                }
            }
            return true;
        } catch (Exception e) {
            ZimbraLog.account.info("Failed updating locator  for accounts : %s ", accounts);

            return false;
        }
    }
}
