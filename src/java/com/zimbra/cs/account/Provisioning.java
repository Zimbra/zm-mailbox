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
 */
package com.zimbra.cs.account;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.mime.MimeTypeInfo;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.L10nUtil;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.AddressException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * @author schemers
 *
 */
public abstract class Provisioning {

    
    public final static String TRUE  = "TRUE";

    public final static String FALSE = "FALSE";

    public static final String DEFAULT_COS_NAME = "default";

    /**
     * generate appts that try to be compatible with exchange
     */
    public static final String CAL_MODE_EXCHANGE = "exchange";

    /**
     * generate appts that try to follow the standard
     */    
    public static final String CAL_MODE_STANDARD = "standard";
    
    /**
     * only use internal
     */
    public static final String GM_ZIMBRA = "zimbra";
    
    /**
     * only use exteranl gal
     */    
    public static final String GM_LDAP = "ldap";
    
    /**
     * use both gals (combine results)
     */
    public static final String GM_BOTH = "both";

    /**
     * zimbraAuthMech type of "zimbra" means our own (use userPassword)
     */
    public static final String AM_ZIMBRA = "zimbra";
    
    /**
     * zimbraAuthMech type of "ldap" means use configured LDAP attrs
     * (zimbraAuthLdapURL, zimbraAuthLdapBindDn)
     */
    public static final String AM_LDAP = "ldap";
    
    /**
     * zimbraAuthMech type of "ad" means use configured LDAP attrs
     * (zimbraAuthLdapURL, zimbraAuthLdapBindDn) for use with ActiveDirectory
     */
    public static final String AM_AD = "ad";
    
    /**
     * zimbraAuthMech type of "kerberos5" means use kerberos5 authentication.
     * The principal can be obtained by, either:
     * (1) {email-local-part}@{domain-attr-zimbraAuthKerberos5Realm}
     * or
     * (2) {principal-name} if account zimbraForeignPrincipal is in the format of 
     *     kerberos5:{principal-name}
     */
    public static final String AM_KERBEROS5 = "kerberos5";
    
    /**
     * zimbraAuthMech type of "custom:{handler}" means use registered extension
     * of ZimbraCustomAuth.authenticate() method 
     * see customauth.txt
     */
    public static final String AM_CUSTOM = "custom:";  
    
    /**
     * For kerberos5 auth, we allow specifying a principal on a per-account basis.
     * If zimbraForeignPrincipal is in the format of kerberos5:{principal-name}, we 
     * use the {principal-name} instead of {user-part}@{kerberos5-realm-of-the-domain}
     */
    public static final String FP_PREFIX_KERBEROS5 = "kerberos5:";  
    
    /**
     * the account is active, and allows logins, etc.
     */
    public static final String ACCOUNT_STATUS_ACTIVE = "active";

    /**
     * the account is in maintenance mode, and shouldn't allow logins and/or access
     * to account data. Maintenance mode is a temporary state.
     */
    public static final String ACCOUNT_STATUS_MAINTENANCE = "maintenance";
    
    /**
     * the account is locked due to administrative reasons and shouldn't allow logins and/or access.
     */
    public static final String ACCOUNT_STATUS_LOCKED = "locked";
    
    /**
     * the account is locked due to too many login failure attempts and shouldn't allow  logins and/or access.
     */
    public static final String ACCOUNT_STATUS_LOCKOUT = "lockout";

    /**
     * the account is marked closed (future garbage collection) and shouldn't allow logins and/or access.
     */
    public static final String ACCOUNT_STATUS_CLOSED = "closed";
    
    /**
     * mail sent to this account's primary email or aliases should be accepted.
     */
    public static final String MAIL_STATUS_ENABLED = "enabled";

    /**
     * mail sent to this account's primary email or aliases should be bounced. An account
     * in maintenance mode will still have its mail status enabled, as this status causes
     * the addresses to appear as "non-existant" to the MTA.
     */
    public static final String MAIL_STATUS_DISABLED = "disabled";
    
    public static final String DOMAIN_STATUS_ACTIVE = "active";
    public static final String DOMAIN_STATUS_MAINTENANCE = "maintenance";
    public static final String DOMAIN_STATUS_LOCKED = "locked";
    public static final String DOMAIN_STATUS_CLOSED = "closed";
    public static final String DOMAIN_STATUS_SUSPENDED = "suspended";
    public static final String DOMAIN_STATUS_SHUTDOWN = "shutdown";

    /**
     * An alias domain is a domain where ALL addresses in the domain
     * are forwarded to the same local part of the address in another
     * domain.
     */
    public static final String DOMAIN_TYPE_ALIAS = "alias";
    
    /**
     * A local domain is not an alias domain - ie the whole domain is
     * not a forwarding domain, normal mailbox addresses and
     * individually listed aliases exist.
     */
    public static final String DOMAIN_TYPE_LOCAL = "local";
    
    /**
     * Compose mail in text format
     */
    public static final String MAIL_FORMAT_TEXT = "text";

    /**
     * Compose mail in html format
     */
    public static final String MAIL_FORMAT_HTML = "html";
    
    /**
     * Forward/reply mail in the same format of message we are replying to
     */
    public static final String MAIL_FORWARDREPLY_FORMAT_SAME = "same";
    
    /**
     * Possible values for zimbraMailMode. "mixed" means web server should
     * authenticate in HTTPS and redirect to HTTP (useful if all clients are on
     * the intranet and you want only do authentication in the clear - TODO we
     * should add networks to not redirect to at some point which would be sweet -
     * that would mean that if you are in mixed mode and coming from a trusted
     * local network we would redirect you to http after login, but if you came
     * externally you would stay in https - one day we will do this.) "both"
     * says to run both https and http, and not do any redirects between the
     * two.  "redirect" means the web server should listen on both HTTP and HTTPS
     * but redirect traffic on the HTTP port to HTTPS.
     */
    public enum MAIL_MODE { http, https, mixed, both, redirect }

    // attributes

    public static final String A_zimbraServiceEnabled = "zimbraServiceEnabled";

    public static final String A_dc = "dc";
        
    /**
     * aliased object name. The dn that an alias points to
     */
    public static final String A_aliasedObjectName = "aliasedObjectName";
    
    /**
     * Organizational Unit
     */
    public static final String A_ou = "ou";    
    
    /**
     * Organizational Name
     */
    public static final String A_o = "o";
    
    /**
     * The (unique) user id. For example: schemers, smith, etc. Used as the RDN.
     */
    public static final String A_uid = "uid";
    
    /**
     * description
     */
    public static final String A_description = "description";
    
    /**
     * the ldap object class
     */
    public static final String A_objectClass = "objectClass";
    
    /**
     * the primary mail address, used for white-pages compatiblity
     */
    public static final String A_mail = "mail";
    
    /**
     * a person's job title
     */
    public static final String A_title = "title";
    
    /**
     * account's password, stored encoded as SSHA (salted-SHA1)
     */
    public static final String A_userPassword = "userPassword";
    
    /**
     * account's fullname (first last) 
     */
    public static final String A_cn = "cn";
    
    /**
     * account's country name
     */
    public static final String A_co = "co";    
    
    /**
     * account's surname (last name) 
     */
    public static final String A_sn = "sn";
    
    /**
     * account's given name (first name)
     */
    public static final String A_givenName = "givenName";
    public static final String A_gn = "gn";
    
    /**
     * account's company
     */
    public static final String A_company = "company";
    
    /**
     * account's displayName
     */
    public static final String A_displayName = "displayName";
    
    /**
     * account's initials (Middle)
     */
    public static final String A_initials = "initials";
    
    /**
     * account's locality (City)
     */
    public static final String A_l = "l";        
    
    /**
     * account's office
     */
    public static final String A_physicalDeliveryOfficeName = "physicalDeliveryOfficeName";

    /**
     * account's street address
     */
    public static final String A_street = "street";

    
    /**
     * account's zip code
     */
    public static final String A_postalCode = "postalCode";
    
    /**
     * account's state
     */
    public static final String A_st = "st";
    
    /**
     * account's telephone
     */
    public static final String A_telephoneNumber = "telephoneNumber";    

    public static final String A_zimbraAuthTokenKey = "zimbraAuthTokenKey";

    /**
     * auth mech to use. 
     */
    public static final String A_zimbraAuthMech = "zimbraAuthMech";

    public static final String A_zimbraAuthFallbackToLocal = "zimbraAuthFallbackToLocal";
    
    public static final String A_zimbraAuthKerberos5Realm = "zimbraAuthKerberos5Realm";

    /**
     * LDAP URL (ldap://ldapserver[:port]/ or ldaps://ldapserver[:port]/)
     */
    public static final String A_zimbraAuthLdapURL = "zimbraAuthLdapURL";

    public static final String A_zimbraAuthLdapSearchBase = "zimbraAuthLdapSearchBase";

    public static final String A_zimbraAuthLdapSearchBindDn = "zimbraAuthLdapSearchBindDn";

    public static final String A_zimbraAuthLdapSearchBindPassword = "zimbraAuthLdapSearchBindPassword";

    public static final String A_zimbraAuthLdapSearchFilter = "zimbraAuthLdapSearchFilter";

    public static final String A_zimbraAuthLdapExternalDn = "zimbraAuthLdapExternalDn";
    
    public static final String A_zimbraHideInGal = "zimbraHideInGal";

    /**
     *  expansions for bind dn string:
     * 
     * %n = username with @ (or without, if no @ was specified)
     * %u = username with @ removed
     * %d = domain as foo.com
     * %D = domain as dc=foo,dc=com
     * 
     * For active directory, where accounts in our system have a domain of test.example.zimbra.com,
     * and accounts in active directory have example.zimbra.com.
     * 
     * zimbraAuthMech      ldap
     * zimbraAuthLdapURL   ldap://exch1/
     * zimbraAuthLdapDn    %n@example.zimbra.com
     * 
     * configuring our own system to auth via an LDAP bind
     * 
     * zimbraAuthMech       ldap
     * zimbraAuthLdapURL    ldap://dogfood.example.zimbra.com/
     * zimbraAuthLdapUserDn uid=%u,ou=people,%D
     *
     */    
    public static final String A_zimbraAuthLdapBindDn = "zimbraAuthLdapBindDn";    
    
    /**
     * UUID for the entry
     */
    public static final String A_zimbraId = "zimbraId";

    /**
     * pointer to the aliased id
     */
    public static final String A_zimbraAliasTargetId = "zimbraAliasTargetId";    
    
    /**
     * the account's status (see ACCOUNT_STATUS_*). Must be "active" to allow logins.
     */
    public static final String A_zimbraAccountStatus = "zimbraAccountStatus";
    
    /**
     * Object classes to add to a zimbraAccount
     */
    public static final String A_zimbraAccountExtraObjectClass = "zimbraAccountExtraObjectClass";

    
    public static final String A_zimbraLocale = "zimbraLocale";
    
    public static final String A_zimbraPrefLocale = "zimbraPrefLocale";
    
    public static final String A_zimbraAvailableLocale = "zimbraAvailableLocale";
    
    /**
     * compat mode for calendar
     */
    public static final String A_zimbraCalendarCompatibilityMode = "zimbraCalendarCompatibilityMode";
    
    /**
     * Default domain name to use in getAccountByName if no domain specified.
     */
    public static final String A_zimbraDefaultDomainName = "zimbraDefaultDomainName";

    
    public static final String A_zimbraDomainDefaultCOSId = "zimbraDomainDefaultCOSId";

    /**
    * Logout URL for domain
    */
    public static final String A_zimbraWebClientLogoutURL = "zimbraWebClientLogoutURL";

    /**
     * For a zimbraDomain object, the domain's name (i.e., widgets.com) 
     */
    public static final String A_zimbraDomainName = "zimbraDomainName";

    /**
     * Whether a domain is local or alias.
     */
    public static final String A_zimbraDomainType = "zimbraDomainType";
    
    public static final String A_zimbraDomainStatus = "zimbraDomainStatus";
    public static final String A_zimbraDomainRenameInfo = "zimbraDomainRenameInfo";
    
    /**
     * Hostname visited by the client to domain name mapping, in order to make
     * virtual hosting work.
     */
    public static final String A_zimbraVirtualHostname = "zimbraVirtualHostname";

    public static final String A_zimbraGalLdapURL = "zimbraGalLdapURL";
    
    public static final String A_zimbraGalLdapSearchBase = "zimbraGalLdapSearchBase";
    
    public static final String A_zimbraGalLdapBindDn = "zimbraGalLdapBindDn";

    public static final String A_zimbraGalLdapBindPassword = "zimbraGalLdapBindPassword";

    public static final String A_zimbraGalLdapFilter = "zimbraGalLdapFilter";
    
    public static final String A_zimbraGalAutoCompleteLdapFilter = "zimbraGalAutoCompleteLdapFilter";
    
    public static final String A_zimbraGalLdapAttrMap = "zimbraGalLdapAttrMap";    

    /**
     * global filters defs. Should be in the format: name:{filter-str}
     */
    public static final String A_zimbraGalLdapFilterDef = "zimbraGalLdapFilterDef";
    
    /**
     * max results to return from a gal search
     */
    public static final String A_zimbraGalMaxResults = "zimbraGalMaxResults";
    
    /**
     * search base for internal GAL searches.
     * special values: "ROOT" for top, "DOMAIN" for domain only, "SUBDOMAINS" for domain and subdomains 
     * 
     */
    public static final String A_zimbraGalInternalSearchBase = "zimbraGalInternalSearchBase";

    /**
     * GAL mode. should be internal, external, or both. 
     */
    public static final String A_zimbraGalMode = "zimbraGalMode";
    
    /**
     *  external LDAP GAL authentication mechanism
     *      none: anonymous binding
     *      simple: zimbraGalLdapBindDn and zimbraGalLdapBindPassword has to be set
     *      kerberos5: zimbraGalLdapKerberos5Principal and zimbraGalLdapKerberos5Keytab has to be set
     */
    public static final String A_zimbraGalLdapAuthMech = "zimbraGalLdapAuthMech";
    
    public static final String LDAP_AM_NONE = "none";
    public static final String LDAP_AM_SIMPLE = "simple";
    public static final String LDAP_AM_KERBEROS5 = "kerberos5";
    
    public static final String A_zimbraGalLdapKerberos5Principal = "zimbraGalLdapKerberos5Principal";
    public static final String A_zimbraGalLdapKerberos5Keytab = "zimbraGalLdapKerberos5Keytab";
    
    
    /**
     * set to true if an account is an admin account
     */
    public static final String A_zimbraIsAdminAccount = "zimbraIsAdminAccount";    

    /**
     * set to true if an account is an domain admin account
     */
    public static final String A_zimbraIsDomainAdminAccount = "zimbraIsDomainAdminAccount";    

    /**
     * Set for entries (accounts/lists) in the directory that have an alias
     */
    public static final String A_zimbraMailAlias = "zimbraMailAlias";    
    
    /**
     * Set to be the address that an entry's local address(es) ultimately resolve to.
     */
    public static final String A_zimbraMailDeliveryAddress = "zimbraMailDeliveryAddress";
    
    /**
     * one or more forwarding addresses for an entry. Used to implement mailing lists, as well as providing forwarding
     * for accounts.
     */
    public static final String A_zimbraMailForwardingAddress = "zimbraMailForwardingAddress";
    
    /**
     * Address to which this account's address must be rewritten.
     */
    public static final String A_zimbraMailCanonicalAddress = "zimbraMailCanonicalAddress";
    
    /**
     * Designates a catch all source address, used in whole domain
     * forwards to mailboxes or other domains.
     */
    public static final String A_zimbraMailCatchAllAddress = "zimbraMailCatchAllAddress";

    /**
     * Designates a catch all destination address, used in whole
     * domain forwards.
     */
    public static final String A_zimbraMailCatchAllForwardingAddress = "zimbraMailCatchAllForwardingAddress";
    
    /**
     * Designates a catch all canonical address, used in whole domain
     * addresss rewrites.
     */
    public static final String A_zimbraMailCatchAllCanonicalAddress = "zimbraMailCatchAllCanonicalAddress";

    /**
     * the host/ip address where a user's mailbox is located 
     */
    public static final String A_zimbraMailHost = "zimbraMailHost";

    /**
     * "<server zimbraId>:<mailbox ID on that host>" indicating the location of
     * mailbox prior to being moved to new server
     */
    public static final String A_zimbraMailboxLocationBeforeMove = "zimbraMailboxLocationBeforeMove";

    /**
     * the postfix transport for the mailbox (derived from zimbraMailHost)
     */
    public static final String A_zimbraMailTransport = "zimbraMailTransport";
    
    /**
     * multi-value COS attr which is list of servers to provision users on when creating accounts
     */
    public static final String A_zimbraMailHostPool = "zimbraMailHostPool";

    /**
     * the mail status (MAIL_STATUS_*) for a given entry. Must be "enabled" to receive mail.
     */
    public static final String A_zimbraMailStatus = "zimbraMailStatus";
    
    /**
     * the quota (in bytes) of a mailbox.
     */
    public static final String A_zimbraMailQuota = "zimbraMailQuota";

    /**
     * maximum quota a domain admin can assign
     */
    public static final String A_zimbraDomainAdminMaxMailQuota = "zimbraDomainAdminMaxMailQuota";    
    
    /**
     * the auto-generated sieve script
     */
    public static final String A_zimbraMailSieveScript = "zimbraMailSieveScript";

    public static final String A_zimbraLogHostname = "zimbraLogHostname";

    public static final String A_zimbraMessageCacheSize = "zimbraMessageCacheSize";
    
    public static final String A_zimbraMessageIdDedupeCacheSize = "zimbraMessageIdDedupeCacheSize";

    public static final String A_zimbraMtaAuthTarget = "zimbraMtaAuthTarget";
    public static final String A_zimbraMtaAuthEnabled = "zimbraMtaAuthEnabled";
    public static final String A_zimbraMtaAuthHost = "zimbraMtaAuthHost";
    public static final String A_zimbraMtaAuthURL = "zimbraMtaAuthURL";

    public static final String A_zimbraMtaBlockedExtension = "zimbraMtaBlockedExtension";
    public static final String A_zimbraMtaCommonBlockedExtension = "zimbraMtaCommonBlockedExtension";
    public static final String A_zimbraMtaDnsLookupsEnabled = "zimbraMtaDnsLookupsEnabled";
    public static final String A_zimbraMtaMyNetworks = "zimbraMtaMyNetworks";    
    public static final String A_zimbraMtaMaxMessageSize = "zimbraMtaMaxMessageSize";
    public static final String A_zimbraMtaRelayHost = "zimbraMtaRelayHost";
    public static final String A_zimbraMtaTlsAuthOnly = "zimbraMtaTlsAuthOnly";
    public static final String A_zimbraMtaRecipientDelimiter = "zimbraMtaRecipientDelimiter";
    
    public static final String A_zimbraPreAuthKey = "zimbraPreAuthKey";
    
    public static final String A_zimbraAvailableSkin = "zimbraAvailableSkin";
    public static final String A_zimbraInstalledSkin = "zimbraInstalledSkin";

    public static final String A_zimbraPrefSkin = "zimbraPrefSkin";
    
    public static final String A_zimbraPrefMailDefaultCharset = "zimbraPrefMailDefaultCharset";
    
    public static final String A_zimbraPrefTimeZoneId = "zimbraPrefTimeZoneId";
    public static final String A_zimbraPrefUseTimeZoneListInCalendar = "zimbraPrefUseTimeZoneListInCalendar";

    public static final String A_zimbraPrefUseRfc2231 = "zimbraPrefUseRfc2231";

    public static final String A_zimbraPrefClientType = "zimbraPrefClientType";
    
    public static final String A_zimbraPrefCalendarNotifyDelegatedChanges = "zimbraPrefCalendarNotifyDelegatedChanges";

    public static final String A_zimbraPrefCalendarFirstDayOfWeek = "zimbraPrefCalendarFirstDayOfWeek";

    public static final String A_zimbraPrefCalendarUseQuickAdd = "zimbraPrefCalendarUseQuickAdd";

    public static final String A_zimbraPrefCalendarInitialCheckedCalendars = "zimbraPrefCalendarInitialCheckedCalendars";

    public static final String A_zimbraPrefCalendarDayHourStart = "zimbraPrefCalendarDayHourStart";

    public static final String A_zimbraPrefCalendarDayHourEnd = "zimbraPrefCalendarDayHourEnd";

    public static final String A_zimbraPrefCalendarInitialView = "zimbraPrefCalendarInitialView";

    public static final String A_zimbraPrefShowSearchString = "zimbraPrefShowSearchString";

    public static final String A_zimbraPrefShowFragments = "zimbraPrefShowFragments";
    
    public static final String A_zimbraPrefShowSelectionCheckbox = "zimbraPrefShowSelectionCheckbox";

    public static final String A_zimbraPrefMessageViewHtmlPreferred = "zimbraPrefMessageViewHtmlPreferred";

    public static final String A_zimbraPrefAutoAddAddressEnabled = "zimbraPrefAutoAddAddressEnabled";

    public static final String A_zimbraPrefContactsPerPage = "zimbraPrefContactsPerPage";   
    


    /**
     * whether or not the signature is automatically included on outgoing email
     */
    public static final String A_zimbraPrefMailSignatureEnabled = "zimbraPrefMailSignatureEnabled";

    public static final String A_zimbraPrefMailForwardingAddress = "zimbraPrefMailForwardingAddress";

    public static final String A_zimbraPrefMailLocalDeliveryDisabled = "zimbraPrefMailLocalDeliveryDisabled";
    
    public static final String A_zimbraPrefMailInitialSearch = "zimbraPrefMailInitialSearch";

    public static final String A_zimbraPrefGroupMailBy = "zimbraPrefGroupMailBy";

    public static final String A_zimbraPrefImapSearchFoldersEnabled = "zimbraPrefImapSearchFoldersEnabled";

    public static final String A_zimbraPrefIncludeSpamInSearch = "zimbraPrefIncludeSpamInSearch";

    public static final String A_zimbraPrefIncludeTrashInSearch = "zimbraPrefIncludeTrashInSearch";

    public static final String A_zimbraPrefMailItemsPerPage = "zimbraPrefMailItemsPerPage";

    public static final String A_zimbraPrefOutOfOfficeReply = "zimbraPrefOutOfOfficeReply";

    public static final String A_zimbraPrefOutOfOfficeReplyEnabled = "zimbraPrefOutOfOfficeReplyEnabled";

    public static final String A_zimbraPrefOutOfOfficeFromDate = "zimbraPrefOutOfOfficeFromDate";
    
    public static final String A_zimbraPrefOutOfOfficeUntilDate = "zimbraPrefOutOfOfficeUntilDate";
    
    public static final String A_zimbraPrefOutOfOfficeCacheDuration = "zimbraPrefOutOfOfficeCacheDuration";
    
    public static final String A_zimbraPrefOutOfOfficeDirectAddress = "zimbraPrefOutOfOfficeDirectAddress";

    public static final String A_zimbraPrefReplyToAddress = "zimbraPrefReplyToAddress";

    public static final String A_zimbraPrefReadingPaneEnabled = "zimbraPrefReadingPaneEnabled";

    public static final String A_zimbraPrefShortcuts = "zimbraPrefShortcuts";

    public static final String A_zimbraPrefUseKeyboardShortcuts = "zimbraPrefUseKeyboardShortcuts";

    public static final String A_zimbraPrefNewMailNotificationEnabled = "zimbraPrefNewMailNotificationEnabled";
    
    public static final String A_zimbraPrefNewMailNotificationAddress = "zimbraPrefNewMailNotificationAddress";
    
    public static final String A_zimbraPrefDedupeMessagesSentToSelf = "zimbraPrefDedupeMessagesSentToSelf";
    
    public static final String A_zimbraFeatureMailForwardingEnabled = "zimbraFeatureMailForwardingEnabled";

    public static final String A_zimbraFeatureMobileSyncEnabled = "zimbraFeatureMobileSyncEnabled";

    public static final String A_zimbraAllowAnyFromAddress = "zimbraAllowAnyFromAddress";

    public static final String A_zimbraAllowFromAddress = "zimbraAllowFromAddress";
    
    public static final String A_zimbraFeatureMailEnabled = "zimbraFeatureMailEnabled";

    public static final String A_zimbraFeatureContactsEnabled = "zimbraFeatureContactsEnabled";

    public static final String A_zimbraFeatureVoiceEnabled = "zimbraFeatureVoiceEnabled";
    
    public static final String A_zimbraFeatureCalendarEnabled = "zimbraFeatureCalendarEnabled";
    
    public static final String A_zimbraFeatureGroupCalendarEnabled = "zimbraFeatureGroupCalendarEnabled";

    public static final String A_zimbraFeatureTasksEnabled = "zimbraFeatureTasksEnabled";

    public static final String A_zimbraFeatureTaggingEnabled = "zimbraFeatureTaggingEnabled";

    public static final String A_zimbraFeatureAdvancedSearchEnabled = "zimbraFeatureAdvancedSearchEnabled";

    public static final String A_zimbraFeatureSavedSearchesEnabled = "zimbraFeatureSavedSearchesEnabled";

    public static final String A_zimbraFeatureConversationsEnabled = "zimbraFeatureConversationsEnabled";

    public static final String A_zimbraFeatureChangePasswordEnabled = "zimbraFeatureChangePasswordEnabled";

    public static final String A_zimbraFeatureInitialSearchPreferenceEnabled = "zimbraFeatureInitialSearchPreferenceEnabled";

    public static final String A_zimbraFeatureFiltersEnabled = "zimbraFeatureFiltersEnabled";

    public static final String A_zimbraFeatureGalEnabled = "zimbraFeatureGalEnabled";

    public static final String A_zimbraFeatureHtmlComposeEnabled = "zimbraFeatureHtmlComposeEnabled";

    public static final String A_zimbraFeatureIMEnabled = "zimbraFeatureIMEnabled";
    
    public static final String A_zimbraFeatureInstantNotify = "zimbraFeatureInstantNotify";

    public static final String A_zimbraFeatureViewInHtmlEnabled = "zimbraFeatureViewInHtmlEnabled";

    public static final String A_zimbraFeatureSharingEnabled = "zimbraFeatureSharingEnabled";

    public static final String A_zimbraFeatureSkinChangeEnabled = "zimbraFeatureSkinChangeEnabled";

    public static final String A_zimbraFeatureNotebookEnabled = "zimbraFeatureNotebookEnabled";

    public static final String A_zimbraFeatureGalAutoCompleteEnabled = "zimbraFeatureGalAutoCompleteEnabled";

    public static final String A_zimbraFeatureOutOfOfficeReplyEnabled = "zimbraFeatureOutOfOfficeReplyEnabled";

    public static final String A_zimbraFeatureNewMailNotificationEnabled = "zimbraFeatureNewMailNotificationEnabled";

    public static final String A_zimbraFeatureIdentitiesEnabled = "zimbraFeatureIdentitiesEnabled";
    
    public static final String A_zimbraFeatureSignaturesEnabled = "zimbraFeatureSignaturesEnabled";

    public static final String A_zimbraFeaturePop3DataSourceEnabled = "zimbraFeaturePop3DataSourceEnabled";
    
    public static final String A_zimbraFeatureShortcutAliasesEnabled = "zimbraFeatureShortcutAliasesEnabled";
    
    public static final String A_zimbraFeatureBriefcasesEnabled = "zimbraFeatureBriefcasesEnabled";
    
    public static final String A_zimbraFeatureFlaggingEnabled = "zimbraFeatureFlaggingEnabled";

    public static final String A_zimbraFeatureMailPriorityEnabled = "zimbraFeatureMailPriorityEnabled";

    public static final String A_zimbraFeaturePortalEnabled = "zimbraFeaturePortalEnabled";

    

    /**
     * administrative notes for an entry.
     */
    public static final String A_zimbraNotes = "zimbraNotes";

    /**
     * For selective enabling of debug logging.
     */
    public static final String A_zimbraDebugInfo = "zimbraDebugInfo";

    /**                                                                                                                         
     * number of old passwords to keep, or 0 for no history.
     */
    public static final String A_zimbraPasswordEnforceHistory = "zimbraPasswordEnforceHistory";
    
    /**
     * old passwords, time:value
     */
    public static final String A_zimbraPasswordHistory = "zimbraPasswordHistory";
    
    /**
     * password is locked and can't be changed by user
     */
    public static final String A_zimbraPasswordLocked = "zimbraPasswordLocked";    
    
    /**
     * minimum password length
     */
    public static final String A_zimbraPasswordMinLength = "zimbraPasswordMinLength";
    
    /**
     * maximum password length. 0 means no limit.
     */
    public static final String A_zimbraPasswordMaxLength = "zimbraPasswordMaxLength";

    /**
     * minimum number of upper case characters required in a password
     */
    public static final String A_zimbraPasswordMinUpperCaseChars = "zimbraPasswordMinUpperCaseChars";

    /**
     * minimum number of lower case characters required in a password
     */
    public static final String A_zimbraPasswordMinLowerCaseChars = "zimbraPasswordMinLowerCaseChars";

    /**
     * minimum number of punctuation characters required in a password
     */
    public static final String A_zimbraPasswordMinPunctuationChars = "zimbraPasswordMinPunctuationChars";

    /**
     * minimum number of numeric characters required in a password
     */
    public static final String A_zimbraPasswordMinNumericChars = "zimbraPasswordMinNumericChars";
    
    /**
     * minimum password lifetime in days. 0 means no limit.
     */
    public static final String A_zimbraPasswordMinAge = "zimbraPasswordMinAge";
    
    /**
     * maximum password lifetime in days. 0 means no limit.
     */
    public static final String A_zimbraPasswordMaxAge = "zimbraPasswordMaxAge";

    /**
     * modified time
     */
    public static final String A_zimbraPasswordModifiedTime = "zimbraPasswordModifiedTime";

    /**
     * must change on next auth
     */
    public static final String A_zimbraPasswordMustChange = "zimbraPasswordMustChange";
    
    /**
     * This attribute specifies the action that should be taken when
     * a user has made a number of failed attempts to authenticate
     * to Zimbra.  If zimbraPasswordLockoutEnabled is TRUE, the user
     * will  not  be allowed to attempt to authenticate to Zimbra after
     * there have been a specified number of consecutive failed login attempts.
     * The maximum number of consecutive failed login attempts allowed 
     * is specified by the zimbraPasswordLockoutMaxFailures attribute.  
     *
     * If zimbraPasswordLockoutEnabled is not present, or if its  value
     * is "FALSE", the password may be used to authenticate no matter 
     * how many consecutive failed login attempts have been made.
     * 
     */
    public static final String A_zimbraPasswordLockoutEnabled = "zimbraPasswordLockoutEnabled";

    /**
     * This attribute contains the duration during which the password
     * cannot  be  used  to  authenticate the user to Zimbra due to too
     * many consecutive failed login attempts. 
     *
     * If zimbraPasswordLockoutDuration is not present, or if its value is 
     * zero (0), the password cannot be used to authenticate the user to
     * Zimbra again until it is reset by an administrator.
     */
    public static final String A_zimbraPasswordLockoutDuration = "zimbraPasswordLockoutDuration";    

    /**
     * This  attribute contains the number of consecutive failed login attempts
     * after which the password may not be used to authenticate a user to
     * Zimbra. If zimbraPasswordLockoutMaxFailures is not present, or its
     * value is zero (0), then a user will be allowed to continue to attempt 
     * to  authenticate  to Zimbra, no matter how many consecutive failed 
     * login attempts have  occurred.
     */
    public static final String A_zimbraPasswordLockoutMaxFailures = "zimbraPasswordLockoutMaxFailures";
   
    /**
     * This  attribute contains the duration after which old consecu-
     * tive failed login attempts are purged from  the  list,  even though 
     * no  successful  authentication  has occurred. It used to determine
     * the window in which a failed login is considered "consecutive". 
     *
     * If zimbraPasswordLockoutFailureLifetime is not present, or its 
     * value is  zero  (0), the  list will only be reset by a successful 
     * authentication.
     *
     * For example, if this attribute is not set, and max failures is set 
     * to 3, then one unsuccessful login attempt a day (with no successful 
     * login attempts) will cause the account to be locked out on the third
     * day. i.e.. If this attribute is set to be one hour, then there must 
     * be three failed login attempts within an hour for the account to get
     * locked out.
     */
    public static final String A_zimbraPasswordLockoutFailureLifetime = "zimbraPasswordLockoutFailureLifetime";

    /**
     * This attribute contains the time that the user's  account  was  locked.
     * If  the  account has been locked, the password may no longer be used to
     * authenticate the user to Zimbra.
     * 
     * An account is considered locked if the current time is less than the
     * value zimbraPasswordLockoutLockedTime + zimbraPasswordLockoutDuration.
     */
    public static final String A_zimbraPasswordLockoutLockedTime = "zimbraPasswordLockoutLockedTime";

    /**
     * If zimbraPasswordLockoutEnabled is set, then this attribute contains the
     * timestamps of each of the consecutive  authentication failures made on 
     * an account.
     *
     * If more then zimbraPasswordLockoutMaxFailures timestamps accumulate
     * here then the account will be locked, and 
     * zimbraPasswordLockoutLockedTime will be set to the current time.
     *
     * Excess timestamps beyond those allowed by 
     * zimbraPasswordLockoutMaxFailures  will be purged.  
     *
     * When a successful authentication is made, all 
     * zimbraPasswordFailureTime entries are removed.
     */
    public static final String A_zimbraPasswordLockoutFailureTime = "zimbraPasswordLockoutFailureTime";
    
    /**
     * the mail .signature value
     */
    public static final String A_zimbraPrefMailSignature = "zimbraPrefMailSignature";
    public static final String A_zimbraPrefMailSignatureHTML = "zimbraPrefMailSignatureHTML";
    public static final String A_zimbraMailSignatureMaxLength = "zimbraMailSignatureMaxLength";
    
    /**
     * wether or not to save outgoing mail
     */
    public static final String A_zimbraPrefSaveToSent = "zimbraPrefSaveToSent";
    
    /**
     * where to save it
     */
    public static final String A_zimbraPrefSentMailFolder = "zimbraPrefSentMailFolder";

    public static final String A_zimbraPrefBccAddress = "zimbraPrefBccAddress";
    public static final String A_zimbraPrefComposeFormat = "zimbraPrefComposeFormat";
    public static final String A_zimbraPrefForwardIncludeOriginalText = "zimbraPrefForwardIncludeOriginalText";
    public static final String A_zimbraPrefForwardReplyFormat = "zimbraPrefForwardReplyFormat";
    public static final String A_zimbraPrefForwardReplyInOriginalFormat = "zimbraPrefForwardReplyInOriginalFormat";
    public static final String A_zimbraPrefForwardReplyPrefixChar = "zimbraPrefForwardReplyPrefixChar";
    public static final String A_zimbraPrefFromAddress = "zimbraPrefFromAddress";
    public static final String A_zimbraPrefFromDisplay = "zimbraPrefFromDisplay";
    public static final String A_zimbraPrefMailSignatureStyle = "zimbraPrefMailSignatureStyle";
    public static final String A_zimbraPrefReplyIncludeOriginalText = "zimbraPrefReplyIncludeOriginalText";
    public static final String A_zimbraPrefReplyToDisplay = "zimbraPrefReplyToDisplay";
    public static final String A_zimbraPrefReplyToEnabled = "zimbraPrefReplyToEnabled";
    public static final String A_zimbraPrefUseDefaultIdentitySettings = "zimbraPrefUseDefaultIdentitySettings";
    public static final String A_zimbraPrefWhenInFolderIds = "zimbraPrefWhenInFolderIds";
    public static final String A_zimbraPrefWhenInFoldersEnabled = "zimbraPrefWhenInFoldersEnabled";
    public static final String A_zimbraPrefWhenSentToAddresses = "zimbraPrefWhenSentToAddresses";
    public static final String A_zimbraPrefWhenSentToEnabled = "zimbraPrefWhenSentToEnabled";
    
    public static final String A_zimbraPrefIdentityId = "zimbraPrefIdentityId";
    public static final String A_zimbraPrefIdentityName = "zimbraPrefIdentityName";

    /**
     * To get default HtmlEditor Properties
     */
    public static final String A_zimbraPrefHtmlEditorDefaultFontFamily = "zimbraPrefHtmlEditorDefaultFontFamily";
    public static final String A_zimbraPrefHtmlEditorDefaultFontSize = "zimbraPrefHtmlEditorDefaultFontSize";    
    public static final String A_zimbraPrefHtmlEditorDefaultFontColor = "zimbraPrefHtmlEditorDefaultFontColor";

    public static final String A_zimbraPrefDefaultSignatureId = "zimbraPrefDefaultSignatureId";
    public static final String A_zimbraSignatureId = "zimbraSignatureId";
    public static final String A_zimbraSignatureName = "zimbraSignatureName";
    
    public static final String DEFAULT_IDENTITY_NAME = "DEFAULT";
    // public static final String DEFAULT_SIGNATURE_NAME = "DEFAULT";
    
    /**
     * delete appointment invite (from our inbox) when we've replied to it?
     * TODO add to schema! 
     */
    public static final String A_zimbraPrefDeleteInviteOnReply = "zimbraPrefDeleteInviteOnReply";
    
    /**
     * for accounts, the zimbraId of the COS that this account belongs to.
     */
    public static final String A_zimbraCOSId = "zimbraCOSId";

    /**
     * for accounts or calendar resources
     *   "USER" for regular accounts (default if not set)
     *   "RESOURCE" for calendar resources
     */
    public static final String A_zimbraAccountCalendarUserType = "zimbraAccountCalendarUserType";

    public static final String A_zimbraMailPort = "zimbraMailPort";
    public static final String A_zimbraMailSSLPort = "zimbraMailSSLPort";
    public static final String A_zimbraMailMode = "zimbraMailMode";
    public static final String A_zimbraMailURL = "zimbraMailURL";

    public static final String A_zimbraAdminPort = "zimbraAdminPort";

    public static final String A_zimbraShareInfo = "zimbraShareInfo";
    
    public static final String A_zimbraSmtpHostname = "zimbraSmtpHostname";
    public static final String A_zimbraSmtpPort = "zimbraSmtpPort";
    public static final String A_zimbraSmtpTimeout = "zimbraSmtpTimeout";
    public static final String A_zimbraSmtpSendPartial = "zimbraSmtpSendPartial";
    public static final String A_zimbraSmtpSendAddOriginatingIP = "zimbraSmtpSendAddOriginatingIP";

    public static final String A_zimbraLmtpAdvertisedName = "zimbraLmtpAdvertisedName";
    public static final String A_zimbraLmtpBindPort = "zimbraLmtpBindPort";
    public static final String A_zimbraLmtpBindOnStartup = "zimbraLmtpBindOnStartup";
    public static final String A_zimbraLmtpBindAddress = "zimbraLmtpBindAddress";
    public static final String A_zimbraLmtpNumThreads = "zimbraLmtpNumThreads";
    public static final String A_zimbraMailDiskStreamingThreshold = "zimbraMailDiskStreamingThreshold";

    public static final String A_zimbraImapAdvertisedName = "zimbraImapAdvertisedName";
    public static final String A_zimbraImapBindPort = "zimbraImapBindPort";
    public static final String A_zimbraImapBindOnStartup = "zimbraImapBindOnStartup";
    public static final String A_zimbraImapBindAddress = "zimbraImapBindAddress";
    public static final String A_zimbraImapNumThreads = "zimbraImapNumThreads";        
    public static final String A_zimbraImapServerEnabled = "zimbraImapServerEnabled";        
    public static final String A_zimbraImapDisabledCapability = "zimbraImapDisabledCapability";        
    public static final String A_zimbraImapSSLBindPort = "zimbraImapSSLBindPort";
    public static final String A_zimbraImapSSLBindOnStartup = "zimbraImapSSLBindOnStartup";
    public static final String A_zimbraImapSSLBindAddress = "zimbraImapSSLBindAddress";
    public static final String A_zimbraImapSSLServerEnabled = "zimbraImapSSLServerEnabled";            
    public static final String A_zimbraImapSSLDisabledCapability = "zimbraImapSSLDisabledCapability";        
    public static final String A_zimbraImapCleartextLoginEnabled = "zimbraImapCleartextLoginEnabled";    
    public static final String A_zimbraImapEnabled = "zimbraImapEnabled";
    public static final String A_zimbraImapProxyBindPort = "zimbraImapProxyBindPort";
    public static final String A_zimbraImapSSLProxyBindPort = "zimbraImapSSLProxyBindPort";
    public static final String A_zimbraImapSaslGssapiEnabled = "zimbraImapSaslGssapiEnabled";

    public static final String A_zimbraPop3AdvertisedName = "zimbraPop3AdvertisedName";
    public static final String A_zimbraPop3BindPort = "zimbraPop3BindPort";
    public static final String A_zimbraPop3BindOnStartup = "zimbraPop3BindOnStartup";
    public static final String A_zimbraPop3BindAddress = "zimbraPop3BindAddress";
    public static final String A_zimbraPop3NumThreads = "zimbraPop3NumThreads";
    public static final String A_zimbraPop3ServerEnabled = "zimbraPop3ServerEnabled";        
    public static final String A_zimbraPop3SSLBindPort = "zimbraPop3SSLBindPort";
    public static final String A_zimbraPop3SSLBindOnStartup = "zimbraPop3SSLBindOnStartup";
    public static final String A_zimbraPop3SSLBindAddress = "zimbraPop3SSLBindAddress";
    public static final String A_zimbraPop3SSLServerEnabled = "zimbraPop3SSLServerEnabled";            
    public static final String A_zimbraPop3CleartextLoginEnabled = "zimbraPop3CleartextLoginEnabled";    
    public static final String A_zimbraPop3Enabled = "zimbraPop3Enabled";
    public static final String A_zimbraPop3ProxyBindPort = "zimbraPop3ProxyBindPort";
    public static final String A_zimbraPop3SSLProxyBindPort = "zimbraPop3SSLProxyBindPort";
    public static final String A_zimbraPop3SaslGssapiEnabled = "zimbraPop3SaslGssapiEnabled";
    
    public static final String A_zimbraNotifyServerEnabled = "zimbraNotifyServerEnabled";
    public static final String A_zimbraNotifyBindAddress = "zimbraNotifyBindAddress";
    public static final String A_zimbraNotifyBindPort = "zimbraNotifyBindPort";
    public static final String A_zimbraNotifySSLServerEnabled = "zimbraNotifySSLServerEnabled";
    public static final String A_zimbraNotifySSLBindAddress = "zimbraNotifySSLBindAddress";
    public static final String A_zimbraNotifySSLBindPort = "zimbraNotifySSLBindPort";

    // Message purging
    public static final String A_zimbraMailTrashLifetime = "zimbraMailTrashLifetime";
    public static final String A_zimbraMailSpamLifetime = "zimbraMailSpamLifetime";
    public static final String A_zimbraMailMessageLifetime = "zimbraMailMessageLifetime";
    public static final String A_zimbraPrefInboxUnreadLifetime = "zimbraPrefInboxUnreadLifetime";
    public static final String A_zimbraPrefInboxReadLifetime = "zimbraPrefInboxReadLifetime";
    public static final String A_zimbraPrefSentLifetime = "zimbraPrefSentLifetime";
    public static final String A_zimbraPrefJunkLifetime = "zimbraPrefJunkLifetime";
    public static final String A_zimbraPrefTrashLifetime = "zimbraPrefTrashLifetime";
    public static final String A_zimbraMailPurgeSleepInterval = "zimbraMailPurgeSleepInterval";
    public static final String A_zimbraMailLastPurgedMailboxId = "zimbraMailLastPurgedMailboxId";
    
    public static final String A_zimbraContactMaxNumEntries = "zimbraContactMaxNumEntries";
    public static final String A_zimbraIdentityMaxNumEntries = "zimbraIdentityMaxNumEntries";
    public static final String A_zimbraSignatureMaxNumEntries = "zimbraSignatureMaxNumEntries";
    public static final String A_zimbraAuthTokenLifetime = "zimbraAuthTokenLifetime";
    public static final String A_zimbraAdminAuthTokenLifetime = "zimbraAdminAuthTokenLifetime";
    public static final String A_zimbraMailMinPollingInterval =  "zimbraMailMinPollingInterval";
    public static final String A_zimbraPrefMailPollingInterval = "zimbraPrefMailPollingInterval";

    public static final String A_zimbraSpamHeader = "zimbraSpamHeader";
    public static final String A_zimbraSpamHeaderValue = "zimbraSpamHeaderValue";
    
    public static final String A_zimbraLastLogonTimestamp = "zimbraLastLogonTimestamp";
    public static final String A_zimbraLastLogonTimestampFrequency = "zimbraLastLogonTimestampFrequency";
    
    public static final String A_zimbraAttachmentsBlocked = "zimbraAttachmentsBlocked";
    public static final String A_zimbraAttachmentsViewInHtmlOnly = "zimbraAttachmentsViewInHtmlOnly";
    public static final String A_zimbraAttachmentsIndexingEnabled = "zimbraAttachmentsIndexingEnabled";    
    
    public static final String A_zimbraAttachmentsScanEnabled = "zimbraAttachmentsScanEnabled";
    public static final String A_zimbraAttachmentsScanClass = "zimbraAttachmentsScanClass";
    public static final String A_zimbraAttachmentsScanURL = "zimbraAttachmentsScanURL";

    public static final String A_zimbraClusterType = "zimbraClusterType";

    public static final String A_zimbraServiceHostname = "zimbraServiceHostname";

    public static final String A_zimbraRedoLogEnabled            = "zimbraRedoLogEnabled";
    public static final String A_zimbraRedoLogLogPath            = "zimbraRedoLogLogPath";
    public static final String A_zimbraRedoLogArchiveDir         = "zimbraRedoLogArchiveDir";
    public static final String A_zimbraRedoLogRolloverFileSizeKB = "zimbraRedoLogRolloverFileSizeKB";
    public static final String A_zimbraRedoLogDeleteOnRollover   = "zimbraRedoLogDeleteOnRollover";
    public static final String A_zimbraRedoLogFsyncIntervalMS    = "zimbraRedoLogFsyncIntervalMS";

    public static final String A_zimbraRedoLogProvider = "zimbraRedoLogProvider";

    public static final String A_zimbraSpellCheckURL = "zimbraSpellCheckURL";
    
    /**
     * Email address of the sender of a new email notification message.
     * @see com.zimbra.cs.mailbox.MailboxManager#notifyIfNecessary
     */
    public static final String A_zimbraNewMailNotificationFrom               = "zimbraNewMailNotificationFrom";

    /**
     * Template used to generate the subject of a new email notification message.
     * @see com.zimbra.cs.mailbox.MailboxManager#notifyIfNecessary
     */
    public static final String A_zimbraNewMailNotificationSubject            = "zimbraNewMailNotificationSubject";
    
    /**
     * Template used to generate the body of a new email notification message.
     * @see com.zimbra.cs.mailbox.MailboxManager#notifyIfNecessary
     */
    public static final String A_zimbraNewMailNotificationBody               = "zimbraNewMailNotificationBody";
    
    /**
     * If set to false, Tomcat server will refuse end-user commands on SOAP
     * and LMTP interfaces, and allow only admin commands.
     */
    public static final String A_zimbraUserServicesEnabled = "zimbraUserServicesEnabled";

    /**
     * Attributes for mime type handlers.
     */
    public static final String A_zimbraMimeType                 = "zimbraMimeType";
    public static final String A_zimbraMimeIndexingEnabled      = "zimbraMimeIndexingEnabled";
    public static final String A_zimbraMimeHandlerClass         = "zimbraMimeHandlerClass";
    public static final String A_zimbraMimeHandlerExtension     = "zimbraMimeHandlerExtension";
    public static final String A_zimbraMimeFileExtension        = "zimbraMimeFileExtension";
    public static final String A_zimbraMimePriority             = "zimbraMimePriority";
    
    /**
     * Attributes for object type handlers.
     */
    public static final String A_zimbraObjectType               = "zimbraObjectType";
    public static final String A_zimbraObjectIndexingEnabled    = "zimbraObjectIndexingEnabled";
    public static final String A_zimbraObjectStoreMatched       = "zimbraObjectStoreMatched"; 
    public static final String A_zimbraObjectHandlerClass       = "zimbraObjectHandlerClass";
    public static final String A_zimbraObjectHandlerConfig      = "zimbraObjectHandlerConfig";

    public static final String A_zimbraSpamCheckEnabled = "zimbraSpamCheckEnabled";
    public static final String A_zimbraSpamKillPercent = "zimbraSpamKillPercent";
    public static final String A_zimbraSpamSubjectTag = "zimbraSpamSubjectTag";
    public static final String A_zimbraSpamTagPercent = "zimbraSpamTagPercent";
    public static final String A_zimbraSpamIsSpamAccount = "zimbraSpamIsSpamAccount";
    public static final String A_zimbraSpamIsNotSpamAccount = "zimbraSpamIsNotSpamAccount";

    public static final String A_zimbraVirusCheckEnabled = "zimbraVirusCheckEnabled";
    public static final String A_zimbraVirusWarnRecipient = "zimbraVirusWarnRecipient";
    public static final String A_zimbraVirusWarnAdmin = "zimbraVirusWarnAdmin";
    public static final String A_zimbraVirusBlockEncryptedArchive = "zimbraVirusBlockEncryptedArchive";
    public static final String A_zimbraVirusDefinitionsUpdateFrequency = "zimbraVirusDefinitionsUpdateFrequency";
    
    public static final String A_zimbraForeignPrincipal = "zimbraForeignPrincipal";
    public static final String A_zimbraFileUploadMaxSize = "zimbraFileUploadMaxSize";
    public static final String A_zimbraSoapRequestMaxSize = "zimbraSoapRequestMaxSize";

    public static final String A_zimbraHttpProxyURL = "zimbraHttpProxyURL";
    
    /**
     * Attributes for time zone objects
     */
    public static final String A_zimbraTimeZoneStandardDtStart = "zimbraTimeZoneStandardDtStart";
    public static final String A_zimbraTimeZoneStandardOffset = "zimbraTimeZoneStandardOffset";
    public static final String A_zimbraTimeZoneStandardRRule = "zimbraTimeZoneStandardRRule";
    public static final String A_zimbraTimeZoneDaylightDtStart = "zimbraTimeZoneDaylightDtStart";
    public static final String A_zimbraTimeZoneDaylightOffset = "zimbraTimeZoneDaylightOffset";
    public static final String A_zimbraTimeZoneDaylightRRule = "zimbraTimeZoneDaylightRRule";

    /**
     * Zimlets
     */
    public static final String A_zimbraZimletEnabled           = "zimbraZimletEnabled";
    public static final String A_zimbraZimletKeyword           = "zimbraZimletKeyword";
    public static final String A_zimbraZimletVersion           = "zimbraZimletVersion";
    public static final String A_zimbraZimletDescription       = "zimbraZimletDescription";
    public static final String A_zimbraZimletIndexingEnabled   = "zimbraZimletIndexingEnabled";
    public static final String A_zimbraZimletStoreMatched      = "zimbraZimletStoreMatched"; 
    public static final String A_zimbraZimletHandlerClass      = "zimbraZimletHandlerClass";
    public static final String A_zimbraZimletHandlerConfig     = "zimbraZimletHandlerConfig";
    public static final String A_zimbraZimletContentObject     = "zimbraZimletContentObject";
    public static final String A_zimbraZimletPanelItem         = "zimbraZimletPanelItem";
    public static final String A_zimbraZimletPriority          = "zimbraZimletPriority";
    public static final String A_zimbraZimletScript            = "zimbraZimletScript";
    public static final String A_zimbraZimletAvailableZimlets  = "zimbraZimletAvailableZimlets";
    public static final String A_zimbraZimletServerIndexRegex  = "zimbraZimletServerIndexRegex";
    public static final String A_zimbraZimletUserProperties    = "zimbraZimletUserProperties";
    public static final String A_zimbraZimletIsExtension       = "zimbraZimletIsExtension";
    public static final String A_zimbraProxyAllowedDomains     = "zimbraProxyAllowedDomains";
    public static final String A_zimbraProxyCacheableContentTypes = "zimbraProxyCacheableContentTypes";

    /**
     * Calendar resources
     */
    public static final String A_zimbraCalResType                 = "zimbraCalResType";
    public static final String A_zimbraCalResAutoAcceptDecline    = "zimbraCalResAutoAcceptDecline";
    public static final String A_zimbraCalResAutoDeclineIfBusy    = "zimbraCalResAutoDeclineIfBusy";
    public static final String A_zimbraCalResAutoDeclineRecurring = "zimbraCalResAutoDeclineRecurring";
    public static final String A_zimbraCalResLocationDisplayName  = "zimbraCalResLocationDisplayName";
    public static final String A_zimbraCalResSite                 = "zimbraCalResSite";
    public static final String A_zimbraCalResBuilding             = "zimbraCalResBuilding";
    public static final String A_zimbraCalResFloor                = "zimbraCalResFloor";
    public static final String A_zimbraCalResRoom                 = "zimbraCalResRoom";
    public static final String A_zimbraCalResCapacity             = "zimbraCalResCapacity";
    public static final String A_zimbraCalResContactName          = "zimbraCalResContactName";
    public static final String A_zimbraCalResContactEmail         = "zimbraCalResContactEmail";
    public static final String A_zimbraCalResContactPhone         = "zimbraCalResContactPhone";

    /*
     * Remote management
     */
    public static final String A_zimbraRemoteManagementCommand = "zimbraRemoteManagementCommand";
    public static final String A_zimbraRemoteManagementUser = "zimbraRemoteManagementUser";
    public static final String A_zimbraRemoteManagementPrivateKeyPath = "zimbraRemoteManagementPrivateKeyPath";
    public static final String A_zimbraRemoteManagementPort = "zimbraRemoteManagementPort";

    /* orphaned here because of network extension dependencies */
    public static final String A_zimbraArchiveAccount = "zimbraArchiveAccount";
    
    /*
     * Wiki
     */
    public static final String A_zimbraNotebookAccount         = "zimbraNotebookAccount";
    public static final String A_zimbraNotebookPageCacheSize   = "zimbraNotebookPageCacheSize";
    public static final String A_zimbraNotebookFolderCacheSize = "zimbraNotebookFolderCacheSize";
    public static final String A_zimbraNotebookMaxCachedTemplatesPerFolder = "zimbraNotebookMaxCachedTemplatesPerFolder";
    public static final String A_zimbraIsSystemResource        = "zimbraIsSystemResource";
    public static final String A_zimbraNotebookMaxRevisions    = "zimbraNotebookMaxRevisions";
    
    /*
     * data sources
     */
    public static final String A_zimbraDataSourceId = "zimbraDataSourceId";
    public static final String A_zimbraDataSourceName = "zimbraDataSourceName";
    public static final String A_zimbraDataSourceEnabled = "zimbraDataSourceEnabled";
    public static final String A_zimbraDataSourceHost = "zimbraDataSourceHost";
    public static final String A_zimbraDataSourcePort = "zimbraDataSourcePort";
    public static final String A_zimbraDataSourceUsername = "zimbraDataSourceUsername";
    public static final String A_zimbraDataSourcePassword = "zimbraDataSourcePassword";
    public static final String A_zimbraDataSourceFolderId = "zimbraDataSourceFolderId";
    public static final String A_zimbraDataSourceConnectionType = "zimbraDataSourceConnectionType";    
    public static final String A_zimbraDataSourceMaxNumEntries = "zimbraDataSourceMaxNumEntries";    
    public static final String A_zimbraDataSourceLeaveOnServer = "zimbraDataSourceLeaveOnServer";
    public static final String A_zimbraDataSourcePollingInterval = "zimbraDataSourcePollingInterval";
    public static final String A_zimbraDataSourceMinPollingInterval = "zimbraDataSourceMinPollingInterval";
    public static final String A_zimbraDataSourceEmailAddress = "zimbraDataSourceEmailAddress";
    public static final String A_zimbraDataSourceUseAddressForForwardReply = "zimbraDataSourceUseAddressForForwardReply";
    
    // Quota warning
    public static final String A_zimbraQuotaWarnPercent = "zimbraQuotaWarnPercent";
    public static final String A_zimbraQuotaLastWarnTime = "zimbraQuotaLastWarnTime";
    public static final String A_zimbraQuotaWarnInterval = "zimbraQuotaWarnInterval";
    public static final String A_zimbraQuotaWarnMessage = "zimbraQuotaWarnMessage";
    
    // Server/globalconfig
    public static final String A_zimbraScheduledTaskNumThreads = "zimbraScheduledTaskNumThreads";

    /*
     * Extension Text Analyzer
     */
    public static final String A_zimbraTextAnalyzer = "zimbraTextAnalyzer";
    
    public static final String A_zimbraXMPPEnabled = "zimbraXMPPEnabled";

    /**
     * object version (int)
     */
    public static final String A_zimbraVersion = "zimbraVersion";

    /**
     * If zimbraPublicServiceHostname is set for a mail domain, the name is used
     * when generating public accessible URLs such as REST URL.
     */
    public static final String A_zimbraPublicServiceHostname = "zimbraPublicServiceHostname";
    
    public static final String A_zimbraDomainMaxAccounts = "zimbraDomainMaxAccounts";
    
    public static final String A_zimbraSyncWindowSize = "zimbraSyncWindowSize";
    
    /*
     * admin saved searches
     */
    public static final String A_zimbraAdminSavedSearches = "zimbraAdminSavedSearches";

    /*
     * family mailboxes
     */
    public static final String A_zimbraChildAccount              = "zimbraChildAccount";
    public static final String A_zimbraPrefChildVisibleAccount   = "zimbraPrefChildVisibleAccount";
    public static final String A_zimbraFeatureOptionsEnabled     = "zimbraFeatureOptionsEnabled";

    /*
     * Backup/Restore
     */
    public static final String A_zimbraBackupTarget = "zimbraBackupTarget";
    public static final String A_zimbraBackupReportEmailRecipients = "zimbraBackupReportEmailRecipients";
    public static final String A_zimbraBackupReportEmailSender = "zimbraBackupReportEmailSender";
    public static final String A_zimbraBackupReportEmailSubjectPrefix = "zimbraBackupReportEmailSubjectPrefix";
    public static final String A_zimbraBackupMode = "zimbraBackupMode";
    public static final String A_zimbraBackupAutoGroupedInterval = "zimbraBackupAutoGroupedInterval";
    public static final String A_zimbraBackupAutoGroupedNumGroups = "zimbraBackupAutoGroupedNumGroups";
    public static final String A_zimbraBackupAutoGroupedThrottled = "zimbraBackupAutoGroupedThrottled";

    /*
     * IM
     */
    public static final String A_zimbraPrefIMFlashIcon      = "zimbraPrefIMFlashIcon";
    public static final String A_zimbraPrefIMNotifyPresence = "zimbraPrefIMNotifyPresence";
    public static final String A_zimbraPrefIMNotifyStatus   = "zimbraPrefIMNotifyStatus";
    public static final String A_zimbraPrefIMAutoLogin      = "zimbraPrefIMAutoLogin";
    public static final String A_zimbraPrefIMInstantNotify  = "zimbraPrefIMInstantNotify";
    public static final String A_zimbraPrefIMLogChats       = "zimbraPrefIMLogChats";
    public static final String A_zimbraIMAvailableInteropGateways = "zimbraIMAvailableInteropGateways";

    
    /*
     * spam report headers
     */
    public static final String A_zimbraSpamReportSenderHeader = "zimbraSpamReportSenderHeader";
    public static final String A_zimbraSpamReportTypeHeader   = "zimbraSpamReportTypeHeader";
    public static final String A_zimbraSpamReportTypeSpam     = "zimbraSpamReportTypeSpam";
    public static final String A_zimbraSpamReportTypeHam      = "zimbraSpamReportTypeHam";
    
    /*
     * proxy
     */
    
    public static final String A_zimbraReverseProxyLookupTarget         = "zimbraReverseProxyLookupTarget";
    public static final String A_zimbraReverseProxyAuthWaitInterval     = "zimbraReverseProxyAuthWaitInterval";
    public static final String A_zimbraReverseProxyMailHostQuery        = "zimbraReverseProxyMailHostQuery";
    public static final String A_zimbraReverseProxyMailHostSearchBase   = "zimbraReverseProxyMailHostSearchBase";
    public static final String A_zimbraReverseProxyMailHostAttribute    = "zimbraReverseProxyMailHostAttribute";
    public static final String A_zimbraReverseProxyPortQuery            = "zimbraReverseProxyPortQuery";
    public static final String A_zimbraReverseProxyPortSearchBase       = "zimbraReverseProxyPortSearchBase";
    public static final String A_zimbraReverseProxyPop3PortAttribute    = "zimbraReverseProxyPop3PortAttribute";
    public static final String A_zimbraReverseProxyPop3SSLPortAttribute = "zimbraReverseProxyPop3SSLPortAttribute";
    public static final String A_zimbraReverseProxyImapPortAttribute    = "zimbraReverseProxyImapPortAttribute";
    public static final String A_zimbraReverseProxyImapSSLPortAttribute = "zimbraReverseProxyImapSSLPortAttribute";
    public static final String A_zimbraReverseProxyDomainNameQuery      = "zimbraReverseProxyDomainNameQuery";
    public static final String A_zimbraReverseProxyDomainNameSearchBase = "zimbraReverseProxyDomainNameSearchBase";
    public static final String A_zimbraReverseProxyDomainNameAttribute  = "zimbraReverseProxyDomainNameAttribute";

    /*
     * whether to use <> or account's real address for out of office 
     * and new mail notifications 
     */
    public static final String A_zimbraAutoSubmittedNullReturnPath = "zimbraAutoSubmittedNullReturnPath";    

    /*
     * Cross mailbox search
     */
    public static final String A_zimbraExcludeFromCMBSearch = "zimbraExcludeFromCMBSearch"; 
    
    /*
     * Voice mail
     */
    public static final String A_zimbraPrefVoiceItemsPerPage = "zimbraPrefVoiceItemsPerPage";
    
    private static Provisioning sProvisioning;

    public synchronized static Provisioning getInstance() {
        if (sProvisioning == null) {
            String className = LC.zimbra_class_provisioning.value();
            if (className != null && !className.equals("")) {
                try {
                    sProvisioning = (Provisioning) Class.forName(className).newInstance();
                } catch (Exception e) {
                    ZimbraLog.account.error("could not instantiate Provisioning interface of class '" + className + "'; defaulting to LdapProvisioning", e);
                }
            }
            if (sProvisioning == null)
                sProvisioning = new LdapProvisioning();
        }
        return sProvisioning;
    }

    public synchronized static void setInstance(Provisioning prov) {
        if (sProvisioning != null)
            ZimbraLog.account.warn("duplicate call to Provisioning.setInstance()");
        sProvisioning = prov;
    }

    /**
     * Modifies this entry.  <code>attrs</code> is a <code>Map</code> consisting of
     * keys that are <code>String</code>s, and values that are either
     * <ul>
     *   <li><code>null</code>, in which case the attr is removed</li>
     *   <li>a single <code>Object</code>, in which case the attr is modified
     *     based on the object's <code>toString()</code> value</li>
     *   <li>an <code>Object</code> array or <code>Collection</code>,
     *     in which case a multi-valued attr is updated</li>
     * </ul>
     * 
     * Calls {@link #modifyAttrs(Map, boolean)} with <code>checkImmutable=false</code>.
     */
    public void modifyAttrs(Entry e, Map<String, ? extends Object> attrs) throws ServiceException
    {
        modifyAttrs(e, attrs, false);
    }
    
    /**
     * Modifies this entry.  <code>attrs</code> is a <code>Map</code> consisting of
     * keys that are <code>String</code>s, and values that are either
     * <ul>
     *   <li><code>null</code>, in which case the attr is removed</li>
     *   <li>a single <code>Object</code>, in which case the attr is modified
     *     based on the object's <code>toString()</code> value</li>
     *   <li>an <code>Object</code> array or <code>Collection</code>,
     *     in which case a multi-valued attr is updated</li>
     * </ul>
     */
    public abstract void modifyAttrs(Entry e,
                                     Map<String, ? extends Object> attrs,
                                     boolean checkImmutable)
    throws ServiceException;

    public abstract void modifyAttrs(Entry e,
                                     Map<String, ? extends Object> attrs,
                                     boolean checkImmutable,
                                     boolean allowCallback)
    throws ServiceException;

    /**
     * reload/refresh the entry.
     */
    public abstract void reload(Entry e) throws ServiceException;

    /**
     * @return the domain this account, or null if an admin account. 
     * @throws ServiceException
     */    
    public Domain getDomain(Account acct) throws ServiceException {
        String dname = acct.getDomainName();
        return dname == null ? null : get(DomainBy.name, dname);
    }

    /**
     * @return the Server object where this account's mailbox is homed
     * @throws ServiceException
     */
    public Server getServer(Account acct) throws ServiceException {
        String serverId = acct.getAttr(Provisioning.A_zimbraMailHost);
        return (serverId == null ? null : get(ServerBy.name, serverId));
    }  
    
    private static final String DATA_COS = "COS";
    
    /**
     * @return the COS object for this account, or null if account has no COS
     * 
     * @throws ServiceException
     */
    public Cos getCOS(Account acct) throws ServiceException {
        // CACHE. If we get reloaded from LDAP, cached data is cleared
        Cos cos = (Cos) acct.getCachedData(DATA_COS);
        if (cos == null) {
            String id = acct.getAccountCOSId();
                if (id != null) cos = get(CosBy.id, id); 
                if (cos == null) {
                    Domain domain = getDomain(acct);
                    String domainCosId = domain != null ? domain.getAttr(Provisioning.A_zimbraDomainDefaultCOSId, null) : null;
                    if (domainCosId != null) cos = get(CosBy.id, domainCosId);
                }
                if (cos == null) cos = get(CosBy.name, Provisioning.DEFAULT_COS_NAME);
                if (cos != null) acct.setCachedData(DATA_COS, cos);
        }
        return cos;
    }
    
    /**
     * @param zimbraId the zimbraId of the dl we are checking for
     * @return true if this account (or one of the dl it belongs to) is a member of the specified dl.
     * @throws ServiceException
     */
    public abstract boolean inDistributionList(Account acct, String zimbraId) throws ServiceException;

    /**
     * @return set of all the zimbraId's of lists this account belongs to, including any list in other list. 
     * @throws ServiceException
     */
    public abstract Set<String> getDistributionLists(Account acct) throws ServiceException; 

    /**
     *      
     * @param directOnly return only DLs this account is a direct member of
     * @param via if non-null and directOnly is false, this map will containing a mapping from a DL name to the DL it was a member of, if 
     *            member was indirect.
     * @return all the DLs
     * @throws ServiceException
     */
    public abstract List<DistributionList> getDistributionLists(Account acct, boolean directOnly, Map<String,String> via) throws ServiceException; 

    
    /**
     *      
     * @param directOnly return only DLs this DL is a direct member of
     * @param via if non-null and directOnly is false, this map will containing a mapping from a DL name to the DL it was a member of, if 
     *            member was indirect.
     * @return all the DLs
     * @throws ServiceException
     */
    public abstract List<DistributionList> getDistributionLists(DistributionList list, boolean directOnly, Map<String,String> via) throws ServiceException; 

    public abstract boolean healthCheck() throws ServiceException;

    public abstract Config getConfig() throws ServiceException;
    
    public abstract List<MimeTypeInfo> getMimeTypes(String mimeType) throws ServiceException;
    
    public abstract List<MimeTypeInfo> getAllMimeTypes() throws ServiceException;
    
    public abstract List<Zimlet> getObjectTypes() throws ServiceException;
    
    /**
     * Creates the specified account. The A_uid attribute is automatically
     * created and should not be passed in.
     * 
     * If A_zimbraId is passed in the attrs list, createAccount honors it if it is a valid uuid per RFC 4122. 
     * It is caller's responsibility to ensure the uuid passed in is unique in the namespace.  
     * createAccount does not check for uniqueness of the uuid passed in as an argument.
     *      
     * For example:
     * <pre>
     * HashMap attrs  = new HashMap();
     * attrs.put(Provisioning.A_sn, "Schemers");
     * attrs.put(Provisioning.A_cn, "Roland Schemers");
     * attrs.put(Provisioning.A_zimbraMailStatus, Provisioning.MAIL_STATUS_ENABLED);
     * attrs.put(Provisioning.A_zimbraMailHost, "server1");
     * attrs.put(Provisioning.A_zimbraMailDeliveryAddress, "roland@tiiq.net");        
     * prov.createAccount("roland@tiiq.net", "dsferulz", Provisioning.ACCOUNT_STATUS_ACTIVE, attrs);
     * </pre>
     * 
     * @param emailAddress email address (domain must already exist) of account being created.
     * @param password password of account being created, or null. Account's without passwords can't be logged into.
     * @param accountStatus the initial account status
     * @param attrs other initial attributes or <code>null</code>
     * @return
     * @throws ServiceException
     */
    public abstract Account createAccount(String emailAddress, String password, Map<String, Object> attrs) throws ServiceException;
    
    /**
     * deletes the specified account, removing the account and all email aliases.
     * does not remove any mailbox associated with the account.
     * @param zimbraId
     * @throws ServiceException
     */
    public abstract void deleteAccount(String zimbraId) throws ServiceException;

    /**
     * renames the specified account
     * @param zimbraId
     * @param newName
     * @throws ServiceException
     */
    public abstract void renameAccount(String zimbraId, String newName) throws ServiceException;

    public static enum AccountBy {
        
        // case must match protocol
        adminName, id, foreignPrincipal, name, krb5Principal;
        
        public static AccountBy fromString(String s) throws ServiceException {
            try {
                return AccountBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    }
    
    /**
     * Looks up an account by the specified key.
     * 
     * @return the <code>Account</code>, or <code>null</code> if no <code>Account</code>
     * with the given key exists.
     * @throws ServiceException if the key is malformed
     */
    public abstract Account get(AccountBy keyType, String key) throws ServiceException;
    
    /**
     * Like get(AccountBy keyType, String key), except that it takes an extra parameter:
     * loadFromMaster that specifies if the account is not in cache whether it should be 
     * loaded from the master store.
     * 
     * For implementations that do not support the loadFromMaster parameter, this method is 
     * equivalent to get(AccountBy keyType, String key).
     */
    public Account get(AccountBy keyType, String key, boolean loadFromMaster) throws ServiceException {
        return get(keyType, key);
    }

    /**
     * return regular accounts from searchAccounts;
     * calendar resource accounts are excluded
     */
    public static final int SA_ACCOUNT_FLAG = 0x1;
    
    /** return aliases from searchAccounts */
    public static final int SA_ALIAS_FLAG = 0x2;
    
    /** return distribution lists from searchAccounts */
    public static final int SA_DISTRIBUTION_LIST_FLAG = 0x4;

    /** return calendar resource accounts from searchAccounts */
    public static final int SA_CALENDAR_RESOURCE_FLAG = 0x8;
    
    /** return domains from searchAccounts. only valid with Provisioning.searchAccounts. */
    public static final int SA_DOMAIN_FLAG = 0x10;

    public static final String A_amavisBypassSpamChecks = "amavisBypassSpamChecks";

    public static final String A_amavisBypassVirusChecks = "amavisBypassVirusChecks";    

    /**
     * Takes a string repsrenting the objects to search for and returns a bit mask of SA_* flags for the given string.
     * The full set of objects is "accounts,aliases,distributionLists,resources,domains".
     * @param types
     * @return
     */
    public static int searchAccountStringToMask(String types) {
        int flags = 0;
        
        if (types.indexOf("accounts") != -1) flags |= Provisioning.SA_ACCOUNT_FLAG;
        if (types.indexOf("aliases") != -1) flags |= Provisioning.SA_ALIAS_FLAG;
        if (types.indexOf("distributionlists") != -1) flags |= Provisioning.SA_DISTRIBUTION_LIST_FLAG;
        if (types.indexOf("resources") != -1) flags |= Provisioning.SA_CALENDAR_RESOURCE_FLAG;
        if (types.indexOf("domains") != -1) flags |= Provisioning.SA_DOMAIN_FLAG;

        return flags;
    }
    
    public static String searchAccountMaskToString(int mask) {
        StringBuilder sb = new StringBuilder();
        if ( (mask & Provisioning.SA_ACCOUNT_FLAG) != 0) sb.append("accounts");
        if ( (mask & Provisioning.SA_ALIAS_FLAG) != 0) { if (sb.length() >0) sb.append(','); sb.append("aliases"); }
        if ( (mask & Provisioning.SA_DISTRIBUTION_LIST_FLAG) != 0) { if (sb.length() >0) sb.append(','); sb.append("distributionlists"); }                
        if ( (mask & Provisioning.SA_CALENDAR_RESOURCE_FLAG) != 0) { if (sb.length() >0) sb.append(','); sb.append("resources"); }
        if ( (mask & Provisioning.SA_DOMAIN_FLAG) != 0) { if (sb.length() >0) sb.append(','); sb.append("domains"); }        
        return sb.toString();
    }
        
    public static enum GAL_SEARCH_TYPE {
        ALL, USER_ACCOUNT, CALENDAR_RESOURCE
    }

    /**
     * @param query LDAP search query
     * @param returnAttrs list of attributes to return. uid is always included. null will return all attrs.
     * @param sortAttr attr to sort on. if null, sorting will be by account name.
     * @param sortAscending sort ascending (true) or descending (false).
     * @param flags - whether to addtionally return distribution lists and/or aliases
     * @return a list of all the accounts that matched.
     * @throws ServiceException
     */
    public abstract List<NamedEntry> searchAccounts(String query, String returnAttrs[], String sortAttr, boolean sortAscending, int flags) throws ServiceException;  

    public abstract List<Account> getAllAdminAccounts()  throws ServiceException;

    public abstract void setCOS(Account acct, Cos cos) throws ServiceException;
    
    public abstract void modifyAccountStatus(Account acct, String newStatus) throws ServiceException;

    public abstract void authAccount(Account acct, String password, String proto) throws ServiceException;
    
    public abstract void preAuthAccount(Account acct, String accountName, String accountBy, long timestamp, long expires, String preAuth) throws ServiceException;
    
    public abstract void changePassword(Account acct, String currentPassword, String newPassword) throws ServiceException;
    
    public abstract void setPassword(Account acct, String newPassword) throws ServiceException;

    public abstract void checkPasswordStrength(Account acct, String password) throws ServiceException;

    public abstract void addAlias(Account acct, String alias) throws ServiceException;
    
    public abstract void removeAlias(Account acct, String alias) throws ServiceException;

    /**
     *  Creates a zimbraDomain object in the directory. Also creates parent domains as needed (as simple dcObject entries though,
     *  not zimbraDomain objects). The extra attrs that can be passed in are:<p />
     * <dl>
     * <dt>description</dt>
     * <dd>textual description of the domain</dd>
     * <dt>zimbraNotes</dt>
     * <dd>additional notes about the domain</dd>
     * </dl>
     * <p />
     * @param name the domain name
     * @param attrs extra attrs
     * @return
     */
    public abstract Domain createDomain(String name, Map<String, Object> attrs) throws ServiceException;

    public static enum DomainBy {
        
        // case must match protocol
        id, name, virtualHostname, krb5Realm;
        
        public static DomainBy fromString(String s) throws ServiceException {
            try {
                return DomainBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    }
    
    public abstract Domain get(DomainBy keyType, String key) throws ServiceException;
    
    public abstract List<Domain> getAllDomains()  throws ServiceException;

    public abstract void deleteDomain(String zimbraId) throws ServiceException;

    public abstract Cos createCos(String name, Map<String, Object> attrs) throws ServiceException;

    public abstract void renameCos(String zimbraId, String newName) throws ServiceException;
    
    public static enum CosBy {
        
        // case must match protocol
        id, name;
        
        public static CosBy fromString(String s) throws ServiceException {
            try {
                return CosBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    
    }
    
    public abstract Cos get(CosBy keyType, String key) throws ServiceException;

    public abstract List<Cos> getAllCos()  throws ServiceException;
    
    public abstract void deleteCos(String zimbraId) throws ServiceException;
    
    public abstract Server getLocalServer() throws ServiceException;
    
    public static boolean onLocalServer(Account account) throws ServiceException {
        String target    = account.getAttr(Provisioning.A_zimbraMailHost);
        String localhost = getInstance().getLocalServer().getAttr(Provisioning.A_zimbraServiceHostname);
        return (target != null && target.equalsIgnoreCase(localhost));
    }

    public abstract Server createServer(String name, Map<String, Object> attrs) throws ServiceException;

    public static enum ServerBy {

        // case must match protocol
        id, name, serviceHostname;
        
        public static ServerBy fromString(String s) throws ServiceException {
            try {
                return ServerBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    
    }
    
    public abstract Server get(ServerBy keyName, String key) throws ServiceException;

    public abstract List<Server> getAllServers()  throws ServiceException;

    public abstract List<Server> getAllServers(String service)  throws ServiceException;
    
    public abstract void deleteServer(String zimbraId) throws ServiceException;

    public abstract DistributionList createDistributionList(String listAddress, Map<String, Object> listAttrs) throws ServiceException;

    public static enum DistributionListBy {
        
        // case must match protocol
        id, name;
        
        public static DistributionListBy fromString(String s) throws ServiceException {
            try {
                return DistributionListBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    
    }
    
    public abstract DistributionList get(DistributionListBy keyType, String key) throws ServiceException;
    
    public abstract void deleteDistributionList(String zimbraId) throws ServiceException;

    public abstract void addAlias(DistributionList dl, String alias) throws ServiceException;
    
    public abstract void removeAlias(DistributionList dl, String alias) throws ServiceException;

    public abstract void renameDistributionList(String zimbraId, String newName) throws ServiceException;
    
    public abstract Zimlet getZimlet(String name) throws ServiceException;
    
    public abstract List<Zimlet> listAllZimlets() throws ServiceException;
    
    public abstract Zimlet createZimlet(String name, Map<String, Object> attrs) throws ServiceException;
    
    public abstract void deleteZimlet(String name) throws ServiceException;
    
    /**
     * Creates the specified calendar resource. The A_zimbraId and A_uid attributes are automatically
     * created and should not be passed in.
     * 
     * For example:
     * <pre>
     * HashMap attrs  = new HashMap();
     * attrs.put(Provisioning.A_zimbraCalResType, "Location");
     * attrs.put(Provisioning.A_zimbraCalResAutoRespondEnabled, "TRUE");
     * prov.createCalendarResource("room-1001@domain.com", attrs);
     * </pre>
     * 
     * @param emailAddress email address (domain must already exist) of calendar resource being created.
     * @param attrs other initial attributes
     * @return
     * @throws ServiceException
     */
    public abstract CalendarResource createCalendarResource(String emailAddress, String password, Map<String, Object> attrs) throws ServiceException;

    /**
     * deletes the specified calendar resource, removing the account and all email aliases.
     * does not remove any mailbox associated with the resourceaccount.
     * @param zimbraId
     * @throws ServiceException
     */
    public abstract void deleteCalendarResource(String zimbraId) throws ServiceException;

    /**
     * renames the specified calendar resource
     * @param zimbraId
     * @param newName
     * @throws ServiceException
     */
    public abstract void renameCalendarResource(String zimbraId, String newName) throws ServiceException;

    public static enum CalendarResourceBy { 

        // case must match protocol
        id, foreignPrincipal, name;
        
        public static CalendarResourceBy fromString(String s) throws ServiceException {
            try {
                return CalendarResourceBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    
    }
    
    public abstract CalendarResource get(CalendarResourceBy keyType, String key) throws ServiceException;
    public CalendarResource get(CalendarResourceBy keyType, String key, boolean loadFromMaster) throws ServiceException {
        return get(keyType, key);
    }

    /**
     * @param filter search filter
     * @param returnAttrs list of attributes to return. uid is always included. null will return all attrs.
     * @param sortAttr attr to sort on. if null, sorting will be by account name.
     * @param sortAscending sort ascending (true) or descending (false).
     * @return a List of all the calendar resources that matched.
     * @throws ServiceException
     */
    public abstract List<NamedEntry> searchCalendarResources(EntrySearchFilter filter, String returnAttrs[], String sortAttr, boolean sortAscending) throws ServiceException;

    private static Locale getEntryLocale(Entry entry, String attr) {
        Locale lc = null;
        if (entry != null) {
            String lcName = entry.getAttr(attr);
            if (lcName != null)
                lc = L10nUtil.lookupLocale(lcName);
        }
        return lc;
    }
    
    private static Locale getEntryLocale(Entry entry) {
        return getEntryLocale(entry, A_zimbraLocale);
    }

    public Locale getLocale(Entry entry) throws ServiceException {
        if (entry instanceof Account) {
            // Order of precedence for Account's locale: (including
            // CalendarResource which extends Account)
            //
            // 1. zimbraPrefLocale set at Account level
            // 2. zimbraPrefLocale set at COS level
            // 3. locale set at Account level
            // 4. locale set at Account's COS level
            // 5. locale set at Account's domain level
            // 6. locale set at Account's Server level
            // 7. locale set at global Config level
            // 8. Locale.getDefault() of current JVM
            Account account = (Account) entry;
            Locale lc = getEntryLocale(account, A_zimbraPrefLocale);
            if (lc != null) return lc;
            Cos cos = getCOS(account);
            lc = getEntryLocale(cos, A_zimbraPrefLocale);
            if (lc != null) return lc;
            lc = getEntryLocale(account);
            if (lc != null) return lc;
            lc = getEntryLocale(cos);
            if (lc != null) return lc;
            lc = getEntryLocale(getDomain(account));
            if (lc != null) return lc;
            return getLocale(getServer(account));
        } else if (entry instanceof Server) {
            // Order of precedence for Server's locale:
            //
            // 1. locale set at Server level
            // 2. locale set at global Config level
            // 3. Locale.getDefault() of current JVM
            Locale lc = getEntryLocale(entry);
            if (lc != null) return lc;
            return getLocale(getInstance().getConfig());
        } else if (entry instanceof Config) {
            // Order of precedence for global Config's locale:
            //
            // 1. locale set at global Config level
            // 2. Locale.getDefault() of current JVM
            Locale lc = getEntryLocale(entry);
            if (lc != null) return lc;
            return Locale.getDefault();
        } else {
            // Order of precedence for locale of all other types of entries,
            // including COS and Domain:
            //
            // 1. locale set at entry level
            // 2. locale set at current Server level
            //    (server the code is executing on, since these entries don't
            //    have the notion of "home" server like Accounts do)
            // 3. locale set at global Config level
            // 4. Locale.getDefault() of current JVM
            Locale lc = getEntryLocale(entry);
            if (lc != null) return lc;
            return getLocale(getInstance().getLocalServer());
        }
    }

    public abstract List getAllAccounts(Domain d) throws ServiceException;
    
    public abstract void getAllAccounts(Domain d, NamedEntry.Visitor visitor) throws ServiceException;

    public abstract List getAllCalendarResources(Domain d) throws ServiceException;

    public abstract void getAllCalendarResources(Domain d, NamedEntry.Visitor visitor) throws ServiceException;

    public abstract List getAllDistributionLists(Domain d) throws ServiceException;
    
    /**
     * @param query LDAP search query
     * @param returnAttrs list of attributes to return. uid is always included.
     * @param sortAttr attr to sort on. if not specified, sorting will be by account name.
     * @param sortAscending sort ascending (true) or descending (false).
     * @return a list of all the accounts that matched.
     * @throws ServiceException
     */
    public abstract List<NamedEntry> searchAccounts(Domain d, String query, String returnAttrs[], String sortAttr, boolean sortAscending, int flags) throws ServiceException;  


    public static class SearchOptions {
        private Domain mDomain;
        private String mBase;
        private String mQuery;
        private String mReturnAttrs[];
        private String mSortAttr;
        private boolean mSortAscending;
        private int mFlags;
        private int mMaxResults;
        private boolean mConvertIDNToAscii;
        

        public Domain getDomain() {
            return mDomain;
        }

        public void setDomain(Domain domain) {
            mDomain = domain;
        }
        
        public String getBase() {
            return mBase;
        }

        public void setBase(String base) {
            mBase = base;
        }

        public String getQuery() {
            return mQuery;
        }

        public void setQuery(String query) {
            mQuery = query;
        }

        public String[] getReturnAttrs() {
            return mReturnAttrs;
        }

        public void setReturnAttrs(String[] returnAttrs) {
            mReturnAttrs = returnAttrs;
        }

        public String getSortAttr() {
            return mSortAttr;
        }

        public void setSortAttr(String sortAttr) {
            mSortAttr = sortAttr;
        }

        public boolean isSortAscending() {
            return mSortAscending;
        }

        public void setSortAscending(boolean sortAscending) {
            mSortAscending = sortAscending;
        }

        public int getFlags() {
            return mFlags;
        }

        public void setFlags(int flags) {
            mFlags = flags;
        }

        public int getMaxResults() {
            return mMaxResults;
        }

        public void setMaxResults(int maxResults) {
            mMaxResults = maxResults;
        }
        
        public boolean getConvertIDNToAscii() {
            return mConvertIDNToAscii;
        }
        
        public void setConvertIDNToAscii(boolean convertIDNToAscii) {
            mConvertIDNToAscii = convertIDNToAscii;
        }
    }

    public abstract List<NamedEntry> searchDirectory(SearchOptions options) throws ServiceException;

    /**
     * 
     * @param query LDAP search query
     * @param type address type to search
     * @return List of GalContact objects
     * @throws ServiceException
     */
    public abstract SearchGalResult searchGal(Domain d, String query,
                                     GAL_SEARCH_TYPE type,
                                     String token)
    throws ServiceException;
    
    
    public static class SearchGalResult {
        public String token;
        public List<GalContact> matches;
        public boolean hadMore; // for auto-complete only
    }

    /**
     * 
     * @param query LDAP search query
     * @param type address type to auto complete
     * @param limit max number to return
     * @return List of GalContact objects
     * @throws ServiceException
     */
    public abstract SearchGalResult autoCompleteGal(Domain d, String query, Provisioning.GAL_SEARCH_TYPE type, int limit) throws ServiceException;

    /**
     * @param filter search filter
     * @param returnAttrs list of attributes to return. uid is always included
     * @param sortAttr attr to sort on. if not specified, sorting will be by account name
     * @param sortAscending sort ascending (true) or descending (false)
     * @return a list of all calendar resources that matched
     * @throws ServiceException
     */
    public abstract List searchCalendarResources(
        Domain d,
        EntrySearchFilter filter,
        String returnAttrs[],
        String sortAttr,
        boolean sortAscending)
    throws ServiceException;

    public abstract void addMembers(DistributionList list, String[] members) throws ServiceException;
    
    public abstract void removeMembers(DistributionList list, String[] member) throws ServiceException;
 
    // identities
    public static enum IdentityBy {
        
        id, name;
        
        public static IdentityBy fromString(String s) throws ServiceException {
            try {
                return IdentityBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    }
    
    public Identity getDefaultIdentity(Account account) throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        Set<String> identityAttrs = AttributeManager.getInstance().getAttrsInClass(AttributeClass.identity);
        
        for (String name : identityAttrs) {
            String value = account.getAttr(name, null);
            if (value != null) attrs.put(name, value);            
        }
        if (attrs.get(A_zimbraPrefIdentityName) == null)
            attrs.put(A_zimbraPrefIdentityName, DEFAULT_IDENTITY_NAME);

        String fromAddress = (String) attrs.get(A_zimbraPrefFromAddress);
        String fromDisplay = (String) attrs.get(A_zimbraPrefFromDisplay);
        
        if (fromAddress == null || fromDisplay == null) { 
            InternetAddress ia = AccountUtil.getFriendlyEmailAddress(account);
            if (fromAddress == null) attrs.put(A_zimbraPrefFromAddress, ia.getAddress());
            if (fromDisplay == null) attrs.put(A_zimbraPrefFromDisplay, ia.getPersonal());
        }
        attrs.put(A_zimbraPrefIdentityId, account.getId());
        
        /*
         *   In 4.0 we had a boolean setting zimbraPrefForwardReplyInOriginalFormat, In 4.5,
         *   that has been obsoleted in favor of zimbraPrefForwardReplyFormat which is an
         *   enum whose values are text/html/same. The default identity needs to correctly
         *   initialize the new value, and it should probably take into account the value of
         *   zimbraPrefComposeFormat.
         */
        if (attrs.get(A_zimbraPrefForwardReplyFormat) == null) {
            boolean forwardReplyInOriginalFormat = account.getBooleanAttr(Provisioning.A_zimbraPrefForwardReplyInOriginalFormat, false);
            if (forwardReplyInOriginalFormat) {     
                attrs.put(A_zimbraPrefForwardReplyFormat, MAIL_FORWARDREPLY_FORMAT_SAME);
            } else {  
                String composeFormat = account.getAttr(Provisioning.A_zimbraPrefComposeFormat, null);
                if (composeFormat == null)
                    attrs.put(A_zimbraPrefForwardReplyFormat, MAIL_FORMAT_TEXT);
                else
                    attrs.put(A_zimbraPrefForwardReplyFormat, composeFormat);
            }
        }
        return new Identity(account, DEFAULT_IDENTITY_NAME, account.getId(), attrs);        
    }
    
    public abstract Identity createIdentity(Account account, String identityName, Map<String, Object> attrs) throws ServiceException;

    public abstract void modifyIdentity(Account account, String identityName, Map<String, Object> attrs) throws ServiceException;
    
    public abstract void deleteIdentity(Account account, String identityName) throws ServiceException;
    
    public abstract List<Identity> getAllIdentities(Account account) throws ServiceException;
    
    public abstract Identity get(Account account, IdentityBy keyType, String key) throws ServiceException;
    
    // signatures
    public static enum SignatureBy {
        
        id, name;
        
        public static SignatureBy fromString(String s) throws ServiceException {
            try {
                return SignatureBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    }
    
    public abstract Signature createSignature(Account account, String signatureName, Map<String, Object> attrs) throws ServiceException;
    
    public abstract void modifySignature(Account account, String signatureId, Map<String, Object> attrs) throws ServiceException;
    
    public abstract void deleteSignature(Account account, String signatureId) throws ServiceException;
    
    public abstract List<Signature> getAllSignatures(Account account) throws ServiceException;
    
    public abstract Signature get(Account account, SignatureBy keyType, String key) throws ServiceException;
    
    // data sources
    public static enum DataSourceBy {
        
        id, name;
        
        public static DataSourceBy fromString(String s) throws ServiceException {
            try {
                return DataSourceBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    }
    
    public abstract DataSource createDataSource(Account account, DataSource.Type type, String dataSourceName, Map<String, Object> attrs) throws ServiceException;
    public abstract DataSource createDataSource(Account account, DataSource.Type type, String dataSourceName, Map<String, Object> attrs, boolean passwdAlreadyEncrypted) throws ServiceException;
    
    public abstract void modifyDataSource(Account account, String dataSourceId, Map<String, Object> attrs) throws ServiceException;
    
    public abstract void deleteDataSource(Account account, String dataSourceId) throws ServiceException;
    
    public abstract List<DataSource> getAllDataSources(Account account) throws ServiceException;
    
    /**
     * Looks up a data source by the specified key.
     * 
     * @return the <code>DataSource</code>, or <code>null</code> if no <code>DataSource</code>
     * with the given key exists.
     * @throws ServiceException if the key is malformed
     */
    public abstract DataSource get(Account account, DataSourceBy keyType, String key) throws ServiceException;
    
    
    public static enum CacheEntryType {
        account,
        config,
        cos,
        domain,
        server,
        zimlet;
        
        public static CacheEntryType fromString(String s) throws ServiceException {
            try {
                return CacheEntryType.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown cache type: "+s, e);
            }
        }
    }
    
    public static enum CacheEntryBy {
        
        // case must match protocol
        id, name;
    }
    
    public static class CacheEntry {
        public CacheEntry(CacheEntryBy entryBy, String entryIdentity) {
            mEntryBy = entryBy;
            mEntryIdentity = entryIdentity;
        }
        public CacheEntryBy mEntryBy;
        public String mEntryIdentity;
    }
    
    /**
     * flush cache by entry type and optional specific e
     * 
     * @param type
     * @param entries
     * @throws ServiceException
     */
    public abstract void flushCache(CacheEntryType type, CacheEntry[] entries) throws ServiceException;


    /**
     * checks to make sure the specified address is a valid email address (addr part only, no personal part)
     *
     * @throws ServiceException
     */
    public static void validEmailAddress(String addr) throws ServiceException {
        try {
            InternetAddress ia = new InternetAddress(addr, true);
            // is this even needed?
            // ia.validate();
            if (ia.getPersonal() != null && !ia.getPersonal().equals(""))
                throw ServiceException.INVALID_REQUEST("invalid email address", null);
        } catch (AddressException e) {
            throw ServiceException.INVALID_REQUEST("invalid email address", e);
        }
    }

    public static void validDomainName(String domain) throws ServiceException {
        String email = "test" + "@" + domain;
        try {
            validEmailAddress(email);
        } catch (ServiceException e) {
            throw ServiceException.INVALID_REQUEST("invalid domain name " + domain, null);
        }
    }
}
