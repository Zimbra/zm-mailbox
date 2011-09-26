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
import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;

import com.zimbra.soap.ZimbraSoapContext;

public class GetDistributionList extends AccountDocumentHandler {
    
    private static final Set<String> OWNER_ATTRS = Sets.newHashSet(
            Provisioning.A_description,
            Provisioning.A_displayName,
            Provisioning.A_mail,
            Provisioning.A_zimbraHideInGal,
            Provisioning.A_zimbraIsAdminGroup,
            Provisioning.A_zimbraLocale,
            Provisioning.A_zimbraMailAlias,
            Provisioning.A_zimbraMailStatus,
            Provisioning.A_zimbraPrefReplyToAddress,
            Provisioning.A_zimbraPrefReplyToDisplay,
            Provisioning.A_zimbraPrefReplyToEnabled);
    
    // not used for now
    private static final Set<String> NON_OWNER_ATTRS = Sets.newHashSet();

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        Account acct = getRequestedAccount(zsc);
        
        Group group = GetDistributionList.getGroup(request, prov);

        boolean isOwner = GetDistributionList.isOwner(acct, group);
        
        Element response = zsc.createElement(AccountConstants.GET_DISTRIBUTION_LIST_RESPONSE);
        
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
        response.addAttribute(AccountConstants.A_IS_OWNER, isOwner);
        
        boolean needOwners = request.getAttributeBool(AccountConstants.A_NEED_OWNERS, false);
        
        Element eDL;
        if (isOwner) {
            eDL = com.zimbra.cs.service.admin.GetDistributionList.encodeDistributionList(
                response, group, true, !needOwners, OWNER_ATTRS, null);
        } else {
            // set encodeAttrs to false fow now since we don't need to return any attr 
            // other than subscription policies for non owners for now
            eDL = com.zimbra.cs.service.admin.GetDistributionList.encodeDistributionList(
                    response, group, true, !needOwners, false, null, null);
        }
        
        // always return subscription policies, they are not included in required attrs 
        // because we want to  retun a default value if they are not set.
        //
        // subscription policies are encoded here using Group API that returns 
        // default policy if the policy attrs are not set.
        eDL.addElement(AccountConstants.E_A).
                addAttribute(AccountConstants.A_N, Provisioning.A_zimbraDistributionListSubscriptionPolicy).
                setText(group.getSubscriptionPolicy().name());
        eDL.addElement(AccountConstants.E_A).
                addAttribute(AccountConstants.A_N, Provisioning.A_zimbraDistributionListUnsubscriptionPolicy).
                setText(group.getUnsubscriptionPolicy().name()); 

        return response;
    }
    
    public static Group getGroup(Element request, Provisioning prov) 
    throws ServiceException {
        Element d = request.getElement(AccountConstants.E_DL);
        String key = d.getAttribute(AccountConstants.A_BY);
        String value = d.getText();
        
        // temporary fix for the caching bug
        // Group group = prov.getGroupBasic(Key.DistributionListBy.fromString(key), value);
        Group group = prov.getGroup(Key.DistributionListBy.fromString(key), value);
        
        if (group == null) {
            throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(value);
        }
        
        return group;
    }
    
    public static Group getGroup(Element request, Account acct, Provisioning prov) 
    throws ServiceException {
        Group group = getGroup(request, prov);
        
        if (!isOwner(acct, group)) {
            throw ServiceException.PERM_DENIED(
                    "you do not have sufficient rights to access this distribution list");
        }
        
        return group;
    }
    
    private static boolean isOwner(Account acct, Group group) throws ServiceException {
        return AccessManager.getInstance().canAccessGroup(acct, group);
    }
}

