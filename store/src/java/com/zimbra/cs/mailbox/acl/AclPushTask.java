/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimerTask;

import com.google.common.collect.ArrayListMultimap;
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
import com.zimbra.cs.util.AccountUtil;
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
        
        Multimap<Integer, List<Integer>> currentItemIdsProcessed = ArrayListMultimap.create();
        		
        try {
        	Date now = new Date();
            Multimap<Integer, Integer> mboxIdToItemIds = DbPendingAclPush.getEntries(now);

            for (int mboxId : mboxIdToItemIds.keySet()) {
                Mailbox mbox;
                List<Integer> itemsProcessed = new ArrayList<Integer> ();
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
                Set<String> updatedSharedItems = new HashSet<String>();

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
                        itemsProcessed.add(item.getId());
                        currentItemIdsProcessed.put(mboxId, itemsProcessed);
                    }
                }
                account.setSharedItem(updatedSharedItems.toArray(new String[updatedSharedItems.size()]));
                AccountUtil.broadcastFlushCache(account);
            } // for
            DbPendingAclPush.deleteEntries(now);
            
        } catch (ServiceException e){
        	ZimbraLog.misc.warn("Error during ACL push task", e);
        	
        } catch (Throwable t) {  //don't let exceptions kill the timer
        	try {
        		
        		// We ran into runtime exception, so we want to delete records from ACL 
        		// table for processed records.
        		 deleteDbAclEntryForProcessedItems(currentItemIdsProcessed);        		
    		} catch (ServiceException e) {
    			ZimbraLog.misc.warn("Error during ACL push task and deleting ACL push entry.");
    		}
            ZimbraLog.misc.warn("Error during ACL push task", t);
        }
        ZimbraLog.misc.debug("Finished pending ACL push");
    }

	/**
	 * @param mailboxIdUnderProcess
	 * @param currentItemIdsProcessed
	 * @throws ServiceException
	 */
	private static void deleteDbAclEntryForProcessedItems( Multimap<Integer, List<Integer>> currentItemIdsProcessed)
			throws ServiceException {
		if (currentItemIdsProcessed.size() != 0) {
			Collection<Entry<Integer, List<Integer>>> mailboxIds = currentItemIdsProcessed.entries();
			for (Entry<Integer, List<Integer>> entry : mailboxIds) {
				int mboxId = entry.getKey();
				List<Integer> itemIds = entry.getValue();
				for (int itemId : itemIds)
					DbPendingAclPush.deleteEntry(mboxId, itemId);
			}
		}
	}

	
}
