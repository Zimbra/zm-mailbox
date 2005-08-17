package com.zimbra.cs.index;

import java.util.*;
import com.zimbra.cs.service.ServiceException;

/**
 * @author tim
 * 
 * Groups hit results for various reasons.  Subclass must override BufferHits below
 *
 */
public abstract class BufferingResultsGrouper implements LiquidQueryResults {

    protected LiquidQueryResults mHits;
    protected List /* LiquidHit */mBufferedHit = new LinkedList();
    protected boolean atStart = true;
    
    
    /**
     * Fills the hit buffer if necessary.  May be called even if the buffer has entries in it,
     * implementation may ignore it (but must return true) in those cases.
     * 
     * @return TRUE if there some hits in the buffer, FALSE if not.
     * @throws ServiceException
     * 
     */
    protected abstract boolean bufferHits() throws ServiceException;
    
    
    public BufferingResultsGrouper(LiquidQueryResults hits) {
        mHits = hits;
    }
    
    public void resetIterator() throws ServiceException {
        if (!atStart) {
            mBufferedHit.clear();
            mHits.resetIterator();
            atStart = true;
        }
    }

    public boolean hasNext() throws ServiceException {
        return bufferHits();
    }
    
    public LiquidHit getFirstHit() throws ServiceException {
        resetIterator();
        return getNext();
    }
    
    public LiquidHit peekNext() throws ServiceException {
        if (bufferHits()) {
            return (LiquidHit)(mBufferedHit.get(0));
        } else {
            return null;
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
    
    public LiquidHit getNext() throws ServiceException {
        atStart = false;
        if (bufferHits()) {
            return (LiquidHit)(mBufferedHit.remove(0));
        } else {
            return null;
        }
    }
    
    public void doneWithSearchResults() throws ServiceException {
        mHits.doneWithSearchResults();
    }


}
