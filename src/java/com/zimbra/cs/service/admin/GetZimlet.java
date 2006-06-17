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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.account.Zimlet;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class GetZimlet extends AdminDocumentHandler {

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        Element z = request.getElement(AdminService.E_ZIMLET);
	    String n = z.getAttribute(AdminService.A_NAME);

	    Zimlet zimlet = prov.getZimlet(n);

        if (zimlet == null)
            throw AccountServiceException.NO_SUCH_ZIMLET(n);

	    Element response = lc.createElement(AdminService.GET_ZIMLET_RESPONSE);
	    doZimlet(response, zimlet);
	    
	    return response;
	}
	
	static Element doZimlet(Element response, Zimlet zimlet) throws ServiceException {
	    Map<String,Object> attrs = zimlet.getAttrs();
	    
        Element zim = response.addElement(AdminService.E_ZIMLET);
    	zim.addAttribute(AdminService.A_N, zimlet.getName());
    	zim.addAttribute(AdminService.A_ID, zimlet.getId());
        for (Map.Entry<String, Object> entry : attrs.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String[]) {
                String zv[] = (String[]) value;
                for (int i = 0; i < zv.length; i++)
                	zim.addElement(AdminService.E_A).addAttribute(AdminService.A_N, name).setText(zv[i]);
            } else if (value instanceof String)
                zim.addElement(AdminService.E_A).addAttribute(AdminService.A_N, name).setText((String) value);
        }
        return zim;
	}
}
