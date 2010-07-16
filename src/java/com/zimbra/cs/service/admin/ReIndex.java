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
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * Admin operation handler for <tt>reIndexMailbox(rim)<tt>.
 */
public class ReIndex extends AdminDocumentHandler {

    private static final String ACTION_START = "start";
    private static final String ACTION_STATUS = "status";
    private static final String ACTION_CANCEL = "cancel";

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
            if (mbox.isReIndexInProgress()) {
                throw ServiceException.ALREADY_IN_PROGRESS(accountId, "ReIndex");
            }

            byte[] types = null;
            String typesStr = mreq.getAttribute(MailConstants.A_SEARCH_TYPES,
                    null);
            if (typesStr != null) {
                types = MailboxIndex.parseTypesString(typesStr);
            }

            Set<Integer> itemIds = null;
            String idsStr = mreq.getAttribute(MailConstants.A_IDS, null);
            if (idsStr != null) {
                itemIds = new HashSet<Integer>();
                String targets[] = idsStr.split(",");
                for (String target : targets) {
                    itemIds.add(Integer.parseInt(target));
                }
            }

            HashSet<Byte> typesSet;
            if (types == null) {
                typesSet = null;
            } else {
                typesSet = new HashSet<Byte>();
                for (byte b : types) {
                    typesSet.add(b);
                }
            }

            mbox.reIndex(getOperationContext(zsc, context),
                    typesSet, itemIds, false);

            response.addAttribute(AdminConstants.A_STATUS, "started");
        } else if (ACTION_STATUS.equalsIgnoreCase(action)) {
            synchronized (mbox) {
                if (mbox.isReIndexInProgress()) {
                    Mailbox.BatchedIndexStatus status = mbox.getReIndexStatus();
                    addStatus(response, status);
                    response.addAttribute(AdminConstants.A_STATUS, "running");
                } else {
                    response.addAttribute(AdminConstants.A_STATUS, "idle");
                }
            }
        } else if (ACTION_CANCEL.equalsIgnoreCase(action)) {
            synchronized (mbox) {
                if (!mbox.isReIndexInProgress()) {
                    throw ServiceException.NOT_IN_PROGRESS(accountId, "ReIndex");
                }

                Mailbox.BatchedIndexStatus status = mbox.getReIndexStatus();
                status.mCancel = true;

                response.addAttribute(AdminConstants.A_STATUS, "cancelled");
                addStatus(response, status);
            }
        } else {
            throw ServiceException.INVALID_REQUEST(
                    "Unknown action: " + action, null);
        }

        return response;
    }

    public static void addStatus(Element response, Mailbox.BatchedIndexStatus status) {
        Element prog = response.addElement(AdminConstants.E_PROGRESS);
        prog.addAttribute(AdminConstants.A_NUM_SUCCEEDED,
                status.mNumProcessed - status.mNumFailed);
        prog.addAttribute(AdminConstants.A_NUM_FAILED, status.mNumFailed);
        prog.addAttribute(AdminConstants.A_NUM_REMAINING,
                status.mNumToProcess - status.mNumProcessed);
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_reindexMailbox);
        relatedRights.add(Admin.R_reindexCalendarResourceMailbox);
    }
}
