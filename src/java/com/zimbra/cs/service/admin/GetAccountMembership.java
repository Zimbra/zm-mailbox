/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.DynamicGroup;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class GetAccountMembership extends AdminDocumentHandler {

    /**
     * must be careful and only return accounts a domain admin can see
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        Element a = request.getElement(AdminConstants.E_ACCOUNT);
        String key = a.getAttribute(AdminConstants.A_BY);
        String value = a.getText();

        Account account = prov.get(AccountBy.fromString(key), value, zsc.getAuthToken());

        if (account == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(value);

        checkAccountRight(zsc, account, Admin.R_getAccountMembership);

        HashMap<String,String> via = new HashMap<String, String>();
        List<Group> groups = prov.getGroups(account, false, via);
        
        Element response = zsc.createElement(AdminConstants.GET_ACCOUNT_MEMBERSHIP_RESPONSE);
        for (Group group: groups) {
            Element eDL = response.addElement(AdminConstants.E_DL);
            eDL.addAttribute(AdminConstants.A_NAME, group.getName());
            eDL.addAttribute(AdminConstants.A_ID,group.getId());
            eDL.addAttribute(AdminConstants.A_DYNAMIC, group.isDynamic());
            String viaDl = via.get(group.getName());
            if (viaDl != null) {
                eDL.addAttribute(AdminConstants.A_VIA, viaDl);
            }
            
            try {
                if (group.isDynamic()) {
                    checkDynamicGroupRight(zsc, (DynamicGroup) group, needGetAttrsRight());
                } else {
                    checkDistributionListRight(zsc, (DistributionList) group, needGetAttrsRight());
                }
                
                String isAdminGroup = group.getAttr(Provisioning.A_zimbraIsAdminGroup);
                if (isAdminGroup != null) {
                    eDL.addElement(AdminConstants.E_A).addAttribute(AdminConstants.A_N, Provisioning.A_zimbraIsAdminGroup).setText(isAdminGroup);
                }
            } catch (ServiceException e) {
                if (ServiceException.PERM_DENIED.equals(e.getCode())) {
                    ZimbraLog.acl.warn("no permission to view " + Provisioning.A_zimbraIsAdminGroup + " of dl " + group.getName());
                }
            }
        }
        return response;
    }
    
    private Set<String> needGetAttrsRight() {
        Set<String> attrsNeeded = new HashSet<String>();
        attrsNeeded.add(Provisioning.A_zimbraIsAdminGroup);
        return attrsNeeded;
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_getAccountMembership);
        notes.add("If the authed admin has get attr right on  distribution list attr " + 
                Provisioning.A_zimbraIsAdminGroup + ", it is returned in the response if set.");
    }
}
