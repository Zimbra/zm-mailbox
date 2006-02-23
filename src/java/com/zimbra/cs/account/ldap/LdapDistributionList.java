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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.ldap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.naming.NamingException;
import javax.naming.directory.AttributeInUseException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.NoSuchAttributeException;

import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.ServiceException;

public class LdapDistributionList extends LdapNamedEntry implements DistributionList {
    private String mName;
    private boolean mIsSecurityGroup;

    LdapDistributionList(String dn, Attributes attrs)
    {
        super(dn, attrs);
        mName = LdapUtil.dnToEmail(mDn);
        mIsSecurityGroup = hasZimbraSecurityGroupObjectClass();
    }

    public String getId() {
        return getAttr(Provisioning.A_zimbraId);
    }

    public String getGroupId() {
        return getAttr(Provisioning.A_zimbraGroupId);
    }

    public String getName() {
        return mName;
    }
    
    public boolean isSecurityGroup() throws ServiceException
    {
        return mIsSecurityGroup;
    }
    
    public void setSecurityGroup(boolean enabled) throws ServiceException
    {
        boolean updateOC =  (enabled && !hasZimbraSecurityGroupObjectClass()) ||
                            (!enabled && hasZimbraSecurityGroupObjectClass());
        
        String groupId = getGroupId();

        if (updateOC) {
            HashMap attrs = new HashMap();
            attrs.put((enabled ? "+" : "-")+Provisioning.A_objectClass, Provisioning.OC_zimbraSecurityGroup);
            if (!enabled) {
                attrs.put(Provisioning.A_zimbraMemberOf, "");
                attrs.put(Provisioning.A_zimbraGroupId, "");
                modifyAttrs(attrs);                
            } else {
                // need to do a search for all DLs this DL is on that are also security groups, and update
                // our zimbraMemberOf attr with their zimbraIds
                groupId = UUID.randomUUID().toString();
                attrs.put(Provisioning.A_zimbraGroupId, groupId);
                modifyAttrs(attrs);                
                String addrs[] = LdapProvisioning.getAllAddrsForDistributionList(this);
                List<DistributionList> lists = Provisioning.getInstance().getAllDistributionListsForAddresses(addrs);
                ArrayList memberOf = null;
                for (DistributionList list: lists) { 
                    if (list.isSecurityGroup()) {
                        if (memberOf == null) memberOf = new ArrayList();
                        memberOf.add(list.getGroupId());
                    }
                }
                if (memberOf != null) {
                    String members[] = (String[]) memberOf.toArray(new String[memberOf.size()]);
                    attrs.put(Provisioning.A_zimbraMemberOf, members);
                }
            }
            
        }
        
        String members[] = getAllMembers();
        if (members == null || members.length == 0) return;
        for (int i=0; i < members.length; i++)
            updateZimbraMemberOf(members[i], groupId, enabled);
    }

    public void addMember(String member) throws ServiceException {
        member = member.toLowerCase();
        String[] parts = member.split("@");
        if (parts.length != 2) {
            throw ServiceException.INVALID_REQUEST("must be valid member email address: " + member, null);
        }
        
        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext(true);
            addAttr(ctxt, Provisioning.A_zimbraMailForwardingAddress, member);
            if (isSecurityGroup()) updateZimbraMemberOf(member, getGroupId(), true);
        } catch (AttributeInUseException aiue) {
            throw AccountServiceException.MEMBER_EXISTS(getName(), member, aiue);
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
            ctxt = LdapUtil.getDirContext(true);
            removeAttr(ctxt, Provisioning.A_zimbraMailForwardingAddress, member);
            if (isSecurityGroup()) updateZimbraMemberOf(member, getGroupId(), false);
        } catch (NoSuchAttributeException nsae) {
            throw AccountServiceException.NO_SUCH_MEMBER(getName(), member, nsae);
        } catch (NamingException ne) {
            throw ServiceException.FAILURE("remove failed for member: " + member, ne);
        } finally {
            LdapUtil.closeContext(ctxt);
        }
    }

    private boolean inMemberOf(NamedEntry e, String groupId) {
        String[] memberOf = e.getMultiAttr(Provisioning.A_zimbraMemberOf);
        for (int i=0; i < memberOf.length; i++)
            if (groupId.equalsIgnoreCase(memberOf[i])) return true;
        return false;
    }
    
    private boolean hasZimbraSecurityGroupObjectClass() {
        String[] oc = getMultiAttr(Provisioning.A_objectClass);
        for (int i=0; i < oc.length; i++)
            if (Provisioning.OC_zimbraSecurityGroup.equalsIgnoreCase(oc[i])) return true;
        return false;
    }
    
    private void updateZimbraMemberOf(String member, String groupId, boolean add) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        NamedEntry entry = prov.getAccountByName(member);
        if (entry == null){
            DistributionList dl = prov.getDistributionListByName(member);
            if (dl == null || !dl.isSecurityGroup()) return;
            entry = dl;
        }

        if (add) {
            if (!inMemberOf(entry, groupId)) {
                HashMap attrs = new HashMap();
                attrs.put("+"+Provisioning.A_zimbraMemberOf, groupId);
                entry.modifyAttrs(attrs);
            }
        } else {
            if (inMemberOf(entry, groupId)) {
                HashMap attrs = new HashMap();
                attrs.put("-"+Provisioning.A_zimbraMemberOf, groupId);
                entry.modifyAttrs(attrs);
            }
        }
    }
    
    public String[] getAllMembers() throws ServiceException {
        return getMultiAttr(Provisioning.A_zimbraMailForwardingAddress);
    }
    
    public String[] getAliases() throws ServiceException
    {
        return getMultiAttr(Provisioning.A_zimbraMailAlias);
    }

}
