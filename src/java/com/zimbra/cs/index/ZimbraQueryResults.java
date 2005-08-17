/*
 * Created on Mar 15, 2005
 *
 */
package com.zimbra.cs.index;

import com.zimbra.cs.service.ServiceException;

/**
 * @author tim
 *
 * Interface for iterating through LiquidHits.  This class is the thing
 * that is returned when you do a Search.
 * 
 */
public interface ZimbraQueryResults {
    
    void resetIterator() throws ServiceException;
    
    LiquidHit getNext() throws ServiceException;
    
    LiquidHit peekNext() throws ServiceException;
    
	LiquidHit getFirstHit() throws ServiceException;
	
    LiquidHit skipToHit(int hitNo) throws ServiceException;
    
    boolean hasNext() throws ServiceException;
    
    /**
     * MUST be called when you are done with this iterator!!
     * 
     * @throws ServiceException
     */
    void doneWithSearchResults() throws ServiceException;
}
