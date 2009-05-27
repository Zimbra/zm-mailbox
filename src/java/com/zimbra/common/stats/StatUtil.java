/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.stats;

import java.util.Date;


public class StatUtil {

    /**
     * Returns the current time formatted as <tt>MM/dd/yyyy hh:mm:ss</tt>.
     */
    public static String getTimestampString() {
        return String.format("%1$tm/%1$td/%1$tY %1$tH:%1$tM:%1$tS", new Date());
    }
}
