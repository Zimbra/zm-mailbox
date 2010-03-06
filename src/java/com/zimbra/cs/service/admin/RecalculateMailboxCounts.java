/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.soap.ZimbraSoapContext;

public class RecalculateMailboxCounts extends AdminDocumentHandler {

    private static final String[] TARGET_ACCOUNT_PATH = new String[] { AdminConstants.E_MAILBOX, AdminConstants.A_ACCOUNTID };
    @Override protected String[] getProxiedAccountPath()  { return TARGET_ACCOUNT_PATH; }

    @Override
    protected Element proxyIfNecessary(Element request, Map<String, Object> context) throws ServiceException {
        // if we've explicitly been told to execute here, don't proxy
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        if (zsc.getProxyTarget() != null)
            return null;

        // check whether we need to proxy to the home server of a target account
        Provisioning prov = Provisioning.getInstance();
        String[] xpath = getProxiedAccountPath();
        String acctId = (xpath != null ? getXPath(request, xpath) : null);
        if (acctId != null) {
            Account acct = getAccount(prov, AccountBy.id, acctId, zsc.getAuthToken());
            if (acct != null && !Provisioning.onLocalServer(acct))
                return proxyRequest(request, context, acctId);
        }

        return null;
    }

    @Override public boolean domainAuthSufficient(Map<String, Object> context) {
        return true;
    }


    @Override public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        Element mreq = request.getElement(AdminConstants.E_MAILBOX);
        String accountId = mreq.getAttribute(AdminConstants.A_ACCOUNTID);

        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(AccountBy.id, accountId, zsc.getAuthToken());
        if (account == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(accountId);
        
        checkAdminLoginAsRight(zsc, prov, account);

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account, false);
        if (mbox == null)
            throw ServiceException.FAILURE("mailbox not found for account " + accountId, null);

        mbox.recalculateFolderAndTagCounts();

        Element response = zsc.createElement(AdminConstants.RECALCULATE_MAILBOX_COUNTS_RESPONSE);
        Element mboxElem = response.addElement(AdminConstants.E_MAILBOX);
        mboxElem.addAttribute(AdminConstants.A_ACCOUNTID, accountId);
        mboxElem.addAttribute(AdminConstants.A_QUOTA_USED, mbox.getSize());
        return response;
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_adminLoginAs);
        relatedRights.add(Admin.R_adminLoginCalendarResourceAs);
        notes.add(AdminRightCheckPoint.Notes.ADMIN_LOGIN_AS);
    }
}
