/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.hsm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxBlob;
import com.zimbra.cs.mailbox.Mailbox.MailboxLock;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.store.StoreManager;

public class BlobMover {
    
    private static Log sLog = LogFactory.getLog(BlobMover.class);

    private Map /* <String, MailboxBlob> */ mAllNewBlobs = null;
    private StoreManager mStore = StoreManager.getInstance();

    /**
     * Moves blobs for mail items to the specified volume.
     * Affects all mailboxes on the current server.
     * 
     * @param timestamp only items that were created earlier than
     * this timestamp will be affected
     * @param destVolumeId the destination volume id
     */
    public void moveItems(long timestamp, short destVolumeId)
    throws ServiceException {
        mAllNewBlobs = new HashMap();
        Connection conn = null;
        
        try {
            conn = DbPool.getConnection();
            int[] mboxIds = Mailbox.getMailboxIds();
            
            for (int i = 0; i < mboxIds.length; i++) {
                Mailbox mbox = Mailbox.getMailboxById(mboxIds[i]);
                moveItems(conn, mbox, timestamp, destVolumeId);
            }
        } finally {
            if (conn != null) {
                DbPool.quietClose(conn);
            }
        }
    }
    
    /**
     * Moves blobs for mail items in a single mailbox to the specified volume.
     * 
     * @param mbox the mailbox
     * @param timestamp only items that were created earlier than
     * this timestamp will be affected
     * @param destVolumeId the destination volume id
     */
    public void moveItems(Mailbox mbox, long timestamp, short destVolumeId)
    throws ServiceException {
        mAllNewBlobs = new HashMap();
        Connection conn = null;
        
        try {
            conn = DbPool.getConnection();
            moveItems(conn, mbox, timestamp, destVolumeId);
        } finally {
            if (conn != null) {
                DbPool.quietClose(conn);
            }
        }
    }

    private void moveItems(Connection conn, Mailbox mbox, long timestamp, short destVolumeId)
    throws ServiceException {
        List oldBlobs = new ArrayList();
        List infos = DbBlobMover.getItemsBeforeTimestamp(conn, mbox, timestamp, destVolumeId);
        sLog.info("Moving " + infos.size() + " messages dated before " +
            new Date(timestamp) + " to volume " + destVolumeId);
        MailboxBlob oldBlob = null;
        
        // Keep track of new blobs for this mailbox in two data structures: a map
        // for fast lookup by digest and an array for deletion, in case of error.
        Map newBlobMap = new HashMap();
        List newBlobList = new ArrayList();
        MailboxLock lock = null;
        
        try {
            lock = Mailbox.beginMaintenance(mbox.getAccountId(), mbox.getId());
            
            Iterator iter = infos.iterator();
            while (iter.hasNext()) {
                MovedItemInfo info = (MovedItemInfo) iter.next();
                
                // Copy blob to new volume
                oldBlob = mStore.getMailboxBlob(
                    mbox, info.getId(), info.getRevision(), info.getVolumeId());
                if (oldBlob != null) {
                    MailboxBlob newBlob = null;

                    try {
                        // If we've already copied this blob, link to the copy,
                        // rather than copying the original
                        MailboxBlob linkSource = (MailboxBlob) mAllNewBlobs.get(info.getBlobDigest());
                        if (linkSource == null) {
                            linkSource = (MailboxBlob) newBlobMap.get(info.getBlobDigest());
                            if (linkSource == null) {
                                linkSource = oldBlob;
                            }
                        }
                        
                        // Create the link
                        newBlob = mStore.link(
                            linkSource.getBlob(), mbox, info.getId(), info.getRevision(), destVolumeId);
                    } catch (IOException e) {
                        throw ServiceException.FAILURE(
                            "Unable to copy " + oldBlob + " to volume " + destVolumeId, e);
                    }
                    
                    oldBlobs.add(oldBlob);
                    newBlobMap.put(info.getBlobDigest(), newBlob);
                    newBlobList.add(newBlob);
                } else {
                    sLog.warn("Could not find blob for message " + info.getId() +
                        ", revision " + info.getRevision());
                }
            }
            
            // Update messages in the database
            DbBlobMover.alterVolume(conn, mbox, timestamp, destVolumeId);
            conn.commit();
            
            // xxx bburtin: endMaintenance() should take care of this
            // mbox.purge(MailItem.TYPE_MESSAGE);
            // mbox.purge(MailItem.TYPE_APPOINTMENT);
            
            // Update global map, now that we know that all ops have succeeded
            mAllNewBlobs.putAll(newBlobMap);

            // Delete old blobs
            iter = oldBlobs.iterator();
            while (iter.hasNext()) {
                MailboxBlob oldMboxBlob = (MailboxBlob) iter.next();
                try {
                    mStore.delete(oldMboxBlob);
                } catch (IOException e) {
                    sLog.error("Unable to delete " + oldMboxBlob + ": " + e);
                }
            }
        } catch (ServiceException e) {
            // Delete new blobs on failure.  It's safe to do this, since we know
            // the database changes were not committed.
            Iterator iter = newBlobList.iterator();
            while (iter.hasNext()) {
                MailboxBlob newBlob = (MailboxBlob) iter.next();
                try {
                    mStore.delete(newBlob);
                } catch (IOException ioe) {
                    sLog.error("Unable to delete " + newBlob + ": " + ioe);
                }
            }
        } finally {
            if (lock != null) {
                Mailbox.endMaintenance(lock, true, true);
            }
        }
    }
}
