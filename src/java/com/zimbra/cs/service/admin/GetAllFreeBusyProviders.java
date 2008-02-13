/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
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

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.fb.FreeBusyProvider;
import com.zimbra.soap.ZimbraSoapContext;

public class GetAllFreeBusyProviders extends AdminDocumentHandler {
	public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Element response = lc.createElement(AdminConstants.GET_ALL_FREE_BUSY_PROVIDERS_RESPONSE);
        
        for (FreeBusyProvider prov : FreeBusyProvider.getProviders()) {
            Element provElem = response.addElement(AdminConstants.E_PROVIDER);
            provElem.addAttribute(AdminConstants.A_NAME, prov.getName());
            provElem.addAttribute(AdminConstants.A_PROPAGATE, prov.registerForMailboxChanges());
            provElem.addAttribute(AdminConstants.A_START, prov.cachedFreeBusyStartTime());
            provElem.addAttribute(AdminConstants.A_END, prov.cachedFreeBusyEndTime());
        }
	    return response;
	}
}
