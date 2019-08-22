/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.ZAttrProvisioning.AccountStatus;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.listeners.AccountListener;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.DeleteAccountRequest;
import com.zimbra.soap.admin.message.DeleteAccountResponse;

/**
 * @author schemers
 */
public class DeleteAccount extends AdminDocumentHandler {

    private static final String[] TARGET_ACCOUNT_PATH = new String[] { AdminConstants.E_ID };
    @Override
    protected String[] getProxiedAccountPath()  { return TARGET_ACCOUNT_PATH; }

    /**
     * must be careful and only allow deletes domain admin has access to
     */
    @Override
    public boolean domainAuthSufficient(Map context) {
        return true;
    }

    /**
     * @return true - which means accept responsibility for measures to prevent account harvesting by delegate admins
     */
    @Override
    public boolean defendsAgainstDelegateAdminAccountHarvesting() {
        return true;
    }

    /**
     * Deletes an account and its mailbox.
     */
    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        DeleteAccountRequest req = zsc.elementToJaxb(request);
        String id = req.getId();
        if (null == id) {
            throw ServiceException.INVALID_REQUEST("missing required attribute: " + AdminConstants.E_ID, null);
        }
        // Confirm that the account exists and that the mailbox is located on the current host
        Account account = prov.get(AccountBy.id, id, zsc.getAuthToken());
        defendAgainstAccountHarvesting(account, AccountBy.id, id, zsc, Admin.R_deleteAccount);
        try {
            AccountListener.invokeBeforeAccountDeletion(account, zsc);
        } catch (ServiceException e) {
            throw ServiceException.FAILURE(
                "Failed to delete account '" + account.getName() + "' in AccountListener", e);
        }
        /*
         * bug 69009
         *
         * We delete the mailbox before deleting the LDAP entry.
         * It's possible that a message delivery or other user action could
         * cause the mailbox to be recreated between the mailbox delete step
         * and the LDAP delete step.
         *
         * To prevent this race condition, put the account in "maintenance" mode
         * so mail delivery and any user action is blocked.
         */
        prov.modifyAccountStatus(account, AccountStatus.maintenance.name());

        Mailbox mbox = Provisioning.onLocalServer(account) ?
                MailboxManager.getInstance().getMailboxByAccount(account, false) : null;
        if (mbox != null) {
            mbox.deleteMailbox();
        }

        prov.deleteAccount(id);

        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
            new String[] {"cmd", "DeleteAccount","name", account.getName(), "id", account.getId()}));
        return zsc.jaxbToElement(new DeleteAccountResponse());
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_deleteAccount);
    }
}
