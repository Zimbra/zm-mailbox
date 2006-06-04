/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Mar 9, 2005
 */
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.ZimbraLog;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author dkarp
 */
public class DeleteMailbox extends AdminDocumentHandler {

    private static final String[] TARGET_ACCOUNT_PATH = new String[] { AdminService.E_MAILBOX, AdminService.A_ACCOUNTID };
    protected String[] getProxiedAccountPath()  { return TARGET_ACCOUNT_PATH; }

    /**
     * must be careful and only allow access to domain if domain admin
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zc = getZimbraSoapContext(context);

        Element mreq = request.getElement(AdminService.E_MAILBOX);
        String accountId = mreq.getAttribute(AdminService.A_ACCOUNTID);
        
        Account account = Provisioning.getInstance().get(AccountBy.id, accountId);
        if (account == null) {
            if (isDomainAdminOnly(zc)) {
                throw ServiceException.PERM_DENIED("account doesn't exist, unable to determine authorization");
            }
            ZimbraLog.account.warn("DeleteMailbox: account doesn't exist: "+accountId+" (still deleting mailbox)");
        } else if (!canAccessAccount(zc, account)) {
            throw ServiceException.PERM_DENIED("can not access account");
        }
        
        Mailbox mbox = Mailbox.getMailboxByAccountId(accountId, false);
        int mailboxId = -1;
        if (mbox != null) {
            mailboxId = mbox.getId();
            mbox.deleteMailbox();
        }
        
        String idString = (mbox == null) ?
            "<no mailbox for account " + accountId + ">" : Integer.toString(mailboxId);
        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
            new String[] {"cmd", "DeleteMailbox","id", idString}));
        
        Element response = zc.createElement(AdminService.DELETE_MAILBOX_RESPONSE);
        if (mbox != null)
            response.addElement(AdminService.E_MAILBOX)
            .addAttribute(AdminService.A_MAILBOXID, mailboxId);
        return response;
    }
}
