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
import com.zimbra.common.calendar.TZIDMapper;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;

import com.zimbra.soap.ZimbraSoapContext;

public class GetDistributionList extends DistributionListDocumentHandler {
    
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
    
    private static final Set<String> NON_OWNER_ATTRS = Sets.newHashSet(
            Provisioning.A_description,
            Provisioning.A_displayName);

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        Account acct = getRequestedAccount(zsc);
        
        Group group = getGroup(request, prov);

        boolean isOwner = isOwner(acct, group);
        
        Element response = zsc.createElement(AccountConstants.GET_DISTRIBUTION_LIST_RESPONSE);
        
        // isMember
        boolean isMember = isMember(prov, acct, group);

        response.addAttribute(AccountConstants.A_IS_MEMBER, isMember);  
        response.addAttribute(AccountConstants.A_IS_OWNER, isOwner);
        
        boolean needOwners = request.getAttributeBool(AccountConstants.A_NEED_OWNERS, false);
        
        Element eDL;
        
        // set encodeAttrs to false, we will be encoded attrs using addKeyValuePair,
        // which is more json friendly
        eDL = com.zimbra.cs.service.admin.GetDistributionList.encodeDistributionList(
                    response, group, true, !needOwners, false, null, null);
        
        encodeAttrs(group, eDL, isOwner ? OWNER_ATTRS : NON_OWNER_ATTRS);

        return response;
    }
    
    private void encodeAttrs(Group group, Element eDL, Set<String> specificPrefs) {
        Map<String, Object> attrsMap = group.getUnicodeAttrs();
        
        if (specificPrefs == null || !specificPrefs.isEmpty()) {
            for (Map.Entry<String, Object> entry : attrsMap.entrySet()) {
                String key = entry.getKey();
    
                if (specificPrefs != null && !specificPrefs.contains(key)) {
                    continue;
                }
                
                Object value = entry.getValue();
                if (value instanceof String[]) {
                    String sa[] = (String[]) value;
                    for (int i = 0; i < sa.length; i++) {
                        eDL.addKeyValuePair(key, sa[i], AccountConstants.E_A, AccountConstants.A_N);
                    }
                } else {
                    eDL.addKeyValuePair(key, (String) value, AccountConstants.E_A, AccountConstants.A_N);
                }
            }
        }
        
        // always include subscription policies.
        // subscription policies are encoded differently, using Group API that returns 
        // default policy if the policy attrs are not set.
        eDL.addKeyValuePair(Provisioning.A_zimbraDistributionListSubscriptionPolicy, 
                group.getSubscriptionPolicy().name(), 
                AccountConstants.E_A, AccountConstants.A_N);
        eDL.addKeyValuePair(Provisioning.A_zimbraDistributionListUnsubscriptionPolicy,
                group.getUnsubscriptionPolicy().name(),
                AccountConstants.E_A, AccountConstants.A_N);
    }   
}

