/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.account;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimerTask;

import com.zimbra.common.account.ZAttrProvisioning.AccountStatus;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Mountpoint;

/**
 * This task:
 * - disables an external virtual account when all its accessible shares have been revoked or expired.
 * - deletes an external virtual account after XXX of being disabled.
 */
public class ExternalAccountManagerTask extends TimerTask {

    @Override
    public void run() {
        ZimbraLog.misc.info("Starting external virtual account status manager task");

        Provisioning prov = Provisioning.getInstance();
        SearchAccountsOptions searchOpts = new SearchAccountsOptions();
        try {
            searchOpts.setFilter(ZLdapFilterFactory.getInstance().externalAccountsHomedOnServer(
                    prov.getLocalServer().getServiceHostname()));
            List<NamedEntry> accounts = prov.searchDirectory(searchOpts);

            for (NamedEntry ne : accounts) {
                Account account = (Account) ne;
                Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
                if (mbox == null) {
                    ZimbraLog.misc.error(
                            "Mailbox for external virtual account %s is unexpectedly null", account.getId());
                    continue;
                }
                List<Folder> folders = mbox.getFolderList(null, SortBy.NONE);
                List<Mountpoint> mountpoints = new LinkedList<Mountpoint>();
                for (Folder folder : folders) {
                    if (folder instanceof Mountpoint) {
                        mountpoints.add((Mountpoint) folder);
                    }
                }
                if (mountpoints.isEmpty()) {
                    disableOrDeleteAccount(prov, account, mbox);
                    continue;
                }
                // Get all shares granted to the external user
                ShareInfoVisitor shareInfoVisitor = new ShareInfoVisitor(mountpoints);
                ShareInfo.Published.get(prov, account, ACL.GRANTEE_GUEST, null, shareInfoVisitor);
                boolean hasValidMountpoint = shareInfoVisitor.getResult();

                if (!hasValidMountpoint) {
                    disableOrDeleteAccount(prov, account, mbox);
                } else if (account.getAccountStatus() == AccountStatus.closed) {
                    // re-enable account
                    account.setAccountStatus(AccountStatus.active);
                }
            }
        } catch (Throwable e) {
            ZimbraLog.misc.warn("Error during external virtual account status manager task run", e);
        }
        ZimbraLog.misc.info("Finished external virtual account status manager task");
    }

    private static void disableOrDeleteAccount(Provisioning prov, Account account, Mailbox mbox)
            throws ServiceException {
        AccountStatus accountStatus = account.getAccountStatus();
        if (accountStatus == AccountStatus.active) {
            account.setExternalAccountDisabledTime(new Date());
            account.setAccountStatus(AccountStatus.closed);
        } else {
            long disabledAcctLifetime = account.getExternalAccountLifetimeAfterDisabled();
            if (accountStatus == AccountStatus.closed && disabledAcctLifetime != 0) {
                Date timeWhenDisabled = account.getExternalAccountDisabledTime();
                if (timeWhenDisabled != null &&
                        System.currentTimeMillis() - timeWhenDisabled.getTime() > disabledAcctLifetime) {
                    mbox.deleteMailbox();
                    prov.deleteAccount(account.getId());
                }
            }
        }
    }

    private static class ShareInfoVisitor implements Provisioning.PublishedShareInfoVisitor {

        private List<Mountpoint> mountpoints;
        private boolean result = false;

        public ShareInfoVisitor(List<Mountpoint> mountpoints) {
            this.mountpoints = mountpoints;
        }

        @Override
        public void visit(ShareInfoData shareInfoData) throws ServiceException {
            for (Mountpoint mountpoint : mountpoints) {
                if (mountpoint.getOwnerId().equals(shareInfoData.getOwnerAcctId()) &&
                        mountpoint.getRemoteId() == shareInfoData.getItemId()) {
                    result = true;
                    break;
                }
            }
        }

        public boolean getResult() {
            return result;
        }
    }
}
