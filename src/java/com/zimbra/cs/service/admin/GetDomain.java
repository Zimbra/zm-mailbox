/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import com.zimbra.common.calendar.TZIDMapper;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.soap.ZimbraSoapContext;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
        Element d = request.getElement(AdminConstants.E_DOMAIN);
	    String key = d.getAttribute(AdminConstants.A_BY);
        String value = d.getText();
	    
	    Domain domain = prov.get(DomainBy.fromString(key), value);

        if (domain == null)
            throw AccountServiceException.NO_SUCH_DOMAIN(value);

        checkDomainRight(zsc, domain, Admin.R_getDomain);

        Element response = zsc.createElement(AdminConstants.GET_DOMAIN_RESPONSE);
        doDomain(response, domain, applyConfig);

	    return response;
	}

    public static void doDomain(Element e, Domain d) throws ServiceException {
        doDomain(e, d, true);
    }
    
    public static void doDomain(Element e, Domain d, boolean applyConfig) throws ServiceException {
        doDomain(e, d, applyConfig, null);
    }
    
    public static void doDomain(Element e, Domain d, boolean applyConfig, Set<String> reqAttrs) throws ServiceException {
        Element domain = e.addElement(AdminConstants.E_DOMAIN);
        domain.addAttribute(AdminConstants.A_NAME,d.getUnicodeName());
        domain.addAttribute(AdminConstants.A_ID,d.getId());
        Map attrs = d.getUnicodeAttrs(applyConfig);
        for (Iterator mit = attrs.entrySet().iterator(); mit.hasNext(); ) {
            Map.Entry entry = (Entry) mit.next();
            String name = (String) entry.getKey();
            Object value = entry.getValue();
            
            if (reqAttrs != null && !reqAttrs.contains(name))
                continue;
            
            if (value instanceof String[]) {
                String sv[] = (String[]) value;
                for (int i = 0; i < sv.length; i++)
                    domain.addElement(AdminConstants.E_A).addAttribute(AdminConstants.A_N, name).setText(sv[i]);
            } else if (value instanceof String) {
                // Fixup for time zone id.  Always use canonical (Olson ZoneInfo) ID.
                if (name.equals(Provisioning.A_zimbraPrefTimeZoneId))
                    value = TZIDMapper.canonicalize((String) value);
                domain.addElement(AdminConstants.E_A).addAttribute(AdminConstants.A_N, name).setText((String) value);
            }
        }
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_getDomain);
    }
}
