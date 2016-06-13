/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.memcached;

/**
 * List of all memcached key prefixes used by ZCS
 */
public final class MemcachedKeyPrefix {

    private static final String DELIMITER = ":";

    public static final String CALENDAR_LIST        = "zmCalsList" + DELIMITER;
    public static final String CTAGINFO             = "zmCtagInfo" + DELIMITER;
    public static final String CALDAV_CTAG_RESPONSE = "zmCtagResp" + DELIMITER;
    public static final String CAL_SUMMARY          = "zmCalSumry" + DELIMITER;

    public static final String EFFECTIVE_FOLDER_ACL = "zmEffFolderACL" + DELIMITER;

    public static final String MBOX_FOLDERS_TAGS    = "zmFldrsTags" + DELIMITER;

    public static final String MBOX_MAILITEM        = "zmMailItem" + DELIMITER;

    public static final String IMAP                 = "zmImap" + DELIMITER;

    public static final String WATCHED_ITEMS        = "zmWatch" + DELIMITER;

    public static final String SYNC_STATE           = "zmSync" + DELIMITER;

    private MemcachedKeyPrefix() {
    }

}
