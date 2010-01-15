/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2010 Zimbra, Inc.
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

package com.zimbra.cs.im.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.wildfire.group.Group;
import org.jivesoftware.wildfire.group.GroupAlreadyExistsException;
import org.jivesoftware.wildfire.group.GroupNotFoundException;
import org.jivesoftware.wildfire.group.GroupProvider;
import org.xmpp.packet.JID;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.im.IMAddr;
import com.zimbra.cs.im.IMPersona;
import com.zimbra.cs.im.IMRouter;

public class ZimbraGroupProvider implements GroupProvider {

    public ZimbraGroupProvider() {
    }

    public void addMember(String groupName, JID user, boolean administrator) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
//        try {
//            IMPersona persona = IMRouter.getInstance().findPersona(null, new IMAddr(user));
//            if (persona != null) {
//                persona.providerGroupAdd(groupName);
//            }
//        } catch (ServiceException ex) {
//        }
    }

    public Group createGroup(String name) throws UnsupportedOperationException, GroupAlreadyExistsException {
        throw new UnsupportedOperationException();
    }

    public void deleteGroup(String name) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public void deleteMember(String groupName, JID user) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
//        try {
//            IMPersona persona = IMRouter.getInstance().findPersona(null, new IMAddr(user));
//            if (persona != null) {
//                persona.providerGroupRemove(groupName);
//            }
//        } catch (ServiceException ex) {
//        }
    }

    public Group getGroup(String name) throws GroupNotFoundException 
    {
        try {
            DistributionList dl = Provisioning.getInstance().get(Provisioning.DistributionListBy.name, name);
            if (dl != null) {
                ArrayList<JID> members = new ArrayList<JID>();
                ArrayList<JID> admins = new ArrayList<JID>();
                for (String member : dl.getAllMembers()) {
                    admins.add(new JID(member));
                }
                
                Group toRet = new Group(name, dl.getName(), members, admins);
                Map<String, String> propMap = toRet.getProperties();
                propMap.put("sharedRoster.showInRoster", "onlyGroup");
                propMap.put("sharedRoster.displayName", dl.getName());
                
                return toRet;
            }
        } catch (ServiceException ex) {
//            ZimbraLog.im.debug("IGNORED ServiceException (this is OK)"+ex, ex);
        }
        
        throw new GroupNotFoundException();
        
//        if ("all".equals(name)) {
//            ArrayList<JID> members = new ArrayList<JID>();
//            ArrayList<JID> admins = new ArrayList<JID>();
//            
//            members.add(new JID("user1@timsmac.local"));
//            members.add(new JID("user2@timsmac.local"));
//            members.add(new JID("user3@timsmac.local"));
//            members.add(new JID("user4@timsmac.local"));
////            members.add(new JID("tim@timsmac.local")); 
//            
//            Group toRet = new Group(name, "Everybody", members, admins);
//            
//            Map<String, String> propMap = toRet.getProperties();
//            
//            propMap.put("sharedRoster.showInRoster", "onlyGroup");
//            
//            return toRet;
//        } else {
//            throw new GroupNotFoundException();
//        }
    }
    
    public Set<String> getSharedGroupsNames() {
        HashSet<String> toRet = new HashSet<String>();
            
        if (false) {
        try {
            List<Domain> domains = Provisioning.getInstance().getAllDomains();
            if (domains != null) {
                for (Domain dm : domains) {
                    List<DistributionList> dls = Provisioning.getInstance().getAllDistributionLists(dm);
                    if (dls != null)
                        for (DistributionList dl : dls) {
                            toRet.add(dl.getName());
                        }
                }
            }
        } catch (ServiceException ex) {
            ZimbraLog.im.debug("Caught ServiceException "+ex, ex);
        }
        }
        
        return toRet;
    }

    /*
     * return the set of groups that this user belongs to
     */
    public Collection<String> getGroupNames(JID user) {
//        try {
//            ArrayList<String> toRet = new ArrayList<String>();
//            if (false) {
//            Account acct = Provisioning.getInstance().get(Provisioning.AccountBy.name,  user.toBareJID().toString());
//            IMPersona persona = IMRouter.getInstance().findPersona(null, new IMAddr(user));
//            if (persona != null) {
//                if (acct != null) {
//                    Set<String> distLists = Provisioning.getInstance().getDistributionLists(acct);
//                    for (String id: distLists) {
//                        DistributionList dl = Provisioning.getInstance().get(Provisioning.DistributionListBy.id, id);
//                        if (persona.inSharedGroup(dl.getName())) {
//                            toRet.add(dl.getName());
//                        }
//                    }
//                }
//            }
//            }
//            return toRet;
//        } catch (ServiceException ex) {
//            ZimbraLog.im.debug("Caught ServiceException "+ex, ex);
//        } 
        return new ArrayList<String>();
     }

    public boolean isReadOnly() {
        return false;
    }

    public void updateMember(String groupName, JID user, boolean administrator) throws UnsupportedOperationException {
        // do nothing (users are always administrators)
//        throw new UnsupportedOperationException();
        ZimbraLog.im.debug("Called updateMember on group: "+groupName+" for user: "+user.toBareJID());
    }

    public Collection<String> getGroupNames() {
        return new ArrayList<String>();
    }

    public Collection<String> getGroupNames(int startIndex, int numResults) {
        return new ArrayList<String>();
    }

    public int getGroupCount() {
        assert(false); // unused!
        return 0;
    }

    public Collection<Group> getGroups() {
        assert(false); // unused!
        return new ArrayList<Group>();
    }

    public Collection<Group> getGroups(Set<String> groupNames) {
        assert(false); // unused!
        return new ArrayList<Group>();
    }

    public Collection<Group> getGroups(int startIndex, int numResults) {
        assert(false); // unused!
        return new ArrayList<Group>();
    }

    public Collection<Group> getGroups(JID user) {
        assert(false); // unused!
        return new ArrayList<Group>();
    }

    public void setDescription(String name, String description) throws GroupNotFoundException {
        assert(false); // unused!
        throw new GroupNotFoundException();
    }

    public void setName(String oldName, String newName) throws UnsupportedOperationException, GroupAlreadyExistsException {
        assert(false); // unused!
        throw new UnsupportedOperationException();
    }
    public boolean isSearchSupported() {
        return false;
    }

    public Collection<String> search(String query) {
        return null;
    }

    public Collection<String> search(String query, int startIndex, int numResults) {
        return null;
    }
}
