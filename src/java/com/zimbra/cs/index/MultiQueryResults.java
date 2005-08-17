/*
 * Created on Mar 17, 2005
 *
 * A QueryResults set which cross multiple mailboxes...
 * 
 */
package com.zimbra.cs.index;

import com.zimbra.cs.service.ServiceException;


/**
 * @author tim
 *
 * Take a bunch of QueryResults (each sorted with the same sort-order) and 
 * iterate them in order....kind of like a MergeSort
 * 
 */
public class MultiQueryResults implements LiquidQueryResults
{
    int mSortOrder;
    HitIdGrouper[] mGroupedHits;
    private LiquidHit mCachedNextHit = null;
    
    public MultiQueryResults(LiquidQueryResults[] res, int sortOrder)
    {
        mSortOrder = sortOrder;
        
        mGroupedHits = new HitIdGrouper[res.length];
        for (int i = 0; i < res.length; i++) {
            mGroupedHits[i] = new HitIdGrouper(res[i], sortOrder); 
        }
    }
    
    public LiquidHit peekNext() throws ServiceException
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
            LiquidHit currentBestHit = null;
            for (i = 0; i < mGroupedHits.length; i++) {
                LiquidQueryResults op = mGroupedHits[i]; 
                if (op.hasNext()) {
                    if (currentBestHitOffset == -1) {
                        currentBestHitOffset = i;
                        currentBestHit = op.peekNext();
                    } else {
                        LiquidHit opNext = op.peekNext();
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
    
    public LiquidHit skipToHit(int hitNo) throws ServiceException {
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
    
    public LiquidHit getFirstHit() throws ServiceException
    {
        resetIterator();
        return getNext();
    }
    
    public LiquidHit getNext() throws ServiceException {
        bufferNextHit();
        LiquidHit toRet = mCachedNextHit;
        mCachedNextHit = null;
        return toRet;
    }
    
    public void doneWithSearchResults() throws ServiceException {
        for (int i = 0; i < mGroupedHits.length; i++) {
            mGroupedHits[i].doneWithSearchResults();
        }
    }
}