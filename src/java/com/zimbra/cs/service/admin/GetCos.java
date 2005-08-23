/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.  Portions
 * created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
 * Reserved.
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

import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.ZimbraContext;

/**
 * @author schemers
 */
public class GetCos extends AdminDocumentHandler {

    public static final String BY_NAME = "name";
    public static final String BY_ID = "id";
    
	public Element handle(Element request, Map context) throws ServiceException {
	    
        ZimbraContext lc = getZimbraContext(context);
        Provisioning prov = Provisioning.getInstance();

        Element d = request.getElement(AdminService.E_COS);
	    String key = d.getAttribute(AdminService.A_BY);
        String value = d.getText();

	    Cos cos = null;

        if (key.equals(BY_NAME)) {
            cos = prov.getCosByName(value);
        } else if (key.equals(BY_ID)) {
            cos = prov.getCosById(value);
        } else {
            throw ServiceException.INVALID_REQUEST("unknown value for by: "+key, null);
        }
	    
        if (cos == null)
            throw AccountServiceException.NO_SUCH_COS(value);

	    Element response = lc.createElement(AdminService.GET_COS_RESPONSE);
        doCos(response, cos);

	    return response;
	}

	public static void doCos(Element e, Cos c) throws ServiceException {
        Config config = Provisioning.getInstance().getConfig();
        Element cos = e.addElement(AdminService.E_COS);
        cos.addAttribute(AdminService.A_NAME, c.getName());
        cos.addAttribute(AdminService.E_ID, c.getId());
        Map attrs = c.getAttrs();
        for (Iterator mit=attrs.entrySet().iterator(); mit.hasNext(); ) {
            Map.Entry entry = (Entry) mit.next();
            String name = (String) entry.getKey();
            Object value = entry.getValue();
            boolean isCosAttr = !config.isInheritedAccountAttr(name);
            if (value instanceof String[]) {
                String sv[] = (String[]) value;
                for (int i = 0; i < sv.length; i++) {
                    Element attr = cos.addElement(AdminService.E_A);
                    attr.addAttribute(AdminService.A_N, name);
                    if (isCosAttr) 
                        attr.addAttribute(AdminService.A_C, "1");
                    attr.setText(sv[i]);
                }
            } else if (value instanceof String){
                Element attr = cos.addElement(AdminService.E_A);
                attr.addAttribute(AdminService.A_N, name);
                if (isCosAttr) 
                    attr.addAttribute(AdminService.A_C, "1");                
                attr.setText((String) value);
            }
        }
    }
}
