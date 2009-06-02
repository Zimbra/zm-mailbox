/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
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

import com.zimbra.common.calendar.TZIDMapper;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Provisioning;
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
        
        checkCosRight(zsc, cos, reqAttrs == null ? Admin.R_getCos : reqAttrs);

        Element response = zsc.createElement(AdminConstants.GET_COS_RESPONSE);
        doCos(response, cos, reqAttrs);

        return response;
    }

    public static void doCos(Element e, Cos c) throws ServiceException {
        doCos(e, c, null);
    }

    public static void doCos(Element e, Cos c, Set<String> reqAttrs) throws ServiceException {
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
            
            if (reqAttrs != null && !reqAttrs.contains(name))
                continue;
            
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
            } else if (value instanceof String) {
                Element attr = cos.addElement(AdminConstants.E_A);
                attr.addAttribute(AdminConstants.A_N, name);
                if (isCosAttr) 
                    attr.addAttribute(AdminConstants.A_C, "1");
                // Fixup for time zone id.  Always use canonical (Olson ZoneInfo) ID.
                if (name.equals(Provisioning.A_zimbraPrefTimeZoneId))
                    value = TZIDMapper.canonicalize((String) value);
                attr.setText((String) value);
            }
        }
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_getCos);
        notes.add(String.format(AdminRightCheckPoint.Notes.GET_ENTRY, Admin.R_getCos.getName()));
    }
}
