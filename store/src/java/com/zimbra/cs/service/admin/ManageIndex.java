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

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxIndex;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.ManageIndexRequest;
import com.zimbra.soap.admin.type.MailboxByAccountIdSelector;

public final class ManageIndex extends AdminDocumentHandler {

    private static final String ACTION_START = "start";
    private static final String ACTION_STATUS = "status";
    private static final String ACTION_CANCEL = "cancel";

    private static final String STATUS_STARTED = "started";
    private static final String STATUS_RUNNING = "running";
    private static final String STATUS_IDLE = "idle";
    private static final String STATUS_CANCELLED = "cancelled";

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

        String action = req.getAction();

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

        boolean deleteIndex = req.getDeleteIndex();
        boolean enableIndexing = req.getEnableIndexing();
        if (deleteIndex == enableIndexing) {
            throw ServiceException.INVALID_REQUEST("deleteIndex and enableIndexing must be exclusive", null);
        }

        Element response = zsc.createElement(AdminConstants.MANAGE_INDEX_RESPONSE);

        if (ACTION_START.equalsIgnoreCase(action)) {
            if (mbox.index.isReIndexInProgress()) {
                response.addAttribute(AdminConstants.A_STATUS, STATUS_RUNNING);
            } else {
                mbox.index.startReIndex(deleteIndex, enableIndexing);
                response.addAttribute(AdminConstants.A_STATUS, STATUS_STARTED);
            }
        } else if (ACTION_STATUS.equalsIgnoreCase(action)) {
            MailboxIndex.ReIndexStatus status = mbox.index.getReIndexStatus();
            if (status != null) {
                addProgressInfo(response, status);
                response.addAttribute(AdminConstants.A_STATUS, STATUS_RUNNING);
            } else {
                response.addAttribute(AdminConstants.A_STATUS, STATUS_IDLE);
            }
        } else if (ACTION_CANCEL.equalsIgnoreCase(action)) {
            if (deleteIndex) {
                throw ServiceException.INVALID_REQUEST("Can't cancel deleting index.", null);
            }
            MailboxIndex.ReIndexStatus status = mbox.index.cancelReIndex();
            if (status != null) {
                response.addAttribute(AdminConstants.A_STATUS, STATUS_CANCELLED);
                addProgressInfo(response, status);
            } else {
                response.addAttribute(AdminConstants.A_STATUS, STATUS_IDLE);
            }
        } else {
            throw ServiceException.INVALID_REQUEST("Unknown action: " + action, null);
        }

        return response;
    }

    private void addProgressInfo(Element response, MailboxIndex.ReIndexStatus status) {
        Element prog = response.addElement(AdminConstants.E_PROGRESS);
        prog.addAttribute(AdminConstants.A_NUM_SUCCEEDED, status.getProcessed() - status.getFailed());
        prog.addAttribute(AdminConstants.A_NUM_FAILED, status.getFailed());
        prog.addAttribute(AdminConstants.A_NUM_REMAINING, status.getTotal() - status.getProcessed());
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_reindexMailbox);
        relatedRights.add(Admin.R_reindexCalendarResourceMailbox);
    }
}
