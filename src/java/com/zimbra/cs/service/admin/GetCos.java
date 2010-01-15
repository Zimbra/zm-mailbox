/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

/*
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.AccessManager.AttrRightChecker;
import com.zimbra.cs.account.Provisioning.CosBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
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

        Set<String> reqAttrs = getReqAttrs(request, AttributeClass.cos);
        
        Element d = request.getElement(AdminConstants.E_COS);
        String key = d.getAttribute(AdminConstants.A_BY);
        String value = d.getText();

        Cos cos = prov.get(CosBy.fromString(key), value);

        if (cos == null)
            throw AccountServiceException.NO_SUCH_COS(value);
        
        AdminAccessControl aac = checkCosRight(zsc, cos, AdminRight.PR_ALWAYS_ALLOW);
        
        Element response = zsc.createElement(AdminConstants.GET_COS_RESPONSE);
        encodeCos(response, cos, reqAttrs, aac.getAttrRightChecker(cos));

        return response;
    }

    public static void encodeCos(Element e, Cos c) throws ServiceException {
        encodeCos(e, c, null, null);
    }

    public static void encodeCos(Element e, Cos c, Set<String> reqAttrs, AttrRightChecker attrRightChecker) throws ServiceException {
        Config config = Provisioning.getInstance().getConfig();
        Element cos = e.addElement(AdminConstants.E_COS);
        cos.addAttribute(AdminConstants.A_NAME, c.getName());
        cos.addAttribute(AdminConstants.E_ID, c.getId());
        
        if (c.isDefaultCos())
            cos.addAttribute(AdminConstants.A_IS_DEFAULT_COS, true);
        
        Map attrs = c.getUnicodeAttrs();
        AttributeManager attrMgr = AttributeManager.getInstance();
        for (Iterator mit=attrs.entrySet().iterator(); mit.hasNext(); ) {
            Map.Entry entry = (Entry) mit.next();
            String name = (String) entry.getKey();
            Object value = entry.getValue();
            
            if (reqAttrs != null && !reqAttrs.contains(name))
                continue;
            
            boolean allowed = attrRightChecker == null ? true : attrRightChecker.allowAttr(name);
            
            boolean isCosAttr = !attrMgr.isAccountInherited(name);
            if (value instanceof String[]) {
                String sv[] = (String[]) value;
                for (int i = 0; i < sv.length; i++) {
                    encodeCosAttr(cos, name, sv[i], isCosAttr, allowed);
                }
            } else if (value instanceof String) {
                value = com.zimbra.cs.service.account.ToXML.fixupZimbraPrefTimeZoneId(name, (String)value);
                encodeCosAttr(cos, name, (String)value, isCosAttr, allowed);
            }
        }
    }
    
    private static void encodeCosAttr(Element parent, String key, String value, boolean isCosAttr, boolean allowed) {
        
        Element e = parent.addElement(AdminConstants.E_A);
        e.addAttribute(AdminConstants.A_N, key);
        
        if (allowed) {
            e.setText(value);
        } else {
            e.addAttribute(AccountConstants.A_PERM_DENIED, true);
        }
        
        if (isCosAttr) 
            e.addAttribute(AdminConstants.A_C, true);
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_getCos);
        notes.add(String.format(AdminRightCheckPoint.Notes.GET_ENTRY, Admin.R_getCos.getName()));
    }
}
