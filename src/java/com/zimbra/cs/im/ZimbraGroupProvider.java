/* ***** BEGIN LICENSE BLOCK *****
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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.im;

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

public class ZimbraGroupProvider implements GroupProvider {

    public ZimbraGroupProvider() {
    }

    public void addMember(String groupName, JID user, boolean administrator) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public Group createGroup(String name) throws UnsupportedOperationException, GroupAlreadyExistsException {
        throw new UnsupportedOperationException();
    }

    public void deleteGroup(String name) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public void deleteMember(String groupName, JID user) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public Group getGroup(String name) throws GroupNotFoundException 
    {
        try {
            DistributionList dl = Provisioning.getInstance().get(Provisioning.DistributionListBy.name, name);

            ArrayList<JID> members = new ArrayList<JID>();
            ArrayList<JID> admins = new ArrayList<JID>();
            
            if (dl != null) {
                for (String member : dl.getAllMembers()) {
                    members.add(new JID(member));
                }
                
                Group toRet = new Group(name, dl.getName(), members, admins);
                Map<String, String> propMap = toRet.getProperties();
                propMap.put("sharedRoster.showInRoster", "onlyGroup");
                propMap.put("sharedRoster.displayName", dl.getName());
                
                return toRet;
            }
        } catch (ServiceException ex) {
            ZimbraLog.im.debug("Caught ServiceException "+ex, ex);
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
        if (false) { // disable this until testing is completed
            HashSet<String> toRet = new HashSet<String>();
            
            try {
                List<Domain> domains = Provisioning.getInstance().getAllDomains();
                for (Domain dm : domains) {
                    List<DistributionList> dls = Provisioning.getInstance().getAllDistributionLists(dm);
                    for (DistributionList dl : dls) {
                        toRet.add(dl.getName());
                    }
                }
            } catch (ServiceException ex) {
                ZimbraLog.im.debug("Caught ServiceException "+ex, ex);
            }
            
            return toRet;
        }
        return new HashSet<String>();
    }

    /*
     * return the set of groups that this user belongs to
     */
    public Collection<String> getGroupNames(JID user) {
        try {
            Account acct = Provisioning.getInstance().get(Provisioning.AccountBy.name,  user.toBareJID().toString());
            
            ArrayList<String> toRet = new ArrayList<String>();
            
            if (acct != null) {
                Set<String> distLists = Provisioning.getInstance().getDistributionLists(acct);
                String DLs = "";
                for (String id: distLists) {
                    DistributionList dl = Provisioning.getInstance().get(Provisioning.DistributionListBy.id, id);
                    
                    toRet.add(dl.getName());
                    DLs = dl.getName() + " " + DLs;
                }
            }
            return toRet;
        } catch (ServiceException ex) {
            ZimbraLog.im.debug("Caught ServiceException "+ex, ex);
        } 
        return new ArrayList<String>();
     }

    public int getGroupCount() {
        return 0;
    }

    public Collection<Group> getGroups() {
        return new ArrayList<Group>();
    }

    public Collection<Group> getGroups(Set<String> groupNames) {
        return new ArrayList<Group>();
    }

    public Collection<Group> getGroups(int startIndex, int numResults) {
        return new ArrayList<Group>();
    }

    public Collection<Group> getGroups(JID user) {
        return new ArrayList<Group>();
    }

    public boolean isReadOnly() {
        return true;
    }

    public void setDescription(String name, String description) throws GroupNotFoundException {
        throw new GroupNotFoundException();
    }

    public void setName(String oldName, String newName) throws UnsupportedOperationException, GroupAlreadyExistsException {
        throw new UnsupportedOperationException();
    }

    public void updateMember(String groupName, JID user, boolean administrator) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public Collection<String> getGroupNames() {
        return new ArrayList<String>();
    }

    public Collection<String> getGroupNames(int startIndex, int numResults) {
        return new ArrayList<String>();
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
