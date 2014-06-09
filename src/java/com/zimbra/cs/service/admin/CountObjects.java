/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013 Zimbra Software, LLC.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.UCService;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.type.CountObjectsType;

public class CountObjects extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        String type = request.getAttribute(AdminConstants.A_TYPE);
        CountObjectsTypeWrapper typeWrapper;
        try {
            typeWrapper = CountObjectsTypeWrapper.valueOf(type);
        } catch (IllegalArgumentException ex) {
            throw ServiceException.INVALID_REQUEST("Invalid object type " + type,null);
        }

        if(typeWrapper == null) {
            throw ServiceException.INVALID_REQUEST("Invalid object type " + type,null);
        }

        UCService ucService = null;
        Element eUCService = request
                .getOptionalElement(AdminConstants.E_UC_SERVICE);
        if (eUCService != null) {
            if (!typeWrapper.allowsUCService()) {
                throw ServiceException.INVALID_REQUEST(
                        "UCService cannot be specified for type: "
                                + typeWrapper.name(), null);
            }

            String key = eUCService.getAttribute(AdminConstants.A_BY);
            String value = eUCService.getText();

            ucService = prov.get(Key.UCServiceBy.fromString(key), value);
            if (ucService == null) {
                throw AccountServiceException.NO_SUCH_UC_SERVICE(value);
            }
        }

        List<Pair<String, String>> domainList = new LinkedList<Pair<String, String>>();
        List<Element> domainElements = request.listElements(AdminConstants.E_DOMAIN);

        if(!typeWrapper.allowsDomain() && !domainElements.isEmpty()) {
            throw ServiceException.INVALID_REQUEST(
                    "domain cannot be specified for type: "
                            + typeWrapper.name(), null);
        }

        for (Element elem : domainElements) {
            domainList.add(new Pair<String, String>(elem
                    .getAttribute(AdminConstants.A_BY), elem.getText()));
        }
        long count = 0;
        if (domainList.isEmpty() && !zsc.getAuthToken().isAdmin()
                && typeWrapper.allowsDomain()
                && !typeWrapper.equals(CountObjectsTypeWrapper.domain)) {
            // if a delegated admin is trying to count objects that exist within
            // a domain, count only within this admin's domains
            List<Domain> domains = prov.getAllDomains();
            AdminAccessControl aac = AdminAccessControl
                    .getAdminAccessControl(zsc);
            for (Iterator<Domain> it = domains.iterator(); it.hasNext();) {
                Domain domain = it.next();
                if (aac.hasRight(domain, typeWrapper.getRight())) {
                    count += prov.countObjects(typeWrapper.getType(), domain,
                            ucService);
                }
            }
        } else if (!domainList.isEmpty() && typeWrapper.allowsDomain()) {
            // count objects within specified domains
            for (Pair<String, String> domainDef : domainList) {
                Domain domain = prov.get(
                        Key.DomainBy.fromString(domainDef.getFirst()),
                        domainDef.getSecond());
                if (domain == null) {
                    throw AccountServiceException.NO_SUCH_DOMAIN(domainDef
                            .getSecond());
                }
                checkDomainRight(zsc, domain, typeWrapper.getRight());
                count += prov.countObjects(typeWrapper.getType(), domain,
                        ucService);
            }
        } else {
            // count objects globally
            this.checkRight(zsc, context, null, typeWrapper.getRight());
            count += prov.countObjects(typeWrapper.getType(), null, ucService);
        }

        Element response = zsc
                .createElement(AdminConstants.COUNT_OBJECTS_RESPONSE);
        response.addAttribute(AdminConstants.A_NUM, count);
        response.addAttribute(AdminConstants.A_TYPE, type);
        return response;
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_countAccount);
        relatedRights.add(Admin.R_countAlias);
        relatedRights.add(Admin.R_countDomain);
        relatedRights.add(Admin.R_countDistributionList);
        relatedRights.add(Admin.R_countCos);
        relatedRights.add(Admin.R_countServer);
        relatedRights.add(Admin.R_countCalendarResource);
    }

    private enum CountObjectsTypeWrapper {
        userAccount(CountObjectsType.userAccount, Admin.R_countAccount), account(
                CountObjectsType.account, Admin.R_countAccount), alias(
                CountObjectsType.alias, Admin.R_countAlias), dl(
                CountObjectsType.dl, Admin.R_countDistributionList), domain(
                CountObjectsType.domain, Admin.R_countDomain), cos(
                CountObjectsType.cos, Admin.R_countCos), server(
                CountObjectsType.server, Admin.R_countServer), calresource(
                CountObjectsType.calresource, Admin.R_countCalendarResource),

        // UC service objects
        accountOnUCService(CountObjectsType.accountOnUCService,
                Admin.R_countAccount), cosOnUCService(
                CountObjectsType.cosOnUCService, Admin.R_countCos), domainOnUCService(
                CountObjectsType.domainOnUCService, Admin.R_countDomain),

        // for license counting
        internalUserAccount(CountObjectsType.internalUserAccount,
                Admin.R_countAccount), internalArchivingAccount(
                CountObjectsType.internalArchivingAccount, Admin.R_countAccount);

        private CountObjectsType type;
        private AdminRight right;

        public CountObjectsType getType() {
            return type;
        }

        public AdminRight getRight() {
            return right;
        }

        public boolean allowsDomain() {
            return type.allowsDomain();
        }

        public boolean allowsUCService() {
            return type.allowsUCService();
        }

        CountObjectsTypeWrapper(CountObjectsType type, AdminRight right) {
            this.type = type;
            this.right = right;
        }
    }
}
