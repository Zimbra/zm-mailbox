/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraContext;

/**
 * @author schemers
 */
public class GetDomain extends AdminDocumentHandler {

    public static final String BY_NAME = "name";
    public static final String BY_ID = "id";
    
	public Element handle(Element request, Map context) throws ServiceException {
	    
        ZimbraContext lc = getZimbraContext(context);
	    Provisioning prov = Provisioning.getInstance();
	    
        boolean applyConfig = request.getAttributeBool(AdminService.A_APPLY_CONFIG, true);
        Element d = request.getElement(AdminService.E_DOMAIN);
	    String key = d.getAttribute(AdminService.A_BY);
        String value = d.getText();
	    
	    Domain domain = null;
        
        if (key.equals(BY_NAME)) {
            domain = prov.getDomainByName(value);
        } else if (key.equals(BY_ID)) {
            domain = prov.getDomainById(value);
        } else {
            throw ServiceException.INVALID_REQUEST("unknown value for by: "+key, null);
        }
	    
        if (domain == null)
            throw AccountServiceException.NO_SUCH_DOMAIN(value);

	    Element response = lc.createElement(AdminService.GET_DOMAIN_RESPONSE);
        doDomain(response, domain, applyConfig);

	    return response;
	}

    public static void doDomain(Element e, Domain d) throws ServiceException {
        doDomain(e, d, true);
    }
    
    public static void doDomain(Element e, Domain d, boolean applyConfig) throws ServiceException {
        Element domain = e.addElement(AdminService.E_DOMAIN);
        domain.addAttribute(AdminService.A_NAME,d.getName());
        domain.addAttribute(AdminService.A_ID,d.getId());
        Map attrs = d.getAttrs(applyConfig);
        for (Iterator mit = attrs.entrySet().iterator(); mit.hasNext(); ) {
            Map.Entry entry = (Entry) mit.next();
            String name = (String) entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String[]) {
                String sv[] = (String[]) value;
                for (int i = 0; i < sv.length; i++)
                    domain.addAttribute(name, sv[i], Element.DISP_ELEMENT);
            } else if (value instanceof String)
                domain.addAttribute(name, (String) value, Element.DISP_ELEMENT);
        }
    }
}
