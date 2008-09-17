/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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

/*
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.CosBy;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class GetCos extends AdminDocumentHandler {

    public boolean domainAuthSufficient(Map context) {
        return true;
    }
    
	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
	    
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        Element d = request.getElement(AdminConstants.E_COS);
	    String key = d.getAttribute(AdminConstants.A_BY);
        String value = d.getText();

	    Cos cos = prov.get(CosBy.fromString(key), value);
	    
	    // if we are a domain admin only, restrict to the COSs viewable by the domain admin
        if (isDomainAdminOnly(zsc)) {
            // for domain admin, if the requested COS does not exist, throw PERM_DENIED instead of NO_SUCH_COS
            if (cos == null || !canAccessCos(zsc, cos.getId()))
                throw ServiceException.PERM_DENIED("can not access cos"); 
        }
	    
        if (cos == null)
            throw AccountServiceException.NO_SUCH_COS(value);

	    Element response = zsc.createElement(AdminConstants.GET_COS_RESPONSE);
        doCos(response, cos);

	    return response;
	}

	public static void doCos(Element e, Cos c) throws ServiceException {
        Config config = Provisioning.getInstance().getConfig();
        Element cos = e.addElement(AdminConstants.E_COS);
        cos.addAttribute(AdminConstants.A_NAME, c.getName());
        cos.addAttribute(AdminConstants.E_ID, c.getId());
        Map attrs = c.getUnicodeAttrs();
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
