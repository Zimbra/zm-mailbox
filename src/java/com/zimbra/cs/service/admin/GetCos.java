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
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.CosBy;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class GetCos extends AdminDocumentHandler {

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
	    
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        Element d = request.getElement(AdminConstants.E_COS);
	    String key = d.getAttribute(AdminConstants.A_BY);
        String value = d.getText();

	    Cos cos = prov.get(CosBy.fromString(key), value);
	    
        if (cos == null)
            throw AccountServiceException.NO_SUCH_COS(value);

	    Element response = lc.createElement(AdminConstants.GET_COS_RESPONSE);
        doCos(response, cos);

	    return response;
	}

	public static void doCos(Element e, Cos c) throws ServiceException {
        Config config = Provisioning.getInstance().getConfig();
        Element cos = e.addElement(AdminConstants.E_COS);
        cos.addAttribute(AdminConstants.A_NAME, c.getName());
        cos.addAttribute(AdminConstants.E_ID, c.getId());
        Map attrs = c.getAttrs();
        AttributeManager attrMgr = AttributeManager.getInstance();
        for (Iterator mit=attrs.entrySet().iterator(); mit.hasNext(); ) {
            Map.Entry entry = (Entry) mit.next();
            String name = (String) entry.getKey();
            Object value = entry.getValue();
            boolean isCosAttr = !attrMgr.isAccountInherited(name);
            if (value instanceof String[]) {
                String sv[] = (String[]) value;
                for (int i = 0; i < sv.length; i++) {
                    Element attr = cos.addElement(AdminConstants.E_A);
                    attr.addAttribute(AdminConstants.A_N, name);
                    if (isCosAttr) 
                        attr.addAttribute(AdminConstants.A_C, "1");
                    attr.setText(sv[i]);
                }
            } else if (value instanceof String){
                Element attr = cos.addElement(AdminConstants.E_A);
                attr.addAttribute(AdminConstants.A_N, name);
                if (isCosAttr) 
                    attr.addAttribute(AdminConstants.A_C, "1");
                attr.setText((String) value);
            }
        }
    }
}
