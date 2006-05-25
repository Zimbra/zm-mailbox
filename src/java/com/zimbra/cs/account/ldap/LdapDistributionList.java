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

import java.util.List;
import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.AttributeInUseException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.NoSuchAttributeException;

import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.ServiceException;

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
    
    private void validateMembers(String[] members) throws ServiceException {
        for (int i = 0; i < members.length; i++) { 
        	members[i] = members[i].toLowerCase();
        	String[] parts = members[i].split("@");
        	if (parts.length != 2) {
        		throw ServiceException.INVALID_REQUEST("invalid member email address: " + members[i], null);
        	}
        }
    }
    
    public void addMembers(String[] members) throws ServiceException {
        validateMembers(members);
        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext(true);
            addAttrMulti(ctxt, Provisioning.A_zimbraMailForwardingAddress, members);
        } catch (AttributeInUseException aiue) {
            throw AccountServiceException.MEMBER_EXISTS(getName(), aiue);
        } catch (NamingException ne) {
            throw ServiceException.FAILURE("error adding to distribution list: " + getName(), ne);
        } finally {
            LdapUtil.closeContext(ctxt);
        }
    }

    public void removeMembers(String[] members) throws ServiceException {
        validateMembers(members);
        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext(true);
            removeAttrMulti(ctxt, Provisioning.A_zimbraMailForwardingAddress, members);
        } catch (NoSuchAttributeException nsae) {
            throw AccountServiceException.NO_SUCH_MEMBER(getName(), "attempted to remove non-existent member", nsae);
        } catch (NamingException ne) {
            throw ServiceException.FAILURE("error removing from distribution list: " + getName(), ne);
        } finally {
            LdapUtil.closeContext(ctxt);
        }
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
