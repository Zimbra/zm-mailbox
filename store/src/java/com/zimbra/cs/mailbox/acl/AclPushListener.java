/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox.acl;

import java.util.Collections;
import java.util.Set;

import com.zimbra.common.mailbox.BaseItemInfo;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailboxListener;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.util.Zimbra;

/**
 * Listens to folder changes and schedules immediate run of {@link AclPushTask} in a separate thread if required.
 */
public class AclPushListener extends MailboxListener {

    private static final Set<MailItem.Type> registeredTypes = Collections.singleton(MailItem.Type.FOLDER);

    @Override
    public Set<MailItem.Type> registerForItemTypes() {
        return registeredTypes;
    }

    @Override
    public void notify(ChangeNotification notification) {
        boolean runAclPushTask = false;
        if (notification.mods.created != null) {
            for (BaseItemInfo created : notification.mods.created.values()) {
                if (created instanceof Folder) {
                    Folder folder = (Folder) created;
                    if (folder.getACL() != null) {
                        runAclPushTask = true;
                        break;
                    }
                }
            }
        }
        if (!runAclPushTask && notification.mods.modified != null) {
            for (Change change : notification.mods.modified.values()) {
                // we also need to check for folder rename and move
                if (change.what instanceof Folder && (change.why & (Change.ACL | Change.NAME | Change.PARENT)) != 0) {
                    runAclPushTask = true;
                    break;
                }
            }
        }
        if (!runAclPushTask && notification.mods.deleted != null) {
            for (Change change : notification.mods.deleted.values()) {
                if (change.preModifyObj == null) {
                    if (change.what == MailItem.Type.FOLDER) {
                        // not sure if the deleted folder had an ACL
                        runAclPushTask = true;
                        break;
                    }
                } else if (change.preModifyObj instanceof Folder && ((Folder) change.preModifyObj).getACL() != null) {
                    runAclPushTask = true;
                    break;
                }
            }
        }
        if (runAclPushTask) {
            // run in separate thread to avoid ldap communication inside mailbox lock
            Zimbra.sTimer.schedule(new AclPushTask(), 0);
        }
    }
}
