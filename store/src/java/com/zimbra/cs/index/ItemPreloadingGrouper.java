/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.index;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.MailItem;
import java.util.*;

/**
 * This Grouper buffers a "chunk" of hits, pre-loading their MailItem objects from the DB.
 *
 * This is done so that we can lower the number of SELECT calls to the DB by batch-fetching the Message objects from the
 * store.
 *
 * @author tim
 */
final class ItemPreloadingGrouper extends BufferingResultsGrouper {

    private final int chunkSize;
    private final boolean inDumpster;
    private final OperationContext opContext;

    ItemPreloadingGrouper(ZimbraQueryResults results, int chunkSize, Mailbox mbox, boolean inDumpster) {
        super(results);
        assert(chunkSize > 0);
        this.chunkSize = chunkSize;
        opContext = mbox.getOperationContext();
        this.inDumpster = inDumpster;
    }

    @Override
    protected boolean bufferHits() throws ServiceException {
        if (bufferedHit.size() > 0){
            return true;
        }

        if (!hits.hasNext()) {
            return false;
        }

        ArrayList<ZimbraHit>toLoad = new ArrayList<ZimbraHit>();

        // FIXME: only preloading for the first mailbox right now
        // ...if this were a cross-mailbox-search, we'd be more efficient
        // if we broke things up into a hash of one load-list-per-mailbox and
        // then did preloading there...but for now we won't worry about it
        ZimbraHit firstHit = hits.peekNext();
        Mailbox mbx = firstHit.getMailbox();

        int numLoaded = 0;
        do {
            ZimbraHit nextHit = hits.getNext();
            bufferedHit.add(nextHit);

            if (nextHit.getMailbox() == mbx && mbx != null) {
                toLoad.add(nextHit);
            }

            numLoaded++;
        } while (numLoaded < chunkSize && hits.hasNext());

        preload(mbx, toLoad);

        return true;
    }

    private void preload(Mailbox mbox, List<ZimbraHit> hits) throws ServiceException {
        int unloadedIds[] = new int[hits.size()];
        int numToLoad = 0;
        for (int i = 0; i < hits.size(); i++) {
            ZimbraHit cur = hits.get(i);
            if (!cur.itemIsLoaded()) {
                numToLoad++;
                unloadedIds[i] = cur.getItemId();
            } else {
                unloadedIds[i] = Mailbox.ID_AUTO_INCREMENT;
            }
        }

        if (numToLoad > 0) {
            MailItem[] items = mbox.getItemById(opContext, unloadedIds, MailItem.Type.UNKNOWN_SEARCHABLE, inDumpster);
            for (int i = 0; i < hits.size(); ++i) {
                if (items[i] != null) {
                    hits.get(i).setItem(items[i]);
                }
            }
        }
    }
}
