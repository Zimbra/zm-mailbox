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

/*
 * Created on 2005. 4. 25.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.localconfig;

import com.zimbra.common.localconfig.LC;

/**
 * @author jhahm
 *
 * Various switches to turn features on/off, mainly for measuring the
 * performance overhead.  Refer to the code that uses these keys to
 * see precisely which code paths are avoided by turning a feature off.
 */
public class DebugConfig {

    // If true, then we do ICalendar Validation every time we generate ICalendar data
    public static boolean validateOutgoingICalendar;
    
    // If true, turns off conversation feature.
    public static boolean disableConversation;

    // If true, turns off filtering of incoming messages.
    public static boolean disableFilter;

    // If true, turns off message structure analysis and text extraction.
    // Attachment extraction, indexing, and objects only work when message
    // analysis is enabled.
    public static boolean disableMessageAnalysis;

    // If true, turns off extracting content of attachments
    // If extraction is disabled, indexing and objects features are
    // meaningless.  When extraction is disabled,
    // not even the text of main text body part is extracted and won't be
    // searchable.  Only the message subject ends up being indexed.
    //
    // Disabling extraction still performs reading the MIME body part data
    // from JavaMail API.  It only skips sending the body data to the code
    // that does type-specific text extraction.  Setting this key to true
    // allows one to test the performance of JavaMail apart from performance
    // of text extraction routines.
    public static boolean disableMimePartExtraction;

    // If true, messages aren't indexed and won't be searchable.
    // If this key is set to true, the keys for indexing attachments
    // separately/together are meaningless.
    public static boolean disableIndexing;

    // If true, turns off DHTML UI's highlighting of attachment with search hit.
    public static boolean disableIndexingAttachmentsSeparately;

    // If true, turns off searching for ANDed list of terms spanning multiple attachments in a message.
    public static boolean disableIndexingAttachmentsTogether;

    // If true, turns off object detection feature.
    public static boolean disableObjects;

    // If true, allow VALARMs whose ACTION is not DISPLAY, namely AUDIO, EMAIL,
    // and PROCEDURE.  False by default, which means only DISPLAY alarms are
    // supported and others are ignored.
    public static boolean calendarAllowNonDisplayAlarms;

    // If true, fsync is skipped for redolog writes.
    public static boolean disableRedoLogFsync;

    // If true, fsync is skipped for message blob file saving.
    public static boolean disableMessageStoreFsync;

    // If true, convert TZ-relative times to UTC time in SOAP responses for
    // non-recurring appointments/tasks.  Recurring series or instances are
    // unaffected by this switch.
    public static boolean calendarForceUTC;

    public static final int numMailboxGroups;

    public static boolean calendarEnableCache;
    public static int calendarCacheLRUSize;
    public static int calendarCacheRangeMonthFrom;
    public static int calendarCacheRangeMonths;
    
    static {
        calendarAllowNonDisplayAlarms = booleanValue("debug_calendar_allow_non_display_alarms", false);
        calendarForceUTC = booleanValue("debug_calendar_force_utc", false);
        validateOutgoingICalendar = booleanValue("debug_validate_outgoing_icalendar", false);        
        disableConversation = booleanValue("debug_disable_conversation", false);
        disableFilter = booleanValue("debug_disable_filter", false);
        disableMessageAnalysis = booleanValue("debug_disable_message_analysis", false);
        if (disableMessageAnalysis) {
            disableMimePartExtraction = true;
            disableIndexing = true;
            disableObjects = true;

            // When message analysis of disabled, conversation fragment is
            // also disabled.
        } else {
            disableMimePartExtraction = booleanValue("debug_disable_mime_part_extraction", false);
            disableIndexing = booleanValue("debug_disable_indexing", false);
            disableObjects = booleanValue("debug_disable_objects", false);
        }
        disableIndexingAttachmentsSeparately = booleanValue("debug_disable_indexing_attachments_separately", false);
        disableIndexingAttachmentsTogether = booleanValue("debug_disable_indexing_attachments_together", false);

        disableRedoLogFsync = booleanValue("debug_disable_redolog_fsync", false);
        disableMessageStoreFsync = booleanValue("debug_disable_message_store_fsync", false);

        numMailboxGroups = Math.max(LC.zimbra_mailbox_groups.intValue(), 1);
        
        calendarEnableCache = booleanValue("debug_enable_calendar_cache", false);
        if (calendarEnableCache)
            calendarCacheLRUSize = intValue("debug_calendar_cache_lru_size", 1000);
        else
            calendarCacheLRUSize = 0;
        calendarCacheRangeMonthFrom = intValue("debug_calendar_cache_range_month_from", 0);
        calendarCacheRangeMonths = intValue("debug_calendar_cache_range_months", 3);
    }

    private static boolean booleanValue(String key, boolean defaultValue) {
        String val = LC.get(key);
        if (val.length() < 1)
            return defaultValue;
        return Boolean.valueOf(val).booleanValue();
    }

    private static int intValue(String key, int defaultValue) {
        String val = LC.get(key);
        if (val.length() < 1)
            return defaultValue;
        return Integer.valueOf(val).intValue();
    }
}
