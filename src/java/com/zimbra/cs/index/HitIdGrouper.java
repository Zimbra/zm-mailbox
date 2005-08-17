/*
 * Created on Mar 15, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.liquidsys.coco.index;

import java.util.*;
import com.liquidsys.coco.service.ServiceException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * @author tim
 * 
 * Take LiquidHits which are already sorted by sort-order and additionally
 * sort them by mail-item-id
 *
 */
public class HitIdGrouper extends BufferingResultsGrouper {
    private int mSortOrder;
    private static Log mLog = LogFactory.getLog(HitIdGrouper.class);
    
    public HitIdGrouper(LiquidQueryResults hits, int sortOrder) {
        super(hits);
        mSortOrder = sortOrder;
    }
     
    protected boolean bufferHits() throws ServiceException {
        if (mBufferedHit.size() > 0){
            return true;
        }
        
        if (!mHits.hasNext()) {
            return false;
        }
        
        LiquidHit curGroupHit = mHits.getNext();
        mBufferedHit.add(curGroupHit);

        // buffer all the hits with the same sort field
        while(mHits.hasNext() && (curGroupHit.compareBySortField(mSortOrder, mHits.peekNext()) == 0))
        {
            if (mLog.isDebugEnabled()) {
                mLog.debug("HitIdGrouper buffering "+mHits.peekNext());
            }
            mBufferedHit.add(mHits.getNext());
        }
        
        // sort them by mail-item-id
        Collections.sort(mBufferedHit, LiquidHit.getSortAndIdComparator(mSortOrder));
        
        // we're done
        return true;
    }
}
