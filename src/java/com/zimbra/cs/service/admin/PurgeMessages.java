/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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

/*
 * Created on Apr 2, 2005
 */
package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.client.ZMailbox;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author dkarp
 */
public class PurgeMessages extends AdminDocumentHandler {

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        Element mreq = request.getOptionalElement(AdminConstants.E_MAILBOX);
        String[] accounts;
        if (mreq != null) {
            accounts = new String[] { mreq.getAttribute(AdminConstants.A_ACCOUNTID) };
            
            // accounts are specified, check right or each account
            Provisioning prov = Provisioning.getInstance();
            for (String acctId : accounts) {
                Account acct = prov.get(AccountBy.id, acctId);
                if (acct == null)
                    throw AccountServiceException.NO_SUCH_ACCOUNT(acctId);
                checkAccountRight(zsc, acct, Admin.R_purgeMessages);
            }
            
        } else {
            // all accounts on the system, has to be a system admin
            checkRight(zsc, context, null, AdminRight.PR_SYSTEM_ADMIN_ONLY);
            
            accounts = MailboxManager.getInstance().getAccountIds();
        }
        
        Element response = zsc.createElement(AdminConstants.PURGE_MESSAGES_RESPONSE);
        for (int i = 0; i < accounts.length; i++) {
            Account account = Provisioning.getInstance().getAccountById(accounts[i]);
            if (account == null)
                continue;
            
            if (Provisioning.onLocalServer(account)) { // local
                Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account, false);
                if (mbox == null)
                    continue;
                mbox.purgeMessages(null);
                Element mresp = response.addElement(AdminConstants.E_MAILBOX);
                mresp.addAttribute(AdminConstants.A_ACCOUNTID, account.getId());
                mresp.addAttribute(AdminConstants.A_SIZE, mbox.getSize());
            } else { // remote
                AuthToken authToken = zsc.getAuthToken();
                ZMailbox.Options zoptions = new ZMailbox.Options(authToken.toZAuthToken(), AccountUtil.getAdminSoapUri(account));
                zoptions.setNoSession(true);
                zoptions.setTargetAccount(account.getId());
                zoptions.setTargetAccountBy(Key.AccountBy.id);    
                ZMailbox zmbx = ZMailbox.getMailbox(zoptions);
                zmbx.purgeMessages();
                Element mresp = response.addElement(AdminConstants.E_MAILBOX);
                mresp.addAttribute(AdminConstants.A_ACCOUNTID, account.getId());
                mresp.addAttribute(AdminConstants.A_SIZE, zmbx.getSize());
            }
        }
        return response;
	}
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_purgeMessages);
        notes.add("If account ids are specified, needs effective " +
                Admin.R_purgeMessages.getName() + " right for each account.  " +
                "If account ids are not specified, the authed account has to be a system admin.");
    }
}
