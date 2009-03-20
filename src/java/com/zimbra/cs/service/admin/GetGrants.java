/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009 Zimbra, Inc.
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

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.TargetBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.RightCommand;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.soap.ZimbraSoapContext;

public class GetGrants extends RightDocumentHandler {
    
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        
        Element eTarget = request.getElement(AdminConstants.E_TARGET);
        String targetType = eTarget.getAttribute(AdminConstants.A_TYPE);
        TargetBy targetBy = null;
        String target = null;
        if (TargetType.fromString(targetType).needsTargetIdentity()) {
            targetBy = TargetBy.fromString(eTarget.getAttribute(AdminConstants.A_BY));
            target = eTarget.getText();
        }
            
        Provisioning prov = Provisioning.getInstance();
        
        // check if the authed admin can see the zimbraACE attr on the target entry
        TargetType tt = TargetType.fromString(targetType);
        Entry targetEntry = TargetType.lookupTarget(prov, tt, targetBy, target);
        checkRight(zsc, targetEntry, Admin.R_viewGrants);
        
        RightCommand.ACL acl = RightCommand.getGrants(Provisioning.getInstance(),
                                                      targetType, targetBy, target);
        
        Element resp = zsc.createElement(AdminConstants.GET_GRANTS_RESPONSE);
        acl.toXML(resp);
        return resp;
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_viewGrants);
        notes.add("Needs a get attr right of zimbraACE of the requested target type.  " +
                "Granting the " + Admin.R_viewGrants.getName() + " is one way to do it, " +
                "which will give the right on all target types.   Use inline right " +
                "if more granularity is needed.   See doc for the " + Admin.R_viewGrants.getName() + 
                " right in zimbra-rights.xml for more details.");
    }
}
