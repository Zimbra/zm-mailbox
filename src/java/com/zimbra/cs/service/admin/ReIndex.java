/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.soap.ZimbraSoapContext;

public class ReIndex extends AdminDocumentHandler {

    private final String ACTION_START = "start";
    private final String ACTION_STATUS = "status";
    private final String ACTION_CANCEL = "cancel";

    private static final String[] TARGET_ACCOUNT_PATH = new String[] { AdminConstants.E_MAILBOX, AdminConstants.A_ACCOUNTID };
    protected String[] getProxiedAccountPath()  { return TARGET_ACCOUNT_PATH; }

    /**
     * must be careful and only allow access to domain if domain admin
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        String action = request.getAttribute(MailConstants.E_ACTION);

        Element mreq = request.getElement(AdminConstants.E_MAILBOX);
        String accountId = mreq.getAttribute(AdminConstants.A_ACCOUNTID);
        
        Account account = Provisioning.getInstance().get(AccountBy.id, accountId);
        if (account == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(accountId);
        if (!canAccessAccount(zsc, account))
            throw ServiceException.PERM_DENIED("can not access account");

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(accountId, false);
        if (mbox == null)
            throw ServiceException.FAILURE("mailbox not found for account " + accountId, null);

        Element response = zsc.createElement(AdminConstants.REINDEX_RESPONSE);

        if (action.equalsIgnoreCase(ACTION_START)) {
            if (mbox.isReIndexInProgress()) {
                throw ServiceException.ALREADY_IN_PROGRESS(accountId, "ReIndex");
            }
            
            byte[] types = null;
            String typesStr = mreq.getAttribute(MailConstants.A_SEARCH_TYPES, null);
            if (typesStr != null) {
                types = MailboxIndex.parseTypesString(typesStr);
            }
            
            Set<Integer> itemIds = null;
            String idsStr = mreq.getAttribute(MailConstants.A_IDS, null);
            if (idsStr != null) {
                itemIds = new HashSet<Integer>();
                String targets[] = idsStr.split(",");
                for (String target : targets)
                    itemIds.add(Integer.parseInt(target));
            }

            ReIndexThread thread = new ReIndexThread(mbox, getOperationContext(zsc, context), types, itemIds);
            thread.start();

            response.addAttribute(AdminConstants.A_STATUS, "started");
        } else if (action.equalsIgnoreCase(ACTION_STATUS)) {
            synchronized (mbox) {
                if (!mbox.isReIndexInProgress()) {
                    throw ServiceException.NOT_IN_PROGRESS(accountId, "ReIndex");
                }
                Mailbox.BatchedIndexStatus status = mbox.getReIndexStatus();
                addStatus(response, status);
            }
            response.addAttribute(AdminConstants.A_STATUS, "running");

        } else if (action.equalsIgnoreCase(ACTION_CANCEL)) {
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
            throw ServiceException.INVALID_REQUEST("Unknown action: "+action, null);
        }

        return response;
    }

    public static void addStatus(Element response, Mailbox.BatchedIndexStatus status) {
        Element prog = response.addElement(AdminConstants.E_PROGRESS);
        prog.addAttribute(AdminConstants.A_NUM_SUCCEEDED, (status.mNumProcessed-status.mNumFailed));
        prog.addAttribute(AdminConstants.A_NUM_FAILED, status.mNumFailed);
        prog.addAttribute(AdminConstants.A_NUM_REMAINING, (status.mNumToProcess-status.mNumProcessed));
    }

    public static class ReIndexThread extends Thread {
        private Mailbox mMbox;
        private OperationContext mOctxt;
        private Set<Byte> mTypes = null;
        private Set<Integer> mItemIds = null;
        
        public ReIndexThread(Mailbox mbox, OperationContext octxt, byte[] types, Set<Integer> itemIds) {
            mMbox = mbox;
            mOctxt = octxt;

            if (types == null)
                mTypes = null;
            else {
                mTypes = new HashSet<Byte>();
                for (byte b : types) {
                    mTypes.add(b);
                }
            }
            
            mItemIds = itemIds;
        }

        public void run() {
            try {
                mMbox.reIndex(mOctxt, mTypes, mItemIds, 0, false);
            } catch (ServiceException e) {
                if (!e.getCode().equals(ServiceException.INTERRUPTED)) 
                    e.printStackTrace();
            }
        }
    }

}
