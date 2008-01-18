/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Mar 15, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.index;

import java.util.*;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.index.MailboxIndex.SortBy;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;


/**
 * @author tim
 * 
 * Take ZimbraHits which are already sorted by sort-order and additionally
 * sort them by mail-item-id
 * 
 * This Grouper has no effect if the current sort mode is "none"
 *
 */
public class HitIdGrouper extends BufferingResultsGrouper {
    private SortBy mSortOrder;
    private static Log mLog = LogFactory.getLog(HitIdGrouper.class);
    
    public static ZimbraQueryResults Create(ZimbraQueryResults hits, SortBy sortOrder) {
        if (sortOrder == SortBy.NONE)
            return hits;
        else
            return new HitIdGrouper(hits, sortOrder);
    }
    
    private HitIdGrouper(ZimbraQueryResults hits, SortBy sortOrder) {
        super(hits);
        mSortOrder = sortOrder;
    }
    
    public boolean hasNext() throws ServiceException {
        return (mBufferedHit.size() > 0 || (mHits.hasNext()));
    }
     
    protected boolean bufferHits() throws ServiceException {
        if (mBufferedHit.size() > 0){
            return true;
        }
        
        if (!mHits.hasNext()) {
            return false;
        }
        
        ZimbraHit curGroupHit = mHits.getNext();
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
        Collections.sort(mBufferedHit, ZimbraHit.getSortAndIdComparator(mSortOrder));
        
        // we're done
        return true;
    }
}
