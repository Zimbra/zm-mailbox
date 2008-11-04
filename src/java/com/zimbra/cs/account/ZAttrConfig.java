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
public class ZAttrConfig extends Entry {

    public ZAttrConfig(Map<String, Object> attrs) {
        super(attrs, null);
    }

    ///// BEGIN-AUTO-GEN-REPLACE

    /* build: 5.0 schemers 20081104-1137 */

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
     * additional account attrs that get returned to a client
     *
     * @return zimbraAccountClientAttr, or ampty array if unset
     */
    @ZAttr(id=112)
    public String[] getAccountClientAttr() {
        return getMultiAttr(Provisioning.A_zimbraAccountClientAttr);
    }

    /**
     * Object classes to add when creating a zimbra account object. Useful if
     * you want to add sambaSamAccount etc to zimbra accounts.
     *
     * @return zimbraAccountExtraObjectClass, or ampty array if unset
     */
    @ZAttr(id=438)
    public String[] getAccountExtraObjectClass() {
        return getMultiAttr(Provisioning.A_zimbraAccountExtraObjectClass);
    }

    /**
     * whether to show catchall addresses in admin console
     *
     * @return zimbraAdminConsoleCatchAllAddressEnabled, or false if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=746)
    public boolean isAdminConsoleCatchAllAddressEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraAdminConsoleCatchAllAddressEnabled, false);
    }

    /**
     * enable MX check feature for domain
     *
     * @return zimbraAdminConsoleDNSCheckEnabled, or false if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=743)
    public boolean isAdminConsoleDNSCheckEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraAdminConsoleDNSCheckEnabled, false);
    }

    /**
     * logout URL for admin console to send the user to upon explicit loggin
     * out
     *
     * @return zimbraAdminConsoleLoginURL, or null unset
     */
    @ZAttr(id=696)
    public String getAdminConsoleLoginURL() {
        return getAttr(Provisioning.A_zimbraAdminConsoleLoginURL);
    }

    /**
     * logout URL for admin console to send the user to upon explicit loggin
     * out
     *
     * @return zimbraAdminConsoleLogoutURL, or null unset
     */
    @ZAttr(id=684)
    public String getAdminConsoleLogoutURL() {
        return getAttr(Provisioning.A_zimbraAdminConsoleLogoutURL);
    }

    /**
     * whether to allow skin management in admin console
     *
     * @return zimbraAdminConsoleSkinEnabled, or false if unset
     *
     * @since ZCS 5.0.11
     */
    @ZAttr(id=751)
    public boolean isAdminConsoleSkinEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraAdminConsoleSkinEnabled, false);
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
     * Address to which archive message bounces should be sent. Typically
     * could be an admin account. This is global across all domains.
     *
     * @return zimbraArchiveMailFrom, or null unset
     */
    @ZAttr(id=430)
    public String getArchiveMailFrom() {
        return getAttr(Provisioning.A_zimbraArchiveMailFrom);
    }

    /**
     * block all attachment downloading
     *
     * @return zimbraAttachmentsBlocked, or false if unset
     */
    @ZAttr(id=115)
    public boolean isAttachmentsBlocked() {
        return getBooleanAttr(Provisioning.A_zimbraAttachmentsBlocked, false);
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
     * Class to use to scan attachments during compose
     *
     * @return zimbraAttachmentsScanClass, or null unset
     */
    @ZAttr(id=238)
    public String getAttachmentsScanClass() {
        return getAttr(Provisioning.A_zimbraAttachmentsScanClass);
    }

    /**
     * Whether to scan attachments during compose
     *
     * @return zimbraAttachmentsScanEnabled, or false if unset
     */
    @ZAttr(id=237)
    public boolean isAttachmentsScanEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraAttachmentsScanEnabled, false);
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
     * view all attachments in html only
     *
     * @return zimbraAttachmentsViewInHtmlOnly, or false if unset
     */
    @ZAttr(id=116)
    public boolean isAttachmentsViewInHtmlOnly() {
        return getBooleanAttr(Provisioning.A_zimbraAttachmentsViewInHtmlOnly, false);
    }

    /**
     * auth token secret key
     *
     * @return zimbraAuthTokenKey, or ampty array if unset
     */
    @ZAttr(id=100)
    public String[] getAuthTokenKey() {
        return getMultiAttr(Provisioning.A_zimbraAuthTokenKey);
    }

    /**
     * Use null return path for envelope MAIL FROM when sending out of office
     * and new mail notifications. If false, use account address for envelope
     *
     * @return zimbraAutoSubmittedNullReturnPath, or false if unset
     */
    @ZAttr(id=502)
    public boolean isAutoSubmittedNullReturnPath() {
        return getBooleanAttr(Provisioning.A_zimbraAutoSubmittedNullReturnPath, false);
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
     * zimbraCOS attrs that get inherited in a zimbraAccount
     *
     * @return zimbraCOSInheritedAttr, or ampty array if unset
     */
    @ZAttr(id=21)
    public String[] getCOSInheritedAttr() {
        return getMultiAttr(Provisioning.A_zimbraCOSInheritedAttr);
    }

    /**
     * alternate location for calendar and task folders
     *
     * @return zimbraCalendarCalDavAlternateCalendarHomeSet, or ampty array if unset
     */
    @ZAttr(id=651)
    public String[] getCalendarCalDavAlternateCalendarHomeSet() {
        return getMultiAttr(Provisioning.A_zimbraCalendarCalDavAlternateCalendarHomeSet);
    }

    /**
     * set true to turn off handling free/busy lookup for CalDAV
     *
     * @return zimbraCalendarCalDavDisableFreebusy, or false if unset
     */
    @ZAttr(id=690)
    public boolean isCalendarCalDavDisableFreebusy() {
        return getBooleanAttr(Provisioning.A_zimbraCalendarCalDavDisableFreebusy, false);
    }

    /**
     * set true to turn off handling scheduling message for CalDAV
     *
     * @return zimbraCalendarCalDavDisableScheduling, or false if unset
     */
    @ZAttr(id=652)
    public boolean isCalendarCalDavDisableScheduling() {
        return getBooleanAttr(Provisioning.A_zimbraCalendarCalDavDisableScheduling, false);
    }

    /**
     * compatibility mode for calendar server
     *
     * <p>Valid values: [exchange, standard]
     *
     * @return zimbraCalendarCompatibilityMode, or null unset
     */
    @ZAttr(id=243)
    public String getCalendarCompatibilityMode() {
        return getAttr(Provisioning.A_zimbraCalendarCompatibilityMode);
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
     * Object classes to add when creating a zimbra calendar resource object.
     *
     * @return zimbraCalendarResourceExtraObjectClass, or ampty array if unset
     *
     * @since ZCS future
     */
    @ZAttr(id=753)
    public String[] getCalendarResourceExtraObjectClass() {
        return getMultiAttr(Provisioning.A_zimbraCalendarResourceExtraObjectClass);
    }

    /**
     * When creating self-signed SSL certs during an install, we also create
     * a local Certificate Authority (CA) to sign these SSL certs. This local
     * CA-s own cert is then added to different applications &quot;trusted
     * CA-s&quot; list/store. This attribute should not be used in a system
     * with real certs issued by well known CAs.
     *
     * @return zimbraCertAuthorityCertSelfSigned, or null unset
     */
    @ZAttr(id=280)
    public String getCertAuthorityCertSelfSigned() {
        return getAttr(Provisioning.A_zimbraCertAuthorityCertSelfSigned);
    }

    /**
     * Please see the documentation for the attribute
     * zimbraCertAuthorityCertSelfSigned. In addition, please note that this
     * attribute is provided at install for convenience during a test install
     * without real certs issued by well known CAs. If you choose to create
     * your own CA for your production uses, please note that it is a bad
     * idea to store your CA-s private key in LDAP, as this data maybe read
     * from zimbraGlobalConfig in the clear.
     *
     * @return zimbraCertAuthorityKeySelfSigned, or null unset
     */
    @ZAttr(id=279)
    public String getCertAuthorityKeySelfSigned() {
        return getAttr(Provisioning.A_zimbraCertAuthorityKeySelfSigned);
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
     * Names of additonal components that have been installed
     *
     * @return zimbraComponentAvailable, or ampty array if unset
     */
    @ZAttr(id=242)
    public String[] getComponentAvailable() {
        return getMultiAttr(Provisioning.A_zimbraComponentAvailable);
    }

    /**
     * Object classes to add when creating a zimbra cos object.
     *
     * @return zimbraCosExtraObjectClass, or ampty array if unset
     *
     * @since ZCS future
     */
    @ZAttr(id=754)
    public String[] getCosExtraObjectClass() {
        return getMultiAttr(Provisioning.A_zimbraCosExtraObjectClass);
    }

    /**
     * This attribute is used for DNS check by customers that configure their
     * MX to point at spam relays or other non-zimbra inbox smtp servers
     *
     * @return zimbraDNSCheckHostname, or null unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=744)
    public String getDNSCheckHostname() {
        return getAttr(Provisioning.A_zimbraDNSCheckHostname);
    }

    /**
     * name of the default domain for accounts when authenticating without a
     * domain
     *
     * @return zimbraDefaultDomainName, or null unset
     */
    @ZAttr(id=172)
    public String getDefaultDomainName() {
        return getAttr(Provisioning.A_zimbraDefaultDomainName);
    }

    /**
     * account attributes that a domain administrator is allowed to modify
     *
     * @return zimbraDomainAdminModifiableAttr, or ampty array if unset
     */
    @ZAttr(id=300)
    public String[] getDomainAdminModifiableAttr() {
        return getMultiAttr(Provisioning.A_zimbraDomainAdminModifiableAttr);
    }

    /**
     * Object classes to add when creating a zimbra domain object.
     *
     * @return zimbraDomainExtraObjectClass, or ampty array if unset
     *
     * @since ZCS future
     */
    @ZAttr(id=755)
    public String[] getDomainExtraObjectClass() {
        return getMultiAttr(Provisioning.A_zimbraDomainExtraObjectClass);
    }

    /**
     * zimbraDomain attrs that get inherited from global config
     *
     * @return zimbraDomainInheritedAttr, or ampty array if unset
     */
    @ZAttr(id=63)
    public String[] getDomainInheritedAttr() {
        return getMultiAttr(Provisioning.A_zimbraDomainInheritedAttr);
    }

    /**
     * domain status. enum values are akin to those of zimbraAccountStatus
     * zimbraAccountStatus values: active - active lockout - no login until
     * lockout duration is over locked - no login maintenance - no login, no
     * delivery(try again, no bouncing) closed - no login, no
     * delivery(bouncing mails) zimbraDomainStatus values: all values for
     * zimbraAccountStatus (except for lockout, see mapping below) suspended
     * - maintenance + no creating/deleting/modifying accounts/DLs under the
     * domain. shutdown - suspended + no modifying domain attrs (can only be
     * set internally, cannot be set in admin console or zmprov) How
     * zimbraDomainStatus affects account behavior :
     * ------------------------------------- zimbraDomainStatus account
     * behavior ------------------------------------- active
     * zimbraAccountStatus locked zimbraAccountStatus if it is maintenance or
     * closed, else locked maintenance zimbraAccountStatus if it is closed,
     * else maintenance suspended zimbraAccountStatus if it is closed, else
     * maintenance shutdown zimbraAccountStatus if it is closed, else
     * maintenance closed closed
     *
     * <p>Valid values: [active, closed, locked, suspended, maintenance, shutdown]
     *
     * @return zimbraDomainStatus, or null unset
     */
    @ZAttr(id=535)
    public String getDomainStatus() {
        return getAttr(Provisioning.A_zimbraDomainStatus);
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
     * Exchange user password for free/busy lookup and propagation
     *
     * @return zimbraFreebusyExchangeAuthPassword, or null unset
     */
    @ZAttr(id=609)
    public String getFreebusyExchangeAuthPassword() {
        return getAttr(Provisioning.A_zimbraFreebusyExchangeAuthPassword);
    }

    /**
     * auth scheme to use
     *
     * <p>Valid values: [basic, form]
     *
     * @return zimbraFreebusyExchangeAuthScheme, or null unset
     */
    @ZAttr(id=611)
    public String getFreebusyExchangeAuthScheme() {
        return getAttr(Provisioning.A_zimbraFreebusyExchangeAuthScheme);
    }

    /**
     * Exchange username for free/busy lookup and propagation
     *
     * @return zimbraFreebusyExchangeAuthUsername, or null unset
     */
    @ZAttr(id=608)
    public String getFreebusyExchangeAuthUsername() {
        return getAttr(Provisioning.A_zimbraFreebusyExchangeAuthUsername);
    }

    /**
     * The duration of f/b block pushed to Exchange server.
     *
     * <p>Use getFreebusyExchangeCachedIntervalAsString to access value as a string.
     *
     * @see #getFreebusyExchangeCachedIntervalAsString()
     *
     * @return zimbraFreebusyExchangeCachedInterval in millseconds, or -1 if unset
     */
    @ZAttr(id=621)
    public long getFreebusyExchangeCachedInterval() {
        return getTimeInterval(Provisioning.A_zimbraFreebusyExchangeCachedInterval, -1);
    }

    /**
     * The duration of f/b block pushed to Exchange server.
     *
     * @return zimbraFreebusyExchangeCachedInterval, or null unset
     */
    @ZAttr(id=621)
    public String getFreebusyExchangeCachedIntervalAsString() {
        return getAttr(Provisioning.A_zimbraFreebusyExchangeCachedInterval);
    }

    /**
     * The value of duration is used to indicate the start date (in the past
     * relative to today) of the f/b interval pushed to Exchange server.
     *
     * <p>Use getFreebusyExchangeCachedIntervalStartAsString to access value as a string.
     *
     * @see #getFreebusyExchangeCachedIntervalStartAsString()
     *
     * @return zimbraFreebusyExchangeCachedIntervalStart in millseconds, or -1 if unset
     */
    @ZAttr(id=620)
    public long getFreebusyExchangeCachedIntervalStart() {
        return getTimeInterval(Provisioning.A_zimbraFreebusyExchangeCachedIntervalStart, -1);
    }

    /**
     * The value of duration is used to indicate the start date (in the past
     * relative to today) of the f/b interval pushed to Exchange server.
     *
     * @return zimbraFreebusyExchangeCachedIntervalStart, or null unset
     */
    @ZAttr(id=620)
    public String getFreebusyExchangeCachedIntervalStartAsString() {
        return getAttr(Provisioning.A_zimbraFreebusyExchangeCachedIntervalStart);
    }

    /**
     * URL to Exchange server for free/busy lookup and propagation
     *
     * @return zimbraFreebusyExchangeURL, or null unset
     */
    @ZAttr(id=607)
    public String getFreebusyExchangeURL() {
        return getAttr(Provisioning.A_zimbraFreebusyExchangeURL);
    }

    /**
     * O and OU used in legacyExchangeDN attribute
     *
     * @return zimbraFreebusyExchangeUserOrg, or null unset
     */
    @ZAttr(id=610)
    public String getFreebusyExchangeUserOrg() {
        return getAttr(Provisioning.A_zimbraFreebusyExchangeUserOrg);
    }

    /**
     * LDAP search filter for external GAL auto-complete queries
     *
     * @return zimbraGalAutoCompleteLdapFilter, or null unset
     */
    @ZAttr(id=360)
    public String getGalAutoCompleteLdapFilter() {
        return getAttr(Provisioning.A_zimbraGalAutoCompleteLdapFilter);
    }

    /**
     * LDAP search base for interal GAL queries (special values:
     * &quot;ROOT&quot; for top, &quot;DOMAIN&quot; for domain only,
     * &quot;SUBDOMAINS&quot; for domain and subdomains)
     *
     * @return zimbraGalInternalSearchBase, or null unset
     */
    @ZAttr(id=358)
    public String getGalInternalSearchBase() {
        return getAttr(Provisioning.A_zimbraGalInternalSearchBase);
    }

    /**
     * LDAP Gal attribute to contact attr mapping
     *
     * @return zimbraGalLdapAttrMap, or ampty array if unset
     */
    @ZAttr(id=153)
    public String[] getGalLdapAttrMap() {
        return getMultiAttr(Provisioning.A_zimbraGalLdapAttrMap);
    }

    /**
     * LDAP search filter definitions for GAL queries
     *
     * @return zimbraGalLdapFilterDef, or ampty array if unset
     */
    @ZAttr(id=52)
    public String[] getGalLdapFilterDef() {
        return getMultiAttr(Provisioning.A_zimbraGalLdapFilterDef);
    }

    /**
     * LDAP page size for paged search control while accessing LDAP server
     * for GAL. This apples to both Zimbra and external LDAP servers. A value
     * of 0 means paging is not enabled.
     *
     * @return zimbraGalLdapPageSize, or -1 if unset
     */
    @ZAttr(id=583)
    public int getGalLdapPageSize() {
        return getIntAttr(Provisioning.A_zimbraGalLdapPageSize, -1);
    }

    /**
     * maximum number of gal entries to return from a search
     *
     * @return zimbraGalMaxResults, or -1 if unset
     */
    @ZAttr(id=53)
    public int getGalMaxResults() {
        return getIntAttr(Provisioning.A_zimbraGalMaxResults, -1);
    }

    /**
     * LDAP search base for internal GAL sync (special values:
     * &quot;ROOT&quot; for top, &quot;DOMAIN&quot; for domain only,
     * &quot;SUBDOMAINS&quot; for domain and subdomains) If not set fallback
     * to zimbraGalInternalSearchBase
     *
     * @return zimbraGalSyncInternalSearchBase, or null unset
     */
    @ZAttr(id=598)
    public String getGalSyncInternalSearchBase() {
        return getAttr(Provisioning.A_zimbraGalSyncInternalSearchBase);
    }

    /**
     * LDAP page size for paged search control while accessing LDAP server
     * for GAL sync. This apples to both Zimbra and external LDAP servers. A
     * value of 0 means paging is not enabled. If not set fallback to
     * zimbraGalLdapPageSize
     *
     * @return zimbraGalSyncLdapPageSize, or -1 if unset
     */
    @ZAttr(id=597)
    public int getGalSyncLdapPageSize() {
        return getIntAttr(Provisioning.A_zimbraGalSyncLdapPageSize, -1);
    }

    /**
     * help URL for admin
     *
     * @return zimbraHelpAdminURL, or null unset
     */
    @ZAttr(id=674)
    public String getHelpAdminURL() {
        return getAttr(Provisioning.A_zimbraHelpAdminURL);
    }

    /**
     * help URL for advanced client
     *
     * @return zimbraHelpAdvancedURL, or null unset
     */
    @ZAttr(id=676)
    public String getHelpAdvancedURL() {
        return getAttr(Provisioning.A_zimbraHelpAdvancedURL);
    }

    /**
     * help URL for delegated admin
     *
     * @return zimbraHelpDelegatedURL, or null unset
     */
    @ZAttr(id=675)
    public String getHelpDelegatedURL() {
        return getAttr(Provisioning.A_zimbraHelpDelegatedURL);
    }

    /**
     * help URL for standard client
     *
     * @return zimbraHelpStandardURL, or null unset
     */
    @ZAttr(id=677)
    public String getHelpStandardURL() {
        return getAttr(Provisioning.A_zimbraHelpStandardURL);
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
    @ZAttr(id=20)
    public long getHsmAge() {
        return getTimeInterval(Provisioning.A_zimbraHsmAge, -1);
    }

    /**
     * Minimum age of mail items whose filesystem data will be moved to
     * secondary storage (nnnnn[hmsd]).
     *
     * @return zimbraHsmAge, or null unset
     */
    @ZAttr(id=20)
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
     * Deprecated since: 5.0. Installed skin list is a per server property,
     * the list is now generated by directory scan of skin files. Orig desc:
     * Skins installed and available on all servers (this is global config
     * only)
     *
     * @return zimbraInstalledSkin, or ampty array if unset
     */
    @ZAttr(id=368)
    public String[] getInstalledSkin() {
        return getMultiAttr(Provisioning.A_zimbraInstalledSkin);
    }

    /**
     * how often (nnnnn[hmsd]) the zimbraLastLogonTimestamp is updated. if
     * set to 0, updating zimbraLastLogonTimestamp is completely disabled
     *
     * <p>Use getLastLogonTimestampFrequencyAsString to access value as a string.
     *
     * @see #getLastLogonTimestampFrequencyAsString()
     *
     * @return zimbraLastLogonTimestampFrequency in millseconds, or -1 if unset
     */
    @ZAttr(id=114)
    public long getLastLogonTimestampFrequency() {
        return getTimeInterval(Provisioning.A_zimbraLastLogonTimestampFrequency, -1);
    }

    /**
     * how often (nnnnn[hmsd]) the zimbraLastLogonTimestamp is updated. if
     * set to 0, updating zimbraLastLogonTimestamp is completely disabled
     *
     * @return zimbraLastLogonTimestampFrequency, or null unset
     */
    @ZAttr(id=114)
    public String getLastLogonTimestampFrequencyAsString() {
        return getAttr(Provisioning.A_zimbraLastLogonTimestampFrequency);
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
     * destination for syslog messages
     *
     * @return zimbraLogHostname, or ampty array if unset
     */
    @ZAttr(id=250)
    public String[] getLogHostname() {
        return getMultiAttr(Provisioning.A_zimbraLogHostname);
    }

    /**
     * lifetime (nnnnn[hmsd]) of raw log rows in consolidated logger tables
     *
     * <p>Use getLogRawLifetimeAsString to access value as a string.
     *
     * @see #getLogRawLifetimeAsString()
     *
     * @return zimbraLogRawLifetime in millseconds, or -1 if unset
     */
    @ZAttr(id=263)
    public long getLogRawLifetime() {
        return getTimeInterval(Provisioning.A_zimbraLogRawLifetime, -1);
    }

    /**
     * lifetime (nnnnn[hmsd]) of raw log rows in consolidated logger tables
     *
     * @return zimbraLogRawLifetime, or null unset
     */
    @ZAttr(id=263)
    public String getLogRawLifetimeAsString() {
        return getAttr(Provisioning.A_zimbraLogRawLifetime);
    }

    /**
     * lifetime (nnnnn[hmsd]) of summarized log rows in consolidated logger
     * tables
     *
     * <p>Use getLogSummaryLifetimeAsString to access value as a string.
     *
     * @see #getLogSummaryLifetimeAsString()
     *
     * @return zimbraLogSummaryLifetime in millseconds, or -1 if unset
     */
    @ZAttr(id=264)
    public long getLogSummaryLifetime() {
        return getTimeInterval(Provisioning.A_zimbraLogSummaryLifetime, -1);
    }

    /**
     * lifetime (nnnnn[hmsd]) of summarized log rows in consolidated logger
     * tables
     *
     * @return zimbraLogSummaryLifetime, or null unset
     */
    @ZAttr(id=264)
    public String getLogSummaryLifetimeAsString() {
        return getAttr(Provisioning.A_zimbraLogSummaryLifetime);
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
     * Size of cache for delivery time dedupe based on Message-Id header. Set
     * to 0 to disable this type of deduping.
     *
     * @return zimbraMessageIdDedupeCacheSize, or -1 if unset
     */
    @ZAttr(id=334)
    public int getMessageIdDedupeCacheSize() {
        return getIntAttr(Provisioning.A_zimbraMessageIdDedupeCacheSize, -1);
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
     * Attachment file extensions that are blocked
     *
     * @return zimbraMtaBlockedExtension, or ampty array if unset
     */
    @ZAttr(id=195)
    public String[] getMtaBlockedExtension() {
        return getMultiAttr(Provisioning.A_zimbraMtaBlockedExtension);
    }

    /**
     * Commonly blocked attachment file extensions
     *
     * @return zimbraMtaCommonBlockedExtension, or ampty array if unset
     */
    @ZAttr(id=196)
    public String[] getMtaCommonBlockedExtension() {
        return getMultiAttr(Provisioning.A_zimbraMtaCommonBlockedExtension);
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
     * Value for postconf message_size_limit
     *
     * @return zimbraMtaMaxMessageSize, or -1 if unset
     */
    @ZAttr(id=198)
    public int getMtaMaxMessageSize() {
        return getIntAttr(Provisioning.A_zimbraMtaMaxMessageSize, -1);
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
     * Value for postconf recipient_delimiter. Also used by ZCS LMTP server
     * to check if it should accept messages to addresses with extensions.
     *
     * @return zimbraMtaRecipientDelimiter, or ampty array if unset
     */
    @ZAttr(id=306)
    public String[] getMtaRecipientDelimiter() {
        return getMultiAttr(Provisioning.A_zimbraMtaRecipientDelimiter);
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
     * restrictions to reject some suspect SMTP clients
     *
     * @return zimbraMtaRestriction, or ampty array if unset
     */
    @ZAttr(id=226)
    public String[] getMtaRestriction() {
        return getMultiAttr(Provisioning.A_zimbraMtaRestriction);
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
     * A signed activation key that authorizes this installation.
     *
     * @return zimbraNetworkActivation, or null unset
     */
    @ZAttr(id=375)
    public String getNetworkActivation() {
        return getAttr(Provisioning.A_zimbraNetworkActivation);
    }

    /**
     * Contents of a signed Zimbra license key - an XML string.
     *
     * @return zimbraNetworkLicense, or null unset
     */
    @ZAttr(id=374)
    public String getNetworkLicense() {
        return getAttr(Provisioning.A_zimbraNetworkLicense);
    }

    /**
     * Account for storing templates and providing space for public wiki
     *
     * @return zimbraNotebookAccount, or null unset
     */
    @ZAttr(id=363)
    public String getNotebookAccount() {
        return getAttr(Provisioning.A_zimbraNotebookAccount);
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
     * registered change password listener name
     *
     * @return zimbraPasswordChangeListener, or null unset
     */
    @ZAttr(id=586)
    public String getPasswordChangeListener() {
        return getAttr(Provisioning.A_zimbraPasswordChangeListener);
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
     * Name to be used in public API such as REST or SOAP proxy.
     *
     * @return zimbraPublicServiceHostname, or null unset
     */
    @ZAttr(id=377)
    public String getPublicServiceHostname() {
        return getAttr(Provisioning.A_zimbraPublicServiceHostname);
    }

    /**
     * Port to be used in public API such as REST or SOAP proxy.
     *
     * @return zimbraPublicServicePort, or null unset
     */
    @ZAttr(id=699)
    public String getPublicServicePort() {
        return getAttr(Provisioning.A_zimbraPublicServicePort);
    }

    /**
     * Protocol to be used in public API such as REST or SOAP proxy.
     *
     * @return zimbraPublicServiceProtocol, or null unset
     */
    @ZAttr(id=698)
    public String getPublicServiceProtocol() {
        return getAttr(Provisioning.A_zimbraPublicServiceProtocol);
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
     * Allowed reverse proxy IP addresses. Lookup servlet will only generate
     * authtokens if request was made from one of these IP addresses
     *
     * @return zimbraReverseProxyAdminIPAddress, or ampty array if unset
     */
    @ZAttr(id=697)
    public String[] getReverseProxyAdminIPAddress() {
        return getMultiAttr(Provisioning.A_zimbraReverseProxyAdminIPAddress);
    }

    /**
     * the attribute that identifies the zimbra admin bind port
     *
     * @return zimbraReverseProxyAdminPortAttribute, or null unset
     */
    @ZAttr(id=700)
    public String getReverseProxyAdminPortAttribute() {
        return getAttr(Provisioning.A_zimbraReverseProxyAdminPortAttribute);
    }

    /**
     * wait duration before nginx sending back the NO response for failed
     * imap/pop3 reverse proxy lookups
     *
     * <p>Use getReverseProxyAuthWaitIntervalAsString to access value as a string.
     *
     * @see #getReverseProxyAuthWaitIntervalAsString()
     *
     * @return zimbraReverseProxyAuthWaitInterval in millseconds, or -1 if unset
     */
    @ZAttr(id=569)
    public long getReverseProxyAuthWaitInterval() {
        return getTimeInterval(Provisioning.A_zimbraReverseProxyAuthWaitInterval, -1);
    }

    /**
     * wait duration before nginx sending back the NO response for failed
     * imap/pop3 reverse proxy lookups
     *
     * @return zimbraReverseProxyAuthWaitInterval, or null unset
     */
    @ZAttr(id=569)
    public String getReverseProxyAuthWaitIntervalAsString() {
        return getAttr(Provisioning.A_zimbraReverseProxyAuthWaitInterval);
    }

    /**
     * time interval that an entry cached by NGINX will remain in the cache
     *
     * <p>Use getReverseProxyCacheEntryTTLAsString to access value as a string.
     *
     * @see #getReverseProxyCacheEntryTTLAsString()
     *
     * @return zimbraReverseProxyCacheEntryTTL in millseconds, or -1 if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=732)
    public long getReverseProxyCacheEntryTTL() {
        return getTimeInterval(Provisioning.A_zimbraReverseProxyCacheEntryTTL, -1);
    }

    /**
     * time interval that an entry cached by NGINX will remain in the cache
     *
     * @return zimbraReverseProxyCacheEntryTTL, or null unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=732)
    public String getReverseProxyCacheEntryTTLAsString() {
        return getAttr(Provisioning.A_zimbraReverseProxyCacheEntryTTL);
    }

    /**
     * time interval that NGINX proxy will wait for a cache result, before
     * considering the result as a cache miss
     *
     * <p>Use getReverseProxyCacheFetchTimeoutAsString to access value as a string.
     *
     * @see #getReverseProxyCacheFetchTimeoutAsString()
     *
     * @return zimbraReverseProxyCacheFetchTimeout in millseconds, or -1 if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=731)
    public long getReverseProxyCacheFetchTimeout() {
        return getTimeInterval(Provisioning.A_zimbraReverseProxyCacheFetchTimeout, -1);
    }

    /**
     * time interval that NGINX proxy will wait for a cache result, before
     * considering the result as a cache miss
     *
     * @return zimbraReverseProxyCacheFetchTimeout, or null unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=731)
    public String getReverseProxyCacheFetchTimeoutAsString() {
        return getAttr(Provisioning.A_zimbraReverseProxyCacheFetchTimeout);
    }

    /**
     * time interval that NGINX proxy will wait before attempting to
     * re-establish a connection to a memcache server that disconnected
     *
     * <p>Use getReverseProxyCacheReconnectIntervalAsString to access value as a string.
     *
     * @see #getReverseProxyCacheReconnectIntervalAsString()
     *
     * @return zimbraReverseProxyCacheReconnectInterval in millseconds, or -1 if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=730)
    public long getReverseProxyCacheReconnectInterval() {
        return getTimeInterval(Provisioning.A_zimbraReverseProxyCacheReconnectInterval, -1);
    }

    /**
     * time interval that NGINX proxy will wait before attempting to
     * re-establish a connection to a memcache server that disconnected
     *
     * @return zimbraReverseProxyCacheReconnectInterval, or null unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=730)
    public String getReverseProxyCacheReconnectIntervalAsString() {
        return getAttr(Provisioning.A_zimbraReverseProxyCacheReconnectInterval);
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
     * LDAP attribute that contains domain name for the domain
     *
     * @return zimbraReverseProxyDomainNameAttribute, or null unset
     */
    @ZAttr(id=547)
    public String getReverseProxyDomainNameAttribute() {
        return getAttr(Provisioning.A_zimbraReverseProxyDomainNameAttribute);
    }

    /**
     * LDAP query to find a domain
     *
     * @return zimbraReverseProxyDomainNameQuery, or null unset
     */
    @ZAttr(id=545)
    public String getReverseProxyDomainNameQuery() {
        return getAttr(Provisioning.A_zimbraReverseProxyDomainNameQuery);
    }

    /**
     * search base for zimbraReverseProxyDomainNameQuery
     *
     * @return zimbraReverseProxyDomainNameSearchBase, or null unset
     */
    @ZAttr(id=546)
    public String getReverseProxyDomainNameSearchBase() {
        return getAttr(Provisioning.A_zimbraReverseProxyDomainNameSearchBase);
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
     * attribute that contains http bind port
     *
     * @return zimbraReverseProxyHttpPortAttribute, or null unset
     */
    @ZAttr(id=632)
    public String getReverseProxyHttpPortAttribute() {
        return getAttr(Provisioning.A_zimbraReverseProxyHttpPortAttribute);
    }

    /**
     * Sets the upper limit on logins from a remote IP via POP or IMAP to
     * this proxy server after which login is rejected with an appropriate
     * protocol specific bye response. This counter is cumulative for all
     * users that appear to the proxy to be logging in from the same IP
     * address. If multiple users appear to the proxy to be logging in from
     * the same IP address (usual with NATing), then each of the different
     * users login will contribute to increasing the hit counter for that IP
     * address, and when the counter eventually exceeds the limit, then the
     * connections from that IP address will be throttled. Therefore, all
     * users from the same IP will contribute to (and be affected by) this
     * counter. Logins using all protocols (POP3/POP3S/IMAP/IMAPS) will
     * affect this counter (the counter is aggregate for all protocols, *not*
     * separate). If this value is set to 0, then no limiting will take place
     * for any IP.
     *
     * @return zimbraReverseProxyIPLoginLimit, or -1 if unset
     */
    @ZAttr(id=622)
    public int getReverseProxyIPLoginLimit() {
        return getIntAttr(Provisioning.A_zimbraReverseProxyIPLoginLimit, -1);
    }

    /**
     * Sets the time-to-live for the hit counter for IP based login
     * throttling. If time is set to 3600 and limit is set to 1000, then it
     * means that NGINX should not allow more than 1000 users to log in via
     * the proxy from the same IP, within the time interval of an hour. The
     * semantics for such a configuration would then be: allow maximum 1000
     * users per hour from any given IP address.
     *
     * @return zimbraReverseProxyIPLoginLimitTime, or -1 if unset
     */
    @ZAttr(id=623)
    public int getReverseProxyIPLoginLimitTime() {
        return getIntAttr(Provisioning.A_zimbraReverseProxyIPLoginLimitTime, -1);
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
     * attribute that contains imap bind port
     *
     * @return zimbraReverseProxyImapPortAttribute, or null unset
     */
    @ZAttr(id=479)
    public String getReverseProxyImapPortAttribute() {
        return getAttr(Provisioning.A_zimbraReverseProxyImapPortAttribute);
    }

    /**
     * attribute that contains imap bind port for SSL
     *
     * @return zimbraReverseProxyImapSSLPortAttribute, or null unset
     */
    @ZAttr(id=480)
    public String getReverseProxyImapSSLPortAttribute() {
        return getAttr(Provisioning.A_zimbraReverseProxyImapSSLPortAttribute);
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
     * The error message with which a connection attempt from an IP address
     * will be throttled, if the connection count exceeds the configured
     * limit
     *
     * @return zimbraReverseProxyIpThrottleMsg, or null unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=727)
    public String getReverseProxyIpThrottleMsg() {
        return getAttr(Provisioning.A_zimbraReverseProxyIpThrottleMsg);
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
     * LDAP attribute that contains mailhost for the user
     *
     * @return zimbraReverseProxyMailHostAttribute, or null unset
     */
    @ZAttr(id=474)
    public String getReverseProxyMailHostAttribute() {
        return getAttr(Provisioning.A_zimbraReverseProxyMailHostAttribute);
    }

    /**
     * LDAP query to find a user
     *
     * @return zimbraReverseProxyMailHostQuery, or null unset
     */
    @ZAttr(id=472)
    public String getReverseProxyMailHostQuery() {
        return getAttr(Provisioning.A_zimbraReverseProxyMailHostQuery);
    }

    /**
     * search base for zimbraReverseProxyMailHostQuery
     *
     * @return zimbraReverseProxyMailHostSearchBase, or null unset
     */
    @ZAttr(id=473)
    public String getReverseProxyMailHostSearchBase() {
        return getAttr(Provisioning.A_zimbraReverseProxyMailHostSearchBase);
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
     * attribute that contains pop3 bind port
     *
     * @return zimbraReverseProxyPop3PortAttribute, or null unset
     */
    @ZAttr(id=477)
    public String getReverseProxyPop3PortAttribute() {
        return getAttr(Provisioning.A_zimbraReverseProxyPop3PortAttribute);
    }

    /**
     * attribute that contains pop3 bind port for SSL
     *
     * @return zimbraReverseProxyPop3SSLPortAttribute, or null unset
     */
    @ZAttr(id=478)
    public String getReverseProxyPop3SSLPortAttribute() {
        return getAttr(Provisioning.A_zimbraReverseProxyPop3SSLPortAttribute);
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
     * LDAP query to find server object
     *
     * @return zimbraReverseProxyPortQuery, or null unset
     */
    @ZAttr(id=475)
    public String getReverseProxyPortQuery() {
        return getAttr(Provisioning.A_zimbraReverseProxyPortQuery);
    }

    /**
     * search base for zimbraReverseProxyPortQuery
     *
     * @return zimbraReverseProxyPortSearchBase, or null unset
     */
    @ZAttr(id=476)
    public String getReverseProxyPortSearchBase() {
        return getAttr(Provisioning.A_zimbraReverseProxyPortSearchBase);
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
     * permitted ciphers for reverse proxy. Ciphers are in the formats
     * supported by OpenSSL e.g.
     * ALL:!ADH:!EXPORT56:RC4+RSA:+HIGH:+MEDIUM:+LOW:+SSLv2:+EXP; if not set,
     * default ciphers permitted by nginx will apply
     *
     * @return zimbraReverseProxySSLCiphers, or null unset
     */
    @ZAttr(id=640)
    public String getReverseProxySSLCiphers() {
        return getAttr(Provisioning.A_zimbraReverseProxySSLCiphers);
    }

    /**
     * whether nginx should send ID command for imap
     *
     * @return zimbraReverseProxySendImapId, or false if unset
     */
    @ZAttr(id=588)
    public boolean isReverseProxySendImapId() {
        return getBooleanAttr(Provisioning.A_zimbraReverseProxySendImapId, false);
    }

    /**
     * whether nginx should send XOIP command for pop3
     *
     * @return zimbraReverseProxySendPop3Xoip, or false if unset
     */
    @ZAttr(id=587)
    public boolean isReverseProxySendPop3Xoip() {
        return getBooleanAttr(Provisioning.A_zimbraReverseProxySendPop3Xoip, false);
    }

    /**
     * Limit how many times a user can login via the proxy. Setting limit to
     * 100 and time to 3600 means: allow maximum 100 logins per hour for any
     * user. As with the ip counterparts, the user hit counter and timeout
     * are cumulative for all protocols. Also, for a given users login, both
     * counters are checked in succession, with the IP counter being checked
     * first. A login may be rejected (throttled) because the IP is
     * over-usage, or because the login name itself is over-usage. A value of
     * 0 indicates that no throttling will take place for any user.
     *
     * @return zimbraReverseProxyUserLoginLimit, or -1 if unset
     */
    @ZAttr(id=624)
    public int getReverseProxyUserLoginLimit() {
        return getIntAttr(Provisioning.A_zimbraReverseProxyUserLoginLimit, -1);
    }

    /**
     * Sets the time-to-live for the hit counter for per user login
     * throttling.
     *
     * @return zimbraReverseProxyUserLoginLimitTime, or -1 if unset
     */
    @ZAttr(id=625)
    public int getReverseProxyUserLoginLimitTime() {
        return getIntAttr(Provisioning.A_zimbraReverseProxyUserLoginLimitTime, -1);
    }

    /**
     * LDAP attribute that contains user name for the principal
     *
     * @return zimbraReverseProxyUserNameAttribute, or null unset
     */
    @ZAttr(id=572)
    public String getReverseProxyUserNameAttribute() {
        return getAttr(Provisioning.A_zimbraReverseProxyUserNameAttribute);
    }

    /**
     * The error message with which a login attempt by a user will be
     * throttled, if the attempt count exceeds the configured limit
     *
     * @return zimbraReverseProxyUserThrottleMsg, or null unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=726)
    public String getReverseProxyUserThrottleMsg() {
        return getAttr(Provisioning.A_zimbraReverseProxyUserThrottleMsg);
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
     * space separated list of excluded cipher suites
     *
     * @return zimbraSSLExcludeCipherSuites, or ampty array if unset
     */
    @ZAttr(id=639)
    public String[] getSSLExcludeCipherSuites() {
        return getMultiAttr(Provisioning.A_zimbraSSLExcludeCipherSuites);
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
     * Object classes to add when creating a zimbra server object.
     *
     * @return zimbraServerExtraObjectClass, or ampty array if unset
     *
     * @since ZCS future
     */
    @ZAttr(id=756)
    public String[] getServerExtraObjectClass() {
        return getMultiAttr(Provisioning.A_zimbraServerExtraObjectClass);
    }

    /**
     * zimbraServer attrs that get inherited from global config
     *
     * @return zimbraServerInheritedAttr, or ampty array if unset
     */
    @ZAttr(id=62)
    public String[] getServerInheritedAttr() {
        return getMultiAttr(Provisioning.A_zimbraServerInheritedAttr);
    }

    /**
     * background color for chameleon skin for the domain
     *
     * @return zimbraSkinBackgroundColor, or null unset
     */
    @ZAttr(id=648)
    public String getSkinBackgroundColor() {
        return getAttr(Provisioning.A_zimbraSkinBackgroundColor);
    }

    /**
     * foreground color for chameleon skin for the domain
     *
     * @return zimbraSkinForegroundColor, or null unset
     */
    @ZAttr(id=647)
    public String getSkinForegroundColor() {
        return getAttr(Provisioning.A_zimbraSkinForegroundColor);
    }

    /**
     * logo app banner for chameleon skin for the domain
     *
     * @return zimbraSkinLogoAppBanner, or null unset
     */
    @ZAttr(id=671)
    public String getSkinLogoAppBanner() {
        return getAttr(Provisioning.A_zimbraSkinLogoAppBanner);
    }

    /**
     * logo login banner for chameleon skin for the domain
     *
     * @return zimbraSkinLogoLoginBanner, or null unset
     */
    @ZAttr(id=670)
    public String getSkinLogoLoginBanner() {
        return getAttr(Provisioning.A_zimbraSkinLogoLoginBanner);
    }

    /**
     * Logo URL for chameleon skin for the domain
     *
     * @return zimbraSkinLogoURL, or null unset
     */
    @ZAttr(id=649)
    public String getSkinLogoURL() {
        return getAttr(Provisioning.A_zimbraSkinLogoURL);
    }

    /**
     * secondary color for chameleon skin for the domain
     *
     * @return zimbraSkinSecondaryColor, or null unset
     */
    @ZAttr(id=668)
    public String getSkinSecondaryColor() {
        return getAttr(Provisioning.A_zimbraSkinSecondaryColor);
    }

    /**
     * selection color for chameleon skin for the domain
     *
     * @return zimbraSkinSelectionColor, or null unset
     */
    @ZAttr(id=669)
    public String getSkinSelectionColor() {
        return getAttr(Provisioning.A_zimbraSkinSelectionColor);
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
     * If true, an X-Authenticated-User header will be added to messages sent
     * via SendMsgRequest.
     *
     * @return zimbraSmtpSendAddAuthenticatedUser, or false if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=747)
    public boolean isSmtpSendAddAuthenticatedUser() {
        return getBooleanAttr(Provisioning.A_zimbraSmtpSendAddAuthenticatedUser, false);
    }

    /**
     * Whether X-Mailer will be added to messages sent by Zimbra
     *
     * @return zimbraSmtpSendAddMailer, or false if unset
     */
    @ZAttr(id=636)
    public boolean isSmtpSendAddMailer() {
        return getBooleanAttr(Provisioning.A_zimbraSmtpSendAddMailer, false);
    }

    /**
     * Whether X-Originating-IP will be added to messages sent via
     * SendMsgRequest.
     *
     * @return zimbraSmtpSendAddOriginatingIP, or false if unset
     */
    @ZAttr(id=435)
    public boolean isSmtpSendAddOriginatingIP() {
        return getBooleanAttr(Provisioning.A_zimbraSmtpSendAddOriginatingIP, false);
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
     * Deprecated since: 4.5. Deprecated in favor of zimbraIsServiceEnabled.
     * Orig desc: Whether to enable spam checking
     *
     * @return zimbraSpamCheckEnabled, or false if unset
     */
    @ZAttr(id=201)
    public boolean isSpamCheckEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraSpamCheckEnabled, false);
    }

    /**
     * mail header name for flagging spam
     *
     * @return zimbraSpamHeader, or null unset
     */
    @ZAttr(id=210)
    public String getSpamHeader() {
        return getAttr(Provisioning.A_zimbraSpamHeader);
    }

    /**
     * regular expression for matching the spam header
     *
     * @return zimbraSpamHeaderValue, or null unset
     */
    @ZAttr(id=211)
    public String getSpamHeaderValue() {
        return getAttr(Provisioning.A_zimbraSpamHeaderValue);
    }

    /**
     * When user classifies a message as not spam forward message via SMTP to
     * this account
     *
     * @return zimbraSpamIsNotSpamAccount, or null unset
     */
    @ZAttr(id=245)
    public String getSpamIsNotSpamAccount() {
        return getAttr(Provisioning.A_zimbraSpamIsNotSpamAccount);
    }

    /**
     * When user classifies a message as spam forward message via SMTP to
     * this account
     *
     * @return zimbraSpamIsSpamAccount, or null unset
     */
    @ZAttr(id=244)
    public String getSpamIsSpamAccount() {
        return getAttr(Provisioning.A_zimbraSpamIsSpamAccount);
    }

    /**
     * Spaminess percentage beyond which a message is dropped
     *
     * @return zimbraSpamKillPercent, or -1 if unset
     */
    @ZAttr(id=202)
    public int getSpamKillPercent() {
        return getIntAttr(Provisioning.A_zimbraSpamKillPercent, -1);
    }

    /**
     * mail header name for sender in spam report
     *
     * @return zimbraSpamReportSenderHeader, or null unset
     */
    @ZAttr(id=465)
    public String getSpamReportSenderHeader() {
        return getAttr(Provisioning.A_zimbraSpamReportSenderHeader);
    }

    /**
     * spam report type value for ham
     *
     * @return zimbraSpamReportTypeHam, or null unset
     */
    @ZAttr(id=468)
    public String getSpamReportTypeHam() {
        return getAttr(Provisioning.A_zimbraSpamReportTypeHam);
    }

    /**
     * mail header name for report type in spam report
     *
     * @return zimbraSpamReportTypeHeader, or null unset
     */
    @ZAttr(id=466)
    public String getSpamReportTypeHeader() {
        return getAttr(Provisioning.A_zimbraSpamReportTypeHeader);
    }

    /**
     * spam report type value for spam
     *
     * @return zimbraSpamReportTypeSpam, or null unset
     */
    @ZAttr(id=467)
    public String getSpamReportTypeSpam() {
        return getAttr(Provisioning.A_zimbraSpamReportTypeSpam);
    }

    /**
     * Subject prefix for spam messages
     *
     * @return zimbraSpamSubjectTag, or null unset
     */
    @ZAttr(id=203)
    public String getSpamSubjectTag() {
        return getAttr(Provisioning.A_zimbraSpamSubjectTag);
    }

    /**
     * Spaminess percentage beyound which a message is marked as spam
     *
     * @return zimbraSpamTagPercent, or -1 if unset
     */
    @ZAttr(id=204)
    public int getSpamTagPercent() {
        return getIntAttr(Provisioning.A_zimbraSpamTagPercent, -1);
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
     * CA Cert used to sign all self signed certs
     *
     * @return zimbraSslCaCert, or null unset
     */
    @ZAttr(id=277)
    public String getSslCaCert() {
        return getAttr(Provisioning.A_zimbraSslCaCert);
    }

    /**
     * CA Key used to sign all self signed certs
     *
     * @return zimbraSslCaKey, or null unset
     */
    @ZAttr(id=278)
    public String getSslCaKey() {
        return getAttr(Provisioning.A_zimbraSslCaKey);
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
     * Whether to block archive files that are password protected or
     * encrypted
     *
     * @return zimbraVirusBlockEncryptedArchive, or false if unset
     */
    @ZAttr(id=205)
    public boolean isVirusBlockEncryptedArchive() {
        return getBooleanAttr(Provisioning.A_zimbraVirusBlockEncryptedArchive, false);
    }

    /**
     * Deprecated since: 4.5. Deprecated in favor of zimbraIsServiceEnabled.
     * Orig desc: Whether to enable virus checking
     *
     * @return zimbraVirusCheckEnabled, or false if unset
     */
    @ZAttr(id=206)
    public boolean isVirusCheckEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraVirusCheckEnabled, false);
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
     * Whether to email admin on virus detection
     *
     * @return zimbraVirusWarnAdmin, or false if unset
     */
    @ZAttr(id=207)
    public boolean isVirusWarnAdmin() {
        return getBooleanAttr(Provisioning.A_zimbraVirusWarnAdmin, false);
    }

    /**
     * Whether to email recipient on virus detection
     *
     * @return zimbraVirusWarnRecipient, or false if unset
     */
    @ZAttr(id=208)
    public boolean isVirusWarnRecipient() {
        return getBooleanAttr(Provisioning.A_zimbraVirusWarnRecipient, false);
    }

    /**
     * link for admin users in the web client
     *
     * @return zimbraWebClientAdminReference, or null unset
     */
    @ZAttr(id=701)
    public String getWebClientAdminReference() {
        return getAttr(Provisioning.A_zimbraWebClientAdminReference);
    }

    /**
     * login URL for web client to send the user to upon failed login, auth
     * expired, or no/invalid auth
     *
     * @return zimbraWebClientLoginURL, or null unset
     */
    @ZAttr(id=506)
    public String getWebClientLoginURL() {
        return getAttr(Provisioning.A_zimbraWebClientLoginURL);
    }

    /**
     * logout URL for web client to send the user to upon explicit loggin out
     *
     * @return zimbraWebClientLogoutURL, or null unset
     */
    @ZAttr(id=507)
    public String getWebClientLogoutURL() {
        return getAttr(Provisioning.A_zimbraWebClientLogoutURL);
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

    /**
     * Shared Secret for XMPP Server Dialback Protocol
     *
     * @return zimbraXMPPServerDialbackKey, or ampty array if unset
     */
    @ZAttr(id=695)
    public String[] getXMPPServerDialbackKey() {
        return getMultiAttr(Provisioning.A_zimbraXMPPServerDialbackKey);
    }

    /**
     * List of Zimlets available to this domain. Zimlets available to
     * accounts in the domain is the union of account/cos attribute
     * zimbraZimletAvailableZimlets and this attribute.
     *
     * @return zimbraZimletDomainAvailableZimlets, or ampty array if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=710)
    public String[] getZimletDomainAvailableZimlets() {
        return getMultiAttr(Provisioning.A_zimbraZimletDomainAvailableZimlets);
    }

    ///// END-AUTO-GEN-REPLACE

}
