/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
 * Created on Sep 23, 2004
 *
 */
package com.zimbra.cs.account;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightCommand;
import com.zimbra.cs.account.accesscontrol.RightModifier;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.account.names.NameUtil;
import com.zimbra.cs.mime.MimeTypeInfo;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.cs.extension.ExtensionUtil;

import javax.mail.internet.InternetAddress;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * @author schemers
 *
 */
public abstract class Provisioning extends ZAttrProvisioning {


    public final static String TRUE  = "TRUE";

    public final static String FALSE = "FALSE";

    public static final String DEFAULT_COS_NAME = "default";

    public static final String SERVICE_MAILBOX   = "mailbox";
    public static final String SERVICE_MEMCACHED = "memcached";

    /**
     * generate appts that try to be compatible with exchange
     */
    public static final String CAL_MODE_EXCHANGE = "exchange";

    /**
     * generate appts that try to follow the standard
     */
    public static final String CAL_MODE_STANDARD = "standard";

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
     * Used to store Active Directory account name for the user for free/busy
     * replication from Zimbra to Exchange.
     */
    public static final String FP_PREFIX_AD = "ad:";

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
     * the account is marked pending and shouldn't allow logins and/or access.
     *
     * Account behavior is like closed, except that when the status is being set to
     * pending, account addresses are not removed from distribution lists.
     * The use case is for hosted.  New account creation based on invites
     * that are not completed until user accepts TOS on account creation confirmation page.
     */
    public static final String ACCOUNT_STATUS_PENDING = "pending";

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
     * Possible values for zimbraMailMode and ZimbraReverseProxyMailMode. "mixed"
     * means web server should authenticate in HTTPS and redirect to HTTP (useful
     * if all clients are on the intranet and you want only do authentication in
     * the clear - TODO we should add networks to not redirect to at some point
     * which would be sweet - that would mean that if you are in mixed mode and
     * coming from a trusted local network we would redirect you to http after
     * login, but if you came externally you would stay in https - one day we
     * will do this.) "both" says to run both https and http, and not do any
     * redirects between the two.  "redirect" means the web server should listen
     * on both HTTP and HTTPS but redirect traffic on the HTTP port to HTTPS.
     */
    public enum MailMode {
        http, https, mixed, both, redirect;

        public static MailMode fromString(String s) throws ServiceException {
            try {
                return MailMode.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown mail mode: " + s, e);
            }
        }
    }

    /**
     * Possible values for zimbraMailReferMode
     */
    public static final String MAIL_REFER_MODE_ALWAYS = "always";
    public static final String MAIL_REFER_MODE_WRONGHOST = "wronghost";
    public static final String MAIL_REFER_MODE_REVERSE_PROXIED = "reverse-proxied";


    // attributes

    public static final String A_dc = "dc";

    /**
     * aliased object name. The dn that an alias points to
     */
    public static final String A_aliasedObjectName = "aliasedObjectName";



    public static final String LDAP_AM_NONE = "none";
    public static final String LDAP_AM_SIMPLE = "simple";
    public static final String LDAP_AM_KERBEROS5 = "kerberos5";

    public static final String DEFAULT_IDENTITY_NAME = "DEFAULT";

    public static final int MAX_ZIMBRA_ID_LEN = 127;

    private List<ProvisioningValidator> validators = new ArrayList<ProvisioningValidator>();

    private static Provisioning sProvisioning;

    public synchronized static Provisioning getInstance() {
        if (sProvisioning == null) {
            String className = LC.zimbra_class_provisioning.value();
            if (className != null && !className.equals("")) {
                try {
                    try {
                        sProvisioning = (Provisioning) Class.forName(className).newInstance();
                    } catch (ClassNotFoundException cnfe) {
                        // ignore and look in extensions
                        sProvisioning = (Provisioning) ExtensionUtil.findClass(className).newInstance();
                    }
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

    public boolean idIsUUID() {
        return true;
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

    public void modifyAttrs(Entry e,
            Map<String, ? extends Object> attrs,
            boolean checkImmutable,
            AuthToken authToken)
    throws ServiceException {
        modifyAttrs(e, attrs, checkImmutable);
    }

    public abstract void modifyAttrs(Entry e,
                                     Map<String, ? extends Object> attrs,
                                     boolean checkImmutable,
                                     boolean allowCallback)
    throws ServiceException;

    /**
     * reload/refresh the entry.
     *
     * (LdapProvisioning will reload the entry from the master)
     */
    public abstract void reload(Entry e) throws ServiceException;


    /**
     * reload/refresh the entry.
     *
     * @param e
     * @param fromMaster whether to reload the entry from the master
     * @throws ServiceException
     */
    public void reload(Entry e, boolean fromMaster) throws ServiceException {
        reload(e);
    }

    /**
     * @return the domain of this account, or null if an admin account.
     * @throws ServiceException
     */
    public Domain getDomain(Account acct) throws ServiceException {
        String dname = acct.getDomainName();
        boolean checkNegativeCache = (acct instanceof GuestAccount);
        return dname == null ? null : getDomain(DomainBy.name, dname, checkNegativeCache);
    }

    /**
     * @return the domain of this alias
     * @throws ServiceException
     */
    public Domain getDomain(Alias alias) throws ServiceException {
        String dname = alias.getDomainName();
        return dname == null ? null : getDomain(DomainBy.name, dname, false);
    }


    /**
     * @return the Server object where this account's mailbox is homed
     * @throws ServiceException
     */
    public Server getServer(Account acct) throws ServiceException {
        String serverId = acct.getAttr(Provisioning.A_zimbraMailHost);
        return (serverId == null ? null : get(ServerBy.name, serverId));
    }



    /**
     * @return the COS object for this account, or null if account has no COS
     *
     * @throws ServiceException
     */
    public Cos getCOS(Account acct) throws ServiceException {
        // CACHE. If we get reloaded from LDAP, cached data is cleared
        Cos cos = (Cos) acct.getCachedData(EntryCacheDataKey.ACCOUNT_COS);
        if (cos == null) {
            String id = acct.getCOSId();
                if (id != null) cos = get(CosBy.id, id);
                if (cos == null) {
                    Domain domain = getDomain(acct);
                    String domainCosId = domain != null ? domain.getAttr(Provisioning.A_zimbraDomainDefaultCOSId, null) : null;
                    if (domainCosId != null) cos = get(CosBy.id, domainCosId);
                }
                if (cos == null) cos = get(CosBy.name, Provisioning.DEFAULT_COS_NAME);
                if (cos != null) acct.setCachedData(EntryCacheDataKey.ACCOUNT_COS, cos);
        }
        return cos;
    }

    public String getEmailAddrByDomainAlias(String emailAddress) throws ServiceException {
        String addr = null;

        String parts[] = emailAddress.split("@");
        if (parts.length == 2) {
            Domain domain = getDomain(DomainBy.name, parts[1], true);
            if (domain != null) {
                String domainType = domain.getAttr(A_zimbraDomainType);
                if (DOMAIN_TYPE_ALIAS.equals(domainType)) {
                    String targetDomainId = domain.getAttr(A_zimbraDomainAliasTargetId);
                    if (targetDomainId != null) {
                        domain = getDomainById(targetDomainId);
                        if (domain != null) {
                            addr = parts[0] + "@" + domain.getName();
                        }
                    }
                }
            }
        }

        return addr;
    }

    /**
     * @param zimbraId the zimbraId of the dl we are checking for
     * @return true if this account (or one of the dl it belongs to) is a member of the specified dl.
     * @throws ServiceException
     */
    public abstract boolean inDistributionList(Account acct, String zimbraId) throws ServiceException;

    /**
     * @param zimbraId the zimbraId of the dl we are checking for
     * @return true if this distribution list (or one of the dl it belongs to) is a member of the specified dl.
     * @throws ServiceException
     */
    public boolean inDistributionList(DistributionList list, String zimbraId) throws ServiceException {
        throw ServiceException.FAILURE("unsupported", null);
    }

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


    //
    // AclGroup
    //
    public DistributionList getAclGroup(DistributionListBy keyType, String key) throws ServiceException {
        throw ServiceException.FAILURE("unsupported", null);
    }


    /**
     * represents a super group in which the perspective object(account, cr, dl) is
     * directly or indirectly a member of
     */
    public static class MemberOf {
        private String mId;            // zimbraId of this group
        private boolean mIsAdminGroup; // is this group is an admin group (zimbraIsAdminGroup == TRUE)

        public MemberOf(String id, boolean isAdminGroup) {
            mId = id;
            mIsAdminGroup = isAdminGroup;
        }

        public String getId() {
            return mId;
        }

        public boolean isAdminGroup() {
            return mIsAdminGroup;
        }
    }

    public static class AclGroups {
        List<MemberOf> mMemberOf;  // list of MemberOf
        List<String> mGroupIds;    // list of group ids

        public AclGroups(List<MemberOf> memberOf, List<String> groupIds) {
            mMemberOf = memberOf;
            mGroupIds = groupIds;
        }

        // create an empty AclGroups
        public AclGroups() {
            this(new ArrayList<MemberOf>(), new ArrayList<String>());
        }

        public List<MemberOf> memberOf() {
            return mMemberOf;
        }

        public List<String> groupIds() {
            return mGroupIds;
        }
    }

    /**
     *
     * @param acct
     * @param adminGroupsOnly return admin groups only
     * @return List of all direct and indirect groups this account belongs to.
     *         The returned List is sorted by "shortest distance" to the account,
     *         the sorter the distance is, the earlier it appears in the returned List.
     * @throws ServiceException
     */
    public AclGroups getAclGroups(Account acct, boolean adminGroupsOnly) throws ServiceException {
        throw ServiceException.FAILURE("unsupported", null);
    }

    /**
     *
     * @param list
     * @param adminGroupsOnly return admin groups only
     * @return List of all the zimbraId's of lists this list belongs to, including any list in other list.
     *         The returned List is sorted by "shortest distance" to the list, the sorter the distance is,
     *         the earlier it appears in the returned List.
     * @throws ServiceException
     */
    public AclGroups getAclGroups(DistributionList list, boolean adminGroupsOnly) throws ServiceException {
        throw ServiceException.FAILURE("unsupported", null);
    }

    /**
     * @return the domain of the distribution list
     * @throws ServiceException
     */
    public Domain getDomain(DistributionList dl) throws ServiceException {
        String dname = dl.getDomainName();
        return dname == null ? null : get(DomainBy.name, dname);
    }

    public abstract boolean healthCheck() throws ServiceException;

    public abstract Config getConfig() throws ServiceException;

    public abstract GlobalGrant getGlobalGrant() throws ServiceException;

    /**
     * Looks up <tt>MimeTypeInfo</tt> objects by type.
     * @return the MIME types, or an empty <tt>List</tt> if none match
     */
    public abstract List<MimeTypeInfo> getMimeTypes(String mimeType) throws ServiceException;

    /**
     * Returns all <tt>MimeTypeInfo</tt> objects.
     * @return the MIME types, or an empty <tt>List</tt> if none exist
     */
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
     *
     * @param emailAddress
     * @param password
     * @param attrs attributes to set while creating the account entry
     * @param origAttrs original attributes when the account was backed, this is for license checking purpose,
     *                  none of the attrs in the origAttrs map will be set while creating the account entry.
     * @return
     * @throws ServiceException
     */
    public abstract Account restoreAccount(String emailAddress, String password,
            Map<String, Object> attrs, Map<String, Object> origAttrs) throws ServiceException;

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
        adminName, appAdminName, id, foreignPrincipal, name, krb5Principal;

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

    public Account getAccountByName(String name) throws ServiceException { return get(AccountBy.name, name); }
    public Account getAccountById(String id) throws ServiceException { return get(AccountBy.id, id); }
    public Account getAccountByAppAdminName(String name) throws ServiceException { return get(AccountBy.appAdminName, name); }
    public Account getAccountByForeignPrincipal(String name) throws ServiceException { return get(AccountBy.foreignPrincipal, name); }
    public Account getAccountByKrb5Principal(String name) throws ServiceException { return get(AccountBy.krb5Principal, name); }

    /**
     * Looks up an account by the specified key.
     *
     * For implementations that do not support the loadFromMaster parameter, this method is
     * equivalent to get(AccountBy keyType, String key).
     *
     * @param keyType
     * @param key
     * @param loadFromMaster whether the account should be loaded from the master directory if the account is not in cache.
     * @return
     * @throws ServiceException
     */
    public Account get(AccountBy keyType, String key, boolean loadFromMaster) throws ServiceException {
        return get(keyType, key);
    }

    /**
     * For Yahoo CalendarProvisioning.
     *
     * The Yahoo paranoids normally want them to do what is called a "credentialed open" when they access UDB.
     * That means they want the UDB client to pass in the Y&T cookies to UDB, so UDB will also double check
     * them. This helps to prevent servers from accidentally responding to a request without properly check
     * credentials and/or operating on the wrong account.
     *
     */
    public Account get(AccountBy keyType, String key, AuthToken authToken) throws ServiceException {
        return get(keyType, key);
    }

    public Account get(AccountBy keyType, String key, boolean loadFromMaster, AuthToken authToken) throws ServiceException {
        return get(keyType, key, loadFromMaster);
    }

    /**
     * Get account by id first, if not found, get by name.
     *
     * Note, this function might do an extra LDAP search, it should only be called from the edge of
     * CLI tools where the call rate is very low.
     *
     * @param key account id or name
     * @return
     */
    public Account getAccount(String key) throws ServiceException {
        Account acct = null;

        if (Provisioning.isUUID(key))
            acct = get(AccountBy.id, key);
        else {
            // could be id or name, try name first, if not found then try id
            acct = get(AccountBy.name, key);
            if (acct == null)
                acct = get(AccountBy.id, key);
        }

        return acct;
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

    /** return coses from searchDirectory */
    public static final int SD_COS_FLAG = 0x20;

    /** do not fixup objectclass in query for searchObject, should only be used from LdapUpgrade */
    public static final int SO_NO_FIXUP_OBJECTCLASS = 0x40;

    /** do not fixup return attrs for searchObject, should only be used from LdapUpgrade */
    public static final int SO_NO_FIXUP_RETURNATTRS = 0x80;

    /**
     *  do not set account defaults in makeAccount
     *
     *  bug 36017, 41533
     *
     *  only used from the admin SearchDirectory and GetQuotaUsage SOAPs, where large number of accounts are
     *  returned from Provisioning.searchDirectory.  In the extreme case where the accounts
     *  span many different domains, the admin console UI/zmprov would seem to be be unresponsive.
     *
     *  Domain is needed for:
     *    - determine the cos if cos is not set on the account
     *    - account secondary default
     *
     *  Caller is responsible for setting the defaults when it needs them.
     */
    public static final int SO_NO_ACCOUNT_DEFAULTS = 0x100;            // do not set defaults and secondary defaults in makeAccount
    public static final int SO_NO_ACCOUNT_SECONDARY_DEFAULTS = 0x200;  // do not set secondary defaults in makeAccount

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

    public abstract void authAccount(Account acct, String password, AuthContext.Protocol proto) throws ServiceException;

    public abstract void authAccount(Account acct, String password, AuthContext.Protocol proto, Map<String, Object> authCtxt) throws ServiceException;

    public void accountAuthed(Account acct) throws ServiceException {
        // noop by default
    }

    public void preAuthAccount(Account acct, String accountName, String accountBy, long timestamp, long expires,
                                        String preAuth,
                                        boolean admin,
                                        Map<String, Object> authCtxt) throws ServiceException
    {
        if (admin)
            throw ServiceException.FAILURE("preAuthAccount unimplemented", null);
        else
            preAuthAccount(acct, accountName, accountBy, timestamp, expires, preAuth, authCtxt);
    }

    public abstract void preAuthAccount(Account acct, String accountName, String accountBy, long timestamp, long expires, String preAuth, Map<String, Object> authCtxt) throws ServiceException;

    public abstract void changePassword(Account acct, String currentPassword, String newPassword) throws ServiceException;

    public abstract void setPassword(Account acct, String newPassword) throws ServiceException;

    public abstract void checkPasswordStrength(Account acct, String password) throws ServiceException;

    public abstract void addAlias(Account acct, String alias) throws ServiceException;

    public abstract void removeAlias(Account acct, String alias) throws ServiceException;

    /**
     * search alias target - will always do a search
     *
     * @param alias
     * @param mustFind
     * @return
     * @throws ServiceException
     */
    public NamedEntry searchAliasTarget(Alias alias, boolean mustFind) throws ServiceException {
        String targetId = alias.getAttr(Provisioning.A_zimbraAliasTargetId);
        SearchOptions options = new SearchOptions();

        int flags = 0;

        flags |= Provisioning.SA_ACCOUNT_FLAG;
        flags |= Provisioning.SA_CALENDAR_RESOURCE_FLAG;
        flags |= Provisioning.SA_DISTRIBUTION_LIST_FLAG;

        String query = "(" + Provisioning.A_zimbraId + "=" + targetId + ")";

        options.setFlags(flags);
        options.setQuery(query);

        List<NamedEntry> entries = searchDirectory(options);

        if (mustFind && entries.size() == 0)
            throw ServiceException.FAILURE("target " + targetId + " of alias " +  alias.getName() + " not found " + query, null);

        if (entries.size() > 1)
            throw AccountServiceException.TOO_MANY_SEARCH_RESULTS("too many results for search " + query, null);

        if (entries.size() == 0)
            return null;
        else
            return entries.get(0);
    }

    /**
     * search alias target - implementation can return cached entry
     *
     * @param alias
     * @param mustFind
     * @return
     * @throws ServiceException
     */
    public NamedEntry getAliasTarget(Alias alias, boolean mustFind) throws ServiceException {
        return searchAliasTarget(alias, mustFind);
    }

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

    /**
     * @param keyType
     * @param key
     * @param checkNegativeCache whether to check the negative domain cache
     *                           if set to true, and if key is found in the
     *                           negative cache, then no LDAP search will be
     *                           issued
     * @return
     * @throws ServiceException
     */
    public Domain getDomain(DomainBy keyType, String key, boolean checkNegativeCache) throws ServiceException {
        return get(keyType, key);
    }

    public Domain getDomainByName(String name) throws ServiceException { return get(DomainBy.name, name); }
    public Domain getDomainById(String id) throws ServiceException { return get(DomainBy.id, id); }
    public Domain getDomainByVirtualHostname(String host) throws ServiceException { return get(DomainBy.virtualHostname, host); }
    public Domain getDomainByKrb5Realm(String realm) throws ServiceException { return get(DomainBy.krb5Realm, realm); }

    public abstract List<Domain> getAllDomains()  throws ServiceException;

    public void getAllDomains(NamedEntry.Visitor visitor, String[] retAttrs) throws ServiceException {
        throw ServiceException.FAILURE("unsupported", null);
    }

    public abstract void deleteDomain(String zimbraId) throws ServiceException;

    public abstract Cos createCos(String name, Map<String, Object> attrs) throws ServiceException;

    public abstract Cos copyCos(String srcCosId, String destCosName) throws ServiceException;

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

    public Cos getCosByName(String name) throws ServiceException { return get(CosBy.name, name); }
    public Cos getCosById(String id) throws ServiceException { return get(CosBy.id, id); }

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

    public Server getServerByName(String name) throws ServiceException { return get(ServerBy.name, name); }
    public Server getServerById(String id) throws ServiceException { return get(ServerBy.id, id); }
    public Server getServerByServiceHostname(String name) throws ServiceException { return get(ServerBy.serviceHostname, name); }

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

    public DistributionList getDistributionListByName(String name) throws ServiceException { return get(DistributionListBy.name, name); }
    public DistributionList getDistributionListById(String id) throws ServiceException { return get(DistributionListBy.id, id); }

    public abstract void deleteDistributionList(String zimbraId) throws ServiceException;

    public abstract void addAlias(DistributionList dl, String alias) throws ServiceException;

    public abstract void removeAlias(DistributionList dl, String alias) throws ServiceException;

    public abstract void renameDistributionList(String zimbraId, String newName) throws ServiceException;

    public abstract Zimlet getZimlet(String name) throws ServiceException;

    public abstract List<Zimlet> listAllZimlets() throws ServiceException;

    public abstract Zimlet createZimlet(String name, Map<String, Object> attrs) throws ServiceException;

    public abstract void deleteZimlet(String name) throws ServiceException;

    public static enum ZimletBy {

        // case must match protocol
        id, name;

        public static ZimletBy fromString(String s) throws ServiceException {
            try {
                return ZimletBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }

    }

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

    public CalendarResource getCalendarResourceByName(String name) throws ServiceException { return get(CalendarResourceBy.name, name); }
    public CalendarResource getCalendarResourceById(String id) throws ServiceException { return get(CalendarResourceBy.id, id); }

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

    public abstract void getAllAccounts(Domain d, Server s, NamedEntry.Visitor visitor) throws ServiceException;

    public abstract List getAllCalendarResources(Domain d) throws ServiceException;

    public abstract void getAllCalendarResources(Domain d, NamedEntry.Visitor visitor) throws ServiceException;

    public abstract void getAllCalendarResources(Domain d, Server s, NamedEntry.Visitor visitor) throws ServiceException;

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
     * Search for all accunts on the server
     *
     * @param server
     * @param opts  note: query in opts is ignored
     * @param visitor
     * @throws ServiceException
     */
    public void searchAccountsOnServer(Server server, SearchOptions opts, NamedEntry.Visitor visitor) throws ServiceException {
        throw ServiceException.FAILURE("unsupported", null);
    }

    public static class SearchOptions {
        // pseudo attr name for target name
        // honored only for Alias entries
        public static final String SORT_BY_TARGET_NAME = "targetName";

        private Domain mDomain;
        private String mBase;
        private String mQuery;
        private String mReturnAttrs[];
        private String mSortAttr;
        private boolean mSortAscending;
        private int mFlags;
        private int mMaxResults;
        private boolean mConvertIDNToAscii;
        private boolean mOnMaster;

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

        public boolean getOnMaster() {
            return mOnMaster;
        }

        public void setOnMaster(boolean onMaster) {
            mOnMaster = onMaster;
        }
    }

    public abstract List<NamedEntry> searchDirectory(SearchOptions options) throws ServiceException;

    public enum GalMode {
        zimbra, // only use internal
        ldap,   // only use exteranl gal
        both;   // use both gals (combine results)

        public static GalMode fromString(String s) throws ServiceException {
            try {
                if (s == null)
                    return null;
                return GalMode.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown gal mode: " + s, e);
            }
        }
    }

    public static class SearchGalResult {

         private String mToken;
         private boolean mHadMore; // for auto-complete only
         private List<GalContact> mMatches;

        /*
         * for auto-complete and search only
         *
         * The Ajax client backtracks on GAL results assuming the results of a more
         * specific key is the subset of a more generic key, and it checks cached
         * results instead of issuing another SOAP request to the server.
         * If search key was tokenized with AND or OR, this cannot be assumed.
         */
        private String mTokenizeKey;

        public static SearchGalResult newSearchGalResult(GalContact.Visitor visitor) {
            if (visitor == null)
                return new SearchGalResult();
            else
                return new VisitorSearchGalResult(visitor);
        }

        private SearchGalResult() {
            mMatches = new ArrayList<GalContact>();
        }

        public String getToken() {
            return mToken;
        }

        public void setToken(String token) {
            mToken = token;
        }

        public boolean getHadMore() {
            return mHadMore;
        }

        public void setHadMore(boolean hadMore) {
            mHadMore = hadMore;
        }

        public String getTokenizeKey() {
            return mTokenizeKey;
        }

        public void setTokenizeKey(String tokenizeKey) {
            mTokenizeKey = tokenizeKey;
        }

        public List<GalContact> getMatches() throws ServiceException {
            return mMatches;
        }

        public int getNumMatches() {
            return mMatches.size();
        }

        public void addMatch(GalContact gc) throws ServiceException {
            mMatches.add(gc);
        }

        public void addMatches(SearchGalResult result) throws ServiceException {
            mMatches.addAll(result.getMatches());
        }
    }

    public static class VisitorSearchGalResult extends SearchGalResult {
        private GalContact.Visitor mVisitor;
        private int mNumMatches; // keep track of num matches

        private VisitorSearchGalResult(GalContact.Visitor visitor) {
            mVisitor = visitor;
        }

        @Override
        public List<GalContact> getMatches() throws ServiceException {
            throw ServiceException.FAILURE("getMatches not supported for VisitorSearchGalResult", null);
        }

        @Override
        public int getNumMatches() {
            return mNumMatches;
        }

        @Override
        public void addMatch(GalContact gc) throws ServiceException {
            mVisitor.visit(gc);
            mNumMatches++;
        }

        @Override
        public void addMatches(SearchGalResult result) throws ServiceException {
            if (!(result instanceof VisitorSearchGalResult))
                throw ServiceException.FAILURE("cannot addMatches with non VisitorSearchGalResult", null);

            mNumMatches += result.getNumMatches();
        }

    }

    /**
     * @param d domain
     * @param query LDAP search query
     * @param type address type to search
     * @param token return entries created/modified after timestamp
     * @return List of GalContact objects
     * @throws ServiceException
     */
    public abstract SearchGalResult searchGal(Domain d, String query, GAL_SEARCH_TYPE type, String token) throws ServiceException;

    /**
     * Interface that invokes the visitor object for each match, instead of adding matches to the SearchGalResult.
     *
     * @param d
     * @param query
     * @param type
     * @param token
     * @param visitor
     * @return
     * @throws ServiceException
     */
    public SearchGalResult searchGal(Domain d, String query, GAL_SEARCH_TYPE type, String token, GalContact.Visitor visitor) throws ServiceException {
        throw ServiceException.FAILURE("unsupported", null);
    }


    /**
     * Interface for CalDAV.  it needs to always search in Zimbra only, regardless of zimbraGalMode configured on the domain.
     *
     * @param d domain
     * @param query LDAP search query
     * @param type address type to search
     * @param mode if given, use the provided mode, if null, use mode(zimbraGalMode) configured on the domain
     * @param token return entries created/modified after timestamp
     * @return List of GalContact objects
     * @throws ServiceException
     */
    public SearchGalResult searchGal(Domain d, String query, GAL_SEARCH_TYPE type, GalMode mode, String token) throws ServiceException {
        throw ServiceException.FAILURE("unsupported", null);
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
    public abstract List<NamedEntry> searchCalendarResources(
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
        return new Identity(account, DEFAULT_IDENTITY_NAME, account.getId(), attrs, this);
    }

    public abstract Identity createIdentity(Account account, String identityName, Map<String, Object> attrs) throws ServiceException;

    public abstract Identity restoreIdentity(Account account, String identityName, Map<String, Object> attrs) throws ServiceException;

    public abstract void modifyIdentity(Account account, String identityName, Map<String, Object> attrs) throws ServiceException;

    public abstract void deleteIdentity(Account account, String identityName) throws ServiceException;

    public abstract List<Identity> getAllIdentities(Account account) throws ServiceException;

    /**
     * Returns the <tt>Identity</tt>, or <tt>null</tt> if an identity with the given
     * key does not exist.
     */
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

    public abstract Signature restoreSignature(Account account, String signatureName, Map<String, Object> attrs) throws ServiceException;

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

    public abstract DataSource restoreDataSource(Account account, DataSource.Type type, String dataSourceName, Map<String, Object> attrs) throws ServiceException;

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

    public static enum XMPPComponentBy {

        // case must match protocol
        id, name, serviceHostname;

        public static XMPPComponentBy fromString(String s) throws ServiceException {
            try {
                return XMPPComponentBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    }

    // XMPPComponents
    public abstract XMPPComponent createXMPPComponent(String name, Domain domain, Server server, Map<String, Object> attrs) throws ServiceException;

    public abstract XMPPComponent get(XMPPComponentBy keyName, String key) throws ServiceException;

    public abstract List<XMPPComponent> getAllXMPPComponents() throws ServiceException;

    public abstract void deleteXMPPComponent(XMPPComponent comp) throws ServiceException;

    //
    // rights
    //
    public static enum TargetBy {

        // case must match protocol
        id, name;

        public static TargetBy fromString(String s) throws ServiceException {
            try {
                return TargetBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    }

    public static enum GranteeBy {

        // case must match protocol
        id, name;

        public static GranteeBy fromString(String s) throws ServiceException {
            try {
                return GranteeBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    }

    public static class RightsDoc {
        String mCmd;
        List<String> mRights;
        List<String> mNotes;

        public RightsDoc(String cmd) {
            mCmd = cmd;
            mRights = new ArrayList<String>();
            mNotes = new ArrayList<String>();
        }

        public void addRight(String right) {
            mRights.add(right);
        }

        public void addNote(String note) {
            mNotes.add(note);
        }

        public String getCmd() {
            return mCmd;
        }

        public List<String> getRights() {
            return mRights;
        }

        public List<String> getNotes() {
            return mNotes;
        }
    }

    public Map<String, List<RightsDoc>> getRightsDoc(String[] pkgs) throws ServiceException {
        throw ServiceException.FAILURE("unsupported", null);
    }

    public Right getRight(String rightName, boolean expandAllAttrs) throws ServiceException {
        throw ServiceException.FAILURE("unsupported", null);
    }

    public List<Right> getAllRights(String targetType, boolean expandAllAttrs)  throws ServiceException {
        throw ServiceException.FAILURE("unsupported", null);
    }

    public boolean checkRight(
            String targetType, TargetBy targetBy, String target,
            GranteeBy granteeBy, String grantee,
            String right, Map<String, Object> attrs,
            AccessManager.ViaGrant via) throws ServiceException {
        throw ServiceException.FAILURE("unsupported", null);
    }

    public RightCommand.AllEffectiveRights getAllEffectiveRights(
            String granteeType, GranteeBy granteeBy, String grantee,
            boolean expandSetAttrs, boolean expandGetAttrs) throws ServiceException {
        throw ServiceException.FAILURE("unsupported", null);
    }

    public RightCommand.EffectiveRights getEffectiveRights(
            String targetType, TargetBy targetBy, String target,
            GranteeBy granteeBy, String grantee,
            boolean expandSetAttrs, boolean expandGetAttrs) throws ServiceException {
        throw ServiceException.FAILURE("unsupported", null);
    }

    public RightCommand.EffectiveRights getCreateObjectAttrs(
            String targetType,
            DomainBy domainBy, String domainStr,
            CosBy cosBy, String cosStr,
            GranteeBy granteeBy, String grantee) throws ServiceException {
        throw ServiceException.FAILURE("unsupported", null);
    }

    public RightCommand.Grants getGrants(
            String targetType, TargetBy targetBy, String target,
            String granteeType, GranteeBy granteeBy, String grantee,
            boolean granteeIncludeGroupsGranteeBelongs) throws ServiceException{
        throw ServiceException.FAILURE("unsupported", null);
    }

    public void grantRight(
            String targetType, TargetBy targetBy, String target,
            String granteeType, GranteeBy granteeBy, String grantee, String secret,
            String right, RightModifier rightModifier) throws ServiceException {
        throw ServiceException.FAILURE("unsupported", null);
    }

    public void revokeRight(
            String targetType, TargetBy targetBy, String target,
            String granteeType, GranteeBy granteeBy, String grantee,
            String right, RightModifier rightModifier) throws ServiceException {
        throw ServiceException.FAILURE("unsupported", null);
    }


    public static enum CacheEntryType {
        // on ldap entries
        locale,
        skin,
        license,

        // ldap entries
        account,
        group,
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

    public static class CountAccountResult {
        public static class CountAccountByCos {

            CountAccountByCos(String cosId, String cosName, long count) {
                mCosId = cosId;
                mCosName = cosName;
                mCount = count;
            }

            private String mCosId;
            private String mCosName;
            private long mCount;

            public String getCosId()   { return mCosId;}
            public String getCosName() { return mCosName; }
            public long getCount()        { return mCount; }
        }

        private List<CountAccountByCos> mCountAccountByCos = new ArrayList<CountAccountByCos>();

        public void addCountAccountByCosResult(String cosId, String cosName, long count) {
            CountAccountByCos r = new CountAccountByCos(cosId, cosName, count);
            mCountAccountByCos.add(r);
        }

        public List<CountAccountByCos> getCountAccountByCos() {
            return mCountAccountByCos;
        }

    }

    public CountAccountResult countAccount(Domain domain) throws ServiceException {
        throw ServiceException.FAILURE("unsupported", null);
    }

    // supported types for countObjects
    // for now just used by the installer
    // add more canned types if needed, we certainly don't want to open up a free form query interface
    public enum CountObjectsType {
        userAccounts;

        public static CountObjectsType fromString(String type) throws ServiceException {
            try {
                return CountObjectsType.valueOf(type);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown count cobjects type: " + type, e);
            }
        }
    }

    public long countObjects(CountObjectsType type, Domain domain) throws ServiceException {
        throw ServiceException.FAILURE("unsupported", null);
    }

    /**
     * checks to make sure the specified address is a valid email address (addr part only, no personal part)
     *
     * @throws ServiceException
     */
    public static void validEmailAddress(String addr) throws ServiceException {
        // delegate to NameUtil, should eventually delete this and refactor all call sites to call NameUtil
        NameUtil.validEmailAddress(addr);
    }


    public static boolean isUUID(String value) {
        if (value.length() == 36 &&
            value.charAt(8) == '-' &&
            value.charAt(13) == '-' &&
            value.charAt(18) == '-' &&
            value.charAt(23) == '-')
            return true;
        return false;
    }

    public String getProxyAuthToken(String acctId) throws ServiceException {
        return null;
    }

    public boolean isOfflineProxyServer(Server server) {
        return false;
    }

    public boolean allowsPingRemote() {
        return true;
    }

    public void purgeAccountCalendarCache(String accountId) throws ServiceException {
        // do nothing by default
    }

    public void reloadMemcachedClientConfig() throws ServiceException {
        // do nothing by default
    }

    ///
    //
    // ShareInfo
    //
    //
    public static enum PublishShareInfoAction {
        add,
        remove;

        public static PublishShareInfoAction fromString(String action) throws ServiceException {
            try {
                return PublishShareInfoAction.valueOf(action);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown ShareInfo action: " + action, e);
            }
        }
    }

    public static interface PublishedShareInfoVisitor {
        public void visit(ShareInfoData shareInfoData) throws ServiceException;
    }

    public void publishShareInfo(DistributionList dl, PublishShareInfoAction action,
            Account ownerAcct, String folderIdOrPath) throws ServiceException {
        throw ServiceException.FAILURE("unsupported", null);
    }

    public void getPublishedShareInfo(DistributionList dl, Account ownerAcct,
            PublishedShareInfoVisitor visitor) throws ServiceException {
        throw ServiceException.FAILURE("unsupported", null);
    }

    public void getShareInfo(Account ownerAcct, PublishedShareInfoVisitor visitor) throws ServiceException {
        throw ServiceException.FAILURE("unsupported", null);
    }

    // TODO: consolidate with CacheType in main
    public static enum EntryType {
        account,
        group,
        config,
        cos,
        domain,
        server,
        zimlet;
    }

    public Map<String, String> getNamesForIds(Set<String> ids, EntryType type) throws ServiceException {
        return new HashMap<String, String>();  // return empty map
    }

    public static interface ProvisioningValidator {
        static final String CREATE_ACCOUNT_SUCCEEDED = "createAccountSucceeded";
        static final String CREATE_ACCOUNT = "createAccount";
        static final String CREATE_ACCOUNT_CHECK_DOMAIN_COS_AND_FEATURE =
            "createAccountCheckDomainCosAndFeature";
        static final String MODIFY_ACCOUNT_CHECK_DOMAIN_COS_AND_FEATURE =
            "modifyAccountCheckDomainCosAndFeature";

        void validate(Provisioning prov, String action, Object... args) throws ServiceException;
        void refresh();
    }

    public void register(ProvisioningValidator validator) {
        synchronized (validators) {
            validators.add(validator);
        }
    }

    protected void validate(String action, Object... args) throws ServiceException {
        for (ProvisioningValidator validator : validators) {
            validator.validate(this, action, args);
        }
    }

    public void refreshValidators() {
        for (ProvisioningValidator validator : validators) {
            validator.refresh();
        }
    }

}
