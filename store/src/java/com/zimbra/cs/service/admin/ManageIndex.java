/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2020 Synacor, Inc.
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
package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.google.common.base.Strings;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.ZAttrProvisioning.DelayedIndexStatus;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.index.MailboxIndexUtil;
import com.zimbra.cs.index.solr.BatchedIndexDeletions;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.ManageIndexRequest;
import com.zimbra.soap.admin.message.ManageIndexResponse;
import com.zimbra.soap.admin.type.MailboxByAccountIdSelector;

public final class ManageIndex extends AdminDocumentHandler {

    private static final String STATUS_STARTED = "started";

    private static final String[] TARGET_ACCOUNT_PATH = new String[] {
        AdminConstants.E_MAILBOX, AdminConstants.A_ACCOUNTID
    };

    @Override
    protected String[] getProxiedAccountPath() {
        return TARGET_ACCOUNT_PATH;
    }

    /**
     * must be careful and only allow access to domain if domain admin.
     */
    @Override
    public boolean domainAuthSufficient(Map<String, Object> context) {
        return true;
    }

    /**
     * @return true - which means accept responsibility for measures to prevent account harvesting by delegate admins
     */
    @Override
    public boolean defendsAgainstDelegateAdminAccountHarvesting() {
        return true;
    }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        ManageIndexRequest req = zsc.elementToJaxb(request);
        OperationContext octxt = getOperationContext(zsc, context);
        MailboxByAccountIdSelector selector = req.getMbox();
        String accountId = selector.getId();

        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(AccountBy.id, accountId, zsc.getAuthToken());
        defendAgainstAccountOrCalendarResourceHarvesting(account, AccountBy.id, accountId, zsc,
                Admin.R_reindexMailbox, Admin.R_reindexCalendarResourceMailbox);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account, false);
        if (mbox == null) {
            throw ServiceException.FAILURE("mailbox not found for account " + accountId, null);
        }

        Action action = Action.fromString(req.getAction());

        if (mbox.index.isReIndexInProgress()) {
            throw ServiceException.FAILURE("re-index is in progress, can't modify the index", null);
        }
        switch (action) {
        case DISABLE_INDEXING:
            ZimbraLog.index.info("disabling indexing for %s and deleting index", account.getName());
            disableIndexing(account, mbox, octxt);
            break;
        case ENABLE_INDEXING:
            ZimbraLog.index.info("enabling indexing for %s and recreating index", account.getName());
            enableIndexing(account, mbox, octxt);
            break;
        default:
            break;
        }

        ManageIndexResponse response = new ManageIndexResponse(STATUS_STARTED);
        return JaxbUtil.jaxbToElement(response);
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_reindexMailbox);
        relatedRights.add(Admin.R_reindexCalendarResourceMailbox);
    }

    private static enum Action {
        ENABLE_INDEXING(AdminConstants.A_ENABLE_INDEXING),
        DISABLE_INDEXING(AdminConstants.A_DISABLE_INDEXING);

        private String str;

        private Action(String str) {
            this.str = str;
        }

        public static Action fromString(String str) throws ServiceException {
            if (Strings.isNullOrEmpty(str)) {
                throw ServiceException.INVALID_REQUEST("must specify 'action'", null);
            }
            for (Action action: values()) {
                if (action.str.equalsIgnoreCase(str)) {
                    return action;
                }
            }
            throw ServiceException.INVALID_REQUEST("invalid action specified", null);
        }
    }

    public static void disableIndexing(Account acct, Mailbox mbox, OperationContext octxt) throws ServiceException {
        acct.setFeatureDelayedIndexEnabled(true);
        acct.setDelayedIndexStatus(DelayedIndexStatus.suppressed);
        MailboxIndexUtil.asyncDeleteIndex(mbox, octxt);
    }

    public static void enableIndexing(Account acct, Mailbox mbox, OperationContext octxt) throws ServiceException {
        // make sure that index data isn't currently batched for deletion
        int removed = BatchedIndexDeletions.getInstance().removeDeletions(acct.getId());
        if (removed > 0) {
            ZimbraLog.index.info("removed %s unprocessed index deletions for %s prior to re-enabling indexing", removed, acct.getId());
        }
        acct.setDelayedIndexStatus(DelayedIndexStatus.indexing);
        MailboxIndexUtil.asyncReIndexMailbox(mbox, octxt);
    }
}
