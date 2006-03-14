
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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
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
import java.util.Map;

import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.mime.MimeTypeInfo;
import com.zimbra.cs.service.ServiceException;

/**
 * @author schemers
 *
 */
public abstract class Provisioning {

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
    public static final String A_postalAddress = "postalAddress";

    
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
     * compat mode for calendar
     */
    public static final String A_zimbraCalendarCompatibilityMode = "zimbraCalendarCompatibilityMode";
    
    /**
     * Default domain name to use in getAccountByNamde if no domain specified.
     */
    public static final String A_zimbraDefaultDomainName = "zimbraDefaultDomainName";

    
    public static final String A_zimbraDomainDefaultCOSId = "zimbraDomainDefaultCOSId";
    
    public static final String A_zimbraDomainAdminModifiableAttr = "zimbraDomainAdminModifiableAttr";

    /**
     * For a zimbraDomain object, the domain's name (i.e., widgets.com) 
     */
    public static final String A_zimbraDomainName = "zimbraDomainName";

    /**
     * Whether a domain is local or alias.
     */
    public static final String A_zimbraDomainType = "zimbraDomainType";

    /**
     * multi-value attr. Each attr is the name of another attribute that is
     * allowed to be inherited from the cos attr to an account attr.
     */
    public static final String A_zimbraCOSInheritedAttr = "zimbraCOSInheritedAttr";
    
    public static final String A_zimbraDomainInheritedAttr = "zimbraDomainInheritedAttr";
    
    public static final String A_zimbraServerInheritedAttr = "zimbraServerInheritedAttr";
    
    public static final String A_zimbraGalLdapURL = "zimbraGalLdapURL";
    
    public static final String A_zimbraGalLdapSearchBase = "zimbraGalLdapSearchBase";
    
    public static final String A_zimbraGalLdapBindDn = "zimbraGalLdapBindDn";

    public static final String A_zimbraGalLdapBindPassword = "zimbraGalLdapBindPassword";

    public static final String A_zimbraGalLdapFilter = "zimbraGalLdapFilter";
    
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
     * for zimbraGroup objects, the UUID (zimbraId) of accounts that are a member of the group. 
     */
    public static final String A_zimbraMember = "zimbraMember";
    
    /**
     * An attribute present on accounts for each group the account is a member of. Its value
     * is the liquiId of the zimbraGroup the account is a member of. 
     */
    public static final String A_zimbraMemberOf = "zimbraMemberOf";
    
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
    public static final String A_zimbraAdminPort = "zimbraAdminPort";

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


    public static final String A_zimbraMailTrashLifetime = "zimbraMailTrashLifetime";
    public static final String A_zimbraMailSpamLifetime = "zimbraMailSpamLifetime";
    public static final String A_zimbraMailMessageLifetime = "zimbraMailMessageLifetime";
    public static final String A_zimbraContactMaxNumEntries = "zimbraContactMaxNumEntries";
    public static final String A_zimbraAuthTokenLifetime = "zimbraAuthTokenLifetime";
    public static final String A_zimbraAdminAuthTokenLifetime = "zimbraAdminAuthTokenLifetime";
    public static final String A_zimbraMailMinPollingInterval =  "zimbraMailMinPollingInterval";
    public static final String A_zimbraPrefMailPollingInterval = "zimbraPrefMailPollingInterval";
    public static final String A_zimbraAccountClientAttr = "zimbraAccountClientAttr";
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
    public static final String A_zimbraCalResAlwaysFree           = "zimbraCalResAlwaysFree";

    private static Provisioning mProvisioning;

    public static Provisioning getInstance() {
        // TODO: config/property to allow for MySQL, etc.
        synchronized(Provisioning.class) {
            if (mProvisioning == null)
                mProvisioning = new LdapProvisioning();
            return mProvisioning;
        }
    }

    public abstract boolean healthCheck();

    public abstract Config getConfig() throws ServiceException;
    
    public abstract MimeTypeInfo getMimeType(String name) throws ServiceException;
    
    public abstract MimeTypeInfo getMimeTypeByExtension(String ext) throws ServiceException;
    
    public abstract List /*<ObjectType>*/ getObjectTypes() throws ServiceException;
    
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
    public abstract Account createAccount(String emailAddress, String password, Map attrs) throws ServiceException;
    
    /**
     * copy an account from the remote system to the local system. The local domain must already exist.
     * 
     * @param emailAddress
     * @param remoteURL remote ldap://host/ URL
     * @param remoteBindDn remote bind dn to use (i.e., uid=zimbra,cn=admins,cn=zimbra)
     * @param remoteBindPassword password for bind dn
     * @return account as created on the local sysem.
     * @throws ServiceException
     */
    public abstract Account copyAccount(String emailAddress, String remoteURL, 
            String remoteBindDn, String remoteBindPassword) throws ServiceException;

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
    
    public abstract Account getAccountById(String zimbraId) throws ServiceException;

    public abstract Account getAccountByName(String emailAddress) throws ServiceException;
    
    public abstract Account getAccountByForeignPrincipal(String principal) throws ServiceException;

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
    public abstract List searchAccounts(String query, String returnAttrs[], String sortAttr, boolean sortAscending, int flags) throws ServiceException;  

    public abstract Account createAdminAccount(String name, String password, Map attrs) throws ServiceException;
    
    public abstract Account getAdminAccountByName(String name) throws ServiceException;
    
    public abstract List getAllAdminAccounts()  throws ServiceException;

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
    public abstract Domain createDomain(String name, Map attrs) throws ServiceException;

    public abstract Domain getDomainById(String zimbraId) throws ServiceException;

    public abstract Domain getDomainByName(String name) throws ServiceException;

    public abstract List getAllDomains()  throws ServiceException;

    public abstract void deleteDomain(String zimbraId) throws ServiceException;
    
    public abstract Cos createCos(String name, Map attrs) throws ServiceException;

    public abstract void renameCos(String zimbraId, String newName) throws ServiceException;
    
    public abstract Cos getCosById(String zimbraId) throws ServiceException;

    public abstract Cos getCosByName(String name) throws ServiceException;

    public abstract List getAllCos()  throws ServiceException;
    
    public abstract void deleteCos(String zimbraId) throws ServiceException;
    
    public abstract Server getLocalServer() throws ServiceException;
    
    public abstract Server createServer(String name, Map attrs) throws ServiceException;

    public abstract Server getServerById(String zimbraId) throws ServiceException;

    public abstract Server getServerById(String zimbraId, boolean reload) throws ServiceException;

    public abstract Server getServerByName(String name) throws ServiceException;

    public abstract Server getServerByName(String name, boolean reload) throws ServiceException;

    public abstract List getAllServers()  throws ServiceException;
    
    public abstract void deleteServer(String zimbraId) throws ServiceException;

    public abstract List /*<WellKnownTimeZone>*/ getAllTimeZones() throws ServiceException;

    public abstract WellKnownTimeZone getTimeZoneById(String tzId) throws ServiceException;

    public abstract DistributionList createDistributionList(String listAddress, Map listAttrs) throws ServiceException;

    public abstract DistributionList getDistributionListById(String zimbraId) throws ServiceException;
    
    public abstract DistributionList getDistributionListByName(String name) throws ServiceException;
    
    public abstract void deleteDistributionList(String zimbraId) throws ServiceException;

    public abstract void addAlias(DistributionList dl, String alias) throws ServiceException;
    
    public abstract void removeAlias(DistributionList dl, String alias) throws ServiceException;

    public abstract void renameDistributionList(String zimbraId, String newName) throws ServiceException;
    
    public abstract Zimlet getZimlet(String name) throws ServiceException;
    
    public abstract List listAllZimlets() throws ServiceException;
    
    public abstract Zimlet createZimlet(String name, Map attrs) throws ServiceException;
    
    public abstract void deleteZimlet(String name) throws ServiceException;
    
    public abstract void addZimletToCOS(String zimlet, String cos) throws ServiceException;

    public abstract void removeZimletFromCOS(String zimlet, String cos) throws ServiceException;

    public abstract void updateZimletConfig(String zimlet, String config) throws ServiceException;
    
    public abstract void addAllowedDomains(String domains, String cos) throws ServiceException;

    public abstract void removeAllowedDomains(String domains, String cos) throws ServiceException;
    
    /**
     * Creates the specified calendar resource. The A_zimbraId and A_uid attributes are automatically
     * created and should not be passed in.
     * 
     * For example:
     * <pre>
     * HashMap attrs  = new HashMap();
     * attrs.put(Provisioning.A_zimbraCalResType, "ROOM");
     * attrs.put(Provisioning.A_zimbraCalResAutoRespondEnabled, "TRUE");
     * prov.createCalendarResource("room-1001@domain.com", attrs);
     * </pre>
     * 
     * @param emailAddress email address (domain must already exist) of calendar resource being created.
     * @param attrs other initial attributes
     * @return
     * @throws ServiceException
     */
    public abstract CalendarResource createCalendarResource(String emailAddress, Map attrs) throws ServiceException;

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

    public abstract CalendarResource getCalendarResourceById(String zimbraId) throws ServiceException;

    public abstract CalendarResource getCalendarResourceByName(String emailAddress) throws ServiceException;

    public abstract CalendarResource getCalendarResourceByForeignPrincipal(String foreignPrincipal) throws ServiceException;

    /**
     * @param query LDAP search query
     * @param returnAttrs list of attributes to return. uid is always included. null will return all attrs.
     * @param sortAttr attr to sort on. if null, sorting will be by account name.
     * @param sortAscending sort ascending (true) or descending (false).
     * @return a List of all the calendar resources that matched.
     * @throws ServiceException
     */
    public abstract List searchCalendarResources(String query, String returnAttrs[], String sortAttr, boolean sortAscending) throws ServiceException;
}
