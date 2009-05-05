/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
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

package com.zimbra.cs.memcached;

import com.zimbra.common.util.ZimbraMemcachedClient.KeyPrefix;

// list of all memcached key prefixes used by ZCS
public class MemcachedKeyPrefix {

    public static final KeyPrefix CALENDAR_LIST        = new KeyPrefix("zmCalsList");
    public static final KeyPrefix CTAGINFO             = new KeyPrefix("zmCtagInfo");
    public static final KeyPrefix CALDAV_CTAG_RESPONSE = new KeyPrefix("zmCtagResp");

}
