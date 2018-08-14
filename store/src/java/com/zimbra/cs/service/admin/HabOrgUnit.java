/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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

import java.util.Map;
import java.util.Set;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.soap.ZimbraSoapContext;


/**
 * @author zimbra
 *
 */
public class HabOrgUnit extends AdminDocumentHandler {
    

    /* (non-Javadoc)
     * @<HabOrgUnit op=“create/delete/rename” name=“”>
     * <domain by=“name/id”></domain>
     * </HabOrgUnit>
     */
    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        Element d = request.getElement(AdminConstants.E_DOMAIN);
        String domainKey = d.getAttribute(AdminConstants.A_BY);
        String domainValue = d.getText();
        
        Domain domain = prov.get(Key.DomainBy.fromString(domainKey), domainValue);
        if (domain == null) {
            throw AccountServiceException.NO_SUCH_DOMAIN(domainValue);
        }
        checkDomainRight(zsc, domain, AdminRight.PR_ALWAYS_ALLOW);
        if (!domain.isLocal()) {
            throw ServiceException.INVALID_REQUEST("domain type must be local", null);
        }

        String habOrgUnitName = request.getAttribute(AdminConstants.A_NAME);
        if (StringUtil.isNullOrEmpty(habOrgUnitName) ) {
            throw ServiceException.INVALID_REQUEST(
                String.format("Hab Org unit name is required, requested name:%s",habOrgUnitName), null);
        }
        
        String operation = request.getAttribute(AdminConstants.A_OPERATION);
        if (StringUtil.isNullOrEmpty(operation) ) {
            throw ServiceException.INVALID_REQUEST(
                String.format("Operation is required, requested name:%s",operation), null);
        }
        
        ZLdapContext zlc = null;
        Element response = zsc.createElement(AdminConstants.HAB_ORG_UNIT_RESPONSE);
        
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.CREATE_OU);

            Set<String> habOrgUnitList = null;
            switch (operation) {
            case "create":
                habOrgUnitList = prov.createHabOrgUnit(domain, habOrgUnitName);
                break;
            case "rename":
                String newHabOrgUnitName = request.getAttribute("newName");
                if (StringUtil.isNullOrEmpty(habOrgUnitName)) {
                    throw ServiceException.INVALID_REQUEST(
                        String.format("New Hab Org unit name is required, requested rename:%s",
                            newHabOrgUnitName),
                        null);
                }
                habOrgUnitList = prov.renameHabOrgUnit(domain, habOrgUnitName, newHabOrgUnitName);
                break;
            case "delete":
                boolean forceDelete = request.getAttributeBool(AdminConstants.A_FORCE_DELETE, false);
                if (forceDelete) {
                    throw ServiceException.INVALID_REQUEST(
                        String.format("Force delete is not supported"), null);
                } else {
                    prov.deleteHabOrgUnit(domain, habOrgUnitName);
                }
                break;
            default:
                throw ServiceException.INVALID_REQUEST(
                    String.format("Invalid operatio name, requested name:%s", operation), null);
            }
           
            if (habOrgUnitList != null) {
                for (String habOrgUnit:  habOrgUnitList) {
                    response.addElement(AdminConstants.E_HAB_ORG_UNIT_NAME).setText(habOrgUnit);
                }
            }
        } finally {
            LdapClient.closeContext(zlc);
        }

        return response;
    }


    /**
     * 
     * @param ouName organizational unit name
     * @param baseDn distinguishedd name
     * @return the dn with ou
     */
    public static String createOuDn(String ouName, String baseDn) {
        StringBuilder sb = new StringBuilder();
        sb.append("ou=");
        sb.append(ouName);
        sb.append(",");
        sb.append(baseDn);
        
        return sb.toString();
    }

}
