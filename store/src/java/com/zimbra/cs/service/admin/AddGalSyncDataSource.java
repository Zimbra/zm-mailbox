/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.AddGalSyncDataSourceRequest;
import com.zimbra.soap.admin.type.GalMode;
import com.zimbra.soap.type.AccountBy;
import com.zimbra.soap.type.AccountSelector;

public class AddGalSyncDataSource extends AdminDocumentHandler {

    @Override
    public boolean domainAuthSufficient(Map<String, Object> context) {
        return true;
    }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        AddGalSyncDataSourceRequest dsRequest = zsc.elementToJaxb(request);

        String name = dsRequest.getName();
        String domainStr = dsRequest.getDomain();
        GalMode type = dsRequest.getType();

        AccountSelector acctSelector = dsRequest.getAccount();
        AccountBy acctBy = acctSelector.getBy();
        String acctValue = acctSelector.getKey();
        String folder = dsRequest.getFolder();

        Domain domain = prov.getDomainByName(domainStr);

        if (domain == null) {
            throw AccountServiceException.NO_SUCH_DOMAIN(domainStr);
        }

        Account account = null;
        try {
            account = prov.get(acctBy.toKeyAccountBy(), acctValue, zsc.getAuthToken());
        } catch (ServiceException se) {
            ZimbraLog.gal.warn("error checking GalSyncAccount", se);
        }

        if (account == null) {
            throw AccountServiceException.NO_SUCH_ACCOUNT(acctValue);
        }

        if (!Provisioning.onLocalServer(account)) {
            return proxyRequest(request, context, account.getId());
        }

        CreateGalSyncAccount.addDataSource(request, zsc, account, domain, folder, name, type);

        Element response = zsc.createElement(AdminConstants.ADD_GAL_SYNC_DATASOURCE_RESPONSE);
        ToXML.encodeAccount(response, account, false, emptySet, null);
        return response;
    }

    private static final Set<String> emptySet = Collections.emptySet();

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        // XXX revisit
        relatedRights.add(Admin.R_createAccount);
        notes.add(String.format(AdminRightCheckPoint.Notes.MODIFY_ENTRY,
                Admin.R_modifyAccount.getName(), "account"));
    }
}
