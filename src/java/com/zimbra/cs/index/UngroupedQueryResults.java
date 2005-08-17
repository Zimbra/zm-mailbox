/*
 * Created on Oct 29, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.index;

import com.zimbra.cs.service.ServiceException;

/*
 * Created on Nov 3, 2004
 *
 * UngroupedQueryResults which do NOT group (ie return parts or messages in whatever mix)
 */
class UngroupedQueryResults extends ZimbraQueryResultsImpl 
{
    ZimbraQueryResults mResults;
    
    public UngroupedQueryResults(ZimbraQueryResults results, byte[] types, int searchOrder) {
        super(types, searchOrder);
        mResults = results;
    }
    
    public void resetIterator() throws ServiceException {
        mResults.resetIterator();
    }
    
    public LiquidHit getNext() throws ServiceException
    {
        return mResults.getNext();
    }
    
    public LiquidHit peekNext() throws ServiceException
    {
        return mResults.peekNext();
    }
    
    public void doneWithSearchResults() throws ServiceException {
        mResults.doneWithSearchResults();
    }

    public LiquidHit skipToHit(int hitNo) throws ServiceException {
        return mResults.skipToHit(hitNo);
    }
}
