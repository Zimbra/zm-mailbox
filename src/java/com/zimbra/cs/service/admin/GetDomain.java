/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.soap.ZimbraSoapContext;

import java.util.Iterator;
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
	    
        ZimbraSoapContext lc = getZimbraSoapContext(context);
	    Provisioning prov = Provisioning.getInstance();
	    
        boolean applyConfig = request.getAttributeBool(AdminConstants.A_APPLY_CONFIG, true);
        Element d = request.getElement(AdminConstants.E_DOMAIN);
	    String key = d.getAttribute(AdminConstants.A_BY);
        String value = d.getText();
	    
	    Domain domain = prov.get(DomainBy.fromString(key), value);

        if (domain == null)
            throw AccountServiceException.NO_SUCH_DOMAIN(value);

        if (isDomainAdminOnly(lc) && !canAccessDomain(lc, domain))
            throw ServiceException.PERM_DENIED("can not access domain");

        Element response = lc.createElement(AdminConstants.GET_DOMAIN_RESPONSE);
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
            
            if (!applyConfig && (reqAttrs != null && !reqAttrs.contains(name)))
                continue;
            
            if (value instanceof String[]) {
                String sv[] = (String[]) value;
                for (int i = 0; i < sv.length; i++)
                    domain.addElement(AdminConstants.E_A).addAttribute(AdminConstants.A_N, name).setText(sv[i]);
            } else if (value instanceof String)
                domain.addElement(AdminConstants.E_A).addAttribute(AdminConstants.A_N, name).setText((String) value);
        }
    }
}
