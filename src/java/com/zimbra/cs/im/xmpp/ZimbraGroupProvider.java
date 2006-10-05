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

package com.zimbra.cs.im.xmpp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.xmpp.packet.JID;

import com.zimbra.cs.im.xmpp.srv.group.Group;
import com.zimbra.cs.im.xmpp.srv.group.GroupAlreadyExistsException;
import com.zimbra.cs.im.xmpp.srv.group.GroupNotFoundException;
import com.zimbra.cs.im.xmpp.srv.group.GroupProvider;

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

    public Group getGroup(String name) throws GroupNotFoundException {
        throw new GroupNotFoundException();
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
}
