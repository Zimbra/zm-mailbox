/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2007, 2009, 2010 Zimbra, Inc.
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
 * Created on Jan 19, 2005
 *
 */
package com.zimbra.cs.convert;

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
