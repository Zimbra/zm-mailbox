/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Mar 17, 2005
 *
 * A QueryResults set which cross multiple mailboxes...
 * 
 */
package com.zimbra.cs.index;

import com.zimbra.cs.index.MailboxIndex.SortBy;
import com.zimbra.cs.service.ServiceException;


/**
 * @author tim
 *
 * Take a bunch of QueryResults (each sorted with the same sort-order) and 
 * iterate them in order....kind of like a MergeSort
 * 
 * The primary use of this class is for cross-mailbox-search, when you need to
 * aggregate search results together from many mailboxes 
 * 
 */
public class MultiQueryResults implements ZimbraQueryResults
{
    SortBy mSortOrder;
    HitIdGrouper[] mGroupedHits;
    private ZimbraHit mCachedNextHit = null;
    
    public SortBy getSortBy() {
        return mSortOrder;
    }
    public MultiQueryResults(ZimbraQueryResults[] res, SortBy sortOrder)
    {
        mSortOrder = sortOrder;
        
        mGroupedHits = new HitIdGrouper[res.length];
        for (int i = 0; i < res.length; i++) {
            mGroupedHits[i] = new HitIdGrouper(res[i], sortOrder); 
        }
    }
    
    public ZimbraHit peekNext() throws ServiceException
    {
        bufferNextHit();
        return mCachedNextHit;
    }
    
    private void internalGetNextHit() throws ServiceException
    {
        if (mCachedNextHit == null) {
            int i = 0;
            
            // loop through QueryOperations and find the "best" hit
            int currentBestHitOffset = -1;
            ZimbraHit currentBestHit = null;
            for (i = 0; i < mGroupedHits.length; i++) {
                ZimbraQueryResults op = mGroupedHits[i]; 
                if (op.hasNext()) {
                    if (currentBestHitOffset == -1) {
                        currentBestHitOffset = i;
                        currentBestHit = op.peekNext();
                    } else {
                        ZimbraHit opNext = op.peekNext();
                        int result = opNext.compareBySortField(mSortOrder, currentBestHit);
                        if (result < 0) {
                            // "before"
                            currentBestHitOffset = i;
                            currentBestHit = opNext;
                        }
                    }
                }
            }
            if (currentBestHitOffset > -1) {
                mCachedNextHit = mGroupedHits[currentBestHitOffset].getNext();
                assert(mCachedNextHit == currentBestHit);
            }
        }
    }
    
    public ZimbraHit skipToHit(int hitNo) throws ServiceException {
        resetIterator();
        for (int i = 0; i < hitNo; i++) {
            if (!hasNext()) {
                return null;
            }
            getNext();
        }
        return getNext();
    }
    
    public boolean hasNext() throws ServiceException {
        return bufferNextHit();
    }
    
    public boolean bufferNextHit() throws ServiceException {
        if (mCachedNextHit == null) {
            internalGetNextHit();
        }
        return (mCachedNextHit != null);
    }
    
    public void resetIterator() throws ServiceException {
        for (int i = 0; i < mGroupedHits.length; i++) {
            mGroupedHits[i].resetIterator();
        }
    }
    
    public ZimbraHit getFirstHit() throws ServiceException
    {
        resetIterator();
        return getNext();
    }
    
    public ZimbraHit getNext() throws ServiceException {
        bufferNextHit();
        ZimbraHit toRet = mCachedNextHit;
        mCachedNextHit = null;
        return toRet;
    }
    
    public void doneWithSearchResults() throws ServiceException {
        for (int i = 0; i < mGroupedHits.length; i++) {
            mGroupedHits[i].doneWithSearchResults();
        }
    }
}