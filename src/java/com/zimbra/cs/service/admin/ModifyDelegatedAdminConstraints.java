/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.CosBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.AttributeConstraint;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.soap.ZimbraSoapContext;

public class ModifyDelegatedAdminConstraints extends AdminDocumentHandler {

    private static final String CONSTRAINT_ATTR = Provisioning.A_zimbraConstraint;
    
    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        Entry entry = GetDelegatedAdminConstraints.getEntry(request);
        
        AdminAccessControl.SetAttrsRight sar = new AdminAccessControl.SetAttrsRight();
        sar.addAttr(Provisioning.A_zimbraConstraint);
        checkRight(zsc, context, entry, sar);
        
        AttributeManager am = AttributeManager.getInstance();
        List<AttributeConstraint> constraints = new ArrayList<AttributeConstraint>();
        for (Element a : request.listElements(AdminConstants.E_A)) {
            String attrName = a.getAttribute(AdminConstants.A_NAME);
            Element eConstraint = a.getElement(AdminConstants.E_CONSTRAINT);
            
            constraints.add(AttributeConstraint.fromXML(am, attrName, eConstraint));
        }
        
        AttributeConstraint.modifyConstraint(entry, constraints);
        
        // log it
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(CONSTRAINT_ATTR, entry.getMultiAttr(CONSTRAINT_ATTR, false));
        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "ModifyDelegatedAdminConstraints","name", entry.getLabel()}, attrs));
        
        Element response = zsc.createElement(AdminConstants.MODIFY_DELEGATED_ADMIN_CONSTRAINTS_RESPONSE);
        return response;
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add("Need set attr right on attribute " + CONSTRAINT_ATTR);
    }

}
