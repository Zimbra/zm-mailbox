/*
 * Created on Sep 23, 2004
 *
 */
package com.liquidsys.coco.account;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.liquidsys.coco.account.ldap.LdapProvisioning;
import com.liquidsys.coco.mime.MimeTypeInfo;
import com.liquidsys.coco.service.ServiceException;

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
    public static final String GM_LIQUID = "liquid";
    
    /**
     * only use exteranl gal
     */    
    public static final String GM_LDAP = "ldap";
    
    /**
     * use both gals (combine results)
     */
    public static final String GM_BOTH = "both";

    /**
     * liquidAuthMech type of "liquid" means our own (use userPassword)
     */
    public static final String AM_LIQUID = "liquid";
    
    /**
     * liquidAuthMech type of "ldap" means use configured LDAP attrs
     * (liquidAuthLdapURL, liquidAuthLdapBindDn)
     */
    public static final String AM_LDAP = "ldap";
    
    /**
     * liquidAuthMech type of "ad" means use configured LDAP attrs
     * (liquidAuthLdapURL, liquidAuthLdapBindDn) for use with ActiveDirectory
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
    

    // attributes
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

    public static final String A_liquidAuthTokenKey = "liquidAuthTokenKey";

    /**
     * auth mech to use. should be AM_LIQUID or AM_LDAP. 
     */
    public static final String A_liquidAuthMech = "liquidAuthMech";
    
    /**
     * LDAP URL (ldap://ldapserver[:port]/ or ldaps://ldapserver[:port]/)
     */
    public static final String A_liquidAuthLdapURL = "liquidAuthLdapURL";

    /**
     *  expansions for bind dn string:
     * 
     * %n = username with @ (or without, if no @ was specified)
     * %u = username with @ removed
     * %d = domain as foo.com
     * %D = domain as dc=foo,dc=com
     * 
     * For active directory, where accounts in our system have a domain of test.liquidsys.com,
     * and accounts in active directory have liquidsys.com.
     * 
     * liquidAuthMech      ldap
     * liquidAuthLdapURL   ldap://exch1/
     * liquidAuthLdapDn    %n@liquidsys.com
     * 
     * configuring our own system to auth via an LDAP bind
     * 
     * liquidAuthMech       ldap
     * liquidAuthLdapURL    ldap://dogfood.liquidsys.com/
     * liquidAuthLdapUserDn uid=%u,ou=people,%D
     *
     */    
    public static final String A_liquidAuthLdapBindDn = "liquidAuthLdapBindDn";    
    
    /**
     * UUID for the entry
     */
    public static final String A_liquidId = "liquidId";
    
    /**
     * pointer to the aliased id
     */
    public static final String A_liquidAliasTargetId = "liquidAliasTargetId";    
    
    /**
     * the account's status (see ACCOUNT_STATUS_*). Must be "active" to allow logins.
     */
    public static final String A_liquidAccountStatus = "liquidAccountStatus";
    
    /**
     * compat mode for calendar
     */
    public static final String A_liquidCalendarCompatibilityMode = "liquidCalendarCompatibilityMode";
    
    /**
     * Default domain name to use in getAccountByNamde if no domain specified.
     */
    public static final String A_liquidDefaultDomainName = "liquidDefaultDomainName";

    
    /**
     * For a liquidDomain object, the domain's name (i.e., widgets.com) 
     */
    public static final String A_liquidDomainName = "liquidDomainName";

    /**
     * Whether a domain is local or alias.
     */
    public static final String A_liquidDomainType = "liquidDomainType";

    /**
     * multi-value attr. Each attr is the name of another attribute that is
     * allowed to be inherited from the cos attr to an account attr.
     */
    public static final String A_liquidCOSInheritedAttr = "liquidCOSInheritedAttr";
    
    public static final String A_liquidDomainInheritedAttr = "liquidDomainInheritedAttr";
    
    public static final String A_liquidServerInheritedAttr = "liquidServerInheritedAttr";
    
    public static final String A_liquidGalLdapURL = "liquidGalLdapURL";
    
    public static final String A_liquidGalLdapSearchBase = "liquidGalLdapSearchBase";
    
    public static final String A_liquidGalLdapBindDn = "liquidGalLdapBindDn";

    public static final String A_liquidGalLdapBindPassword = "liquidGalLdapBindPassword";

    public static final String A_liquidGalLdapFilter = "liquidGalLdapFilter";
    
    public static final String A_liquidGalLdapAttrMap = "liquidGalLdapAttrMap";    

    /**
     * global filters defs. Should be in the format: name:{filter-str}
     */
    public static final String A_liquidGalLdapFilterDef = "liquidGalLdapFilterDef";
    
    /**
     * max results to return from a gal search
     */
    public static final String A_liquidGalMaxResults = "liquidGalMaxResults";
    
    /**
     * GAL mode. should be internal, external, or both. 
     */
    public static final String A_liquidGalMode = "liquidGalMode";
    
    /**
     * set to true if an account is an admin account
     */
    public static final String A_liquidIsAdminAccount = "liquidIsAdminAccount";    

    /**
     * Set for entries (accounts/lists) in the directory that have a local address
     */
    public static final String A_liquidMailAddress = "liquidMailAddress";
    
    /**
     * Set for entries (accounts/lists) in the directory that have an alias
     */
    public static final String A_liquidMailAlias = "liquidMailAlias";    
    
    /**
     * Set to be the address that an entry's local address(es) ultimately resolve to.
     */
    public static final String A_liquidMailDeliveryAddress = "liquidMailDeliveryAddress";
    
    /**
     * one or more forwarding addresses for an entry. Used to implement mailing lists, as well as providing forwarding
     * for accounts.
     */
    public static final String A_liquidMailForwardingAddress = "liquidMailForwardingAddress";
    
    /**
     * Address to which this account's address must be rewritten.
     */
    public static final String A_liquidMailCanonicalAddress = "liquidMailCanonicalAddress";
    
    /**
     * Designates a catch all source address, used in whole domain
     * forwards to mailboxes or other domains.
     */
    public static final String A_liquidMailCatchAllAddress = "liquidMailCatchAllAddress";

    /**
     * Designates a catch all destination address, used in whole
     * domain forwards.
     */
    public static final String A_liquidMailCatchAllForwardingAddress = "liquidMailCatchAllForwardingAddress";
    
    /**
     * Designates a catch all canonical address, used in whole domain
     * addresss rewrites.
     */
    public static final String A_liquidMailCatchAllCanonicalAddress = "liquidMailCatchAllCanonicalAddress";

    /**
     * the host/ip address where a user's mailbox is located 
     */
    public static final String A_liquidMailHost = "liquidMailHost";

    /**
     * multi-value COS attr which is list of servers to provision users on when creating accounts
     */
    public static final String A_liquidMailHostPool = "liquidMailHostPool";

    /**
     * the mail status (MAIL_STATUS_*) for a given entry. Must be "enabled" to receive mail.
     */
    public static final String A_liquidMailStatus = "liquidMailStatus";
    
    /**
     * the quota (in bytes) of a mailbox.
     */
    public static final String A_liquidMailQuota = "liquidMailQuota";
    
    /**
     * the auto-generated sieve script
     */
    public static final String A_liquidMailSieveScript = "liquidMailSieveScript";
    
    public static final String A_liquidMtaAuthEnabled = "liquidMtaAuthEnabled";
    public static final String A_liquidMtaBlockedExtension = "liquidMtaBlockedExtension";
    public static final String A_liquidMtaCommonBlockedExtension = "liquidMtaCommonBlockedExtension";
    public static final String A_liquidMtaDnsLookupsEnabled = "liquidMtaDnsLookupsEnabled";
    public static final String A_liquidMtaMaxMessageSize = "liquidMtaMaxMessageSize";
    public static final String A_liquidMtaRelayHost = "liquidMtaRelayHost";
    public static final String A_liquidMtaTlsAuthOnly = "liquidMtaTlsAuthOnly";

    public static final String A_liquidPrefTimeZoneId = "liquidPrefTimeZoneId";
    public static final String A_liquidPrefUseTimeZoneListInCalendar = "liquidPrefUseTimeZoneListInCalendar";

    /**
     * whether or not the signature is automatically included on outgoing email
     */
    public static final String A_liquidPrefMailSignatureEnabled = "liquidPrefMailSignatureEnabled";

    public static final String A_liquidPrefMailInitialSearch = "liquidPrefMailInitialSearch";

    public static final String A_liquidPrefGroupMailBy = "liquidPrefGroupMailBy";

    public static final String A_liquidPrefImapSearchFoldersEnabled = "liquidPrefImapSearchFoldersEnabled";

    public static final String A_liquidPrefIncludeSpamInSearch = "liquidPrefIncludeSpamInSearch";

    public static final String A_liquidPrefIncludeTrashInSearch = "liquidPrefIncludeTrashInSearch";

    public static final String A_liquidPrefMailItemsPerPage = "liquidPrefMailItemsPerPage";

    public static final String A_liquidPrefOutOfOfficeReply = "liquidPrefOutOfOfficeReply";

    public static final String A_liquidPrefOutOfOfficeReplyEnabled = "liquidPrefOutOfOfficeReplyEnabled";

    public static final String A_liquidPrefReplyToAddress = "liquidPrefReplyToAddress";

    public static final String A_liquidPrefUseKeyboardShortcuts = "liquidPrefUseKeyboardShortcuts";

    public static final String A_liquidPrefNewMailNotificationEnabled = "liquidPrefNewMailNotificationEnabled";
    
    public static final String A_liquidPrefNewMailNotificationAddress = "liquidPrefNewMailNotificationAddress";
    
    public static final String A_liquidPrefDedupeMessagesSentToSelf = "liquidPrefDedupeMessagesSentToSelf";

    /**
     * administrative notes for an entry.
     */
    public static final String A_liquidNotes = "liquidNotes";

    /**
     * number of old passwords to keep, or 0 for no history.
     */
    public static final String A_liquidPasswordEnforceHistory = "liquidPasswordEnforceHistory";
    
    /**
     * old passwords, time:value
     */
    public static final String A_liquidPasswordHistory = "liquidPasswordHistory";
    
    /**
     * password is locked and can't be changed by user
     */
    public static final String A_liquidPasswordLocked = "liquidPasswordLocked";    
    
    /**
     * minimum password length
     */
    public static final String A_liquidPasswordMinLength = "liquidPasswordMinLength";
    
    /**
     * maximum password length. 0 means no limit.
     */
    public static final String A_liquidPasswordMaxLength = "liquidPasswordMaxLength";

    /**
     * minimum password lifetime in days. 0 means no limit.
     */
    public static final String A_liquidPasswordMinAge = "liquidPasswordMinAge";
    
    /**
     * maximum password lifetime in days. 0 means no limit.
     */
    public static final String A_liquidPasswordMaxAge = "liquidPasswordMaxAge";

    /**
     * modified time
     */
    public static final String A_liquidPasswordModifiedTime = "liquidPasswordModifiedTime";

    /**
     * must change on next auth
     */
    public static final String A_liquidPasswordMustChange = "liquidPasswordMustChange";
    
    /**
     * the mail .signature value
     */
    public static final String A_liquidPrefMailSignature = "liquidPrefMailSignature";
    
    /**
     * wether or not to save outgoing mail
     */
    public static final String A_liquidPrefSaveToSent = "liquidPrefSaveToSent";
    
    /**
     * where to save it
     */
    public static final String A_liquidPrefSentMailFolder = "liquidPrefSentMailFolder";
    
    /**
     * delete appointment invite (from our inbox) when we've replied to it?
     * TODO add to schema! 
     */
    public static final String A_liquidPrefDeleteInviteOnReply = "liquidPrefDeleteInviteOnReply";
    
    /**
     * for liquidGroup objects, the UUID (liquidId) of accounts that are a member of the group. 
     */
    public static final String A_liquidMember = "liquidMember";
    
    /**
     * An attribute present on accounts for each group the account is a member of. Its value
     * is the liquiId of the liquidGroup the account is a member of. 
     */
    public static final String A_liquidMemberOf = "liquidMemberOf";
    
    /**
     * for accounts, the liquidId of the COS that this account belongs to.
     */
    public static final String A_liquidCOSId = "liquidCOSId";


    public static final String A_liquidMailPort = "liquidMailPort";
    public static final String A_liquidMailSSLPort = "liquidMailSSLPort";
    public static final String A_liquidAdminPort = "liquidAdminPort";

    public static final String A_liquidSmtpHostname = "liquidSmtpHostname";
    public static final String A_liquidSmtpPort = "liquidSmtpPort";
    public static final String A_liquidSmtpTimeout = "liquidSmtpTimeout";

    public static final String A_liquidLmtpAdvertisedName = "liquidLmtpAdvertisedName";
    public static final String A_liquidLmtpBindPort = "liquidLmtpBindPort";
    public static final String A_liquidLmtpBindAddress = "liquidLmtpBindAddress";
    public static final String A_liquidLmtpNumThreads = "liquidLmtpNumThreads";

    public static final String A_liquidImapAdvertisedName = "liquidImapAdvertisedName";
    public static final String A_liquidImapBindPort = "liquidImapBindPort";
    public static final String A_liquidImapBindAddress = "liquidImapBindAddress";
    public static final String A_liquidImapNumThreads = "liquidImapNumThreads";        
    public static final String A_liquidImapServerEnabled = "liquidImapServerEnabled";        
    public static final String A_liquidImapSSLBindPort = "liquidImapSSLBindPort";
    public static final String A_liquidImapSSLBindAddress = "liquidImapSSLBindAddress";
    public static final String A_liquidImapSSLServerEnabled = "liquidImapSSLServerEnabled";            
    public static final String A_liquidImapCleartextLoginEnabled = "liquidImapCleartextLoginEnabled";    
    public static final String A_liquidImapEnabled = "liquidImapEnabled";

    public static final String A_liquidPop3AdvertisedName = "liquidPop3AdvertisedName";
    public static final String A_liquidPop3BindPort = "liquidPop3BindPort";
    public static final String A_liquidPop3BindAddress = "liquidPop3BindAddress";
    public static final String A_liquidPop3NumThreads = "liquidPop3NumThreads";
    public static final String A_liquidPop3ServerEnabled = "liquidPop3ServerEnabled";        
    public static final String A_liquidPop3SSLBindPort = "liquidPop3SSLBindPort";
    public static final String A_liquidPop3SSLBindAddress = "liquidPop3SSLBindAddress";
    public static final String A_liquidPop3SSLServerEnabled = "liquidPop3SSLServerEnabled";            
    public static final String A_liquidPop3CleartextLoginEnabled = "liquidPop3CleartextLoginEnabled";    
    public static final String A_liquidPop3Enabled = "liquidPop3Enabled";


    public static final String A_liquidMailTrashLifetime = "liquidMailTrashLifetime";
    public static final String A_liquidMailSpamLifetime = "liquidMailSpamLifetime";
    public static final String A_liquidMailMessageLifetime = "liquidMailMessageLifetime";
    public static final String A_liquidContactMaxNumEntries = "liquidContactMaxNumEntries";
    public static final String A_liquidAuthTokenLifetime = "liquidAuthTokenLifetime";
    public static final String A_liquidAdminAuthTokenLifetime = "liquidAdminAuthTokenLifetime";
    public static final String A_liquidMailMinPollingInterval =  "liquidMailMinPollingInterval";
    public static final String A_liquidPrefMailPollingInterval = "liquidPrefMailPollingInterval";
    public static final String A_liquidAccountClientAttr = "liquidAccountClientAttr";
    public static final String A_liquidSpamHeader = "liquidSpamHeader";
    public static final String A_liquidSpamHeaderValue = "liquidSpamHeaderValue";
    
    public static final String A_liquidLastLogonTimestamp = "liquidLastLogonTimestamp";
    public static final String A_liquidLastLogonTimestampFrequency = "liquidLastLogonTimestampFrequency";
    
    public static final String A_liquidAttachmentsBlocked = "liquidAttachmentsBlocked";
    public static final String A_liquidAttachmentsViewInHtmlOnly = "liquidAttachmentsViewInHtmlOnly";
    public static final String A_liquidAttachmentsIndexingEnabled = "liquidAttachmentsIndexingEnabled";    
    
    public static final String A_liquidAttachmentsScanEnabled = "liquidAttachmentsScanEnabled";
    public static final String A_liquidAttachmentsScanClass = "liquidAttachmentsScanClass";
    public static final String A_liquidAttachmentsScanURL = "liquidAttachmentsScanURL";
    
    public static final String A_liquidServiceHostname = "liquidServiceHostname";

    public static final String A_liquidRedoLogEnabled            = "liquidRedoLogEnabled";
    public static final String A_liquidRedoLogLogPath            = "liquidRedoLogLogPath";
    public static final String A_liquidRedoLogArchiveDir         = "liquidRedoLogArchiveDir";
    public static final String A_liquidRedoLogRolloverFileSizeKB = "liquidRedoLogRolloverFileSizeKB";
    public static final String A_liquidRedoLogFsyncIntervalMS    = "liquidRedoLogFsyncIntervalMS";

    public static final String A_liquidRedoLogProvider = "liquidRedoLogProvider";

    /**
     * Email address of the sender of a new email notification message.
     * @see com.liquidsys.coco.mailbox.MailboxManager#notifyIfNecessary
     */
    public static final String A_liquidNewMailNotificationFrom               = "liquidNewMailNotificationFrom";

    /**
     * Template used to generate the subject of a new email notification message.
     * @see com.liquidsys.coco.mailbox.MailboxManager#notifyIfNecessary
     */
    public static final String A_liquidNewMailNotificationSubject            = "liquidNewMailNotificationSubject";
    
    /**
     * Template used to generate the body of a new email notification message.
     * @see com.liquidsys.coco.mailbox.MailboxManager#notifyIfNecessary
     */
    public static final String A_liquidNewMailNotificationBody               = "liquidNewMailNotificationBody";
    
    /**
     * If set to false, Tomcat server will refuse end-user commands on SOAP
     * and LMTP interfaces, and allow only admin commands.
     */
    public static final String A_liquidUserServicesEnabled = "liquidUserServicesEnabled";

    /**
     * Attributes for mime type handlers.
     */
    public static final String A_liquidMimeType                 = "liquidMimeType";
    public static final String A_liquidMimeIndexingEnabled      = "liquidMimeIndexingEnabled";
    public static final String A_liquidMimeHandlerClass         = "liquidMimeHandlerClass";
    public static final String A_liquidMimeFileExtension        = "liquidMimeFileExtension";
    
    /**
     * Attributes for object type handlers.
     */
    public static final String A_liquidObjectType               = "liquidObjectType";
    public static final String A_liquidObjectIndexingEnabled    = "liquidObjectIndexingEnabled";
    public static final String A_liquidObjectStoreMatched       = "liquidObjectStoreMatched"; 
    public static final String A_liquidObjectHandlerClass       = "liquidObjectHandlerClass";
    public static final String A_liquidObjectHandlerConfig      = "liquidObjectHandlerConfig";

    public static final String A_liquidTableMaintenanceMinRows      = "liquidTableMaintenanceMinRows";
    public static final String A_liquidTableMaintenanceMaxRows      = "liquidTableMaintenanceMaxRows";
    public static final String A_liquidTableMaintenanceOperation    = "liquidTableMaintenanceOperation";
    public static final String A_liquidTableMaintenanceGrowthFactor = "liquidTableMaintenanceGrowthFactor";
    
    public static final String A_liquidSpamCheckEnabled = "liquidSpamCheckEnabled";
    public static final String A_liquidSpamKillPercent = "liquidSpamKillPercent";
    public static final String A_liquidSpamSubjectTag = "liquidSpamSubjectTag";
    public static final String A_liquidSpamTagPercent = "liquidSpamTagPercent";

    public static final String A_liquidVirusCheckEnabled = "liquidVirusCheckEnabled";
    public static final String A_liquidVirusWarnRecipient = "liquidVirusWarnRecipient";
    public static final String A_liquidVirusWarnAdmin = "liquidVirusWarnAdmin";
    public static final String A_liquidVirusBlockEncryptedArchive = "liquidVirusBlockEncryptedArchive";
    public static final String A_liquidVirusDefinitionsUpdateFrequency = "liquidVirusDefinitionsUpdateFrequency";
    
    public static final String A_liquidFileUploadMaxSize = "liquidFileUploadMaxSize";

    /**
     * Attributes for time zone objects
     */
    public static final String A_liquidTimeZoneStandardDtStart = "liquidTimeZoneStandardDtStart";
    public static final String A_liquidTimeZoneStandardOffset = "liquidTimeZoneStandardOffset";
    public static final String A_liquidTimeZoneStandardRRule = "liquidTimeZoneStandardRRule";
    public static final String A_liquidTimeZoneDaylightDtStart = "liquidTimeZoneDaylightDtStart";
    public static final String A_liquidTimeZoneDaylightOffset = "liquidTimeZoneDaylightOffset";
    public static final String A_liquidTimeZoneDaylightRRule = "liquidTimeZoneDaylightRRule";

    private static Provisioning mProvisioning;

    public static Provisioning getInstance() {
        // TODO: config/property to allow for MySQL, etc.
        if (mProvisioning == null) synchronized(Provisioning.class) {
            if (mProvisioning == null)
                mProvisioning = new LdapProvisioning();
        }
        return mProvisioning;
    }

    public abstract boolean healthCheck();

    public abstract Config getConfig() throws ServiceException;
    
    public abstract MimeTypeInfo getMimeType(String name) throws ServiceException;
    
    public abstract MimeTypeInfo getMimeTypeByExtension(String ext) throws ServiceException;
    
    public abstract List /*<ObjectType>*/ getObjectTypes() throws ServiceException;
    
    /**
     * Creates the specified account. The A_liquidId and A_uid attributes are automatically
     * created and should not be passed in.
     * 
     * For example:
     * <pre>
     * HashMap attrs  = new HashMap();
     * attrs.put(Provisioning.A_sn, "Schemers");
     * attrs.put(Provisioning.A_cn, "Roland Schemers");
     * attrs.put(Provisioning.A_liquidMailStatus, Provisioning.MAIL_STATUS_ENABLED);
     * attrs.put(Provisioning.A_liquidMailHost, "server1");
     * attrs.put(Provisioning.A_liquidMailDeliveryAddress, "roland@tiiq.net");        
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
     * @param remoteBindDn remote bind dn to use (i.e., uid=liquid,cn=admins,cn=liquid)
     * @param remoteBindPassword password for bind dn
     * @return account as created on the local sysem.
     * @throws ServiceException
     */
    public abstract Account copyAccount(String emailAddress, String remoteURL, 
            String remoteBindDn, String remoteBindPassword) throws ServiceException;

    /**
     * deletes the specified account, removing the account and all email aliases.
     * does not remove any mailbox associated with the account.
     * @param liquidId
     * @throws ServiceException
     */
    public abstract void deleteAccount(String liquidId) throws ServiceException;

    /**
     * renames the specified account
     * @param liquidId
     * @param newName
     * @throws ServiceException
     */
    public abstract void renameAccount(String liquidId, String newName) throws ServiceException;
    
    public abstract Account getAccountById(String liquidId) throws ServiceException;

    public abstract Account getAccountByName(String emailAddress) throws ServiceException;

    /**
     * @param query LDAP search query
     * @param returnAttrs list of attributes to return. uid is always included. null will return all attrs.
     * @param sortAttr attr to sort on. if null, sorting will be by account name.
     * @param sortAscending sort ascending (true) or descending (false).
     * @return an ArrayList of all the accounts that matched.
     * @throws ServiceException
     */
    public abstract ArrayList searchAccounts(String query, String returnAttrs[], String sortAttr, boolean sortAscending) throws ServiceException;  

    public abstract Account createAdminAccount(String name, String password, Map attrs) throws ServiceException;
    
    public abstract Account getAdminAccountByName(String name) throws ServiceException;
    
    public abstract List getAllAdminAccounts()  throws ServiceException;

    public abstract void setCOS(Account acct, Cos cos) throws ServiceException;
    
    public abstract void modifyAccountStatus(Account acct, String newStatus) throws ServiceException;

    public abstract void authAccount(Account acct, String password) throws ServiceException;
    
    public abstract void changePassword(Account acct, String currentPassword, String newPassword) throws ServiceException;
    
    public abstract void setPassword(Account acct, String newPassword) throws ServiceException;
    
    /**
     * create the specified alias for the given account. The alias must be the uid part only
     * 
     * @param alias
     * @throws ServiceException
     */
    public abstract void addAlias(Account acct, String alias) throws ServiceException;
    
    public abstract void removeAlias(Account acct, String alias) throws ServiceException;
 
    /**
     *  Creates a liquidDomain object in the directory. Also creates parent domains as needed (as simple dcObject entries though,
     *  not liquidDomain objects). The extra attrs that can be passed in are:<p />
     * <dl>
     * <dt>description</dt>
     * <dd>textual description of the domain</dd>
     * <dt>liquidNotes</dt>
     * <dd>additional notes about the domain</dd>
     * </dl>
     * <p />
     * @param name the domain name
     * @param attrs extra attrs
     * @return
     */
    public abstract Domain createDomain(String name, Map attrs) throws ServiceException;

    public abstract Domain getDomainById(String liquidId) throws ServiceException;

    public abstract Domain getDomainByName(String name) throws ServiceException;

    public abstract List getAllDomains()  throws ServiceException;

    public abstract void deleteDomain(String liquidId) throws ServiceException;
    
    public abstract Cos createCos(String name, Map attrs) throws ServiceException;

    public abstract void renameCos(String liquidId, String newName) throws ServiceException;
    
    public abstract Cos getCosById(String liquidId) throws ServiceException;

    public abstract Cos getCosByName(String name) throws ServiceException;

    public abstract List getAllCos()  throws ServiceException;
    
    public abstract void deleteCos(String liquidId) throws ServiceException;
    
    public abstract Server getLocalServer() throws ServiceException;
    
    public abstract Server createServer(String name, Map attrs) throws ServiceException;

    public abstract Server getServerById(String liquidId) throws ServiceException;

    public abstract Server getServerById(String liquidId, boolean reload) throws ServiceException;

    public abstract Server getServerByName(String name) throws ServiceException;

    public abstract Server getServerByName(String name, boolean reload) throws ServiceException;

    public abstract List getAllServers()  throws ServiceException;
    
    public abstract void deleteServer(String liquidId) throws ServiceException;

    public abstract List /*<WellKnownTimeZone>*/ getAllTimeZones() throws ServiceException;

    public abstract WellKnownTimeZone getTimeZoneById(String tzId) throws ServiceException;

    public abstract DistributionList createDistributionList(String listAddress, Map listAttrs) throws ServiceException;

    public abstract DistributionList getDistributionListById(String liquidId) throws ServiceException;
    
    public abstract DistributionList getDistributionListByName(String name) throws ServiceException;
    
    public abstract List getAllDistributionLists() throws ServiceException;

    public abstract void deleteDistributionList(String liquidId) throws ServiceException;

}
