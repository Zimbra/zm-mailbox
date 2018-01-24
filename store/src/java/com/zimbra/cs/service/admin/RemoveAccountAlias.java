/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2015, 2016 Synacor, Inc.
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
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.RemoveAccountAliasRequest;
import com.zimbra.soap.admin.message.RemoveAccountAliasResponse;

/**
 * @author schemers
 */
public class RemoveAccountAlias extends AdminDocumentHandler {

    private static final String[] TARGET_ACCOUNT_PATH = new String[] { AdminConstants.E_ID };
    @Override
    protected String[] getProxiedAccountPath()  { return TARGET_ACCOUNT_PATH; }

    /**
     * must be careful and only allow access to domain if domain admin
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

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        RemoveAccountAliasRequest req = zsc.elementToJaxb(request);
        String id = req.getId();
        String alias = req.getAlias();

        Account account = null;
        if (id != null) {
            account = prov.get(AccountBy.id, id, zsc.getAuthToken());
        }

        try {
            defendAgainstAccountOrCalendarResourceHarvesting(account, AccountBy.id, id, zsc,
                    Admin.R_removeAccountAlias, Admin.R_removeCalendarResourceAlias);
        } catch (AccountServiceException ase) {
            // still may want to remove the alias, even if it doesn't point at anything
            // note: if we got a permission denied instead of AccountServiceException,
            //       means we don't have the rights so shouldn't get any further
        }

        String acctName = "";
        if (account != null) {
            acctName = account.getName();
        }

        // if the admin can remove an alias in the domain
        checkDomainRightByEmail(zsc, alias, Admin.R_deleteAlias);

        // If account is null, still invoke removeAlias. Ensures dangling aliases are cleaned up as much as possible
        prov.removeAlias(account, alias);

        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "RemoveAccountAlias","name", acctName, "alias", alias}));

        return zsc.jaxbToElement(new RemoveAccountAliasResponse());
    }


    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_removeAccountAlias);
        relatedRights.add(Admin.R_removeCalendarResourceAlias);
        relatedRights.add(Admin.R_deleteAlias);
    }
}
