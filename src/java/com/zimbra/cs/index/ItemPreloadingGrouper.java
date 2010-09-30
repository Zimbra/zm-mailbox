/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.index;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.MailItem;
import java.util.*;

/**
 * @author tim
 *
 * This Grouper buffers a "chunk" of hits, pre-loading their 
 * MailItem objects from the DB. 
 * 
 * This is done so that we can lower the number of SELECT calls to the DB
 * by batch-fetching the Message objects from the store 
 */
class ItemPreloadingGrouper extends BufferingResultsGrouper {

    private int mChunkSize;
    private boolean mInDumpster;
    private OperationContext mOpContext;

    ItemPreloadingGrouper(ZimbraQueryResults results, int chunkSize, Mailbox mbox, boolean inDumpster) {
        super(results);
        mChunkSize = chunkSize;
        mOpContext = mbox.getOperationContext();
        mInDumpster = inDumpster;
        assert(mChunkSize > 0);
    }
    
    protected boolean bufferHits() throws ServiceException
    {
        if (mBufferedHit.size() > 0){
            return true;
        }
        
        if (!mHits.hasNext()) {
            return false;
        }
        
        ArrayList<ZimbraHit>toLoad = new ArrayList<ZimbraHit>();
        
        // FIXME: only preloading for the first mailbox right now
        // ...if this were a cross-mailbox-search, we'd be more efficient
        // if we broke things up into a hash of one load-list-per-mailbox and
        // then did preloading there...but for now we won't worry about it
        ZimbraHit firstHit = mHits.peekNext();
        Mailbox mbx = firstHit.getMailbox();
        
        int numLoaded = 0;
        do {
            ZimbraHit nextHit = mHits.getNext();
            mBufferedHit.add(nextHit);

            if (nextHit.getMailbox() == mbx && mbx != null) {
                toLoad.add(nextHit);
            }
             
            numLoaded++;
        } while (numLoaded < mChunkSize && mHits.hasNext());
        
        preload(mbx, toLoad);

        return true;
    }
    
    private void preload(Mailbox mbox, ArrayList /* ZimbraHit */ hits) throws ServiceException {
        int unloadedIds[] = new int[hits.size()];
        int numToLoad = 0;
        for (int i = 0; i < hits.size(); i++) {
            ZimbraHit cur = (ZimbraHit) hits.get(i);
            if (!cur.itemIsLoaded()) {
                numToLoad++;
                unloadedIds[i] = cur.getItemId();
            } else {
                unloadedIds[i] = Mailbox.ID_AUTO_INCREMENT;
            }
//            unloadedIds[i] = cur.itemIsLoaded() ? Mailbox.ID_AUTO_INCREMENT : cur.getItemId();
        }

        if (numToLoad > 0) {
            MailItem[] items;
            items = mbox.getItemById(mOpContext, unloadedIds, MailItem.TYPE_UNKNOWN, mInDumpster);
            for (int i = 0; i < hits.size(); ++i)
                if (items[i] != null)
                    ((ZimbraHit) hits.get(i)).setItem(items[i]);
        }
    }
}
