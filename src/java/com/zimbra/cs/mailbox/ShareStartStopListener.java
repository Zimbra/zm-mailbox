/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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
package com.zimbra.cs.mailbox;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ShareLocator;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.util.Zimbra;

public class ShareStartStopListener extends MailboxListener {

    private static final Set<MailItem.Type> registeredTypes = Collections.singleton(MailItem.Type.FOLDER);

    @Override
    public Set<MailItem.Type> registerForItemTypes() {
        return registeredTypes;
    }

    @Override
    public void notify(ChangeNotification notification) {
        if (notification.mods.created != null) {
            // A new folder with non-empty ACL means start of sharing.
            for (MailItem created : notification.mods.created.values()) {
                if (created instanceof Folder) {
                    Folder folder = (Folder) created;
                    if (folder.getACL() != null) {
                        startShare(folder);
                    }
                }
            }
        }
        if (notification.mods.modified != null) {
            // ACL change on folder can mean start or stop of sharing.
            for (Change change : notification.mods.modified.values()) {
                if ((change.why & Change.ACL) != 0 &&
                        change.preModifyObj instanceof Folder && change.what instanceof Folder) {
                    Folder before = (Folder) change.preModifyObj;
                    Folder after = (Folder) change.what;
                    boolean beforeHasACL = before.getACL() != null;
                    boolean afterHasACL = after.getACL() != null;
                    if (!beforeHasACL && afterHasACL) {
                        startShare(after);
                    } else if (beforeHasACL && !afterHasACL) {
                        stopShare(after);
                    }
                    // Note: No attempt is made to start/stop share based on the folder moving into
                    // or out of trash/spam folder.  This is because no notification is generated
                    // when the folder crosses the boundary by virtue of one of its parent folders moving.
                }
            }
        }
        if (notification.mods.deleted != null) {
            // Deletion of a folder with non-empty ACL stops sharing.
            for (Change change : notification.mods.deleted.values()) {
                Folder folder = null;
                if (change.what instanceof Folder) {
                    folder = (Folder) change.what;
                } else if (change.preModifyObj instanceof Folder) {
                    folder = (Folder) change.preModifyObj;
                }
                if (folder != null && folder.getACL() != null) {
                    stopShare(folder);
                }
            }
        }
    }

    // Add the share locator entry for this folder.
    private void startShare(Folder folder) {
        final String me = folder.getMailbox().getAccountId();
        final String uuid = folder.getUuid();
        TimerTask t = new TimerTask() {
            @Override
            public void run() {
                try {
                    Provisioning prov = Provisioning.getInstance();
                    ShareLocator shloc = prov.getShareLocatorById(uuid);
                    if (shloc == null) {
                        prov.createShareLocator(uuid, me);
                    } else {
                        // Get the latest value from ldap master to avoid race condition during share relocation.
                        prov.reload(shloc, true);
                        // Skip if locator already points to this account.
                        if (!me.equalsIgnoreCase(shloc.getShareOwnerAccountId())) {
                            // Change owner to this account.
                            Map<String, Object> attrs = new HashMap<String, Object>();
                            attrs.put(Provisioning.A_zimbraShareOwnerAccountId, me);
                            prov.modifyAttrs(shloc, attrs);
                        }
                    }
                } catch (Throwable t) {  //don't let exceptions kill the timer
                    ZimbraLog.share.warn("error while processing share start notification", t);
                }
            }
        };
        Zimbra.sTimer.schedule(t, 0);  // run in separate thread to avoid ldap communication inside mailbox lock
    }

    // Remove the share locator entry for this folder.
    private void stopShare(Folder folder) {
        final String me = folder.getMailbox().getAccountId();
        final String uuid = folder.getUuid();
        TimerTask t = new TimerTask() {
            @Override
            public void run() {
                try {
                    Provisioning prov = Provisioning.getInstance();
                    ShareLocator shloc = prov.getShareLocatorById(uuid);
                    if (shloc != null) {
                        // Get the latest value from ldap master to avoid race condition during share relocation.
                        prov.reload(shloc, true);
                        // Delete locator only if it's not claimed by another account.  (i.e. share moved)
                        if (me.equalsIgnoreCase(shloc.getShareOwnerAccountId())) {
                            prov.deleteShareLocator(uuid);
                        }
                    }
                } catch (Throwable t) {  //don't let exceptions kill the timer
                    ZimbraLog.share.warn("error while processing share stop notification", t);
                }
            }
        };
        Zimbra.sTimer.schedule(t, 0);  // run in separate thread to avoid ldap communication inside mailbox lock
    }
}
