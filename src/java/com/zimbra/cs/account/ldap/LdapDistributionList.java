/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
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

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.EmailUtil;

class LdapDistributionList extends DistributionList implements LdapEntry {
    private String mDn;

    LdapDistributionList(String dn, Attributes attrs) throws NamingException {
        super(LdapUtil.dnToEmail(dn),
                LdapUtil.getAttrString(attrs, Provisioning.A_zimbraId), 
                LdapUtil.getAttrs(attrs));
        mDn = dn;
    }
    
    void addMembers(String[] members, LdapProvisioning prov) throws ServiceException {
    	Set<String> existing = getMultiAttrSet(Provisioning.A_zimbraMailForwardingAddress);
    	Set<String> mods = new HashSet<String>();
    	
        for (int i = 0; i < members.length; i++) { 
        	members[i] = members[i].toLowerCase();
        	String[] parts = members[i].split("@");
        	if (parts.length != 2) {
        		throw ServiceException.INVALID_REQUEST("invalid member email address: " + members[i], null);
        	}
        	if (!EmailUtil.validDomain(parts[1])) {
        		throw ServiceException.INVALID_REQUEST("invalid domain in member email address: " + members[i], null);
        	}
        	if (!existing.contains(members[i])) {
        		mods.add(members[i]);
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
    	Set<String> existing = getMultiAttrSet(Provisioning.A_zimbraMailForwardingAddress);
    	Set<String> mods = new HashSet<String>();
    	HashSet<String> failed = new HashSet<String>();
    	
        for (int i = 0; i < members.length; i++) { 
            members[i] = members[i].toLowerCase();
            if (members[i].length() == 0) {
                throw ServiceException.INVALID_REQUEST("invalid member email address: " + members[i], null);
            }
            // We do not do any further validation of the remove address for
            // syntax - removes should be liberal so any bad entries added by
            // some other means can be removed
            //
            // members[] can contain:
            //   - the primary address of an account or another DL
            //   - an alias of an account or another DL
            //   - junk (allAddrs will be returned as null)
            List<String> allAddrs = getAllAddressesOfEntry(prov, members[i]);
            
            if (mods.contains(members[i])) {
                // already been added in mods (is the primary or alias of previous entries in members[])
            } else if (existing.contains(members[i])) {
                if (allAddrs != null)
                    mods.addAll(allAddrs);
                else
                    mods.add(members[i]);  // just get rid of it regardless what it is
            } else {
                boolean inList = false;
                if (allAddrs != null) {
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
                    failed.add(members[i]);
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

    public String getDN() {
        return mDn;
    }
    
    //
    // returns the primary address and all aliases of the named account or DL 
    //
    private List<String> getAllAddressesOfEntry(LdapProvisioning prov, String name) {
        
        List<String> addrs = new ArrayList<String>();
        String primary = null;
        String aliases[] = null;
        
        try {
            Account acct = prov.get(Provisioning.AccountBy.name, name);
            if (acct != null) {
                primary = acct.getName();
                aliases = acct.getAliases();
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
            addrs.add(primary);
        if (aliases != null)
            addrs.addAll(Arrays.asList(aliases));
        
        if (addrs.size() > 0)
            return addrs;
        else
            return null;
    }

}
