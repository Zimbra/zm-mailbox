/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.service.account;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.ZimbraSoapContext;

public class GetDistributionListInfo extends AccountDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account acct = getRequestedAccount(zsc);
        Provisioning prov = Provisioning.getInstance();
        
        Group group = GetDistributionList.getGroup(request, prov);
        Element response = zsc.createElement(AccountConstants.GET_DISTRIBUTION_LIST_INFO_RESPONSE);
        
        // isMember
        boolean isMember = false;
        List<Group> groups = prov.getGroups(acct, false, null); // all groups the account is a member of
        for (Group inGroup : groups) {
            if (inGroup.getId().equalsIgnoreCase(group.getId())) {
                isMember = true;
                break;
            }
        }
        response.addAttribute(AccountConstants.A_IS_MEMBER, isMember);  
        
        // isOwner
        // TODO: should exclude global admins, and return only account s allowed by ACL
        boolean isOwner = AccessManager.getInstance().canAccessGroup(acct, group);
        response.addAttribute(AccountConstants.A_IS_OWNER, isOwner);
        
        boolean needOwners = request.getAttributeBool(AccountConstants.A_NEED_OWNERS, false);
        Set<String> reqAttrs = Sets.newHashSet();
        reqAttrs.add(Provisioning.A_zimbraDistributionListSubscriptionPolicy);
        reqAttrs.add(Provisioning.A_zimbraDistributionListUnsubscriptionPolicy);
        
        // do not encode attrs in encodeDistributionList
        // subscription policies are encoded here using Group API that returns 
        // default policy if the policy attrs are not set.
        Element eDL = com.zimbra.cs.service.admin.GetDistributionList.encodeDistributionList(
                response, group, true, !needOwners, false, reqAttrs, null);
        eDL.addElement(AccountConstants.E_A).
                addAttribute(AccountConstants.A_N, Provisioning.A_zimbraDistributionListSubscriptionPolicy).
                setText(group.getSubscriptionPolicy().name());
        eDL.addElement(AccountConstants.E_A).
            addAttribute(AccountConstants.A_N, Provisioning.A_zimbraDistributionListUnsubscriptionPolicy).
            setText(group.getUnsubscriptionPolicy().name()); 
        
        return response;
    }


}
