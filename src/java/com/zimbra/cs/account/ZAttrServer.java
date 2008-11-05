/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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
 * Created on Sep 23, 2004
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.account;

import java.util.Map;

/**
 * AUTO-GENERATED. DO NOT EDIT.
 *
 */
public class ZAttrServer extends NamedEntry {

    public ZAttrServer(String name, String id, Map<String,Object> attrs, Map<String,Object> defaults) {
        super(name, id, attrs, defaults);
    }

    ///// BEGIN-AUTO-GEN-REPLACE

    /* build: 5.0 schemers 20081104-1827 */

    /**
     * RFC2256: common name(s) for which the entity is known by
     *
     * @return cn, or null unset
     */
    @ZAttr(id=-1)
    public String getCn() {
        return getAttr(Provisioning.A_cn);
    }

    /**
     * RFC2256: descriptive information
     *
     * @return description, or null unset
     */
    @ZAttr(id=-1)
    public String getDescription() {
        return getAttr(Provisioning.A_description);
    }

    /**
     * Zimbra access control list
     *
     * @return zimbraACE, or ampty array if unset
     */
    @ZAttr(id=659)
    public String[] getACE() {
        return getMultiAttr(Provisioning.A_zimbraACE);
    }

    /**
     * SSL port for admin UI
     *
     * @return zimbraAdminPort, or null unset
     */
    @ZAttr(id=155)
    public String getAdminPort() {
        return getAttr(Provisioning.A_zimbraAdminPort);
    }

    /**
     * URL prefix for where the zimbraAdmin app resides on this server
     *
     * @return zimbraAdminURL, or null unset
     */
    @ZAttr(id=497)
    public String getAdminURL() {
        return getAttr(Provisioning.A_zimbraAdminURL);
    }

    /**
     * Maximum number of characters that will be indexed for a given MIME
     * part.
     *
     * @return zimbraAttachmentsIndexedTextLimit, or -1 if unset
     */
    @ZAttr(id=582)
    public int getAttachmentsIndexedTextLimit() {
        return getIntAttr(Provisioning.A_zimbraAttachmentsIndexedTextLimit, -1);
    }

    /**
     * Data for class that scans attachments during compose
     *
     * @return zimbraAttachmentsScanURL, or null unset
     */
    @ZAttr(id=239)
    public String getAttachmentsScanURL() {
        return getAttr(Provisioning.A_zimbraAttachmentsScanURL);
    }

    /**
     * length of each interval in auto-grouped backup
     *
     * @return zimbraBackupAutoGroupedInterval, or null unset
     */
    @ZAttr(id=513)
    public String getBackupAutoGroupedInterval() {
        return getAttr(Provisioning.A_zimbraBackupAutoGroupedInterval);
    }

    /**
     * number of groups to auto-group backups over
     *
     * @return zimbraBackupAutoGroupedNumGroups, or -1 if unset
     */
    @ZAttr(id=514)
    public int getBackupAutoGroupedNumGroups() {
        return getIntAttr(Provisioning.A_zimbraBackupAutoGroupedNumGroups, -1);
    }

    /**
     * if true, limit the number of mailboxes in auto-grouped backup to total
     * mailboxes divided by auto-group days
     *
     * @return zimbraBackupAutoGroupedThrottled, or false if unset
     */
    @ZAttr(id=515)
    public boolean isBackupAutoGroupedThrottled() {
        return getBooleanAttr(Provisioning.A_zimbraBackupAutoGroupedThrottled, false);
    }

    /**
     * backup mode
     *
     * <p>Valid values: [Auto-Grouped, Standard]
     *
     * @return zimbraBackupMode, or null unset
     */
    @ZAttr(id=512)
    public String getBackupMode() {
        return getAttr(Provisioning.A_zimbraBackupMode);
    }

    /**
     * Backup report email recipients
     *
     * @return zimbraBackupReportEmailRecipients, or ampty array if unset
     */
    @ZAttr(id=459)
    public String[] getBackupReportEmailRecipients() {
        return getMultiAttr(Provisioning.A_zimbraBackupReportEmailRecipients);
    }

    /**
     * Backup report email From address
     *
     * @return zimbraBackupReportEmailSender, or null unset
     */
    @ZAttr(id=460)
    public String getBackupReportEmailSender() {
        return getAttr(Provisioning.A_zimbraBackupReportEmailSender);
    }

    /**
     * Backup report email subject prefix
     *
     * @return zimbraBackupReportEmailSubjectPrefix, or null unset
     */
    @ZAttr(id=461)
    public String getBackupReportEmailSubjectPrefix() {
        return getAttr(Provisioning.A_zimbraBackupReportEmailSubjectPrefix);
    }

    /**
     * Default backup target path
     *
     * @return zimbraBackupTarget, or null unset
     */
    @ZAttr(id=458)
    public String getBackupTarget() {
        return getAttr(Provisioning.A_zimbraBackupTarget);
    }

    /**
     * Maximum number of days a DAILY recurrence rule can span; 0 means
     * unlimited
     *
     * @return zimbraCalendarRecurrenceDailyMaxDays, or -1 if unset
     */
    @ZAttr(id=661)
    public int getCalendarRecurrenceDailyMaxDays() {
        return getIntAttr(Provisioning.A_zimbraCalendarRecurrenceDailyMaxDays, -1);
    }

    /**
     * Maximum number of instances expanded per recurrence rule; 0 means
     * unlimited
     *
     * @return zimbraCalendarRecurrenceMaxInstances, or -1 if unset
     */
    @ZAttr(id=660)
    public int getCalendarRecurrenceMaxInstances() {
        return getIntAttr(Provisioning.A_zimbraCalendarRecurrenceMaxInstances, -1);
    }

    /**
     * Maximum number of months a MONTHLY recurrence rule can span; 0 means
     * unlimited
     *
     * @return zimbraCalendarRecurrenceMonthlyMaxMonths, or -1 if unset
     */
    @ZAttr(id=663)
    public int getCalendarRecurrenceMonthlyMaxMonths() {
        return getIntAttr(Provisioning.A_zimbraCalendarRecurrenceMonthlyMaxMonths, -1);
    }

    /**
     * Maximum number of years a recurrence rule can span for frequencies
     * other than DAILY/WEEKLY/MONTHLY/YEARLY; 0 means unlimited
     *
     * @return zimbraCalendarRecurrenceOtherFrequencyMaxYears, or -1 if unset
     */
    @ZAttr(id=665)
    public int getCalendarRecurrenceOtherFrequencyMaxYears() {
        return getIntAttr(Provisioning.A_zimbraCalendarRecurrenceOtherFrequencyMaxYears, -1);
    }

    /**
     * Maximum number of weeks a WEEKLY recurrence rule can span; 0 means
     * unlimited
     *
     * @return zimbraCalendarRecurrenceWeeklyMaxWeeks, or -1 if unset
     */
    @ZAttr(id=662)
    public int getCalendarRecurrenceWeeklyMaxWeeks() {
        return getIntAttr(Provisioning.A_zimbraCalendarRecurrenceWeeklyMaxWeeks, -1);
    }

    /**
     * Maximum number of years a YEARLY recurrence rule can span; 0 means
     * unlimited
     *
     * @return zimbraCalendarRecurrenceYearlyMaxYears, or -1 if unset
     */
    @ZAttr(id=664)
    public int getCalendarRecurrenceYearlyMaxYears() {
        return getIntAttr(Provisioning.A_zimbraCalendarRecurrenceYearlyMaxYears, -1);
    }

    /**
     * Type of HA cluster software in use; &quot;none&quot; by default,
     * &quot;RedHat&quot; for Red Hat cluster or &quot;Veritas&quot; for
     * Veritas Cluster Server from Symantec
     *
     * <p>Valid values: [Veritas, none, RedHat]
     *
     * @return zimbraClusterType, or null unset
     */
    @ZAttr(id=508)
    public String getClusterType() {
        return getAttr(Provisioning.A_zimbraClusterType);
    }

    /**
     * Maximum size in bytes for attachments
     *
     * @return zimbraFileUploadMaxSize, or -1 if unset
     */
    @ZAttr(id=227)
    public long getFileUploadMaxSize() {
        return getLongAttr(Provisioning.A_zimbraFileUploadMaxSize, -1);
    }

    /**
     * Minimum age of mail items whose filesystem data will be moved to
     * secondary storage (nnnnn[hmsd]).
     *
     * <p>Use getHsmAgeAsString to access value as a string.
     *
     * @see #getHsmAgeAsString()
     *
     * @return zimbraHsmAge in millseconds, or -1 if unset
     */
    @ZAttr(id=8)
    public long getHsmAge() {
        return getTimeInterval(Provisioning.A_zimbraHsmAge, -1);
    }

    /**
     * Minimum age of mail items whose filesystem data will be moved to
     * secondary storage (nnnnn[hmsd]).
     *
     * @return zimbraHsmAge, or null unset
     */
    @ZAttr(id=8)
    public String getHsmAgeAsString() {
        return getAttr(Provisioning.A_zimbraHsmAge);
    }

    /**
     * number of http handler threads
     *
     * @return zimbraHttpNumThreads, or -1 if unset
     */
    @ZAttr(id=518)
    public int getHttpNumThreads() {
        return getIntAttr(Provisioning.A_zimbraHttpNumThreads, -1);
    }

    /**
     * the http proxy URL to connect to when making outgoing connections
     * (Zimlet proxy, RSS/ATOM feeds, etc)
     *
     * @return zimbraHttpProxyURL, or ampty array if unset
     */
    @ZAttr(id=388)
    public String[] getHttpProxyURL() {
        return getMultiAttr(Provisioning.A_zimbraHttpProxyURL);
    }

    /**
     * Deprecated since: 5.0. not applicable for jetty. Orig desc: number of
     * https handler threads
     *
     * @return zimbraHttpSSLNumThreads, or -1 if unset
     */
    @ZAttr(id=519)
    public int getHttpSSLNumThreads() {
        return getIntAttr(Provisioning.A_zimbraHttpSSLNumThreads, -1);
    }

    /**
     * interface address(es) on which IM server should listen; if empty,
     * binds to all interfaces
     *
     * @return zimbraIMBindAddress, or ampty array if unset
     */
    @ZAttr(id=567)
    public String[] getIMBindAddress() {
        return getMultiAttr(Provisioning.A_zimbraIMBindAddress);
    }

    /**
     * Zimbra Systems Unique ID
     *
     * @return zimbraId, or null unset
     */
    @ZAttr(id=1)
    public String getId() {
        return getAttr(Provisioning.A_zimbraId);
    }

    /**
     * name to use in greeting and sign-off; if empty, uses hostname
     *
     * @return zimbraImapAdvertisedName, or null unset
     */
    @ZAttr(id=178)
    public String getImapAdvertisedName() {
        return getAttr(Provisioning.A_zimbraImapAdvertisedName);
    }

    /**
     * interface address(es) on which IMAP server should listen; if empty,
     * binds to all interfaces
     *
     * @return zimbraImapBindAddress, or ampty array if unset
     */
    @ZAttr(id=179)
    public String[] getImapBindAddress() {
        return getMultiAttr(Provisioning.A_zimbraImapBindAddress);
    }

    /**
     * Whether to bind to port on startup irrespective of whether the server
     * is enabled. Useful when port to bind is privileged and must be bound
     * early.
     *
     * @return zimbraImapBindOnStartup, or false if unset
     */
    @ZAttr(id=268)
    public boolean isImapBindOnStartup() {
        return getBooleanAttr(Provisioning.A_zimbraImapBindOnStartup, false);
    }

    /**
     * port number on which IMAP server should listen
     *
     * @return zimbraImapBindPort, or null unset
     */
    @ZAttr(id=180)
    public String getImapBindPort() {
        return getAttr(Provisioning.A_zimbraImapBindPort);
    }

    /**
     * whether or not to allow cleartext logins over a non SSL/TLS connection
     *
     * @return zimbraImapCleartextLoginEnabled, or false if unset
     */
    @ZAttr(id=185)
    public boolean isImapCleartextLoginEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraImapCleartextLoginEnabled, false);
    }

    /**
     * disabled IMAP capabilities. Capabilities are listed on the CAPABILITY
     * line, also known in RFCs as extensions
     *
     * @return zimbraImapDisabledCapability, or ampty array if unset
     */
    @ZAttr(id=443)
    public String[] getImapDisabledCapability() {
        return getMultiAttr(Provisioning.A_zimbraImapDisabledCapability);
    }

    /**
     * Whether to expose version on IMAP banner
     *
     * @return zimbraImapExposeVersionOnBanner, or false if unset
     */
    @ZAttr(id=693)
    public boolean isImapExposeVersionOnBanner() {
        return getBooleanAttr(Provisioning.A_zimbraImapExposeVersionOnBanner, false);
    }

    /**
     * number of handler threads
     *
     * @return zimbraImapNumThreads, or -1 if unset
     */
    @ZAttr(id=181)
    public int getImapNumThreads() {
        return getIntAttr(Provisioning.A_zimbraImapNumThreads, -1);
    }

    /**
     * port number on which IMAP proxy server should listen
     *
     * @return zimbraImapProxyBindPort, or null unset
     */
    @ZAttr(id=348)
    public String getImapProxyBindPort() {
        return getAttr(Provisioning.A_zimbraImapProxyBindPort);
    }

    /**
     * interface address(es) on which IMAP server should listen; if empty,
     * binds to all interfaces
     *
     * @return zimbraImapSSLBindAddress, or ampty array if unset
     */
    @ZAttr(id=182)
    public String[] getImapSSLBindAddress() {
        return getMultiAttr(Provisioning.A_zimbraImapSSLBindAddress);
    }

    /**
     * Whether to bind to port on startup irrespective of whether the server
     * is enabled. Useful when port to bind is privileged and must be bound
     * early.
     *
     * @return zimbraImapSSLBindOnStartup, or false if unset
     */
    @ZAttr(id=269)
    public boolean isImapSSLBindOnStartup() {
        return getBooleanAttr(Provisioning.A_zimbraImapSSLBindOnStartup, false);
    }

    /**
     * port number on which IMAP SSL server should listen on
     *
     * @return zimbraImapSSLBindPort, or null unset
     */
    @ZAttr(id=183)
    public String getImapSSLBindPort() {
        return getAttr(Provisioning.A_zimbraImapSSLBindPort);
    }

    /**
     * disabled IMAP SSL capabilities. Capabilities are listed on the
     * CAPABILITY line, also known in RFCs as extensions
     *
     * @return zimbraImapSSLDisabledCapability, or ampty array if unset
     */
    @ZAttr(id=444)
    public String[] getImapSSLDisabledCapability() {
        return getMultiAttr(Provisioning.A_zimbraImapSSLDisabledCapability);
    }

    /**
     * port number on which IMAPS proxy server should listen
     *
     * @return zimbraImapSSLProxyBindPort, or null unset
     */
    @ZAttr(id=349)
    public String getImapSSLProxyBindPort() {
        return getAttr(Provisioning.A_zimbraImapSSLProxyBindPort);
    }

    /**
     * whether IMAP SSL server is enabled for a given server
     *
     * @return zimbraImapSSLServerEnabled, or false if unset
     */
    @ZAttr(id=184)
    public boolean isImapSSLServerEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraImapSSLServerEnabled, false);
    }

    /**
     * whether POP3 SASL GSSAPI is enabled for a given server
     *
     * @return zimbraImapSaslGssapiEnabled, or false if unset
     */
    @ZAttr(id=555)
    public boolean isImapSaslGssapiEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraImapSaslGssapiEnabled, false);
    }

    /**
     * whether IMAP server is enabled for a given server
     *
     * @return zimbraImapServerEnabled, or false if unset
     */
    @ZAttr(id=176)
    public boolean isImapServerEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraImapServerEnabled, false);
    }

    /**
     * true if this server is the monitor host
     *
     * @return zimbraIsMonitorHost, or false if unset
     */
    @ZAttr(id=132)
    public boolean isIsMonitorHost() {
        return getBooleanAttr(Provisioning.A_zimbraIsMonitorHost, false);
    }

    /**
     * name to use in greeting and sign-off; if empty, uses hostname
     *
     * @return zimbraLmtpAdvertisedName, or null unset
     */
    @ZAttr(id=23)
    public String getLmtpAdvertisedName() {
        return getAttr(Provisioning.A_zimbraLmtpAdvertisedName);
    }

    /**
     * interface address(es) on which LMTP server should listen; if empty,
     * binds to all interfaces
     *
     * @return zimbraLmtpBindAddress, or ampty array if unset
     */
    @ZAttr(id=25)
    public String[] getLmtpBindAddress() {
        return getMultiAttr(Provisioning.A_zimbraLmtpBindAddress);
    }

    /**
     * Whether to bind to port on startup irrespective of whether the server
     * is enabled. Useful when port to bind is privileged and must be bound
     * early.
     *
     * @return zimbraLmtpBindOnStartup, or false if unset
     */
    @ZAttr(id=270)
    public boolean isLmtpBindOnStartup() {
        return getBooleanAttr(Provisioning.A_zimbraLmtpBindOnStartup, false);
    }

    /**
     * port number on which LMTP server should listen
     *
     * @return zimbraLmtpBindPort, or null unset
     */
    @ZAttr(id=24)
    public String getLmtpBindPort() {
        return getAttr(Provisioning.A_zimbraLmtpBindPort);
    }

    /**
     * Whether to expose version on LMTP banner
     *
     * @return zimbraLmtpExposeVersionOnBanner, or false if unset
     */
    @ZAttr(id=691)
    public boolean isLmtpExposeVersionOnBanner() {
        return getBooleanAttr(Provisioning.A_zimbraLmtpExposeVersionOnBanner, false);
    }

    /**
     * number of handler threads, should match MTA concurrency setting for
     * this server
     *
     * @return zimbraLmtpNumThreads, or -1 if unset
     */
    @ZAttr(id=26)
    public int getLmtpNumThreads() {
        return getIntAttr(Provisioning.A_zimbraLmtpNumThreads, -1);
    }

    /**
     * If true, a permanent failure (552) is returned when the user is over
     * quota. If false, a temporary failure (452) is returned.
     *
     * @return zimbraLmtpPermanentFailureWhenOverQuota, or false if unset
     */
    @ZAttr(id=657)
    public boolean isLmtpPermanentFailureWhenOverQuota() {
        return getBooleanAttr(Provisioning.A_zimbraLmtpPermanentFailureWhenOverQuota, false);
    }

    /**
     * whether LMTP server is enabled for a given server
     *
     * @return zimbraLmtpServerEnabled, or false if unset
     */
    @ZAttr(id=630)
    public boolean isLmtpServerEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraLmtpServerEnabled, false);
    }

    /**
     * locale of entry, e.g. en_US
     *
     * @return zimbraLocale, or null unset
     */
    @ZAttr(id=345)
    public String getLocaleAsString() {
        return getAttr(Provisioning.A_zimbraLocale);
    }

    /**
     * whether mailbox server should log to syslog
     *
     * @return zimbraLogToSyslog, or false if unset
     */
    @ZAttr(id=520)
    public boolean isLogToSyslog() {
        return getBooleanAttr(Provisioning.A_zimbraLogToSyslog, false);
    }

    /**
     * Incoming messages larger than this number of bytes are streamed to
     * disk during LMTP delivery, instead of being read into memory. This
     * limits memory consumption at the expense of higher disk utilization.
     *
     * @return zimbraMailDiskStreamingThreshold, or -1 if unset
     */
    @ZAttr(id=565)
    public int getMailDiskStreamingThreshold() {
        return getIntAttr(Provisioning.A_zimbraMailDiskStreamingThreshold, -1);
    }

    /**
     * The id of the last purged mailbox.
     *
     * @return zimbraMailLastPurgedMailboxId, or -1 if unset
     */
    @ZAttr(id=543)
    public int getMailLastPurgedMailboxId() {
        return getIntAttr(Provisioning.A_zimbraMailLastPurgedMailboxId, -1);
    }

    /**
     * whether to run HTTP or HTTPS or both/mixed mode or redirect mode. See
     * also related attributes zimbraMailPort and zimbraMailSSLPort
     *
     * <p>Valid values: [both, https, redirect, http, mixed]
     *
     * @return zimbraMailMode, or null unset
     */
    @ZAttr(id=308)
    public String getMailMode() {
        return getAttr(Provisioning.A_zimbraMailMode);
    }

    /**
     * HTTP port for end-user UI
     *
     * @return zimbraMailPort, or null unset
     */
    @ZAttr(id=154)
    public String getMailPort() {
        return getAttr(Provisioning.A_zimbraMailPort);
    }

    /**
     * HTTP proxy port
     *
     * @return zimbraMailProxyPort, or null unset
     */
    @ZAttr(id=626)
    public String getMailProxyPort() {
        return getAttr(Provisioning.A_zimbraMailProxyPort);
    }

    /**
     * Sleep time between subsequent mailbox purges. 0 means that mailbox
     * purging is disabled.
     *
     * <p>Use getMailPurgeSleepIntervalAsString to access value as a string.
     *
     * @see #getMailPurgeSleepIntervalAsString()
     *
     * @return zimbraMailPurgeSleepInterval in millseconds, or -1 if unset
     */
    @ZAttr(id=542)
    public long getMailPurgeSleepInterval() {
        return getTimeInterval(Provisioning.A_zimbraMailPurgeSleepInterval, -1);
    }

    /**
     * Sleep time between subsequent mailbox purges. 0 means that mailbox
     * purging is disabled.
     *
     * @return zimbraMailPurgeSleepInterval, or null unset
     */
    @ZAttr(id=542)
    public String getMailPurgeSleepIntervalAsString() {
        return getAttr(Provisioning.A_zimbraMailPurgeSleepInterval);
    }

    /**
     * whether to send back a refer tag in an auth response to force a client
     * redirect. always - always send refer wronghost - send refer if only if
     * the account being authenticated does not live on this mail host
     * reverse-proxied - reverse proxy is in place and should never send
     * refer
     *
     * <p>Valid values: [wronghost, always, reverse-proxied]
     *
     * @return zimbraMailReferMode, or null unset
     */
    @ZAttr(id=613)
    public String getMailReferMode() {
        return getAttr(Provisioning.A_zimbraMailReferMode);
    }

    /**
     * SSL port for end-user UI
     *
     * @return zimbraMailSSLPort, or null unset
     */
    @ZAttr(id=166)
    public String getMailSSLPort() {
        return getAttr(Provisioning.A_zimbraMailSSLPort);
    }

    /**
     * SSL port HTTP proxy
     *
     * @return zimbraMailSSLProxyPort, or null unset
     */
    @ZAttr(id=627)
    public String getMailSSLProxyPort() {
        return getAttr(Provisioning.A_zimbraMailSSLProxyPort);
    }

    /**
     * URL prefix for where the zimbra app resides on this server
     *
     * @return zimbraMailURL, or null unset
     */
    @ZAttr(id=340)
    public String getMailURL() {
        return getAttr(Provisioning.A_zimbraMailURL);
    }

    /**
     * interface address(es) on which memcached server
     *
     * @return zimbraMemcachedBindAddress, or ampty array if unset
     */
    @ZAttr(id=581)
    public String[] getMemcachedBindAddress() {
        return getMultiAttr(Provisioning.A_zimbraMemcachedBindAddress);
    }

    /**
     * port number on which memcached server should listen
     *
     * @return zimbraMemcachedBindPort, or null unset
     */
    @ZAttr(id=580)
    public String getMemcachedBindPort() {
        return getAttr(Provisioning.A_zimbraMemcachedBindPort);
    }

    /**
     * Size limit in number of bytes on the message cache.
     *
     * @return zimbraMessageCacheSize, or -1 if unset
     */
    @ZAttr(id=297)
    public int getMessageCacheSize() {
        return getIntAttr(Provisioning.A_zimbraMessageCacheSize, -1);
    }

    /**
     * mta anti spam lock method.
     *
     * @return zimbraMtaAntiSpamLockMethod, or null unset
     */
    @ZAttr(id=612)
    public String getMtaAntiSpamLockMethod() {
        return getAttr(Provisioning.A_zimbraMtaAntiSpamLockMethod);
    }

    /**
     * Value for postconf smtpd_use_tls
     *
     * @return zimbraMtaAuthEnabled, or false if unset
     */
    @ZAttr(id=194)
    public boolean isMtaAuthEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraMtaAuthEnabled, false);
    }

    /**
     * Host running SOAP service for use by MTA auth. Setting this sets
     * zimbraMtaAuthURL via attr callback mechanism.
     *
     * @return zimbraMtaAuthHost, or ampty array if unset
     */
    @ZAttr(id=309)
    public String[] getMtaAuthHost() {
        return getMultiAttr(Provisioning.A_zimbraMtaAuthHost);
    }

    /**
     * whether this server is a mta auth target
     *
     * @return zimbraMtaAuthTarget, or false if unset
     */
    @ZAttr(id=505)
    public boolean isMtaAuthTarget() {
        return getBooleanAttr(Provisioning.A_zimbraMtaAuthTarget, false);
    }

    /**
     * URL at which this MTA (via zimbra saslauthd) should authenticate. Set
     * by setting zimbraMtaAuthHost.
     *
     * @return zimbraMtaAuthURL, or ampty array if unset
     */
    @ZAttr(id=310)
    public String[] getMtaAuthURL() {
        return getMultiAttr(Provisioning.A_zimbraMtaAuthURL);
    }

    /**
     * Value for postconf disable_dns_lookups (note enable v. disable)
     *
     * @return zimbraMtaDnsLookupsEnabled, or false if unset
     */
    @ZAttr(id=197)
    public boolean isMtaDnsLookupsEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraMtaDnsLookupsEnabled, false);
    }

    /**
     * value of postfix mydestination
     *
     * @return zimbraMtaMyDestination, or null unset
     */
    @ZAttr(id=524)
    public String getMtaMyDestination() {
        return getAttr(Provisioning.A_zimbraMtaMyDestination);
    }

    /**
     * value of postfix myhostname
     *
     * @return zimbraMtaMyHostname, or null unset
     */
    @ZAttr(id=509)
    public String getMtaMyHostname() {
        return getAttr(Provisioning.A_zimbraMtaMyHostname);
    }

    /**
     * value of postfix mynetworks
     *
     * @return zimbraMtaMyNetworks, or ampty array if unset
     */
    @ZAttr(id=311)
    public String[] getMtaMyNetworks() {
        return getMultiAttr(Provisioning.A_zimbraMtaMyNetworks);
    }

    /**
     * value of postfix myorigin
     *
     * @return zimbraMtaMyOrigin, or null unset
     */
    @ZAttr(id=510)
    public String getMtaMyOrigin() {
        return getAttr(Provisioning.A_zimbraMtaMyOrigin);
    }

    /**
     * value for postfix non_smtpd_milters
     *
     * @return zimbraMtaNonSmtpdMilters, or null unset
     */
    @ZAttr(id=673)
    public String getMtaNonSmtpdMilters() {
        return getAttr(Provisioning.A_zimbraMtaNonSmtpdMilters);
    }

    /**
     * Value for postconf relayhost
     *
     * @return zimbraMtaRelayHost, or ampty array if unset
     */
    @ZAttr(id=199)
    public String[] getMtaRelayHost() {
        return getMultiAttr(Provisioning.A_zimbraMtaRelayHost);
    }

    /**
     * value for postfix smtpd_milters
     *
     * @return zimbraMtaSmtpdMilters, or null unset
     */
    @ZAttr(id=672)
    public String getMtaSmtpdMilters() {
        return getAttr(Provisioning.A_zimbraMtaSmtpdMilters);
    }

    /**
     * Value for postconf smtpd_tls_auth_only
     *
     * @return zimbraMtaTlsAuthOnly, or false if unset
     */
    @ZAttr(id=200)
    public boolean isMtaTlsAuthOnly() {
        return getBooleanAttr(Provisioning.A_zimbraMtaTlsAuthOnly, false);
    }

    /**
     * The size of Wiki / Notebook folder cache on the server.
     *
     * @return zimbraNotebookFolderCacheSize, or -1 if unset
     */
    @ZAttr(id=370)
    public int getNotebookFolderCacheSize() {
        return getIntAttr(Provisioning.A_zimbraNotebookFolderCacheSize, -1);
    }

    /**
     * The maximum number of cached templates in each Wiki / Notebook folder
     * cache.
     *
     * @return zimbraNotebookMaxCachedTemplatesPerFolder, or -1 if unset
     */
    @ZAttr(id=371)
    public int getNotebookMaxCachedTemplatesPerFolder() {
        return getIntAttr(Provisioning.A_zimbraNotebookMaxCachedTemplatesPerFolder, -1);
    }

    /**
     * The size of composed Wiki / Notebook page cache on the server.
     *
     * @return zimbraNotebookPageCacheSize, or -1 if unset
     */
    @ZAttr(id=369)
    public int getNotebookPageCacheSize() {
        return getIntAttr(Provisioning.A_zimbraNotebookPageCacheSize, -1);
    }

    /**
     * administrative notes
     *
     * @return zimbraNotes, or null unset
     */
    @ZAttr(id=9)
    public String getNotes() {
        return getAttr(Provisioning.A_zimbraNotes);
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Network interface on which notification server
     * should listen; if empty, binds to all interfaces.
     *
     * @return zimbraNotifyBindAddress, or ampty array if unset
     */
    @ZAttr(id=317)
    public String[] getNotifyBindAddress() {
        return getMultiAttr(Provisioning.A_zimbraNotifyBindAddress);
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Port number on which notification server should
     * listen.
     *
     * @return zimbraNotifyBindPort, or -1 if unset
     */
    @ZAttr(id=318)
    public int getNotifyBindPort() {
        return getIntAttr(Provisioning.A_zimbraNotifyBindPort, -1);
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Network interface on which SSL notification server
     * should listen; if empty, binds to all interfaces
     *
     * @return zimbraNotifySSLBindAddress, or ampty array if unset
     */
    @ZAttr(id=320)
    public String[] getNotifySSLBindAddress() {
        return getMultiAttr(Provisioning.A_zimbraNotifySSLBindAddress);
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Port number on which notification server should
     * listen.
     *
     * @return zimbraNotifySSLBindPort, or -1 if unset
     */
    @ZAttr(id=321)
    public int getNotifySSLBindPort() {
        return getIntAttr(Provisioning.A_zimbraNotifySSLBindPort, -1);
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Whether SSL notification server should be enabled.
     *
     * @return zimbraNotifySSLServerEnabled, or false if unset
     */
    @ZAttr(id=319)
    public boolean isNotifySSLServerEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraNotifySSLServerEnabled, false);
    }

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Whether notification server should be enabled.
     *
     * @return zimbraNotifyServerEnabled, or false if unset
     */
    @ZAttr(id=316)
    public boolean isNotifyServerEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraNotifyServerEnabled, false);
    }

    /**
     * name to use in greeting and sign-off; if empty, uses hostname
     *
     * @return zimbraPop3AdvertisedName, or null unset
     */
    @ZAttr(id=93)
    public String getPop3AdvertisedName() {
        return getAttr(Provisioning.A_zimbraPop3AdvertisedName);
    }

    /**
     * interface address(es) on which POP3 server should listen; if empty,
     * binds to all interfaces
     *
     * @return zimbraPop3BindAddress, or ampty array if unset
     */
    @ZAttr(id=95)
    public String[] getPop3BindAddress() {
        return getMultiAttr(Provisioning.A_zimbraPop3BindAddress);
    }

    /**
     * Whether to bind to port on startup irrespective of whether the server
     * is enabled. Useful when port to bind is privileged and must be bound
     * early.
     *
     * @return zimbraPop3BindOnStartup, or false if unset
     */
    @ZAttr(id=271)
    public boolean isPop3BindOnStartup() {
        return getBooleanAttr(Provisioning.A_zimbraPop3BindOnStartup, false);
    }

    /**
     * port number on which POP3 server should listen
     *
     * @return zimbraPop3BindPort, or null unset
     */
    @ZAttr(id=94)
    public String getPop3BindPort() {
        return getAttr(Provisioning.A_zimbraPop3BindPort);
    }

    /**
     * whether or not to allow cleartext logins over a non SSL/TLS connection
     *
     * @return zimbraPop3CleartextLoginEnabled, or false if unset
     */
    @ZAttr(id=189)
    public boolean isPop3CleartextLoginEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPop3CleartextLoginEnabled, false);
    }

    /**
     * Whether to expose version on POP3 banner
     *
     * @return zimbraPop3ExposeVersionOnBanner, or false if unset
     */
    @ZAttr(id=692)
    public boolean isPop3ExposeVersionOnBanner() {
        return getBooleanAttr(Provisioning.A_zimbraPop3ExposeVersionOnBanner, false);
    }

    /**
     * number of handler threads
     *
     * @return zimbraPop3NumThreads, or -1 if unset
     */
    @ZAttr(id=96)
    public int getPop3NumThreads() {
        return getIntAttr(Provisioning.A_zimbraPop3NumThreads, -1);
    }

    /**
     * port number on which POP3 proxy server should listen
     *
     * @return zimbraPop3ProxyBindPort, or null unset
     */
    @ZAttr(id=350)
    public String getPop3ProxyBindPort() {
        return getAttr(Provisioning.A_zimbraPop3ProxyBindPort);
    }

    /**
     * interface address(es) on which POP3 server should listen; if empty,
     * binds to all interfaces
     *
     * @return zimbraPop3SSLBindAddress, or ampty array if unset
     */
    @ZAttr(id=186)
    public String[] getPop3SSLBindAddress() {
        return getMultiAttr(Provisioning.A_zimbraPop3SSLBindAddress);
    }

    /**
     * Whether to bind to port on startup irrespective of whether the server
     * is enabled. Useful when port to bind is privileged and must be bound
     * early.
     *
     * @return zimbraPop3SSLBindOnStartup, or false if unset
     */
    @ZAttr(id=272)
    public boolean isPop3SSLBindOnStartup() {
        return getBooleanAttr(Provisioning.A_zimbraPop3SSLBindOnStartup, false);
    }

    /**
     * port number on which POP3 server should listen
     *
     * @return zimbraPop3SSLBindPort, or null unset
     */
    @ZAttr(id=187)
    public String getPop3SSLBindPort() {
        return getAttr(Provisioning.A_zimbraPop3SSLBindPort);
    }

    /**
     * port number on which POP3S proxy server should listen
     *
     * @return zimbraPop3SSLProxyBindPort, or null unset
     */
    @ZAttr(id=351)
    public String getPop3SSLProxyBindPort() {
        return getAttr(Provisioning.A_zimbraPop3SSLProxyBindPort);
    }

    /**
     * whether POP3 SSL server is enabled for a server
     *
     * @return zimbraPop3SSLServerEnabled, or false if unset
     */
    @ZAttr(id=188)
    public boolean isPop3SSLServerEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPop3SSLServerEnabled, false);
    }

    /**
     * whether POP3 SASL GSSAPI is enabled for a given server
     *
     * @return zimbraPop3SaslGssapiEnabled, or false if unset
     */
    @ZAttr(id=554)
    public boolean isPop3SaslGssapiEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPop3SaslGssapiEnabled, false);
    }

    /**
     * whether IMAP is enabled for a server
     *
     * @return zimbraPop3ServerEnabled, or false if unset
     */
    @ZAttr(id=177)
    public boolean isPop3ServerEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPop3ServerEnabled, false);
    }

    /**
     * redolog rollover destination
     *
     * @return zimbraRedoLogArchiveDir, or null unset
     */
    @ZAttr(id=76)
    public String getRedoLogArchiveDir() {
        return getAttr(Provisioning.A_zimbraRedoLogArchiveDir);
    }

    /**
     * whether logs are delete on rollover or archived
     *
     * @return zimbraRedoLogDeleteOnRollover, or false if unset
     */
    @ZAttr(id=251)
    public boolean isRedoLogDeleteOnRollover() {
        return getBooleanAttr(Provisioning.A_zimbraRedoLogDeleteOnRollover, false);
    }

    /**
     * whether redo logging is enabled
     *
     * @return zimbraRedoLogEnabled, or false if unset
     */
    @ZAttr(id=74)
    public boolean isRedoLogEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraRedoLogEnabled, false);
    }

    /**
     * how frequently writes to redo log get fsynced to disk
     *
     * @return zimbraRedoLogFsyncIntervalMS, or -1 if unset
     */
    @ZAttr(id=79)
    public int getRedoLogFsyncIntervalMS() {
        return getIntAttr(Provisioning.A_zimbraRedoLogFsyncIntervalMS, -1);
    }

    /**
     * name and location of the redolog file
     *
     * @return zimbraRedoLogLogPath, or null unset
     */
    @ZAttr(id=75)
    public String getRedoLogLogPath() {
        return getAttr(Provisioning.A_zimbraRedoLogLogPath);
    }

    /**
     * provider class name for redo logging
     *
     * @return zimbraRedoLogProvider, or ampty array if unset
     */
    @ZAttr(id=225)
    public String[] getRedoLogProvider() {
        return getMultiAttr(Provisioning.A_zimbraRedoLogProvider);
    }

    /**
     * redo.log file rolls over when it gets to this size
     *
     * @return zimbraRedoLogRolloverFileSizeKB, or -1 if unset
     */
    @ZAttr(id=78)
    public int getRedoLogRolloverFileSizeKB() {
        return getIntAttr(Provisioning.A_zimbraRedoLogRolloverFileSizeKB, -1);
    }

    /**
     * Path to remote management command to execute on this server
     *
     * @return zimbraRemoteManagementCommand, or null unset
     */
    @ZAttr(id=336)
    public String getRemoteManagementCommand() {
        return getAttr(Provisioning.A_zimbraRemoteManagementCommand);
    }

    /**
     * Port on which remote management sshd listening on this server.
     *
     * @return zimbraRemoteManagementPort, or -1 if unset
     */
    @ZAttr(id=339)
    public int getRemoteManagementPort() {
        return getIntAttr(Provisioning.A_zimbraRemoteManagementPort, -1);
    }

    /**
     * Private key this server should use to access another server
     *
     * @return zimbraRemoteManagementPrivateKeyPath, or null unset
     */
    @ZAttr(id=338)
    public String getRemoteManagementPrivateKeyPath() {
        return getAttr(Provisioning.A_zimbraRemoteManagementPrivateKeyPath);
    }

    /**
     * Login name of user allowed to execute remote management command
     *
     * @return zimbraRemoteManagementUser, or null unset
     */
    @ZAttr(id=337)
    public String getRemoteManagementUser() {
        return getAttr(Provisioning.A_zimbraRemoteManagementUser);
    }

    /**
     * The default realm that will be used by NGINX mail proxy, when the
     * realm is not specified in GSSAPI Authentication
     *
     * @return zimbraReverseProxyDefaultRealm, or null unset
     */
    @ZAttr(id=703)
    public String getReverseProxyDefaultRealm() {
        return getAttr(Provisioning.A_zimbraReverseProxyDefaultRealm);
    }

    /**
     * Whether to enable HTTP proxy
     *
     * @return zimbraReverseProxyHttpEnabled, or false if unset
     */
    @ZAttr(id=628)
    public boolean isReverseProxyHttpEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraReverseProxyHttpEnabled, false);
    }

    /**
     * NGINX reverse proxy imap capabilities
     *
     * @return zimbraReverseProxyImapEnabledCapability, or ampty array if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=719)
    public String[] getReverseProxyImapEnabledCapability() {
        return getMultiAttr(Provisioning.A_zimbraReverseProxyImapEnabledCapability);
    }

    /**
     * Whether to expose version on Proxy IMAP banner
     *
     * @return zimbraReverseProxyImapExposeVersionOnBanner, or false if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=713)
    public boolean isReverseProxyImapExposeVersionOnBanner() {
        return getBooleanAttr(Provisioning.A_zimbraReverseProxyImapExposeVersionOnBanner, false);
    }

    /**
     * whether IMAP SASL GSSAPI is enabled for reverse proxy
     *
     * @return zimbraReverseProxyImapSaslGssapiEnabled, or false if unset
     */
    @ZAttr(id=643)
    public boolean isReverseProxyImapSaslGssapiEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraReverseProxyImapSaslGssapiEnabled, false);
    }

    /**
     * whether IMAP SASL PLAIN is enabled for reverse proxy
     *
     * @return zimbraReverseProxyImapSaslPlainEnabled, or false if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=728)
    public boolean isReverseProxyImapSaslPlainEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraReverseProxyImapSaslPlainEnabled, false);
    }

    /**
     * on - on the plain POP/IMAP port, starttls is allowed off - no starttls
     * is offered on plain port only - you have to use starttls before clear
     * text login
     *
     * <p>Valid values: [only, off, on]
     *
     * @return zimbraReverseProxyImapStartTlsMode, or null unset
     */
    @ZAttr(id=641)
    public String getReverseProxyImapStartTlsMode() {
        return getAttr(Provisioning.A_zimbraReverseProxyImapStartTlsMode);
    }

    /**
     * Time interval after which NGINX mail proxy will disconnect an inactive
     * IMAP/POP connection
     *
     * <p>Use getReverseProxyInactivityTimeoutAsString to access value as a string.
     *
     * @see #getReverseProxyInactivityTimeoutAsString()
     *
     * @return zimbraReverseProxyInactivityTimeout in millseconds, or -1 if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=735)
    public long getReverseProxyInactivityTimeout() {
        return getTimeInterval(Provisioning.A_zimbraReverseProxyInactivityTimeout, -1);
    }

    /**
     * Time interval after which NGINX mail proxy will disconnect an inactive
     * IMAP/POP connection
     *
     * @return zimbraReverseProxyInactivityTimeout, or null unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=735)
    public String getReverseProxyInactivityTimeoutAsString() {
        return getAttr(Provisioning.A_zimbraReverseProxyInactivityTimeout);
    }

    /**
     * Log level for NGINX Proxy error log
     *
     * <p>Valid values: [debug, info, crit, warn, error, notice]
     *
     * @return zimbraReverseProxyLogLevel, or null unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=723)
    public String getReverseProxyLogLevel() {
        return getAttr(Provisioning.A_zimbraReverseProxyLogLevel);
    }

    /**
     * whether this server is a reverse proxy lookup target
     *
     * @return zimbraReverseProxyLookupTarget, or false if unset
     */
    @ZAttr(id=504)
    public boolean isReverseProxyLookupTarget() {
        return getBooleanAttr(Provisioning.A_zimbraReverseProxyLookupTarget, false);
    }

    /**
     * Whether to enable IMAP/POP proxy
     *
     * @return zimbraReverseProxyMailEnabled, or false if unset
     */
    @ZAttr(id=629)
    public boolean isReverseProxyMailEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraReverseProxyMailEnabled, false);
    }

    /**
     * whether to run proxy in HTTP, HTTPS, both, mixed, or redirect mode.
     * See also related attributes zimbraMailProxyPort and
     * zimbraMailSSLProxyPort
     *
     * <p>Valid values: [both, https, redirect, http, mixed]
     *
     * @return zimbraReverseProxyMailMode, or null unset
     */
    @ZAttr(id=685)
    public String getReverseProxyMailMode() {
        return getAttr(Provisioning.A_zimbraReverseProxyMailMode);
    }

    /**
     * whether NGINX mail proxy will pass upstream server errors back to the
     * downstream email clients
     *
     * @return zimbraReverseProxyPassErrors, or false if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=736)
    public boolean isReverseProxyPassErrors() {
        return getBooleanAttr(Provisioning.A_zimbraReverseProxyPassErrors, false);
    }

    /**
     * NGINX reverse proxy pop3 capabilities
     *
     * @return zimbraReverseProxyPop3EnabledCapability, or ampty array if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=721)
    public String[] getReverseProxyPop3EnabledCapability() {
        return getMultiAttr(Provisioning.A_zimbraReverseProxyPop3EnabledCapability);
    }

    /**
     * Whether to expose version on Proxy POP3 banner
     *
     * @return zimbraReverseProxyPop3ExposeVersionOnBanner, or false if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=712)
    public boolean isReverseProxyPop3ExposeVersionOnBanner() {
        return getBooleanAttr(Provisioning.A_zimbraReverseProxyPop3ExposeVersionOnBanner, false);
    }

    /**
     * whether POP3 SASL GSSAPI is enabled for reverse proxy
     *
     * @return zimbraReverseProxyPop3SaslGssapiEnabled, or false if unset
     */
    @ZAttr(id=644)
    public boolean isReverseProxyPop3SaslGssapiEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraReverseProxyPop3SaslGssapiEnabled, false);
    }

    /**
     * whether POP3 SASL PLAIN is enabled for reverse proxy
     *
     * @return zimbraReverseProxyPop3SaslPlainEnabled, or false if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=729)
    public boolean isReverseProxyPop3SaslPlainEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraReverseProxyPop3SaslPlainEnabled, false);
    }

    /**
     * on - on the plain POP/IMAP port, starttls is allowed off - no starttls
     * is offered on plain port only - you have to use starttls before clear
     * text login
     *
     * <p>Valid values: [only, off, on]
     *
     * @return zimbraReverseProxyPop3StartTlsMode, or null unset
     */
    @ZAttr(id=642)
    public String getReverseProxyPop3StartTlsMode() {
        return getAttr(Provisioning.A_zimbraReverseProxyPop3StartTlsMode);
    }

    /**
     * Time interval after which NGINX will fail over to the next route
     * lookup handler, if a handler does not respond to the route lookup
     * request within this time
     *
     * <p>Use getReverseProxyRouteLookupTimeoutAsString to access value as a string.
     *
     * @see #getReverseProxyRouteLookupTimeoutAsString()
     *
     * @return zimbraReverseProxyRouteLookupTimeout in millseconds, or -1 if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=745)
    public long getReverseProxyRouteLookupTimeout() {
        return getTimeInterval(Provisioning.A_zimbraReverseProxyRouteLookupTimeout, -1);
    }

    /**
     * Time interval after which NGINX will fail over to the next route
     * lookup handler, if a handler does not respond to the route lookup
     * request within this time
     *
     * @return zimbraReverseProxyRouteLookupTimeout, or null unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=745)
    public String getReverseProxyRouteLookupTimeoutAsString() {
        return getAttr(Provisioning.A_zimbraReverseProxyRouteLookupTimeout);
    }

    /**
     * Maximum number of connections that an NGINX Proxy worker process is
     * allowed to handle
     *
     * @return zimbraReverseProxyWorkerConnections, or -1 if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=725)
    public int getReverseProxyWorkerConnections() {
        return getIntAttr(Provisioning.A_zimbraReverseProxyWorkerConnections, -1);
    }

    /**
     * Number of worker processes of NGINX Proxy
     *
     * @return zimbraReverseProxyWorkerProcesses, or -1 if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=724)
    public int getReverseProxyWorkerProcesses() {
        return getIntAttr(Provisioning.A_zimbraReverseProxyWorkerProcesses, -1);
    }

    /**
     * SSL certificate
     *
     * @return zimbraSSLCertificate, or null unset
     */
    @ZAttr(id=563)
    public String getSSLCertificate() {
        return getAttr(Provisioning.A_zimbraSSLCertificate);
    }

    /**
     * SSL private key
     *
     * @return zimbraSSLPrivateKey, or null unset
     */
    @ZAttr(id=564)
    public String getSSLPrivateKey() {
        return getAttr(Provisioning.A_zimbraSSLPrivateKey);
    }

    /**
     * Maximum number of scheduled tasks that can run simultaneously.
     *
     * @return zimbraScheduledTaskNumThreads, or -1 if unset
     */
    @ZAttr(id=522)
    public int getScheduledTaskNumThreads() {
        return getIntAttr(Provisioning.A_zimbraScheduledTaskNumThreads, -1);
    }

    /**
     * services that are enabled on this server
     *
     * @return zimbraServiceEnabled, or ampty array if unset
     */
    @ZAttr(id=220)
    public String[] getServiceEnabled() {
        return getMultiAttr(Provisioning.A_zimbraServiceEnabled);
    }

    /**
     * public hostname of the host
     *
     * @return zimbraServiceHostname, or null unset
     */
    @ZAttr(id=65)
    public String getServiceHostname() {
        return getAttr(Provisioning.A_zimbraServiceHostname);
    }

    /**
     * services that are installed on this server
     *
     * @return zimbraServiceInstalled, or ampty array if unset
     */
    @ZAttr(id=221)
    public String[] getServiceInstalled() {
        return getMultiAttr(Provisioning.A_zimbraServiceInstalled);
    }

    /**
     * the SMTP server to connect to when sending mail
     *
     * @return zimbraSmtpHostname, or ampty array if unset
     */
    @ZAttr(id=97)
    public String[] getSmtpHostname() {
        return getMultiAttr(Provisioning.A_zimbraSmtpHostname);
    }

    /**
     * the SMTP server port to connect to when sending mail
     *
     * @return zimbraSmtpPort, or null unset
     */
    @ZAttr(id=98)
    public String getSmtpPort() {
        return getAttr(Provisioning.A_zimbraSmtpPort);
    }

    /**
     * Value of the mail.smtp.sendpartial property
     *
     * @return zimbraSmtpSendPartial, or false if unset
     */
    @ZAttr(id=249)
    public boolean isSmtpSendPartial() {
        return getBooleanAttr(Provisioning.A_zimbraSmtpSendPartial, false);
    }

    /**
     * timeout value in seconds
     *
     * @return zimbraSmtpTimeout, or -1 if unset
     */
    @ZAttr(id=99)
    public int getSmtpTimeout() {
        return getIntAttr(Provisioning.A_zimbraSmtpTimeout, -1);
    }

    /**
     * If TRUE, enables support for GetVersionInfo for account SOAP requests.
     * If FALSE, GetVersionInfoRequest returns a SOAP fault.
     *
     * @return zimbraSoapExposeVersion, or false if unset
     */
    @ZAttr(id=708)
    public boolean isSoapExposeVersion() {
        return getBooleanAttr(Provisioning.A_zimbraSoapExposeVersion, false);
    }

    /**
     * Maximum size in bytes for incoming SOAP requests. 0 means no limit.
     *
     * @return zimbraSoapRequestMaxSize, or -1 if unset
     */
    @ZAttr(id=557)
    public int getSoapRequestMaxSize() {
        return getIntAttr(Provisioning.A_zimbraSoapRequestMaxSize, -1);
    }

    /**
     * URL of the server running the spell checking service. Multi-valued
     * attribute that allows multiple spell check servers to be specified. If
     * the request to the first server fails, a request to the second server
     * is sent and so on.
     *
     * @return zimbraSpellCheckURL, or ampty array if unset
     */
    @ZAttr(id=267)
    public String[] getSpellCheckURL() {
        return getMultiAttr(Provisioning.A_zimbraSpellCheckURL);
    }

    /**
     * Public key of this server, used by other hosts to authorize this
     * server to login.
     *
     * @return zimbraSshPublicKey, or null unset
     */
    @ZAttr(id=262)
    public String getSshPublicKey() {
        return getAttr(Provisioning.A_zimbraSshPublicKey);
    }

    /**
     * Deprecated since: 4.5.7. We now maintain all tables unconditionally.
     * See bug 19145. Orig desc: table maintenance will be performed if the
     * number of rows grows by this factor
     *
     * @return zimbraTableMaintenanceGrowthFactor, or -1 if unset
     */
    @ZAttr(id=171)
    public int getTableMaintenanceGrowthFactor() {
        return getIntAttr(Provisioning.A_zimbraTableMaintenanceGrowthFactor, -1);
    }

    /**
     * Deprecated since: 4.5.7. We now maintain all tables unconditionally.
     * See bug 19145. Orig desc: maximum number of rows required for database
     * table maintenance
     *
     * @return zimbraTableMaintenanceMaxRows, or -1 if unset
     */
    @ZAttr(id=169)
    public int getTableMaintenanceMaxRows() {
        return getIntAttr(Provisioning.A_zimbraTableMaintenanceMaxRows, -1);
    }

    /**
     * Deprecated since: 4.5.7. We now maintain all tables unconditionally.
     * See bug 19145. Orig desc: minimum number of rows required for database
     * table maintenance
     *
     * @return zimbraTableMaintenanceMinRows, or -1 if unset
     */
    @ZAttr(id=168)
    public int getTableMaintenanceMinRows() {
        return getIntAttr(Provisioning.A_zimbraTableMaintenanceMinRows, -1);
    }

    /**
     * Deprecated since: 4.5.7. We now maintain all tables unconditionally.
     * See bug 19145. Orig desc: table maintenance operation that will be
     * performed. Valid options: &quot;ANALYZE&quot;, &quot;OPTIMIZE&quot;
     *
     * <p>Valid values: [OPTIMIZE, ANALYZE]
     *
     * @return zimbraTableMaintenanceOperation, or ampty array if unset
     */
    @ZAttr(id=170)
    public String[] getTableMaintenanceOperation() {
        return getMultiAttr(Provisioning.A_zimbraTableMaintenanceOperation);
    }

    /**
     * whether end-user services on SOAP and LMTP interfaces are enabled
     *
     * @return zimbraUserServicesEnabled, or false if unset
     */
    @ZAttr(id=146)
    public boolean isUserServicesEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraUserServicesEnabled, false);
    }

    /**
     * how often (nnnnn[hmsd]) the virus definitions are updated
     *
     * <p>Use getVirusDefinitionsUpdateFrequencyAsString to access value as a string.
     *
     * @see #getVirusDefinitionsUpdateFrequencyAsString()
     *
     * @return zimbraVirusDefinitionsUpdateFrequency in millseconds, or -1 if unset
     */
    @ZAttr(id=191)
    public long getVirusDefinitionsUpdateFrequency() {
        return getTimeInterval(Provisioning.A_zimbraVirusDefinitionsUpdateFrequency, -1);
    }

    /**
     * how often (nnnnn[hmsd]) the virus definitions are updated
     *
     * @return zimbraVirusDefinitionsUpdateFrequency, or null unset
     */
    @ZAttr(id=191)
    public String getVirusDefinitionsUpdateFrequencyAsString() {
        return getAttr(Provisioning.A_zimbraVirusDefinitionsUpdateFrequency);
    }

    /**
     * Enable XMPP support for IM
     *
     * @return zimbraXMPPEnabled, or false if unset
     */
    @ZAttr(id=397)
    public boolean isXMPPEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraXMPPEnabled, false);
    }

    ///// END-AUTO-GEN-REPLACE

}
