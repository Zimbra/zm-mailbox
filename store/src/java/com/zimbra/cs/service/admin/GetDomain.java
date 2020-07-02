/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.AccessManager.AttrRightChecker;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.soap.ZimbraSoapContext;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author schemers
 */
public class GetDomain extends AdminDocumentHandler {

    /**
     * must be careful and only return accounts a domain admin can see
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        boolean applyConfig = request.getAttributeBool(AdminConstants.A_APPLY_CONFIG, true);
        Set<String> reqAttrs = getReqAttrs(request, AttributeClass.domain);
        
        Element d = request.getElement(AdminConstants.E_DOMAIN);
        String key = d.getAttribute(AdminConstants.A_BY);
        String value = d.getText();

        Domain domain = prov.get(Key.DomainBy.fromString(key), value);

        if (domain == null)
            throw AccountServiceException.NO_SUCH_DOMAIN(value);

        AdminAccessControl aac = checkDomainRight(zsc, domain, AdminRight.PR_ALWAYS_ALLOW);

        Element response = zsc.createElement(AdminConstants.GET_DOMAIN_RESPONSE);
        encodeDomain(response, domain, applyConfig, reqAttrs, aac.getAttrRightChecker(domain));

        return response;
    }

    public static void encodeDomain(Element e, Domain d) throws ServiceException {
        encodeDomain(e, d, true);
    }
    
    public static void encodeDomain(Element e, Domain d, boolean applyConfig) throws ServiceException {
        encodeDomain(e, d, applyConfig, null, null);
    }
    
    public static void encodeDomain(Element e, Domain d, boolean applyConfig, Set<String> reqAttrs, 
            AttrRightChecker attrRightChecker) throws ServiceException {
        Element domain = e.addElement(AdminConstants.E_DOMAIN);
        domain.addAttribute(AdminConstants.A_NAME,d.getUnicodeName());
        domain.addAttribute(AdminConstants.A_ID,d.getId());
        Map attrs = d.getUnicodeAttrs(applyConfig);
        Object value = attrs.get(Provisioning.A_zimbraAuthLdapURL);
		if (value != null && value instanceof String[]) {
			String sv[] = (String[]) value;
			String joinedValue = String.join(" ", sv);
			attrs.put(Provisioning.A_zimbraAuthLdapURL, joinedValue);
		}
        ToXML.encodeAttrs(domain, attrs, reqAttrs, attrRightChecker);
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_getDomain);
        notes.add(String.format(AdminRightCheckPoint.Notes.GET_ENTRY, Admin.R_getDomain.getName()));
    }
}
