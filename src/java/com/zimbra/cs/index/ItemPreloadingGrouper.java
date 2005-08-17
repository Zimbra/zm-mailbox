package com.zimbra.cs.index;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;
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
public class ItemPreloadingGrouper extends BufferingResultsGrouper 
{
    private int mChunkSize;

    public ItemPreloadingGrouper(LiquidQueryResults results, int chunkSize) {
        super(results);
        mChunkSize = chunkSize;
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
        
        ArrayList /* LiquidHit */ toLoad = new ArrayList();
        
        // FIXME: only preloading for the first mailbox right now
        // ...if this were a cross-mailbox-search, we'd be more efficient
        // if we broke things up into a hash of one load-list-per-mailbox and
        // then did preloading there...but for now we won't worry about it
        LiquidHit firstHit = mHits.peekNext();
        Mailbox mbx = firstHit.getMailbox();
        
        int numLoaded = 0;
        do {
            LiquidHit nextHit = mHits.getNext();
            mBufferedHit.add(nextHit);

            if (nextHit.getMailbox() == mbx) {
                toLoad.add(nextHit);
            }
             
            numLoaded++;
        } while (numLoaded < mChunkSize && mHits.hasNext());
        
        preload(mbx, toLoad);

        return true;
    }
    
    private void preload(Mailbox mbox, ArrayList /* LiquidHit */ hits) throws ServiceException {
        int unloadedIds[] = new int[hits.size()];
        int numToLoad = 0;
        for (int i = 0; i < hits.size(); i++) {
            LiquidHit cur = (LiquidHit) hits.get(i);
            if (!cur.itemIsLoaded()) {
                numToLoad++;
                unloadedIds[i] = cur.getItemId();
            } else {
                unloadedIds[i] = Mailbox.ID_AUTO_INCREMENT;
            }
//            unloadedIds[i] = cur.itemIsLoaded() ? Mailbox.ID_AUTO_INCREMENT : cur.getItemId();
        }

        if (numToLoad > 0) {
            MailItem[] items = mbox.getItemById(unloadedIds, MailItem.TYPE_UNKNOWN);
            for (int i = 0; i < hits.size(); i++)
                if (items[i] != null)
                    ((LiquidHit) hits.get(i)).setItem(items[i]);
        }
    }
}
