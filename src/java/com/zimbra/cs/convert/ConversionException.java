/*
 * Created on Jan 19, 2005
 *
 */
package com.liquidsys.coco.convert;

/**
 * @author kchen
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ConversionException extends Exception {
    private boolean mTemporary;
    
    public ConversionException(String msg, Throwable t) {
        super(msg, t);
    }

    public ConversionException(String msg, Throwable t, boolean temp) {
        this(msg, t);
        mTemporary = temp;
    }
    
    public ConversionException(String msg) {
        super(msg);
    }
    
    public ConversionException(String msg, boolean temp) {
        this(msg);
        mTemporary = temp;
    }
    
    public boolean isTemporary() {
        return mTemporary;
    }
    
    /**
     * Returns true if the cause of the wrapper exception is a temporary ConversionException;
     * false otherwise.
     * @param wrapper
     * @return
     */
    public static boolean isTemporaryCauseOf(Throwable wrapper) {
        Throwable cause = wrapper.getCause();
        if (cause instanceof ConversionException) {
            ConversionException convEx = (ConversionException) cause;
            return convEx.isTemporary();
        }
        return false;
    }
}
