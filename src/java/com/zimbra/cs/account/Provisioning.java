
/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Sep 23, 2004
 *
 */
package com.zimbra.cs.account;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mime.MimeTypeInfo;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.L10nUtil;

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
     * Possible values for zimbraMailMode. "mixed" means web server should
     * authenticate in HTTPS and redirect to HTTP (useful if all clients are on
     * the intranet and you want only do authentication in the clear - TODO we
     * should add networks to not redirect to at some point which would be sweet -
     * that would mean that if you are in mixed mode and coming from a trusted
     * local network we would redirect you to http after login, but if you came
     * externally you would stay in https - one day we will do this.) "both"
     * says to run both https and http, and not do any redirects between the
     * two.
     */
    public enum MAIL_MODE { http, https, mixed, both }

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
     * auth mech to use. should be AM_ZIMBRA or AM_LDAP. 
     */
    public static final String A_zimbraAuthMech = "zimbraAuthMech";

    public static final String A_zimbraAuthFallbackToLocal = "zimbraAuthFallbackToLocal";

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

    public static final String A_zimbraLocale = "zimbraLocale";

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
     * For a zimbraDomain object, the domain's name (i.e., widgets.com) 
     */
    public static final String A_zimbraDomainName = "zimbraDomainName";

    /**
     * Whether a domain is local or alias.
     */
    public static final String A_zimbraDomainType = "zimbraDomainType";
    
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
     * the auto-generated sieve script
     */
    public static final String A_zimbraMailSieveScript = "zimbraMailSieveScript";

    public static final String A_zimbraLogHostname = "zimbraLogHostname";

    public static final String A_zimbraMessageCacheSize = "zimbraMessageCacheSize";
    
    public static final String A_zimbraMessageIdDedupeCacheSize = "zimbraMessageIdDedupeCacheSize";

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
    
    public static final String A_zimbraPrefTimeZoneId = "zimbraPrefTimeZoneId";
    public static final String A_zimbraPrefUseTimeZoneListInCalendar = "zimbraPrefUseTimeZoneListInCalendar";

    public static final String A_zimbraPrefCalendarNotifyDelegatedChanges = "zimbraPrefCalendarNotifyDelegatedChanges";
    
    public static final String A_zimbraPrefCalendarUseQuickAdd = "zimbraPrefCalendarUseQuickAdd";
    
    public static final String A_zimbraPrefCalendarInitialCheckedCalendars = "zimbraPrefCalendarInitialCheckedCalendars";

    /**
     * whether or not the signature is automatically included on outgoing email
     */
    public static final String A_zimbraPrefMailSignatureEnabled = "zimbraPrefMailSignatureEnabled";

    public static final String A_zimbraPrefMailForwardingAddress = "zimbraPrefMailForwardingAddress";

    public static final String A_zimbraPrefMailInitialSearch = "zimbraPrefMailInitialSearch";

    public static final String A_zimbraPrefGroupMailBy = "zimbraPrefGroupMailBy";

    public static final String A_zimbraPrefImapSearchFoldersEnabled = "zimbraPrefImapSearchFoldersEnabled";

    public static final String A_zimbraPrefIncludeSpamInSearch = "zimbraPrefIncludeSpamInSearch";

    public static final String A_zimbraPrefIncludeTrashInSearch = "zimbraPrefIncludeTrashInSearch";

    public static final String A_zimbraPrefMailItemsPerPage = "zimbraPrefMailItemsPerPage";

    public static final String A_zimbraPrefOutOfOfficeReply = "zimbraPrefOutOfOfficeReply";

    public static final String A_zimbraPrefOutOfOfficeReplyEnabled = "zimbraPrefOutOfOfficeReplyEnabled";

    public static final String A_zimbraPrefReplyToAddress = "zimbraPrefReplyToAddress";

    public static final String A_zimbraPrefUseKeyboardShortcuts = "zimbraPrefUseKeyboardShortcuts";

    public static final String A_zimbraPrefNewMailNotificationEnabled = "zimbraPrefNewMailNotificationEnabled";
    
    public static final String A_zimbraPrefNewMailNotificationAddress = "zimbraPrefNewMailNotificationAddress";
    
    public static final String A_zimbraPrefDedupeMessagesSentToSelf = "zimbraPrefDedupeMessagesSentToSelf";
    
    public static final String A_zimbraFeatureMailForwardingEnabled = "zimbraFeatureMailForwardingEnabled";

    public static final String A_zimbraFeatureMobileSyncEnabled = "zimbraFeatureMobileSyncEnabled";

    /**
     * administrative notes for an entry.
     */
    public static final String A_zimbraNotes = "zimbraNotes";

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
     * the mail .signature value
     */
    public static final String A_zimbraPrefMailSignature = "zimbraPrefMailSignature";
    
    /**
     * wether or not to save outgoing mail
     */
    public static final String A_zimbraPrefSaveToSent = "zimbraPrefSaveToSent";
    
    /**
     * where to save it
     */
    public static final String A_zimbraPrefSentMailFolder = "zimbraPrefSentMailFolder";
    
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

    public static final String A_zimbraLmtpAdvertisedName = "zimbraLmtpAdvertisedName";
    public static final String A_zimbraLmtpBindPort = "zimbraLmtpBindPort";
    public static final String A_zimbraLmtpBindOnStartup = "zimbraLmtpBindOnStartup";
    public static final String A_zimbraLmtpBindAddress = "zimbraLmtpBindAddress";
    public static final String A_zimbraLmtpNumThreads = "zimbraLmtpNumThreads";

    public static final String A_zimbraImapAdvertisedName = "zimbraImapAdvertisedName";
    public static final String A_zimbraImapBindPort = "zimbraImapBindPort";
    public static final String A_zimbraImapBindOnStartup = "zimbraImapBindOnStartup";
    public static final String A_zimbraImapBindAddress = "zimbraImapBindAddress";
    public static final String A_zimbraImapNumThreads = "zimbraImapNumThreads";        
    public static final String A_zimbraImapServerEnabled = "zimbraImapServerEnabled";        
    public static final String A_zimbraImapSSLBindPort = "zimbraImapSSLBindPort";
    public static final String A_zimbraImapSSLBindOnStartup = "zimbraImapSSLBindOnStartup";
    public static final String A_zimbraImapSSLBindAddress = "zimbraImapSSLBindAddress";
    public static final String A_zimbraImapSSLServerEnabled = "zimbraImapSSLServerEnabled";            
    public static final String A_zimbraImapCleartextLoginEnabled = "zimbraImapCleartextLoginEnabled";    
    public static final String A_zimbraImapEnabled = "zimbraImapEnabled";

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

    public static final String A_zimbraNotifyServerEnabled = "zimbraNotifyServerEnabled";
    public static final String A_zimbraNotifyBindAddress = "zimbraNotifyBindAddress";
    public static final String A_zimbraNotifyBindPort = "zimbraNotifyBindPort";
    public static final String A_zimbraNotifySSLServerEnabled = "zimbraNotifySSLServerEnabled";
    public static final String A_zimbraNotifySSLBindAddress = "zimbraNotifySSLBindAddress";
    public static final String A_zimbraNotifySSLBindPort = "zimbraNotifySSLBindPort";

    public static final String A_zimbraMailTrashLifetime = "zimbraMailTrashLifetime";
    public static final String A_zimbraMailSpamLifetime = "zimbraMailSpamLifetime";
    public static final String A_zimbraMailMessageLifetime = "zimbraMailMessageLifetime";
    public static final String A_zimbraContactMaxNumEntries = "zimbraContactMaxNumEntries";
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
    
    /**
     * Attributes for object type handlers.
     */
    public static final String A_zimbraObjectType               = "zimbraObjectType";
    public static final String A_zimbraObjectIndexingEnabled    = "zimbraObjectIndexingEnabled";
    public static final String A_zimbraObjectStoreMatched       = "zimbraObjectStoreMatched"; 
    public static final String A_zimbraObjectHandlerClass       = "zimbraObjectHandlerClass";
    public static final String A_zimbraObjectHandlerConfig      = "zimbraObjectHandlerConfig";

    public static final String A_zimbraTableMaintenanceMinRows      = "zimbraTableMaintenanceMinRows";
    public static final String A_zimbraTableMaintenanceMaxRows      = "zimbraTableMaintenanceMaxRows";
    public static final String A_zimbraTableMaintenanceOperation    = "zimbraTableMaintenanceOperation";
    public static final String A_zimbraTableMaintenanceGrowthFactor = "zimbraTableMaintenanceGrowthFactor";
    
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

    
    /*
     * Wiki
     */
    public static final String A_zimbraNotebookAccount = "zimbraNotebookAccount";
    
    private static Provisioning mProvisioning;

    public static Provisioning getInstance() {
        // TODO: config/property to allow for MySQL, etc.
        synchronized(Provisioning.class) {
            if (mProvisioning == null)
                mProvisioning = new LdapProvisioning();
            return mProvisioning;
        }
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
    public abstract void modifyAttrs(Entry e, Map<String, ? extends Object> attrs, boolean checkImmutable) throws ServiceException;
    
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
     * Returns account's time zone
     * @return
     * @throws ServiceException
     */
    public abstract ICalTimeZone getTimeZone(Account acct) throws ServiceException;

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
    
    public abstract MimeTypeInfo getMimeType(String name) throws ServiceException;
    
    public abstract MimeTypeInfo getMimeTypeByExtension(String ext) throws ServiceException;
    
    public abstract List<Zimlet> getObjectTypes() throws ServiceException;
    
    /**
     * Creates the specified account. The A_zimbraId and A_uid attributes are automatically
     * created and should not be passed in.
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
     * @param attrs other initial attributes
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
        adminName, id, foreignPrincipal, name;
        
        public static AccountBy fromString(String s) throws ServiceException {
            try {
                return AccountBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    }
    
    public abstract Account get(AccountBy keyType, String key) throws ServiceException;

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

    public abstract void authAccount(Account acct, String password) throws ServiceException;
    
    public abstract void preAuthAccount(Account acct, String accountName, String accountBy, long timestamp, long expires, String preAuth) throws ServiceException;
    
    public abstract void changePassword(Account acct, String currentPassword, String newPassword) throws ServiceException;
    
    public abstract void setPassword(Account acct, String newPassword) throws ServiceException;
    
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
        id, name, virtualHostname;
        
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

    public abstract List<WellKnownTimeZone> getAllTimeZones() throws ServiceException;

    public abstract WellKnownTimeZone getTimeZoneById(String tzId) throws ServiceException;

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
    public abstract CalendarResource createCalendarResource(String emailAddress, Map<String, Object> attrs) throws ServiceException;

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

    /**
     * @param filter search filter
     * @param returnAttrs list of attributes to return. uid is always included. null will return all attrs.
     * @param sortAttr attr to sort on. if null, sorting will be by account name.
     * @param sortAscending sort ascending (true) or descending (false).
     * @return a List of all the calendar resources that matched.
     * @throws ServiceException
     */
    public abstract List<NamedEntry> searchCalendarResources(EntrySearchFilter filter, String returnAttrs[], String sortAttr, boolean sortAscending) throws ServiceException;

    private static Locale getEntryLocale(Entry entry) {
        Locale lc = null;
        String lcName = entry.getAttr(A_zimbraLocale);
        if (lcName != null)
            lc = L10nUtil.lookupLocale(lcName);
        return lc;
    }

    public Locale getLocale(Entry entry) throws ServiceException {
        if (entry instanceof Account) {
            // Order of precedence for Account's locale: (including
            // CalendarResource which extends Account)
            //
            // 1. locale set at Account level
            // 2. locale set at Account's COS level
            // 3. locale set at Account's domain level
            // 4. locale set at Account's Server level
            // 5. locale set at global Config level
            // 6. Locale.getDefault() of current JVM
            Account account = (Account) entry;
            Locale lc = getEntryLocale(account);
            if (lc != null) return lc;
            lc = getEntryLocale(getCOS(account));
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

}
