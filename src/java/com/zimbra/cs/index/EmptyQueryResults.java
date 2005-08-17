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
class EmptyQueryResults extends LiquidQueryResultsImpl {

    
    EmptyQueryResults(byte[] types, int searchOrder) {
        super(types, searchOrder);
    }
    
    public void resetIterator()  {
    }

    public LiquidHit getNext() {
        return null;
    }

    public LiquidHit peekNext() {
        return null;
    }
    
    public void doneWithSearchResults() {
    }

    public LiquidHit skipToHit(int hitNo) {
        return null;
    }
}
