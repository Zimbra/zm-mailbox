/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import com.zimbra.common.service.ServiceException;

import java.util.Map;
import java.util.Set;

public abstract class DistributionList extends ZAttrDistributionList implements GroupedEntry {

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
        for (int i=0; i < aliases.length; i++) {
            addrs[i+1] = aliases[i];
        }
        return addrs;
    }

}
