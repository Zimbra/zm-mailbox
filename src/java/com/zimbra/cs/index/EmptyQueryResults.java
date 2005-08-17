/*
 * Created on Oct 22, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.index;

/**
 * @author tim
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
class EmptyQueryResults extends ZimbraQueryResultsImpl {

    
    EmptyQueryResults(byte[] types, int searchOrder) {
        super(types, searchOrder);
    }
    
    public void resetIterator()  {
    }

    public ZimbraHit getNext() {
        return null;
    }

    public ZimbraHit peekNext() {
        return null;
    }
    
    public void doneWithSearchResults() {
    }

    public ZimbraHit skipToHit(int hitNo) {
        return null;
    }
}
