/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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
package com.zimbra.common.localconfig;


/**
 * Various switches to turn features on/off, mainly for measuring the
 * performance overhead.  Refer to the code that uses these keys to
 * see precisely which code paths are avoided by turning a feature off.
 *
 * @since 2005. 4. 25.
 */
public final class DebugConfig {

    private DebugConfig() {
    }

    /** If true, then we do ICalendar Validation every time we generate
     *  ICalendar data. */
    public static final boolean validateOutgoingICalendar = value("debug_validate_outgoing_icalendar", false);

    /** If true, turns off conversation feature. */
    public static final boolean disableConversation = value("debug_disable_conversation", false);

    /** If true, turns off filtering of incoming messages. */
    public static final boolean disableIncomingFilter = value("debug_disable_filter", false);

    /** If true, turns off filtering of outgoing messages. */
    public static final boolean disableOutgoingFilter = value("debug_disable_outgoing_filter", false);

    /** If true, turns off message structure analysis and text extraction.
     *  Attachment extraction, indexing, and objects only work when message
     *  analysis is enabled. */
    public static final boolean disableMessageAnalysis = value("debug_disable_message_analysis", false);

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
    public static final boolean disableMimePartExtraction = disableMessageAnalysis ?
            true : value("debug_disable_mime_part_extraction", false);

    /** If true, turns off object detection feature. */
    public static final boolean disableObjects = disableMessageAnalysis ? true : value("debug_disable_objects", false);

    /** If true, turns off DHTML UI's highlighting of attachment with search
     *  hit. */
    public static final boolean disableIndexingAttachmentsSeparately =
            value("debug_disable_indexing_attachments_separately", false);

    /** If true, turns off searching for ANDed list of terms spanning multiple
     *  attachments in a message. */
    public static final boolean disableIndexingAttachmentsTogether =
            value("debug_disable_indexing_attachments_together", false);

    /** If true, allow VALARMs whose ACTION is PROCEDURE. (false by default) */
    public static final boolean calendarAllowProcedureAlarms = value("debug_calendar_allow_procedure_alarms", false);

    /** If true, convert AUDIO and PROCEDURE VALARMs to DISPLAY when serializing to xml,
     *  so ZWC can see them as regular alarms. (true by default) */
    public static final boolean calendarConvertNonDisplayAlarm =
            value("debug_calendar_convert_non_display_alarms", true);

    /** If true, use alarms specified by organizer in an invite email.  If
     *  false (default), discard organizer alarms and set one based on
     *  attendee's preferences. */
    public static final boolean calendarAllowOrganizerSpecifiedAlarms =
            value("debug_calendar_allow_organizer_specified_alarms", false);

    /** If true, fsync is skipped for redolog writes. */
    public static final boolean disableRedoLogFsync = value("debug_disable_redolog_fsync", false);

    /** If true, fsync is skipped for message blob file saving. */
    public static final boolean disableMessageStoreFsync = value("debug_disable_message_store_fsync", false);

    /** If true, convert TZ-relative times to UTC time in SOAP responses for
     *  non-recurring appointments/tasks.  Recurring series or instances are
     *  unaffected by this switch. */
    public static final boolean calendarForceUTC = value("debug_calendar_force_utc", false);

    /** Whether to send permission denied auto reply when organizer is not
     *  permitted to invite user and user is an indirect attendee through a
     *  mailing list rather than a directly named attendee.  Default is to
     *  suppress auto reply to reduce noise. */
    public static final boolean calendarEnableInviteDeniedReplyForUnlistedAttendee =
            value("debug_calendar_enable_invite_denied_reply_for_unlisted_attendee", false);

    /** If true, every item marked as "modified" in the Mailbox's cache is
     *  checked against the database at the end of the transaction in order
     *  to ensure cache consistency (very expensive) */
    public static final boolean checkMailboxCacheConsistency = value("debug_check_mailbox_cache_consistency", false);

    /** If true, the database layer assumes that every mailbox gets its own
     *  database instance and that all the tables in that database do *not*
     *  have the MAILBOX_ID column.  All mutable mailbox data, including
     *  sizes and checkpoints as well as out-of-office reply tracking and
     *  the "mailbox metadata" scratch space, are moved out of the ZIMBRA
     *  database and into this per-user database. */
    public static final boolean disableMailboxGroups = value("debug_disable_mailbox_group", false);

    /** If true, the database layer assumes that the MailboxManager
     *  implementation has an external mapping from accounts to mailbox
     *  IDs.  As a result, the ZIMBRA.MAILBOX table will not be populated
     *  and will not be read at startup. */
    public static final boolean externalMailboxDirectory =
            disableMailboxGroups && value("debug_external_mailbox_directory", false);

    /**
     * The number of {@code MBOXGROUP<N>} databases to distribute the users over. This is a middle ground between One
     * Huge Database (contention and the effects of corruption are issues) and database-per-user (which most DBMSes
     * can't deal with).
     */
    public static final int numMailboxGroups = disableMailboxGroups ?
            Integer.MAX_VALUE : Math.max(LC.zimbra_mailbox_groups.intValue(), 1);

    /** If true, more than one server may be sharing the same store and
     *  database install.  In that case, the server must perform extra checks
     *  to ensure that mailboxes "homed" on other servers are treated
     *  accordingly. */
    public static final boolean mockMultiserverInstall = value("debug_mock_multiserver_install", false);

    /** If true, the GAL sync visitor mechanism is disabled.  SyncGal will use
     *  the traditional way of adding matches to a SearchGalResult, then add
     *  each match in the SOAP response.  The GAL sync visitor mechanism
     *  reduces chance of OOME when there is a huge result. */
    public static final boolean disableGalSyncVisitor = value("debug_disable_gal_sync_visitor", false);
    public static final boolean disableCalendarTZMatchByID = value("debug_disable_calendar_tz_match_by_id", false);
    public static final boolean disableCalendarTZMatchByRule = value("debug_disable_calendar_tz_match_by_rule", false);
    public static final boolean disableMimeConvertersForCalendarBlobs =
            value("debug_force_mime_converters_for_calendar_blobs", false);
    public static final boolean enableTnefToICalendarConversion =
            value("debug_enable_tnef_to_icalendar_conversion", true);

    /** If true, disable the memcached-based folders/tags cache of mailboxes. */
    public static final boolean disableFoldersTagsCache = value("debug_disable_folders_tags_cache", false);
    public static final boolean enableContactLocalizedSort = value("debug_enable_contact_localized_sort", true);
    public static final boolean enableMigrateUserZimletPrefs = value("migrate_user_zimlet_prefs", false);
    public static final boolean disableGroupTargetForAdminRight = value("disable_group_target_for_admin_right", false);
    public static final boolean disableComputeGroupMembershipOptimization =
            value("disable_compute_group_membership_optimization", false);
    public static final boolean disableCalendarReminderEmail = value("debug_disable_calendar_reminder_email", false);
    public static final int imapSerializedSessionNotificationOverloadThreshold =
            value("debug_imap_serialized_session_notification_overload_threshold", 100);
    public static final int imapSessionSerializerFrequency = value("debug_imap_session_serializer_frequency", 120);
    public static final boolean imapCacheConsistencyCheck = value("debug_imap_cache_consistency_check", false);
    public static final int imapSessionInactivitySerializationTime =
            value("debug_imap_session_inactivity_serialization_time", 600);
    public static final int imapTotalNonserializedSessionFootprintLimit =
            value("debug_imap_total_nonserialized_session_footprint_limit", Integer.MAX_VALUE);
    public static final int imapNoninteractiveSessionLimit =
            value("debug_imap_noninteractive_session_limit", Integer.MAX_VALUE);
    public static final boolean imapTerminateSessionOnClose = value("imap_terminate_session_on_close", false);
    public static final boolean imapSerializeSessionOnClose = value("imap_serialize_session_on_close", true);
    public static final boolean imapProxyToLocalhost = value("imap_proxy_to_localhost", false);

    /** For QA only. bug 57279 */
    public static final boolean allowModifyingDeprecatedAttributes =
            value("allow_modifying_deprecated_attributes", true);
    public static final boolean enableRdate = value("debug_enable_calendar_rdate", false);
    public static final boolean enableThisAndFuture = value("debug_enable_calendar_thisandfuture", false);
    public static final boolean caldavAllowAttendeeForOrganizer =
            value("debug_caldav_allow_attendee_for_organizer", false);

    public static boolean certAuthCaptureClientCertificate =
        value("debug_certauth_capture_client_certificate", false);
    
    public static boolean running_unittest = 
        value("debug_running_unittest", false);
    
    public static boolean useInMemoryLdapServer = 
        value("debug_use_in_memory_ldap_server", false);

    public static final boolean disableShareExpirationListener =
            value("debug_disable_share_expiration_listener", false);

    private static boolean value(String key, boolean defaultValue) {
        String value = LC.get(key);
        return value.isEmpty() ? defaultValue : Boolean.parseBoolean(value);
    }

    private static int value(String key, int defaultValue) {
        String value = LC.get(key);
        try {
            return value.isEmpty() ? defaultValue : Integer.parseInt(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
