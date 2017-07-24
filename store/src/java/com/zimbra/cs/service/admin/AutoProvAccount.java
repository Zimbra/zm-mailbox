/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.AutoProvAccountRequest;
import com.zimbra.soap.type.AutoProvPrincipalBy;

public class AutoProvAccount extends AdminDocumentHandler {

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

        AutoProvAccountRequest req = zsc.elementToJaxb(request);
        DomainBy domainBy = req.getDomain().getBy().toKeyDomainBy();
        String domainKey = req.getDomain().getKey();
        Domain domain = prov.get(domainBy, domainKey);
        if (domain == null) {
            throw AccountServiceException.NO_SUCH_DOMAIN(domainKey);
        }

        checkRight(zsc, context, domain, Admin.R_autoProvisionAccount);

        AutoProvPrincipalBy by = req.getPrincipal().getBy();
        String principal = req.getPrincipal().getKey();

        String password = req.getPassword();

        Account acct = prov.autoProvAccountManual(domain, by, principal, password);
        if (acct == null) {
            throw ServiceException.FAILURE("unable to auto provision account: " + principal, null);
        }

        Element response = zsc.createElement(AdminConstants.AUTO_PROV_ACCOUNT_RESPONSE);
        ToXML.encodeAccount(response, acct);
        return response;
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_autoProvisionAccount);
    }
}
