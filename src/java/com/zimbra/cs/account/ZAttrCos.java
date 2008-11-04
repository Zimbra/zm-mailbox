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

import java.util.Date;
import java.util.Map;

/**
 * AUTO-GENERATED. DO NOT EDIT.
 *
 */
public class ZAttrCos extends NamedEntry {

    public ZAttrCos(String name, String id, Map<String,Object> attrs) {
        super(name, id, attrs, null);
    }

    ///// BEGIN-AUTO-GEN-REPLACE

    /* build: 5.0 schemers 20081104-1137 */

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
     * lifetime (nnnnn[hmsd]) of newly created admin auth tokens
     *
     * <p>Use getAdminAuthTokenLifetimeAsString to access value as a string.
     *
     * @see #getAdminAuthTokenLifetimeAsString()
     *
     * @return zimbraAdminAuthTokenLifetime in millseconds, or -1 if unset
     */
    @ZAttr(id=109)
    public long getAdminAuthTokenLifetime() {
        return getTimeInterval(Provisioning.A_zimbraAdminAuthTokenLifetime, -1);
    }

    /**
     * lifetime (nnnnn[hmsd]) of newly created admin auth tokens
     *
     * @return zimbraAdminAuthTokenLifetime, or null unset
     */
    @ZAttr(id=109)
    public String getAdminAuthTokenLifetimeAsString() {
        return getAttr(Provisioning.A_zimbraAdminAuthTokenLifetime);
    }

    /**
     * admin saved searches
     *
     * @return zimbraAdminSavedSearches, or ampty array if unset
     */
    @ZAttr(id=446)
    public String[] getAdminSavedSearches() {
        return getMultiAttr(Provisioning.A_zimbraAdminSavedSearches);
    }

    /**
     * Whether this account can use any from address. Not changeable by
     * domain admin to allow arbitrary addresses
     *
     * @return zimbraAllowAnyFromAddress, or false if unset
     */
    @ZAttr(id=427)
    public boolean isAllowAnyFromAddress() {
        return getBooleanAttr(Provisioning.A_zimbraAllowAnyFromAddress, false);
    }

    /**
     * An account or CoS setting that works with the name template that
     * allows you to dictate the date format used in the name template. This
     * is a Java SimpleDateFormat specifier. The default is an LDAP
     * generalized time format:
     *
     * @return zimbraArchiveAccountDateTemplate, or null unset
     */
    @ZAttr(id=432)
    public String getArchiveAccountDateTemplate() {
        return getAttr(Provisioning.A_zimbraArchiveAccountDateTemplate);
    }

    /**
     * An account or CoS setting - typically only in CoS - that tells the
     * archiving system how to derive the archive mailbox name. ID, USER,
     * DATE, and DOMAIN are expanded.
     *
     * @return zimbraArchiveAccountNameTemplate, or null unset
     */
    @ZAttr(id=431)
    public String getArchiveAccountNameTemplate() {
        return getAttr(Provisioning.A_zimbraArchiveAccountNameTemplate);
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
     * whether or not to index attachemts
     *
     * @return zimbraAttachmentsIndexingEnabled, or false if unset
     */
    @ZAttr(id=173)
    public boolean isAttachmentsIndexingEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraAttachmentsIndexingEnabled, false);
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
     * lifetime (nnnnn[hmsd]) of newly created auth tokens
     *
     * <p>Use getAuthTokenLifetimeAsString to access value as a string.
     *
     * @see #getAuthTokenLifetimeAsString()
     *
     * @return zimbraAuthTokenLifetime in millseconds, or -1 if unset
     */
    @ZAttr(id=108)
    public long getAuthTokenLifetime() {
        return getTimeInterval(Provisioning.A_zimbraAuthTokenLifetime, -1);
    }

    /**
     * lifetime (nnnnn[hmsd]) of newly created auth tokens
     *
     * @return zimbraAuthTokenLifetime, or null unset
     */
    @ZAttr(id=108)
    public String getAuthTokenLifetimeAsString() {
        return getAttr(Provisioning.A_zimbraAuthTokenLifetime);
    }

    /**
     * Locales available for this account
     *
     * @return zimbraAvailableLocale, or ampty array if unset
     */
    @ZAttr(id=487)
    public String[] getAvailableLocale() {
        return getMultiAttr(Provisioning.A_zimbraAvailableLocale);
    }

    /**
     * Skins available for this account. Fallback order is: 1. the normal
     * account/cos inheritance 2. if not set on account/cos, use the value on
     * the domain of the account
     *
     * @return zimbraAvailableSkin, or ampty array if unset
     */
    @ZAttr(id=364)
    public String[] getAvailableSkin() {
        return getMultiAttr(Provisioning.A_zimbraAvailableSkin);
    }

    /**
     * Batch size to use when indexing data
     *
     * @return zimbraBatchedIndexingSize, or -1 if unset
     */
    @ZAttr(id=619)
    public int getBatchedIndexingSize() {
        return getIntAttr(Provisioning.A_zimbraBatchedIndexingSize, -1);
    }

    /**
     * maximum number of revisions to keep for calendar items (appointments
     * and tasks). 0 means unlimited.
     *
     * @return zimbraCalendarMaxRevisions, or -1 if unset
     */
    @ZAttr(id=709)
    public int getCalendarMaxRevisions() {
        return getIntAttr(Provisioning.A_zimbraCalendarMaxRevisions, -1);
    }

    /**
     * Maximum number of contacts allowed in mailbox. 0 means no limit.
     *
     * @return zimbraContactMaxNumEntries, or -1 if unset
     */
    @ZAttr(id=107)
    public int getContactMaxNumEntries() {
        return getIntAttr(Provisioning.A_zimbraContactMaxNumEntries, -1);
    }

    /**
     * Maximum number of data sources allowed on an account
     *
     * @return zimbraDataSourceMaxNumEntries, or -1 if unset
     */
    @ZAttr(id=426)
    public int getDataSourceMaxNumEntries() {
        return getIntAttr(Provisioning.A_zimbraDataSourceMaxNumEntries, -1);
    }

    /**
     * Shortest allowed duration for zimbraDataSourcePollingInterval.
     *
     * <p>Use getDataSourceMinPollingIntervalAsString to access value as a string.
     *
     * @see #getDataSourceMinPollingIntervalAsString()
     *
     * @return zimbraDataSourceMinPollingInterval in millseconds, or -1 if unset
     */
    @ZAttr(id=525)
    public long getDataSourceMinPollingInterval() {
        return getTimeInterval(Provisioning.A_zimbraDataSourceMinPollingInterval, -1);
    }

    /**
     * Shortest allowed duration for zimbraDataSourcePollingInterval.
     *
     * @return zimbraDataSourceMinPollingInterval, or null unset
     */
    @ZAttr(id=525)
    public String getDataSourceMinPollingIntervalAsString() {
        return getAttr(Provisioning.A_zimbraDataSourceMinPollingInterval);
    }

    /**
     * The time interval between automated data imports for a data source, or
     * all data sources owned by an account. If unset or 0, the data source
     * will not be scheduled for automated polling.
     *
     * <p>Use getDataSourcePollingIntervalAsString to access value as a string.
     *
     * @see #getDataSourcePollingIntervalAsString()
     *
     * @return zimbraDataSourcePollingInterval in millseconds, or -1 if unset
     */
    @ZAttr(id=455)
    public long getDataSourcePollingInterval() {
        return getTimeInterval(Provisioning.A_zimbraDataSourcePollingInterval, -1);
    }

    /**
     * The time interval between automated data imports for a data source, or
     * all data sources owned by an account. If unset or 0, the data source
     * will not be scheduled for automated polling.
     *
     * @return zimbraDataSourcePollingInterval, or null unset
     */
    @ZAttr(id=455)
    public String getDataSourcePollingIntervalAsString() {
        return getAttr(Provisioning.A_zimbraDataSourcePollingInterval);
    }

    /**
     * maximum amount of mail quota a domain admin can set on a user
     *
     * @return zimbraDomainAdminMaxMailQuota, or -1 if unset
     */
    @ZAttr(id=398)
    public long getDomainAdminMaxMailQuota() {
        return getLongAttr(Provisioning.A_zimbraDomainAdminMaxMailQuota, -1);
    }

    /**
     * advanced search button enabled
     *
     * @return zimbraFeatureAdvancedSearchEnabled, or false if unset
     */
    @ZAttr(id=138)
    public boolean isFeatureAdvancedSearchEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureAdvancedSearchEnabled, false);
    }

    /**
     * whether to allow use of briefcase feature
     *
     * @return zimbraFeatureBriefcasesEnabled, or false if unset
     */
    @ZAttr(id=498)
    public boolean isFeatureBriefcasesEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureBriefcasesEnabled, false);
    }

    /**
     * calendar features
     *
     * @return zimbraFeatureCalendarEnabled, or false if unset
     */
    @ZAttr(id=136)
    public boolean isFeatureCalendarEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureCalendarEnabled, false);
    }

    /**
     * calendar upsell enabled
     *
     * @return zimbraFeatureCalendarUpsellEnabled, or false if unset
     */
    @ZAttr(id=531)
    public boolean isFeatureCalendarUpsellEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureCalendarUpsellEnabled, false);
    }

    /**
     * calendar upsell URL
     *
     * @return zimbraFeatureCalendarUpsellURL, or null unset
     */
    @ZAttr(id=532)
    public String getFeatureCalendarUpsellURL() {
        return getAttr(Provisioning.A_zimbraFeatureCalendarUpsellURL);
    }

    /**
     * password changing
     *
     * @return zimbraFeatureChangePasswordEnabled, or false if unset
     */
    @ZAttr(id=141)
    public boolean isFeatureChangePasswordEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureChangePasswordEnabled, false);
    }

    /**
     * whether or not compose messages in a new windows is allowed
     *
     * @return zimbraFeatureComposeInNewWindowEnabled, or false if unset
     */
    @ZAttr(id=584)
    public boolean isFeatureComposeInNewWindowEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureComposeInNewWindowEnabled, false);
    }

    /**
     * contact features
     *
     * @return zimbraFeatureContactsEnabled, or false if unset
     */
    @ZAttr(id=135)
    public boolean isFeatureContactsEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureContactsEnabled, false);
    }

    /**
     * address book upsell enabled
     *
     * @return zimbraFeatureContactsUpsellEnabled, or false if unset
     */
    @ZAttr(id=529)
    public boolean isFeatureContactsUpsellEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureContactsUpsellEnabled, false);
    }

    /**
     * address book upsell URL
     *
     * @return zimbraFeatureContactsUpsellURL, or null unset
     */
    @ZAttr(id=530)
    public String getFeatureContactsUpsellURL() {
        return getAttr(Provisioning.A_zimbraFeatureContactsUpsellURL);
    }

    /**
     * conversations
     *
     * @return zimbraFeatureConversationsEnabled, or false if unset
     */
    @ZAttr(id=140)
    public boolean isFeatureConversationsEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureConversationsEnabled, false);
    }

    /**
     * filter prefs enabled
     *
     * @return zimbraFeatureFiltersEnabled, or false if unset
     */
    @ZAttr(id=143)
    public boolean isFeatureFiltersEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureFiltersEnabled, false);
    }

    /**
     * whether to allow use of flagging feature
     *
     * @return zimbraFeatureFlaggingEnabled, or false if unset
     */
    @ZAttr(id=499)
    public boolean isFeatureFlaggingEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureFlaggingEnabled, false);
    }

    /**
     * enable auto-completion from the GAL, zimbraFeatureGalEnabled also has
     * to be enabled for the auto-completion feature
     *
     * @return zimbraFeatureGalAutoCompleteEnabled, or false if unset
     */
    @ZAttr(id=359)
    public boolean isFeatureGalAutoCompleteEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureGalAutoCompleteEnabled, false);
    }

    /**
     * whether GAL features are enabled
     *
     * @return zimbraFeatureGalEnabled, or false if unset
     */
    @ZAttr(id=149)
    public boolean isFeatureGalEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureGalEnabled, false);
    }

    /**
     * whether GAL sync feature is enabled
     *
     * @return zimbraFeatureGalSyncEnabled, or false if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=711)
    public boolean isFeatureGalSyncEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureGalSyncEnabled, false);
    }

    /**
     * group calendar features. if set to FALSE, calendar works as a personal
     * calendar and attendees and scheduling etc are turned off in web UI
     *
     * @return zimbraFeatureGroupCalendarEnabled, or false if unset
     */
    @ZAttr(id=481)
    public boolean isFeatureGroupCalendarEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureGroupCalendarEnabled, false);
    }

    /**
     * enabled html composing
     *
     * @return zimbraFeatureHtmlComposeEnabled, or false if unset
     */
    @ZAttr(id=219)
    public boolean isFeatureHtmlComposeEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureHtmlComposeEnabled, false);
    }

    /**
     * IM features
     *
     * @return zimbraFeatureIMEnabled, or false if unset
     */
    @ZAttr(id=305)
    public boolean isFeatureIMEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureIMEnabled, false);
    }

    /**
     * whether to allow use of identities feature
     *
     * @return zimbraFeatureIdentitiesEnabled, or false if unset
     */
    @ZAttr(id=415)
    public boolean isFeatureIdentitiesEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureIdentitiesEnabled, false);
    }

    /**
     * whether user is allowed to retrieve mail from an external IMAP data
     * source
     *
     * @return zimbraFeatureImapDataSourceEnabled, or false if unset
     */
    @ZAttr(id=568)
    public boolean isFeatureImapDataSourceEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureImapDataSourceEnabled, false);
    }

    /**
     * whether import export folder feature is enabled
     *
     * @return zimbraFeatureImportExportFolderEnabled, or false if unset
     *
     * @since ZCS future
     */
    @ZAttr(id=750)
    public boolean isFeatureImportExportFolderEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureImportExportFolderEnabled, false);
    }

    /**
     * preference to set initial search
     *
     * @return zimbraFeatureInitialSearchPreferenceEnabled, or false if unset
     */
    @ZAttr(id=142)
    public boolean isFeatureInitialSearchPreferenceEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureInitialSearchPreferenceEnabled, false);
    }

    /**
     * Enable instant notifications
     *
     * @return zimbraFeatureInstantNotify, or false if unset
     */
    @ZAttr(id=521)
    public boolean isFeatureInstantNotify() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureInstantNotify, false);
    }

    /**
     * email features enabled
     *
     * @return zimbraFeatureMailEnabled, or false if unset
     */
    @ZAttr(id=489)
    public boolean isFeatureMailEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureMailEnabled, false);
    }

    /**
     * enable end-user mail forwarding features
     *
     * @return zimbraFeatureMailForwardingEnabled, or false if unset
     */
    @ZAttr(id=342)
    public boolean isFeatureMailForwardingEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureMailForwardingEnabled, false);
    }

    /**
     * enable end-user mail forwarding defined in mail filters features
     *
     * @return zimbraFeatureMailForwardingInFiltersEnabled, or false if unset
     */
    @ZAttr(id=704)
    public boolean isFeatureMailForwardingInFiltersEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureMailForwardingInFiltersEnabled, false);
    }

    /**
     * Deprecated since: 5.0. done via skin template overrides. Orig desc:
     * whether user is allowed to set mail polling interval
     *
     * @return zimbraFeatureMailPollingIntervalPreferenceEnabled, or false if unset
     */
    @ZAttr(id=441)
    public boolean isFeatureMailPollingIntervalPreferenceEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureMailPollingIntervalPreferenceEnabled, false);
    }

    /**
     * mail priority feature
     *
     * @return zimbraFeatureMailPriorityEnabled, or false if unset
     */
    @ZAttr(id=566)
    public boolean isFeatureMailPriorityEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureMailPriorityEnabled, false);
    }

    /**
     * email upsell enabled
     *
     * @return zimbraFeatureMailUpsellEnabled, or false if unset
     */
    @ZAttr(id=527)
    public boolean isFeatureMailUpsellEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureMailUpsellEnabled, false);
    }

    /**
     * email upsell URL
     *
     * @return zimbraFeatureMailUpsellURL, or null unset
     */
    @ZAttr(id=528)
    public String getFeatureMailUpsellURL() {
        return getAttr(Provisioning.A_zimbraFeatureMailUpsellURL);
    }

    /**
     * whether to permit mobile sync
     *
     * @return zimbraFeatureMobileSyncEnabled, or false if unset
     */
    @ZAttr(id=347)
    public boolean isFeatureMobileSyncEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureMobileSyncEnabled, false);
    }

    /**
     * Whether user can create address books
     *
     * @return zimbraFeatureNewAddrBookEnabled, or false if unset
     */
    @ZAttr(id=631)
    public boolean isFeatureNewAddrBookEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureNewAddrBookEnabled, false);
    }

    /**
     * Whether new mail notification feature should be allowed for this
     * account or in this cos
     *
     * @return zimbraFeatureNewMailNotificationEnabled, or false if unset
     */
    @ZAttr(id=367)
    public boolean isFeatureNewMailNotificationEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureNewMailNotificationEnabled, false);
    }

    /**
     * Whether notebook feature should be allowed for this account or in this
     * cos
     *
     * @return zimbraFeatureNotebookEnabled, or false if unset
     */
    @ZAttr(id=356)
    public boolean isFeatureNotebookEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureNotebookEnabled, false);
    }

    /**
     * whether or not open a new msg/conv in a new windows is allowed
     *
     * @return zimbraFeatureOpenMailInNewWindowEnabled, or false if unset
     */
    @ZAttr(id=585)
    public boolean isFeatureOpenMailInNewWindowEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureOpenMailInNewWindowEnabled, false);
    }

    /**
     * whether an account can modify its zimbraPref* attributes
     *
     * @return zimbraFeatureOptionsEnabled, or false if unset
     */
    @ZAttr(id=451)
    public boolean isFeatureOptionsEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureOptionsEnabled, false);
    }

    /**
     * Whether out of office reply feature should be allowed for this account
     * or in this cos
     *
     * @return zimbraFeatureOutOfOfficeReplyEnabled, or false if unset
     */
    @ZAttr(id=366)
    public boolean isFeatureOutOfOfficeReplyEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureOutOfOfficeReplyEnabled, false);
    }

    /**
     * whether user is allowed to retrieve mail from an external POP3 data
     * source
     *
     * @return zimbraFeaturePop3DataSourceEnabled, or false if unset
     */
    @ZAttr(id=416)
    public boolean isFeaturePop3DataSourceEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeaturePop3DataSourceEnabled, false);
    }

    /**
     * portal features
     *
     * @return zimbraFeaturePortalEnabled, or false if unset
     */
    @ZAttr(id=447)
    public boolean isFeaturePortalEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeaturePortalEnabled, false);
    }

    /**
     * saved search feature
     *
     * @return zimbraFeatureSavedSearchesEnabled, or false if unset
     */
    @ZAttr(id=139)
    public boolean isFeatureSavedSearchesEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureSavedSearchesEnabled, false);
    }

    /**
     * enabled sharing
     *
     * @return zimbraFeatureSharingEnabled, or false if unset
     */
    @ZAttr(id=335)
    public boolean isFeatureSharingEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureSharingEnabled, false);
    }

    /**
     * keyboard shortcuts aliases features
     *
     * @return zimbraFeatureShortcutAliasesEnabled, or false if unset
     */
    @ZAttr(id=452)
    public boolean isFeatureShortcutAliasesEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureShortcutAliasesEnabled, false);
    }

    /**
     * whether to allow use of signature feature
     *
     * @return zimbraFeatureSignaturesEnabled, or false if unset
     */
    @ZAttr(id=494)
    public boolean isFeatureSignaturesEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureSignaturesEnabled, false);
    }

    /**
     * Whether changing skin is allowed for this account or in this cos
     *
     * @return zimbraFeatureSkinChangeEnabled, or false if unset
     */
    @ZAttr(id=354)
    public boolean isFeatureSkinChangeEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureSkinChangeEnabled, false);
    }

    /**
     * tagging feature
     *
     * @return zimbraFeatureTaggingEnabled, or false if unset
     */
    @ZAttr(id=137)
    public boolean isFeatureTaggingEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureTaggingEnabled, false);
    }

    /**
     * whether to allow use of tasks feature
     *
     * @return zimbraFeatureTasksEnabled, or false if unset
     */
    @ZAttr(id=436)
    public boolean isFeatureTasksEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureTasksEnabled, false);
    }

    /**
     * option to view attachments in html
     *
     * @return zimbraFeatureViewInHtmlEnabled, or false if unset
     */
    @ZAttr(id=312)
    public boolean isFeatureViewInHtmlEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureViewInHtmlEnabled, false);
    }

    /**
     * Voicemail features enabled
     *
     * @return zimbraFeatureVoiceEnabled, or false if unset
     */
    @ZAttr(id=445)
    public boolean isFeatureVoiceEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureVoiceEnabled, false);
    }

    /**
     * voice upsell enabled
     *
     * @return zimbraFeatureVoiceUpsellEnabled, or false if unset
     */
    @ZAttr(id=533)
    public boolean isFeatureVoiceUpsellEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureVoiceUpsellEnabled, false);
    }

    /**
     * voice upsell URL
     *
     * @return zimbraFeatureVoiceUpsellURL, or null unset
     */
    @ZAttr(id=534)
    public String getFeatureVoiceUpsellURL() {
        return getAttr(Provisioning.A_zimbraFeatureVoiceUpsellURL);
    }

    /**
     * whether web search feature is enabled
     *
     * @return zimbraFeatureWebSearchEnabled, or false if unset
     */
    @ZAttr(id=602)
    public boolean isFeatureWebSearchEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureWebSearchEnabled, false);
    }

    /**
     * Zimbra Assistant enabled
     *
     * @return zimbraFeatureZimbraAssistantEnabled, or false if unset
     */
    @ZAttr(id=544)
    public boolean isFeatureZimbraAssistantEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureZimbraAssistantEnabled, false);
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
     * when set to TRUE, free/busy for the account is not calculated from
     * local mailbox.
     *
     * @return zimbraFreebusyLocalMailboxNotActive, or false if unset
     *
     * @since ZCS 5.0.11
     */
    @ZAttr(id=752)
    public boolean isFreebusyLocalMailboxNotActive() {
        return getBooleanAttr(Provisioning.A_zimbraFreebusyLocalMailboxNotActive, false);
    }

    /**
     * available IM interop gateways
     *
     * @return zimbraIMAvailableInteropGateways, or ampty array if unset
     */
    @ZAttr(id=571)
    public String[] getIMAvailableInteropGateways() {
        return getMultiAttr(Provisioning.A_zimbraIMAvailableInteropGateways);
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
     * maximum number of identities allowed on an account
     *
     * @return zimbraIdentityMaxNumEntries, or -1 if unset
     */
    @ZAttr(id=414)
    public int getIdentityMaxNumEntries() {
        return getIntAttr(Provisioning.A_zimbraIdentityMaxNumEntries, -1);
    }

    /**
     * whether IMAP is enabled for an account
     *
     * @return zimbraImapEnabled, or false if unset
     */
    @ZAttr(id=174)
    public boolean isImapEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraImapEnabled, false);
    }

    /**
     * The address to which legal intercept messages will be sent.
     *
     * @return zimbraInterceptAddress, or ampty array if unset
     */
    @ZAttr(id=614)
    public String[] getInterceptAddress() {
        return getMultiAttr(Provisioning.A_zimbraInterceptAddress);
    }

    /**
     * Template used to construct the body of a legal intercept message.
     *
     * @return zimbraInterceptBody, or null unset
     */
    @ZAttr(id=618)
    public String getInterceptBody() {
        return getAttr(Provisioning.A_zimbraInterceptBody);
    }

    /**
     * Template used to construct the sender of a legal intercept message.
     *
     * @return zimbraInterceptFrom, or null unset
     */
    @ZAttr(id=616)
    public String getInterceptFrom() {
        return getAttr(Provisioning.A_zimbraInterceptFrom);
    }

    /**
     * Specifies whether legal intercept messages should contain the entire
     * original message or just the headers.
     *
     * @return zimbraInterceptSendHeadersOnly, or false if unset
     */
    @ZAttr(id=615)
    public boolean isInterceptSendHeadersOnly() {
        return getBooleanAttr(Provisioning.A_zimbraInterceptSendHeadersOnly, false);
    }

    /**
     * Template used to construct the subject of a legal intercept message.
     *
     * @return zimbraInterceptSubject, or null unset
     */
    @ZAttr(id=617)
    public String getInterceptSubject() {
        return getAttr(Provisioning.A_zimbraInterceptSubject);
    }

    /**
     * Whether to index junk messages
     *
     * @return zimbraJunkMessagesIndexingEnabled, or false if unset
     */
    @ZAttr(id=579)
    public boolean isJunkMessagesIndexingEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraJunkMessagesIndexingEnabled, false);
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
     * servers that an account can be initially provisioned on
     *
     * @return zimbraMailHostPool, or ampty array if unset
     */
    @ZAttr(id=125)
    public String[] getMailHostPool() {
        return getMultiAttr(Provisioning.A_zimbraMailHostPool);
    }

    /**
     * idle timeout (nnnnn[hmsd])
     *
     * <p>Use getMailIdleSessionTimeoutAsString to access value as a string.
     *
     * @see #getMailIdleSessionTimeoutAsString()
     *
     * @return zimbraMailIdleSessionTimeout in millseconds, or -1 if unset
     */
    @ZAttr(id=147)
    public long getMailIdleSessionTimeout() {
        return getTimeInterval(Provisioning.A_zimbraMailIdleSessionTimeout, -1);
    }

    /**
     * idle timeout (nnnnn[hmsd])
     *
     * @return zimbraMailIdleSessionTimeout, or null unset
     */
    @ZAttr(id=147)
    public String getMailIdleSessionTimeoutAsString() {
        return getAttr(Provisioning.A_zimbraMailIdleSessionTimeout);
    }

    /**
     * lifetime (nnnnn[hmsd]) of a mail message regardless of location
     *
     * <p>Use getMailMessageLifetimeAsString to access value as a string.
     *
     * @see #getMailMessageLifetimeAsString()
     *
     * @return zimbraMailMessageLifetime in millseconds, or -1 if unset
     */
    @ZAttr(id=106)
    public long getMailMessageLifetime() {
        return getTimeInterval(Provisioning.A_zimbraMailMessageLifetime, -1);
    }

    /**
     * lifetime (nnnnn[hmsd]) of a mail message regardless of location
     *
     * @return zimbraMailMessageLifetime, or null unset
     */
    @ZAttr(id=106)
    public String getMailMessageLifetimeAsString() {
        return getAttr(Provisioning.A_zimbraMailMessageLifetime);
    }

    /**
     * minimum allowed value for zimbraPrefMailPollingInterval (nnnnn[hmsd])
     *
     * <p>Use getMailMinPollingIntervalAsString to access value as a string.
     *
     * @see #getMailMinPollingIntervalAsString()
     *
     * @return zimbraMailMinPollingInterval in millseconds, or -1 if unset
     */
    @ZAttr(id=110)
    public long getMailMinPollingInterval() {
        return getTimeInterval(Provisioning.A_zimbraMailMinPollingInterval, -1);
    }

    /**
     * minimum allowed value for zimbraPrefMailPollingInterval (nnnnn[hmsd])
     *
     * @return zimbraMailMinPollingInterval, or null unset
     */
    @ZAttr(id=110)
    public String getMailMinPollingIntervalAsString() {
        return getAttr(Provisioning.A_zimbraMailMinPollingInterval);
    }

    /**
     * If TRUE, a message is purged from trash based on the date that it was
     * moved to the Trash folder. If FALSE, a message is purged from Trash
     * based on the date that it was added to the mailbox.
     *
     * @return zimbraMailPurgeUseChangeDateForTrash, or false if unset
     *
     * @since ZCS future
     */
    @ZAttr(id=748)
    public boolean isMailPurgeUseChangeDateForTrash() {
        return getBooleanAttr(Provisioning.A_zimbraMailPurgeUseChangeDateForTrash, false);
    }

    /**
     * mail quota in bytes
     *
     * @return zimbraMailQuota, or -1 if unset
     */
    @ZAttr(id=16)
    public long getMailQuota() {
        return getLongAttr(Provisioning.A_zimbraMailQuota, -1);
    }

    /**
     * maximum length of mail signature, 0 means unlimited. If not set,
     * default is 1024
     *
     * @return zimbraMailSignatureMaxLength, or -1 if unset
     */
    @ZAttr(id=454)
    public long getMailSignatureMaxLength() {
        return getLongAttr(Provisioning.A_zimbraMailSignatureMaxLength, -1);
    }

    /**
     * Retention period of messages in the Junk folder. 0 means that all
     * messages will be retained. This admin-modifiable attribute works in
     * conjunction with zimbraPrefJunkLifetime, which is user-modifiable. The
     * shorter duration is used.
     *
     * <p>Use getMailSpamLifetimeAsString to access value as a string.
     *
     * @see #getMailSpamLifetimeAsString()
     *
     * @return zimbraMailSpamLifetime in millseconds, or -1 if unset
     */
    @ZAttr(id=105)
    public long getMailSpamLifetime() {
        return getTimeInterval(Provisioning.A_zimbraMailSpamLifetime, -1);
    }

    /**
     * Retention period of messages in the Junk folder. 0 means that all
     * messages will be retained. This admin-modifiable attribute works in
     * conjunction with zimbraPrefJunkLifetime, which is user-modifiable. The
     * shorter duration is used.
     *
     * @return zimbraMailSpamLifetime, or null unset
     */
    @ZAttr(id=105)
    public String getMailSpamLifetimeAsString() {
        return getAttr(Provisioning.A_zimbraMailSpamLifetime);
    }

    /**
     * Retention period of messages in the Trash folder. 0 means that all
     * messages will be retained. This admin-modifiable attribute works in
     * conjunction with zimbraPrefTrashLifetime, which is user-modifiable.
     * The shorter duration is used.
     *
     * <p>Use getMailTrashLifetimeAsString to access value as a string.
     *
     * @see #getMailTrashLifetimeAsString()
     *
     * @return zimbraMailTrashLifetime in millseconds, or -1 if unset
     */
    @ZAttr(id=104)
    public long getMailTrashLifetime() {
        return getTimeInterval(Provisioning.A_zimbraMailTrashLifetime, -1);
    }

    /**
     * Retention period of messages in the Trash folder. 0 means that all
     * messages will be retained. This admin-modifiable attribute works in
     * conjunction with zimbraPrefTrashLifetime, which is user-modifiable.
     * The shorter duration is used.
     *
     * @return zimbraMailTrashLifetime, or null unset
     */
    @ZAttr(id=104)
    public String getMailTrashLifetimeAsString() {
        return getAttr(Provisioning.A_zimbraMailTrashLifetime);
    }

    /**
     * template used to construct the body of an email notification message
     *
     * @return zimbraNewMailNotificationBody, or null unset
     */
    @ZAttr(id=152)
    public String getNewMailNotificationBody() {
        return getAttr(Provisioning.A_zimbraNewMailNotificationBody);
    }

    /**
     * template used to construct the sender of an email notification message
     *
     * @return zimbraNewMailNotificationFrom, or null unset
     */
    @ZAttr(id=150)
    public String getNewMailNotificationFrom() {
        return getAttr(Provisioning.A_zimbraNewMailNotificationFrom);
    }

    /**
     * template used to construct the subject of an email notification
     * message
     *
     * @return zimbraNewMailNotificationSubject, or null unset
     */
    @ZAttr(id=151)
    public String getNewMailNotificationSubject() {
        return getAttr(Provisioning.A_zimbraNewMailNotificationSubject);
    }

    /**
     * maximum number of revisions to keep for wiki pages and documents. 0
     * means unlimited.
     *
     * @return zimbraNotebookMaxRevisions, or -1 if unset
     */
    @ZAttr(id=482)
    public int getNotebookMaxRevisions() {
        return getIntAttr(Provisioning.A_zimbraNotebookMaxRevisions, -1);
    }

    /**
     * whether to strip off potentially harming HTML tags in Wiki and HTML
     * Documents.
     *
     * @return zimbraNotebookSanitizeHtml, or false if unset
     */
    @ZAttr(id=646)
    public boolean isNotebookSanitizeHtml() {
        return getBooleanAttr(Provisioning.A_zimbraNotebookSanitizeHtml, false);
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
     * whether or not to enforce password history. Number of unique passwords
     * a user must have before being allowed to re-use an old one. A value of
     * 0 means no password history.
     *
     * @return zimbraPasswordEnforceHistory, or -1 if unset
     */
    @ZAttr(id=37)
    public int getPasswordEnforceHistory() {
        return getIntAttr(Provisioning.A_zimbraPasswordEnforceHistory, -1);
    }

    /**
     * user is unable to change password
     *
     * @return zimbraPasswordLocked, or false if unset
     */
    @ZAttr(id=45)
    public boolean isPasswordLocked() {
        return getBooleanAttr(Provisioning.A_zimbraPasswordLocked, false);
    }

    /**
     * how long an account is locked out. Use 0 to lockout an account until
     * admin resets it
     *
     * <p>Use getPasswordLockoutDurationAsString to access value as a string.
     *
     * @see #getPasswordLockoutDurationAsString()
     *
     * @return zimbraPasswordLockoutDuration in millseconds, or -1 if unset
     */
    @ZAttr(id=379)
    public long getPasswordLockoutDuration() {
        return getTimeInterval(Provisioning.A_zimbraPasswordLockoutDuration, -1);
    }

    /**
     * how long an account is locked out. Use 0 to lockout an account until
     * admin resets it
     *
     * @return zimbraPasswordLockoutDuration, or null unset
     */
    @ZAttr(id=379)
    public String getPasswordLockoutDurationAsString() {
        return getAttr(Provisioning.A_zimbraPasswordLockoutDuration);
    }

    /**
     * whether or not account lockout is enabled.
     *
     * @return zimbraPasswordLockoutEnabled, or false if unset
     */
    @ZAttr(id=378)
    public boolean isPasswordLockoutEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPasswordLockoutEnabled, false);
    }

    /**
     * the duration after which old consecutive failed login attempts are
     * purged from the list, even though no successful authentication has
     * occurred
     *
     * <p>Use getPasswordLockoutFailureLifetimeAsString to access value as a string.
     *
     * @see #getPasswordLockoutFailureLifetimeAsString()
     *
     * @return zimbraPasswordLockoutFailureLifetime in millseconds, or -1 if unset
     */
    @ZAttr(id=381)
    public long getPasswordLockoutFailureLifetime() {
        return getTimeInterval(Provisioning.A_zimbraPasswordLockoutFailureLifetime, -1);
    }

    /**
     * the duration after which old consecutive failed login attempts are
     * purged from the list, even though no successful authentication has
     * occurred
     *
     * @return zimbraPasswordLockoutFailureLifetime, or null unset
     */
    @ZAttr(id=381)
    public String getPasswordLockoutFailureLifetimeAsString() {
        return getAttr(Provisioning.A_zimbraPasswordLockoutFailureLifetime);
    }

    /**
     * number of consecutive failed login attempts until an account is locked
     * out
     *
     * @return zimbraPasswordLockoutMaxFailures, or -1 if unset
     */
    @ZAttr(id=380)
    public int getPasswordLockoutMaxFailures() {
        return getIntAttr(Provisioning.A_zimbraPasswordLockoutMaxFailures, -1);
    }

    /**
     * maximum days between password changes
     *
     * @return zimbraPasswordMaxAge, or -1 if unset
     */
    @ZAttr(id=36)
    public int getPasswordMaxAge() {
        return getIntAttr(Provisioning.A_zimbraPasswordMaxAge, -1);
    }

    /**
     * max length of a password
     *
     * @return zimbraPasswordMaxLength, or -1 if unset
     */
    @ZAttr(id=34)
    public int getPasswordMaxLength() {
        return getIntAttr(Provisioning.A_zimbraPasswordMaxLength, -1);
    }

    /**
     * minimum days between password changes
     *
     * @return zimbraPasswordMinAge, or -1 if unset
     */
    @ZAttr(id=35)
    public int getPasswordMinAge() {
        return getIntAttr(Provisioning.A_zimbraPasswordMinAge, -1);
    }

    /**
     * minimum length of a password
     *
     * @return zimbraPasswordMinLength, or -1 if unset
     */
    @ZAttr(id=33)
    public int getPasswordMinLength() {
        return getIntAttr(Provisioning.A_zimbraPasswordMinLength, -1);
    }

    /**
     * minimum number of lower case characters required in a password
     *
     * @return zimbraPasswordMinLowerCaseChars, or -1 if unset
     */
    @ZAttr(id=390)
    public int getPasswordMinLowerCaseChars() {
        return getIntAttr(Provisioning.A_zimbraPasswordMinLowerCaseChars, -1);
    }

    /**
     * minimum number of numeric characters required in a password
     *
     * @return zimbraPasswordMinNumericChars, or -1 if unset
     */
    @ZAttr(id=392)
    public int getPasswordMinNumericChars() {
        return getIntAttr(Provisioning.A_zimbraPasswordMinNumericChars, -1);
    }

    /**
     * minimum number of ascii punctuation characters required in a password
     *
     * @return zimbraPasswordMinPunctuationChars, or -1 if unset
     */
    @ZAttr(id=391)
    public int getPasswordMinPunctuationChars() {
        return getIntAttr(Provisioning.A_zimbraPasswordMinPunctuationChars, -1);
    }

    /**
     * minimum number of upper case characters required in a password
     *
     * @return zimbraPasswordMinUpperCaseChars, or -1 if unset
     */
    @ZAttr(id=389)
    public int getPasswordMinUpperCaseChars() {
        return getIntAttr(Provisioning.A_zimbraPasswordMinUpperCaseChars, -1);
    }

    /**
     * whether POP3 is enabled for an account
     *
     * @return zimbraPop3Enabled, or false if unset
     */
    @ZAttr(id=175)
    public boolean isPop3Enabled() {
        return getBooleanAttr(Provisioning.A_zimbraPop3Enabled, false);
    }

    /**
     * portal name
     *
     * @return zimbraPortalName, or null unset
     */
    @ZAttr(id=448)
    public String getPortalName() {
        return getAttr(Provisioning.A_zimbraPortalName);
    }

    /**
     * After login, whether the advanced client should enforce minimum
     * display resolution
     *
     * @return zimbraPrefAdvancedClientEnforceMinDisplay, or false if unset
     */
    @ZAttr(id=678)
    public boolean isPrefAdvancedClientEnforceMinDisplay() {
        return getBooleanAttr(Provisioning.A_zimbraPrefAdvancedClientEnforceMinDisplay, false);
    }

    /**
     * whether or not new address in outgoing email are auto added to address
     * book
     *
     * @return zimbraPrefAutoAddAddressEnabled, or false if unset
     */
    @ZAttr(id=131)
    public boolean isPrefAutoAddAddressEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPrefAutoAddAddressEnabled, false);
    }

    /**
     * time to wait before auto saving a draft(nnnnn[hmsd])
     *
     * <p>Use getPrefAutoSaveDraftIntervalAsString to access value as a string.
     *
     * @see #getPrefAutoSaveDraftIntervalAsString()
     *
     * @return zimbraPrefAutoSaveDraftInterval in millseconds, or -1 if unset
     */
    @ZAttr(id=561)
    public long getPrefAutoSaveDraftInterval() {
        return getTimeInterval(Provisioning.A_zimbraPrefAutoSaveDraftInterval, -1);
    }

    /**
     * time to wait before auto saving a draft(nnnnn[hmsd])
     *
     * @return zimbraPrefAutoSaveDraftInterval, or null unset
     */
    @ZAttr(id=561)
    public String getPrefAutoSaveDraftIntervalAsString() {
        return getAttr(Provisioning.A_zimbraPrefAutoSaveDraftInterval);
    }

    /**
     * whether to allow a cancel email sent to organizer of appointment
     *
     * @return zimbraPrefCalendarAllowCancelEmailToSelf, or false if unset
     */
    @ZAttr(id=702)
    public boolean isPrefCalendarAllowCancelEmailToSelf() {
        return getBooleanAttr(Provisioning.A_zimbraPrefCalendarAllowCancelEmailToSelf, false);
    }

    /**
     * whether calendar invite part in a forwarded email is auto-added to
     * calendar
     *
     * @return zimbraPrefCalendarAllowForwardedInvite, or false if unset
     */
    @ZAttr(id=686)
    public boolean isPrefCalendarAllowForwardedInvite() {
        return getBooleanAttr(Provisioning.A_zimbraPrefCalendarAllowForwardedInvite, false);
    }

    /**
     * whether calendar invite part without method parameter in Content-Type
     * header is auto-added to calendar
     *
     * @return zimbraPrefCalendarAllowMethodlessInvite, or false if unset
     */
    @ZAttr(id=687)
    public boolean isPrefCalendarAllowMethodlessInvite() {
        return getBooleanAttr(Provisioning.A_zimbraPrefCalendarAllowMethodlessInvite, false);
    }

    /**
     * whether calendar invite part with PUBLISH method is auto-added to
     * calendar
     *
     * @return zimbraPrefCalendarAllowPublishMethodInvite, or false if unset
     */
    @ZAttr(id=688)
    public boolean isPrefCalendarAllowPublishMethodInvite() {
        return getBooleanAttr(Provisioning.A_zimbraPrefCalendarAllowPublishMethodInvite, false);
    }

    /**
     * always show the mini calendar
     *
     * @return zimbraPrefCalendarAlwaysShowMiniCal, or false if unset
     */
    @ZAttr(id=276)
    public boolean isPrefCalendarAlwaysShowMiniCal() {
        return getBooleanAttr(Provisioning.A_zimbraPrefCalendarAlwaysShowMiniCal, false);
    }

    /**
     * number of minutes (0 = never) before appt to show reminder dialog
     *
     * @return zimbraPrefCalendarApptReminderWarningTime, or -1 if unset
     */
    @ZAttr(id=341)
    public int getPrefCalendarApptReminderWarningTime() {
        return getIntAttr(Provisioning.A_zimbraPrefCalendarApptReminderWarningTime, -1);
    }

    /**
     * hour of day that the day view should end at, non-inclusive (16=4pm, 24
     * = midnight, etc)
     *
     * @return zimbraPrefCalendarDayHourEnd, or -1 if unset
     */
    @ZAttr(id=440)
    public int getPrefCalendarDayHourEnd() {
        return getIntAttr(Provisioning.A_zimbraPrefCalendarDayHourEnd, -1);
    }

    /**
     * hour of day that the day view should start at (1=1 AM, 8=8 AM, etc)
     *
     * @return zimbraPrefCalendarDayHourStart, or -1 if unset
     */
    @ZAttr(id=439)
    public int getPrefCalendarDayHourStart() {
        return getIntAttr(Provisioning.A_zimbraPrefCalendarDayHourStart, -1);
    }

    /**
     * first day of week to show in calendar (0=sunday, 6=saturday)
     *
     * @return zimbraPrefCalendarFirstDayOfWeek, or -1 if unset
     */
    @ZAttr(id=261)
    public int getPrefCalendarFirstDayOfWeek() {
        return getIntAttr(Provisioning.A_zimbraPrefCalendarFirstDayOfWeek, -1);
    }

    /**
     * initial calendar view to use
     *
     * <p>Valid values: [list, month, schedule, day, workWeek, week]
     *
     * @return zimbraPrefCalendarInitialView, or null unset
     */
    @ZAttr(id=240)
    public String getPrefCalendarInitialView() {
        return getAttr(Provisioning.A_zimbraPrefCalendarInitialView);
    }

    /**
     * If set to true, user is notified by email of changes made to her
     * calendar by others via delegated calendar access.
     *
     * @return zimbraPrefCalendarNotifyDelegatedChanges, or false if unset
     */
    @ZAttr(id=273)
    public boolean isPrefCalendarNotifyDelegatedChanges() {
        return getBooleanAttr(Provisioning.A_zimbraPrefCalendarNotifyDelegatedChanges, false);
    }

    /**
     * When to send the first reminder for an event.
     *
     * @return zimbraPrefCalendarReminderDuration1, or null unset
     */
    @ZAttr(id=573)
    public String getPrefCalendarReminderDuration1() {
        return getAttr(Provisioning.A_zimbraPrefCalendarReminderDuration1);
    }

    /**
     * When to send the second reminder for an event.
     *
     * @return zimbraPrefCalendarReminderDuration2, or null unset
     */
    @ZAttr(id=574)
    public String getPrefCalendarReminderDuration2() {
        return getAttr(Provisioning.A_zimbraPrefCalendarReminderDuration2);
    }

    /**
     * The email the reminder goes to.
     *
     * @return zimbraPrefCalendarReminderEmail, or null unset
     */
    @ZAttr(id=575)
    public String getPrefCalendarReminderEmail() {
        return getAttr(Provisioning.A_zimbraPrefCalendarReminderEmail);
    }

    /**
     * Flash title when on appointment remimnder notification
     *
     * @return zimbraPrefCalendarReminderFlashTitle, or false if unset
     */
    @ZAttr(id=682)
    public boolean isPrefCalendarReminderFlashTitle() {
        return getBooleanAttr(Provisioning.A_zimbraPrefCalendarReminderFlashTitle, false);
    }

    /**
     * The mobile device (phone) the reminder goes to.
     *
     * @return zimbraPrefCalendarReminderMobile, or false if unset
     */
    @ZAttr(id=577)
    public boolean isPrefCalendarReminderMobile() {
        return getBooleanAttr(Provisioning.A_zimbraPrefCalendarReminderMobile, false);
    }

    /**
     * To send email or to not send email is the question.
     *
     * @return zimbraPrefCalendarReminderSendEmail, or false if unset
     */
    @ZAttr(id=576)
    public boolean isPrefCalendarReminderSendEmail() {
        return getBooleanAttr(Provisioning.A_zimbraPrefCalendarReminderSendEmail, false);
    }

    /**
     * whether audible alert is enabled when appointment notification is
     * played
     *
     * @return zimbraPrefCalendarReminderSoundsEnabled, or false if unset
     */
    @ZAttr(id=667)
    public boolean isPrefCalendarReminderSoundsEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPrefCalendarReminderSoundsEnabled, false);
    }

    /**
     * Send a reminder via YIM
     *
     * @return zimbraPrefCalendarReminderYMessenger, or false if unset
     */
    @ZAttr(id=578)
    public boolean isPrefCalendarReminderYMessenger() {
        return getBooleanAttr(Provisioning.A_zimbraPrefCalendarReminderYMessenger, false);
    }

    /**
     * whether or not use quick add dialog or go into full appt edit view
     *
     * @return zimbraPrefCalendarUseQuickAdd, or false if unset
     */
    @ZAttr(id=274)
    public boolean isPrefCalendarUseQuickAdd() {
        return getBooleanAttr(Provisioning.A_zimbraPrefCalendarUseQuickAdd, false);
    }

    /**
     * user preference of client type
     *
     * <p>Valid values: [standard, advanced]
     *
     * @return zimbraPrefClientType, or null unset
     */
    @ZAttr(id=453)
    public String getPrefClientType() {
        return getAttr(Provisioning.A_zimbraPrefClientType);
    }

    /**
     * whether or not to compose in html or text.
     *
     * <p>Valid values: [html, text]
     *
     * @return zimbraPrefComposeFormat, or null unset
     */
    @ZAttr(id=217)
    public String getPrefComposeFormat() {
        return getAttr(Provisioning.A_zimbraPrefComposeFormat);
    }

    /**
     * whether or not compose messages in a new windows by default
     *
     * @return zimbraPrefComposeInNewWindow, or false if unset
     */
    @ZAttr(id=209)
    public boolean isPrefComposeInNewWindow() {
        return getBooleanAttr(Provisioning.A_zimbraPrefComposeInNewWindow, false);
    }

    /**
     * initial contact view to use
     *
     * <p>Valid values: [list, cards]
     *
     * @return zimbraPrefContactsInitialView, or null unset
     */
    @ZAttr(id=167)
    public String getPrefContactsInitialView() {
        return getAttr(Provisioning.A_zimbraPrefContactsInitialView);
    }

    /**
     * number of contacts per page
     *
     * @return zimbraPrefContactsPerPage, or -1 if unset
     */
    @ZAttr(id=148)
    public int getPrefContactsPerPage() {
        return getIntAttr(Provisioning.A_zimbraPrefContactsPerPage, -1);
    }

    /**
     * dedupeNone|secondCopyIfOnToOrCC|moveSentMessageToInbox|dedupeAll
     *
     * <p>Valid values: [dedupeAll, dedupeNone, secondCopyifOnToOrCC]
     *
     * @return zimbraPrefDedupeMessagesSentToSelf, or null unset
     */
    @ZAttr(id=144)
    public String getPrefDedupeMessagesSentToSelf() {
        return getAttr(Provisioning.A_zimbraPrefDedupeMessagesSentToSelf);
    }

    /**
     * whether meeting invite emails are moved to Trash folder upon
     * accept/decline
     *
     * @return zimbraPrefDeleteInviteOnReply, or false if unset
     */
    @ZAttr(id=470)
    public boolean isPrefDeleteInviteOnReply() {
        return getBooleanAttr(Provisioning.A_zimbraPrefDeleteInviteOnReply, false);
    }

    /**
     * whether to display external images in HTML mail
     *
     * @return zimbraPrefDisplayExternalImages, or false if unset
     */
    @ZAttr(id=511)
    public boolean isPrefDisplayExternalImages() {
        return getBooleanAttr(Provisioning.A_zimbraPrefDisplayExternalImages, false);
    }

    /**
     * whether or not folder tree is expanded
     *
     * @return zimbraPrefFolderTreeOpen, or false if unset
     */
    @ZAttr(id=637)
    public boolean isPrefFolderTreeOpen() {
        return getBooleanAttr(Provisioning.A_zimbraPrefFolderTreeOpen, false);
    }

    /**
     * what part of the original message to include during forwards
     * (deprecatedSince 5.0 in identity)
     *
     * <p>Valid values: [includeAsAttachment, includeBodyAndHeadersWithPrefix, includeBody, includeBodyWithPrefix]
     *
     * @return zimbraPrefForwardIncludeOriginalText, or null unset
     */
    @ZAttr(id=134)
    public String getPrefForwardIncludeOriginalText() {
        return getAttr(Provisioning.A_zimbraPrefForwardIncludeOriginalText);
    }

    /**
     * what format we reply/forward messages in (deprecatedSince 5.0 in
     * identity)
     *
     * <p>Valid values: [same, html, text]
     *
     * @return zimbraPrefForwardReplyFormat, or null unset
     */
    @ZAttr(id=413)
    public String getPrefForwardReplyFormat() {
        return getAttr(Provisioning.A_zimbraPrefForwardReplyFormat);
    }

    /**
     * Deprecated since: 4.5. Deprecated in favor of
     * zimbraPrefForwardReplyFormat. Orig desc: whether or not to use same
     * format (text or html) of message we are replying to
     *
     * @return zimbraPrefForwardReplyInOriginalFormat, or false if unset
     */
    @ZAttr(id=218)
    public boolean isPrefForwardReplyInOriginalFormat() {
        return getBooleanAttr(Provisioning.A_zimbraPrefForwardReplyInOriginalFormat, false);
    }

    /**
     * prefix character to use during forward/reply (deprecatedSince 5.0 in
     * identity)
     *
     * @return zimbraPrefForwardReplyPrefixChar, or null unset
     */
    @ZAttr(id=130)
    public String getPrefForwardReplyPrefixChar() {
        return getAttr(Provisioning.A_zimbraPrefForwardReplyPrefixChar);
    }

    /**
     * whether end-user wants auto-complete from GAL. Feature must also be
     * enabled.
     *
     * @return zimbraPrefGalAutoCompleteEnabled, or false if unset
     */
    @ZAttr(id=372)
    public boolean isPrefGalAutoCompleteEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPrefGalAutoCompleteEnabled, false);
    }

    /**
     * whether end-user wants search from GAL. Feature must also be enabled
     *
     * @return zimbraPrefGalSearchEnabled, or false if unset
     */
    @ZAttr(id=635)
    public boolean isPrefGalSearchEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPrefGalSearchEnabled, false);
    }

    /**
     * how to group mail by default
     *
     * <p>Valid values: [conversation, message]
     *
     * @return zimbraPrefGroupMailBy, or null unset
     */
    @ZAttr(id=54)
    public String getPrefGroupMailBy() {
        return getAttr(Provisioning.A_zimbraPrefGroupMailBy);
    }

    /**
     * default font color
     *
     * @return zimbraPrefHtmlEditorDefaultFontColor, or null unset
     */
    @ZAttr(id=260)
    public String getPrefHtmlEditorDefaultFontColor() {
        return getAttr(Provisioning.A_zimbraPrefHtmlEditorDefaultFontColor);
    }

    /**
     * default font family
     *
     * @return zimbraPrefHtmlEditorDefaultFontFamily, or null unset
     */
    @ZAttr(id=258)
    public String getPrefHtmlEditorDefaultFontFamily() {
        return getAttr(Provisioning.A_zimbraPrefHtmlEditorDefaultFontFamily);
    }

    /**
     * default font size
     *
     * @return zimbraPrefHtmlEditorDefaultFontSize, or null unset
     */
    @ZAttr(id=259)
    public String getPrefHtmlEditorDefaultFontSize() {
        return getAttr(Provisioning.A_zimbraPrefHtmlEditorDefaultFontSize);
    }

    /**
     * whether to login to the IM client automatically
     *
     * @return zimbraPrefIMAutoLogin, or false if unset
     */
    @ZAttr(id=488)
    public boolean isPrefIMAutoLogin() {
        return getBooleanAttr(Provisioning.A_zimbraPrefIMAutoLogin, false);
    }

    /**
     * IM buddy list sort order
     *
     * @return zimbraPrefIMBuddyListSort, or null unset
     */
    @ZAttr(id=705)
    public String getPrefIMBuddyListSort() {
        return getAttr(Provisioning.A_zimbraPrefIMBuddyListSort);
    }

    /**
     * Flash IM icon on new messages
     *
     * @return zimbraPrefIMFlashIcon, or false if unset
     */
    @ZAttr(id=462)
    public boolean isPrefIMFlashIcon() {
        return getBooleanAttr(Provisioning.A_zimbraPrefIMFlashIcon, false);
    }

    /**
     * Flash title bar when a new IM arrives
     *
     * @return zimbraPrefIMFlashTitle, or false if unset
     */
    @ZAttr(id=679)
    public boolean isPrefIMFlashTitle() {
        return getBooleanAttr(Provisioning.A_zimbraPrefIMFlashTitle, false);
    }

    /**
     * whether to hide IM blocked buddies
     *
     * @return zimbraPrefIMHideBlockedBuddies, or false if unset
     */
    @ZAttr(id=707)
    public boolean isPrefIMHideBlockedBuddies() {
        return getBooleanAttr(Provisioning.A_zimbraPrefIMHideBlockedBuddies, false);
    }

    /**
     * whether to hide IM offline buddies
     *
     * @return zimbraPrefIMHideOfflineBuddies, or false if unset
     */
    @ZAttr(id=706)
    public boolean isPrefIMHideOfflineBuddies() {
        return getBooleanAttr(Provisioning.A_zimbraPrefIMHideOfflineBuddies, false);
    }

    /**
     * IM idle status
     *
     * <p>Valid values: [away, xa, offline, invisible]
     *
     * @return zimbraPrefIMIdleStatus, or null unset
     */
    @ZAttr(id=560)
    public String getPrefIMIdleStatus() {
        return getAttr(Provisioning.A_zimbraPrefIMIdleStatus);
    }

    /**
     * IM session idle timeout in minutes
     *
     * @return zimbraPrefIMIdleTimeout, or -1 if unset
     */
    @ZAttr(id=559)
    public int getPrefIMIdleTimeout() {
        return getIntAttr(Provisioning.A_zimbraPrefIMIdleTimeout, -1);
    }

    /**
     * Enable instant notifications
     *
     * @return zimbraPrefIMInstantNotify, or false if unset
     */
    @ZAttr(id=517)
    public boolean isPrefIMInstantNotify() {
        return getBooleanAttr(Provisioning.A_zimbraPrefIMInstantNotify, false);
    }

    /**
     * whether to log IM chats to the Chats folder
     *
     * @return zimbraPrefIMLogChats, or false if unset
     */
    @ZAttr(id=556)
    public boolean isPrefIMLogChats() {
        return getBooleanAttr(Provisioning.A_zimbraPrefIMLogChats, false);
    }

    /**
     * whether IM log chats is enabled
     *
     * @return zimbraPrefIMLogChatsEnabled, or false if unset
     */
    @ZAttr(id=552)
    public boolean isPrefIMLogChatsEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPrefIMLogChatsEnabled, false);
    }

    /**
     * Notify for presence modifications
     *
     * @return zimbraPrefIMNotifyPresence, or false if unset
     */
    @ZAttr(id=463)
    public boolean isPrefIMNotifyPresence() {
        return getBooleanAttr(Provisioning.A_zimbraPrefIMNotifyPresence, false);
    }

    /**
     * Notify for status change
     *
     * @return zimbraPrefIMNotifyStatus, or false if unset
     */
    @ZAttr(id=464)
    public boolean isPrefIMNotifyStatus() {
        return getBooleanAttr(Provisioning.A_zimbraPrefIMNotifyStatus, false);
    }

    /**
     * whether to report IM idle status
     *
     * @return zimbraPrefIMReportIdle, or false if unset
     */
    @ZAttr(id=558)
    public boolean isPrefIMReportIdle() {
        return getBooleanAttr(Provisioning.A_zimbraPrefIMReportIdle, false);
    }

    /**
     * whether sounds is enabled in IM
     *
     * @return zimbraPrefIMSoundsEnabled, or false if unset
     */
    @ZAttr(id=570)
    public boolean isPrefIMSoundsEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPrefIMSoundsEnabled, false);
    }

    /**
     * whether or not the IMAP server exports search folders
     *
     * @return zimbraPrefImapSearchFoldersEnabled, or false if unset
     */
    @ZAttr(id=241)
    public boolean isPrefImapSearchFoldersEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPrefImapSearchFoldersEnabled, false);
    }

    /**
     * Retention period of read messages in the Inbox folder. 0 means that
     * all messages will be retained.
     *
     * <p>Use getPrefInboxReadLifetimeAsString to access value as a string.
     *
     * @see #getPrefInboxReadLifetimeAsString()
     *
     * @return zimbraPrefInboxReadLifetime in millseconds, or -1 if unset
     */
    @ZAttr(id=538)
    public long getPrefInboxReadLifetime() {
        return getTimeInterval(Provisioning.A_zimbraPrefInboxReadLifetime, -1);
    }

    /**
     * Retention period of read messages in the Inbox folder. 0 means that
     * all messages will be retained.
     *
     * @return zimbraPrefInboxReadLifetime, or null unset
     */
    @ZAttr(id=538)
    public String getPrefInboxReadLifetimeAsString() {
        return getAttr(Provisioning.A_zimbraPrefInboxReadLifetime);
    }

    /**
     * Retention period of unread messages in the Inbox folder. 0 means that
     * all messages will be retained.
     *
     * <p>Use getPrefInboxUnreadLifetimeAsString to access value as a string.
     *
     * @see #getPrefInboxUnreadLifetimeAsString()
     *
     * @return zimbraPrefInboxUnreadLifetime in millseconds, or -1 if unset
     */
    @ZAttr(id=537)
    public long getPrefInboxUnreadLifetime() {
        return getTimeInterval(Provisioning.A_zimbraPrefInboxUnreadLifetime, -1);
    }

    /**
     * Retention period of unread messages in the Inbox folder. 0 means that
     * all messages will be retained.
     *
     * @return zimbraPrefInboxUnreadLifetime, or null unset
     */
    @ZAttr(id=537)
    public String getPrefInboxUnreadLifetimeAsString() {
        return getAttr(Provisioning.A_zimbraPrefInboxUnreadLifetime);
    }

    /**
     * whether or not to include spam in search by default
     *
     * @return zimbraPrefIncludeSpamInSearch, or false if unset
     */
    @ZAttr(id=55)
    public boolean isPrefIncludeSpamInSearch() {
        return getBooleanAttr(Provisioning.A_zimbraPrefIncludeSpamInSearch, false);
    }

    /**
     * whether or not to include trash in search by default
     *
     * @return zimbraPrefIncludeTrashInSearch, or false if unset
     */
    @ZAttr(id=56)
    public boolean isPrefIncludeTrashInSearch() {
        return getBooleanAttr(Provisioning.A_zimbraPrefIncludeTrashInSearch, false);
    }

    /**
     * Retention period of messages in the Junk folder. 0 means that all
     * messages will be retained. This user-modifiable attribute works in
     * conjunction with zimbraMailSpamLifetime, which is admin-modifiable.
     * The shorter duration is used.
     *
     * <p>Use getPrefJunkLifetimeAsString to access value as a string.
     *
     * @see #getPrefJunkLifetimeAsString()
     *
     * @return zimbraPrefJunkLifetime in millseconds, or -1 if unset
     */
    @ZAttr(id=540)
    public long getPrefJunkLifetime() {
        return getTimeInterval(Provisioning.A_zimbraPrefJunkLifetime, -1);
    }

    /**
     * Retention period of messages in the Junk folder. 0 means that all
     * messages will be retained. This user-modifiable attribute works in
     * conjunction with zimbraMailSpamLifetime, which is admin-modifiable.
     * The shorter duration is used.
     *
     * @return zimbraPrefJunkLifetime, or null unset
     */
    @ZAttr(id=540)
    public String getPrefJunkLifetimeAsString() {
        return getAttr(Provisioning.A_zimbraPrefJunkLifetime);
    }

    /**
     * list view columns in web client
     *
     * @return zimbraPrefListViewColumns, or null unset
     */
    @ZAttr(id=694)
    public String getPrefListViewColumns() {
        return getAttr(Provisioning.A_zimbraPrefListViewColumns);
    }

    /**
     * user locale preference, e.g. en_US Whenever the server looks for the
     * user locale, it will first look for zimbraPrefLocale, if it is not set
     * then it will fallback to the current mechanism of looking for
     * zimbraLocale in the various places for a user. zimbraLocale is the non
     * end-user attribute that specifies which locale an object defaults to,
     * it is not an end-user setting.
     *
     * @return zimbraPrefLocale, or null unset
     */
    @ZAttr(id=442)
    public String getPrefLocale() {
        return getAttr(Provisioning.A_zimbraPrefLocale);
    }

    /**
     * Default Charset for mail composing and parsing text
     *
     * @return zimbraPrefMailDefaultCharset, or null unset
     */
    @ZAttr(id=469)
    public String getPrefMailDefaultCharset() {
        return getAttr(Provisioning.A_zimbraPrefMailDefaultCharset);
    }

    /**
     * Flash icon when a new email arrives
     *
     * @return zimbraPrefMailFlashIcon, or false if unset
     */
    @ZAttr(id=681)
    public boolean isPrefMailFlashIcon() {
        return getBooleanAttr(Provisioning.A_zimbraPrefMailFlashIcon, false);
    }

    /**
     * Flash title bar when a new email arrives
     *
     * @return zimbraPrefMailFlashTitle, or false if unset
     */
    @ZAttr(id=680)
    public boolean isPrefMailFlashTitle() {
        return getBooleanAttr(Provisioning.A_zimbraPrefMailFlashTitle, false);
    }

    /**
     * initial search done by dhtml client
     *
     * @return zimbraPrefMailInitialSearch, or null unset
     */
    @ZAttr(id=102)
    public String getPrefMailInitialSearch() {
        return getAttr(Provisioning.A_zimbraPrefMailInitialSearch);
    }

    /**
     * number of messages/conversations per page
     *
     * @return zimbraPrefMailItemsPerPage, or -1 if unset
     */
    @ZAttr(id=57)
    public int getPrefMailItemsPerPage() {
        return getIntAttr(Provisioning.A_zimbraPrefMailItemsPerPage, -1);
    }

    /**
     * interval at which the web client polls the server for new messages
     * (nnnnn[hmsd])
     *
     * <p>Use getPrefMailPollingIntervalAsString to access value as a string.
     *
     * @see #getPrefMailPollingIntervalAsString()
     *
     * @return zimbraPrefMailPollingInterval in millseconds, or -1 if unset
     */
    @ZAttr(id=111)
    public long getPrefMailPollingInterval() {
        return getTimeInterval(Provisioning.A_zimbraPrefMailPollingInterval, -1);
    }

    /**
     * interval at which the web client polls the server for new messages
     * (nnnnn[hmsd])
     *
     * @return zimbraPrefMailPollingInterval, or null unset
     */
    @ZAttr(id=111)
    public String getPrefMailPollingIntervalAsString() {
        return getAttr(Provisioning.A_zimbraPrefMailPollingInterval);
    }

    /**
     * mail signature style outlook|internet (deprecatedSince 5.0 in
     * identity)
     *
     * <p>Valid values: [outlook, internet]
     *
     * @return zimbraPrefMailSignatureStyle, or null unset
     */
    @ZAttr(id=156)
    public String getPrefMailSignatureStyle() {
        return getAttr(Provisioning.A_zimbraPrefMailSignatureStyle);
    }

    /**
     * whether audible alert is enabled when a new email arrives
     *
     * @return zimbraPrefMailSoundsEnabled, or false if unset
     */
    @ZAttr(id=666)
    public boolean isPrefMailSoundsEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPrefMailSoundsEnabled, false);
    }

    /**
     * whether mandatory spell check is enabled
     *
     * @return zimbraPrefMandatorySpellCheckEnabled, or false if unset
     *
     * @since ZCS future
     */
    @ZAttr(id=749)
    public boolean isPrefMandatorySpellCheckEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPrefMandatorySpellCheckEnabled, false);
    }

    /**
     * whether and mark a message as read -1: Do not mark read 0: Mark read
     * 1..n: Mark read after this many seconds
     *
     * @return zimbraPrefMarkMsgRead, or -1 if unset
     */
    @ZAttr(id=650)
    public int getPrefMarkMsgRead() {
        return getIntAttr(Provisioning.A_zimbraPrefMarkMsgRead, -1);
    }

    /**
     * whether client prefers text/html or text/plain
     *
     * @return zimbraPrefMessageViewHtmlPreferred, or false if unset
     */
    @ZAttr(id=145)
    public boolean isPrefMessageViewHtmlPreferred() {
        return getBooleanAttr(Provisioning.A_zimbraPrefMessageViewHtmlPreferred, false);
    }

    /**
     * whether or not the client opens a new msg/conv in a new window (via
     * dbl-click)
     *
     * @return zimbraPrefOpenMailInNewWindow, or false if unset
     */
    @ZAttr(id=500)
    public boolean isPrefOpenMailInNewWindow() {
        return getBooleanAttr(Provisioning.A_zimbraPrefOpenMailInNewWindow, false);
    }

    /**
     * server remembers addresses to which notifications have been sent for
     * this interval, and does not send duplicate notifications in this
     * interval
     *
     * <p>Use getPrefOutOfOfficeCacheDurationAsString to access value as a string.
     *
     * @see #getPrefOutOfOfficeCacheDurationAsString()
     *
     * @return zimbraPrefOutOfOfficeCacheDuration in millseconds, or -1 if unset
     */
    @ZAttr(id=386)
    public long getPrefOutOfOfficeCacheDuration() {
        return getTimeInterval(Provisioning.A_zimbraPrefOutOfOfficeCacheDuration, -1);
    }

    /**
     * server remembers addresses to which notifications have been sent for
     * this interval, and does not send duplicate notifications in this
     * interval
     *
     * @return zimbraPrefOutOfOfficeCacheDuration, or null unset
     */
    @ZAttr(id=386)
    public String getPrefOutOfOfficeCacheDurationAsString() {
        return getAttr(Provisioning.A_zimbraPrefOutOfOfficeCacheDuration);
    }

    /**
     * download pop3 messages since
     *
     * <p>Use getPrefPop3DownloadSinceAsString to access value as a string.
     *
     * @see #getPrefPop3DownloadSinceAsString()
     *
     * @return zimbraPrefPop3DownloadSince as Date, null if unset or unable to parse
     */
    @ZAttr(id=653)
    public Date getPrefPop3DownloadSince() {
        return getGeneralizedTimeAttr(Provisioning.A_zimbraPrefPop3DownloadSince, null);
    }

    /**
     * download pop3 messages since
     *
     * @return zimbraPrefPop3DownloadSince, or null unset
     */
    @ZAttr(id=653)
    public String getPrefPop3DownloadSinceAsString() {
        return getAttr(Provisioning.A_zimbraPrefPop3DownloadSince);
    }

    /**
     * whether reading pane is shown by default
     *
     * @return zimbraPrefReadingPaneEnabled, or false if unset
     */
    @ZAttr(id=394)
    public boolean isPrefReadingPaneEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPrefReadingPaneEnabled, false);
    }

    /**
     * what part of the original message to include during replies
     * (deprecatedSince 5.0 in identity)
     *
     * <p>Valid values: [includeAsAttachment, includeNone, includeBodyAndHeadersWithPrefix, includeSmart, includeBody, includeBodyWithPrefix]
     *
     * @return zimbraPrefReplyIncludeOriginalText, or null unset
     */
    @ZAttr(id=133)
    public String getPrefReplyIncludeOriginalText() {
        return getAttr(Provisioning.A_zimbraPrefReplyIncludeOriginalText);
    }

    /**
     * whether or not to save outgoing mail (deprecatedSince 5.0 in identity)
     *
     * @return zimbraPrefSaveToSent, or false if unset
     */
    @ZAttr(id=22)
    public boolean isPrefSaveToSent() {
        return getBooleanAttr(Provisioning.A_zimbraPrefSaveToSent, false);
    }

    /**
     * whether or not search tree is expanded
     *
     * @return zimbraPrefSearchTreeOpen, or false if unset
     */
    @ZAttr(id=634)
    public boolean isPrefSearchTreeOpen() {
        return getBooleanAttr(Provisioning.A_zimbraPrefSearchTreeOpen, false);
    }

    /**
     * Retention period of messages in the Sent folder. 0 means that all
     * messages will be retained.
     *
     * <p>Use getPrefSentLifetimeAsString to access value as a string.
     *
     * @see #getPrefSentLifetimeAsString()
     *
     * @return zimbraPrefSentLifetime in millseconds, or -1 if unset
     */
    @ZAttr(id=539)
    public long getPrefSentLifetime() {
        return getTimeInterval(Provisioning.A_zimbraPrefSentLifetime, -1);
    }

    /**
     * Retention period of messages in the Sent folder. 0 means that all
     * messages will be retained.
     *
     * @return zimbraPrefSentLifetime, or null unset
     */
    @ZAttr(id=539)
    public String getPrefSentLifetimeAsString() {
        return getAttr(Provisioning.A_zimbraPrefSentLifetime);
    }

    /**
     * name of folder to save sent mail in (deprecatedSince 5.0 in identity)
     *
     * @return zimbraPrefSentMailFolder, or null unset
     */
    @ZAttr(id=103)
    public String getPrefSentMailFolder() {
        return getAttr(Provisioning.A_zimbraPrefSentMailFolder);
    }

    /**
     * keyboard shortcuts
     *
     * @return zimbraPrefShortcuts, or null unset
     */
    @ZAttr(id=396)
    public String getPrefShortcuts() {
        return getAttr(Provisioning.A_zimbraPrefShortcuts);
    }

    /**
     * show fragments in conversation and message lists
     *
     * @return zimbraPrefShowFragments, or false if unset
     */
    @ZAttr(id=192)
    public boolean isPrefShowFragments() {
        return getBooleanAttr(Provisioning.A_zimbraPrefShowFragments, false);
    }

    /**
     * whether to show search box or not
     *
     * @return zimbraPrefShowSearchString, or false if unset
     */
    @ZAttr(id=222)
    public boolean isPrefShowSearchString() {
        return getBooleanAttr(Provisioning.A_zimbraPrefShowSearchString, false);
    }

    /**
     * show selection checkbox for selecting email, contact, voicemial items
     * in a list view for batch operations
     *
     * @return zimbraPrefShowSelectionCheckbox, or false if unset
     */
    @ZAttr(id=471)
    public boolean isPrefShowSelectionCheckbox() {
        return getBooleanAttr(Provisioning.A_zimbraPrefShowSelectionCheckbox, false);
    }

    /**
     * Skin to use for this account
     *
     * @return zimbraPrefSkin, or null unset
     */
    @ZAttr(id=355)
    public String getPrefSkin() {
        return getAttr(Provisioning.A_zimbraPrefSkin);
    }

    /**
     * whether standard client should operate in accessilbity Mode
     *
     * @return zimbraPrefStandardClientAccessilbityMode, or false if unset
     */
    @ZAttr(id=689)
    public boolean isPrefStandardClientAccessilbityMode() {
        return getBooleanAttr(Provisioning.A_zimbraPrefStandardClientAccessilbityMode, false);
    }

    /**
     * whether or not tag tree is expanded
     *
     * @return zimbraPrefTagTreeOpen, or false if unset
     */
    @ZAttr(id=633)
    public boolean isPrefTagTreeOpen() {
        return getBooleanAttr(Provisioning.A_zimbraPrefTagTreeOpen, false);
    }

    /**
     * time zone of user or COS
     *
     * @return zimbraPrefTimeZoneId, or ampty array if unset
     */
    @ZAttr(id=235)
    public String[] getPrefTimeZoneId() {
        return getMultiAttr(Provisioning.A_zimbraPrefTimeZoneId);
    }

    /**
     * Retention period of messages in the Trash folder. 0 means that all
     * messages will be retained. This user-modifiable attribute works in
     * conjunction with zimbraMailTrashLifetime, which is admin-modifiable.
     * The shorter duration is used.
     *
     * <p>Use getPrefTrashLifetimeAsString to access value as a string.
     *
     * @see #getPrefTrashLifetimeAsString()
     *
     * @return zimbraPrefTrashLifetime in millseconds, or -1 if unset
     */
    @ZAttr(id=541)
    public long getPrefTrashLifetime() {
        return getTimeInterval(Provisioning.A_zimbraPrefTrashLifetime, -1);
    }

    /**
     * Retention period of messages in the Trash folder. 0 means that all
     * messages will be retained. This user-modifiable attribute works in
     * conjunction with zimbraMailTrashLifetime, which is admin-modifiable.
     * The shorter duration is used.
     *
     * @return zimbraPrefTrashLifetime, or null unset
     */
    @ZAttr(id=541)
    public String getPrefTrashLifetimeAsString() {
        return getAttr(Provisioning.A_zimbraPrefTrashLifetime);
    }

    /**
     * whether or not keyboard shortcuts are enabled
     *
     * @return zimbraPrefUseKeyboardShortcuts, or false if unset
     */
    @ZAttr(id=61)
    public boolean isPrefUseKeyboardShortcuts() {
        return getBooleanAttr(Provisioning.A_zimbraPrefUseKeyboardShortcuts, false);
    }

    /**
     * When composing and sending mail, whether to use RFC 2231 MIME
     * parameter value encoding. If set to FALSE, then RFC 2047 style
     * encoding is used.
     *
     * @return zimbraPrefUseRfc2231, or false if unset
     */
    @ZAttr(id=395)
    public boolean isPrefUseRfc2231() {
        return getBooleanAttr(Provisioning.A_zimbraPrefUseRfc2231, false);
    }

    /**
     * whether list of well known time zones is displayed in calendar UI
     *
     * @return zimbraPrefUseTimeZoneListInCalendar, or false if unset
     */
    @ZAttr(id=236)
    public boolean isPrefUseTimeZoneListInCalendar() {
        return getBooleanAttr(Provisioning.A_zimbraPrefUseTimeZoneListInCalendar, false);
    }

    /**
     * number of voice messages/call logs per page
     *
     * @return zimbraPrefVoiceItemsPerPage, or -1 if unset
     */
    @ZAttr(id=526)
    public int getPrefVoiceItemsPerPage() {
        return getIntAttr(Provisioning.A_zimbraPrefVoiceItemsPerPage, -1);
    }

    /**
     * whether to display a warning when users try to navigate away from ZCS
     *
     * @return zimbraPrefWarnOnExit, or false if unset
     */
    @ZAttr(id=456)
    public boolean isPrefWarnOnExit() {
        return getBooleanAttr(Provisioning.A_zimbraPrefWarnOnExit, false);
    }

    /**
     * whether or not zimlet tree is expanded
     *
     * @return zimbraPrefZimletTreeOpen, or false if unset
     */
    @ZAttr(id=638)
    public boolean isPrefZimletTreeOpen() {
        return getBooleanAttr(Provisioning.A_zimbraPrefZimletTreeOpen, false);
    }

    /**
     * Allowed domains for Proxy servlet
     *
     * @return zimbraProxyAllowedDomains, or ampty array if unset
     */
    @ZAttr(id=294)
    public String[] getProxyAllowedDomains() {
        return getMultiAttr(Provisioning.A_zimbraProxyAllowedDomains);
    }

    /**
     * Content types that can be cached by proxy servlet
     *
     * @return zimbraProxyCacheableContentTypes, or ampty array if unset
     */
    @ZAttr(id=303)
    public String[] getProxyCacheableContentTypes() {
        return getMultiAttr(Provisioning.A_zimbraProxyCacheableContentTypes);
    }

    /**
     * Minimum duration of time between quota warnings.
     *
     * <p>Use getQuotaWarnIntervalAsString to access value as a string.
     *
     * @see #getQuotaWarnIntervalAsString()
     *
     * @return zimbraQuotaWarnInterval in millseconds, or -1 if unset
     */
    @ZAttr(id=485)
    public long getQuotaWarnInterval() {
        return getTimeInterval(Provisioning.A_zimbraQuotaWarnInterval, -1);
    }

    /**
     * Minimum duration of time between quota warnings.
     *
     * @return zimbraQuotaWarnInterval, or null unset
     */
    @ZAttr(id=485)
    public String getQuotaWarnIntervalAsString() {
        return getAttr(Provisioning.A_zimbraQuotaWarnInterval);
    }

    /**
     * Quota warning message template.
     *
     * @return zimbraQuotaWarnMessage, or null unset
     */
    @ZAttr(id=486)
    public String getQuotaWarnMessage() {
        return getAttr(Provisioning.A_zimbraQuotaWarnMessage);
    }

    /**
     * Threshold for quota warning messages.
     *
     * @return zimbraQuotaWarnPercent, or -1 if unset
     */
    @ZAttr(id=483)
    public int getQuotaWarnPercent() {
        return getIntAttr(Provisioning.A_zimbraQuotaWarnPercent, -1);
    }

    /**
     * maximum number of signatures allowed on an account
     *
     * @return zimbraSignatureMaxNumEntries, or -1 if unset
     */
    @ZAttr(id=493)
    public int getSignatureMaxNumEntries() {
        return getIntAttr(Provisioning.A_zimbraSignatureMaxNumEntries, -1);
    }

    /**
     * minimum number of signatures allowed on an account, this is only used
     * in the client
     *
     * @return zimbraSignatureMinNumEntries, or -1 if unset
     */
    @ZAttr(id=523)
    public int getSignatureMinNumEntries() {
        return getIntAttr(Provisioning.A_zimbraSignatureMinNumEntries, -1);
    }

    /**
     * If TRUE, spam messages will be affected by user mail filters instead
     * of being automatically filed into the Junk folder. This attribute is
     * deprecated and will be removed in a future release. See bug 23886 for
     * details.
     *
     * @return zimbraSpamApplyUserFilters, or false if unset
     */
    @ZAttr(id=604)
    public boolean isSpamApplyUserFilters() {
        return getBooleanAttr(Provisioning.A_zimbraSpamApplyUserFilters, false);
    }

    /**
     * The maximum batch size for each ZimbraSync transaction. Default value
     * of 0 means to follow client requested size. If set to any positive
     * integer, the value will be the maximum number of items to sync even if
     * client requests more. This setting affects all sync categories
     * including email, contacts, calendar and tasks.
     *
     * @return zimbraSyncWindowSize, or -1 if unset
     */
    @ZAttr(id=437)
    public int getSyncWindowSize() {
        return getIntAttr(Provisioning.A_zimbraSyncWindowSize, -1);
    }

    /**
     * The registered name of the Zimbra Analyzer Extension for this account
     * to use
     *
     * @return zimbraTextAnalyzer, or null unset
     */
    @ZAttr(id=393)
    public String getTextAnalyzer() {
        return getAttr(Provisioning.A_zimbraTextAnalyzer);
    }

    /**
     * List of Zimlets available to this COS
     *
     * @return zimbraZimletAvailableZimlets, or ampty array if unset
     */
    @ZAttr(id=291)
    public String[] getZimletAvailableZimlets() {
        return getMultiAttr(Provisioning.A_zimbraZimletAvailableZimlets);
    }

    ///// END-AUTO-GEN-REPLACE

}
