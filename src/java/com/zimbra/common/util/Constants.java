/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.common.util;

/**
 * A place to keep commonly-used constants.
 * 
 * @author bburtin
 */
public class Constants {
    public static final long MILLIS_PER_SECOND = 1000;
    public static final long MILLIS_PER_MINUTE = MILLIS_PER_SECOND * 60;
    public static final long MILLIS_PER_HOUR   = MILLIS_PER_MINUTE * 60;
    public static final long MILLIS_PER_DAY    = MILLIS_PER_HOUR * 24;
    public static final long MILLIS_PER_WEEK   = MILLIS_PER_DAY * 7;    
    public static final long MILLIS_PER_MONTH  = MILLIS_PER_DAY * 31;    

    public static final int SECONDS_PER_MINUTE = 60;
    public static final int SECONDS_PER_HOUR   = SECONDS_PER_MINUTE * 60;
    public static final int SECONDS_PER_DAY    = SECONDS_PER_HOUR * 24;
    public static final int SECONDS_PER_WEEK   = SECONDS_PER_DAY * 7;    
    public static final int SECONDS_PER_MONTH  = SECONDS_PER_DAY * 31;    
}
