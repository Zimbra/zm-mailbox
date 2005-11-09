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
 * Created on Mar 15, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.index;

import java.util.*;
import com.zimbra.cs.service.ServiceException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * @author tim
 * 
 * Take ZimbraHits which are already sorted by sort-order and additionally
 * sort them by mail-item-id
 *
 */
public class HitIdGrouper extends BufferingResultsGrouper {
    private int mSortOrder;
    private static Log mLog = LogFactory.getLog(HitIdGrouper.class);
    
    public HitIdGrouper(ZimbraQueryResults hits, int sortOrder) {
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
