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

// list of all memcached key prefixes used by ZCS
public class MemcachedKeyPrefix {

    private static final String DELIMITER = ":";

    public static final String CALENDAR_LIST        = "zmCalsList" + DELIMITER;
    public static final String CTAGINFO             = "zmCtagInfo" + DELIMITER;
    public static final String CALDAV_CTAG_RESPONSE = "zmCtagResp" + DELIMITER;
    public static final String CAL_SUMMARY          = "zmCalSumry" + DELIMITER;

    public static final String EFFECTIVE_FOLDER_ACL = "zmEffFolderACL" + DELIMITER;

    public static final String MBOX_FOLDERS_TAGS    = "zmFldrsTags" + DELIMITER;
}
