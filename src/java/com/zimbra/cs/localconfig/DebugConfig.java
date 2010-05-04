/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
 * Created on 2005. 4. 25.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.localconfig;

import com.zimbra.common.localconfig.LC;

/**
 * Various switches to turn features on/off, mainly for measuring the
 * performance overhead.  Refer to the code that uses these keys to
 * see precisely which code paths are avoided by turning a feature off.
 */
public class DebugConfig {

    /** If true, then we do ICalendar Validation every time we generate
     *  ICalendar data. */
    public static boolean validateOutgoingICalendar;
    
    /** If true, turns off conversation feature. */
    public static boolean disableConversation;

    /** If true, turns off filtering of incoming messages. */
    public static boolean disableFilter;

    /** If true, turns off message structure analysis and text extraction.
     *  Attachment extraction, indexing, and objects only work when message
     *  analysis is enabled. */
    public static boolean disableMessageAnalysis;

    /** If true, turns off extracting content of attachments
     *  If extraction is disabled, indexing and objects features are
     *  meaningless.  When extraction is disabled,
     *  not even the text of main text body part is extracted and won't be
     *  searchable.  Only the message subject ends up being indexed.
     * 
     *  Disabling extraction still performs reading the MIME body part data
     *  from JavaMail API.  It only skips sending the body data to the code
     *  that does type-specific text extraction.  Setting this key to true
     *  allows one to test the performance of JavaMail apart from performance
     *  of text extraction routines. */
    public static boolean disableMimePartExtraction;

    /** If true, messages aren't indexed and won't be searchable.
     *  If this key is set to true, the keys for indexing attachments
     *  separately/together are meaningless. */
    public static boolean disableIndexing;

    /** If true, turns off DHTML UI's highlighting of attachment with search
     *  hit. */
    public static boolean disableIndexingAttachmentsSeparately;

    /** If true, turns off searching for ANDed list of terms spanning multiple
     *  attachments in a message. */
    public static boolean disableIndexingAttachmentsTogether;

    /** If true, turns off object detection feature. */
    public static boolean disableObjects;

    /** If true, allow VALARMs whose ACTION is not DISPLAY, namely AUDIO,
     *  EMAIL, and PROCEDURE.  False by default, which means only DISPLAY
     *  alarms are supported and others are ignored. */
    public static boolean calendarAllowNonDisplayAlarms;

    /** If true, use alarms specified by organizer in an invite email.  If
     *  false (default), discard organizer alarms and set one based on
     *  attendee's preferences. */
    public static boolean calendarAllowOrganizerSpecifiedAlarms;

    /** If true, fsync is skipped for redolog writes. */
    public static boolean disableRedoLogFsync;

    /** If true, fsync is skipped for message blob file saving. */
    public static boolean disableMessageStoreFsync;

    /** If true, convert TZ-relative times to UTC time in SOAP responses for
     *  non-recurring appointments/tasks.  Recurring series or instances are
     *  unaffected by this switch. */
    public static boolean calendarForceUTC;

    /** Whether to send permission denied auto reply when organizer is not
     *  permitted to invite user and user is an indirect attendee through a
     *  mailing list rather than a directly named attendee.  Default is to
     *  suppress auto reply to reduce noise. */
    public static boolean calendarEnableInviteDeniedReplyForUnlistedAttendee;

    /** If true, every item marked as "modified" in the Mailbox's cache is
     *  checked against the database at the end of the transaction in order
     *  to ensure cache consistency (very expensive) */
    public static boolean checkMailboxCacheConsistency;

    /** If true, the database layer assumes that the MailboxManager
     *  implementation has an external mapping from accounts to mailbox
     *  IDs.  As a result, the ZIMBRA.MAILBOX table will not be populated
     *  and will not be read at startup. */
    public static boolean externalMailboxDirectory;

    /** If true, the database layer assumes that every mailbox gets its own
     *  database instance and that all the tables in that database do *not*
     *  have the MAILBOX_ID column.  All mutable mailbox data, including
     *  sizes and checkpoints as well as out-of-office reply tracking and
     *  the "mailbox metadata" scratch space, are moved out of the ZIMBRA
     *  database and into this per-user database. */
    public static boolean disableMailboxGroups;

    /** The number of MBOXGROUP<N> databases to distribute the users over.
     *  This is a middle ground between One Huge Database (contention and
     *  the effects of corruption are issues) and database-per-user (which
     *  most DBMSes can't deal with). */
    public static final int numMailboxGroups;
    
    /** If true, more than one server may be sharing the same store and
     *  database install.  In that case, the server must perform extra checks
     *  to ensure that mailboxes "homed" on other servers are treated
     *  accordingly. */
    public static final boolean mockMultiserverInstall;

    /** If true, the GAL sync visitor mechanism is disabled.  SyncGal will use
     *  the traditional way of adding matches to a SearchGalResult, then add 
     *  each match in the SOAP response.  The GAL sync visitor mechanism
     *  reduces chance of OOME when there is a huge result. */
    public static boolean disableGalSyncVisitor;

    public static boolean disableCalendarTZMatchByID;
    public static boolean disableCalendarTZMatchByRule;

    public static boolean forceMimeConvertersForCalendarBlobs;

    /** If true, disable the memcached-based folders/tags cache of mailboxes.
     */
    public static boolean disableFoldersTagsCache;
    
    public static boolean enableContactLocalizedSort;
    
    public static boolean enableRefCountedIndexReaderStats;
    
    public static boolean enableMigrateUserZimletPrefs;
    
    static {
        calendarAllowNonDisplayAlarms = booleanValue("debug_calendar_allow_non_display_alarms", false);
        calendarAllowOrganizerSpecifiedAlarms = booleanValue("debug_calendar_allow_organizer_specified_alarms", false);
        calendarForceUTC = booleanValue("debug_calendar_force_utc", false);
        validateOutgoingICalendar = booleanValue("debug_validate_outgoing_icalendar", false);
        calendarEnableInviteDeniedReplyForUnlistedAttendee = booleanValue("debug_calendar_enable_invite_denied_reply_for_unlisted_attendee", false);

        disableConversation = booleanValue("debug_disable_conversation", false);
        disableFilter = booleanValue("debug_disable_filter", false);
        disableMessageAnalysis = booleanValue("debug_disable_message_analysis", false);
        if (disableMessageAnalysis) {
            disableMimePartExtraction = true;
            disableIndexing = true;
            disableObjects = true;

            // When message analysis is disabled, conversation fragment is
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

        checkMailboxCacheConsistency = booleanValue("debug_check_mailbox_cache_consistency", false);

        disableMailboxGroups = booleanValue("debug_disable_mailbox_group", false);
        numMailboxGroups = disableMailboxGroups ? Integer.MAX_VALUE : Math.max(LC.zimbra_mailbox_groups.intValue(), 1);

        externalMailboxDirectory = booleanValue("debug_external_mailbox_directory", false) && disableMailboxGroups;

        mockMultiserverInstall = booleanValue("debug_mock_multiserver_install", false);

        disableGalSyncVisitor = booleanValue("debug_disable_gal_sync_visitor", false);

        disableCalendarTZMatchByID = booleanValue("debug_disable_calendar_tz_match_by_id", false);
        disableCalendarTZMatchByRule = booleanValue("debug_disable_calendar_tz_match_by_rule", false);

        forceMimeConvertersForCalendarBlobs = booleanValue("debug_force_mime_converters_for_calendar_blobs", false);

        disableFoldersTagsCache = booleanValue("debug_disable_folders_tags_cache", false);
        
        enableContactLocalizedSort = booleanValue("debug_enable_contact_localized_sort", true);
        
        enableRefCountedIndexReaderStats = booleanValue("debug_enable_ref_counted_index_reader_stats", false);
        
        enableMigrateUserZimletPrefs = booleanValue("migrate_user_zimlet_prefs", false);
    }

    protected static boolean booleanValue(String key, boolean defaultValue) {
        String val = LC.get(key);
        if (val.length() < 1)
            return defaultValue;
        return Boolean.valueOf(val).booleanValue();
    }

    @SuppressWarnings("unused")
    private static int intValue(String key, int defaultValue) {
        String val = LC.get(key);
        if (val.length() < 1)
            return defaultValue;
        return Integer.valueOf(val).intValue();
    }
}
