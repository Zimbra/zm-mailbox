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
package com.zimbra.cs.mailbox.acl;

import com.google.common.collect.ImmutableSet;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailboxListener;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.mailbox.ScheduledTask;
import com.zimbra.cs.mailbox.ScheduledTaskManager;
import com.zimbra.cs.session.PendingModifications.Change;

import java.util.Date;
import java.util.Set;

/**
 * Listens to ACL changes on folder and document items and schedules an {@link ExpireGrantsTask} for the item
 * if required.
 */
public class ShareExpirationListener extends MailboxListener {

    private static final ImmutableSet<MailItem.Type> registeredTypes = ImmutableSet.of(
            MailItem.Type.FOLDER, MailItem.Type.DOCUMENT
    );

    @Override
    public Set<MailItem.Type> registerForItemTypes() {
        return registeredTypes;
    }

    @Override
    public void notify(ChangeNotification notification) {
        if (notification.mods.created != null) {
            for (MailItem created : notification.mods.created.values()) {
                if (created instanceof Folder || created instanceof Document) {
                    if (created.getACL() != null) {
                        scheduleExpireAccessOpIfReq(created);
                    }
                }
            }
        }
        if (notification.mods.modified != null) {
            for (Change change : notification.mods.modified.values()) {
                if ((change.what instanceof Folder || change.what instanceof Document) && 
                        (change.why & Change.ACL) != 0 && change.op != MailboxOperation.ExpireAccess) {
                    scheduleExpireAccessOpIfReq((MailItem) change.what);
                }
            }
        }
    }

    static void scheduleExpireAccessOpIfReq(MailItem item) {
        // first cancel any existing task for this item
        try {
            ExpireGrantsTask.cancel(item.getMailboxId(), item.getId());
        } catch (ServiceException e) {
            ZimbraLog.scheduler.warn("Error in canceling existing ExpireGrantsTask for (id=%s,mailboxId=%s)", 
                    item.getId(), item.getMailboxId(), e);
            return;
        }
        ACL acl = item.getACL();
        if (acl == null) {
            return;
        }
        long nextExpiry = 0; // 0 indicates never expires
        for (ACL.Grant grant : acl.getGrants()) {
            long expiry = grant.getEffectiveExpiry(acl);
            if (expiry != 0) {
                nextExpiry = nextExpiry == 0 ? expiry : expiry < nextExpiry ? expiry : nextExpiry;
            }
        }
        if (nextExpiry == 0) {
            // there isn't any grant that's going to expire
            return;
        }
        ScheduledTask task = new ExpireGrantsTask();
        task.setMailboxId(item.getMailboxId());
        // schedule after a minute of the next expiry time
        task.setExecTime(new Date(nextExpiry + Constants.MILLIS_PER_MINUTE));
        task.setProperty(ExpireGrantsTask.ITEM_ID_PROP_NAME, Integer.toString(item.getId()));
        try {
            ScheduledTaskManager.schedule(task);
        } catch (ServiceException e) {
            ZimbraLog.scheduler.warn("Error in scheduling task %s", task.toString(), e);
        }
    }
}
