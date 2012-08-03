/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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
package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.soap.ZimbraSoapContext;

public class CompactIndex extends AdminDocumentHandler {

    private static final String ACTION_START = "start";
    private static final String ACTION_STATUS = "status";

    private static final String STATUS_STARTED = "started";
    private static final String STATUS_RUNNING = "running";
    private static final String STATUS_IDLE = "idle";

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

    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        String action = request.getAttribute(MailConstants.E_ACTION);

        Element mreq = request.getElement(AdminConstants.E_MAILBOX);
        String accountId = mreq.getAttribute(AdminConstants.A_ACCOUNTID);

        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(AccountBy.id, accountId, zsc.getAuthToken());
        if (account == null) {
            throw AccountServiceException.NO_SUCH_ACCOUNT(accountId);
        }

        if (account.isCalendarResource()) {
            // need a CalendarResource instance for RightChecker
            CalendarResource resource = prov.get(Key.CalendarResourceBy.id, account.getId());
            checkCalendarResourceRight(zsc, resource, Admin.R_reindexCalendarResourceMailbox);
        } else {
            checkAccountRight(zsc, account, Admin.R_reindexMailbox);
        }

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account, false);
        if (mbox == null) {
            throw ServiceException.FAILURE("mailbox not found for account " + accountId, null);
        }

        Element response = zsc.createElement(AdminConstants.COMPACT_INDEX_RESPONSE);
        if (ACTION_START.equalsIgnoreCase(action)) {
            if (mbox.index.isCompactIndexInProgress()) {
                response.addAttribute(AdminConstants.A_STATUS, STATUS_RUNNING);
            } else {
                mbox.index.startCompactIndex();
            }
            response.addAttribute(AdminConstants.A_STATUS, STATUS_STARTED);
        } else if (ACTION_STATUS.equalsIgnoreCase(action)) {
            if (mbox.index.isCompactIndexInProgress()) {
                response.addAttribute(AdminConstants.A_STATUS, STATUS_RUNNING);
            } else {
                response.addAttribute(AdminConstants.A_STATUS, STATUS_IDLE);
            }
        } else {
            throw ServiceException.INVALID_REQUEST("Unknown action: " + action, null);
        }

        return response;
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_reindexMailbox);
        relatedRights.add(Admin.R_reindexCalendarResourceMailbox);
    }

}
