/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.  Portions
 * created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
 * Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.ldap;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.ServiceException;

public class LdapDistributionList extends LdapNamedEntry implements DistributionList {
    private String mName;

    LdapDistributionList(String dn, Attributes attrs)
    {
        super(dn, attrs);
        mName = LdapUtil.dnToEmail(mDn);
    }

    public String getId() {
        return getAttr(Provisioning.A_zimbraId);
    }

    public String getName() {
        return mName;
    }

    public void addMember(String member) throws ServiceException {
        member = member.toLowerCase();
        String[] parts = member.split("@");
        if (parts.length != 2) {
            throw ServiceException.INVALID_REQUEST("must be valid member email address: " + member, null);
        }
        
        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext();
            addAttr(ctxt, Provisioning.A_zimbraMailForwardingAddress, member);
        } catch (NamingException ne) {
            throw ServiceException.FAILURE("add failed for member: " + member, ne);
        } finally {
            LdapUtil.closeContext(ctxt);
        }
    }

    public void removeMember(String member) throws ServiceException {
        member = member.toLowerCase();
        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext();
            removeAttr(ctxt, Provisioning.A_zimbraMailForwardingAddress, member);
        } catch (NamingException ne) {
            throw ServiceException.FAILURE("remove failed for member: " + member, ne);
        } finally {
            LdapUtil.closeContext(ctxt);
        }
    }

    public String[] getAllMembers() throws ServiceException {
        return getMultiAttr(Provisioning.A_zimbraMailForwardingAddress);
    }
}
