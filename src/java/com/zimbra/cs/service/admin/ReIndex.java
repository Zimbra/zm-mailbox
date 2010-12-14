/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Splitter;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.CalendarResourceBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mailbox.IndexHelper;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * Admin operation handler for {@code reIndexMailbox(rim)}.
 *
 * @author tim
 * @author ysasaki
 */
public final class ReIndex extends AdminDocumentHandler {

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
            CalendarResource resource = prov.get(CalendarResourceBy.id,
                    account.getId());
            checkCalendarResourceRight(zsc, resource,
                    Admin.R_reindexCalendarResourceMailbox);
        } else {
            checkAccountRight(zsc, account, Admin.R_reindexMailbox);
        }

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(
                account, false);
        if (mbox == null) {
            throw ServiceException.FAILURE(
                    "mailbox not found for account " + accountId, null);
        }

        Element response = zsc.createElement(AdminConstants.REINDEX_RESPONSE);

        if (ACTION_START.equalsIgnoreCase(action)) {
            if (mbox.index.isReIndexInProgress()) {
                response.addAttribute(AdminConstants.A_STATUS, STATUS_RUNNING);
            } else {
                String typesStr = mreq.getAttribute(MailConstants.A_SEARCH_TYPES, null);
                String idsStr = mreq.getAttribute(MailConstants.A_IDS, null);

                if (typesStr != null && idsStr != null) {
                    ServiceException.INVALID_REQUEST("Can't specify both 'types' and 'ids'", null);
                }

                if (typesStr != null) {
                    Set<Byte> types = MailboxIndex.parseTypes(typesStr);
                    mbox.index.startReIndexByType(getOperationContext(zsc, context), types);
                } else if (idsStr != null) {
                    Set<Integer> ids = new HashSet<Integer>();
                    for (String id : Splitter.on(',').trimResults().split(idsStr)) {
                        try {
                            ids.add(Integer.parseInt(id));
                        } catch (NumberFormatException e) {
                            ServiceException.INVALID_REQUEST("invalid item ID: " + id, e);
                        }
                    }
                    mbox.index.startReIndexById(getOperationContext(zsc, context), ids);
                } else {
                    mbox.index.startReIndex(getOperationContext(zsc, context));
                }

                response.addAttribute(AdminConstants.A_STATUS, STATUS_STARTED);
            }
        } else if (ACTION_STATUS.equalsIgnoreCase(action)) {
            IndexHelper.ReIndexStatus status = mbox.index.getReIndexStatus();
            if (status != null) {
                addProgressInfo(response, status);
                response.addAttribute(AdminConstants.A_STATUS, STATUS_RUNNING);
            } else {
                response.addAttribute(AdminConstants.A_STATUS, STATUS_IDLE);
            }
        } else if (ACTION_CANCEL.equalsIgnoreCase(action)) {
            IndexHelper.ReIndexStatus status = mbox.index.cancelReIndex();
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

    private void addProgressInfo(Element response, IndexHelper.ReIndexStatus status) {
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
