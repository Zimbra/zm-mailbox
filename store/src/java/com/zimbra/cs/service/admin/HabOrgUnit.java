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

import org.apache.commons.lang3.ArrayUtils;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.ldap.LdapObjectClass;
import com.zimbra.cs.account.ldap.entry.LdapEntry;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.ldap.ZLdapFilterFactory.FilterId;
import com.zimbra.cs.ldap.ZSearchControls;
import com.zimbra.cs.ldap.ZSearchResultEnumeration;
import com.zimbra.cs.ldap.ZSearchScope;
import com.zimbra.cs.service.account.ToXML;
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
        String domainDn = ((LdapEntry)domain).getDN();
        
        
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

            ZAttributes ouAttrs = null;
            switch (operation) {
            case "create":
                
                String[] objClass = ArrayUtils.toStringArray(LdapObjectClass.getOrganizationUnitObjectClasses().toArray());
                String[] ldapAttrs = { Provisioning.A_ou, habOrgUnitName };
                zlc.createEntry(createOuDn(habOrgUnitName, domainDn), objClass, ldapAttrs);
                ouAttrs = zlc.getAttributes(createOuDn(habOrgUnitName, domainDn), null);
                break;
            case "rename":
                String newHabOrgUnitName = request.getAttribute("newName");
                if (StringUtil.isNullOrEmpty(habOrgUnitName)) {
                    throw ServiceException.INVALID_REQUEST(
                        String.format("New Hab Org unit name is required, requested rename:%s",
                            newHabOrgUnitName),
                        null);
                }
                zlc.renameEntry(createOuDn(habOrgUnitName, domainDn),
                    createOuDn(newHabOrgUnitName, domainDn));
                ouAttrs = zlc.getAttributes(createOuDn(newHabOrgUnitName, domainDn), null);
                break;
            case "delete":
                if (isEmptyOu(habOrgUnitName, domainDn, zlc)) {
                    zlc.deleteEntry(createOuDn(habOrgUnitName, domainDn));
                } else {
                    throw ServiceException.FAILURE(String.format("HabOrgUnit: %s"
                       + " of doamin:%s  is not empty", habOrgUnitName, domainDn) , null);
                }
                break;
            default:
                throw ServiceException.INVALID_REQUEST(
                    String.format("Invalid operatio name, requested name:%s", operation), null);
            }
           
            if (ouAttrs != null) {
                Map<String, Object> attrMap = ouAttrs.getAttrs();
                for (String key:  attrMap.keySet()) {
                    ToXML.encodeAttr(response, key,attrMap.get(key));
                }
            }
        } finally {
            LdapClient.closeContext(zlc);
        }

        return response;
    }
    
    /**
     * @param habOrgUnitName  organizational unit name
     * @param domainDn the domain distinguishedd name
     * @param zlc ldap context
     * @return true if the ou has groups or false if empty
     */
    private static boolean isEmptyOu(String habOrgUnitName, String domainDn, ZLdapContext zlc) throws LdapException{
        String baseDN = createOuDn(habOrgUnitName, domainDn);
        String filter = "(objectClass=zimbraDistributionList)";
        String returnAttrs[] = new String[]{"cn"};
        ZLdapFilter zFilter = ZLdapFilterFactory.getInstance().fromFilterString(FilterId.ALL_DISTRIBUTION_LISTS, filter);
        
        ZSearchControls searchControls = ZSearchControls.createSearchControls(
                ZSearchScope.SEARCH_SCOPE_SUBTREE, ZSearchControls.SIZE_UNLIMITED, 
                returnAttrs);
        
        ZSearchResultEnumeration ne = zlc.searchDir(baseDN, zFilter, searchControls);
        return !ne.hasMore();
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
