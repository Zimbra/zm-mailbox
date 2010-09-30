/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AttributeFlag;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.soap.ZimbraSoapContext;

public class GetDomainInfo extends AdminDocumentHandler {
    
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        
        boolean applyConfig = request.getAttributeBool(AdminConstants.A_APPLY_CONFIG, true);
        Element d = request.getElement(AdminConstants.E_DOMAIN);
        String key = d.getAttribute(AdminConstants.A_BY);
        String value = d.getText();
        
        DomainBy domainBy = DomainBy.fromString(key);
        Domain domain = prov.getDomain(domainBy, value, true);

        Element response = lc.createElement(AdminConstants.GET_DOMAIN_INFO_RESPONSE);
        
        if (domain == null && domainBy != DomainBy.name && domainBy != DomainBy.virtualHostname) {
            // domain not found, and we don't have info for walking up sub domains
            // return attributes on global config 
            toXML(response, prov.getConfig(), applyConfig);
        } else {
            /*
             * for all the attrs we can return (like login/logout URL), start stripping off 
             * subdomains and checking the parent domain's settings. i.e., if a.b.com lookup fails, 
             * try "b.com" and see if it has any settings defined; ultimately falling back to global.
             * 
             * see if we can still find a domain.  We do this by
             * 
             * 1. if by virtualHostname, see if we can find a domain by the name.  If we can, use that doamin.
             * 
             * 2. if get(DomainBy.name) returns null using the supplied value, we walk up the sub-domains
             *    and see if we can find a domain by the name.  
             *    e.g  if x.y.z.com was passed in, we check y.z.com, then z.com.
             * 
             * 3. If still no domain found, return attributes on global config 
             * 
             */
            
            if (domain == null) {
                if (domainBy == DomainBy.virtualHostname)
                    domain = prov.getDomain(DomainBy.name, value, true);
                
                if (domain == null)
                    domain = findDomain(prov, value);
            }
            
            if (domain != null)
                toXML(response, domain, applyConfig);
            else
                toXML(response, prov.getConfig(), applyConfig);
        }

        return response;
    }
    
    private void toXML(Element e, Entry entry, boolean applyConfig) throws ServiceException {
        Element domain = e.addElement(AdminConstants.E_DOMAIN);
        if (entry instanceof Domain) {
            Domain d = (Domain)entry;
            domain.addAttribute(AdminConstants.A_NAME, d.getUnicodeName());
            domain.addAttribute(AdminConstants.A_ID, d.getId());
        } else {
            // weird, need to populate name and id because client expects them to construct a Domain object (but don't really use it)
            domain.addAttribute(AdminConstants.A_NAME, "globalconfig");
            domain.addAttribute(AdminConstants.A_ID, "globalconfig-dummy-id");
        }
        
        Set<String> attrList = AttributeManager.getInstance().getAttrsWithFlag(AttributeFlag.domainInfo);
        Map attrsMap = entry.getUnicodeAttrs(applyConfig);
        
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

    private static Domain findDomain(Provisioning prov, String value) throws ServiceException {
        Domain domain = null;
        
        int firstDotAt = value.indexOf('.');
        int secondDotAt = firstDotAt == -1? -1 : value.indexOf('.', firstDotAt+1);
        
        // will do the get only if the remaining has at least two segments.
        // e.g will do z.com
        //     will not do com
        while (secondDotAt != -1) {
            // System.out.println(value.substring(firstDotAt+1));
            domain = prov.getDomain(DomainBy.name, value.substring(firstDotAt+1), true);
            if (domain != null)
                break;
            else {
                firstDotAt = secondDotAt;
                secondDotAt = value.indexOf('.', firstDotAt+1);
            }
        }
        
        return domain;
    }
    
    public boolean needsAuth(Map<String, Object> context) {
        return false;
    }

    public boolean needsAdminAuth(Map<String, Object> context) {
        return false;
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add(AdminRightCheckPoint.Notes.ALLOW_ALL_ADMINS);
    }
    
    public static void main(String args[]) throws ServiceException {
        // findDomain(Provisioning.getInstance(), "x");  System.out.println();
        // findDomain(Provisioning.getInstance(), "x.y");  System.out.println();
        // findDomain(Provisioning.getInstance(), "x.y.z");  System.out.println();
        findDomain(Provisioning.getInstance(), "x.y.z.a.b.c");  System.out.println();

    }
}
