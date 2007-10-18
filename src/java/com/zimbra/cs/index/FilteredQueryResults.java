/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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
package com.zimbra.cs.index;

import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.index.MailboxIndex.SortBy;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;

/** 
 * Result set that does filtering
 */
public class FilteredQueryResults implements ZimbraQueryResults {
    ZimbraQueryResults mResults;
    
    private boolean mFilterTagDeleted = false;
    
    FilteredQueryResults(ZimbraQueryResults other) {
        mResults = other;
    }

    void setFilterTagDeleted(boolean truthiness) {
        mFilterTagDeleted = truthiness;
    }
    
    public void doneWithSearchResults() throws ServiceException {
        mResults.doneWithSearchResults();
    }

    public int estimateResultSize() throws ServiceException {
        return mResults.estimateResultSize();
    }

    public ZimbraHit getFirstHit() throws ServiceException {
        resetIterator();
        return getNext();
    }

    public List<QueryInfo> getResultInfo() {
        return mResults.getResultInfo();
    }

    public SortBy getSortBy() {
        return mResults.getSortBy();
    }

    public void resetIterator() throws ServiceException {
        mResults.resetIterator();
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

    public ZimbraHit getNext() throws ServiceException {
        ZimbraHit toRet = peekNext();
        if (toRet != null)
            mResults.getNext(); // skip the current hit
        return toRet;
    }

    public boolean hasNext() throws ServiceException {
        return peekNext() != null;
    }
    
    /**
     * @return TRUE if the passed-in hit should be filtered (removed) from 
     * the result set
     */
    private boolean shouldFilter(ZimbraHit hit) throws ServiceException {
        if (mFilterTagDeleted) {
            if (hit.isLocal()) {
                MailItem item = hit.getMailItem();
                if (item == null) {
                    System.out.println("NULL!");
                }
                if ((item.getFlagBitmask() & Flag.BITMASK_DELETED) != 0)
                    return true; // filter it
            }
        }
        
        return false; // if we got here, include it
    }

    public ZimbraHit peekNext() throws ServiceException {
        ZimbraHit cur = mResults.peekNext();
        while (cur != null) {
            boolean filterThisHit = false;
            if (cur instanceof ConversationHit) {
                ConversationHit ch = (ConversationHit)cur;
                for (MessageHit mh : ch.getMessageHits()) {
                    filterThisHit = shouldFilter(mh);
                    if (!filterThisHit) 
                        break; // found at least one valid message hit in this conv
                }
            } else {
                filterThisHit = shouldFilter(cur);
            }
            
            if (!filterThisHit)
                return cur;
            
            mResults.getNext(); // skip next hit
            cur = mResults.peekNext();
        }
        return null;
    }

    
}
