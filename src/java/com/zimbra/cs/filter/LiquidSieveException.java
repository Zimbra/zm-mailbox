/*
 * Created on Jan 11, 2005
 *
 */
package com.zimbra.cs.filter;

import org.apache.jsieve.SieveException;

/**
 * @author kchen
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class LiquidSieveException extends SieveException {
    private Throwable mCause;
    
    public LiquidSieveException(Throwable t) {
        mCause = t;
    }
    
    public Throwable getCause() {
        return mCause;
    }
}
