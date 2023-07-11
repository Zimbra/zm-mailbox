/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2018 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.mail.internet.InternetAddress;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.UCServiceBy;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.account.ZAttrProvisioning;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ExceptionToString;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightCommand;
import com.zimbra.cs.account.accesscontrol.RightModifier;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.account.gal.GalOp;
import com.zimbra.cs.account.ldap.entry.LdapDistributionList;
import com.zimbra.cs.account.names.NameUtil;
import com.zimbra.cs.extension.ExtensionUtil;
import com.zimbra.cs.gal.GalSearchParams;
import com.zimbra.cs.ldap.ZLdapFilterFactory.FilterId;
import com.zimbra.cs.mime.MimeTypeInfo;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.soap.account.type.AddressListInfo;
import com.zimbra.soap.account.type.HABGroupMember;
import com.zimbra.soap.admin.type.CacheEntryType;
import com.zimbra.soap.admin.type.CmdRightsInfo;
import com.zimbra.soap.admin.type.CountObjectsType;
import com.zimbra.soap.admin.type.DataSourceType;
import com.zimbra.soap.admin.type.DistributionListSelector;
import com.zimbra.soap.admin.type.DomainSelector;
import com.zimbra.soap.admin.type.GranteeSelector.GranteeBy;
import com.zimbra.soap.admin.type.ServerSelector;
import com.zimbra.soap.admin.type.UCServiceSelector;
import com.zimbra.soap.type.AccountSelector;
import com.zimbra.soap.type.AutoProvPrincipalBy;
import com.zimbra.soap.type.GalSearchType;
import com.zimbra.soap.type.NamedElement;
import com.zimbra.soap.type.TargetBy;

/**
 * @since Sep 23, 2004
 * @author schemers
 */
public abstract class Provisioning extends ZAttrProvisioning {

    public static final String DEFAULT_COS_NAME = "default";
    public static final String DEFAULT_EXTERNAL_COS_NAME = "defaultExternal";

    public static final String SERVICE_MAILBOX   = "mailbox";
    public static final String SERVICE_PROXY = "proxy";
    public static final String SERVICE_MEMCACHED = "memcached";
    public static final String SERVICE_ONLYOFFICE = "onlyoffice";
    /**
     * generate appts that try to be compatible with exchange
     */
    public static final String CAL_MODE_EXCHANGE = "exchange";

    /**
     * generate appts that try to follow the standard
     */
    public static final String CAL_MODE_STANDARD = "standard";

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
     * Foreign principal format for two-way SSL authentication
     */
    public static final String FP_PREFIX_CERT = "cert %s:%s";

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


    //
    // attributes (not generated)
    //
    public static final String A_dc = "dc";
    public static final String A_dgIdentity = "dgIdentity";
    public static final String A_member = "member";

    public static final String LDAP_AM_NONE = "none";
    public static final String LDAP_AM_SIMPLE = "simple";
    public static final String LDAP_AM_KERBEROS5 = "kerberos5";

    public static final int MAX_ZIMBRA_ID_LEN = 127;

    private final List<ProvisioningValidator> validators = new ArrayList<ProvisioningValidator>();

    private static volatile Provisioning singleton;

    public static enum CacheMode {
        DEFAULT,  // use the Provisioning implementation's default caching mode
        ON,
        OFF
    }

    public static enum AuthMode {
        PASSWORD,
        RECOVERY_CODE
    }

    public static final String AUTH_MODE_KEY = "authMode";

    /**
     * return regular accounts from searchAccounts/searchDirectory;
     * calendar resource accounts are excluded
     */
    public static final int SD_ACCOUNT_FLAG = 0x1;

    /** return aliases from searchAccounts/searchDirectory */
    public static final int SD_ALIAS_FLAG = 0x2;

    /** return distribution lists from searchAccounts/searchDirectory */
    public static final int SD_DISTRIBUTION_LIST_FLAG = 0x4;

    /** return calendar resource accounts from searchAccounts/searchDirectory */
    public static final int SD_CALENDAR_RESOURCE_FLAG = 0x8;

    /** return domains from searchAccounts/searchDirectory. only valid with Provisioning.searchAccounts. */
    public static final int SD_DOMAIN_FLAG = 0x10;

    /** return coses from searchDirectory */
    public static final int SD_COS_FLAG = 0x20;

    public static final int SD_SERVER_FLAG = 0x40;

    public static final int SD_UC_SERVICE_FLAG = 0x80;

    /** return coses from searchDirectory */
    public static final int SD_DYNAMIC_GROUP_FLAG = 0x100;

    /** do not fixup objectclass in query for searchObject, only used from LdapUpgrade */
    public static final int SO_NO_FIXUP_OBJECTCLASS = 0x200;

    /** do not fixup return attrs for searchObject, onlt used from LdapUpgrade */
    public static final int SO_NO_FIXUP_RETURNATTRS = 0x400;

    /** return distribution lists from searchAccounts/searchDirectory */
    public static final int SD_HAB_FLAG = 0x12;

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
    public static final int SO_NO_ACCOUNT_DEFAULTS = 0x200;            // do not set defaults and secondary defaults in makeAccount
    public static final int SO_NO_ACCOUNT_SECONDARY_DEFAULTS = 0x400;  // do not set secondary defaults in makeAccount
    public static final String SERVICE_WEBCLIENT = "zimbra";
    public static final String SERVICE_ADMINCLIENT = "zimbraAdmin";
    public static final String SERVICE_ZIMLET = "zimlet";
    public static final String SERVICE_MAILCLIENT = "service";
    public static final String SERVICE_IMAP = "imapd";

    public static Provisioning getInstance() {
        return getInstance(CacheMode.DEFAULT);
    }

    /**
     * This signature allows callsites to specify whether cache should be used in the
     * Provisioning instance returned.
     *
     * !!!Note!!!: setting useCache to false will hurt performance badly, as ***nothing***
     * is cached.  For LdapProvisionig, each LDAP related method will cost one or more LDAP
     * trips.  The only usage for useCache=false is zmconfigd. (bug 70975 and 71267)
     *
     * @param useCache
     * @return
     */
    public static Provisioning getInstance(CacheMode origCacheMode) {
        CacheMode cacheMode = origCacheMode;
        if (singleton == null) {
            synchronized (Provisioning.class) {
                if (singleton == null) {
                    if (cacheMode == null) {
                        cacheMode = CacheMode.DEFAULT;
                    }

                    String className = LC.zimbra_class_provisioning.value();

                    if (className != null && !className.equals("")) {
                        Class<?> klass = null;
                        try {
                            try {
                                klass = Class.forName(className);
                            } catch (ClassNotFoundException cnfe) {
                                // ignore and look in extensions
                                klass = ExtensionUtil.findClass(className);
                            }

                            if (cacheMode != CacheMode.DEFAULT) {
                                try {
                                    singleton =
                                            (Provisioning) klass.getConstructor(CacheMode.class).newInstance(cacheMode);
                                } catch (NoSuchMethodException e) {
                                    ZimbraLog.account.error("could not find constructor with CacheMode parameter '" +
                                            className + "'; defaulting to LdapProvisioning", e);
                                }
                            } else {
                                singleton = (Provisioning) klass.newInstance();
                            }
                        } catch (Exception e) {
                            ZimbraLog.account.error("could not instantiate Provisioning interface of class '" +
                                    className + "'; defaulting to LdapProvisioning", e);
                        }
                    }

                    if (singleton == null) {
                        singleton = new com.zimbra.cs.account.ldap.LdapProvisioning(cacheMode);
                        ZimbraLog.account.error("defaulting to " + singleton.getClass().getCanonicalName());
                    }
                }
            }
        }
        return singleton;
    }

    @VisibleForTesting
    public synchronized static void setInstance(Provisioning prov) {
        singleton = prov;
    }

    /**
     * Updates the values of the following attributes in the provided account argument:
     * - userPassword
     * - zimbraAuthTokens
     * - zimbraAuthTokenValidityValue
     * @param account Account instance who's credentials are to be refreshed
     */
    public abstract void refreshUserCredentials(Account account) throws ServiceException;

    public boolean idIsUUID() {
        return true;
    }

    public boolean isOctopus() throws ServiceException {
        return getConfig().getProduct() != Product.ZCS;
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

    public void restoreAccountAttrs(Account acct, Map<String, ? extends Object> backupAttrs)
    throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    /**
     * Replace sensitive strings like passwords with "VALUE-BLOCKED"
     */
    public static Object sanitizedAttrValue(String name, Object realValue) {
        // Never return password.
        if (name.equalsIgnoreCase(Provisioning.A_userPassword) ||
            name.equalsIgnoreCase(Provisioning.A_zimbraDataSourcePassword) ||
            name.equalsIgnoreCase(Provisioning.A_zimbraFreebusyExchangeAuthPassword) ||
            name.equalsIgnoreCase(Provisioning.A_zimbraUCPassword) ||
            name.equalsIgnoreCase(Provisioning.A_zimbraTwoFactorAuthSecret) ||
            name.equalsIgnoreCase(Provisioning.A_zimbraTwoFactorAuthScratchCodes) ||
            name.equalsIgnoreCase(Provisioning.A_zimbraAppSpecificPassword) ||
            name.equalsIgnoreCase(Provisioning.A_zimbraTwoFactorAuthTrustedDevices) ||
            name.equalsIgnoreCase(Provisioning.A_zimbraDataSourceOAuthToken) ||
            name.equalsIgnoreCase(Provisioning.A_zimbraDataSourceOAuthClientSecret) ||
            name.equalsIgnoreCase(Provisioning.A_zimbraDataSourceOAuthRefreshToken)) {
            return "VALUE-BLOCKED";
        }
        return realValue;
    }

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

    public Domain getDomain(MailTarget mailTarget) throws ServiceException {
        if (mailTarget instanceof Alias) {
            return getDomain((Alias) mailTarget);
        } else if (mailTarget instanceof Account) {
            return getDomain((Account) mailTarget);
        } else if (mailTarget instanceof DistributionList) {
            return getDomain((DistributionList) mailTarget);
        } else if (mailTarget instanceof DynamicGroup) {
            return getDomain((DynamicGroup) mailTarget);
        }
        return null;
    }

    /**
     * @return the domain of this account, or null if an admin account.
     * @throws ServiceException
     */
    public Domain getDomain(Account acct) throws ServiceException {
        String dname = acct.getDomainName();
        boolean checkNegativeCache = (acct instanceof GuestAccount);
        return dname == null ? null : getDomain(Key.DomainBy.name, dname, checkNegativeCache);
    }

    /**
     * @return the domain of this alias
     * @throws ServiceException
     */
    public Domain getDomain(Alias alias) throws ServiceException {
        String dname = alias.getDomainName();
        return dname == null ? null : getDomain(Key.DomainBy.name, dname, false);
    }

    public Domain getDomainByEmailAddr(String emailAddr) throws ServiceException{
        String domainName = NameUtil.EmailAddress.getDomainNameFromEmail(emailAddr);
        Domain domain = Provisioning.getInstance().get(Key.DomainBy.name, domainName);
        if (domain == null) {
            throw AccountServiceException.NO_SUCH_DOMAIN(domainName);
        }
        return domain;
    }

    public Domain getDefaultDomain() throws ServiceException {
        String dname = getConfig().getDefaultDomainName();
        return dname == null ? null : getDomain(Key.DomainBy.name, dname, true);
    }


    /**
     * @return the Server object where this account's mailbox is homed
     * @throws ServiceException
     */
    // TODO: change all callsite to call Account.getServer();
    public Server getServer(Account acct) throws ServiceException {
        return acct.getServer();
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
                if (id != null) {
                    cos = get(Key.CosBy.id, id);
                }
                if (cos == null) {
                    Domain domain = getDomain(acct);
                    String domainCosId = domain != null ? acct.isIsExternalVirtualAccount() ?
                            domain.getDomainDefaultExternalUserCOSId() : domain.getDomainDefaultCOSId() : null;
                    if (domainCosId != null) {
                        cos = get(Key.CosBy.id, domainCosId);
                    }
                }
                if (cos == null) {
                    cos = get(Key.CosBy.name, acct.isIsExternalVirtualAccount() ?
                            Provisioning.DEFAULT_EXTERNAL_COS_NAME : Provisioning.DEFAULT_COS_NAME);
                }
                if (cos != null) {
                    acct.setCachedData(EntryCacheDataKey.ACCOUNT_COS, cos);
                }
        }
        return cos;
    }

    /**
     * @return the default COS object for this domain
     *
     * @throws ServiceException
     */
    public Cos getDefaultCOS(Domain domain) throws ServiceException {
        // CACHE. If we get reloaded from LDAP, cached data is cleared
        Cos cos = (Cos) domain.getCachedData(EntryCacheDataKey.DOMAIN_DEFAULT_COS);
        if (cos == null) {
            String domainCosId = domain.getDomainDefaultCOSId();
            cos = domainCosId != null ? get(Key.CosBy.id, domainCosId) :
                    get(Key.CosBy.name, Provisioning.DEFAULT_COS_NAME);
            if (cos != null) {
                domain.setCachedData(EntryCacheDataKey.DOMAIN_DEFAULT_COS, cos);
            }
        }
        return cos;
    }

    /**
     * @return the AlwaysOnCluster object for this server, or null if server has no AlwaysOnCluster
     *
     * @throws ServiceException
     */
    public AlwaysOnCluster getAlwaysOnCluster(Server server) throws ServiceException {
        // CACHE. If we get reloaded from LDAP, cached data is cleared
        AlwaysOnCluster aoc = (AlwaysOnCluster) server.getCachedData(EntryCacheDataKey.SERVER_ALWAYSONCLUSTER);
        if (aoc == null) {
            String id = server.getAlwaysOnClusterId();
            if (id != null) {
                aoc = get(Key.AlwaysOnClusterBy.id, id);
            }
            if (aoc != null) {
                server.setCachedData(EntryCacheDataKey.SERVER_ALWAYSONCLUSTER, aoc);
            }
        }
        return aoc;
    }

    /**
     * returns addr@<local domain> for addr@<alias domain>, null if given address is a local domain address
     * @param emailAddress original addr
     * @return addr@<local domain> or null
     * @throws ServiceException
     */
    public String getEmailAddrByDomainAlias(String emailAddress) throws ServiceException {
        String addr = null;

        String parts[] = emailAddress.split("@");
        if (parts.length == 2) {
            Domain domain = getDomain(Key.DomainBy.name, parts[1], true);
            if ((domain != null) && (!domain.isLocal())) {
                String targetDomainId = domain.getAttr(A_zimbraDomainAliasTargetId);
                if (targetDomainId != null) {
                    domain = getDomainById(targetDomainId);
                    if (domain != null) {
                        addr = parts[0] + "@" + domain.getName();
                    }
                }
            }
        }

        return addr;
    }

    /**
     * if the group has only internal members (or don't have any members):
     *   - groupAddr() returns the group's email address
     *   - internalAddrs() return null
     *   - externalAddrs() return null
     *
     * if the group has only external members:
     *   - groupAddr() returns null
     *   - internalAddrs() return null
     *   - externalAddrs() return email addrs of all external members
     *
     * if the group has both internal and external members:
     *   - groupAddr() returns null
     *   - internalAddrs() return email addrs of all internal members
     *   - externalAddrs() return email addrs of all external members
     *
     * Callsite can safely do:
     * GroupMemberEmailAddrs addrs = Provisioning.getInstance().getMemberAddrs(group);
     * if (addrs.groupAddr() != null) {
     *     ...
     * }
     * if (addrs.internalAddrs() != null) {
     *     ...
     * }
     * if (addrs.externalAddrs() != null) {
     *     ...
     * }
     *
     */
    public static class GroupMemberEmailAddrs {
        private String groupAddr;
        private Collection<String> internalAddrs;
        private Collection<String> externalAddrs;

        public void setGroupAddr(String addr) {
            groupAddr = addr;
        }

        public void setInternalAddrs(Collection<String> addrs) {
            internalAddrs = addrs;
        }

        public void setExternalAddrs(Collection<String> addrs) {
            externalAddrs = addrs;
        }

        /**
         * if the group has only internal members, returns the group's email address
         * if the group has external members, return null;
         */
        public String groupAddr() {
            return groupAddr;
        }

        /**
         * if the group has only internal or only external members, returns null
         * otherwise returns internal members of the group
         */
        public Collection<String> internalAddrs() {
            return internalAddrs;
        }

        /**
         * if the group has only internal members, returns null
         * otherwise returns external members of the group
         */
        public Collection<String> externalAddrs() {
            return externalAddrs;
        }
    }

    public GroupMemberEmailAddrs getMemberAddrs(Group group) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    public boolean inDistributionList(MailTarget mailTarget, String zimbraId)
    throws ServiceException {
        if (mailTarget instanceof Account) {
            return inDistributionList((Account) mailTarget, zimbraId);
        } else if (mailTarget instanceof DistributionList) {
            return inDistributionList((DistributionList) mailTarget, zimbraId);
        }
        return false;
    }

    /**
     * @param zimbraId the zimbraId of the dl we are checking for
     * @return true if this account (or one of the dl it belongs to) is a member of the specified dl.
     * @throws ServiceException
     */
    public abstract boolean inDistributionList(Account acct, String zimbraId)
    throws ServiceException;

    /**
     * @param zimbraId the zimbraId of the dl we are checking for
     * @return true if this distribution list (or one of the dl it belongs to) is a member
     *         of the specified dl.
     * @throws ServiceException
     */
    public boolean inDistributionList(DistributionList list, String zimbraId)
    throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    /**
     * @return set of all the zimbraId's of lists this account belongs to, including any
     *         list in other list.
     * @throws ServiceException
     */
    public abstract Set<String> getDistributionLists(Account acct) throws ServiceException;

    /**
     * @return set of all the zimbraId's of direct lists this account belongs to
     * @throws ServiceException
     */
    public abstract Set<String> getDirectDistributionLists(Account acct) throws ServiceException;

    /**
     *
     * @param directOnly return only DLs this account is a direct member of
     * @param via if non-null and directOnly is false, this map will containing a mapping
     *        from a DL name to the DL it was a member of, if member was indirect.
     * @return all the DLs
     * @throws ServiceException
     */
    public abstract List<DistributionList> getDistributionLists(Account acct,
            boolean directOnly, Map<String,String> via)
    throws ServiceException;

    /**
     *
     * @param directOnly return only DLs this DL is a direct member of
     * @param via if non-null and directOnly is false, this map will containing a mapping from a DL
     *        name to the DL it was a member of, if member was indirect.
     * @return all the DLs
     * @throws ServiceException
     */
    public abstract List<DistributionList> getDistributionLists(DistributionList list,
            boolean directOnly, Map<String,String> via)
    throws ServiceException;

    /**
     * represents a super group in which the perspective object(account, cr, dl) is
     * directly or indirectly a member of
     */
    public static class MemberOf {
        private final String mId;            // zimbraId of this group
        private final boolean mIsAdminGroup; // if this group is an admin group (zimbraIsAdminGroup == TRUE)
        private final boolean mIsDynamicGroup; // if this group is a dynamic group

        public MemberOf(String id, boolean isAdminGroup, boolean isDynamicGroup) {
            mId = id;
            mIsAdminGroup = isAdminGroup;
            mIsDynamicGroup = isDynamicGroup;
        }

        public String getId() {
            return mId;
        }

        public boolean isAdminGroup() {
            return mIsAdminGroup;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                        .add("mId", mId)
                        .add("mIsAdminGroup", mIsAdminGroup)
                        .add("mIsDynamicGroup", mIsDynamicGroup)
                        .toString();
        }
    }

    /**
     * Intended to store membership information only related to groups who define their membership by a custom
     * MemberURL.  As we can't control what updates affect these, the only handle we have on whether the
     * the information is valid is how long ago it was accurate.
     *
     */
    public static class GroupMembershipAtTime {
        private final GroupMembership membership;
        private final long correctAtTime;
        public GroupMembershipAtTime(GroupMembership members, long accurateAtTime) {
            membership = members;
            correctAtTime = accurateAtTime;
        }
        public GroupMembership getMembership() {
            return membership;
        }
        public long getCorrectAtTime() {
            return correctAtTime;
        }
    }

    public static class GroupMembership {
        private final List<MemberOf> mMemberOf;  // list of MemberOf
        private final List<String> mGroupIds;    // list of group ids

        public GroupMembership(List<MemberOf> memberOf, List<String> groupIds) {
            mMemberOf = memberOf;
            mGroupIds = groupIds;
        }

        // create an empty AclGroups
        public GroupMembership() {
            this(new ArrayList<MemberOf>(), new ArrayList<String>());
        }

        public void append(MemberOf memberOf, String groupId) {
            mMemberOf.add(memberOf);
            mGroupIds.add(groupId);
        }

        public void append(MemberOf memberOf) {
            mMemberOf.add(memberOf);
            mGroupIds.add(memberOf.getId());
        }

        public GroupMembership mergeFrom(GroupMembership other) {
            for (int i = 0; i < other.mMemberOf.size(); i++) {
                if (!mGroupIds.contains(other.mGroupIds.get(i))) {
                    append(other.mMemberOf.get(i), other.mGroupIds.get(i));
                }
            }
            return this;
        }

        public List<MemberOf> memberOf() {
            return mMemberOf;
        }

        public MemberOf getMemberOfForId(String grpId) {
            for (MemberOf memberOf : mMemberOf) {
                if (grpId.equals(memberOf.getId())) {
                    return memberOf;
                }
            }
            return null;
        }

        public List<String> groupIds() {
            return mGroupIds;
        }

        @Override
        public GroupMembership clone() {
            GroupMembership copy = new GroupMembership();
            for (int i = 0; i < mMemberOf.size(); i++) {
                copy.append(mMemberOf.get(i), mGroupIds.get(i));
            }
            return copy;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                        .add("mMemberOf", mMemberOf)
                        .add("mGroupIds", mGroupIds)
                        .toString();
        }
    }

    /**
     * @param mailTarget
     * @param adminGroupsOnly return admin groups only
     * @return List of all direct and indirect groups this mailTarget belongs to.
     *         The returned List is sorted by "shortest distance" to the mailTarget,
     *         the shorter the distance is, the earlier it appears in the returned List.
     * @throws ServiceException
     */
    public GroupMembership getGroupMembership(MailTarget mailTarget, boolean adminGroupsOnly)
    throws ServiceException {
        if (mailTarget instanceof Account) {
            return getGroupMembership((Account) mailTarget, adminGroupsOnly);
        } else if (mailTarget instanceof DistributionList) {
            return getGroupMembership((DistributionList) mailTarget, adminGroupsOnly);
        }
        return new GroupMembership();
    }

    /**
     * @param acct
     * @param adminGroupsOnly return admin groups only
     * @return List of all direct and indirect groups this account belongs to.
     *         The returned List is sorted by "shortest distance" to the account,
     *         the shorter the distance is, the earlier it appears in the returned List.
     * @throws ServiceException
     */
    public GroupMembership getGroupMembership(Account acct, boolean adminGroupsOnly)
    throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    /**
     *
     * @param list
     * @param adminGroupsOnly return admin groups only
     * @return List of all the zimbraId's of lists this list belongs to, including any list in other list.
     *         The returned List is sorted by "shortest distance" to the list, the shorter the distance is,
     *         the earlier it appears in the returned List.
     * @throws ServiceException
     */
    public GroupMembership getGroupMembership(DistributionList list, boolean adminGroupsOnly)
    throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    /**
     * @param adminGroupsOnly return admin groups only
     * @param rights - the rights to check.  null or empty means "any rights"
     * @return Groups which {@code acct} is a member of which have been granted one or more or the {@code rights}
     * @return List of all direct and indirect groups {@code acct} is a member of that have been granted
     * one or more or the {@code rights}.
     * The returned List is not sorted in any particular way.
     */
    public GroupMembership getGroupMembershipWithRights(Account acct, Set<Right> rights, boolean adminGroupsOnly)
    throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    /**
     * @return the domain of the distribution list
     * @throws ServiceException
     */
    public Domain getDomain(DistributionList dl) throws ServiceException {
        String dname = dl.getDomainName();
        return dname == null ? null : get(Key.DomainBy.name, dname);
    }

    /**
     * @return the domain of the dynamic grop
     * @throws ServiceException
     */
    public Domain getDomain(DynamicGroup dynGroup) throws ServiceException {
        String dname = dynGroup.getDomainName();
        return dname == null ? null : get(Key.DomainBy.name, dname);
    }

    public abstract boolean healthCheck() throws ServiceException;

    public abstract Config getConfig() throws ServiceException;

    public Config getConfig(String attr) throws ServiceException {
        return getConfig();
    }

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
     * prov.createAccount("roland@tiiq.net", "dsferulz", attrs);
     * </pre>
     *
     * @param emailAddress email address (domain must already exist) of account being created.
     * @param password password of account being created, or null. Account's without passwords can't be logged into.
     * @param attrs other initial attributes or <code>null</code>
     * @return
     * @throws ServiceException
     */
    public abstract Account createAccount(String emailAddress, String password,
            Map<String, Object> attrs) throws ServiceException;

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

    /**
     * Returns the domain under which ZMG app accounts are created by default.
     *
     * @return
     * @throws ServiceException
     */
    public Domain getDefaultZMGDomain() throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    /**
     * Creates an account for an app that interfaces with the Mobile Gateway features.
     *
     * @param accountId id to use for the new account
     * @param appCredsDigest a string representing the unique set of credentials of the app
     * @return
     */
    public Account autoProvZMGAppAccount(String accountId, String appCredsDigest) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public Pair<Account, Boolean> autoProvZMGProxyAccount(String emailAddr, String password) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    /**
     * Looks up an account by the specified key.
     *
     * @return the <code>Account</code>, or <code>null</code> if no <code>Account</code>
     * with the given key exists.
     * @throws ServiceException if the key is malformed
     */
    public abstract Account get(AccountBy keyType, String key) throws ServiceException;

    public Account get(AccountSelector acctSel)
    throws ServiceException {
        return get(acctSel.getBy().toKeyAccountBy(), acctSel.getKey());
    }

    public static interface EagerAutoProvisionScheduler {
        // returns whether a shutdown has been request to the scheduler
        public boolean isShutDownRequested();
    }
    /**
     * Auto provisioning account in EAGER mode.
     *
     * @param domain
     * @return
     * @throws ServiceException
     */
    public void autoProvAccountEager(EagerAutoProvisionScheduler scheduler)
    throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    /**
     * Auto provisioning account in LAZY mode.
     *
     * Auto create account on login if account auto provision are enabled on the domain,
     * and if the principal can be or had been authenticated.
     *
     * returns instance of the auto-provisioned account if the account is successfully created.
     * returns null otherwise.
     *
     *
     * @param loginName the login name (the name presented to the autoenticator) identifying the user
     * @param loginPassword password if provided
     * @param authMech auth mechanism via which the principal was successfully authenticated to zimbra
     *                 null if the principal had not been authenticated
     * @param domain
     * @return an account instance if the account is successfully created
     * @throws ServiceException
     */
    public Account autoProvAccountLazy(Domain domain, String loginName, String loginPassword,
            AutoProvAuthMech authMech) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    /**
     * Auto provisioning account in MANUAL mode.
     *
     * @param domain
     * @param by
     * @param principal
     * @return an account instance if the account is successfully created
     * @throws ServiceException
     */
    public Account autoProvAccountManual(Domain domain, AutoProvPrincipalBy by,
            String principal, String password)
    throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public void autoProvControl(String action) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public static interface DirectoryEntryVisitor {
        void visit(String dn, Map<String, Object> attrs);
    };

    public void searchAutoProvDirectory(Domain domain, String filter, String name,
            String[] returnAttrs, int maxResults, DirectoryEntryVisitor visitor)
    throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }



    public Account getAccountByName(String name) throws ServiceException { return get(AccountBy.name, name); }
    public Account getAccountById(String id) throws ServiceException { return get(AccountBy.id, id); }
    public Account getAccountByAppAdminName(String name) throws ServiceException { return get(AccountBy.appAdminName, name); }
    public Account getAccountByForeignPrincipal(String name) throws ServiceException { return get(AccountBy.foreignPrincipal, name); }
    public Account getAccountByKrb5Principal(String name) throws ServiceException { return get(AccountBy.krb5Principal, name); }

    public Account getAccountByForeignName(String foreignName, String application, Domain domain) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

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

    public Account get(AccountSelector acctSel, AuthToken authToken)
    throws ServiceException {
        return get(acctSel.getBy().toKeyAccountBy(), acctSel.getKey(), authToken);
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

    public abstract List<Account> getAllAdminAccounts()  throws ServiceException;

    public abstract void setCOS(Account acct, Cos cos) throws ServiceException;

    public abstract void modifyAccountStatus(Account acct, String newStatus) throws ServiceException;

    public abstract void authAccount(Account acct, String password, AuthContext.Protocol proto)
    throws ServiceException;

    public abstract void authAccount(Account acct, String password,
            AuthContext.Protocol proto, Map<String, Object> authCtxt)
    throws ServiceException;

    public void accountAuthed(Account acct) throws ServiceException {
        // noop by default
    }

    public void preAuthAccount(Account acct, String accountName, String accountBy, long timestamp, long expires,
                                        String preAuth,
                                        boolean admin,
                                        Map<String, Object> authCtxt) throws ServiceException
    {
        if (admin)
            throw ServiceException.UNSUPPORTED();
        else
            preAuthAccount(acct, accountName, accountBy, timestamp, expires, preAuth, authCtxt);
    }

    public abstract void preAuthAccount(Account acct, String accountName, String accountBy,
            long timestamp, long expires, String preAuth, Map<String, Object> authCtxt)
    throws ServiceException;

    public void preAuthAccount(Domain domain, String accountName, String accountBy,
            long timestamp, long expires, String preAuth, Map<String, Object> authCtxt)
    throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public abstract void ssoAuthAccount(Account acct, AuthContext.Protocol proto, Map<String, Object> authCtxt)
    throws ServiceException;

    public abstract void changePassword(Account acct, String currentPassword, String newPassword) throws ServiceException;
    public abstract void changePassword(Account acct, String currentPassword, String newPassword, boolean dryRun) throws ServiceException;

    public static class SetPasswordResult {
        private String msg;

        public SetPasswordResult() {
        }

        public SetPasswordResult(String msg) {
            setMessage(msg);
        }

        public boolean hasMessage() {
            return msg != null;
        }

        public void setMessage(String msg) {
            this.msg = msg;
        }

        public String getMessage() {
            return msg;
        }
    }

    public abstract SetPasswordResult setPassword(Account acct, String newPassword) throws ServiceException;

    public SetPasswordResult setPassword(Account acct, String newPassword, boolean enforcePasswordPolicy)
    throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public abstract void resetPassword(Account acct, String newPassword, boolean dryRun) throws ServiceException;

    public abstract void checkPasswordStrength(Account acct, String password) throws ServiceException;

    public abstract void addAlias(Account acct, String alias) throws ServiceException;

    public abstract void addAlias(Account acct, String alias, boolean isHiddenAlias) throws ServiceException;

    public abstract void removeAlias(Account acct, String alias) throws ServiceException;

    /**
     * search alias target - will always do a search
     *
     * @param alias
     * @param mustFind
     * @return
     * @throws ServiceException
     */
    public NamedEntry searchAliasTarget(Alias alias, boolean mustFind)
    throws ServiceException {
        String targetId = alias.getAttr(Provisioning.A_zimbraAliasTargetId);

        String query = "(" + Provisioning.A_zimbraId + "=" + targetId + ")";

        SearchDirectoryOptions options = new SearchDirectoryOptions();
        options.setTypes(SearchDirectoryOptions.ObjectType.accounts,
                SearchDirectoryOptions.ObjectType.resources,
                SearchDirectoryOptions.ObjectType.distributionlists,
                SearchDirectoryOptions.ObjectType.dynamicgroups);
        options.setFilterString(FilterId.SEARCH_ALIAS_TARGET, query);

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
     * @param server
     * @return may return null
     */
    public static ServerSelector getSelector(Server server) {
        if (server == null)
            return null;
        return ServerSelector.fromId(server.getId());
    }

    /**
     * @param domain
     * @return may return null
     */
    public static DomainSelector getSelector(Domain domain) {
        if (domain == null)
            return null;
        return DomainSelector.fromId(domain.getId());
    }

    /**
     * @param ucService
     * @return may return null
     */
    public static UCServiceSelector getSelector(UCService ucService) {
        if (ucService == null)
            return null;
        return UCServiceSelector.fromId(ucService.getId());
    }

    /**
     * @param acct
     * @return may return null
     */
    public static AccountSelector getSelector(Account acct) {
        if (acct == null)
            return null;
        return AccountSelector.fromId(acct.getId());
    }

    /**
     * @param dl
     * @return may return null
     */
    public static DistributionListSelector getSelector(DistributionList dl) {
        if (dl == null)
            return null;
        return DistributionListSelector.fromId(dl.getId());
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

    public abstract Domain get(Key.DomainBy keyType, String key) throws ServiceException;

    public Domain get(DomainSelector domSel)
    throws ServiceException {
        return get(domSel.getBy().toKeyDomainBy(), domSel.getKey());
    }

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
    public Domain getDomain(Key.DomainBy keyType, String key, boolean checkNegativeCache) throws ServiceException {
        return get(keyType, key);
    }

    public Domain getDomainByName(String name) throws ServiceException { return get(Key.DomainBy.name, name); }
    public Domain getDomainById(String id) throws ServiceException { return get(Key.DomainBy.id, id); }
    public Domain getDomainByVirtualHostname(String host) throws ServiceException { return get(Key.DomainBy.virtualHostname, host); }
    public Domain getDomainByKrb5Realm(String realm) throws ServiceException { return get(Key.DomainBy.krb5Realm, realm); }
    public Domain getDomainByForeignName(String realm) throws ServiceException { return get(Key.DomainBy.foreignName, realm); }


    public abstract List<Domain> getAllDomains()  throws ServiceException;

    public void getAllDomains(NamedEntry.Visitor visitor, String[] retAttrs) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public abstract void deleteDomain(String zimbraId) throws ServiceException;

    public abstract void deleteDomainAfterRename(String zimbraId) throws ServiceException;

    public abstract Cos createCos(String name, Map<String, Object> attrs) throws ServiceException;

    public abstract Cos copyCos(String srcCosId, String destCosName) throws ServiceException;

    public abstract void renameCos(String zimbraId, String newName) throws ServiceException;

    public abstract Cos get(Key.CosBy keyType, String key) throws ServiceException;

    public Cos getCosByName(String name) throws ServiceException { return get(Key.CosBy.name, name); }
    public Cos getCosById(String id) throws ServiceException { return get(Key.CosBy.id, id); }

    public abstract List<Cos> getAllCos()  throws ServiceException;

    public abstract void deleteCos(String zimbraId) throws ServiceException;

    public abstract Server getLocalServer() throws ServiceException;
    public abstract Server getLocalServerIfDefined();

    public static final class Reasons {
        private final StringBuilder sb = new StringBuilder();
        public void addReason(String reason) {
            if (0 < sb.length()) {
                sb.append('\n');
            }
            sb.append(reason);
        }
        public String getReason() {
            return sb.toString();
        }
    }

    public static boolean onLocalServer(Account account) throws ServiceException {
        return onLocalServer(account, null);
    }

    public static boolean onLocalServer(Account account, Reasons reasons) throws ServiceException {
        String target = account.getAttr(Provisioning.A_zimbraMailHost);
        String localhost = getInstance().getLocalServer().getAttr(Provisioning.A_zimbraServiceHostname);
        boolean isLocal = (target != null && target.equalsIgnoreCase(localhost));
        boolean onLocalSvr =  (isLocal || isAlwaysOn(account));
        if (!onLocalSvr && reasons != null) {
            reasons.addReason(String.format("onLocalSvr=%b isLocal=%b target=%s localhost=%s account=%s",
                    onLocalSvr, isLocal, target, localhost, account.getName()));
        }
        return onLocalSvr;
    }

    public static boolean canUseLocalIMAP(Account account) throws ServiceException {
        if(account == null) {
            return false;
        }
        Server homeServer = account.getServer();
        if(homeServer == null) {
            return false;
        }
        String[] upstreamIMAPServers = homeServer.getReverseProxyUpstreamImapServers();
        if(upstreamIMAPServers != null && upstreamIMAPServers.length > 0) {
            return Arrays.asList(upstreamIMAPServers).contains(getInstance().getLocalServer().getServiceHostname());
        } else {
            return onLocalServer(account);
        }
    }

    public static List<Server> getPreferredIMAPServers(Account account) throws ServiceException {
        Server homeServer = account.getServer();
        if (homeServer == null) {
            return Collections.emptyList();
        }
        List<Server> imapDaemonServers = getIMAPDaemonServers(homeServer);
        if (!imapDaemonServers.isEmpty()) {
            return imapDaemonServers;
        }
        return Lists.newArrayList(getInstance().getServerByServiceHostname(account.getMailHost()));
    }

    public static List<Server> getIMAPDaemonServers(Account account) throws ServiceException {
        Server homeServer = account.getServer();
        if (homeServer == null) {
            return Collections.emptyList();
        }
        return getIMAPDaemonServers(homeServer);
    }

    public static List<Server> getIMAPDaemonServers(Server server) throws ServiceException {
        String[] upstreamIMAPServers = server.getReverseProxyUpstreamImapServers();
        Provisioning prov = getInstance();
        List<Server> imapServers = new ArrayList<>(upstreamIMAPServers.length);
        for (String serverName : upstreamIMAPServers) {
            Server svr = prov.getServerByServiceHostname(serverName);
            if (svr == null) {
                ZimbraLog.imap.warn("cannot find imap server by service hostname for '%s'", serverName);
                continue;
            }
            imapServers.add(svr);
        }
        return imapServers;
    }

    public static List<Server> getIMAPDaemonServersForLocalServer() throws ServiceException {
        return getIMAPDaemonServers(getInstance().getLocalServer());
    }

    private static boolean isAlwaysOn(Account account) throws ServiceException {
        return isAlwaysOn(account, null);
    }

    private static boolean isAlwaysOn(Account account, Reasons reasons) throws ServiceException {
        String localServerClusterId = Zimbra.getAlwaysOnClusterId();
        Server server = Provisioning.getInstance().getServer(account);
        String accountHostingServerClusterId = null;
        if (server != null) {
            accountHostingServerClusterId = server.getAlwaysOnClusterId();
        }
        boolean isAlwaysOn = (server != null) && localServerClusterId != null &&
                accountHostingServerClusterId != null && localServerClusterId.equals(accountHostingServerClusterId);
        if (!isAlwaysOn && reasons != null) {
            reasons.addReason(String.format("isAlwaysOn=%b server=%s localServerClusterId=%s account=%s",
                    isAlwaysOn, server, localServerClusterId, account.getName()));
        }
        return isAlwaysOn;
    }

    public static boolean onLocalServer(Group group) throws ServiceException {
        String target    = group.getAttr(Provisioning.A_zimbraMailHost);
        String localhost = getInstance().getLocalServer().getAttr(Provisioning.A_zimbraServiceHostname);
        return (target != null && target.equalsIgnoreCase(localhost));
    }

    public abstract Server createServer(String name, Map<String, Object> attrs) throws ServiceException;

    public abstract Server get(Key.ServerBy keyName, String key) throws ServiceException;

    public Server getServerByName(String name) throws ServiceException { return get(Key.ServerBy.name, name); }
    public Server getServerById(String id) throws ServiceException { return get(Key.ServerBy.id, id); }
    public Server getServerByServiceHostname(String name) throws ServiceException { return get(Key.ServerBy.serviceHostname, name); }

    public abstract List<Server> getAllServers()  throws ServiceException;

    public abstract List<Server> getAllServers(String service)  throws ServiceException;

    public abstract List<Server> getAllServers(String service, String clusterId) throws ServiceException;

    public List<Server> getAllWebClientServers() throws ServiceException {
        List<Server> mailboxservers = getAllServers(Provisioning.SERVICE_MAILBOX);
        List<Server> webclientservers = getAllServers(Provisioning.SERVICE_WEBCLIENT);

        for (Server server : mailboxservers) {
            String version = server.getAttr(Provisioning.A_zimbraServerVersion, null);
            // We get all pre 8.5 servers first (ones which don't have the zimbraServerVersion set)
            if (version != null) {
                continue;
            }
            // Add it to the list of 8.5+ webclient servers and return this list
            webclientservers.add(server);
        }

        return webclientservers;
    }

    public List<Server> getAllAdminClientServers() throws ServiceException {
        List<Server> mailboxservers = getAllServers(Provisioning.SERVICE_MAILBOX);
        List<Server> adminclientservers = getAllServers(Provisioning.SERVICE_ADMINCLIENT);

        for (Server server : mailboxservers) {
            String version = server.getAttr(Provisioning.A_zimbraServerVersion, null);
            // We get all pre 8.5 servers first (ones which don't have the zimbraServerVersion set)
            if (version != null) {
                continue;
            }
            // Add it to the list of 8.5+ adminclient servers and return this list
            adminclientservers.add(server);
        }

        return adminclientservers;
    }

    public List<Server> getAllZimletServers() throws ServiceException {
        List<Server> mailboxservers = getAllServers(Provisioning.SERVICE_MAILBOX);
        List<Server> zimletservers = getAllServers(Provisioning.SERVICE_ZIMLET);

        for (Server server : mailboxservers) {
            String version = server.getAttr(Provisioning.A_zimbraServerVersion, null);
            // We get all pre 8.5 servers first (ones which don't have the zimbraServerVersion set)
            if (version != null) {
                continue;
            }
            // Add it to the list of 8.5+ zimlet servers and return this list
            zimletservers.add(server);
        }

        return zimletservers;
    }

    public List<Server> getAllDeployableZimletServers() throws ServiceException
    {
        List<Server> deployableservers = new ArrayList<Server>();

        for (Server server : getAllZimletServers()) {
            if (server.isLocalServer() || server.hasMailClientService()) {
                deployableservers.add(server);
            }
        }

        return deployableservers;
    }

    public List<Server> getAllMailClientServers() throws ServiceException {
        List<Server> mailboxservers = getAllServers(Provisioning.SERVICE_MAILBOX);
        List<Server> mailclientservers = getAllServers(Provisioning.SERVICE_MAILCLIENT);

        for (Server server : mailboxservers) {
            String version = server.getAttr(Provisioning.A_zimbraServerVersion, null);
            // We get all pre 8.5 servers first (ones which don't have the zimbraServerVersion set)
            if (version != null) {
                continue;
            }
            // Add it to the list of 8.5+ mailclient servers and return this list
            mailclientservers.add(server);
        }

        return mailclientservers;
    }

    public abstract void deleteServer(String zimbraId) throws ServiceException;

    /*
     * AlwaysOnCluster
     */
    public abstract AlwaysOnCluster createAlwaysOnCluster(String name, Map<String, Object> attrs) throws ServiceException;
    public abstract AlwaysOnCluster get(Key.AlwaysOnClusterBy keyname, String key) throws ServiceException;
    public abstract void deleteAlwaysOnCluster(String zimbraId) throws ServiceException;
    public abstract List<AlwaysOnCluster> getAllAlwaysOnClusters()  throws ServiceException;

    /*
     * UC service
     */
    public abstract UCService createUCService(String name, Map<String, Object> attrs) throws ServiceException;
    public abstract void deleteUCService(String zimbraId) throws ServiceException;
    public abstract UCService get(UCServiceBy keyName, String key) throws ServiceException;
    public abstract List<UCService> getAllUCServices()  throws ServiceException;
    public abstract void renameUCService(String zimbraId, String newName) throws ServiceException;

    public String updatePresenceSessionId(String zimbraId, String username, String password)
    throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    /*
     * ==============================
     *
     * Distribution list (static group) methods
     *
     * ==============================
     */

    public abstract DistributionList createDistributionList(String listAddress, Map<String, Object> listAttrs)
    throws ServiceException;

    public abstract DistributionList get(Key.DistributionListBy keyType, String key) throws ServiceException;

    public abstract void deleteDistributionList(String zimbraId) throws ServiceException;

    public abstract void addMembers(DistributionList list, String[] members) throws ServiceException;

    public abstract void removeMembers(DistributionList list, String[] members) throws ServiceException;

    public abstract void addAlias(DistributionList dl, String alias) throws ServiceException;

    public abstract void removeAlias(DistributionList dl, String alias) throws ServiceException;

    public abstract void renameDistributionList(String zimbraId, String newName) throws ServiceException;

    public boolean isDistributionList(String addr) {
        return false;
    }

    /**
     * Like get(DistributionListBy keyType, String key)
     * The difference is this API returns a DistributionList object
     * contains only basic DL attributes.  It does not contain members
     * of the group.
     *
     * Note: in the LdapProvisioning implementation, this API uses cache whereas
     * get(DistributionListBy keyType, String key) does *not* use cache.
     * Callsites should use this API if all they need is basic info on the DL, like
     * id or name.
     */
    public DistributionList getDLBasic(Key.DistributionListBy keyType, String key)
    throws ServiceException {
        return get(keyType, key);
    }


    /*
     * ==============================
     *
     * Dynamic Group methods
     *
     * ==============================
     */
    public DynamicGroup createDynamicGroup(String listAddress, Map<String, Object> listAttrs)
    throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }


    /*
     * ==============================
     *
     * Group (static/dynamic neutral) methods
     *
     * ==============================
     */

    /**
     * create a static or dynamic group
     *
     * @param listAddress
     * @param listAttrs
     * @param dynamic
     * @return
     * @throws ServiceException
     */
    public Group createGroup(String listAddress, Map<String, Object> listAttrs, boolean dynamic)
    throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public Group createDelegatedGroup(String listAddress, Map<String, Object> listAttrs,
            boolean dynamic, Account creator)
    throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public void deleteGroup(String zimbraId) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public void deleteGroup(String zimbraId, boolean cascadeDelete) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public void renameGroup(String zimbraId, String newName) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public Group getGroup(Key.DistributionListBy keyType, String key) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public Group getGroup(Key.DistributionListBy keyType, String key, boolean loadFromMaster, boolean basicAttrsOnly)
    throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }


    /*
     * returns only basic attributes on group, does *not* return members
     */
    public Group getGroupBasic(Key.DistributionListBy keyType, String key) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    /**
     * Get all static distribution lists and dynamic groups
     */
    public List getAllGroups(Domain domain) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public void addGroupMembers(Group group, String[] members) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public void removeGroupMembers(Group group, String[] members) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public void addGroupAlias(Group group, String alias) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public void removeGroupAlias(Group group, String alias) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public String[] getGroupMembers(Group group) throws ServiceException {
        return group.getAllMembers();
    }

    public List<HABGroupMember> getHABGroupMembers(Group group) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    /**
     * @return set of all the zimbraId's of groups this account belongs to, including
     *         dynamic groups and direct/nested static distribution lists.
     * @throws ServiceException
     */
    public Set<String> getGroups(Account acct) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    /**
     *
     * @param directOnly return only DLs this account is a direct member of
     * @param via if non-null and directOnly is false, this map will containing a mapping
     *        from a DL name to the DL it was a member of, if member was indirect.
     * @return all the DLs
     * @throws ServiceException
     */
    public List<Group> getGroups(Account acct, boolean directOnly, Map<String,String> via)
    throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    /**
     * @param zimbraId zimbraId of the group (static or dynamic) we are checking for
     * @return true if this account is a member of the specified group, and the group
     *         is eligible as a grantee of ACL.
     *         If the group is a static group, also true if this account is a member of a
     *         group that is a member of the specified group.
     * @throws ServiceException
     */
    public boolean inACLGroup(Account acct, String zimbraId) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    /*
     * Zimlet
     */

    public abstract Zimlet getZimlet(String name) throws ServiceException;

    public abstract List<Zimlet> listAllZimlets() throws ServiceException;

    public abstract Zimlet createZimlet(String name, Map<String, Object> attrs) throws ServiceException;

    public abstract void deleteZimlet(String name) throws ServiceException;

    /**
     * Creates the specified calendar resource. The A_zimbraId and A_uid attributes are
     * automatically created and should not be passed in.
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
    public abstract CalendarResource createCalendarResource(String emailAddress,
            String password, Map<String, Object> attrs) throws ServiceException;

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

    public abstract CalendarResource get(Key.CalendarResourceBy keyType, String key) throws ServiceException;

    public CalendarResource getCalendarResourceByName(String name) throws ServiceException {
        return get(Key.CalendarResourceBy.name, name);
    }

    public CalendarResource getCalendarResourceById(String id) throws ServiceException {
        return get(Key.CalendarResourceBy.id, id);
    }

    public CalendarResource get(Key.CalendarResourceBy keyType, String key, boolean loadFromMaster) throws ServiceException {
        return get(keyType, key);
    }

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
     * Search for all accounts on the server.
     *
     * Note: Sorting is not supported on search APIs with a visitor.
     *
     * @param server
     * @param opts
     * @param visitor
     * @throws ServiceException
     */
    public void searchAccountsOnServer(Server server, SearchAccountsOptions opts, NamedEntry.Visitor visitor)
    throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    /**
     * Search for all accounts on the server.
     *
     * @param server
     * @param opts
     * @param visitor
     * @throws ServiceException
     */
    public List<NamedEntry> searchAccountsOnServer(Server server, SearchAccountsOptions opts)
    throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public List<NamedEntry> searchDirectory(SearchDirectoryOptions options) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public void searchDirectory(SearchDirectoryOptions options, NamedEntry.Visitor visitor)
    throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

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
         private final List<GalContact> mMatches;
         private String ldapTimeStamp = "";
         private String maxLdapTimeStamp = "";

         private int ldapMatchCount = 0;
         private int limit = 0;

        /*
         * for auto-complete and search only
         *
         * The Ajax client backtracks on GAL results assuming the results of a more
         * specific key is the subset of a more generic key, and it checks cached
         * results instead of issuing another SOAP request to the server.
         * If search key was tokenized with AND or OR, this cannot be assumed.
         */
         private String mTokenizeKey;

         public String getMaxLdapTimeStamp() {
            return maxLdapTimeStamp;
         }

         public void setMaxLdapTimeStamp(String maxLdapTimeStamp) {
           this.maxLdapTimeStamp = maxLdapTimeStamp;
         }

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

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

        public String getLdapTimeStamp() {
            return ldapTimeStamp;
        }

        public void setLdapTimeStamp(String ldapTimeStamp) {
            this.ldapTimeStamp = ldapTimeStamp;
        }

        public int getLdapMatchCount() {
            return ldapMatchCount;
        }

        public void setLdapMatchCount(int ldapMatchCount) {
            this.ldapMatchCount = ldapMatchCount;
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
        private final GalContact.Visitor mVisitor;
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

    public SearchGalResult autoCompleteGal(Domain domain, String query, GalSearchType type, int limit,
            GalContact.Visitor visitor)
    throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public SearchGalResult searchGal(Domain domain, String query, GalSearchType type, int limit,
            GalContact.Visitor visitor)
    throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public SearchGalResult syncGal(Domain domain, String token, GalContact.Visitor visitor)
    throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    /**
     * Interface for CalDAV.  It needs to always search in Zimbra only,
     * regardless of zimbraGalMode configured on the domain.
     *
     * @param d domain
     * @param query LDAP search query
     * @param type address type to search
     * @param mode if given, use the provided mode, if null, use mode(zimbraGalMode) configured on the domain
     * @param token return entries created/modified after timestamp
     * @return List of GalContact objects
     * @throws ServiceException
     */
    public SearchGalResult searchGal(Domain d, String query, GalSearchType type,
            GalMode mode, String token)
    throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public void searchGal(GalSearchParams params) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public Identity getDefaultIdentity(Account account) throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        Set<String> identityAttrs = AttributeManager.getInstance().getAttrsInClass(AttributeClass.identity);

        for (String name : identityAttrs) {
            String value = account.getAttr(name, null);
            if (value != null) attrs.put(name, value);
        }
        if (attrs.get(A_zimbraPrefIdentityName) == null)
            attrs.put(A_zimbraPrefIdentityName, ProvisioningConstants.DEFAULT_IDENTITY_NAME);

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
        return new Identity(account, ProvisioningConstants.DEFAULT_IDENTITY_NAME, account.getId(), attrs, this);
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
    public abstract Identity get(Account account, Key.IdentityBy keyType, String key) throws ServiceException;

    public abstract Signature createSignature(Account account, String signatureName, Map<String, Object> attrs) throws ServiceException;

    public abstract Signature restoreSignature(Account account, String signatureName, Map<String, Object> attrs) throws ServiceException;

    public abstract void modifySignature(Account account, String signatureId, Map<String, Object> attrs) throws ServiceException;

    public abstract void deleteSignature(Account account, String signatureId) throws ServiceException;

    public abstract List<Signature> getAllSignatures(Account account) throws ServiceException;

    public abstract Signature get(Account account, Key.SignatureBy keyType, String key) throws ServiceException;

    public abstract DataSource createDataSource(Account account, DataSourceType type, String dataSourceName, Map<String, Object> attrs) throws ServiceException;
    public abstract DataSource createDataSource(Account account, DataSourceType type, String dataSourceName, Map<String, Object> attrs, boolean passwdAlreadyEncrypted) throws ServiceException;

    public abstract DataSource restoreDataSource(Account account, DataSourceType type, String dataSourceName, Map<String, Object> attrs) throws ServiceException;

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
    public abstract DataSource get(Account account, Key.DataSourceBy keyType, String key) throws ServiceException;

    // XMPPComponents
    public abstract XMPPComponent createXMPPComponent(String name, Domain domain, Server server, Map<String, Object> attrs) throws ServiceException;

    public abstract XMPPComponent get(Key.XMPPComponentBy keyName, String key) throws ServiceException;

    public abstract List<XMPPComponent> getAllXMPPComponents() throws ServiceException;

    public abstract void deleteXMPPComponent(XMPPComponent comp) throws ServiceException;

    public abstract Set<String> createHabOrgUnit(Domain domain, String habOrgUnitName) throws ServiceException;
    public abstract Set<String> listHabOrgUnit(Domain domain) throws ServiceException;
    public abstract Set<String> renameHabOrgUnit(Domain domain, String habOrgUnitName, String newHabOrgUnitName) throws ServiceException;
    public abstract void deleteHabOrgUnit(Domain domain, String habOrgUnitName) throws ServiceException;

    public static class RightsDoc {
        private final String mCmd;
        private final List<String> mRights;
        private final List<String> mNotes;

        public RightsDoc(String cmd) {
            mCmd = cmd;
            mRights = Lists.newArrayList();
            mNotes = Lists.newArrayList();
        }

        public RightsDoc(CmdRightsInfo cmd) {
            this(cmd.getName());
            for (NamedElement right : cmd.getRights())
                addRight(right.getName());
            for (String note : cmd.getNotes())
                addNote(note);
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
        throw ServiceException.UNSUPPORTED();
    }

    public Right getRight(String rightName, boolean expandAllAttrs) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public List<Right> getAllRights(String targetType, boolean expandAllAttrs, String rightClass)  throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public boolean checkRight(
            String targetType, TargetBy targetBy, String target,
            GranteeBy granteeBy, String grantee,
            String right, Map<String, Object> attrs,
            AccessManager.ViaGrant via) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public RightCommand.AllEffectiveRights getAllEffectiveRights(
            String granteeType, GranteeBy granteeBy, String grantee,
            boolean expandSetAttrs, boolean expandGetAttrs) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public RightCommand.EffectiveRights getEffectiveRights(
            String targetType, TargetBy targetBy, String target,
            GranteeBy granteeBy, String grantee,
            boolean expandSetAttrs, boolean expandGetAttrs) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public RightCommand.EffectiveRights getCreateObjectAttrs(
            String targetType,
            Key.DomainBy domainBy, String domainStr,
            Key.CosBy cosBy, String cosStr,
            GranteeBy granteeBy, String grantee) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public RightCommand.Grants getGrants(
            String targetType, TargetBy targetBy, String target,
            String granteeType, GranteeBy granteeBy, String grantee,
            boolean granteeIncludeGroupsGranteeBelongs) throws ServiceException{
        throw ServiceException.UNSUPPORTED();
    }

    public void grantRight(
            String targetType, TargetBy targetBy, String target,
            String granteeType, GranteeBy granteeBy, String grantee, String secret,
            String right, RightModifier rightModifier) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public void revokeRight(
            String targetType, TargetBy targetBy, String target,
            String granteeType, GranteeBy granteeBy, String grantee,
            String right, RightModifier rightModifier) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public ShareLocator getShareLocatorById(String id) throws ServiceException { return get(Key.ShareLocatorBy.id, id); }

    public abstract ShareLocator get(Key.ShareLocatorBy keyType, String key) throws ServiceException;
    public abstract ShareLocator createShareLocator(String id, Map<String, Object> attrs) throws ServiceException;
    public abstract void deleteShareLocator(String id) throws ServiceException;

    public ShareLocator createShareLocator(String id, String ownerAccountId) throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraShareOwnerAccountId, ownerAccountId);
        return createShareLocator(id, attrs);
    }

    public static class CacheEntry {
        public Key.CacheEntryBy mEntryBy;
        public String mEntryIdentity;
        public CacheEntry(Key.CacheEntryBy entryBy, String entryIdentity) {
            mEntryBy = entryBy;
            mEntryIdentity = entryIdentity;
        }
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
        private final List<CountAccountByCos> mCountAccountByCos = new ArrayList<CountAccountByCos>();

        public static class CountAccountByCos {
            private final String mCosId;
            private final String mCosName;
            private final long mCount;

            CountAccountByCos(String cosId, String cosName, long count) {
                mCosId = cosId;
                mCosName = cosName;
                mCount = count;
            }

            public String getCosId()   { return mCosId;}
            public String getCosName() { return mCosName; }
            public long getCount()        { return mCount; }
        }

        public void addCountAccountByCosResult(String cosId, String cosName, long count) {
            CountAccountByCos r = new CountAccountByCos(cosId, cosName, count);
            mCountAccountByCos.add(r);
        }

        public List<CountAccountByCos> getCountAccountByCos() {
            return mCountAccountByCos;
        }

    }

    public CountAccountResult countAccount(Domain domain) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public long countObjects(CountObjectsType type, Domain domain, UCService ucService)
    throws ServiceException {
        throw ServiceException.UNSUPPORTED();
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
        return StringUtil.isUUID(value);
    }

    /**
     * Get auth token for proxying. Only implemented in OfflineProvisioning
     * @param targetAcctId - the account we are proxying to
     * @param originalContext - the original request context. Used internally to ensure proxy token is obtained for correct mountpoint account
     */
    public String getProxyAuthToken(String targetAcctId, Map<String,Object> originalContext) throws ServiceException {
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

    public static interface PublishedShareInfoVisitor {
        public void visit(ShareInfoData shareInfoData) throws ServiceException;
    }

    public void getShareInfo(Account ownerAcct, PublishedShareInfoVisitor visitor) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
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
        static final String RENAME_ACCOUNT = "renameAccount";
        static final String RENAME_ACCOUNT_CHECK_DOMAIN_COS_AND_FEATURE =
            "renameAccountCheckDomainCosAndFeature";
        static final String DELETE_ACCOUNT_SUCCEEDED = "deleteAccountSucceeded";

        void validate(Provisioning prov, String action, Object... args) throws ServiceException;
        void refresh();
    }

    public static class Result {
        private final String code;
        private final String message;
        private final String detail;

        public String getCode() { return code; }
        public String getMessage() { return message; }
        public String getComputedDn() {return detail; }
        public Object getDetail() { return  detail; }

        public Result(String status, String message, String detail) {
            this.code = status;
            this.message = message;
            this.detail = detail;
        }

        public Result(String status, Exception e, String detail) {
            this.code = status;
            this.message = ExceptionToString.ToString(e);
            this.detail = detail;
        }

        @Override
        public String toString() {
            return "Result { code: "+code+" detail: "+detail+" message: "+message+" }";
        }
    }

    public Provisioning.Result checkAuthConfig(Map<String, Object> attrs, String name, String password)
    throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public static class GalResult extends Result {
        private final List<GalContact> mResult;
        public GalResult(String status, String message, List<GalContact> result) {
            super(status, message, null);
            mResult = result;
        }

        public List<GalContact> getContacts() {
            return mResult;
        }
    }

    public Provisioning.Result checkGalConfig(Map attrs, String query, int limit, GalOp galOp)
    throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    //
    //
    // SMIME config
    //
    //
    // SMIME config on domain
    public Map<String, Map<String, Object>> getDomainSMIMEConfig(Domain domain, String configName) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public void modifyDomainSMIMEConfig(Domain domain, String configName, Map<String, Object> attrs) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public void removeDomainSMIMEConfig(Domain domain, String configName) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    // SMIME config on globalconfig
    public Map<String, Map<String, Object>> getConfigSMIMEConfig(String configName) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public void modifyConfigSMIMEConfig(String configName, Map<String, Object> attrs) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public void removeConfigSMIMEConfig(String configName) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public AddressList getAddressList(String id) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public AddressListInfo getAddressListByName(String name, Domain domain) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }


    public List<AddressListInfo> getAllAddressLists(Domain domain, boolean activeOnly) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public void deleteAddressList(String addressListId) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public void modifyAddressList(AddressList addressList, String name, Map<String, String> attrs)
        throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    //
    //
    // Validators
    //
    //
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

    /**
     * @param domain
     * @param rootDn
     * @return
     */
    public List<LdapDistributionList> getAllHabGroups(Domain domain, String rootDn) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    /**
     * @param oldDn
     * @param parentDn
     */
    public void changeHABGroupParent(String oldDn, String parentDn) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

   /**
    *
    * @param group Dynamic group
    * @param dlsToCheck list of dl to be checked for membership of dynamic group
    * @return true if one of the dl is a member of dynamic group
    * @throws ServiceException
    */
    public boolean  dlIsInDynamicHABGroup(DynamicGroup group, List<String> dlsToCheck)
        throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }
    // address list
    public String createAddressList(Domain domain, String name, String desc, Map<String, Object> attrs) throws ServiceException {
        throw new UnsupportedOperationException("Currently address list is not supported.");
    }
    
    public abstract String sendMdmEmail(String status, String timeInterval) throws ServiceException;
}
