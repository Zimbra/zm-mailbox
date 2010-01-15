/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
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

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class GetConfig extends AdminDocumentHandler {
    
	public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        Element a = request.getElement(AdminConstants.E_A);
        String name = a.getAttribute(AdminConstants.A_N);

        String value[] = prov.getConfig().getMultiAttr(name);

        Element response = lc.createElement(AdminConstants.GET_CONFIG_RESPONSE);
        doConfig(response, name, value);

        return response;
	}

	public static void doConfig(Element e, String name, String[] value) {
        if (value == null)
            return;
        for (int i = 0; i < value.length; i++)
            e.addElement(AdminConstants.E_A).addAttribute(AdminConstants.A_N, name).setText(value[i]);
    }

	public static void doConfig(Element e, String name, String value) {
        if (value == null)
            return;
        e.addElement(AdminConstants.E_A).addAttribute(AdminConstants.A_N, name).setText(value);
    }
}
