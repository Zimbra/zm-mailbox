/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;

import com.google.common.collect.Multimap;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.ShareInfoData;
import com.zimbra.cs.db.DbPendingAclPush;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.util.ZimbraApplication;

/**
 * This task publishes shared item updates to LDAP to enable centralized discovery of shares, e.g.
 * to discover all shares accessible to a particular account.
 */
public class AclPushTask extends TimerTask {

    private static boolean supported;

    static {
         supported = ZimbraApplication.getInstance().supports(AclPushTask.class);
    }

    @Override
    public void run() {
        doWork();
    }

    public static synchronized void doWork() {
        if (!supported)
            return;
        ZimbraLog.misc.debug("Starting pending ACL push");
        try {
            Date now = new Date();
            Multimap<Integer, Integer> mboxIdToItemIds = DbPendingAclPush.getEntries(now);

            for (int mboxId : mboxIdToItemIds.keySet()) {
                Mailbox mbox;
                try {
                    mbox = MailboxManager.getInstance().getMailboxById(mboxId);
                } catch (ServiceException e) {
                    ZimbraLog.misc.info("Exception occurred while getting mailbox for id %s during ACL push", mboxId, e);
                    continue;
                }
                Collection<Integer> itemIds = mboxIdToItemIds.get(mboxId);
                MailItem[] items = null;
                try {
                    items = mbox.getItemById(null, itemIds, MailItem.Type.UNKNOWN);
                } catch (MailServiceException.NoSuchItemException e) {
                    // one or more folders no longer exist
                    if (itemIds.size() > 1) {
                        List<MailItem> itemList = new ArrayList<MailItem>();
                        for (int itemId : itemIds) {
                            try {
                                itemList.add(mbox.getItemById(null, itemId, MailItem.Type.UNKNOWN));
                            } catch (MailServiceException.NoSuchItemException ignored) {
                            }
                        }
                        items = itemList.toArray(new MailItem[itemList.size()]);
                    }
                }

                Account account = mbox.getAccount();
                String[] existingSharedItems = account.getSharedItem();
                List<String> updatedSharedItems = new ArrayList<String>();

                for (String sharedItem : existingSharedItems) {
                    ShareInfoData shareData = AclPushSerializer.deserialize(sharedItem);
                    if (!itemIds.contains(shareData.getItemId())) {
                        updatedSharedItems.add(sharedItem);
                    }
                }

                if (items != null) {
                    for (MailItem item : items) {
                        if (item == null) {
                            continue;
                        }
                        // for now push the Folder grants to LDAP
                        if (!(item instanceof Folder)) {
                            continue;
                        }
                        ACL acl = item.getACL();
                        if (acl == null) {
                            continue;
                        }
                        for (ACL.Grant grant : acl.getGrants()) {
                            updatedSharedItems.add(AclPushSerializer.serialize(item, grant));
                        }
                    }
                }

                account.setSharedItem(updatedSharedItems.toArray(new String[updatedSharedItems.size()]));
            }

            DbPendingAclPush.deleteEntries(now);
        } catch (Throwable t) {  //don't let exceptions kill the timer
            ZimbraLog.misc.warn("Error during ACL push task", t);
        }
        ZimbraLog.misc.debug("Finished pending ACL push");
    }
}
