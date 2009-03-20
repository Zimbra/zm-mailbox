/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AclGroups;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.soap.ZimbraSoapContext;

public class GetAdminConsoleUIComp extends AdminDocumentHandler {
    
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        
        Element resp = zsc.createElement(AdminConstants.GET_ADMIN_CONSOLE_UI_COMP_RESPONSE);
        
        Set<String> added = new HashSet<String>();
        
        Account acct = getAuthenticatedAccount(zsc);
        addValues(acct, resp, added);
        
        Provisioning prov = Provisioning.getInstance();
        AclGroups aclGroups = prov.getAclGroups(acct, true);
        for (String groupId : aclGroups.groupIds()) {
            DistributionList dl = prov.get(DistributionListBy.id, groupId);
            addValues(dl, resp, added);
        }
        
        return resp;
    }
    
    private void addValues(NamedEntry entry, Element resp, Set<String> added) {
        Set<String> values = entry.getMultiAttrSet(Provisioning.A_zimbraAdminConsoleUIComponents);
        for (String value: values) {
            if (!added.contains(value)) {
                resp.addElement(AdminConstants.E_A).setText(value);
                added.add(value);
            }
        }
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add(AdminRightCheckPoint.Notes.ALLOW_ALL_ADMINS);
    }
    
}
