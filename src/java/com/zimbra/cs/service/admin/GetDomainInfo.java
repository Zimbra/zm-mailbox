/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AttributeFlag;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.soap.ZimbraSoapContext;

public class GetDomainInfo extends AdminDocumentHandler {
    
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        
        boolean applyConfig = request.getAttributeBool(AdminConstants.A_APPLY_CONFIG, true);
        Element d = request.getElement(AdminConstants.E_DOMAIN);
        String key = d.getAttribute(AdminConstants.A_BY);
        String value = d.getText();
        
        Domain domain = prov.get(DomainBy.fromString(key), value);

        /* 
        if (domain != null && !canAccessDomain(lc, domain))
            throw ServiceException.PERM_DENIED("can not access domain");
        */
        
        Element response = lc.createElement(AdminConstants.GET_DOMAIN_INFO_RESPONSE);
        if (domain != null)
            doDomain(response, domain, applyConfig);

        return response;
    }
    
    private void doDomain(Element e, Domain d, boolean applyConfig) throws ServiceException {
        Element domain = e.addElement(AdminConstants.E_DOMAIN);
        domain.addAttribute(AdminConstants.A_NAME,d.getUnicodeName());
        domain.addAttribute(AdminConstants.A_ID,d.getId());
        Set<String> attrList = AttributeManager.getInstance().getAttrsWithFlag(AttributeFlag.domainInfo);
        Map attrsMap = d.getUnicodeAttrs(applyConfig);
        
        for (String name : attrList) {
            Object value = attrsMap.get(name);
            
            if (value instanceof String[]) {
                String sv[] = (String[]) value;
                for (int i = 0; i < sv.length; i++)
                    domain.addElement(AdminConstants.E_A).addAttribute(AdminConstants.A_N, name).setText(sv[i]);
            } else if (value instanceof String)
                domain.addElement(AdminConstants.E_A).addAttribute(AdminConstants.A_N, name).setText((String) value);
        }
    }
    
    public boolean needsAuth(Map<String, Object> context) {
        return false;
    }

    public boolean needsAdminAuth(Map<String, Object> context) {
        return false;
    }
}
