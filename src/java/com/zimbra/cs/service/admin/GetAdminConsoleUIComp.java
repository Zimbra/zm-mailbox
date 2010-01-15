/*
 * ***** BEGIN LICENSE BLOCK *****
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
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.AclGroups;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.soap.ZimbraSoapContext;

public class GetAdminConsoleUIComp extends AdminDocumentHandler {
    
    public boolean domainAuthSufficient(Map context) {
        return true;
    }
    
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        
        Element resp = zsc.createElement(AdminConstants.GET_ADMIN_CONSOLE_UI_COMP_RESPONSE);
        
        Element eAccount = request.getOptionalElement(AdminConstants.E_ACCOUNT);
        Element eDL = request.getOptionalElement(AdminConstants.E_DL);
        
        if (eAccount != null && eDL != null)
            throw ServiceException.INVALID_REQUEST("can only specify eith account or dl", null);
        
        Account authedAcct = getAuthenticatedAccount(zsc);
        
        Set<String> added = new HashSet<String>();
        AclGroups aclGroups;
        
        if (eAccount != null) {
            AccountBy by = AccountBy.fromString(eAccount.getAttribute(AdminConstants.A_BY));
            String key = eAccount.getText();
            Account acct = prov.get(by, key);
            
            if (acct == null)
                throw AccountServiceException.NO_SUCH_ACCOUNT(key);
            
            if (!authedAcct.getId().equals(acct.getId()))
                checkRight(zsc, context, acct, Admin.R_viewAccountAdminUI);
            
            addValues(acct, resp, added, false);
            aclGroups = prov.getAclGroups(acct, true);
            
        } else if (eDL != null) {
            DistributionListBy by = DistributionListBy.fromString(eDL.getAttribute(AdminConstants.A_BY));
            String key = eDL.getText();
            DistributionList dl = prov.getAclGroup(by, key);
            
            if (dl == null)
                throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(key);
            
            checkRight(zsc, context, dl, Admin.R_viewDistributionListAdminUI);
            
            addValues(dl, resp, added, false);
            aclGroups = prov.getAclGroups(dl, true);
        } else {
            // use the authed account
            addValues(authedAcct, resp, added, false);
            aclGroups = prov.getAclGroups(authedAcct, true);
        }
        
        for (String groupId : aclGroups.groupIds()) {
            DistributionList dl = prov.get(DistributionListBy.id, groupId);
            addValues(dl, resp, added, true);
        }
        
        return resp;
    }
    
    private void addValues(NamedEntry entry, Element resp, Set<String> added, boolean inherited) {
        Set<String> values = entry.getMultiAttrSet(Provisioning.A_zimbraAdminConsoleUIComponents);
        for (String value: values) {
            if (!added.contains(value)) {
                resp.addElement(AdminConstants.E_A).setText(value).addAttribute(AdminConstants.A_INHERITED, inherited);
                added.add(value);
            }
        }
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_viewAccountAdminUI);
        relatedRights.add(Admin.R_viewDistributionListAdminUI);
        
        notes.add("If account/dl is not specified, " + AdminRightCheckPoint.Notes.ALLOW_ALL_ADMINS);
        notes.add("If an account is specified, need the " + Admin.R_viewAccountAdminUI.getName() +
                " right.");
        notes.add("If a dl is specified, need the " + Admin.R_viewDistributionListAdminUI.getName() +
                " right.");
        notes.add("Note, this call does not check for the get attr right for " + 
                Provisioning.A_zimbraAdminConsoleUIComponents + " attribute on the account/dl, nor " +
                "on the admin groups they belong.  It simply checks the " + Admin.R_viewAccountAdminUI.getName() +
                " or " + Admin.R_viewDistributionListAdminUI.getName() + " right.");
    }
    
}
