/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.account.ldap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.EntryCacheDataKey;
import com.zimbra.cs.account.IDNUtil;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.common.service.ServiceException;

class LdapDistributionList extends DistributionList implements LdapEntry {
    private String mDn;

    LdapDistributionList(String dn, String email, Attributes attrs, Provisioning prov) throws NamingException {
        super(email,
              LdapUtil.getAttrString(attrs, Provisioning.A_zimbraId), 
              LdapUtil.getAttrs(attrs), prov);
        mDn = dn;
    }
    
    void addMembers(String[] members, LdapProvisioning prov) throws ServiceException {
        Set<String> existing = getMultiAttrSet(Provisioning.A_zimbraMailForwardingAddress);
        Set<String> mods = new HashSet<String>();
        
        // all addrs of thie DL
        AddrsOfEntry addrsOfDL = getAllAddressesOfEntry(prov, getName());
        
        for (int i = 0; i < members.length; i++) { 
            String memberName = members[i].toLowerCase();
            memberName = IDNUtil.toAsciiEmail(memberName);
            
            if (addrsOfDL.isIn(memberName))
                throw ServiceException.INVALID_REQUEST("Cannot add self as a member: " + memberName, null);
            
            if (!existing.contains(memberName)) {
                mods.add(memberName);

                // clear the DL cache on accounts/dl

                // can't do prov.getFromCache because it only caches by primary name 
                Account acct = prov.get(AccountBy.name, memberName);
                if (acct != null)
                    clearUpwardMembershipCache(acct);
                else {
                    // for DistributionList/ACLGroup, get it from cache because 
                    // if the dl is not in cache, after loading it prov.getAclGroup 
                    // always compute the upward membership.  Sounds silly if we are 
                    // about to clean the cache.  If memberName is indeed an alias 
                    // of one of the cached DL/ACLGroup, it will get expired after 15 
                    // mins, just like the multi-node case. 
                    //
                    // Note: do not call clearUpwardMembershipCache for AclGroup because
                    // the upward membership cache for that is computed and cache only when 
                    // the entry is loaded/being cached, instead of lazily computed like we 
                    // do for account.
                    prov.removeGroupFromCache(DistributionListBy.name, memberName);
                }
            }
        }

        if (mods.isEmpty()) {
        	// nothing to do...
        	return;
        }
        
        Map<String,String[]> modmap = new HashMap<String,String[]>();
        modmap.put("+" + Provisioning.A_zimbraMailForwardingAddress, (String[])mods.toArray(new String[0]));
        prov.modifyAttrs(this, modmap, true);
    }

    void removeMembers(String[] members, LdapProvisioning prov) throws ServiceException {
        Set<String> curMembers = getMultiAttrSet(Provisioning.A_zimbraMailForwardingAddress);
        
        // bug 46219, need a case insentitive Set
        Set<String> existing = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        existing.addAll(curMembers);
        
        Set<String> mods = new HashSet<String>();
        HashSet<String> failed = new HashSet<String>();

        for (int i = 0; i < members.length; i++) { 
            String memberName = members[i].toLowerCase();
            memberName = IDNUtil.toAsciiEmail(memberName);
            if (memberName.length() == 0) {
                throw ServiceException.INVALID_REQUEST("invalid member email address: " + memberName, null);
            }
            // We do not do any further validation of the remove address for
            // syntax - removes should be liberal so any bad entries added by
            // some other means can be removed
            //
            // members[] can contain:
            //   - the primary address of an account or another DL
            //   - an alias of an account or another DL
            //   - junk (allAddrs will be returned as null)
            AddrsOfEntry addrsOfEntry = getAllAddressesOfEntry(prov, memberName);
            List<String> allAddrs = addrsOfEntry.getAll();
                
            if (mods.contains(memberName)) {
                // already been added in mods (is the primary or alias of previous entries in members[])
            } else if (existing.contains(memberName)) {
                if (allAddrs.size() > 0)
                    mods.addAll(allAddrs);
                else
                    mods.add(memberName);  // just get rid of it regardless what it is
            } else {
                boolean inList = false;
                if (allAddrs.size() > 0) {
                    // go through all addresses of the entry see if any is on the DL
                    for (Iterator it = allAddrs.iterator(); it.hasNext() && !inList; ) {
                        String addr = (String)it.next();
                        if (existing.contains(addr)) {
                            mods.addAll(allAddrs);
                            inList = true;
                        }
                    }
                }
                if (!inList)
                    failed.add(memberName);
            }
            
            // clear the DL cache on accounts/dl
            String primary = addrsOfEntry.getPrimary();
            if (primary != null) {
                if (addrsOfEntry.isAccount()) {
                    Account acct = prov.getFromCache(AccountBy.name, primary);
                    if (acct != null)
                        clearUpwardMembershipCache(acct);
                } else {
                    prov.removeGroupFromCache(DistributionListBy.name, primary);
                }
            }
        }

        if (!failed.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            Iterator<String> iter = failed.iterator();
            while (true) {
                sb.append(iter.next());
                if (!iter.hasNext())
                    break;
                sb.append(",");
            }
            throw AccountServiceException.NO_SUCH_MEMBER(getName(), sb.toString());
        }

        if (mods.isEmpty()) {
            throw ServiceException.INVALID_REQUEST("empty remove set", null);
        }
        
        Map<String,String[]> modmap = new HashMap<String,String[]>();
        modmap.put("-" + Provisioning.A_zimbraMailForwardingAddress, (String[])mods.toArray(new String[0]));
        prov.modifyAttrs(this, modmap);

    }

    private void clearUpwardMembershipCache(Account acct) {
        acct.setCachedData(LdapProvisioning.DATA_DL_SET, null);
        acct.setCachedData(LdapProvisioning.DATA_ACLGROUP_LIST, null);
        acct.setCachedData(LdapProvisioning.DATA_ACLGROUP_LIST_ADMINS_ONLY, null);
        acct.setCachedData(EntryCacheDataKey.GROUPEDENTRY_DIRECT_GROUPIDS.getKeyName(), null);
    }
    
    public String getDN() {
        return mDn;
    }
    
    class AddrsOfEntry {
        List<String> mAllAddrs = new ArrayList<String>(); // including primary
        String mPrimary = null;  // primary addr
        boolean mIsAccount = false;
        
        void setPrimary(String primary) {
            mPrimary = primary;
            add(primary);
        }
        
        void setIsAccount(boolean isAccount) {
            mIsAccount = isAccount;
        }

        void add(String addr) {
            mAllAddrs.add(addr);
        }
        
        void addAll(String[] addrs) {
            mAllAddrs.addAll(Arrays.asList(addrs));
        }
        
        List<String> getAll() {
            return mAllAddrs;
        }
        
        String getPrimary() {
            return mPrimary;
        }
        
        boolean isAccount() {
            return mIsAccount;
        }
        
        int size() {
            return mAllAddrs.size();
        }
        
        boolean isIn(String addr) {
            return mAllAddrs.contains(addr.toLowerCase());
        }
    }
    
    
    //
    // returns the primary address and all aliases of the named account or DL 
    //
    private AddrsOfEntry getAllAddressesOfEntry(LdapProvisioning prov, String name) {
        
        String primary = null;
        String aliases[] = null;
        AddrsOfEntry addrs = new AddrsOfEntry();
        
        try {
            Account acct = prov.get(Provisioning.AccountBy.name, name);
            if (acct != null) {
                addrs.setIsAccount(true);
                primary = acct.getName();
                aliases = acct.getMailAlias();
            } else {
                DistributionList dl = prov.get(Provisioning.DistributionListBy.name, name);
                if (dl != null) {
                    primary = dl.getName();
                    aliases = dl.getAliases();
                }
            }
        } catch (ServiceException se) {
            // swallow any exception and go on
        }
        
        if (primary != null)
            addrs.setPrimary(primary);
        if (aliases != null)
            addrs.addAll(aliases);
               
        return addrs;
    }

}
