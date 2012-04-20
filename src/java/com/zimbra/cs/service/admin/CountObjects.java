/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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

import java.util.Map;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.UCService;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.type.CountObjectsType;

public class CountObjects extends AdminDocumentHandler {
    
    public Element handle(Element request, Map<String, Object> context) 
    throws ServiceException {
        
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        
        CountObjectsType type = CountObjectsType.fromString(
                request.getAttribute(AdminConstants.A_TYPE));
        
        Domain domain = null;
        Element eDomain = request.getOptionalElement(AdminConstants.E_DOMAIN);
        if (eDomain != null) {
            if (!type.allowsDomain()) {
                throw ServiceException.INVALID_REQUEST("domain cannot be specified for type: " + type.name(), null);
            }
            
            String key = eDomain.getAttribute(AdminConstants.A_BY);
            String value = eDomain.getText();
                
            domain = prov.get(Key.DomainBy.fromString(key), value);
            if (domain == null) {
                throw AccountServiceException.NO_SUCH_DOMAIN(value);
            }
        }
        
        UCService ucService = null;
        Element eUCService = request.getOptionalElement(AdminConstants.E_UC_SERVICE);
        if (eUCService != null) {
            if (!type.allowsUCService()) {
                throw ServiceException.INVALID_REQUEST("UCService cannot be specified for type: " + type.name(), null);
            }
            
            String key = eUCService.getAttribute(AdminConstants.A_BY);
            String value = eUCService.getText();
                
            ucService = prov.get(Key.UCServiceBy.fromString(key), value);
            if (ucService == null) {
                throw AccountServiceException.NO_SUCH_UC_SERVICE(value);
            }
        }
        
        long count = prov.countObjects(type, domain, ucService);

        Element response = zsc.createElement(AdminConstants.COUNT_OBJECTS_RESPONSE);
        response.addAttribute(AdminConstants.A_NUM, count);
        return response;
    }
    
}
