/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.cs.account.Zimlet;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class GetZimlet extends AdminDocumentHandler {

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        Element z = request.getElement(AdminConstants.E_ZIMLET);
	    String n = z.getAttribute(AdminConstants.A_NAME);

	    Zimlet zimlet = prov.getZimlet(n);

        if (zimlet == null)
            throw AccountServiceException.NO_SUCH_ZIMLET(n);

	    Element response = lc.createElement(AdminConstants.GET_ZIMLET_RESPONSE);
	    doZimlet(response, zimlet);
	    
	    return response;
	}
	
	static Element doZimlet(Element response, Zimlet zimlet) throws ServiceException {
	    Map<String,Object> attrs = zimlet.getUnicodeAttrs();
	    
        Element zim = response.addElement(AdminConstants.E_ZIMLET);
    	zim.addAttribute(AdminConstants.A_NAME, zimlet.getName());
    	zim.addAttribute(AdminConstants.A_ID, zimlet.getId());
        for (Map.Entry<String, Object> entry : attrs.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String[]) {
                String zv[] = (String[]) value;
                for (int i = 0; i < zv.length; i++)
                	zim.addElement(AdminConstants.E_A).addAttribute(AdminConstants.A_N, name).setText(zv[i]);
            } else if (value instanceof String)
                zim.addElement(AdminConstants.E_A).addAttribute(AdminConstants.A_N, name).setText((String) value);
        }
        return zim;
	}
}
