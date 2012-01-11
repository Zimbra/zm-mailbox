/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.account;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.ZAttrProvisioning.DistributionListSubscriptionPolicy;
import com.zimbra.common.account.ZAttrProvisioning.DistributionListUnsubscriptionPolicy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.accesscontrol.ACLUtil;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.ZimbraACE;
import com.zimbra.cs.account.accesscontrol.Rights.User;

public abstract class Group extends MailTarget implements AliasedEntry {
    
    public static final DistributionListSubscriptionPolicy 
            DEFAULT_SUBSCRIPTION_POLICY = DistributionListSubscriptionPolicy.REJECT;
    
    public static final DistributionListUnsubscriptionPolicy 
            DEFAULT_UNSUBSCRIPTION_POLICY = DistributionListUnsubscriptionPolicy.REJECT;
    
    public Group(String name, String id, Map<String, Object> attrs, Provisioning prov) {
        super(name, id, attrs, null, prov);
    }
    
    public abstract boolean isDynamic();

    public abstract Domain getDomain() throws ServiceException;
    
    public abstract String[] getAllMembers() throws ServiceException;
    
    public abstract Set<String> getAllMembersSet() throws ServiceException;
    
    public abstract String getDisplayName();
    
    abstract DistributionListSubscriptionPolicy getDistributionListSubscriptionPolicy();
    abstract DistributionListUnsubscriptionPolicy getDistributionListUnsubscriptionPolicy();
    
    public Server getServer() throws ServiceException {
        String serverName = getAttr(Provisioning.A_zimbraMailHost);
        return (serverName == null ? null : getProvisioning().get(Key.ServerBy.name, serverName));
    }
    
    public DistributionListSubscriptionPolicy getSubscriptionPolicy() {
        DistributionListSubscriptionPolicy policy = getDistributionListSubscriptionPolicy();
        if (policy == null) {
            return DEFAULT_SUBSCRIPTION_POLICY;
        } else {
            return policy;
        }
    }
    
    public DistributionListUnsubscriptionPolicy getUnsubscriptionPolicy() {
        DistributionListUnsubscriptionPolicy policy = getDistributionListUnsubscriptionPolicy();
        if (policy == null) {
            return DEFAULT_UNSUBSCRIPTION_POLICY;
        } else {
            return policy;
        }
    }

    @Override
    public boolean isAddrOfEntry(String addr) {
        addr = addr.toLowerCase();
        if (getName().equals(addr)) {
            return true;
        } else {
            Set<String> aliases = getMultiAttrSet(Provisioning.A_zimbraMailAlias);
            return aliases.contains(addr);
        }
    }
    
    @Override
    public Set<String> getAllAddrsSet() {
        Set<String> addrs = Sets.newHashSet();
        addrs.add(getName());
        addrs.addAll(getMultiAttrSet(Provisioning.A_zimbraMailAlias));
        return Collections.unmodifiableSet(addrs);
    }
    
    
    public static class GroupOwner {
        /*
         * The ownDistList right can only be granted on group target 
         * (defined in the right definition xml), not domain, not globalgrant.  
         * 
         * It is implemented a as right (instead of an attribute on group) 
         * because we need to support various types (user, group, external group) of 
         * owners and it makes sense to use the delegated admin framework. 
         */
        public static Right GROUP_OWNER_RIGHT = User.R_ownDistList;
        
        private GranteeType type;
        private String id;
        private String name;
        
        private GroupOwner(ZimbraACE ace, boolean needName) {
            type = ace.getGranteeType();
            id = ace.getGrantee();
            if (needName) {
                name = ace.getGranteeDisplayName();
            }
        }
        
        public GranteeType getType() {
            return type;
        }
        
        public String getId() {
            return id;
        }
        
        public String getName() {
            return name;
        }
        
        
        public static Set<Group> getOwnedGroups(Account acct) throws ServiceException {
            AccessManager accessMgr = AccessManager.getInstance();
            
            Right right = GROUP_OWNER_RIGHT;
            Map<Right, Set<Entry>> discoveredRights = accessMgr.discoverRights(acct, 
                    Collections.singleton(right));
            
            Set<Entry> ownerOf = discoveredRights.get(right);
            Set<Group> ownerOfGroups = Sets.newHashSet();
            
            if (ownerOf != null) {
                for (Entry entry : ownerOf) {
                    if (!(entry instanceof Group)) {
                        // skip non group targets. AccessManager.discoverRights currently 
                        // only returns group targets, but it can change later.
                        continue;
                    }
                    ownerOfGroups.add((Group) entry);
                }
            }
            
            return ownerOfGroups;
        }
        
        public static Set<String> getOwnedGroupsIds(Account acct) throws ServiceException {
            Set<Group> groups = getOwnedGroups(acct);
            
            Set<String> ids = Sets.newHashSet();
            if (groups != null) {
                for (Group group : groups) {
                    ids.add(group.getId());
                }
            }
            
            return ids;
        }
        
        /*
         * returns whether acct is an appointed owner via the grants.
         * will return false for global admin accounts
         */
        public static boolean isOwner(Account acct, Group group) throws ServiceException {
            // do not take into account admin privilege
            return AccessManager.getInstance().canAccessGroup(acct, group, false);
        }
        
        /*
         * returns whether acct has effective permission for the ownDistList rights.
         * will return true for global admin accounts
         */
        public static boolean hasOwnerPrivilege(Account acct, Group group) 
        throws ServiceException {
            // take into account admin privilege
            return AccessManager.getInstance().canAccessGroup(acct, group, true);
        }
        
        public static List<GroupOwner> getOwners(Group group, boolean needName) 
        throws ServiceException {
            List<GroupOwner> owners = new ArrayList<GroupOwner>();
            
            /*
             * No need to check rights granted on the domain or globalgrant,
             * The ownDistList can only be granted on group target.
             */
            List<ZimbraACE> acl = ACLUtil.getAllACEs(group);
            if (acl != null) {
                for (ZimbraACE ace : acl) {
                    Right right = ace.getRight();
                    if (GROUP_OWNER_RIGHT == right) {
                        owners.add(new GroupOwner(ace, needName));
                    }
                }
            }
            
            return owners;
        }
        
        public static void getOwnerEmails(Group group, Collection<String> result) 
        throws ServiceException {
            /*
             * No need to check rights granted on the doamin or globalgrant,
             * The ownDistList can only be granted on group target.
             */
            List<ZimbraACE> acl = ACLUtil.getACEs(group, Collections.singleton(GROUP_OWNER_RIGHT));
            if (acl != null) {
                for (ZimbraACE ace : acl) {
                    Right right = ace.getRight();
                    result.add(ace.getGranteeDisplayName());
                }
            }
        }

    }
}
