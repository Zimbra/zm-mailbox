/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2015, 2016, 2020 Synacor, Inc.
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
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.listeners.AccountListener;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.RenameAccountRequest;

/**
 * @author schemers
 */
public class RenameAccount extends AdminDocumentHandler {

    private static final String[] TARGET_ACCOUNT_PATH = new String[] { AdminConstants.E_ID };
    @Override
    protected String[] getProxiedAccountPath()  { return TARGET_ACCOUNT_PATH; }

    /**
     * must be careful and only allow renames from/to domains a domain admin can see
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
        Provisioning prov = Provisioning.getInstance();

        RenameAccountRequest req = zsc.elementToJaxb(request);
        String id = req.getId();
        String newName = req.getNewName();
        boolean rollbackOnFailure = LC.rollback_on_account_listener_failure.booleanValue();

        Account account = prov.get(AccountBy.id, id, zsc.getAuthToken());
        defendAgainstAccountHarvesting(account, AccountBy.id, id, zsc, Admin.R_renameAccount);

        String oldName = account.getName();

        // check if the admin can rename the account
        checkAccountRight(zsc, account, Admin.R_renameAccount);

        // check if the admin can "create account" in the domain (can be same or diff)
        checkDomainRightByEmail(zsc, newName, Admin.R_createAccount);

        Mailbox mbox = Provisioning.onLocalServer(account) ? MailboxManager.getInstance().getMailboxByAccount(account) : null;
        try {
            AccountListener.invokeOnAccountRename(account, oldName, newName, zsc,
                rollbackOnFailure);
        } catch (ServiceException se) {
            ZimbraLog.account.error(se.getMessage());
            throw se;
        }

        try {
            prov.renameAccount(id, newName);
            if (mbox != null) {
                mbox.renameMailbox(oldName, newName);
            }
        } catch (ServiceException se) {
            if (rollbackOnFailure) {
                ZimbraLog.account.debug(
                    "Exception occured while renaming account in zimbra for %s, roll back listener updates.",
                    account.getMail());
                // roll back rename updates
                try {
                    AccountListener.invokeOnAccountRename(account, newName, oldName, zsc,
                        rollbackOnFailure);
                } catch (ServiceException sse) {
                    ZimbraLog.account.error(sse.getMessage());
                    throw sse;
                }
            } else {
                ZimbraLog.account.warn(
                    "No rollback on account listener for zimbra rename account failure, there may be inconsistency in account. %s",
                    se.getMessage());
            }
            throw se;
        }
        
        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "RenameAccount","name", oldName, "newName", newName}));

        // get again with new name...
        account = prov.get(AccountBy.id, id, true, zsc.getAuthToken());
        if (account == null) {
            throw ServiceException.FAILURE("unable to get account after rename: " + id, null);
        }
        Element response = zsc.createElement(AdminConstants.RENAME_ACCOUNT_RESPONSE);
        ToXML.encodeAccount(response, account);
        return response;
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_renameAccount);
        relatedRights.add(Admin.R_createAccount);
    }
}
