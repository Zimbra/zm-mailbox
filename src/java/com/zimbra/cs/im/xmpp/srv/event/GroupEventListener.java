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

package com.zimbra.cs.im.xmpp.srv.event;

import com.zimbra.cs.im.xmpp.srv.group.Group;

import java.util.Map;

/**
 * Interface to listen for group events. Use the
 * {@link GroupEventDispatcher#addListener(GroupEventListener)}
 * method to register for events.
 *
 * @author Matt Tucker
 */
public interface GroupEventListener {

    /**
     * A group was created.
     *
     * @param group the group.
     * @param params event parameters.
     */
    public void groupCreated(Group group, Map params);

    /**
     * A group is being deleted.
     *
     * @param group the group.
     * @param params event parameters.
     */
    public void groupDeleting(Group group, Map params);

    /**
     * A group's name, description, or an extended property was changed.
     *
     * @param group the group.
     * @param params event parameters.
     */
    public void groupModified(Group group, Map params);

    /**
     * A member was added to a group.
     *
     * @param group the group.
     * @param params event parameters.
     */
    public void memberAdded(Group group, Map params);

    /**
     * A member was removed from a group.
     *
     * @param group the group.
     * @param params event parameters.
     */
    public void memberRemoved(Group group, Map params);

    /**
     * An administrator was added to a group.
     *
     * @param group the group.
     * @param params event parameters.
     */
    public void adminAdded(Group group, Map params);

    /**
     * An administrator was removed from a group.
     *
     * @param group the group.
     * @param params event parameters.
     */
    public void adminRemoved(Group group, Map params);
}