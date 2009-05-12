/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
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
