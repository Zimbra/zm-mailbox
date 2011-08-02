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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;

/**
 * This task publishes shared item updates to LDAP to enable centralized discovery of shares, e.g.
 * to discover all shares accessible to a particular account.
 */
public class AclPushTask extends TimerTask {

    @Override
    public void run() {
        doWork();
    }

    public static void doWork() {
        ZimbraLog.misc.info("Starting pending ACL push");
        Date now = new Date();
        try {
            Multimap<Integer, Integer> mboxIdToItemIds = DbPendingAclPush.getEntries(now);

            for (int mboxId : mboxIdToItemIds.keySet()) {
                Mailbox mbox = MailboxManager.getInstance().getMailboxById(mboxId);
                Collection<Integer> itemIds = mboxIdToItemIds.get(mboxId);
                MailItem[] folders = null;
                try {
                    folders = mbox.getItemById(null, itemIds, MailItem.Type.FOLDER);
                } catch (MailServiceException.NoSuchItemException e) {
                    // one or more folders no longer exist
                    if (itemIds.size() > 1) {
                        List<MailItem> fList = new ArrayList<MailItem>();
                        for (int itemId : itemIds) {
                            try {
                                fList.add(mbox.getItemById(null, itemId, MailItem.Type.FOLDER));
                            } catch (MailServiceException.NoSuchItemException ignored) {
                            }
                            folders = fList.toArray(new MailItem[fList.size()]);
                        }
                    }
                }

                Account account = mbox.getAccount();
                String[] existingSharedItems = account.getSharedItem();
                List<String> updatedSharedItems = new ArrayList<String>();

                for (String sharedItem : existingSharedItems) {
                    ShareInfoData shareData = AclPushSerializer.deserialize(sharedItem);
                    if (!itemIds.contains(shareData.getFolderId())) {
                        updatedSharedItems.add(sharedItem);
                    }
                }

                if (folders != null) {
                    for (MailItem folderItem : folders) {
                        if (folderItem == null || !(folderItem instanceof Folder)) {
                            continue;
                        }
                        Folder folder = (Folder) folderItem;
                        ACL acl = folder.getACL();
                        if (acl == null) {
                            continue;
                        }
                        for (ACL.Grant grant : acl.getGrants()) {
                            updatedSharedItems.add(AclPushSerializer.serialize(folder, grant));
                        }
                    }
                }

                account.setSharedItem(updatedSharedItems.toArray(new String[updatedSharedItems.size()]));
            }

            DbPendingAclPush.deleteEntries(now);
        } catch (ServiceException e) {
            ZimbraLog.misc.warn("Error during ACL push task", e);
        }
        ZimbraLog.misc.info("Finished pending ACL push");
    }
}
