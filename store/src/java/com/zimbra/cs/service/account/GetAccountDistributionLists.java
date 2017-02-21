/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.account;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.entry.LdapDistributionList;
import com.zimbra.cs.account.ldap.entry.LdapDynamicGroup;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.account.type.MemberOfSelector;

public class GetAccountDistributionLists extends AccountDocumentHandler {
    
    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account acct = getRequestedAccount(zsc);
        Provisioning prov = Provisioning.getInstance();
        
        if (!canAccessAccount(zsc, acct)) {
            throw ServiceException.PERM_DENIED("can not access account");
        }
        
        boolean needOwnerOf = request.getAttributeBool(AccountConstants.A_OWNER_OF, false);
        MemberOfSelector needMemberOf = MemberOfSelector.fromString(
                request.getAttribute(AccountConstants.A_MEMBER_OF, MemberOfSelector.directOnly.name()));
        
        Iterable<String> needAttrs = Splitter.on(',').trimResults().split(
                request.getAttribute(AccountConstants.A_ATTRS, ""));
        
        Set<Group> ownerOf = null;
        List<Group> memberOf = null;
        HashMap<String, String> via = new HashMap<String, String>();
        
        if (needOwnerOf) {
            ownerOf = Group.GroupOwner.getOwnedGroups(acct);
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
            for (Group group : ownerOf) {
                String groupId = group.getId();
                ownerOfGroupIds.add(groupId);
                
                if (!combinedIds.contains(groupId)) {
                    combined.add(group);
                    combinedIds.add(groupId);
                }
            }
        }
        
        if (memberOf != null) {
            for (Group group : memberOf) {
                String groupId = group.getId();
                memberOfGroupIds.add(groupId);
                
                if (!combinedIds.contains(groupId)) {
                    combined.add(group);
                    combinedIds.add(groupId);
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
            if (group.isDynamic()) {
                eDL.addAttribute(AccountConstants.A_REF, ((LdapDynamicGroup) group).getDN());
            } else {
                eDL.addAttribute(AccountConstants.A_REF, ((LdapDistributionList) group).getDN());
            }
            eDL.addAttribute(AccountConstants.A_ID, group.getId());
            eDL.addAttribute(AccountConstants.A_DISPLAY, group.getDisplayName());
            eDL.addAttribute(AccountConstants.A_DYNAMIC, group.isDynamic());

            boolean isOwner = ownerOfGroupIds.contains(group.getId());

            if (needOwnerOf) {
                eDL.addAttribute(AccountConstants.A_IS_OWNER, isOwner);
            }
            
            if (MemberOfSelector.none != needMemberOf) {
                boolean isMember = memberOfGroupIds.contains(group.getId());
                eDL.addAttribute(AccountConstants.A_IS_MEMBER, isMember);
                
                if (isMember) {
                    String viaDl = via.get(group.getName());
                    if (viaDl != null) {
                        eDL.addAttribute(AccountConstants.A_VIA, viaDl);
                    }
                }
            }
            
            Set<String> returnAttrs = GetDistributionList.visibleAttrs(needAttrs, isOwner);
            if (!returnAttrs.isEmpty()) {
                GetDistributionList.encodeAttrs(group, eDL, returnAttrs);
            }
        }
        return response;
    }

}
