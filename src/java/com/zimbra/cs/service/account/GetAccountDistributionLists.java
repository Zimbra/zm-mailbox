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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.account.message.GetAccountDistributionListsRequest.MemberOfSelector;

public class GetAccountDistributionLists extends AccountDocumentHandler {
    
    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account acct = getRequestedAccount(zsc);
        Provisioning prov = Provisioning.getInstance();
        
        boolean needOwnerOf = request.getAttributeBool(AccountConstants.A_OWNER_OF, false);
        MemberOfSelector needMemberOf = MemberOfSelector.fromString(
                request.getAttribute(AccountConstants.A_MEMBER_OF, MemberOfSelector.directOnly.name()));
        
        Iterable<String> needAttrs = Splitter.on(',').trimResults().split(
                request.getAttribute(AccountConstants.A_ATTRS, ""));
        
        Set<Entry> ownerOf = null;
        List<Group> memberOf = null;
        HashMap<String,String> via = new HashMap<String, String>();
        
        if (needOwnerOf) {
            ownerOf = getOwnedGroups(acct);
        }
        
        if (MemberOfSelector.none != needMemberOf) {
            memberOf = prov.getGroups(acct, MemberOfSelector.directOnly == needMemberOf, via);
        }
        
        /*
         * merge the two results into one locale-sensitive sorted list
         */
        Set<Entry> combined = Sets.newHashSet();
        Set<String> combinedIds = Sets.newHashSet();
        Set<String> ownerOfGroupIds = Sets.newHashSet();
        Set<String> memberOfGroupIds = Sets.newHashSet();
        
        if (ownerOf != null) {
            for (Entry entry : ownerOf) {
                if (!(entry instanceof Group)) {
                    // skip non group targets. AccessManager.discoverRights currently 
                    // only returns group targets, but it can change later.
                    continue;
                }
                Group group = (Group) entry;
                ownerOfGroupIds.add(group.getId());
                
                if (!combinedIds.contains(group.getId())) {
                    combined.add(group);
                    combinedIds.add(group.getId());
                }
            }
        }
        if (memberOf != null) {
            for (Group group : memberOf) {
                memberOfGroupIds.add(group.getId());
                
                if (!combinedIds.contains(group.getId())) {
                    combined.add(group);
                    combinedIds.add(group.getId());
                }
            }
        }
        
        // sort it
        List<Entry> sortedGroups = Entry.sortByDisplayName(combined, acct.getLocale());
        
        Element response = zsc.createElement(AccountConstants.GET_ACCOUNT_DISTRIBUTION_LISTS_RESPONSE);
        
        for (Entry entry: sortedGroups) {
            Group group = (Group) entry;
            
            Element eDL = response.addElement(AccountConstants.E_DL);
            eDL.addAttribute(AccountConstants.A_NAME, group.getName());
            eDL.addAttribute(AccountConstants.A_ID, group.getId());
            eDL.addAttribute(AccountConstants.A_DISPLAY, group.getDisplayName());
            eDL.addAttribute(AccountConstants.A_DYNAMIC, group.isDynamic());
            
            boolean isOwner = ownerOfGroupIds.contains(group.getId());
            
            if (needOwnerOf && isOwner) {
                eDL.addAttribute(AccountConstants.A_IS_OWNER, true);
            }
            
            if (MemberOfSelector.none != needMemberOf && memberOfGroupIds.contains(group.getId())) {
                eDL.addAttribute(AccountConstants.A_IS_MEMBER, true);
                String viaDl = via.get(group.getName());
                if (viaDl != null) {
                    eDL.addAttribute(AccountConstants.A_VIA, viaDl);
                }
            }
            
            Set<String> returnAttrs = GetDistributionList.visibleAttrs(needAttrs, isOwner);
            if (!returnAttrs.isEmpty()) {
                GetDistributionList.encodeAttrs(group, eDL, returnAttrs);
            }
        }
        return response;
    }
    
    private Set<Entry> getOwnedGroups(Account acct) throws ServiceException {
        AccessManager accessMgr = AccessManager.getInstance();
        
        Right right = Group.GroupOwner.GROUP_OWNER_RIGHT;
        Map<Right, Set<Entry>> discoveredRights = accessMgr.discoverRights(acct, 
                Collections.singleton(right));
        
        return discoveredRights.get(right);
    }

}
