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

package com.zimbra.cs.account;

import com.zimbra.common.service.ServiceException;

import java.util.Map;
import java.util.Set;

public class DistributionList extends ZAttrDistributionList implements GroupedEntry {

    protected static final String MEMBER_ATTR = Provisioning.A_zimbraMailForwardingAddress;
    
    public DistributionList(String name, String id, Map<String, Object> attrs, Provisioning prov) {
        super(name, id, attrs, prov);
    }
    
    @Override
    public EntryType getEntryType() {
        return EntryType.DISTRIBUTIONLIST;
    }
    
    @Override
    public boolean isDynamic() {
        return false;
    }
    
    @Override
    public Domain getDomain() throws ServiceException {
        return getProvisioning().getDomain(this);
    }

    public void modify(Map<String, Object> attrs) throws ServiceException {
        getProvisioning().modifyAttrs(this, attrs);
    }

    public void deleteDistributionList() throws ServiceException {
        getProvisioning().deleteDistributionList(getId());
    }

    public void addAlias(String alias) throws ServiceException {
        getProvisioning().addAlias(this, alias);
    }

    public void removeAlias(String alias) throws ServiceException {
        getProvisioning().removeAlias(this, alias);
    }

    public void renameDistributionList(String newName) throws ServiceException {
        getProvisioning().renameDistributionList(getId(), newName);
    }

    public void addMembers(String[] members) throws ServiceException {
        getProvisioning().addMembers(this, members);
    }

    public void removeMembers(String[] member) throws ServiceException {
        getProvisioning().removeMembers(this, member);
    }
    
    @Override  // overriden in LdapDistributionList
    public String[] getAllMembers() throws ServiceException {
        return getMultiAttr(MEMBER_ATTR);
    }
    
    @Override  // overriden in LdapDistributionList
    public Set<String> getAllMembersSet() throws ServiceException {
        return getMultiAttrSet(MEMBER_ATTR);
    }
    
    @Override
    public String[] getAliases() throws ServiceException {
        return getMultiAttr(Provisioning.A_zimbraMailAlias);
    }
    
    @Override
    protected void resetData() {
        super.resetData();
    }

    @Override
    public String[] getAllAddrsAsGroupMember() throws ServiceException {
        String aliases[] = getAliases();
        String addrs[] = new String[aliases.length+1];
        addrs[0] = getName();
        for (int i=0; i < aliases.length; i++)
            addrs[i+1] = aliases[i];
        return addrs;
    }
    


}
