/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.service.admin;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.GranteeBy;
import com.zimbra.cs.account.Provisioning.TargetBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.RightCommand;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.soap.ZimbraSoapContext;

public class GetGrants extends RightDocumentHandler {
    
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        
        String targetType = null;
        TargetBy targetBy = null;
        String target = null;
        Element eTarget = request.getOptionalElement(AdminConstants.E_TARGET);
        if (eTarget != null) {
            targetType = eTarget.getAttribute(AdminConstants.A_TYPE);
            if (TargetType.fromCode(targetType).needsTargetIdentity()) {
                targetBy = TargetBy.fromString(eTarget.getAttribute(AdminConstants.A_BY));
                target = eTarget.getText();
            }
            
            // check if the authed admin has right to view grants on the desired target
            TargetType tt = TargetType.fromCode(targetType);
            Entry targetEntry = TargetType.lookupTarget(prov, tt, targetBy, target);
            
            // targetEntry cannot be null by now, because lookupTarget would have thrown 
            // if the specified target does not exist 
            checkRight(zsc, targetEntry, Admin.R_viewGrants);
        }
        
        String granteeType = null;
        GranteeBy granteeBy = null;
        String grantee = null;
        boolean granteeIncludeGroupsGranteeBelongs = true;
        Element eGrantee = request.getOptionalElement(AdminConstants.E_GRANTEE);
        if (eGrantee != null) {
            granteeType = eGrantee.getAttribute(AdminConstants.A_TYPE);
            granteeBy = GranteeBy.fromString(eGrantee.getAttribute(AdminConstants.A_BY));
            grantee = eGrantee.getText();
            granteeIncludeGroupsGranteeBelongs = eGrantee.getAttributeBool(AdminConstants.A_ALL);
        }
        
        RightCommand.Grants grants = RightCommand.getGrants(
                prov,
                targetType, targetBy, target,
                granteeType, granteeBy, grantee, granteeIncludeGroupsGranteeBelongs);
        
        // check if the authed admin can see the zimbraACE attr on 
        // each of the target on which grants for the specified grantee are found
        Set<String> OKedTarget = new HashSet<String>();
        for (RightCommand.ACE ace : grants.getACEs()) {
            TargetType tt = TargetType.fromCode(ace.targetType());
            // has to look up target by name, because zimlet can only be looked up by name
            Entry targetEntry = TargetType.lookupTarget(prov, tt, TargetBy.name, ace.targetName());
            String targetKey = ace.targetType() + "-" + ace.targetId();
            if (!OKedTarget.contains(targetKey)) {
                checkRight(zsc, targetEntry, Admin.R_viewGrants);
                OKedTarget.add(targetKey);  // add the target to our OKed set, so we don't check again
            }
        }
        
        Element resp = zsc.createElement(AdminConstants.GET_GRANTS_RESPONSE);
        grants.toXML(resp);
        return resp;
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_viewGrants);
        notes.add("Needs a get attr right of zimbraACE on each the target entry.  " +
                "Granting the " + Admin.R_viewGrants.getName() + " is one way to do it, " +
                "which will give the right on all target types.   Use inline right " +
                "if more granularity is needed.   See doc for the " + Admin.R_viewGrants.getName() + 
                " right in zimbra-rights.xml for more details.");
    }
}
