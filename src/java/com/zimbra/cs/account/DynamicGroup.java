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
package com.zimbra.cs.account;

import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;

public class DynamicGroup extends ZAttrDynamicGroup {
    
    public DynamicGroup(String name, String id, Map<String, Object> attrs, Provisioning prov) {
        super(name, id, attrs, prov);
    }

    public EntryType getEntryType() {
        return EntryType.DYNAMICGROUP;
    }
    
    public static String getDefaultMemberURL(String zimbraId) {
        return String.format("ldap:///??sub?(zimbraMemberOf=%s)", zimbraId);     
    }
    
    public String getDefaultMemberURL() {
        return getDefaultMemberURL(getId());
    }
    
    @Override
    public boolean isDynamic() {
        return true;
    }
    
    @Override
    public Domain getDomain() throws ServiceException {
        return getProvisioning().getDomain(this);
    }
    
    @Override  // Override in LdapDynamicGroup and SoapDynamicGroup
    public String[] getAllMembers() throws ServiceException {
        return getMultiAttr(Provisioning.A_member);
    }
    
    @Override  // overriden on LdapDynamicGroup
    public Set<String> getAllMembersSet() throws ServiceException {
        return getMultiAttrSet(Provisioning.A_member);
    }
    
    @Override
    public String[] getAliases() throws ServiceException {
        return getMailAlias();
    }

    /*
     * Override in LdapDynamicGroup
     *  
     * Default implementation is calling getAllMembers() regardless 
     * of supportNonDefaultMemberURL.
     * 
     * Should only be called from the edge: ProvUtil or adminNamespace
     * GetDistributuionList.  If supportNonDefaultMemberURL is true,
     * this call can be very expensive.
     */
    public String[] getAllMembers(boolean supportNonDefaultMemberURL) 
    throws ServiceException {
        return getAllMembers();
    }
}
