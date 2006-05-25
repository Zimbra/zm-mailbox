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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.ldap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.directory.Attributes;

import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.EmailUtil;

public class LdapDistributionList extends LdapNamedEntry implements DistributionList {
    private String mName;

    LdapDistributionList(String dn, Attributes attrs) {
        super(dn, attrs);
        mName = LdapUtil.dnToEmail(mDn);
    }

    public String getId() {
        return getAttr(Provisioning.A_zimbraId);
    }

    public String getName() {
        return mName;
    }
    
    public void addMembers(String[] members) throws ServiceException {
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
        modifyAttrs(modmap);
    }

    public void removeMembers(String[] members) throws ServiceException {
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
        	if (existing.contains(members[i])) {
            	mods.add(members[i]);
        	} else {
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
        modifyAttrs(modmap);
    }

    public String[] getAllMembers() {
        return getMultiAttr(Provisioning.A_zimbraMailForwardingAddress);
    }
    
    public String[] getAliases() {
        return getMultiAttr(Provisioning.A_zimbraMailAlias);
    }

    public List<DistributionList> getDistributionLists(boolean directOnly, Map<String, String> via) throws ServiceException {
        String addrs[] = LdapProvisioning.getAllAddrsForDistributionList(this);
        return LdapProvisioning.getDistributionLists(addrs, directOnly, via, false);
    }
}
