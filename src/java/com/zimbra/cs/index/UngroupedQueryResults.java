/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2010 Zimbra, Inc.
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

/*
 * Created on Oct 29, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.index;

import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.index.MailboxIndex.SortBy;
import com.zimbra.cs.mailbox.Mailbox;

/*
 * Created on Nov 3, 2004
 *
 * UngroupedQueryResults which do NOT group (ie return parts or messages in whatever mix)
 */
class UngroupedQueryResults extends ZimbraQueryResultsImpl 
{
    ZimbraQueryResults mResults;
    
    UngroupedQueryResults(ZimbraQueryResults results, byte[] types, SortBy searchOrder, Mailbox.SearchResultMode mode) {
        super(types, searchOrder, mode);
        mResults = results;
    }
    
    public void resetIterator() throws ServiceException {
        mResults.resetIterator();
    }
    
    public ZimbraHit getNext() throws ServiceException
    {
        return mResults.getNext();
    }
    
    public ZimbraHit peekNext() throws ServiceException
    {
        return mResults.peekNext();
    }
    
    public void doneWithSearchResults() throws ServiceException {
        mResults.doneWithSearchResults();
    }

    public ZimbraHit skipToHit(int hitNo) throws ServiceException {
        return mResults.skipToHit(hitNo);
    }
    
    public List<QueryInfo> getResultInfo() { return mResults.getResultInfo(); }
    
    public int estimateResultSize() throws ServiceException { return mResults.estimateResultSize(); }
    
}
