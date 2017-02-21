/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2007, 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
 * Created on Jan 11, 2005
 *
 */
package com.zimbra.cs.filter;

import org.apache.jsieve.exception.SieveException;

@SuppressWarnings("serial")
public class ZimbraSieveException extends SieveException {
    private Throwable mCause;
    
    public ZimbraSieveException(Throwable t) {
        mCause = t;
    }
    
    public Throwable getCause() {
        return mCause;
    }
}
