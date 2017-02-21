/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

/*
 * Created on Mar 9, 2005
 */
package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class DeleteMailbox extends AdminDocumentHandler {

    private static final String[] TARGET_ACCOUNT_PATH = new String[] { AdminConstants.E_MAILBOX, AdminConstants.A_ACCOUNTID };
    @Override protected String[] getProxiedAccountPath()  { return TARGET_ACCOUNT_PATH; }

    /**
     * must be careful and only allow access to domain if domain admin
     */
    @Override public boolean domainAuthSufficient(Map<String, Object> context) {
        return true;
    }

    @Override public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        Element mreq = request.getElement(AdminConstants.E_MAILBOX);
        String accountId = mreq.getAttribute(AdminConstants.A_ACCOUNTID);
        
        Account account = Provisioning.getInstance().get(AccountBy.id, accountId, zsc.getAuthToken());
        if (account == null) {
            // Note: isDomainAdminOnly *always* returns false for pure ACL based AccessManager 
            if (isDomainAdminOnly(zsc)) {
                throw ServiceException.PERM_DENIED("account doesn't exist, unable to determine authorization");
            }
            
            // still need to check right, since we don't have an account, the 
            // last resort is checking the global grant.  Do this for now until 
            // there is complain.
            checkRight(zsc, context, null, Admin.R_deleteAccount); 
            
            ZimbraLog.account.warn("DeleteMailbox: account doesn't exist: "+accountId+" (still deleting mailbox)");

        } else {
            checkAccountRight(zsc, account, Admin.R_deleteAccount);   
        }

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(accountId, false);
        int mailboxId = -1;
        if (mbox != null) {
            mailboxId = mbox.getId();
            mbox.deleteMailbox();
        }
        
        String idString = (mbox == null) ?
            "<no mailbox for account " + accountId + ">" : Integer.toString(mailboxId);
        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
            new String[] {"cmd", "DeleteMailbox","id", idString}));
        
        Element response = zsc.createElement(AdminConstants.DELETE_MAILBOX_RESPONSE);
        if (mbox != null)
            response.addElement(AdminConstants.E_MAILBOX)
            .addAttribute(AdminConstants.A_MAILBOXID, mailboxId);
        return response;
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_deleteAccount);
    }
}
