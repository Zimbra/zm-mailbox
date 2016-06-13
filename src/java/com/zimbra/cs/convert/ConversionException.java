/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
