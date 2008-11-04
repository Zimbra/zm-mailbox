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
public class ZAttrDomain extends NamedEntry {

    public ZAttrDomain(String name, String id, Map<String, Object> attrs, Map<String, Object> defaults) {
        super(name, id, attrs, defaults);

    }

    ///// BEGIN-AUTO-GEN-REPLACE

    /* build: 5.0 schemers 20081104-1137 */

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
     * fallback to local auth if external mech fails
     *
     * @return zimbraAuthFallbackToLocal, or false if unset
     */
    @ZAttr(id=257)
    public boolean isAuthFallbackToLocal() {
        return getBooleanAttr(Provisioning.A_zimbraAuthFallbackToLocal, false);
    }

    /**
     * kerberos5 realm for kerberos5 auth mech
     *
     * @return zimbraAuthKerberos5Realm, or null unset
     */
    @ZAttr(id=548)
    public String getAuthKerberos5Realm() {
        return getAttr(Provisioning.A_zimbraAuthKerberos5Realm);
    }

    /**
     * LDAP bind dn for ldap auth mech
     *
     * @return zimbraAuthLdapBindDn, or null unset
     */
    @ZAttr(id=44)
    public String getAuthLdapBindDn() {
        return getAttr(Provisioning.A_zimbraAuthLdapBindDn);
    }

    /**
     * LDAP search base for ldap auth mech
     *
     * @return zimbraAuthLdapSearchBase, or null unset
     */
    @ZAttr(id=252)
    public String getAuthLdapSearchBase() {
        return getAttr(Provisioning.A_zimbraAuthLdapSearchBase);
    }

    /**
     * LDAP search bind dn for ldap auth mech
     *
     * @return zimbraAuthLdapSearchBindDn, or null unset
     */
    @ZAttr(id=253)
    public String getAuthLdapSearchBindDn() {
        return getAttr(Provisioning.A_zimbraAuthLdapSearchBindDn);
    }

    /**
     * LDAP search bind password for ldap auth mech
     *
     * @return zimbraAuthLdapSearchBindPassword, or null unset
     */
    @ZAttr(id=254)
    public String getAuthLdapSearchBindPassword() {
        return getAttr(Provisioning.A_zimbraAuthLdapSearchBindPassword);
    }

    /**
     * LDAP search filter for ldap auth mech
     *
     * @return zimbraAuthLdapSearchFilter, or null unset
     */
    @ZAttr(id=255)
    public String getAuthLdapSearchFilter() {
        return getAttr(Provisioning.A_zimbraAuthLdapSearchFilter);
    }

    /**
     * whether to use startTLS for external LDAP auth
     *
     * @return zimbraAuthLdapStartTlsEnabled, or false if unset
     */
    @ZAttr(id=654)
    public boolean isAuthLdapStartTlsEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraAuthLdapStartTlsEnabled, false);
    }

    /**
     * LDAP URL for ldap auth mech
     *
     * @return zimbraAuthLdapURL, or ampty array if unset
     */
    @ZAttr(id=43)
    public String[] getAuthLdapURL() {
        return getMultiAttr(Provisioning.A_zimbraAuthLdapURL);
    }

    /**
     * mechanism to use for authentication. Valid values are zimbra, ldap,
     * ad, kerberos5, custom:{handler-name} [arg1 arg2 ...]
     *
     * @return zimbraAuthMech, or null unset
     */
    @ZAttr(id=42)
    public String getAuthMech() {
        return getAttr(Provisioning.A_zimbraAuthMech);
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
     * maximum number of accounts allowed to be assigned to specified COSes
     * in a domain
     *
     * @return zimbraDomainCOSMaxAccounts, or ampty array if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=714)
    public String[] getDomainCOSMaxAccounts() {
        return getMultiAttr(Provisioning.A_zimbraDomainCOSMaxAccounts);
    }

    /**
     * COS zimbraID
     *
     * @return zimbraDomainDefaultCOSId, or null unset
     */
    @ZAttr(id=299)
    public String getDomainDefaultCOSId() {
        return getAttr(Provisioning.A_zimbraDomainDefaultCOSId);
    }

    /**
     * maximum number of accounts allowed to have specified features in a
     * domain
     *
     * @return zimbraDomainFeatureMaxAccounts, or ampty array if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=715)
    public String[] getDomainFeatureMaxAccounts() {
        return getMultiAttr(Provisioning.A_zimbraDomainFeatureMaxAccounts);
    }

    /**
     * maximum number of accounts allowed in a domain
     *
     * @return zimbraDomainMaxAccounts, or -1 if unset
     */
    @ZAttr(id=400)
    public int getDomainMaxAccounts() {
        return getIntAttr(Provisioning.A_zimbraDomainMaxAccounts, -1);
    }

    /**
     * name of the domain
     *
     * @return zimbraDomainName, or null unset
     */
    @ZAttr(id=19)
    public String getDomainName() {
        return getAttr(Provisioning.A_zimbraDomainName);
    }

    /**
     * domain rename info/status
     *
     * @return zimbraDomainRenameInfo, or null unset
     */
    @ZAttr(id=536)
    public String getDomainRenameInfo() {
        return getAttr(Provisioning.A_zimbraDomainRenameInfo);
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
     * should be one of: local, alias
     *
     * <p>Valid values: [local, alias]
     *
     * @return zimbraDomainType, or null unset
     */
    @ZAttr(id=212)
    public String getDomainType() {
        return getAttr(Provisioning.A_zimbraDomainType);
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
     * external LDAP GAL authentication mechanism none: anonymous binding
     * simple: zimbraGalLdapBindDn and zimbraGalLdapBindPassword has to be
     * set kerberos5: zimbraGalLdapKerberos5Principal and
     * zimbraGalLdapKerberos5Keytab has to be set
     *
     * <p>Valid values: [kerberos5, none, simple]
     *
     * @return zimbraGalLdapAuthMech, or null unset
     */
    @ZAttr(id=549)
    public String getGalLdapAuthMech() {
        return getAttr(Provisioning.A_zimbraGalLdapAuthMech);
    }

    /**
     * LDAP bind dn for external GAL queries
     *
     * @return zimbraGalLdapBindDn, or null unset
     */
    @ZAttr(id=49)
    public String getGalLdapBindDn() {
        return getAttr(Provisioning.A_zimbraGalLdapBindDn);
    }

    /**
     * LDAP bind password for external GAL queries
     *
     * @return zimbraGalLdapBindPassword, or null unset
     */
    @ZAttr(id=50)
    public String getGalLdapBindPassword() {
        return getAttr(Provisioning.A_zimbraGalLdapBindPassword);
    }

    /**
     * LDAP search filter for external GAL search queries
     *
     * @return zimbraGalLdapFilter, or null unset
     */
    @ZAttr(id=51)
    public String getGalLdapFilter() {
        return getAttr(Provisioning.A_zimbraGalLdapFilter);
    }

    /**
     * kerberos5 keytab file path for external GAL queries
     *
     * @return zimbraGalLdapKerberos5Keytab, or null unset
     */
    @ZAttr(id=551)
    public String getGalLdapKerberos5Keytab() {
        return getAttr(Provisioning.A_zimbraGalLdapKerberos5Keytab);
    }

    /**
     * kerberos5 principal for external GAL queries
     *
     * @return zimbraGalLdapKerberos5Principal, or null unset
     */
    @ZAttr(id=550)
    public String getGalLdapKerberos5Principal() {
        return getAttr(Provisioning.A_zimbraGalLdapKerberos5Principal);
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
     * LDAP search base for external GAL queries
     *
     * @return zimbraGalLdapSearchBase, or null unset
     */
    @ZAttr(id=48)
    public String getGalLdapSearchBase() {
        return getAttr(Provisioning.A_zimbraGalLdapSearchBase);
    }

    /**
     * whether to use startTLS for external GAL. startTLS will be used for
     * external GAL access only if this attribute is true and
     * zimbraGalLdapURL(or zimbraGalSyncLdapURL for sync) does not contain a
     * ldaps URL.
     *
     * @return zimbraGalLdapStartTlsEnabled, or false if unset
     */
    @ZAttr(id=655)
    public boolean isGalLdapStartTlsEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraGalLdapStartTlsEnabled, false);
    }

    /**
     * LDAP URL for external GAL queries
     *
     * @return zimbraGalLdapURL, or ampty array if unset
     */
    @ZAttr(id=47)
    public String[] getGalLdapURL() {
        return getMultiAttr(Provisioning.A_zimbraGalLdapURL);
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
     * should be internal (query internal only), external (external only), or
     * both
     *
     * <p>Valid values: [both, ldap, zimbra]
     *
     * @return zimbraGalMode, or null unset
     */
    @ZAttr(id=46)
    public String getGalMode() {
        return getAttr(Provisioning.A_zimbraGalMode);
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
     * external LDAP GAL authentication mechanism for GAL sync none:
     * anonymous binding simple: zimbraGalLdapBindDn and
     * zimbraGalLdapBindPassword has to be set kerberos5:
     * zimbraGalLdapKerberos5Principal and zimbraGalLdapKerberos5Keytab has
     * to be set if not set fallback to zimbraGalLdapAuthMech
     *
     * <p>Valid values: [kerberos5, none, simple]
     *
     * @return zimbraGalSyncLdapAuthMech, or null unset
     */
    @ZAttr(id=592)
    public String getGalSyncLdapAuthMech() {
        return getAttr(Provisioning.A_zimbraGalSyncLdapAuthMech);
    }

    /**
     * LDAP bind dn for external GAL sync queries, if not set fallback to
     * zimbraGalLdapBindDn
     *
     * @return zimbraGalSyncLdapBindDn, or null unset
     */
    @ZAttr(id=593)
    public String getGalSyncLdapBindDn() {
        return getAttr(Provisioning.A_zimbraGalSyncLdapBindDn);
    }

    /**
     * LDAP bind password for external GAL sync queries, if not set fallback
     * to zimbraGalLdapBindPassword
     *
     * @return zimbraGalSyncLdapBindPassword, or null unset
     */
    @ZAttr(id=594)
    public String getGalSyncLdapBindPassword() {
        return getAttr(Provisioning.A_zimbraGalSyncLdapBindPassword);
    }

    /**
     * LDAP search filter for external GAL sync queries, if not set fallback
     * to zimbraGalLdapFilter
     *
     * @return zimbraGalSyncLdapFilter, or null unset
     */
    @ZAttr(id=591)
    public String getGalSyncLdapFilter() {
        return getAttr(Provisioning.A_zimbraGalSyncLdapFilter);
    }

    /**
     * kerberos5 keytab file path for external GAL sync queries, if not set
     * fallback to zimbraGalLdapKerberos5Keytab
     *
     * @return zimbraGalSyncLdapKerberos5Keytab, or null unset
     */
    @ZAttr(id=596)
    public String getGalSyncLdapKerberos5Keytab() {
        return getAttr(Provisioning.A_zimbraGalSyncLdapKerberos5Keytab);
    }

    /**
     * kerberos5 principal for external GAL sync queries, if not set fallback
     * to zimbraGalLdapKerberos5Principal
     *
     * @return zimbraGalSyncLdapKerberos5Principal, or null unset
     */
    @ZAttr(id=595)
    public String getGalSyncLdapKerberos5Principal() {
        return getAttr(Provisioning.A_zimbraGalSyncLdapKerberos5Principal);
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
     * LDAP search base for external GAL sync queries, if not set fallback to
     * zimbraGalLdapSearchBase
     *
     * @return zimbraGalSyncLdapSearchBase, or null unset
     */
    @ZAttr(id=590)
    public String getGalSyncLdapSearchBase() {
        return getAttr(Provisioning.A_zimbraGalSyncLdapSearchBase);
    }

    /**
     * whether to use startTLS for external GAL sync, if not set fallback to
     * zimbraGalLdapStartTlsEnabled
     *
     * @return zimbraGalSyncLdapStartTlsEnabled, or false if unset
     */
    @ZAttr(id=656)
    public boolean isGalSyncLdapStartTlsEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraGalSyncLdapStartTlsEnabled, false);
    }

    /**
     * LDAP URL for external GAL sync, if not set fallback to
     * zimbraGalLdapURL
     *
     * @return zimbraGalSyncLdapURL, or ampty array if unset
     */
    @ZAttr(id=589)
    public String[] getGalSyncLdapURL() {
        return getMultiAttr(Provisioning.A_zimbraGalSyncLdapURL);
    }

    /**
     * whether to tokenize key and AND or OR the tokenized queries for GAL
     * auto complete, if not set, key is not tokenized
     *
     * <p>Valid values: [and, or]
     *
     * @return zimbraGalTokenizeAutoCompleteKey, or null unset
     */
    @ZAttr(id=599)
    public String getGalTokenizeAutoCompleteKey() {
        return getAttr(Provisioning.A_zimbraGalTokenizeAutoCompleteKey);
    }

    /**
     * whether to tokenize key and AND or OR the tokenized queries for GAL
     * search, if not set, key is not tokenized
     *
     * <p>Valid values: [and, or]
     *
     * @return zimbraGalTokenizeSearchKey, or null unset
     */
    @ZAttr(id=600)
    public String getGalTokenizeSearchKey() {
        return getAttr(Provisioning.A_zimbraGalTokenizeSearchKey);
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
     * Zimbra Systems Unique ID
     *
     * @return zimbraId, or null unset
     */
    @ZAttr(id=1)
    public String getId() {
        return getAttr(Provisioning.A_zimbraId);
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
     * Account for storing templates and providing space for public wiki
     *
     * @return zimbraNotebookAccount, or null unset
     */
    @ZAttr(id=363)
    public String getNotebookAccount() {
        return getAttr(Provisioning.A_zimbraNotebookAccount);
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
     * registered change password listener name
     *
     * @return zimbraPasswordChangeListener, or null unset
     */
    @ZAttr(id=586)
    public String getPasswordChangeListener() {
        return getAttr(Provisioning.A_zimbraPasswordChangeListener);
    }

    /**
     * preauth secret key
     *
     * @return zimbraPreAuthKey, or null unset
     */
    @ZAttr(id=307)
    public String getPreAuthKey() {
        return getAttr(Provisioning.A_zimbraPreAuthKey);
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
     * time zone of user or COS
     *
     * @return zimbraPrefTimeZoneId, or ampty array if unset
     */
    @ZAttr(id=235)
    public String[] getPrefTimeZoneId() {
        return getMultiAttr(Provisioning.A_zimbraPrefTimeZoneId);
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
     * An alias for this domain, used to determine default login domain based
     * on URL client is visiting
     *
     * @return zimbraVirtualHostname, or ampty array if unset
     */
    @ZAttr(id=352)
    public String[] getVirtualHostname() {
        return getMultiAttr(Provisioning.A_zimbraVirtualHostname);
    }

    /**
     * An virtual IP address for this domain, used to determine domain based
     * on an IP address
     *
     * @return zimbraVirtualIPAddress, or ampty array if unset
     */
    @ZAttr(id=562)
    public String[] getVirtualIPAddress() {
        return getMultiAttr(Provisioning.A_zimbraVirtualIPAddress);
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
