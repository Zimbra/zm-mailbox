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

package com.zimbra.cs.account.ldap;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.service.ServiceException.Argument;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.EmailUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountCache;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.DomainCache.GetFromDomainCacheOption;
import com.zimbra.cs.account.Alias;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.DomainCache;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.EntryCacheDataKey;
import com.zimbra.cs.account.EntrySearchFilter;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.GlobalGrant;
import com.zimbra.cs.account.GroupedEntry;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.IDNUtil;
import com.zimbra.cs.account.Identity;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.NamedEntryCache;
import com.zimbra.cs.account.PreAuthKey;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Signature;
import com.zimbra.cs.account.XMPPComponent;
import com.zimbra.cs.account.Zimlet;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightCommand;
import com.zimbra.cs.account.accesscontrol.RightCommand.EffectiveRights;
import com.zimbra.cs.account.accesscontrol.RightModifier;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.account.auth.AuthMechanism;
import com.zimbra.cs.account.auth.PasswordUtil;
import com.zimbra.cs.account.callback.MailSignature;
import com.zimbra.cs.account.gal.GalNamedFilter;
import com.zimbra.cs.account.gal.GalOp;
import com.zimbra.cs.account.gal.GalParams;
import com.zimbra.cs.account.gal.GalUtil;
import com.zimbra.cs.account.krb5.Krb5Principal;
import com.zimbra.cs.account.names.NameUtil;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mime.MimeTypeInfo;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.zimlet.ZimletException;
import com.zimbra.cs.zimlet.ZimletUtil;

import javax.naming.AuthenticationException;
import javax.naming.AuthenticationNotSupportedException;
import javax.naming.ContextNotEmptyException;
import javax.naming.InvalidNameException;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.SizeLimitExceededException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.InvalidAttributeIdentifierException;
import javax.naming.directory.InvalidAttributeValueException;
import javax.naming.directory.InvalidAttributesException;
import javax.naming.directory.InvalidSearchFilterException;
import javax.naming.directory.SchemaViolationException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

/**
 * LDAP implementation of {@link Provisioning}.
 *
 * @since Sep 23, 2004
 * @author schemers
 */
public class LdapProvisioning extends Provisioning {

    // object classes
    public static final String C_zimbraAccount = "zimbraAccount";
    public static final String C_zimbraCOS = "zimbraCOS";
    public static final String C_zimbraDomain = "zimbraDomain";
    public static final String C_zimbraMailList = "zimbraDistributionList";
    public static final String C_zimbraMailRecipient = "zimbraMailRecipient";
    public static final String C_zimbraServer = "zimbraServer";
    public static final String C_zimbraCalendarResource = "zimbraCalendarResource";
    public static final String C_zimbraAlias = "zimbraAlias";
    public static final String C_zimbraMimeEntry = "zimbraMimeEntry";

    private static final long ONE_DAY_IN_MILLIS = 1000*60*60*24;

    private static final SearchControls sObjectSC = new SearchControls(SearchControls.OBJECT_SCOPE, 0, 0, null, false, false);

    static final SearchControls sSubtreeSC = new SearchControls(SearchControls.SUBTREE_SCOPE, 0, 0, null, false, false);

    private static final Log mLog = LogFactory.getLog(LdapProvisioning.class);

    private static LdapConfig sConfig = null;

    private static GlobalGrant sGlobalGrant = null;

    private static final String[] sInvalidAccountCreateModifyAttrs = {
            Provisioning.A_zimbraMailAlias,
            Provisioning.A_zimbraMailDeliveryAddress,
            Provisioning.A_uid
    };

    private static final String[] sMinimalDlAttrs = {
            Provisioning.A_zimbraMailAlias,
            Provisioning.A_zimbraId,
            Provisioning.A_uid,
            Provisioning.A_zimbraACE,
            Provisioning.A_zimbraIsAdminGroup,
            Provisioning.A_zimbraAdminConsoleUIComponents
    };


    private static AccountCache sAccountCache =
        new AccountCache(
                LC.ldap_cache_account_maxsize.intValue(),
                LC.ldap_cache_account_maxage.intValue() * Constants.MILLIS_PER_MINUTE);

    private static NamedEntryCache<LdapCos> sCosCache =
        new NamedEntryCache<LdapCos>(
                LC.ldap_cache_cos_maxsize.intValue(),
                LC.ldap_cache_cos_maxage.intValue() * Constants.MILLIS_PER_MINUTE);

    private static DomainCache sDomainCache =
        new DomainCache(
                LC.ldap_cache_domain_maxsize.intValue(),
                LC.ldap_cache_domain_maxage.intValue() * Constants.MILLIS_PER_MINUTE,
                LC.ldap_cache_external_domain_maxsize.intValue(),
                LC.ldap_cache_external_domain_maxage.intValue() * Constants.MILLIS_PER_MINUTE);

    private static NamedEntryCache<Server> sServerCache =
        new NamedEntryCache<Server>(
                LC.ldap_cache_server_maxsize.intValue(),
                LC.ldap_cache_server_maxage.intValue() * Constants.MILLIS_PER_MINUTE);

    private static NamedEntryCache<LdapZimlet> sZimletCache =
        new NamedEntryCache<LdapZimlet>(
                LC.ldap_cache_zimlet_maxsize.intValue(),
                LC.ldap_cache_zimlet_maxage.intValue() * Constants.MILLIS_PER_MINUTE);


    private static NamedEntryCache<DistributionList> sAclGroupCache =
        new NamedEntryCache<DistributionList>(
                LC.ldap_cache_group_maxsize.intValue(),
                LC.ldap_cache_group_maxage.intValue() * Constants.MILLIS_PER_MINUTE);

    // TODO: combine with sAclGroupCache
    //       note: DLs cached in this cache only contains sMinimalDlAttrs
    private static NamedEntryCache<DistributionList> sDLCache =
        new NamedEntryCache<DistributionList>(
                LC.ldap_cache_group_maxsize.intValue(),
                LC.ldap_cache_group_maxage.intValue() * Constants.MILLIS_PER_MINUTE);

    private static NamedEntryCache<XMPPComponent> sXMPPComponentCache =
        new NamedEntryCache<XMPPComponent>(
                LC.ldap_cache_xmppcomponent_maxsize.intValue(),
                LC.ldap_cache_xmppcomponent_maxage.intValue() * Constants.MILLIS_PER_MINUTE);

    public int getAccountCacheSize() { return sAccountCache.getSize(); }
    public double getAccountCacheHitRate() { return sAccountCache.getHitRate(); }
    public int getCosCacheSize() { return sCosCache.getSize(); }
    public double getCosCacheHitRate() { return sCosCache.getHitRate(); }
    public int getDomainCacheSize() { return sDomainCache.getSize(); }
    public double getDomainCacheHitRate() { return sDomainCache.getHitRate(); }
    public int getServerCacheSize() { return sServerCache.getSize(); }
    public double getServerCacheHitRate() { return sServerCache.getHitRate(); }
    public int getZimletCacheSize() { return sZimletCache.getSize(); }
    public double getZimletCacheHitRate() { return sZimletCache.getHitRate(); }
    public int getGroupCacheSize() { return sAclGroupCache.getSize(); }
    public double getGroupCacheHitRate() { return sAclGroupCache.getHitRate(); }
    public int getXMPPCacheSize() { return sXMPPComponentCache.getSize(); }
    public double getXMPPCacheHitRate() { return sXMPPComponentCache.getHitRate(); }

    protected LdapDIT mDIT;

    public LdapProvisioning() {
        setDIT();
        register(new Validators.DomainAccountValidator());
        register(new Validators.DomainMaxAccountsValidator());
    }

    protected void setDIT() {
        mDIT = new LdapDIT(this);
    }

    public LdapDIT getDIT() {
        return mDIT;
    }

    /*
     * Contains parallel arrays of old addrs and new addrs as a result of domain change
     */
    protected static class ReplaceAddressResult {
        ReplaceAddressResult(String oldAddrs[], String newAddrs[]) {
            mOldAddrs = oldAddrs;
            mNewAddrs = newAddrs;
        }
        private String mOldAddrs[];
        private String mNewAddrs[];

        public String[] oldAddrs() { return mOldAddrs; }
        public String[] newAddrs() { return mNewAddrs; }
    }

    private static final Random sPoolRandom = new Random();

    @Override
    public void modifyAttrs(Entry e, Map<String, ? extends Object> attrs, boolean checkImmutable)
            throws ServiceException {
        modifyAttrs(e, attrs, checkImmutable, true);
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
    @Override
    public void modifyAttrs(Entry e, Map<String, ? extends Object> attrs, boolean checkImmutable, boolean allowCallback)
            throws ServiceException {
        Map<Object, Object> context = new HashMap<Object, Object>();
        AttributeManager.getInstance().preModify(attrs, e, context, false, checkImmutable, allowCallback);
        modifyAttrsInternal(e, null, attrs);
        AttributeManager.getInstance().postModify(attrs, e, context, false, allowCallback);
    }

    /**
     * should only be called internally.
     *
     * @param initCtxt
     * @param attrs
     * @throws ServiceException
     */
    protected void modifyAttrsInternal(Entry entry, ZimbraLdapContext initZlc, Map<?, ?> attrs)
            throws ServiceException {
        ZimbraLdapContext zlc = initZlc;
        try {
            if (entry instanceof Account && !(entry instanceof CalendarResource)) {
                Account acct = (Account) entry;
                validate(ProvisioningValidator.MODIFY_ACCOUNT_CHECK_DOMAIN_COS_AND_FEATURE,
                        acct.getAttr(A_zimbraMailDeliveryAddress), attrs, acct);
            }
            if (zlc == null)
                zlc = new ZimbraLdapContext(true);
            LdapUtil.modifyAttrs(zlc, ((LdapEntry)entry).getDN(), attrs, entry);
            refreshEntry(entry, zlc, this);
        } catch (InvalidAttributeIdentifierException e) {
            throw AccountServiceException.INVALID_ATTR_NAME(
                    "invalid attr name: " + e.getMessage(), e);
        } catch (InvalidAttributeValueException e) {
            throw AccountServiceException.INVALID_ATTR_VALUE(
                    "invalid attr value: " + e.getMessage(), e);
        } catch (InvalidAttributesException e) {
            throw ServiceException.INVALID_REQUEST(
                    "invalid set of attributes: " + e.getMessage(), e);
        } catch (SchemaViolationException e) {
            throw ServiceException.INVALID_REQUEST("LDAP schema violation: "
                    + e.getMessage(), e);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to modify attrs: "
                    + e.getMessage(), e);
        } finally {
            if (initZlc == null)
                ZimbraLdapContext.closeContext(zlc);
        }
    }

    /**
     * reload/refresh the entry from the ***master***.
     */
    @Override
    public void reload(Entry e) throws ServiceException {
        reload(e, true);
    }

    @Override
    public void reload(Entry e, boolean master) throws ServiceException {

        ZimbraLdapContext zlc = null;
        try {
            new ZimbraLdapContext(master);
            refreshEntry(e, zlc, this);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }

    void refreshEntry(Entry entry, ZimbraLdapContext initZlc, LdapProvisioning prov) throws ServiceException {

        ZimbraLdapContext zlc = initZlc;
        try {
            if (zlc == null)
                zlc = new ZimbraLdapContext();
            String dn = ((LdapEntry)entry).getDN();
            Attributes attributes = zlc.getAttributes(dn);
            Map<String, Object> attrs = LdapUtil.getAttrs(attributes);

            Map<String,Object> defaults = null;
            Map<String,Object> secondaryDefaults = null;

            if (entry instanceof Account) {
                //
                // We can get here from either modifyAttrsInternal or reload path.
                //
                // If we got here from modifyAttrsInternal, zimbraCOSId on account
                // might have been changed, added, removed, but entry now still contains
                // the old attrs.  Create a temp Account object from the new attrs, and then
                // use the same cos of the temp Account object for our entry object.
                //
                // If we got here from reload, attrs are likely not changed, the callsites
                // just want a refreshed object.  For this case it's best if we still
                // always resolve the COS correctly.  makeAccount is a cheap call and won't
                // add any overhead like loading cos/domain from LDAP: even if cos/domain
                // has to be loaded (because not in cache) in the getCOS(temp) call, it's
                // just the same as calling (buggy) getCOS(entry) before.
                //
                // We only need the temp object for the getCOS call, don't need to setup
                // primary/secondary defaults on the temp object because:
                //     zimbraCOSId is only on account(of course), and that's all needed
                //     for determining the COS for the account in the getCOS call: if
                //     zimbraCOSId is not set on account, it will fallback to the domain
                //     default COS, then fallback to the system default COS.
                //
                Account temp = makeAccountNoDefaults(dn, attributes);
                Cos cos = prov.getCOS(temp);
                if (cos != null)
                    defaults = cos.getAccountDefaults();
                Domain domain = prov.getDomain((Account)entry);
                if (domain != null)
                    secondaryDefaults = domain.getAccountDefaults();
            } else if (entry instanceof Domain) {
                defaults = prov.getConfig().getDomainDefaults();
            } else if (entry instanceof Server) {
                defaults = prov.getConfig().getServerDefaults();
            }

            if (defaults == null && secondaryDefaults == null)
                entry.setAttrs(attrs);
            else
                entry.setAttrs(attrs, defaults, secondaryDefaults);

        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to refresh entry", e);
        } finally {
            if (initZlc == null)
                ZimbraLdapContext.closeContext(zlc);
        }

    }

    // TODO: not in use, delete after the new code is settled for a while
    void refreshEntry_old(Entry entry, ZimbraLdapContext initZlc, LdapProvisioning prov)
    throws ServiceException {
        ZimbraLdapContext zlc = initZlc;
        try {
            Map<String,Object> defaults = null;
            Map<String,Object> secondaryDefaults = null;

            if (entry instanceof Account) {
                Cos cos = prov.getCOS((Account)entry);
                if (cos != null)
                    defaults = cos.getAccountDefaults();
                Domain domain = prov.getDomain((Account)entry);
                if (domain != null)
                    secondaryDefaults = domain.getAccountDefaults();
            } else if (entry instanceof Domain) {
                defaults = prov.getConfig().getDomainDefaults();
            } else if (entry instanceof Server) {
                defaults = prov.getConfig().getServerDefaults();
            }

            if (zlc == null)
                zlc = new ZimbraLdapContext();
            String dn = ((LdapEntry)entry).getDN();
            if (defaults == null && secondaryDefaults == null)
                entry.setAttrs(LdapUtil.getAttrs(zlc.getAttributes(dn)));
            else
                entry.setAttrs(LdapUtil.getAttrs(zlc.getAttributes(dn)), defaults, secondaryDefaults);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to refresh entry", e);
        } finally {
            if (initZlc == null)
                ZimbraLdapContext.closeContext(zlc);
        }
    }


    /**
     * Status check on LDAP connection.  Search for global config entry.
     */
    @Override
    public boolean healthCheck() throws ServiceException {
        boolean result = false;
        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext();
            Attributes attrs = zlc.getAttributes(mDIT.configDN());
            result = attrs != null;
        } catch (NamingException e) {
            mLog.warn("LDAP health check error", e);
        } catch (ServiceException e) {
            mLog.warn("LDAP health check error", e);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
        return result;
    }

    @Override
    public Config getConfig() throws ServiceException
    {
        // TODO: failure scenarios? fallback to static config file or hard-coded defaults?
        // double-checked-locking is broken
        if (sConfig == null) {
            synchronized(LdapProvisioning.class) {
                if (sConfig == null) {
                    ZimbraLdapContext zlc = null;
                    try {
                        String configDn = mDIT.configDN();
                        zlc = new ZimbraLdapContext();
                        Attributes attrs = zlc.getAttributes(configDn);
                        sConfig = new LdapConfig(configDn, attrs, this);
                    } catch (NamingException e) {
                        throw ServiceException.FAILURE("unable to get config", e);
                    } finally {
                        ZimbraLdapContext.closeContext(zlc);
                    }
                }
            }
        }
        return sConfig;
    }

    @Override
    public GlobalGrant getGlobalGrant() throws ServiceException
    {
        // TODO: failure scenarios? fallback to static config file or hard-coded defaults?
        if (sGlobalGrant == null) {
            synchronized(LdapProvisioning.class) {
                if (sGlobalGrant == null) {
                    ZimbraLdapContext zlc = null;
                    try {
                        String globalGrantDn = mDIT.globalGrantDN();
                        zlc = new ZimbraLdapContext();
                        Attributes attrs = zlc.getAttributes(globalGrantDn);
                        sGlobalGrant = new LdapGlobalGrant(globalGrantDn, attrs, this);
                    } catch (NamingException e) {
                        throw ServiceException.FAILURE("unable to get globalgrant", e);
                    } finally {
                        ZimbraLdapContext.closeContext(zlc);
                    }
                }
            }
        }
        return sGlobalGrant;
    }

    @Override
    public List<MimeTypeInfo> getMimeTypes(String mimeType) throws ServiceException {
        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext();
            mimeType = LdapUtil.escapeSearchFilterArg(mimeType);
            NamingEnumeration<SearchResult> ne = zlc.searchDir(
                    mDIT.mimeBaseDN(), LdapFilter.mimeEntryByMimeType(mimeType), sSubtreeSC);
            List<MimeTypeInfo> mimeTypes = new ArrayList<MimeTypeInfo>();
            while (ne.hasMore()) {
                SearchResult sr = ne.next();
                mimeTypes.add(new LdapMimeType(sr.getNameInNamespace(), sr.getAttributes(), this));
            }
            ne.close();
            return mimeTypes;
        } catch (NameNotFoundException e) {
            return Collections.emptyList();
        } catch (InvalidNameException e) {
            return Collections.emptyList();
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to get mime types for " + mimeType, e);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }

    @Override
    public List<MimeTypeInfo> getAllMimeTypes() throws ServiceException {
        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext();
            List<MimeTypeInfo> mimeTypes = new ArrayList<MimeTypeInfo>();
            NamingEnumeration<SearchResult> ne = zlc.searchDir(
                    mDIT.mimeBaseDN(), LdapFilter.allMimeEntries(), sSubtreeSC);
            while (ne.hasMore()) {
                SearchResult sr = ne.next();
                mimeTypes.add(new LdapMimeType(sr.getNameInNamespace(), sr.getAttributes(), this));
            }
            ne.close();
            return mimeTypes;
        } catch (NameNotFoundException e) {
            return Collections.emptyList();
        } catch (InvalidNameException e) {
            return Collections.emptyList();
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to get mime types", e);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }

    }

    @Override
    public List<Zimlet> getObjectTypes() throws ServiceException {
        return listAllZimlets();
    }

    private Account getAccountByQuery(String base, String query, ZimbraLdapContext initZlc, boolean loadFromMaster) throws ServiceException {
        ZimbraLdapContext zlc = initZlc;
        try {
            if (zlc == null)
                zlc = new ZimbraLdapContext(loadFromMaster);
            NamingEnumeration<SearchResult> ne = zlc.searchDir(base, query, sSubtreeSC);
            if (ne.hasMore()) {
                SearchResult sr = ne.next();
                if (ne.hasMore()) {
                    String dups = LdapUtil.formatMultipleMatchedEntries(sr, ne);
                    throw AccountServiceException.MULTIPLE_ACCOUNTS_MATCHED("getAccountByQuery: "+query+" returned multiple entries at "+dups);
                }
                ne.close();
                return makeAccount(sr.getNameInNamespace(), sr.getAttributes());
            }
        } catch (NameNotFoundException e) {
            return null;
        } catch (InvalidNameException e) {
            return null;
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to lookup account via query: "+query+" message: "+e.getMessage(), e);
        } finally {
            if (initZlc == null)
                ZimbraLdapContext.closeContext(zlc);
        }
        return null;
    }

    private Account getAccountById(String zimbraId, ZimbraLdapContext zlc, boolean loadFromMaster) throws ServiceException {
        if (zimbraId == null)
            return null;
        Account a = sAccountCache.getById(zimbraId);
        if (a == null) {
            zimbraId = LdapUtil.escapeSearchFilterArg(zimbraId);
            String query = LdapFilter.accountById(zimbraId);

            a = getAccountByQuery(mDIT.mailBranchBaseDN(), query, zlc, loadFromMaster);

            // search again under the admin base if not found and admin base is not under mail base
            if (a == null && !mDIT.isUnder(mDIT.mailBranchBaseDN(), mDIT.adminBaseDN()))
                a = getAccountByQuery(mDIT.adminBaseDN(), query, zlc, loadFromMaster);

            sAccountCache.put(a);
        }
        return a;
    }

    @Override
    public Account get(AccountBy keyType, String key) throws ServiceException {
        return get(keyType, key, false);
    }

    @Override
    public Account get(AccountBy keyType, String key, boolean loadFromMaster) throws ServiceException {
        switch(keyType) {
        case adminName:
            return getAdminAccountByName(key, loadFromMaster);
        case appAdminName:
            return getAppAdminAccountByName(key, loadFromMaster);
        case id:
            return getAccountById(key, null, loadFromMaster);
        case foreignPrincipal:
            return getAccountByForeignPrincipal(key, loadFromMaster);
        case name:
            return getAccountByName(key, loadFromMaster);
        case krb5Principal:
            return Krb5Principal.getAccountFromKrb5Principal(key, loadFromMaster);
        default:
            return null;
        }
    }

    public Account getFromCache(AccountBy keyType, String key) throws ServiceException {
        switch(keyType) {
        case adminName:
            return sAccountCache.getByName(key);
        case id:
            return sAccountCache.getById(key);
        case foreignPrincipal:
            return sAccountCache.getByForeignPrincipal(key);
        case name:
            return sAccountCache.getByName(key);
        case krb5Principal:
            throw ServiceException.FAILURE("key type krb5Principal is not supported by getFromCache", null);
        default:
            return null;
        }
    }

    private Account getAccountByForeignPrincipal(String foreignPrincipal, boolean loadFromMaster) throws ServiceException {
        Account a = sAccountCache.getByForeignPrincipal(foreignPrincipal);

        // bug 27966, always do a search so dup entries can be thrown
        foreignPrincipal = LdapUtil.escapeSearchFilterArg(foreignPrincipal);
        Account acct = getAccountByQuery(
                mDIT.mailBranchBaseDN(),
                LdapFilter.accountByForeignPrincipal(foreignPrincipal),
                null, loadFromMaster);

        // all is well, put the account in cache if it was not in cache
        // this is so we don't change our caching behavior - the above search was just to check for dup - we did that anyway
        // before bug 23372 was fixed.
        if (a == null) {
            a = acct;
            sAccountCache.put(a);
        }
        return a;
    }

    private Account getAdminAccountByName(String name, boolean loadFromMaster) throws ServiceException {
        Account a = sAccountCache.getByName(name);
        if (a == null) {
            name = LdapUtil.escapeSearchFilterArg(name);
            a = getAccountByQuery(
                    mDIT.adminBaseDN(),
                    LdapFilter.adminAccountByRDN(mDIT.accountNamingRdnAttr(), name),
                    null, loadFromMaster);
            sAccountCache.put(a);
        }
        return a;
    }

    private Account getAppAdminAccountByName(String name, boolean loadFromMaster) throws ServiceException {
        Account a = sAccountCache.getByName(name);
        if (a == null) {
            name = LdapUtil.escapeSearchFilterArg(name);
            a = getAccountByQuery(
                    mDIT.appAdminBaseDN(),
                    LdapFilter.adminAccountByRDN(mDIT.accountNamingRdnAttr(), name),
                    null, loadFromMaster);
            sAccountCache.put(a);
        }
        return a;
    }

    private String fixupAccountName(String emailAddress) throws ServiceException {
        int index = emailAddress.indexOf('@');
        String domain = null;
        if (index == -1) {
            // domain is already in ASCII name
            domain = getConfig().getAttr(Provisioning.A_zimbraDefaultDomainName, null);
            if (domain == null)
                throw ServiceException.INVALID_REQUEST("must be valid email address: "+emailAddress, null);
            else
                emailAddress = emailAddress + "@" + domain;
        } else
            emailAddress = IDNUtil.toAsciiEmail(emailAddress);

        return emailAddress;
    }

    private Account getAccountByName(String emailAddress, boolean loadFromMaster) throws ServiceException {

        Account account = getAccountByNameInternal(emailAddress, loadFromMaster);

        // if not found, see if the domain is an alias domain and if so try to get account by the alias domain target
        if (account == null) {
            String addrByDomainAlias = getEmailAddrByDomainAlias(emailAddress);
            if (addrByDomainAlias != null)
                account = getAccountByNameInternal(addrByDomainAlias, loadFromMaster);
        }

        return account;
    }

    private Account getAccountByNameInternal(String emailAddress, boolean loadFromMaster) throws ServiceException {

        emailAddress = fixupAccountName(emailAddress);

        Account account = sAccountCache.getByName(emailAddress);
        if (account == null) {
            emailAddress = LdapUtil.escapeSearchFilterArg(emailAddress);
            account = getAccountByQuery(
                    mDIT.mailBranchBaseDN(),
                    LdapFilter.accountByName(emailAddress),
                    null, loadFromMaster);
            sAccountCache.put(account);
        }
        return account;
    }


    private Cos lookupCos(String key, ZimbraLdapContext zlc) throws ServiceException {
        Cos c = null;
        c = getCosById(key, zlc);
        if (c == null)
            c = getCosByName(key, zlc);
        if (c == null)
            throw AccountServiceException.NO_SUCH_COS(key);
        else
            return c;
    }

    @Override
    public Account createAccount(String emailAddress, String password, Map<String, Object> attrs) throws ServiceException {
        return createAccount(emailAddress, password, attrs, mDIT.handleSpecialAttrs(attrs), null, false, null);
    }

    @Override
    public Account restoreAccount(String emailAddress, String password,
            Map<String, Object> attrs, Map<String, Object> origAttrs) throws ServiceException {
        return createAccount(emailAddress, password, attrs, mDIT.handleSpecialAttrs(attrs), null, true, origAttrs);
    }

    private Account createAccount(String emailAddress,
                                  String password,
                                  Map<String, Object> acctAttrs,
                                  SpecialAttrs specialAttrs,
                                  String[] additionalObjectClasses,
                                  boolean restoring,
                                  Map<String, Object> origAttrs) throws ServiceException {

        String uuid = specialAttrs.getZimbraId();
        String baseDn = specialAttrs.getLdapBaseDn();

        emailAddress = emailAddress.toLowerCase().trim();
        String parts[] = emailAddress.split("@");
        if (parts.length != 2)
            throw ServiceException.INVALID_REQUEST("must be valid email address: "+emailAddress, null);

        String localPart = parts[0];
        String domain = parts[1];
        domain = IDNUtil.toAsciiDomainName(domain);
        emailAddress = localPart + "@" + domain;

        validEmailAddress(emailAddress);

        if (restoring) {
            validate(ProvisioningValidator.CREATE_ACCOUNT,
                    emailAddress, additionalObjectClasses, origAttrs);
            validate(ProvisioningValidator.CREATE_ACCOUNT_CHECK_DOMAIN_COS_AND_FEATURE,
                    emailAddress, origAttrs);
        } else {
            validate(ProvisioningValidator.CREATE_ACCOUNT,
                    emailAddress, additionalObjectClasses, acctAttrs);
            validate(ProvisioningValidator.CREATE_ACCOUNT_CHECK_DOMAIN_COS_AND_FEATURE,
                    emailAddress, acctAttrs);
        }

        Map<?, ?> attrManagerContext = new HashMap<Object, Object>();
        if (acctAttrs == null) {
            acctAttrs = new HashMap<String, Object>();
        }
        AttributeManager.getInstance().preModify(acctAttrs, null, attrManagerContext, true, true);

        String dn = null;
        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext(true);

            Domain d = getDomainByAsciiName(domain, zlc);
            if (d == null)
                throw AccountServiceException.NO_SUCH_DOMAIN(domain);
            String domainType = d.getAttr(Provisioning.A_zimbraDomainType, Provisioning.DOMAIN_TYPE_LOCAL);
            if (!domainType.equals(Provisioning.DOMAIN_TYPE_LOCAL))
                throw ServiceException.INVALID_REQUEST("domain type must be local", null);

            Attributes attrs = new BasicAttributes(true);
            LdapUtil.mapToAttrs(acctAttrs, attrs);

            for (int i=0; i < sInvalidAccountCreateModifyAttrs.length; i++) {
                String a = sInvalidAccountCreateModifyAttrs[i];
                if (attrs.get(a) != null)
                    throw ServiceException.INVALID_REQUEST("invalid attribute for CreateAccount: "+a, null);
            }

            Set<String> ocs;
            if (additionalObjectClasses == null) {
                // We are creating a pure account object, get all object classes for account.
                //
                // If restoring, only add zimbra default object classes, do not add extra
                // ones configured.  After createAccount, the restore code will issue a
                // modifyAttrs call and all object classes in the backed up account will be
                // in the attr map passed to modifyAttrs.
                //
                ocs = LdapObjectClass.getAccountObjectClasses(this, restoring);
            } else {
                // We are creating a "subclass" of account (e.g. calendar resource), get just the
                // zimbra default object classes for account, then add extra object classes needed
                // by the subclass.  All object classes needed by the subclass (calendar resource)
                // were figured out in the createCalendarResource method: including the zimbra
                // default (zimbracalendarResource) and any extra ones configured via
                // globalconfig.zimbraCalendarResourceExtraObjectClass.
                //
                // It doesn't matter if the additionalObjectClasses already contains object classes
                // added by the getAccountObjectClasses(this, true).  When additional object classes
                // are added to the set, duplicated once will only appear once.
                //
                //
                // The "restoring" flag is ignored in this path.
                // When restoring a calendar a resource, the restoring code:
                //     - always calls createAccount, not createCalendarResource
                //     - always pass null for additionalObjectClasses
                //     - like restoring an account, it will call modifyAttrs after the
                //       entry is created, any object classes in the backed up data
                //       will be in the attr map passed to modifyAttrs.
                ocs = LdapObjectClass.getAccountObjectClasses(this, true);
                for (int i = 0; i < additionalObjectClasses.length; i++)
                    ocs.add(additionalObjectClasses[i]);
            }

            /* bug 48226
             *
             * Check if any of the OCs in the backup is a structural OC that subclasses
             * our default OC: organizationalPerson.  If so, add that OC now while creating
             * the account, because it cannot be modified later.
             */
            if (restoring && origAttrs != null) {
                Object ocsInBackupObj = origAttrs.get(A_objectClass);
                String[] ocsInBackup;
                if (ocsInBackupObj instanceof String) {
                    ocsInBackup = new String[1];
                    ocsInBackup[0] = (String)ocsInBackupObj;
                } else if (ocsInBackupObj instanceof String[]) {
                    ocsInBackup = (String[])ocsInBackupObj;
                } else {
                    throw ServiceException.FAILURE("internal error", null);
                }

                String mostSpecificOC = LdapObjectClassHierarchy.getMostSpecificOC(ocsInBackup, LdapObjectClass.ZIMBRA_DEFAULT_PERSON_OC);

                if (!LdapObjectClass.ZIMBRA_DEFAULT_PERSON_OC.equalsIgnoreCase(mostSpecificOC))
                    ocs.add(mostSpecificOC);
            }

            LdapUtil.addAttr(attrs, A_objectClass, ocs);


            String zimbraIdStr;
            if (uuid == null)
                zimbraIdStr = LdapUtil.generateUUID();
            else
                zimbraIdStr = uuid;
            attrs.put(A_zimbraId, zimbraIdStr);
            attrs.put(A_zimbraCreateTimestamp, DateUtil.toGeneralizedTime(new Date()));

            // default account status is active
            if (attrs.get(Provisioning.A_zimbraAccountStatus) == null)
                attrs.put(A_zimbraAccountStatus, Provisioning.ACCOUNT_STATUS_ACTIVE);

            Cos cos = null;
            Attribute cosIdAttr = attrs.get(Provisioning.A_zimbraCOSId);
            String cosId = null;

            if (cosIdAttr != null) {
                cosId = (String) cosIdAttr.get();
                cos = lookupCos(cosId, zlc);
                if (!cos.getId().equals(cosId)) {
                    cosId = cos.getId();
                }
                attrs.put(Provisioning.A_zimbraCOSId, cosId);
            } else {
                String domainCosId = domain != null ? d.getAttr(Provisioning.A_zimbraDomainDefaultCOSId, null) : null;
                if (domainCosId != null) cos = get(CosBy.id, domainCosId);
                if (cos == null) cos = getCosByName(Provisioning.DEFAULT_COS_NAME, zlc);
            }

            boolean hasMailTransport = (attrs.get(Provisioning.A_zimbraMailTransport) == null)?false:true;

            // if zimbraMailTransport is NOT provided, pick a server and add zimbraMailHost(and zimbraMailTransport) if it is not specified
            if (!hasMailTransport) {
                // if zimbraMailHost is not specified, and we have a COS, see if there is a pool to pick from.
                if (cos != null && attrs.get(Provisioning.A_zimbraMailHost) == null) {
                    String mailHostPool[] = cos.getMultiAttr(Provisioning.A_zimbraMailHostPool);
                    addMailHost(attrs, mailHostPool, cos.getName());
                }

                // if zimbraMailHost still not specified, default to local server's zimbraServiceHostname if it has
                // the mailbox service enabled, otherwise look through all servers and pick first with the service enabled.
                // this means every account will always have a mailbox
                if (attrs.get(Provisioning.A_zimbraMailHost) == null) {
                    addDefaultMailHost(attrs);
                }
            }

            // set all the mail-related attrs if zimbraMailHost or zimbraMailTransport was specified
            if (attrs.get(Provisioning.A_zimbraMailHost) != null || attrs.get(Provisioning.A_zimbraMailTransport) != null) {
                // default mail status is enabled
                if (attrs.get(Provisioning.A_zimbraMailStatus) == null)
                    attrs.put(A_zimbraMailStatus, MAIL_STATUS_ENABLED);

                // default account mail delivery address is email address
                if (attrs.get(Provisioning.A_zimbraMailDeliveryAddress) == null) {
                    attrs.put(A_zimbraMailDeliveryAddress, emailAddress);
                }
            } else
                throw ServiceException.INVALID_REQUEST("missing " + Provisioning.A_zimbraMailHost + " or " + Provisioning.A_zimbraMailTransport +  " for CreateAccount: " + emailAddress, null);

            // amivisAccount requires the mail attr, so we always add it
            attrs.put(A_mail, emailAddress);

            // required for organizationalPerson class
            if (attrs.get(Provisioning.A_cn) == null) {
                Attribute a = attrs.get(Provisioning.A_displayName);
                if (a != null) {
                    attrs.put(A_cn, a.get());
                } else {
                    attrs.put(A_cn, localPart);
                }
            }

            // required for organizationalPerson class
            if (attrs.get(Provisioning.A_sn) == null)
                attrs.put(A_sn, localPart);

            attrs.put(A_uid, localPart);

            setInitialPassword(cos, attrs, password);

            dn = mDIT.accountDNCreate(baseDn, attrs, localPart, domain);

            zlc.createEntry(dn, attrs, "createAccount");
            Account acct = getAccountById(zimbraIdStr, zlc, true);
            if (acct == null)
                throw ServiceException.FAILURE("unable to get account after creating LDAP account entry: "+emailAddress+", check ldap log for possible BDB deadlock", null);
            AttributeManager.getInstance().postModify(acctAttrs, acct, attrManagerContext, true);

            validate(ProvisioningValidator.CREATE_ACCOUNT_SUCCEEDED,
                    emailAddress, acct);
            return acct;
        } catch (NameAlreadyBoundException nabe) {
            throw AccountServiceException.ACCOUNT_EXISTS(emailAddress, dn, nabe);
        } catch (NamingException e) {
           throw ServiceException.FAILURE("unable to create account: "+emailAddress, e);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }

    private boolean addDefaultMailHost(Attributes attrs, Server server)  throws ServiceException {
        String localMailHost = server.getAttr(Provisioning.A_zimbraServiceHostname);
        boolean hasMailboxService = server.getMultiAttrSet(Provisioning.A_zimbraServiceEnabled).contains(Provisioning.SERVICE_MAILBOX);
        if (hasMailboxService && localMailHost != null) {
            attrs.put(Provisioning.A_zimbraMailHost, localMailHost);
            int lmtpPort = getLocalServer().getIntAttr(Provisioning.A_zimbraLmtpBindPort, com.zimbra.cs.util.Config.D_LMTP_BIND_PORT);
            String transport = "lmtp:" + localMailHost + ":" + lmtpPort;
            attrs.put(Provisioning.A_zimbraMailTransport, transport);
            return true;
        }
        return false;
    }

    private void addDefaultMailHost(Attributes attrs)  throws ServiceException {
        if (!addDefaultMailHost(attrs, getLocalServer())) {
            for (Server server: getAllServers()) {
                if (addDefaultMailHost(attrs, server)) {
                    return;
                }
            }
        }
    }

    private String addMailHost(Attributes attrs, String[] mailHostPool, String cosName) throws ServiceException {
        if (mailHostPool.length == 0) {
            return null;
        } else if (mailHostPool.length > 1) {
            // copy it, since we are dealing with a cached String[]
            String pool[] = new String[mailHostPool.length];
            System.arraycopy(mailHostPool, 0, pool, 0, mailHostPool.length);
            mailHostPool = pool;
        }

        // shuffule up and deal
        int max = mailHostPool.length;
        while (max > 0) {
            int i = sPoolRandom.nextInt(max);
            String mailHostId = mailHostPool[i];
            Server s = (mailHostId == null) ? null : getServerByIdInternal(mailHostId);
            if (s != null) {
                String mailHost = s.getAttr(Provisioning.A_zimbraServiceHostname);
                if (mailHost != null) {
                    boolean hasMailboxService = s.getMultiAttrSet(Provisioning.A_zimbraServiceEnabled).contains(Provisioning.SERVICE_MAILBOX);
                    if (hasMailboxService) {
                        attrs.put(Provisioning.A_zimbraMailHost, mailHost);
                        int lmtpPort = s.getIntAttr(Provisioning.A_zimbraLmtpBindPort, com.zimbra.cs.util.Config.D_LMTP_BIND_PORT);
                        String transport = "lmtp:" + mailHost + ":" + lmtpPort;
                        attrs.put(Provisioning.A_zimbraMailTransport, transport);
                        return mailHost;
                    } else
                        ZimbraLog.account.warn("cos("+cosName+") mailHostPool server("+s.getName()+") is not enabled for mailbox service");
                } else {
                    ZimbraLog.account.warn("cos("+cosName+") mailHostPool server("+s.getName()+") has no service hostname");
                }
            } else {
                ZimbraLog.account.warn("cos("+cosName+") has invalid server in pool: "+mailHostId);
            }
            if (i != max-1) {
                mailHostPool[i] = mailHostPool[max-1];
            }
            max--;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Account> getAllAdminAccounts() throws ServiceException {
        return (List<Account>) searchAccountsInternal(LdapFilter.adminAccountByAdminFlag(), null, null, true, Provisioning.SA_ACCOUNT_FLAG);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<NamedEntry> searchAccounts(String query, String returnAttrs[], final String sortAttr, final boolean sortAscending, int flags) throws ServiceException {
        return (List<NamedEntry>) searchAccountsInternal(query, returnAttrs, sortAttr, sortAscending, flags);
    }

    @Override
    public void searchAccountsOnServer(Server server, SearchOptions opts, NamedEntry.Visitor visitor) throws ServiceException {
        String base = getDIT().mailBranchBaseDN();

        // searchObjects put the caller's query before objectClass.
        // objectClass is indexed but zimbraMailHost is not.
        // put together the query here
        String query = "(&(objectclass=zimbraAccount)(" + Provisioning.A_zimbraMailHost + "=" + server.getName() + "))";

        int flags = opts.getFlags() | Provisioning.SO_NO_FIXUP_OBJECTCLASS;
        searchObjects(query, opts.getReturnAttrs(), base, flags, visitor, opts.getMaxResults(), true, opts.getOnMaster());
    }

    private List<?> searchAccountsInternal(String query, String returnAttrs[], final String sortAttr, final boolean sortAscending, int flags)
        throws ServiceException
    {
        //flags &= ~Provisioning.SA_DOMAIN_FLAG; // leaving on for now
        return searchObjects(query, returnAttrs, sortAttr, sortAscending, mDIT.mailBranchBaseDN(), flags, 0);
    }

    private static String getObjectClassQuery(int flags) {
        boolean accounts = (flags & Provisioning.SA_ACCOUNT_FLAG) != 0;
        boolean aliases = (flags & Provisioning.SA_ALIAS_FLAG) != 0;
        boolean lists = (flags & Provisioning.SA_DISTRIBUTION_LIST_FLAG) != 0;
        boolean calendarResources = (flags & Provisioning.SA_CALENDAR_RESOURCE_FLAG) != 0;
        boolean domains = (flags & Provisioning.SA_DOMAIN_FLAG) != 0;
        boolean coses = (flags & Provisioning.SD_COS_FLAG) != 0;

        int num = (accounts ? 1 : 0) +
                  (aliases ? 1 : 0) +
                  (lists ? 1 : 0) +
                  (domains ? 1 : 0) +
                  (coses ? 1 : 0) +
                  (calendarResources ? 1 : 0);
        if (num == 0)
            accounts = true;

        // If searching for user accounts/aliases/lists, filter looks like:
        //
        //   (&(objectclass=zimbraAccount)!(objectclass=zimbraCalendarResource))
        //
        // If searching for calendar resources, filter looks like:
        //
        //   (objectclass=zimbraCalendarResource)
        //
        // The !resource condition is there in first case because a calendar
        // resource is also a zimbraAccount.
        //
        StringBuffer oc = new StringBuffer();

        if (accounts && !calendarResources) oc.append("(&");

        if (num > 1) oc.append("(|");

        if (accounts) oc.append("(objectclass=zimbraAccount)");
        if (aliases) oc.append("(objectclass=zimbraAlias)");
        if (lists) oc.append("(objectclass=zimbraDistributionList)");
        if (domains) oc.append("(objectclass=zimbraDomain)");
        if (coses) oc.append("(objectclass=zimbraCos)");
        if (calendarResources) oc.append("(objectclass=zimbraCalendarResource)");

        if (num > 1) oc.append(")");

        if (accounts && !calendarResources)
            oc.append("(!(objectclass=zimbraCalendarResource)))");

        return oc.toString();
    }

    List<NamedEntry> searchObjects(String query, String returnAttrs[],
            final String sortAttr, final boolean sortAscending, String base,
            int flags, int maxResults) throws ServiceException {
        return searchObjects(query, returnAttrs, sortAttr, sortAscending,
                new String[] {base}, flags, maxResults, true, false);
    }

    private static class NamedEntryComparator implements Comparator<NamedEntry> {
        final Provisioning mProv;
        final String mSortAttr;
        final boolean mSortAscending;
        final boolean mByName;

        NamedEntryComparator(Provisioning prov, String sortAttr, boolean sortAscending) {
            mProv = prov;
            mSortAttr = sortAttr;
            mSortAscending = sortAscending;
            mByName = sortAttr == null || sortAttr.equals("name");
        }

        @Override
        public int compare(NamedEntry a, NamedEntry b) {
            int comp = 0;

            if (mByName)
                comp = a.getName().compareToIgnoreCase(b.getName());
            else {
                String sa = null;
                String sb = null;
                if (SearchOptions.SORT_BY_TARGET_NAME.equals(mSortAttr) && (a instanceof Alias) && (b instanceof Alias)) {
                    try {
                        sa = ((Alias)a).getTargetUnicodeName(mProv);
                    } catch (ServiceException e) {
                        ZimbraLog.account.error("unable to get target name: "+a.getName(), e);
                    }
                    try {
                        sb = ((Alias)b).getTargetUnicodeName(mProv);
                    } catch (ServiceException e) {
                        ZimbraLog.account.error("unable to get target name: "+b.getName(), e);
                    }

                } else {
                    sa = a.getAttr(mSortAttr);
                    sb = b.getAttr(mSortAttr);
                }
                if (sa == null) sa = "";
                if (sb == null) sb = "";
                comp = sa.compareToIgnoreCase(sb);
            }
            return mSortAscending ? comp : -comp;
        }
    };

    List<NamedEntry> searchObjects(String query, String returnAttrs[],
            final String sortAttr, final boolean sortAscending, String[] bases,
            int flags, int maxResults, boolean useConnPool, boolean useMaster)
    throws ServiceException {
        final List<NamedEntry> result = new ArrayList<NamedEntry>();

        NamedEntry.Visitor visitor = new NamedEntry.Visitor() {
            @Override
            public void visit(NamedEntry entry) {
                result.add(entry);
            }
        };

        if (bases == null || bases.length == 0)
            searchObjects(query, returnAttrs, "", flags, visitor, maxResults, useConnPool, useMaster);
        else {
            for (String base : bases)
                searchObjects(query, returnAttrs, base, flags, visitor, maxResults, useConnPool, useMaster);
        }

        NamedEntryComparator comparator = new NamedEntryComparator(Provisioning.getInstance(), sortAttr, sortAscending);
        Collections.sort(result, comparator);
        return result;
    }

    void searchObjects(String query, String returnAttrs[], String base, int flags, NamedEntry.Visitor visitor, int maxResults)
        throws ServiceException {
        searchObjects(query, returnAttrs, base, flags, visitor, maxResults, true, false);
    }

    public void searchObjects(String query, String returnAttrs[], String base, int flags,
            NamedEntry.Visitor visitor, int maxResults,
            boolean useConnPool, boolean useMaster) throws ServiceException {

        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext(useMaster, useConnPool);

            if ((flags & Provisioning.SO_NO_FIXUP_OBJECTCLASS) == 0) {
                String objectClass = getObjectClassQuery(flags);

                if (query == null || query.equals("")) {
                    query = objectClass;
                } else {
                    if (query.startsWith("(") && query.endsWith(")")) {
                        query = "(&"+query+objectClass+")";
                    } else {
                        query = "(&("+query+")"+objectClass+")";
                    }
                }
            }

            if ((flags & Provisioning.SO_NO_FIXUP_RETURNATTRS) == 0)
                returnAttrs = fixReturnAttrs(returnAttrs, flags);

            SearchControls searchControls =
                new SearchControls(SearchControls.SUBTREE_SCOPE, maxResults, 0, returnAttrs, false, false);

            //Set the page size and initialize the cookie that we pass back in subsequent pages
            int pageSize = LdapUtil.adjustPageSize(maxResults, 1000);
            byte[] cookie = null;

            // we don't want to ever cache any of these, since they might not have all their attributes

            NamingEnumeration<SearchResult> ne = null;

            int total = 0;
            String configBranchBaseDn = mDIT.configBranchBaseDN();
            try {
                do {
                    zlc.setPagedControl(pageSize, cookie, true);

                    ne = zlc.searchDir(base, query, searchControls);
                    while (ne != null && ne.hasMore()) {
                        if (maxResults > 0 && total++ > maxResults)
                        throw new SizeLimitExceededException("exceeded limit of "+maxResults);
                        SearchResult sr = ne.nextElement();
                        String dn = sr.getNameInNamespace();

                        Attributes attrs = sr.getAttributes();
                        Attribute objectclass = attrs.get("objectclass");

                        // skip admin accounts
                        // if we are looking for domains or coses, they can be under config branch in non default DIT impl.
                        if (dn.endsWith(configBranchBaseDn) && !objectclass.contains(C_zimbraDomain) && !objectclass.contains(C_zimbraCOS))
                            continue;

                        if (objectclass == null || objectclass.contains(C_zimbraAccount))
                            visitor.visit(makeAccount(dn, attrs, flags));
                        else if (objectclass.contains(C_zimbraAlias))
                            visitor.visit(makeAlias(dn, attrs, this));
                        else if (objectclass.contains(C_zimbraMailList))
                            visitor.visit(makeDistributionList(dn, attrs));
                        else if (objectclass.contains(C_zimbraDomain))
                            visitor.visit(new LdapDomain(dn, attrs, getConfig().getDomainDefaults(), this));
                        else if (objectclass.contains(C_zimbraCOS))
                            visitor.visit(new LdapCos(dn, attrs, this));
                    }
                    cookie = zlc.getCookie();
                } while (cookie != null);
            } finally {
                if (ne != null) ne.close();
            }
        } catch (InvalidSearchFilterException e) {
            throw ServiceException.INVALID_REQUEST("invalid search filter "+e.getMessage(), e);
        } catch (NameNotFoundException e) {
            // happens when base doesn't exist
            ZimbraLog.account.warn("unable to list all objects", e);
        } catch (SizeLimitExceededException e) {
            throw AccountServiceException.TOO_MANY_SEARCH_RESULTS("too many search results returned", e);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to list all objects", e);
        } catch (IOException e) {
            throw ServiceException.FAILURE("unable to list all objects", e);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }

    /**
     * add "uid" to list of return attrs if not specified, since we need it to construct an Account
     * @param returnAttrs
     * @return
     */
    private String[] fixReturnAttrs(String[] returnAttrs, int flags) {
        if (returnAttrs == null || returnAttrs.length == 0)
            return null;

        boolean needUID = true;
        boolean needID = true;
        boolean needCOSId = true;
        boolean needObjectClass = true;
        boolean needAliasTargetId = (flags & Provisioning.SA_ALIAS_FLAG) != 0;
        boolean needCalendarUserType = (flags & Provisioning.SA_CALENDAR_RESOURCE_FLAG) != 0;
        boolean needDomainName = true;
        boolean needZimbraACE = true;
        boolean needCn = (flags & Provisioning.SD_COS_FLAG) != 0;


        for (int i=0; i < returnAttrs.length; i++) {
            if (Provisioning.A_uid.equalsIgnoreCase(returnAttrs[i]))
                needUID = false;
            else if (Provisioning.A_zimbraId.equalsIgnoreCase(returnAttrs[i]))
                needID = false;
            else if (Provisioning.A_zimbraCOSId.equalsIgnoreCase(returnAttrs[i]))
                needCOSId = false;
            else if (Provisioning.A_zimbraAliasTargetId.equalsIgnoreCase(returnAttrs[i]))
                needAliasTargetId = false;
            else if (Provisioning.A_objectClass.equalsIgnoreCase(returnAttrs[i]))
                needObjectClass = false;
            else if (Provisioning.A_zimbraAccountCalendarUserType.equalsIgnoreCase(returnAttrs[i]))
                needCalendarUserType = false;
            else if (Provisioning.A_zimbraDomainName.equalsIgnoreCase(returnAttrs[i]))
                needDomainName = false;
            else if (Provisioning.A_zimbraACE.equalsIgnoreCase(returnAttrs[i]))
                needZimbraACE = false;
            else if (Provisioning.A_cn.equalsIgnoreCase(returnAttrs[i]))
                needCn = false;
        }

        int num = (needUID ? 1 : 0) +
                  (needID ? 1 : 0) +
                  (needCOSId ? 1 : 0) +
                  (needAliasTargetId ? 1 : 0) +
                  (needObjectClass ? 1 :0) +
                  (needCalendarUserType ? 1 : 0) +
                  (needDomainName ? 1 : 0) +
                  (needZimbraACE ? 1 : 0) +
                  (needCn ? 1 : 0);

        if (num == 0) return returnAttrs;

        String[] result = new String[returnAttrs.length+num];
        int i = 0;
        if (needUID) result[i++] = Provisioning.A_uid;
        if (needID) result[i++] = Provisioning.A_zimbraId;
        if (needCOSId) result[i++] = Provisioning.A_zimbraCOSId;
        if (needAliasTargetId) result[i++] = Provisioning.A_zimbraAliasTargetId;
        if (needObjectClass) result[i++] = Provisioning.A_objectClass;
        if (needCalendarUserType) result[i++] = Provisioning.A_zimbraAccountCalendarUserType;
        if (needDomainName) result[i++] = Provisioning.A_zimbraDomainName;
        if (needZimbraACE) result[i++] = Provisioning.A_zimbraACE;
        if (needCn) result[i++] = Provisioning.A_cn;
        System.arraycopy(returnAttrs, 0, result, i, returnAttrs.length);
        return result;
    }

    @Override
    public void setCOS(Account acct, Cos cos) throws ServiceException {
        HashMap<String, String> attrs = new HashMap<String, String>();
        attrs.put(Provisioning.A_zimbraCOSId, cos.getId());
        modifyAttrs(acct, attrs);
    }

    @Override
    public void modifyAccountStatus(Account acct, String newStatus) throws ServiceException {
        HashMap<String, String> attrs = new HashMap<String, String>();
        attrs.put(Provisioning.A_zimbraAccountStatus, newStatus);
        modifyAttrs(acct, attrs);
    }


    static String[] addMultiValue(String values[], String value) {
        List<String> list = new ArrayList<String>(Arrays.asList(values));
        list.add(value);
        return list.toArray(new String[list.size()]);
    }

    String[] addMultiValue(NamedEntry acct, String attr, String value) {
        return addMultiValue(acct.getMultiAttr(attr), value);
    }

    String[] removeMultiValue(NamedEntry acct, String attr, String value) {
        return LdapUtil.removeMultiValue(acct.getMultiAttr(attr), value);
    }

    @Override
    public void addAlias(Account acct, String alias) throws ServiceException {
        addAliasInternal(acct, alias);
    }

    @Override
    public void removeAlias(Account acct, String alias) throws ServiceException {
        removeAliasInternal(acct, alias);
    }

    @Override
    public void addAlias(DistributionList dl, String alias) throws ServiceException {
        addAliasInternal(dl, alias);
    }

    @Override
    public void removeAlias(DistributionList dl, String alias) throws ServiceException {
        removeAliasInternal(dl, alias);
    }

    private boolean isEntryAlias(Attributes attrs) throws NamingException {

        Map<String, Object> entryAttrs = LdapUtil.getAttrs(attrs);
        Object ocs = entryAttrs.get(Provisioning.A_objectClass);
        if (ocs instanceof String)
            return ((String)ocs).equalsIgnoreCase(C_zimbraAlias);
        else if (ocs instanceof String[]) {
            for (String oc : (String[])ocs) {
                if (oc.equalsIgnoreCase(C_zimbraAlias))
                    return true;
            }
        }
        return false;
    }

    private void addAliasInternal(NamedEntry entry, String alias) throws ServiceException {

        String targetDomainName = null;
        if (entry instanceof Account)
            targetDomainName = ((Account)entry).getDomainName();
        else if (entry instanceof DistributionList)
            targetDomainName = ((DistributionList)entry).getDomainName();
        else
            assert(false);

        alias = alias.toLowerCase().trim();
        alias = IDNUtil.toAsciiEmail(alias);

        validEmailAddress(alias);

        String parts[] = alias.split("@");
        String aliasName = parts[0];
        String aliasDomain = parts[1];

        ZimbraLdapContext zlc = null;
        String aliasDn = null;
        try {
            zlc = new ZimbraLdapContext(true);

            Domain domain = getDomainByAsciiName(aliasDomain, zlc);
            if (domain == null)
                throw AccountServiceException.NO_SUCH_DOMAIN(aliasDomain);

            aliasDn = mDIT.aliasDN(((LdapEntry)entry).getDN(), targetDomainName, aliasName, aliasDomain);
            // the create and addAttr ideally would be in the same transaction

            String aliasUuid = LdapUtil.generateUUID();
            String targetEntryId = entry.getId();
            try {
                zlc.simpleCreate(aliasDn, "zimbraAlias",
                    new String[] { Provisioning.A_uid, aliasName,
                                   Provisioning.A_zimbraId, aliasUuid,
                                   Provisioning.A_zimbraCreateTimestamp, DateUtil.toGeneralizedTime(new Date()),
                                   Provisioning.A_zimbraAliasTargetId, targetEntryId} );
            } catch (NameAlreadyBoundException e) {
                /*
                 * check if the alias is a dangling alias.  If so remove the dangling alias
                 * and create a new one.
                 */
                Attributes attrs = zlc.getAttributes(aliasDn);

                // see if the entry is an alias
                if (!isEntryAlias(attrs))
                    throw e;

                Alias aliasEntry = makeAlias(aliasDn, attrs, this);
                NamedEntry targetEntry = searchAliasTarget(aliasEntry, false);
                if (targetEntry == null) {
                    // remove the dangling alias
                    try {
                        removeAliasInternal(null, alias);
                    } catch (ServiceException se) {
                        // ignore
                    }

                    // try creating the alias again
                    zlc.simpleCreate(aliasDn, "zimbraAlias",
                            new String[] { Provisioning.A_uid, aliasName,
                                           Provisioning.A_zimbraId, aliasUuid,
                                           Provisioning.A_zimbraCreateTimestamp, DateUtil.toGeneralizedTime(new Date()),
                                           Provisioning.A_zimbraAliasTargetId, targetEntryId} );
                } else if (targetEntryId.equals(targetEntry.getId())) {
                    // the alias target points this account/DL
                    Set<String> mailAliases = entry.getMultiAttrSet(Provisioning.A_zimbraMailAlias);
                    Set<String> mails = entry.getMultiAttrSet(Provisioning.A_mail);
                    if (mailAliases != null && mailAliases.contains(alias) &&
                        mails != null && mails.contains(alias))
                        throw e;
                    else
                        ZimbraLog.account.warn("alias entry exists at " + aliasDn +
                                               ", but either mail or zimbraMailAlias of the target does not contain " + alias +
                                               ", adding " + alias + " to entry " + entry.getName());
                } else {
                    // not dangling, neither is the target the same entry as the account/DL
                    // for which the alias is being added for, rethrow the naming exception
                    throw e;
                }
            }

            HashMap<String, String> attrs = new HashMap<String, String>();
            attrs.put("+" + Provisioning.A_zimbraMailAlias, alias);
            attrs.put("+" + Provisioning.A_mail, alias);

            // UGH
            modifyAttrsInternal(entry, zlc, attrs);
        } catch (NameAlreadyBoundException nabe) {
            throw AccountServiceException.ACCOUNT_EXISTS(alias, aliasDn, nabe);
        } catch (InvalidNameException e) {
            throw ServiceException.INVALID_REQUEST("invalid alias name: "+e.getMessage(), e);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to create alias: "+e.getMessage(), e);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }


    /*
     * 1. remove alias from mail and zimbraMailAlias attributes of the entry
     * 2. remove alias from all distribution lists
     * 3. delete the alias entry
     *
     * A. entry exists, alias exists
     *    - if alias points to the entry:            do 1, 2, 3
     *    - if alias points to other existing entry: do 1, and then throw NO_SUCH_ALIAS
     *    - if alias points to a non-existing entry: do 1, 2, 3, and then throw NO_SUCH_ALIAS
     *
     * B. entry exists, alias does not exist:  do 1, 2, and then throw NO_SUCH_ALIAS
     *
     * C. entry does not exist, alias exists:
     *    - if alias points to other existing entry: do nothing (and then throw NO_SUCH_ACCOUNT/NO_SUCH_DISTRIBUTION_LIST in ProvUtil)
     *    - if alias points to a non-existing entry: do 2, 3 (and then throw NO_SUCH_ACCOUNT/NO_SUCH_DISTRIBUTION_LIST in ProvUtil)
     *
     * D. entry does not exist, alias does not exist:  do 2 (and then throw NO_SUCH_ACCOUNT/NO_SUCH_DISTRIBUTION_LIST in ProvUtil)
     *
     *
     */
    private void removeAliasInternal(NamedEntry entry, String alias) throws ServiceException {

        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext(true);

            alias = alias.toLowerCase();
            alias = IDNUtil.toAsciiEmail(alias);

            String parts[] = alias.split("@");
            String aliasName = parts[0];
            String aliasDomain = parts[1];

            Domain domain = getDomainByAsciiName(aliasDomain, zlc);
            if (domain == null)
                throw AccountServiceException.NO_SUCH_DOMAIN(aliasDomain);

            String targetDn = (entry == null)?null:((LdapEntry)entry).getDN();
            String targetDomainName = null;
            if (entry != null) {
                if (entry instanceof Account)
                    targetDomainName = ((Account)entry).getDomainName();
                else if (entry instanceof DistributionList)
                    targetDomainName = ((DistributionList)entry).getDomainName();
                else
                    throw ServiceException.INVALID_REQUEST("invalid entry type for alias", null);
            }
            String aliasDn = mDIT.aliasDN(targetDn, targetDomainName, aliasName, aliasDomain);

            Attributes aliasAttrs = null;
            Alias aliasEntry = null;
            try {
                aliasAttrs = zlc.getAttributes(aliasDn);

                // see if the entry is an alias
                if (!isEntryAlias(aliasAttrs))
                    throw AccountServiceException.NO_SUCH_ALIAS(alias);

                aliasEntry = makeAlias(aliasDn, aliasAttrs, this);
            } catch (NamingException e) {
                ZimbraLog.account.warn("alias " + alias + " does not exist");
            }

            NamedEntry targetEntry = null;
            if (aliasEntry != null)
                targetEntry = searchAliasTarget(aliasEntry, false);

            boolean aliasPointsToEntry = ((entry != null) && (aliasEntry != null) &&
                                          entry.getId().equals(aliasEntry.getAttr(Provisioning.A_zimbraAliasTargetId)));

            boolean aliasPointsToOtherExistingEntry = ((aliasEntry != null) && (targetEntry != null) &&
                                                        ((entry == null) || (!entry.getId().equals(targetEntry.getId()))));

            boolean aliasPointsToNonExistingEntry = ((aliasEntry != null) && (targetEntry == null));

            // 1. remove alias from mail/zimbraMailAlias attrs
            if (entry != null) {
                try {
                    HashMap<String, String> attrs = new HashMap<String, String>();
                    attrs.put("-" + Provisioning.A_mail, alias);
                    attrs.put("-" + Provisioning.A_zimbraMailAlias, alias);
                    modifyAttrsInternal(entry, zlc, attrs);
                } catch (ServiceException e) {
                    ZimbraLog.account.warn("unable to remove zimbraMailAlias/mail attrs: "+alias);
                }
            }

            // 2. remove address from all DLs
            if (!aliasPointsToOtherExistingEntry)
                removeAddressFromAllDistributionLists(alias);

            // 3. remove the alias entry
            if (aliasPointsToEntry || aliasPointsToNonExistingEntry) {
                try {
                    zlc.unbindEntry(aliasDn);
                } catch (NamingException e) {
                    // should not happen, log it
                    ZimbraLog.account.warn("unable to remove alias entry at : " + aliasDn);
                }
            }

            // throw NO_SUCH_ALIAS if necessary
            if (((entry != null) && (aliasEntry == null)) ||
                ((entry != null) && (aliasEntry != null) && !aliasPointsToEntry))
                throw AccountServiceException.NO_SUCH_ALIAS(alias);

        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }


    }

    /**
     * search alias target - implementation can return cached entry
     *
     * @param alias
     * @param mustFind
     * @return
     * @throws ServiceException
     */
    @Override
    public NamedEntry getAliasTarget(Alias alias, boolean mustFind) throws ServiceException {

        String targetId = alias.getAttr(Provisioning.A_zimbraAliasTargetId);
        NamedEntry target;

        // maybe it's an account/cr
        target = get(AccountBy.id, targetId);
        if (target != null)
            return target;


        // maybe it's a group
        // (note, entries in this DL cache contains only minimal attrs)
        target = getGroup(DistributionListBy.id, targetId);

        return target;
    }

    @Override
    public Domain createDomain(String name, Map<String, Object> domainAttrs) throws ServiceException {
        name = name.toLowerCase().trim();
        name = IDNUtil.toAsciiDomainName(name);

        NameUtil.validNewDomainName(name);

        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext(true);

            LdapDomain d = (LdapDomain) getDomainByAsciiName(name, zlc);
            if (d != null)
                throw AccountServiceException.DOMAIN_EXISTS(name);

            Map<?, ?> attrManagerContext = new HashMap<Object, Object>();

            // Attribute checking can not express "allow setting on
            // creation, but do not allow modifies afterwards"
            String domainType = (String) domainAttrs.get(A_zimbraDomainType);
            if (domainType == null) {
                domainType = DOMAIN_TYPE_LOCAL;
            } else {
                domainAttrs.remove(A_zimbraDomainType); // add back later
            }

            String domainStatus = (String) domainAttrs.get(A_zimbraDomainStatus);
            if (domainStatus == null) {
                domainStatus = DOMAIN_STATUS_ACTIVE;
            } else {
                domainAttrs.remove(A_zimbraDomainStatus); // add back later
            }

            AttributeManager.getInstance().preModify(domainAttrs, null, attrManagerContext, true, true);

            // Add back attrs we circumvented from attribute checking
            domainAttrs.put(A_zimbraDomainType, domainType);
            domainAttrs.put(A_zimbraDomainStatus, domainStatus);

            String parts[] = name.split("\\.");
            String dns[] = mDIT.domainToDNs(parts);
            createParentDomains(zlc, parts, dns);

            Attributes attrs = new BasicAttributes(true);
            LdapUtil.mapToAttrs(domainAttrs, attrs);

            Set<String> ocs = LdapObjectClass.getDomainObjectClasses(this);
            LdapUtil.addAttr(attrs, A_objectClass, ocs);

            String zimbraIdStr = LdapUtil.generateUUID();
            attrs.put(A_zimbraId, zimbraIdStr);
            attrs.put(A_zimbraCreateTimestamp, DateUtil.toGeneralizedTime(new Date()));
            attrs.put(A_zimbraDomainName, name);

            String mailStatus = (String) domainAttrs.get(A_zimbraMailStatus);
            if (mailStatus == null)
                attrs.put(A_zimbraMailStatus, MAIL_STATUS_ENABLED);

            if (domainType.equalsIgnoreCase(DOMAIN_TYPE_ALIAS)) {
                attrs.put(A_zimbraMailCatchAllAddress, "@" + name);
            }

            attrs.put(A_o, name+" domain");
            attrs.put(A_dc, parts[0]);

            String dn = dns[0];
            //NOTE: all four of these should be in a transaction...
            try {
                zlc.createEntry(dn, attrs, "createDomain");
            } catch (NameAlreadyBoundException e) {
                zlc.replaceAttributes(dn, attrs);
            }

            String acctBaseDn = mDIT.domainDNToAccountBaseDN(dn);
            if (!acctBaseDn.equals(dn)) {
                /*
                 * create the account base dn entry only if if is not the same as the domain dn
                 *
                 * TODO, the objectclass(organizationalRole) and attrs(ou and cn) for the account
                 * base dn entry is still hardcoded,  it should be parameterized in LdapDIT
                 * according the BASE_RDN_ACCOUNT.  This is actually a design decision depending
                 * on how far we want to allow the DIT to be customized.
                 */
                zlc.simpleCreate(mDIT.domainDNToAccountBaseDN(dn),
                                 "organizationalRole",
                                 new String[] { A_ou, "people", A_cn, "people"});
            }

            Domain domain = getDomainById(zimbraIdStr, zlc);

            AttributeManager.getInstance().postModify(domainAttrs, domain, attrManagerContext, true);
            return domain;

        } catch (NameAlreadyBoundException nabe) {
            throw AccountServiceException.DOMAIN_EXISTS(name);
        } catch (NamingException e) {
            //if (e instanceof )
            throw ServiceException.FAILURE("unable to create domain: "+name, e);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }

    private LdapDomain getDomainByQuery(String query, ZimbraLdapContext initZlc) throws ServiceException {
        ZimbraLdapContext zlc = initZlc;
        try {
            if (zlc == null)
                zlc = new ZimbraLdapContext();
            NamingEnumeration<SearchResult> ne = zlc.searchDir(mDIT.domainBaseDN(), query, sSubtreeSC);
            if (ne.hasMore()) {
                SearchResult sr = ne.next();
                if (ne.hasMore()) {
                    String dups = LdapUtil.formatMultipleMatchedEntries(sr, ne);
                    throw AccountServiceException.MULTIPLE_DOMAINS_MATCHED("getDomainByQuery: "+query+" returned multiple entries at "+dups);
                }
                ne.close();
                return new LdapDomain(sr.getNameInNamespace(), sr.getAttributes(), getConfig().getDomainDefaults(), this);
            }
        } catch (NameNotFoundException e) {
            return null;
        } catch (InvalidNameException e) {
            return null;
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to lookup domain via query: "+query+" message:"+e.getMessage(), e);
        } finally {
            if (initZlc == null)
                ZimbraLdapContext.closeContext(zlc);
        }
        return null;
    }

    @Override
    public Domain get(DomainBy keyType, String key) throws ServiceException {
        return getDomain(keyType, key, false);
    }

    @Override
    public Domain getDomain(DomainBy keyType, String key, boolean checkNegativeCache) throws ServiceException {

        GetFromDomainCacheOption option = checkNegativeCache ? GetFromDomainCacheOption.BOTH : GetFromDomainCacheOption.POSITIVE;

        switch(keyType) {
            case name:
                return getDomainByNameInternal(key, option);
            case id:
                return getDomainByIdInternal(key, null, option);
            case virtualHostname:
                return getDomainByVirtualHostnameInternal(key, option);
            case krb5Realm:
                return getDomainByKrb5RealmInternal(key, option);
            default:
                return null;
        }
    }

    private Domain getFromCache(DomainBy keyType, String key, GetFromDomainCacheOption option) {
        switch(keyType) {
            case name:
                String asciiName = IDNUtil.toAsciiDomainName(key);
                return sDomainCache.getByName(asciiName, option);
            case id:
                return sDomainCache.getById(key, option);
            case virtualHostname:
                return sDomainCache.getByVirtualHostname(key, option);
            case krb5Realm:
                return sDomainCache.getByKrb5Realm(key, option);
            default:
                return null;
        }
    }

    private Domain getDomainById(String zimbraId, ZimbraLdapContext zlc) throws ServiceException {
        return getDomainByIdInternal(zimbraId, zlc, GetFromDomainCacheOption.POSITIVE);
    }

    private Domain getDomainByIdInternal(String zimbraId, ZimbraLdapContext zlc, GetFromDomainCacheOption option) throws ServiceException {
        if (zimbraId == null)
            return null;

        Domain d = sDomainCache.getById(zimbraId, option);
        if (d instanceof DomainCache.NonExistingDomain)
            return null;

        LdapDomain domain = (LdapDomain)d;
        if (domain == null) {
            zimbraId = LdapUtil.escapeSearchFilterArg(zimbraId);
            domain = getDomainByQuery(LdapFilter.domainById(zimbraId), zlc);
            sDomainCache.put(DomainBy.id, zimbraId, domain);
        }
        return domain;
    }

    private Domain getDomainByNameInternal(String name, GetFromDomainCacheOption option) throws ServiceException {
        String asciiName = IDNUtil.toAsciiDomainName(name);
        return getDomainByAsciiNameInternal(asciiName, null, option);
    }

    private Domain getDomainByAsciiName(String name, ZimbraLdapContext zlc) throws ServiceException {
        return getDomainByAsciiNameInternal(name, zlc, GetFromDomainCacheOption.POSITIVE);
    }

    private Domain getDomainByAsciiNameInternal(String name, ZimbraLdapContext zlc, GetFromDomainCacheOption option) throws ServiceException {
        Domain d = sDomainCache.getByName(name, option);
        if (d instanceof DomainCache.NonExistingDomain)
            return null;

        LdapDomain domain = (LdapDomain)d;
        if (domain == null) {
            name = LdapUtil.escapeSearchFilterArg(name);
            domain = getDomainByQuery(LdapFilter.domainByName(name), zlc);
            sDomainCache.put(DomainBy.name, name, domain);
        }
        return domain;
    }

    private Domain getDomainByVirtualHostnameInternal(String virtualHostname, GetFromDomainCacheOption option) throws ServiceException {
        Domain d = sDomainCache.getByVirtualHostname(virtualHostname, option);
        if (d instanceof DomainCache.NonExistingDomain)
            return null;

        LdapDomain domain = (LdapDomain)d;
        if (domain == null) {
            virtualHostname = LdapUtil.escapeSearchFilterArg(virtualHostname);
            domain = getDomainByQuery(LdapFilter.domainByVirtualHostame(virtualHostname), null);
            sDomainCache.put(DomainBy.virtualHostname, virtualHostname, domain);
        }
        return domain;
    }

    private Domain getDomainByKrb5RealmInternal(String krb5Realm, GetFromDomainCacheOption option) throws ServiceException {
        Domain d = sDomainCache.getByKrb5Realm(krb5Realm, option);
        if (d instanceof DomainCache.NonExistingDomain)
            return null;

        LdapDomain domain = (LdapDomain)d;
        if (domain == null) {
            krb5Realm = LdapUtil.escapeSearchFilterArg(krb5Realm);
            domain = getDomainByQuery(LdapFilter.domainByKrb5Realm(krb5Realm), null);
            sDomainCache.put(DomainBy.krb5Realm, krb5Realm, domain);
        }
        return domain;
    }

    @Override
    public List<Domain> getAllDomains() throws ServiceException {
        final List<Domain> result = new ArrayList<Domain>();

        NamedEntry.Visitor visitor = new NamedEntry.Visitor() {
            @Override
            public void visit(NamedEntry entry) {
                result.add((LdapDomain)entry);
            }
        };

        getAllDomains(visitor, null);
        Collections.sort(result);
        return result;
    }

    @Override
    public void getAllDomains(NamedEntry.Visitor visitor, String[] retAttrs) throws ServiceException {

        int flags = Provisioning.SA_DOMAIN_FLAG;

        // if asking for specific attrs only, make sure we have the minimum attrs required
        // for the search and to construct a LdapDomain object
        if (retAttrs != null) {
            Set<String> attrs = new HashSet<String>(Arrays.asList(retAttrs));
            attrs.add(Provisioning.A_objectClass);
            attrs.add(Provisioning.A_zimbraId);
            attrs.add(Provisioning.A_zimbraDomainName);
            retAttrs = attrs.toArray(new String[attrs.size()]);

            flags |= Provisioning.SO_NO_FIXUP_RETURNATTRS;
        }

        searchObjects(null,
                      retAttrs,
                      mDIT.domainBaseDN(),
                      flags,
                      visitor,
                      0);
    }

    private static boolean domainDnExists(ZimbraLdapContext zlc, String dn) throws NamingException {
        try {
            NamingEnumeration<SearchResult> ne = zlc.searchDir(dn,
                    LdapFilter.domainLabel(), sObjectSC);
            boolean result = ne.hasMore();
            ne.close();
            return result;
        } catch (InvalidNameException e) {
            return false;
        } catch (NameNotFoundException nnfe) {
            return false;
        }
    }

    private static void createParentDomains(ZimbraLdapContext zlc, String parts[], String dns[]) throws NamingException {
        for (int i=dns.length-1; i > 0; i--) {
            if (!domainDnExists(zlc, dns[i])) {
                String dn = dns[i];
                String domain = parts[i];
                // don't create ZimbraDomain objects, since we don't want them to show up in list domains
                zlc.simpleCreate(dn, new String[] {"dcObject", "organization"},
                        new String[] { A_o, domain+" domain", A_dc, domain });
            }
        }
    }

    @Override
    public Cos createCos(String name, Map<String, Object> cosAttrs) throws ServiceException {
        String defaultCosId =  getCosByName(DEFAULT_COS_NAME, null).getId();
        return copyCos(defaultCosId, name, cosAttrs);
    }

    @Override
    public Cos copyCos(String srcCosId, String destCosName) throws ServiceException {
        return copyCos(srcCosId, destCosName, null);
    }

    private Cos copyCos(String srcCosId, String destCosName, Map<String, Object> cosAttrs) throws ServiceException {
        destCosName = destCosName.toLowerCase().trim();

        Map<String, Object> allAttrs = new HashMap<String, Object>();
        Cos srcCos = getCosById(srcCosId, null);
        if (srcCos == null)
            throw AccountServiceException.NO_SUCH_COS(srcCosId);

        for (Map.Entry<String, Object> e : srcCos.getAttrs().entrySet()) {
            allAttrs.put(e.getKey(), e.getValue());
        }
        allAttrs.remove(Provisioning.A_objectClass);
        allAttrs.remove(Provisioning.A_zimbraId);
        allAttrs.remove(Provisioning.A_zimbraCreateTimestamp);
        allAttrs.remove(Provisioning.A_zimbraACE);
        allAttrs.remove(Provisioning.A_cn);
        allAttrs.remove(Provisioning.A_description);
        if (cosAttrs != null) {
            for (Map.Entry<String, Object> e : cosAttrs.entrySet()) {
                allAttrs.put(e.getKey(), e.getValue());
            }
        }

        Map<?, ?> attrManagerContext = new HashMap<Object, Object>();
        AttributeManager.getInstance().preModify(allAttrs, null, attrManagerContext, true, true);

        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext(true);

            Attributes attrs = new BasicAttributes(true);
            LdapUtil.mapToAttrs(allAttrs, attrs);

            Set<String> ocs = LdapObjectClass.getCosObjectClasses(this);
            LdapUtil.addAttr(attrs, A_objectClass, ocs);

            String zimbraIdStr = LdapUtil.generateUUID();
            attrs.put(A_zimbraId, zimbraIdStr);
            attrs.put(A_zimbraCreateTimestamp, DateUtil.toGeneralizedTime(new Date()));
            attrs.put(A_cn, destCosName);
            String dn = mDIT.cosNametoDN(destCosName);
            zlc.createEntry(dn, attrs, "createCos");

            Cos cos = getCosById(zimbraIdStr, zlc);
            AttributeManager.getInstance().postModify(allAttrs, cos, attrManagerContext, true);
            return cos;
        } catch (NameAlreadyBoundException nabe) {
            throw AccountServiceException.COS_EXISTS(destCosName);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }

    @Override
    public void renameCos(String zimbraId, String newName) throws ServiceException {
        LdapCos cos = (LdapCos) get(CosBy.id, zimbraId);
        if (cos == null)
            throw AccountServiceException.NO_SUCH_COS(zimbraId);

        if (cos.isDefaultCos())
            throw ServiceException.INVALID_REQUEST("unable to rename default cos", null);

        newName = newName.toLowerCase().trim();
        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext(true);
            String newDn = mDIT.cosNametoDN(newName);
            zlc.renameEntry(cos.getDN(), newDn);
            // remove old cos from cache
            sCosCache.remove(cos);
        } catch (NameAlreadyBoundException nabe) {
            throw AccountServiceException.COS_EXISTS(newName);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to rename cos: "+zimbraId, e);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }

    private LdapCos getCOSByQuery(String query, ZimbraLdapContext initZlc) throws ServiceException {
        ZimbraLdapContext zlc = initZlc;
        try {
            if (zlc == null)
                zlc = new ZimbraLdapContext();
            NamingEnumeration<SearchResult> ne = zlc.searchDir(mDIT.cosBaseDN(), query, sSubtreeSC);
            if (ne.hasMore()) {
                SearchResult sr = ne.next();
                ne.close();
                return new LdapCos(sr.getNameInNamespace(), sr.getAttributes(), this);
            }
        } catch (NameNotFoundException e) {
            return null;
        } catch (InvalidNameException e) {
            return null;
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to lookup cos via query: "+query+ " message: "+e.getMessage(), e);
        } finally {
            if (initZlc == null)
                ZimbraLdapContext.closeContext(zlc);
        }
        return null;
    }

    private Cos getCosById(String zimbraId, ZimbraLdapContext zlc) throws ServiceException {
        if (zimbraId == null)
            return null;

        LdapCos cos = sCosCache.getById(zimbraId);
        if (cos == null) {
            zimbraId = LdapUtil.escapeSearchFilterArg(zimbraId);
            cos = getCOSByQuery(LdapFilter.cosById(zimbraId), zlc);
            sCosCache.put(cos);
        }
        return cos;
    }

    @Override
    public Cos get(CosBy keyType, String key) throws ServiceException {
        switch(keyType) {
            case name:
                return getCosByName(key, null);
            case id:
                return getCosById(key, null);
            default:
                    return null;
        }
    }

    private Cos getFromCache(CosBy keyType, String key) {
        switch(keyType) {
            case name:
                return sCosCache.getByName(key);
            case id:
                return sCosCache.getById(key);
            default:
                return null;
        }
    }

    private Cos getCosByName(String name, ZimbraLdapContext initZlc) throws ServiceException {
        ZimbraLdapContext zlc = initZlc;
        LdapCos cos = sCosCache.getByName(name);
        if (cos != null)
            return cos;

        try {
            if (zlc == null)
                zlc = new ZimbraLdapContext();
            String dn = mDIT.cosNametoDN(name);
            Attributes attrs = zlc.getAttributes(dn);
            cos  = new LdapCos(dn, attrs, this);
            sCosCache.put(cos);
            return cos;
        } catch (NameNotFoundException e) {
            return null;
        } catch (InvalidNameException e) {
            return null;
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to lookup COS by name: "+name+" message: "+e.getMessage(), e);
        } finally {
            if (initZlc == null)
                ZimbraLdapContext.closeContext(zlc);
        }
    }

    @Override
    public List<Cos> getAllCos() throws ServiceException {
        List<Cos> result = new ArrayList<Cos>();
        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext();
            NamingEnumeration<SearchResult> ne = zlc.searchDir(mDIT.cosBaseDN(),
                    LdapFilter.allCoses(), sSubtreeSC);
            while (ne.hasMore()) {
                SearchResult sr = ne.next();
                result.add(new LdapCos(sr.getNameInNamespace(), sr.getAttributes(), this));
            }
            ne.close();
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to list all COS", e);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }

        Collections.sort(result);
        return result;
    }

    @Override
    public void deleteAccount(String zimbraId) throws ServiceException {
        Account acc = getAccountById(zimbraId);
        LdapEntry entry = (LdapEntry) getAccountById(zimbraId);
        if (acc == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(zimbraId);

        // remove the account from all DLs
        removeAddressFromAllDistributionLists(acc.getName()); // this doesn't throw any exceptions

        // delete all aliases of the account
        String aliases[] = acc.getMailAlias();
        if (aliases != null)
            for (int i=0; i < aliases.length; i++)
                removeAlias(acc, aliases[i]); // this also removes each alias from any DLs

        // delete all grants granted to the account
        try {
            RightCommand.revokeAllRights(this, GranteeType.GT_USER, zimbraId);
        } catch (ServiceException e) {
            // eat the exception and continue
            ZimbraLog.account.warn("cannot revoke grants", e);
        }

        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext(true);

            zlc.deleteChildren(entry.getDN());
            zlc.unbindEntry(entry.getDN());
            sAccountCache.remove(acc);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to purge account: "+zimbraId, e);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }

    }

    @Override
    public void renameAccount(String zimbraId, String newName) throws ServiceException {
        newName = IDNUtil.toAsciiEmail(newName);
        validEmailAddress(newName);

        ZimbraLdapContext zlc = null;
        Account acct = getAccountById(zimbraId, zlc, true);
        LdapEntry entry = (LdapEntry) acct;
        if (acct == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(zimbraId);
        String oldEmail = acct.getName();

        try {
            zlc = new ZimbraLdapContext(true);

            String oldDn = entry.getDN();
            String oldDomain = EmailUtil.getValidDomainPart(oldEmail);

            newName = newName.toLowerCase().trim();
            String[] parts = EmailUtil.getLocalPartAndDomain(newName);
            if (parts == null)
                throw ServiceException.INVALID_REQUEST("bad value for newName", null);
            String newLocal = parts[0];
            String newDomain = parts[1];

            Domain domain = getDomainByAsciiName(newDomain, zlc);
            if (domain == null)
                throw AccountServiceException.NO_SUCH_DOMAIN(newDomain);

            String newDn = mDIT.accountDNRename(oldDn, newLocal, domain.getName());
            boolean dnChanged = (!newDn.equals(oldDn));

            Map<String,Object> newAttrs = acct.getAttrs(false);

            newAttrs.put(Provisioning.A_uid, newLocal);
            newAttrs.put(Provisioning.A_zimbraMailDeliveryAddress, newName);
            if (oldEmail.equals(newAttrs.get(
                    Provisioning.A_zimbraPrefFromAddress))) {
                newAttrs.put(Provisioning.A_zimbraPrefFromAddress, newName);
            }

            ReplaceAddressResult replacedMails = replaceMailAddresses(acct, Provisioning.A_mail, oldEmail, newName);
            if (replacedMails.newAddrs().length == 0) {
                // Set mail to newName if the account currently does not have a mail
                newAttrs.put(Provisioning.A_mail, newName);
            } else {
                newAttrs.put(Provisioning.A_mail, replacedMails.newAddrs());
            }

            boolean domainChanged = !oldDomain.equals(newDomain);
            ReplaceAddressResult replacedAliases = replaceMailAddresses(acct, Provisioning.A_zimbraMailAlias, oldEmail, newName);
            if (replacedAliases.newAddrs().length > 0) {
                newAttrs.put(Provisioning.A_zimbraMailAlias, replacedAliases.newAddrs());

                String newDomainDN = mDIT.domainToAccountSearchDN(newDomain);

                String[] aliasNewAddrs = replacedAliases.newAddrs();
                // check up front if any of renamed aliases already exists in the new domain (if domain also got changed)
                if (domainChanged && addressExists(zlc, newDomainDN, aliasNewAddrs))
                    throw AccountServiceException.ACCOUNT_EXISTS(newName);

                // if any of the renamed aliases clashes with the account's new name, it won't be caught by the
                // above check, do a separate check.
                for (int i=0; i < aliasNewAddrs.length; i++) {
                    if (newName.equalsIgnoreCase(aliasNewAddrs[i]))
                        throw AccountServiceException.ACCOUNT_EXISTS(newName);
                }
            }

            Attributes attributes = new BasicAttributes(true);
            LdapUtil.mapToAttrs(newAttrs, attributes);

            if (dnChanged) {
                zlc.createEntry(newDn, attributes, "createAccount");
            }

            try {
                if (dnChanged)
                    zlc.moveChildren(oldDn, newDn);

                // rename the account and all it's renamed aliases to the new name in all distribution lists
                // doesn't throw exceptions, just logs
                renameAddressesInAllDistributionLists(oldEmail, newName, replacedAliases);

                // MOVE OVER ALL aliases
                // doesn't throw exceptions, just logs
                if (domainChanged)
                    moveAliases(zlc, replacedAliases, newDomain, null, oldDn, newDn, oldDomain, newDomain);

                if (!dnChanged)
                    modifyAttrs(acct, newAttrs, false, false);
            } catch (ServiceException e) {
                throw e;
            } finally {
                if (dnChanged)
                    zlc.unbindEntry(oldDn);  // unbind old dn
            }

        } catch (NameAlreadyBoundException nabe) {
            throw AccountServiceException.ACCOUNT_EXISTS(newName);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to rename account: "+zimbraId, e);
        } finally {
            // prune cache
            sAccountCache.remove(acct);
            ZimbraLdapContext.closeContext(zlc);
        }

        // reload it to cache using the master, bug 45736
        getAccountById(zimbraId, null, true);

    }

    @Override
    public void deleteDomain(String zimbraId) throws ServiceException {
        // TODO: should only allow a domain delete to succeed if there are no people
        // if there aren't, we need to delete the people trees first, then delete the domain.
        ZimbraLdapContext zlc = null;
        LdapDomain d = null;
        String acctBaseDn = null;
        try {
            zlc = new ZimbraLdapContext(true);

            d = (LdapDomain) getDomainById(zimbraId, zlc);
            if (d == null)
                throw AccountServiceException.NO_SUCH_DOMAIN(zimbraId);

            String name = d.getName();

            acctBaseDn = mDIT.domainDNToAccountBaseDN(d.getDN());
            if (!acctBaseDn.equals(d.getDN()))
                zlc.unbindEntry(acctBaseDn);

            try {
                zlc.unbindEntry(d.getDN());
                sDomainCache.remove(d);
            } catch (ContextNotEmptyException e) {
                // remove from cache before nuking all attrs
                sDomainCache.remove(d);
                // assume subdomains exist and turn into plain dc object
                Map<String, String> attrs = new HashMap<String, String>();
                attrs.put("-"+A_objectClass, "zimbraDomain");
                // remove all zimbra attrs
                for (String key : d.getAttrs(false).keySet()) {
                    if (key.startsWith("zimbra"))
                        attrs.put(key, "");
                }
                // cannot invoke callback here.  If another domain attr is added in a callback,
                // e.g. zimbraDomainStatus would add zimbraMailStatus, then we will get a LDAP
                // schema violation naming error(zimbraDomain is removed, thus there cannot be
                // any zimbraAttrs left) and the modify will fail.
                modifyAttrs(d, attrs, false, false);
            }

            String defaultDomain = getConfig().getAttr(A_zimbraDefaultDomainName, null);
            if (name.equalsIgnoreCase(defaultDomain)) {
                try {
                    Map<String, String> attrs = new HashMap<String, String>();
                    attrs.put(A_zimbraDefaultDomainName, "");
                    modifyAttrs(getConfig(), attrs);
                } catch (Exception e) {
                    ZimbraLog.account.warn("unable to remove config attr:"+A_zimbraDefaultDomainName, e);
                }
            }
        } catch (ContextNotEmptyException e) {
            // get a few entries to include in the error message
            StringBuilder sb = new StringBuilder();
            sb.append(" (remaining entries: ");
            try {
                int maxEntriesToGet = 5;
                SearchControls searchControls =
                    new SearchControls(SearchControls.SUBTREE_SCOPE, maxEntriesToGet, 0, null, false, false);
                NamingEnumeration<SearchResult> ne = zlc.searchDir(acctBaseDn, "(objectClass=*)", searchControls);
                while (ne.hasMore()) {
                    SearchResult sr = ne.next();
                    // don't show the dn itself
                    if (!sr.getNameInNamespace().equals(acctBaseDn))
                        sb.append("[" + sr.getNameInNamespace() + "] ");
                }
                ne.close();
            } catch (SizeLimitExceededException sle) {
                // this is fine
            } catch (NamingException ne) {
                ZimbraLog.account.warn("unable to get sample entries in non-empty domain " + d.getName() + " for reporting", ne);
            }
            sb.append("...)");
            throw AccountServiceException.DOMAIN_NOT_EMPTY(d.getName() + sb.toString(), e);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to purge domain: "+zimbraId, e);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }



    public void renameDomain(String zimbraId, String newDomainName) throws ServiceException {
        newDomainName = newDomainName.toLowerCase().trim();
        newDomainName = IDNUtil.toAsciiDomainName(newDomainName);
        NameUtil.validNewDomainName(newDomainName);

        ZimbraLdapContext zlc = null;

        try {
            zlc = new ZimbraLdapContext(true);

            Domain oldDomain = getDomainById(zimbraId, zlc);
            if (oldDomain == null)
               throw AccountServiceException.NO_SUCH_DOMAIN(zimbraId);

            RenameDomain rd = new RenameDomain(zlc, this, oldDomain, newDomainName);
            rd.execute();
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }

    @Override
    public void deleteCos(String zimbraId) throws ServiceException {
        LdapCos c = (LdapCos) get(CosBy.id, zimbraId);
        if (c == null)
            throw AccountServiceException.NO_SUCH_COS(zimbraId);

        if (c.isDefaultCos())
            throw ServiceException.INVALID_REQUEST("unable to delete default cos", null);

        // TODO: should we go through all accounts with this cos and remove the zimbraCOSId attr?
        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext(true);
            zlc.unbindEntry(c.getDN());
            sCosCache.remove(c);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to purge cos: "+zimbraId, e);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }

    @Override
    public Server createServer(String name, Map<String, Object> serverAttrs) throws ServiceException {
        name = name.toLowerCase().trim();

        Map<?, ?> attrManagerContext = new HashMap<Object, Object>();
        AttributeManager.getInstance().preModify(serverAttrs, null, attrManagerContext, true, true);

        String authHost = (String)serverAttrs.get(A_zimbraMtaAuthHost);
        if (authHost != null) {
            serverAttrs.put(A_zimbraMtaAuthURL, URLUtil.getMtaAuthURL(authHost));
        }

        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext(true);

            Attributes attrs = new BasicAttributes(true);
            LdapUtil.mapToAttrs(serverAttrs, attrs);

            Set<String> ocs = LdapObjectClass.getServerObjectClasses(this);
            LdapUtil.addAttr(attrs, A_objectClass, ocs);

            String zimbraIdStr = LdapUtil.generateUUID();
            attrs.put(A_zimbraId, zimbraIdStr);
            attrs.put(A_zimbraCreateTimestamp, DateUtil.toGeneralizedTime(new Date()));
            attrs.put(A_cn, name);
            String dn = mDIT.serverNametoDN(name);

            Attribute zimbraServiceHostnameAttr = attrs.get(Provisioning.A_zimbraServiceHostname);
            if (zimbraServiceHostnameAttr == null) {
                attrs.put(Provisioning.A_zimbraServiceHostname, name);
            }

            zlc.createEntry(dn, attrs, "createServer");

            Server server = getServerById(zimbraIdStr, zlc, true);
            AttributeManager.getInstance().postModify(serverAttrs, server, attrManagerContext, true);
            return server;

        } catch (NameAlreadyBoundException nabe) {
            throw AccountServiceException.SERVER_EXISTS(name);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }

    private Server getServerByQuery(String query, ZimbraLdapContext initZlc) throws ServiceException {
        ZimbraLdapContext zlc = initZlc;
        try {
            if (zlc == null)
                zlc = new ZimbraLdapContext();
            NamingEnumeration<SearchResult> ne = zlc.searchDir(mDIT.serverBaseDN(), query, sSubtreeSC);
            if (ne.hasMore()) {
                SearchResult sr = ne.next();
                ne.close();
                return new LdapServer(sr.getNameInNamespace(), sr.getAttributes(), getConfig().getServerDefaults(), this);
            }
        } catch (NameNotFoundException e) {
            return null;
        } catch (InvalidNameException e) {
            return null;
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to lookup server via query: "+query+" message: "+e.getMessage(), e);
        } finally {
            if (initZlc == null)
                ZimbraLdapContext.closeContext(zlc);
        }
        return null;
    }

    private Server getServerById(String zimbraId, ZimbraLdapContext zlc, boolean nocache) throws ServiceException {
        if (zimbraId == null)
            return null;
        Server s = null;
        if (!nocache)
            s = sServerCache.getById(zimbraId);
        if (s == null) {
            zimbraId = LdapUtil.escapeSearchFilterArg(zimbraId);
            s = getServerByQuery(LdapFilter.serverById(zimbraId), zlc);
            sServerCache.put(s);
        }
        return s;
    }

    @Override
    public Server get(ServerBy keyType, String key) throws ServiceException {
        switch(keyType) {
            case name:
                return getServerByNameInternal(key);
            case id:
                return getServerByIdInternal(key);
            case serviceHostname:
                List<Server> servers = getAllServers();
                for (Server server : servers) {
                    // when replication is enabled, should return server representing current master
                    if (key.equalsIgnoreCase(server.getAttr(Provisioning.A_zimbraServiceHostname, ""))) {
                        return server;
                    }
                }
                return null;
            default:
                    return null;
        }
    }

    private Server getServerByIdInternal(String zimbraId) throws ServiceException {
        return getServerById(zimbraId, null, false);
    }

    private Server getServerByNameInternal(String name) throws ServiceException {
        return getServerByName(name, false);
    }

    private Server getServerByName(String name, boolean nocache) throws ServiceException {
        if (!nocache) {
            Server s = sServerCache.getByName(name);
            if (s != null)
                return s;
        }
        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext();
            String dn = mDIT.serverNametoDN(name);
            Attributes attrs = zlc.getAttributes(dn);
            LdapServer s = new LdapServer(dn, attrs, getConfig().getServerDefaults(), this);
            sServerCache.put(s);
            return s;
        } catch (NameNotFoundException e) {
            return null;
        } catch (InvalidNameException e) {
            return null;
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to lookup server by name: "+name+" message: "+e.getMessage(), e);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }

    @Override
    public List<Server> getAllServers() throws ServiceException {
        return getAllServers(null);
    }

    @Override
    public List<Server> getAllServers(String service) throws ServiceException {
        List<Server> result = new ArrayList<Server>();
        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext();
            String filter;
            if (service != null) {
                filter = LdapFilter.serverByService(LdapUtil.escapeSearchFilterArg(service));
            } else {
                filter = LdapFilter.allServers();
            }
            NamingEnumeration<SearchResult> ne = zlc.searchDir(mDIT.serverBaseDN(), filter, sSubtreeSC);
            while (ne.hasMore()) {
                SearchResult sr = ne.next();
                LdapServer s = new LdapServer(sr.getNameInNamespace(), sr.getAttributes(), getConfig().getServerDefaults(), this);
                result.add(s);
            }
            ne.close();
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to list all servers", e);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
        if (result.size() > 0)
            sServerCache.put(result, true);
        Collections.sort(result);
        return result;
    }

    private List<Cos> searchCOS(String query, ZimbraLdapContext initZlc) throws ServiceException {
        List<Cos> result = new ArrayList<Cos>();
        ZimbraLdapContext zlc = initZlc;
        try {
            if (zlc == null)
                zlc = new ZimbraLdapContext();
            NamingEnumeration<SearchResult> ne = zlc.searchDir(mDIT.cosBaseDN(), query, sSubtreeSC);
            while (ne.hasMore()) {
                SearchResult sr = ne.next();
                result.add(new LdapCos(sr.getNameInNamespace(), sr.getAttributes(), this));
            }
            ne.close();
        } catch (NameNotFoundException e) {
            return null;
        } catch (InvalidNameException e) {
            return null;
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to lookup cos via query: "+query+ " message: "+e.getMessage(), e);
        } finally {
            if (initZlc == null)
                ZimbraLdapContext.closeContext(zlc);
        }
        return result;
    }

    private void removeServerFromAllCOSes(String server, ZimbraLdapContext initZlc) {
        List<Cos> coses = null;
        try {
            coses = searchCOS(LdapFilter.cosesByMailHostPool(server), initZlc);
            for (Cos cos: coses) {
                Map<String, String> attrs = new HashMap<String, String>();
                attrs.put("-"+Provisioning.A_zimbraMailHostPool, server);
                modifyAttrs(cos, attrs);
                // invalidate cached cos
                sCosCache.remove((LdapCos)cos);
            }
        } catch (ServiceException se) {
            ZimbraLog.account.warn("unable to remove "+server+" from all COSes ", se);
            return;
        }

     }

    @Override
    public void deleteServer(String zimbraId) throws ServiceException {
        LdapServer s = (LdapServer) getServerByIdInternal(zimbraId);
        if (s == null)
            throw AccountServiceException.NO_SUCH_SERVER(zimbraId);

        // TODO: what if accounts still have this server as a mailbox?
        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext(true);
            removeServerFromAllCOSes(zimbraId, zlc);
            zlc.unbindEntry(s.getDN());
            sServerCache.remove(s);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to purge server: "+zimbraId, e);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }

    /*
     *  Distribution lists.
     */

    @Override
    public DistributionList createDistributionList(String listAddress, Map<String, Object> listAttrs) throws ServiceException {

        SpecialAttrs specialAttrs = mDIT.handleSpecialAttrs(listAttrs);
        String baseDn = specialAttrs.getLdapBaseDn();

        listAddress = listAddress.toLowerCase().trim();

        String parts[] = listAddress.split("@");
        if (parts.length != 2)
            throw ServiceException.INVALID_REQUEST("must be valid list address: " + listAddress, null);

        String localPart = parts[0];
        String domain = parts[1];
        domain = IDNUtil.toAsciiDomainName(domain);
        listAddress = localPart + "@" + domain;

        validEmailAddress(listAddress);

        Map<?, ?> attrManagerContext = new HashMap<Object, Object>();
        AttributeManager.getInstance().preModify(listAttrs, null, attrManagerContext, true, true);

        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext(true);

            Domain d = getDomainByAsciiName(domain, zlc);
            if (d == null)
                throw AccountServiceException.NO_SUCH_DOMAIN(domain);
            String domainType = d.getAttr(Provisioning.A_zimbraDomainType, Provisioning.DOMAIN_TYPE_LOCAL);
            if (!domainType.equals(Provisioning.DOMAIN_TYPE_LOCAL))
                throw ServiceException.INVALID_REQUEST("domain type must be local", null);

            Attributes attrs = new BasicAttributes(true);
            LdapUtil.mapToAttrs(listAttrs, attrs);
            Attribute oc = LdapUtil.addAttr(attrs, A_objectClass, "zimbraDistributionList");
            oc.add("zimbraMailRecipient");

            String zimbraIdStr = LdapUtil.generateUUID();
            attrs.put(A_zimbraId, zimbraIdStr);
            attrs.put(A_zimbraCreateTimestamp, DateUtil.toGeneralizedTime(new Date()));
            attrs.put(A_zimbraMailAlias, listAddress);
            attrs.put(A_mail, listAddress);

            // by default a distribution list is always created enabled
            if (attrs.get(Provisioning.A_zimbraMailStatus) == null) {
                attrs.put(A_zimbraMailStatus, MAIL_STATUS_ENABLED);
            }

            Attribute a = attrs.get(Provisioning.A_displayName);
            if (a != null) {
                attrs.put(A_cn, a.get());
            }

            attrs.put(A_uid, localPart);

            String dn = mDIT.distributionListDNCreate(baseDn, attrs, localPart, domain);

            zlc.createEntry(dn, attrs, "createDistributionList");

            DistributionList dlist = getDistributionListById(zimbraIdStr, zlc);
            AttributeManager.getInstance().postModify(listAttrs, dlist, attrManagerContext, true);
            return dlist;

        } catch (NameAlreadyBoundException nabe) {
            throw AccountServiceException.DISTRIBUTION_LIST_EXISTS(listAddress);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to create distribution listt: "+listAddress, e);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }

    @Override
    public List<DistributionList> getDistributionLists(DistributionList list, boolean directOnly, Map<String, String> via) throws ServiceException {
        return LdapProvisioning.getGroups(list, directOnly, via);
    }

    private DistributionList getDistributionListByQuery(String base, String query, ZimbraLdapContext initZlc, String[] returnAttrs) throws ServiceException {
        ZimbraLdapContext zlc = initZlc;
        try {
            if (zlc == null)
                zlc = new ZimbraLdapContext();

            SearchControls searchControls =
                new SearchControls(SearchControls.SUBTREE_SCOPE, 0, 0, returnAttrs, false, false);

            NamingEnumeration<SearchResult> ne = zlc.searchDir(base, query, searchControls);
            if (ne.hasMore()) {
                SearchResult sr = ne.next();
                ne.close();
                return makeDistributionList(sr.getNameInNamespace(), sr.getAttributes());
            }
        } catch (NameNotFoundException e) {
            return null;
        } catch (InvalidNameException e) {
            return null;
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to lookup distribution list via query: "+query+ " message: "+e.getMessage(), e);
        } finally {
            if (initZlc == null)
                ZimbraLdapContext.closeContext(zlc);
        }
        return null;
    }

    @Override
    public void renameDistributionList(String zimbraId, String newEmail) throws ServiceException {
        newEmail = IDNUtil.toAsciiEmail(newEmail);
        validEmailAddress(newEmail);

        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext(true);

            LdapDistributionList dl = (LdapDistributionList) getDistributionListById(zimbraId, zlc);
            if (dl == null)
                throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(zimbraId);

            String oldEmail = dl.getName();
            String oldDomain = EmailUtil.getValidDomainPart(oldEmail);

            newEmail = newEmail.toLowerCase().trim();
            String[] parts = EmailUtil.getLocalPartAndDomain(newEmail);
            if (parts == null)
                throw ServiceException.INVALID_REQUEST("bad value for newName", null);
            String newLocal = parts[0];
            String newDomain = parts[1];

            boolean domainChanged = !oldDomain.equals(newDomain);

            Domain domain = getDomainByAsciiName(newDomain, zlc);
            if (domain == null)
                throw AccountServiceException.NO_SUCH_DOMAIN(newDomain);

            Map<String, Object> attrs = new HashMap<String, Object>();

            ReplaceAddressResult replacedMails = replaceMailAddresses(dl, Provisioning.A_mail, oldEmail, newEmail);
            if (replacedMails.newAddrs().length == 0) {
                // Set mail to newName if the account currently does not have a mail
                attrs.put(Provisioning.A_mail, newEmail);
            } else {
                attrs.put(Provisioning.A_mail, replacedMails.newAddrs());
            }

            ReplaceAddressResult replacedAliases = replaceMailAddresses(dl, Provisioning.A_zimbraMailAlias, oldEmail, newEmail);
            if (replacedAliases.newAddrs().length > 0) {
                attrs.put(Provisioning.A_zimbraMailAlias, replacedAliases.newAddrs());

                String newDomainDN = mDIT.domainToAccountSearchDN(newDomain);

                // check up front if any of renamed aliases already exists in the new domain (if domain also got changed)
                if (domainChanged && addressExists(zlc, newDomainDN, replacedAliases.newAddrs()))
                    throw AccountServiceException.DISTRIBUTION_LIST_EXISTS(newEmail);
            }

            /*
             * always reset uid to the local part, because in non default DIT the naming RDN might not
             * be uid, and ctxt.rename won't change the uid to the new localpart in that case.
             */
            attrs.put(A_uid, newLocal);

            // move over the distribution list entry
            String oldDn = dl.getDN();
            String newDn = mDIT.distributionListDNRename(oldDn, newLocal, domain.getName());
            boolean dnChanged = (!oldDn.equals(newDn));

            if (dnChanged)
                zlc.renameEntry(oldDn, newDn);

            dl = (LdapDistributionList) getDistributionListById(zimbraId, zlc);

            // rename the distribution list and all it's renamed aliases to the new name in all distribution lists
            // doesn't throw exceptions, just logs
            renameAddressesInAllDistributionLists(oldEmail, newEmail, replacedAliases); // doesn't throw exceptions, just logs

            // MOVE OVER ALL aliases
            // doesn't throw exceptions, just logs
            if (domainChanged) {
                String newUid = dl.getAttr(Provisioning.A_uid);
                moveAliases(zlc, replacedAliases, newDomain, newUid, oldDn, newDn, oldDomain, newDomain);
            }

            // this is non-atomic. i.e., rename could succeed and updating A_mail
            // could fail. So catch service exception here and log error
            try {
                modifyAttrsInternal(dl, zlc, attrs);
            } catch (ServiceException e) {
                ZimbraLog.account.error("distribution list renamed to " + newLocal +
                        " but failed to move old name's LDAP attributes", e);
                throw ServiceException.FAILURE("unable to rename distribution list: "+zimbraId, e);
            }
        } catch (NameAlreadyBoundException nabe) {
            throw AccountServiceException.DISTRIBUTION_LIST_EXISTS(newEmail);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to rename distribution list: " + zimbraId, e);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }

    @Override
    public DistributionList get(DistributionListBy keyType, String key) throws ServiceException {
        switch(keyType) {
            case id:
                return getDistributionListByIdInternal(key);
            case name:
                return getDistributionListByNameInternal(key);
            default:
                return null;
        }
    }

    private DistributionList getDistributionListById(String zimbraId, ZimbraLdapContext zlc) throws ServiceException {
        //zimbraId = LdapUtil.escapeSearchFilterArg(zimbraId);
        return getDistributionListByQuery(mDIT.mailBranchBaseDN(),
                                          LdapFilter.distributionListById(zimbraId),
                                          zlc, null);
    }

    private DistributionList getDistributionListByIdInternal(String zimbraId) throws ServiceException {
        return getDistributionListById(zimbraId, null);
    }

    @Override
    public void deleteDistributionList(String zimbraId) throws ServiceException {
        LdapDistributionList dl = (LdapDistributionList) getDistributionListByIdInternal(zimbraId);
        if (dl == null)
            throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(zimbraId);

        // remove the DL from all DLs
        removeAddressFromAllDistributionLists(dl.getName()); // this doesn't throw any exceptions

        // delete all aliases of the DL
        String aliases[] = dl.getAliases();
        if (aliases != null) {
            String dlName = dl.getName();
            for (int i=0; i < aliases.length; i++) {
                // the DL name shows up in zimbraMailAlias on the DL entry, don't bother to remove
                // this "alias" if it is the DL name, the entire DL entry will be deleted anyway.
                if (!dlName.equalsIgnoreCase(aliases[i]))
                removeAlias(dl, aliases[i]); // this also removes each alias from any DLs
            }
        }

        // delete all grants granted to the DL
        try {
             RightCommand.revokeAllRights(this, GranteeType.GT_GROUP, zimbraId);
        } catch (ServiceException e) {
            // eat the exception and continue
            ZimbraLog.account.warn("cannot revoke grants", e);
        }

        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext(true);
            zlc.unbindEntry(dl.getDN());
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to purge distribution list: "+zimbraId, e);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }

    private DistributionList getDistributionListByNameInternal(String listAddress) throws ServiceException {
        listAddress = IDNUtil.toAsciiEmail(listAddress);

        listAddress = LdapUtil.escapeSearchFilterArg(listAddress);
        return getDistributionListByQuery(mDIT.mailBranchBaseDN(),
                                          LdapFilter.distributionListByName(listAddress),
                                          null, null);
    }

    //
    // ACL group
    //
    static final String DATA_ACLGROUP_LIST = "AG_LIST";
    static final String DATA_ACLGROUP_LIST_ADMINS_ONLY = "AG_LIST_ADMINS_ONLY";


    @Override
    /*
     * - cached in LdapProvisioning
     * - returned entry contains only attrs in sMinimalDlAttrs minus zimbraMailAlias
     * - returned entry is not a "general purpose" DistributionList, it should only be used for ACL purposes
     *   to check upward membership.
     *
     */
    public DistributionList getAclGroup(DistributionListBy keyType, String key) throws ServiceException {
        switch(keyType) {
            case id:
                return getAclGroupById(key);
            case name:
                return getAclGroupByName(key);
            default:
                return null;
        }
    }

    private DistributionList getAclGroupFromCache(DistributionListBy keyType, String key) {
        switch(keyType) {
        case id:
            return sAclGroupCache.getById(key);
        case name:
            return sAclGroupCache.getByName(key);
        default:
            return null;
        }
    }

    // GROUP-TODO: consolidate with sAclGroupCache
    private DistributionList getDLFromCache(DistributionListBy keyType, String key) {
        switch(keyType) {
        case id:
            return sDLCache.getById(key);
        case name:
            return sDLCache.getByName(key);
        default:
            return null;
        }
    }

    void removeGroupFromCache(DistributionListBy keyType, String key) {
        DistributionList group = getAclGroupFromCache(keyType, key);
        if (group != null)
            removeFromCache(group);

        group = getDLFromCache(keyType, key);
        if (group != null)
            removeFromCache(group);
    }

    private DistributionList getAclGroupById(String groupId) throws ServiceException {

        DistributionList dl = sAclGroupCache.getById(groupId);
        if (dl == null) {
            dl = getDistributionListByQuery(mDIT.mailBranchBaseDN(),
                    LdapFilter.distributionListById(groupId),
                    null, sMinimalDlAttrs);
            if (dl != null) {
                // while we have the members, compute upward membership and cache it
                AclGroups groups = computeUpwardMembership(dl);
                dl.setCachedData(DATA_ACLGROUP_LIST, groups);

                // done computing upward membership, trim off big attrs that contain all members
                dl.turnToAclGroup();

                // cache it
                sAclGroupCache.put(dl);
            }
        }
        return dl;
    }

    private DistributionList getAclGroupByName(String groupName) throws ServiceException {

        DistributionList dl = sAclGroupCache.getByName(groupName);
        if (dl == null) {
            dl = getDistributionListByQuery(mDIT.mailBranchBaseDN(),
                                            LdapFilter.distributionListByName(groupName),
                                            null, sMinimalDlAttrs);
            if (dl != null) {
                // while we have the members, compute upward membership and cache it
                AclGroups groups = computeUpwardMembership(dl);
                dl.setCachedData(DATA_ACLGROUP_LIST, groups);

                // done computing upward membership, trim off big attrs that contain all members
                dl.turnToAclGroup();

                // cache it
                sAclGroupCache.put(dl);
            }
        }
        return dl;
    }

    private AclGroups computeUpwardMembership(DistributionList list) throws ServiceException {
        Map<String, String> via = new HashMap<String, String>();
        List<DistributionList> lists = LdapProvisioning.getGroups(list, false, via);
        return computeUpwardMembership(lists);
    }

    private AclGroups computeUpwardMembership(List<DistributionList> lists) {
        List<MemberOf> groups = new ArrayList<MemberOf>();
        List<String> groupIds = new ArrayList<String>();

        for (DistributionList dl : lists) {
            boolean isAdminGroup = dl.getBooleanAttr(Provisioning.A_zimbraIsAdminGroup, false);

            groups.add(new MemberOf(dl.getId(), isAdminGroup));
            groupIds.add(dl.getId());
        }

        groups = Collections.unmodifiableList(groups);
        groupIds = Collections.unmodifiableList(groupIds);

        return new AclGroups(groups, groupIds);
    }

    // filter out non-admin groups from an AclGroups instance
    private AclGroups getAdminAclGroups(AclGroups aclGroups) {
        List<MemberOf> groups = new ArrayList<MemberOf>();
        List<String> groupIds = new ArrayList<String>();

        List<MemberOf> memberOf = aclGroups.memberOf();
        for (MemberOf mo : memberOf) {
            if (mo.isAdminGroup()) {
                groups.add(mo);
                groupIds.add(mo.getId());
            }
        }

        groups = Collections.unmodifiableList(groups);
        groupIds = Collections.unmodifiableList(groupIds);

        return new AclGroups(groups, groupIds);
    }

    @Override
    public AclGroups getAclGroups(Account acct, boolean adminGroupsOnly) throws ServiceException {
        String cacheKey = adminGroupsOnly?DATA_ACLGROUP_LIST_ADMINS_ONLY:DATA_ACLGROUP_LIST;

        AclGroups dls = (AclGroups)acct.getCachedData(cacheKey);
        if (dls != null)
            return dls;

        Map<String, String> via = new HashMap<String, String>();
        List<DistributionList> lists = LdapProvisioning.getGroups(acct, false, via);

        dls = computeUpwardMembership(lists);

        if (adminGroupsOnly)
            dls = getAdminAclGroups(dls); // filter out non-admin groups

        acct.setCachedData(cacheKey, dls);
        return dls;
    }

    @Override
    /*
     * Should only be called if list was obtained from getAclGroup.
     *
     * DistributionList returned by getAclGroup always have the upward membership cached in its data cache.
     * But the data cache can be cleared by a prov.modifyAttrs call on the object. We recompute the
     * upward membership if it is not in cache.
     *
     */
    public AclGroups getAclGroups(DistributionList list, boolean adminGroupsOnly) throws ServiceException {

        // sanity check
        if (!list.isAclGroup())
            throw ServiceException.FAILURE("internal error", null);

        String cacheKey = adminGroupsOnly?DATA_ACLGROUP_LIST_ADMINS_ONLY:DATA_ACLGROUP_LIST;

        AclGroups dls = (AclGroups)list.getCachedData(cacheKey);
        if (dls != null)
            return dls;

        // reload the entry from ldap because its zimbraMailAlias was trimmed off.
        DistributionList dl = get(DistributionListBy.id, list.getId());
        if (dl == null)
            throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(list.getName());

        dls = computeUpwardMembership(dl);

        if (adminGroupsOnly)
            dls = getAdminAclGroups(dls); // filter out non-admin groups

        dl.setCachedData(cacheKey, dls);

        return dls;
    }

    @Override
    public Server getLocalServer() throws ServiceException {
        String hostname = LC.zimbra_server_hostname.value();
        if (hostname == null) {
            Zimbra.halt("zimbra_server_hostname not specified in localconfig.xml");
        }
        Server local = getServerByNameInternal(hostname);
        if (local == null) {
            Zimbra.halt("Could not find an LDAP entry for server '" + hostname + "'");
        }
        return local;
    }

    public static final long TIMESTAMP_WINDOW = Constants.MILLIS_PER_MINUTE * 5;

    private void checkAccountStatus(Account acct, Map<String, Object> authCtxt) throws ServiceException {
        /*
         * We no longer do this reload(see bug 18981):
         *     Stale data can be read back if there are replication delays and the account is
         *     refreshed from a not-caught-up replica.
         *
         * We put this reload back for bug 46767.
         *     SetPassword is always proxied to the home server,
         *     but not the AuthRequest.  Not reloading here creates the problem that after a
         *     SetPassword, if an AuthRequest (or auth from other protocols: imap/pop3) comes
         *     in on a non-home server, the new password won't get hornored; even worse, the old
         *     password will!  This hole will last until the account is aged out of cache.
         *
         *     We have to put back this reload.
         *         - if we relaod from the master, bug 18981 and bug 46767 will be taken care of,
         *           but will regress bug 20634 - master down should not block login.
         *
         *         - if we reload from the replica,
         *           1. if the replica is always caught up, everything is fine.
         *
         *           2. if the replica is slow, bug 18981 and bug 46767 can still happen.
         *              To minimize impact to bug 18981, we do this reload only when the server
         *              is not the home server of the account.
         *              For bug 46767, customer will have to fix their replica, period!
         *
         *              Note, if nginx is fronting, auth is always redirected to the home
         *              server, so bug 46767 should not happen and the reload should never
         *              be triggered(good for bug 18981).
         */
        if (!onLocalServer(acct))
            reload(acct, false);  // reload from the replica

        String accountStatus = acct.getAccountStatus(Provisioning.getInstance());
        if (accountStatus == null)
            throw AuthFailedServiceException.AUTH_FAILED(acct.getName(), AuthMechanism.namePassedIn(authCtxt), "missing account status");
        if (accountStatus.equals(Provisioning.ACCOUNT_STATUS_MAINTENANCE))
            throw AccountServiceException.MAINTENANCE_MODE();

        if (!(accountStatus.equals(Provisioning.ACCOUNT_STATUS_ACTIVE) ||
              accountStatus.equals(Provisioning.ACCOUNT_STATUS_LOCKOUT)))
            throw AuthFailedServiceException.AUTH_FAILED(acct.getName(), AuthMechanism.namePassedIn(authCtxt), "account(or domain) status is "+accountStatus);
    }

    @Override
    public void preAuthAccount(Account acct, String acctValue, String acctBy, long timestamp, long expires, String preAuth, Map<String, Object> authCtxt) throws ServiceException {
        preAuthAccount(acct, acctValue, acctBy, timestamp, expires, preAuth, false, authCtxt);
    }

    @Override
    public void preAuthAccount(Account acct, String acctValue, String acctBy, long timestamp, long expires, String preAuth, boolean admin, Map<String, Object> authCtxt) throws ServiceException {
        try {
            preAuth(acct, acctValue, acctBy, timestamp, expires, preAuth, admin, authCtxt);
            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "PreAuth","account", acct.getName(), "admin", admin+""}));
        } catch (AuthFailedServiceException e) {
            ZimbraLog.security.warn(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "PreAuth","account", acct.getName(), "admin", admin+"", "error", e.getMessage()+e.getReason(", %s")}));
            throw e;
        } catch (ServiceException e) {
            ZimbraLog.security.warn(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "PreAuth","account", acct.getName(), "admin", admin+"", "error", e.getMessage()}));
            throw e;
        }
    }


    private void verifyPreAuth(Account acct, String acctValue, String acctBy, long timestamp, long expires,
            String preAuth, boolean admin, Map<String, Object> authCtxt) throws ServiceException {

        checkAccountStatus(acct, authCtxt);
        if (preAuth == null || preAuth.length() == 0)
            throw ServiceException.INVALID_REQUEST("preAuth must not be empty", null);

        // see if domain is configured for preauth
        Provisioning prov = Provisioning.getInstance();
        String domainPreAuthKey = prov.getDomain(acct).getAttr(Provisioning.A_zimbraPreAuthKey, null);
        if (domainPreAuthKey == null)
            throw ServiceException.INVALID_REQUEST("domain is not configured for preauth", null);

        // see if request is recent
        long now = System.currentTimeMillis();
        long diff = Math.abs(now-timestamp);
        if (diff > TIMESTAMP_WINDOW) {
            Date nowDate = new Date(now);
            Date preauthDate = new Date(timestamp);
            throw AuthFailedServiceException.AUTH_FAILED(acct.getName(), AuthMechanism.namePassedIn(authCtxt),
                "preauth timestamp is too old, server time: " + nowDate.toString() + ", preauth timestamp: " + preauthDate.toString());
        }

        // compute expected preAuth
        HashMap<String,String> params = new HashMap<String,String>();
        params.put("account", acctValue);
        if (admin) params.put("admin", "1");
        params.put("by", acctBy);
        params.put("timestamp", timestamp+"");
        params.put("expires", expires+"");
        String computedPreAuth = PreAuthKey.computePreAuth(params, domainPreAuthKey);
        if (!computedPreAuth.equalsIgnoreCase(preAuth))
            throw AuthFailedServiceException.AUTH_FAILED(acct.getName(), AuthMechanism.namePassedIn(authCtxt), "preauth mismatch");

    }

    private void preAuth(Account acct, String acctValue, String acctBy, long timestamp, long expires,
            String preAuth, boolean admin, Map<String, Object> authCtxt) throws ServiceException {

        LdapLockoutPolicy lockoutPolicy = new LdapLockoutPolicy(this, acct);
        try {
            if (lockoutPolicy.isLockedOut())
                throw AuthFailedServiceException.AUTH_FAILED(acct.getName(), AuthMechanism.namePassedIn(authCtxt), "account lockout");

            // attempt to verify the preauth
            verifyPreAuth(acct, acctValue, acctBy, timestamp, expires, preAuth, admin, authCtxt);

            lockoutPolicy.successfulLogin();
        } catch (AccountServiceException e) {
            lockoutPolicy.failedLogin();
            // re-throw original exception
            throw e;
        }

        // update/check last logon
        updateLastLogon(acct);
    }

    @Override
    public void authAccount(Account acct, String password, AuthContext.Protocol proto) throws ServiceException {
        authAccount(acct, password, proto, null);
    }

    @Override
    public void authAccount(Account acct, String password, AuthContext.Protocol proto, Map<String, Object> authCtxt) throws ServiceException {
        try {
            if (password == null || password.equals(""))
                throw AuthFailedServiceException.AUTH_FAILED(acct.getName(), AuthMechanism.namePassedIn(authCtxt), "empty password");

            if (authCtxt == null)
                authCtxt = new HashMap<String, Object>();

            // add proto to the auth context
            authCtxt.put(AuthContext.AC_PROTOCOL, proto);

            authAccount(acct, password, true, authCtxt);
            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "Auth","account", acct.getName(), "protocol", proto.toString()}));
        } catch (AuthFailedServiceException e) {
            ZimbraLog.security.warn(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "Auth","account", acct.getName(), "protocol", proto.toString(), "error", e.getMessage() + e.getReason(", %s")}));
            throw e;
        } catch (ServiceException e) {
            ZimbraLog.security.warn(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "Auth","account", acct.getName(), "protocol", proto.toString(), "error", e.getMessage()}));
            throw e;
        }
    }

    private void authAccount(Account acct, String password, boolean checkPasswordPolicy, Map<String, Object> authCtxt) throws ServiceException {
        checkAccountStatus(acct, authCtxt);

        AuthMechanism authMech = AuthMechanism.makeInstance(acct);
        verifyPassword(acct, password, authMech, authCtxt);

        // true:  authenticating
        // false: changing password (we do *not* want to update last login in this case)
        if (!checkPasswordPolicy)
            return;

        if (authMech.checkPasswordAging()) {
            // below this point, the only fault that may be thrown is CHANGE_PASSWORD
            int maxAge = acct.getIntAttr(Provisioning.A_zimbraPasswordMaxAge, 0);
            if (maxAge > 0) {
                Date lastChange = acct.getGeneralizedTimeAttr(Provisioning.A_zimbraPasswordModifiedTime, null);
                if (lastChange != null) {
                    long last = lastChange.getTime();
                    long curr = System.currentTimeMillis();
                    if ((last+(ONE_DAY_IN_MILLIS * maxAge)) < curr)
                        throw AccountServiceException.CHANGE_PASSWORD();
                }
            }

            boolean mustChange = acct.getBooleanAttr(Provisioning.A_zimbraPasswordMustChange, false);
            if (mustChange)
                throw AccountServiceException.CHANGE_PASSWORD();
        }

        // update/check last logon
        updateLastLogon(acct);

    }

    @Override
    public void accountAuthed(Account acct) throws ServiceException {
        updateLastLogon(acct);
    }

    private void updateLastLogon(Account acct) throws ServiceException {
        Config config = Provisioning.getInstance().getConfig();
        long freq = config.getTimeInterval(
                    Provisioning.A_zimbraLastLogonTimestampFrequency,
                    com.zimbra.cs.util.Config.D_ZIMBRA_LAST_LOGON_TIMESTAMP_FREQUENCY);

        // never update timestamp if frequency is 0
        if (freq == 0)
            return;

        Date lastLogon = acct.getGeneralizedTimeAttr(Provisioning.A_zimbraLastLogonTimestamp, null);
        if (lastLogon == null) {
            Map<String, String> attrs = new HashMap<String, String>();
            attrs.put(Provisioning.A_zimbraLastLogonTimestamp, DateUtil.toGeneralizedTime(new Date()));
            try {
                modifyAttrs(acct, attrs);
            } catch (ServiceException e) {
                ZimbraLog.account.warn("updating zimbraLastLogonTimestamp", e);
            }
        } else {
            long current = System.currentTimeMillis();
            if (current - freq >= lastLogon.getTime()) {
                Map<String, String> attrs = new HashMap<String , String>();
                attrs.put(Provisioning.A_zimbraLastLogonTimestamp, DateUtil.toGeneralizedTime(new Date()));
                try {
                    modifyAttrs(acct, attrs);
                } catch (ServiceException e) {
                    ZimbraLog.account.warn("updating zimbraLastLogonTimestamp", e);
                }
            }
        }

    }

    public void externalLdapAuth(Domain d, String authMech, Account acct, String password, Map<String, Object> authCtxt) throws ServiceException {
        String url[] = d.getMultiAttr(Provisioning.A_zimbraAuthLdapURL);

        if (url == null || url.length == 0) {
            String msg = "attr not set "+Provisioning.A_zimbraAuthLdapURL;
            ZimbraLog.account.fatal(msg);
            throw ServiceException.FAILURE(msg, null);
        }

        boolean requireStartTLS = d.getBooleanAttr(Provisioning.A_zimbraAuthLdapStartTlsEnabled, false);

        try {
            // try explicit externalDn first
            String externalDn = acct.getAttr(Provisioning.A_zimbraAuthLdapExternalDn);

            if (externalDn != null) {
                if (ZimbraLog.account.isDebugEnabled()) ZimbraLog.account.debug("auth with explicit dn of "+externalDn);
                LdapUtil.ldapAuthenticate(url, requireStartTLS, externalDn, password);
                return;
            }

            String searchFilter = d.getAttr(Provisioning.A_zimbraAuthLdapSearchFilter);
            if (searchFilter != null && !AM_AD.equals(authMech)) {
                String searchPassword = d.getAttr(Provisioning.A_zimbraAuthLdapSearchBindPassword);
                String searchDn = d.getAttr(Provisioning.A_zimbraAuthLdapSearchBindDn);
                String searchBase = d.getAttr(Provisioning.A_zimbraAuthLdapSearchBase);
                if (searchBase == null) searchBase = "";
                searchFilter = LdapUtil.computeAuthDn(acct.getName(), searchFilter);
                if (ZimbraLog.account.isDebugEnabled()) ZimbraLog.account.debug("auth with search filter of "+searchFilter);
                LdapUtil.ldapAuthenticate(url, requireStartTLS, password, searchBase, searchFilter, searchDn, searchPassword);
                return;
            }

            String bindDn = d.getAttr(Provisioning.A_zimbraAuthLdapBindDn);
            if (bindDn != null) {
                String dn = LdapUtil.computeAuthDn(acct.getName(), bindDn);
                if (ZimbraLog.account.isDebugEnabled()) ZimbraLog.account.debug("auth with bind dn template of "+dn);
                LdapUtil.ldapAuthenticate(url, requireStartTLS, dn, password);
                return;
            }

        } catch (AuthenticationException e) {
            throw AuthFailedServiceException.AUTH_FAILED(acct.getName(), AuthMechanism.namePassedIn(authCtxt), "external LDAP auth failed, "+e.getMessage(), e);
        } catch (AuthenticationNotSupportedException e) {
            throw AuthFailedServiceException.AUTH_FAILED(acct.getName(), AuthMechanism.namePassedIn(authCtxt), "external LDAP auth failed, "+e.getMessage(), e);
        } catch (NamingException e) {
            throw ServiceException.FAILURE(e.getMessage(), e);
        } catch (IOException e) {
            throw ServiceException.FAILURE(e.getMessage(), e);
        }

        String msg = "one of the following attrs must be set "+
                Provisioning.A_zimbraAuthLdapBindDn+", "+Provisioning.A_zimbraAuthLdapSearchFilter;
        ZimbraLog.account.fatal(msg);
        throw ServiceException.FAILURE(msg, null);
    }

    private void verifyPassword(Account acct, String password, AuthMechanism authMech, Map<String, Object> authCtxt) throws ServiceException {

        LdapLockoutPolicy lockoutPolicy = new LdapLockoutPolicy(this, acct);
        try {
            if (lockoutPolicy.isLockedOut())
                throw AuthFailedServiceException.AUTH_FAILED(acct.getName(), AuthMechanism.namePassedIn(authCtxt), "account lockout");

            // attempt to verify the password
            verifyPasswordInternal(acct, password, authMech, authCtxt);

            lockoutPolicy.successfulLogin();
        } catch (AccountServiceException e) {
            // TODO: only consider it failed if exception was due to password-mismatch
            lockoutPolicy.failedLogin();
            // re-throw original exception
            throw e;
        }
    }

    /*
     * authAccount does all the status/mustChange checks, this just takes the
     * password and auths the user
     */
    private void verifyPasswordInternal(Account acct, String password, AuthMechanism authMech, Map<String, Object> context) throws ServiceException {

        Domain domain = Provisioning.getInstance().getDomain(acct);

        boolean allowFallback = true;
        if (!authMech.isZimbraAuth())
            allowFallback =
                domain.getBooleanAttr(Provisioning.A_zimbraAuthFallbackToLocal, false) ||
                acct.getBooleanAttr(Provisioning.A_zimbraIsAdminAccount, false) ||
                acct.getBooleanAttr(Provisioning.A_zimbraIsDomainAdminAccount, false);

        try {
            authMech.doAuth(this, domain, acct, password, context);
            return;
        } catch (ServiceException e) {
            if (!allowFallback || authMech.isZimbraAuth())
                throw e;
            ZimbraLog.account.warn(authMech.getMechanism() + " auth for domain " +
                domain.getName() + " failed, fall back to zimbra default auth mechanism", e);
        }

        // fall back to zimbra default auth
        AuthMechanism.doZimbraAuth(this, domain, acct, password, context);
    }

     /**
       * Takes the specified format string, and replaces any % followed by a single character
       * with the value in the specified vars hash. If the value isn't found in the hash, uses
       * a default value of "".
       * @param fmt the format string
       * @param vars should have a key which is a String, and a value which is also a String.
       * @return the formatted string
       */
      public static String expandStr(String fmt, Map<String, String> vars) {
         if (fmt == null || fmt.equals(""))
             return fmt;

         if (fmt.indexOf('%') == -1)
             return fmt;

         StringBuffer sb = new StringBuffer(fmt.length()+32);
         for (int i=0; i < fmt.length(); i++) {
             char ch = fmt.charAt(i);
             if (ch == '%') {
                 i++;
                 if (i > fmt.length())
                     return sb.toString();
                 ch = fmt.charAt(i);
                 if (ch != '%') {
                     String val = vars.get(Character.toString(ch));
                     if (val != null)
                         sb.append(val);
                     else
                         sb.append(ch);
                 } else {
                     sb.append(ch);
                 }
             } else {
                 sb.append(ch);
             }
         }
         return sb.toString();
     }

    @Override
    public void changePassword(Account acct, String currentPassword, String newPassword) throws ServiceException {
        authAccount(acct, currentPassword, false, null);
        boolean locked = acct.getBooleanAttr(Provisioning.A_zimbraPasswordLocked, false);
        if (locked)
            throw AccountServiceException.PASSWORD_LOCKED();
        setPassword(acct, newPassword, true);
    }

    /**
     * @param newPassword
     * @throws AccountServiceException
     */
    private void checkHistory(String newPassword, String[] history) throws AccountServiceException {
        if (history == null)
            return;
        for (int i=0; i < history.length; i++) {
            int sepIndex = history[i].indexOf(':');
            if (sepIndex != -1)  {
                String encoded = history[i].substring(sepIndex+1);
                if (PasswordUtil.SSHA.verifySSHA(encoded, newPassword))
                    throw AccountServiceException.PASSWORD_RECENTLY_USED();
            }
        }
    }



    /**
     * update password history
     * @param history current history
     * @param currentPassword the current encoded-password
     * @param maxHistory number of prev passwords to keep
     * @return new hsitory
     */
    private String[] updateHistory(String history[], String currentPassword, int maxHistory) {
        String[] newHistory = history;
        if (currentPassword == null)
            return null;

        String currentHistory = System.currentTimeMillis() + ":"+currentPassword;

        // just add if empty or room
        if (history == null || history.length < maxHistory) {

            if (history == null) {
                newHistory = new String[1];
            } else {
                newHistory = new String[history.length+1];
                System.arraycopy(history, 0, newHistory, 0, history.length);
            }
            newHistory[newHistory.length-1] = currentHistory;
            return newHistory;
        }

        // remove oldest, add current
        long min = Long.MAX_VALUE;
        int minIndex = -1;
        for (int i = 0; i < history.length; i++) {
            int sepIndex = history[i].indexOf(':');
            if (sepIndex == -1) {
                // nuke it if no separator
                minIndex = i;
                break;
            }
            long val = Long.parseLong(history[i].substring(0, sepIndex));
            if (val < min) {
                min = val;
                minIndex = i;
            }
        }
        if (minIndex == -1)
            minIndex = 0;
        history[minIndex] = currentHistory;
        return history;
    }

    @Override
    public void setPassword(Account acct, String newPassword) throws ServiceException {
        setPassword(acct, newPassword, false);
    }

    @Override
    public void checkPasswordStrength(Account acct, String password) throws ServiceException {
        checkPasswordStrength(password, acct, null, null);
    }

    private int getInt(Account acct, Cos cos, Attributes attrs, String name, int defaultValue) throws NamingException {
        if (acct != null)
            return acct.getIntAttr(name, defaultValue);

        String v = LdapUtil.getAttrString(attrs, name);
        if (v == null)
            return cos.getIntAttr(name, defaultValue);
        else {
            try {
                return Integer.parseInt(v);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
    }


    /**
     * called to check password strength. Should pass in either an Account, or Cos/Attributes (during creation).
     *
     * @param password
     * @param acct
     * @param cos
     * @param attrs
     * @throws ServiceException
     */
    private void checkPasswordStrength(String password, Account acct, Cos cos, Attributes attrs) throws ServiceException {
        try {
            int minLength = getInt(acct, cos, attrs, Provisioning.A_zimbraPasswordMinLength, 0);
            if (minLength > 0 && password.length() < minLength)
                throw AccountServiceException.INVALID_PASSWORD("too short", new Argument(Provisioning.A_zimbraPasswordMinLength, minLength, Argument.Type.NUM));

            int maxLength = getInt(acct, cos, attrs, Provisioning.A_zimbraPasswordMaxLength, 0);
            if (maxLength > 0 && password.length() > maxLength)
                throw AccountServiceException.INVALID_PASSWORD("too long", new Argument(Provisioning.A_zimbraPasswordMaxLength, maxLength, Argument.Type.NUM));

            int minUpperCase = getInt(acct, cos, attrs, Provisioning.A_zimbraPasswordMinUpperCaseChars, 0);
            int minLowerCase = getInt(acct, cos, attrs, Provisioning.A_zimbraPasswordMinLowerCaseChars, 0);
            int minNumeric = getInt(acct, cos, attrs, Provisioning.A_zimbraPasswordMinNumericChars, 0);
            int minPunctuation = getInt(acct, cos, attrs, Provisioning.A_zimbraPasswordMinPunctuationChars, 0);

            if (minUpperCase > 0 || minLowerCase > 0 || minPunctuation > 0 || minNumeric > 0) {
                int upper=0, lower=0, punctuation = 0, numeric = 0;
                for (int i=0; i < password.length(); i++) {
                    int ch = password.charAt(i);
                    if (Character.isUpperCase(ch)) upper++;
                    else if (Character.isLowerCase(ch)) lower++;
                    else if (Character.isDigit(ch)) numeric++;
                    else if (isAsciiPunc(ch)) punctuation++;
                }

                if (upper < minUpperCase) throw AccountServiceException.INVALID_PASSWORD("not enough upper case characters",
                                                                                         new Argument(Provisioning.A_zimbraPasswordMinUpperCaseChars, minUpperCase, Argument.Type.NUM));
                if (lower < minLowerCase) throw AccountServiceException.INVALID_PASSWORD("not enough lower case characters",
                                                                                         new Argument(Provisioning.A_zimbraPasswordMinLowerCaseChars, minLowerCase, Argument.Type.NUM));
                if (numeric < minNumeric) throw AccountServiceException.INVALID_PASSWORD("not enough numeric characters",
                                                                                         new Argument(Provisioning.A_zimbraPasswordMinNumericChars, minNumeric, Argument.Type.NUM));
                if (punctuation < minPunctuation) throw AccountServiceException.INVALID_PASSWORD("not enough punctuation characters",
                                                                                         new Argument(Provisioning.A_zimbraPasswordMinPunctuationChars, minPunctuation, Argument.Type.NUM));
            }

        } catch (NamingException ne) {
            throw ServiceException.FAILURE(ne.getMessage(), ne);
        }
    }

    private boolean isAsciiPunc(int ch) {
        return
            (ch >= 33 && ch <= 47) || // ! " # $ % & ' ( ) * + , - . /
            (ch >= 58 && ch <= 64) || // : ; < = > ? @
            (ch >= 91 && ch <= 96) || // [ \ ] ^ _ `
            (ch >=123 && ch <= 126);  // { | } ~
    }

    // called by create account
    private void setInitialPassword(Cos cos, Attributes attrs, String newPassword) throws ServiceException, NamingException {
        String userPassword = LdapUtil.getAttrString(attrs, Provisioning.A_userPassword);
        if (userPassword == null && (newPassword == null || "".equals(newPassword))) return;

        if (userPassword == null) {
            checkPasswordStrength(newPassword, null, cos, attrs);
            userPassword = PasswordUtil.SSHA.generateSSHA(newPassword, null);
        }
        attrs.put(Provisioning.A_userPassword, userPassword);
        attrs.put(Provisioning.A_zimbraPasswordModifiedTime, DateUtil.toGeneralizedTime(new Date()));
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Account#setPassword(java.lang.String)
     */
    void setPassword(Account acct, String newPassword, boolean enforcePolicy) throws ServiceException {

        boolean mustChange = acct.getBooleanAttr(Provisioning.A_zimbraPasswordMustChange, false);

        if (enforcePolicy) {
            checkPasswordStrength(newPassword, acct, null, null);

            // skip min age checking if mustChange is set
            if (!mustChange) {
                int minAge = acct.getIntAttr(Provisioning.A_zimbraPasswordMinAge, 0);
                if (minAge > 0) {
                    Date lastChange = acct.getGeneralizedTimeAttr(Provisioning.A_zimbraPasswordModifiedTime, null);
                    if (lastChange != null) {
                        long last = lastChange.getTime();
                        long curr = System.currentTimeMillis();
                        if ((last+(ONE_DAY_IN_MILLIS * minAge)) > curr)
                            throw AccountServiceException.PASSWORD_CHANGE_TOO_SOON();
                    }
                }
            }
        }

        Map<String, Object> attrs = new HashMap<String, Object>();

        int enforceHistory = acct.getIntAttr(Provisioning.A_zimbraPasswordEnforceHistory, 0);
        if (enforceHistory > 0) {
            String[] newHistory = updateHistory(
                    acct.getMultiAttr(Provisioning.A_zimbraPasswordHistory),
                    acct.getAttr(Provisioning.A_userPassword),
                    enforceHistory);
            attrs.put(Provisioning.A_zimbraPasswordHistory, newHistory);
            checkHistory(newPassword, newHistory);
        }

        String encodedPassword = PasswordUtil.SSHA.generateSSHA(newPassword, null);

        // unset it so it doesn't take up space...
        if (mustChange)
            attrs.put(Provisioning.A_zimbraPasswordMustChange, "");

        attrs.put(Provisioning.A_userPassword, encodedPassword);
        attrs.put(Provisioning.A_zimbraPasswordModifiedTime, DateUtil.toGeneralizedTime(new Date()));

        // update the validity value to invalidate auto-standing auth tokens
        acct.setAuthTokenValidityValue(acct.getAuthTokenValidityValue()+1, attrs);

        ChangePasswordListener cpListener = ChangePasswordListener.getHandler(acct);
        Map<String, Object> context = null;
        if (cpListener != null) {
            context = new HashMap<String, Object>();
            cpListener.preModify(acct, newPassword, context, attrs);
        }

        modifyAttrs(acct, attrs);

        if (cpListener != null)
            cpListener.postModify(acct, newPassword, context);
    }

    @Override
    public Zimlet getZimlet(String name) throws ServiceException {
        return getZimlet(name, null, true);
    }

    private Zimlet getFromCache(ZimletBy keyType, String key) {
        switch(keyType) {
        case name:
            return sZimletCache.getByName(key);
        case id:
            return sZimletCache.getById(key);
        default:
            return null;
        }
    }

    Zimlet lookupZimlet(String name, ZimbraLdapContext zlc) throws ServiceException {
        return getZimlet(name, zlc, false);
    }

    private Zimlet getZimlet(String name, ZimbraLdapContext initZlc, boolean useCache) throws ServiceException {
        LdapZimlet zimlet = sZimletCache.getByName(name);
        if (!useCache || zimlet == null) {
            ZimbraLdapContext zlc = initZlc;
            try {
                if (zlc == null) {
                    zlc = new ZimbraLdapContext();
                }
                String dn = mDIT.zimletNameToDN(name);
                Attributes attrs = zlc.getAttributes(dn);
                zimlet = new LdapZimlet(dn, attrs, this);
                if (useCache) {
                    ZimletUtil.reloadZimlet(name);
                    sZimletCache.put(zimlet);  // put LdapZimlet into the cache after successful ZimletUtil.reloadZimlet()
                }
            } catch (NameNotFoundException nnfe) {
                return null;
            } catch (NamingException ne) {
                throw ServiceException.FAILURE("unable to get zimlet: "+name, ne);
            } catch (ZimletException ze) {
                throw ServiceException.FAILURE("unable to load zimlet: "+name, ze);
            } finally {
                if (initZlc == null) {
                    ZimbraLdapContext.closeContext(zlc);
                }
            }
        }
        return zimlet;
    }

    @Override
    public List<Zimlet> listAllZimlets() throws ServiceException {
        List<Zimlet> result = new ArrayList<Zimlet>();
        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext();
            NamingEnumeration<SearchResult> ne = zlc.searchDir(mDIT.zimletBaseDN(), LdapFilter.allZimlets(), sSubtreeSC);
            while (ne.hasMore()) {
                SearchResult sr = ne.next();
             result.add(new LdapZimlet(sr.getNameInNamespace(), sr.getAttributes(), this));
            }
            ne.close();
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to list all zimlets", e);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
        Collections.sort(result);
        return result;
    }

    @Override
    public Zimlet createZimlet(String name, Map<String, Object> zimletAttrs) throws ServiceException {
        name = name.toLowerCase().trim();

        Map<String, Object> attrManagerContext = new HashMap<String, Object>();
        AttributeManager.getInstance().preModify(zimletAttrs, null, attrManagerContext, true, true);

        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext();

            Attributes attrs = new BasicAttributes(true);
            String hasKeyword = LdapUtil.LDAP_FALSE;
            if (zimletAttrs.containsKey(A_zimbraZimletKeyword)) {
                hasKeyword = Provisioning.TRUE;
            }
            LdapUtil.mapToAttrs(zimletAttrs, attrs);
            LdapUtil.addAttr(attrs, A_objectClass, "zimbraZimletEntry");
            LdapUtil.addAttr(attrs, A_zimbraZimletEnabled, Provisioning.FALSE);
            LdapUtil.addAttr(attrs, A_zimbraZimletIndexingEnabled, hasKeyword);
            LdapUtil.addAttr(attrs, A_zimbraCreateTimestamp, DateUtil.toGeneralizedTime(new Date()));

            String dn = mDIT.zimletNameToDN(name);
            zlc.createEntry(dn, attrs, "createZimlet");

            Zimlet zimlet = lookupZimlet(name, zlc);
            AttributeManager.getInstance().postModify(zimletAttrs, zimlet, attrManagerContext, true);
            return zimlet;
        } catch (NameAlreadyBoundException nabe) {
            throw ServiceException.FAILURE("zimlet already exists: "+name, nabe);
        } catch (ServiceException se) {
            throw se;
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }

    @Override
    public void deleteZimlet(String name) throws ServiceException {
        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext();
            LdapZimlet zimlet = (LdapZimlet)getZimlet(name, zlc, true);
            if (zimlet != null) {
                sZimletCache.remove(zimlet);
                zlc.unbindEntry(zimlet.getDN());
            }
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to delete zimlet: "+name, e);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }

    @Override
    public CalendarResource createCalendarResource(String emailAddress,String password,
            Map<String, Object> calResAttrs) throws ServiceException {
        emailAddress = emailAddress.toLowerCase().trim();

        calResAttrs.put(Provisioning.A_zimbraAccountCalendarUserType,
                        AccountCalendarUserType.RESOURCE.toString());

        SpecialAttrs specialAttrs = mDIT.handleSpecialAttrs(calResAttrs);

        Map<String, Object> attrManagerContext = new HashMap<String, Object>();

        Set<String> ocs = LdapObjectClass.getCalendarResourceObjectClasses(this);
        Account acct = createAccount(emailAddress, password, calResAttrs, specialAttrs, ocs.toArray(new String[0]), false, null);

        LdapCalendarResource resource =
            (LdapCalendarResource) getCalendarResourceById(acct.getId(), true);
        AttributeManager.getInstance().
            postModify(calResAttrs, resource, attrManagerContext, true);
        return resource;
    }

    @Override
    public void deleteCalendarResource(String zimbraId)
    throws ServiceException {
        deleteAccount(zimbraId);
    }

    @Override
    public void renameCalendarResource(String zimbraId, String newName)
    throws ServiceException {
        renameAccount(zimbraId, newName);
    }

    @Override
    public CalendarResource get(CalendarResourceBy keyType, String key) throws ServiceException {
        return get(keyType, key, false);
    }

    @Override
    public CalendarResource get(CalendarResourceBy keyType, String key, boolean loadFromMaster) throws ServiceException {
        switch(keyType) {
            case id:
                return getCalendarResourceById(key, loadFromMaster);
            case foreignPrincipal:
                return getCalendarResourceByForeignPrincipal(key, loadFromMaster);
            case name:
                return getCalendarResourceByName(key, loadFromMaster);
            default:
                    return null;
        }
    }

    private CalendarResource getCalendarResourceById(String zimbraId, boolean loadFromMaster)
    throws ServiceException {
        if (zimbraId == null)
            return null;
        LdapCalendarResource resource =
            (LdapCalendarResource) sAccountCache.getById(zimbraId);
        if (resource == null) {
            zimbraId = LdapUtil.escapeSearchFilterArg(zimbraId);
            resource = (LdapCalendarResource) getAccountByQuery(
                mDIT.mailBranchBaseDN(),
                LdapFilter.calendarResourceById(zimbraId),
                null, loadFromMaster);
            sAccountCache.put(resource);
        }
        return resource;
    }

    private CalendarResource getCalendarResourceByName(String emailAddress, boolean loadFromMaster)
    throws ServiceException {

        emailAddress = fixupAccountName(emailAddress);

        LdapCalendarResource resource =
            (LdapCalendarResource) sAccountCache.getByName(emailAddress);
        if (resource == null) {
            emailAddress = LdapUtil.escapeSearchFilterArg(emailAddress);
            resource = (LdapCalendarResource) getAccountByQuery(
                mDIT.mailBranchBaseDN(),
                LdapFilter.calendarResourceByName(emailAddress),
                null, loadFromMaster);
            sAccountCache.put(resource);
        }
        return resource;
    }

    private CalendarResource getCalendarResourceByForeignPrincipal(String foreignPrincipal, boolean loadFromMaster)
    throws ServiceException {
        foreignPrincipal = LdapUtil.escapeSearchFilterArg(foreignPrincipal);
        LdapCalendarResource resource =
            (LdapCalendarResource) getAccountByQuery(
                mDIT.mailBranchBaseDN(),
                LdapFilter.calendarResourceByForeignPrincipal(foreignPrincipal),
                null, loadFromMaster);
        sAccountCache.put(resource);
        return resource;
    }

    @Override
    public List<NamedEntry> searchCalendarResources(EntrySearchFilter filter,
            String returnAttrs[], String sortAttr, boolean sortAscending)
            throws ServiceException {
        return searchCalendarResources(filter, returnAttrs, sortAttr,
                sortAscending, mDIT.mailBranchBaseDN());
    }

    List<NamedEntry> searchCalendarResources(EntrySearchFilter filter,
            String returnAttrs[], String sortAttr, boolean sortAscending,
            String base) throws ServiceException {
        String query = LdapEntrySearchFilter.toLdapCalendarResourcesFilter(filter);
        return searchObjects(query, returnAttrs, sortAttr, sortAscending, base,
                Provisioning.SA_CALENDAR_RESOURCE_FLAG, 0);
    }

    private Account makeAccount(String dn, Attributes attrs) throws NamingException, ServiceException {
        return makeAccount(dn, attrs, 0);
    }

    private Account makeAccountNoDefaults(String dn, Attributes attrs) throws NamingException, ServiceException {
        return makeAccount(dn, attrs, Provisioning.SO_NO_ACCOUNT_DEFAULTS | Provisioning.SO_NO_ACCOUNT_SECONDARY_DEFAULTS);
    }

    private Account makeAccount(String dn, Attributes attrs, int flags) throws NamingException, ServiceException {
        Attribute a = attrs.get(Provisioning.A_zimbraAccountCalendarUserType);
        boolean isAccount = (a == null) || a.contains(AccountCalendarUserType.USER.toString());

        String emailAddress = LdapUtil.getAttrString(attrs, Provisioning.A_zimbraMailDeliveryAddress);
        if (emailAddress == null)
            emailAddress = mDIT.dnToEmail(dn, attrs);

        Account acct = (isAccount) ? new LdapAccount(dn, emailAddress, attrs, null, this) : new LdapCalendarResource(dn, emailAddress, attrs, null, this);

        setAccountDefaults(acct, flags);

        return acct;
    }

    public void setAccountDefaults(Account acct, int flags) throws ServiceException {
        boolean dontSetDefaults = (flags & Provisioning.SO_NO_ACCOUNT_DEFAULTS) == Provisioning.SO_NO_ACCOUNT_DEFAULTS;
        if (dontSetDefaults)
            return;

        boolean dontSetSecondaryDefaults = (flags & Provisioning.SO_NO_ACCOUNT_SECONDARY_DEFAULTS) == Provisioning.SO_NO_ACCOUNT_SECONDARY_DEFAULTS;

        Cos cos = getCOS(acct); // will set cos if not set yet

        Map<String, Object> defaults = null;
        if (cos != null)
            defaults = cos.getAccountDefaults();

        if (dontSetSecondaryDefaults) {
            // set only primary defaults
            acct.setDefaults(defaults);
        } else {
            // set primary and secondary defaults
            Map<String, Object> secondaryDefaults = null;
            Domain domain = getDomain(acct);
            if (domain != null)
                secondaryDefaults = domain.getAccountDefaults();
            acct.setDefaults(defaults, secondaryDefaults);
        }
    }

    private Alias makeAlias(String dn, Attributes attrs, LdapProvisioning prov) throws NamingException, ServiceException {
        String emailAddress = mDIT.dnToEmail(dn, attrs);
        Alias alias = new LdapAlias(dn, emailAddress, attrs, prov);
        return alias;
    }

    private DistributionList makeDistributionList(String dn, Attributes attrs) throws NamingException, ServiceException {
        String emailAddress = mDIT.dnToEmail(dn, attrs);
        DistributionList dl = new LdapDistributionList(dn, emailAddress, attrs, this);
        return dl;
    }


    /**
     *  called when an account/dl is renamed
     *
     */
    protected void renameAddressesInAllDistributionLists(String oldName, String newName, ReplaceAddressResult replacedAliasPairs) {
        Map<String, String> changedPairs = new HashMap<String, String>();

        changedPairs.put(oldName, newName);
        for (int i=0 ; i < replacedAliasPairs.oldAddrs().length; i++) {
            String oldAddr = replacedAliasPairs.oldAddrs()[i];
            String newAddr = replacedAliasPairs.newAddrs()[i];
            if (!oldAddr.equals(newAddr))
                changedPairs.put(oldAddr, newAddr);
        }

        renameAddressesInAllDistributionLists(changedPairs);
    }

    protected void renameAddressesInAllDistributionLists(Map<String, String> changedPairs) {

        String oldAddrs[] = changedPairs.keySet().toArray(new String[0]);
        String newAddrs[] = changedPairs.values().toArray(new String[0]);

        List<DistributionList> lists = null;
        Map<String, String[]> attrs = null;

        try {
            lists = getAllDistributionListsForAddresses(oldAddrs, false);
        } catch (ServiceException se) {
            ZimbraLog.account.warn("unable to rename addr "+oldAddrs.toString()+" in all DLs ", se);
            return;
        }

        for (DistributionList list: lists) {
            // should we just call removeMember/addMember? This might be quicker, because calling
            // removeMember/addMember might have to update an entry's zimbraMemberId twice
            if (attrs == null) {
                attrs = new HashMap<String, String[]>();
                attrs.put("-" + Provisioning.A_zimbraMailForwardingAddress, oldAddrs);
                attrs.put("+" + Provisioning.A_zimbraMailForwardingAddress, newAddrs);
            }
            try {
                modifyAttrs(list, attrs);
                //list.removeMember(oldName)
                //list.addMember(newName);
            } catch (ServiceException se) {
                // log warning an continue
                ZimbraLog.account.warn("unable to rename "+oldAddrs.toString()+" to "+newAddrs.toString()+" in DL "+list.getName(), se);
            }
        }
    }

    /**
     *  called when an account is being deleted. swallows all exceptions (logs warnings).
     */
    void removeAddressFromAllDistributionLists(String address) {
        String addrs[] = new String[] { address } ;
        removeAddressesFromAllDistributionLists(addrs);
    }

    /**
     *  called when an account is being deleted or status being set to closed. swallows all exceptions (logs warnings).
     */
    public void removeAddressesFromAllDistributionLists(String[] addrs) {
        List<DistributionList> lists = null;
        try {
            lists = getAllDistributionListsForAddresses(addrs, false);
        } catch (ServiceException se) {
            StringBuilder sb = new StringBuilder();
            for (String addr : addrs)
                sb.append(addr + ", ");
            ZimbraLog.account.warn("unable to get all DLs for addrs " + sb.toString());
            return;
        }

        for (DistributionList list: lists) {
            try {
                removeMembers(list, addrs);
            } catch (ServiceException se) {
                // log warning and continue
                StringBuilder sb = new StringBuilder();
                for (String addr : addrs)
                    sb.append(addr + ", ");
                ZimbraLog.account.warn("unable to remove "+sb.toString()+" from DL "+list.getName(), se);
            }
        }
    }

    private List<DistributionList> getAllDistributionListsForAddresses(String addrs[], boolean minimalData) throws ServiceException {
        if (addrs == null || addrs.length == 0)
            return new ArrayList<DistributionList>();
        StringBuilder sb = new StringBuilder();
        if (addrs.length > 1)
            sb.append("(|");
        for (int i=0; i < addrs.length; i++) {
            sb.append(String.format("(%s=%s)", Provisioning.A_zimbraMailForwardingAddress, addrs[i]));
        }
        if (addrs.length > 1)
            sb.append(")");
        String [] attrs = minimalData ? sMinimalDlAttrs : null;

        return (List<DistributionList>) searchAccountsInternal(sb.toString(), attrs, null, true, Provisioning.SA_DISTRIBUTION_LIST_FLAG);

    }

    // GROUP-TODO: retire after getGroupsInternal is stable and have callsites call it directly
    private static List<DistributionList> getGroups(Entry entry, boolean directOnly, Map<String, String> via) throws ServiceException {
        if (DebugConfig.disableComputeGroupMembershipOptimization) {
            // old way
            String[] addr = ((GroupedEntry)entry).getAllAddrsAsGroupMember();
            return getDistributionLists(addr, directOnly, via);
        } else {
            // new way
            return getGroupsInternal(entry, directOnly, via);
        }
    }

    // GROUP-TODO: deprecate after getGroupsInternal is stable
    private static List<DistributionList> getDistributionLists(String addrs[], boolean directOnly, Map<String, String> via)
        throws ServiceException
    {
        LdapProvisioning prov = (LdapProvisioning) Provisioning.getInstance(); // GROSS
        List<DistributionList> directDLs = prov.getAllDistributionListsForAddresses(addrs, true);
        HashSet<String> directDLSet = new HashSet<String>();
        HashSet<String> checked = new HashSet<String>();
        List<DistributionList> result = new ArrayList<DistributionList>();

        Stack<DistributionList> dlsToCheck = new Stack<DistributionList>();

        for (DistributionList dl : directDLs) {
            dlsToCheck.push(dl);
            directDLSet.add(dl.getName());
        }

        while (!dlsToCheck.isEmpty()) {
            DistributionList dl = dlsToCheck.pop();
            if (checked.contains(dl.getId())) continue;
            result.add(dl);
            checked.add(dl.getId());
            if (directOnly) continue;

            String[] dlAddrs = dl.getAllAddrsAsGroupMember();
            List<DistributionList> newLists = prov.getAllDistributionListsForAddresses(dlAddrs, true);

            for (DistributionList newDl: newLists) {
                if (!directDLSet.contains(newDl.getName())) {
                    if (via != null) via.put(newDl.getName(), dl.getName());
                    dlsToCheck.push(newDl);
                }
            }
        }
        Collections.sort(result);
        return result;
    }

    private static List<DistributionList> getAllDirectGroups(LdapProvisioning prov, Entry entry) throws ServiceException {
        if (!(entry instanceof GroupedEntry))
            throw ServiceException.FAILURE("internal error", null);

        String cacheKey = EntryCacheDataKey.GROUPEDENTRY_DIRECT_GROUPIDS.getKeyName();
        @SuppressWarnings("unchecked")
        List<String> directGroupIds = (List<String>) entry.getCachedData(cacheKey);

        List<DistributionList> directGroups = null;

        if (directGroupIds == null) {
            String[] addrs = ((GroupedEntry)entry).getAllAddrsAsGroupMember();

            // fetch from LDAP
            directGroups = prov.getAllDistributionListsForAddresses(addrs, true);

            // - build the group id list and cache it on the entry
            // - add each group in cache only if it is not already in.
            //   we do not want to overwrite the entry in cache, because it
            //   might have its direct group ids cached on it.
            // - if the group is already in cache, return the cached instance
            //   instead of the instance we ject fetched, because the cached
            //   instance might have its direct group ids cached on it.
            directGroupIds = new ArrayList<String>(directGroups.size());
            List<DistributionList> directGroupsToReturn = new ArrayList<DistributionList>(directGroups.size());
            for (DistributionList group : directGroups) {
                String groupId = group.getId();
                directGroupIds.add(groupId);
                DistributionList cached = sDLCache.getById(groupId);
                if (cached == null) {
                    sDLCache.put(group);
                    directGroupsToReturn.add(group);
                } else {
                    directGroupsToReturn.add(cached);
                }
            }
            entry.setCachedData(cacheKey, directGroupIds);
            return directGroupsToReturn;

        } else {
            directGroups = new ArrayList<DistributionList>();
            Set<String> idsToRemove = null;
            for (String groupId : directGroupIds) {
                DistributionList group = prov.getGroup(DistributionListBy.id, groupId);
                if (group == null) {
                    // the group could have been deleted
                    // remove it from our direct group id cache on the entry
                    if (idsToRemove == null)
                        idsToRemove = new HashSet<String>();
                    idsToRemove.add(groupId);
                } else
                    directGroups.add(group);
            }

            // update our direct group id cache if needed
            if (idsToRemove != null) {
                // create a new object, do *not* update directly on the cached copy
                List<String> updatedDirectGroupIds = new ArrayList<String>();
                for (String id : directGroupIds) {
                    if (!idsToRemove.contains(id))
                        updatedDirectGroupIds.add(id);
                }

                // swap the new data in
                entry.setCachedData(cacheKey, updatedDirectGroupIds);
            }
        }

        return directGroups;
    }

    // like get(DistributionListBy keyType, String key)
    // the difference are:
    //     - cached
    //     - entry returned only contains minimal DL attrs
    //
    // TODO: generalize it to be a Provisioning method
    private DistributionList getGroup(DistributionListBy keyType, String key) throws ServiceException {
        switch(keyType) {
        case id:
            return getGroupById(key);
        case name:
            return getGroupByName(key);
        default:
           return null;
        }
    }

    private DistributionList getGroupById(String groupId) throws ServiceException {
        DistributionList group = sDLCache.getById(groupId);
        if (group == null) {
            // fetch from LDAP
            group = getDistributionListByQuery(mDIT.mailBranchBaseDN(),
                        LdapFilter.distributionListById(groupId),
                        null, sMinimalDlAttrs);
            if (group != null)
                sDLCache.put(group);
        }

        return group;
    }

    private DistributionList getGroupByName(String groupName) throws ServiceException {
        DistributionList group = sDLCache.getByName(groupName);
        if (group == null) {
            // fetch from LDAP
            group = getDistributionListByQuery(mDIT.mailBranchBaseDN(),
                        LdapFilter.distributionListByName(groupName),
                        null, sMinimalDlAttrs);
            if (group != null)
                sDLCache.put(group);
        }

        return group;
    }

    private static List<DistributionList> getGroupsInternal(Entry entry, boolean directOnly, Map<String, String> via)
        throws ServiceException
    {
        LdapProvisioning prov = (LdapProvisioning) Provisioning.getInstance(); // GROSS
        List<DistributionList> directDLs = getAllDirectGroups(prov, entry);
        HashSet<String> directDLSet = new HashSet<String>();
        HashSet<String> checked = new HashSet<String>();
        List<DistributionList> result = new ArrayList<DistributionList>();

        Stack<DistributionList> dlsToCheck = new Stack<DistributionList>();

        for (DistributionList dl : directDLs) {
            dlsToCheck.push(dl);
            directDLSet.add(dl.getName());
        }

        while (!dlsToCheck.isEmpty()) {
            DistributionList dl = dlsToCheck.pop();
            if (checked.contains(dl.getId())) continue;
            result.add(dl);
            checked.add(dl.getId());
            if (directOnly) continue;

            List<DistributionList> newLists = getAllDirectGroups(prov, dl);

            for (DistributionList newDl: newLists) {
                if (!directDLSet.contains(newDl.getName())) {
                    if (via != null) via.put(newDl.getName(), dl.getName());
                    dlsToCheck.push(newDl);
                }
            }
        }
        Collections.sort(result);
        return result;
    }

    static final String DATA_DL_SET = "DL_SET";

    @Override
    public Set<String> getDistributionLists(Account acct) throws ServiceException {
        @SuppressWarnings("unchecked")
        Set<String> dls = (Set<String>) acct.getCachedData(DATA_DL_SET);
        if (dls != null) return dls;

        dls = new HashSet<String>();

        List<DistributionList> lists = getDistributionLists(acct, false, null);

        for (DistributionList dl : lists) {
            dls.add(dl.getId());
        }
        dls = Collections.unmodifiableSet(dls);
        acct.setCachedData(DATA_DL_SET, dls);
        return dls;

    }

    @Override
    public boolean inDistributionList(Account acct, String zimbraId) throws ServiceException {
        return getDistributionLists(acct).contains(zimbraId);
    }


    @Override
    public boolean inDistributionList(DistributionList list, String zimbraId) throws ServiceException {
        DistributionList group = list;

        // if the dl is not an AclGroup, get one because AclGroup are cached in LdapProvisioning and
        // upward membership are for the group is cached on the AclGroup object
        if (!list.isAclGroup())
            group = getAclGroup(DistributionListBy.id, list.getId());

        AclGroups aclGroups = getAclGroups(group, false);
        return aclGroups.groupIds().contains(zimbraId);
    }

    @Override
    public List<DistributionList> getDistributionLists(Account acct, boolean directOnly, Map<String, String> via) throws ServiceException {
        return LdapProvisioning.getGroups(acct, directOnly, via);
    }

    private static final int DEFAULT_GAL_MAX_RESULTS = 100;

    private static final String DATA_GAL_RULES = "GAL_RULES";



    @Override
    public List<?> getAllAccounts(Domain d) throws ServiceException {
        return searchAccounts(d, mDIT.filterAccountsByDomain(d, false), null, null, true, Provisioning.SA_ACCOUNT_FLAG);
    }

    @Override
    public void getAllAccounts(Domain d, NamedEntry.Visitor visitor) throws ServiceException {
        LdapDomain ld = (LdapDomain) d;
        searchObjects(mDIT.filterAccountsByDomain(d, false), null, mDIT.domainDNToAccountSearchDN(ld.getDN()), Provisioning.SA_ACCOUNT_FLAG, visitor, 0);
    }

    @Override
    public void getAllAccounts(Domain d, Server s, NamedEntry.Visitor visitor) throws ServiceException {
        getAllAccountsInternal(d, s, visitor, false);
    }


    public void getAllAccountsNoDefaults(Domain d, Server s, NamedEntry.Visitor visitor) throws ServiceException {
        getAllAccountsInternal(d, s, visitor, true);
    }

    private void getAllAccountsInternal(Domain d, Server s, NamedEntry.Visitor visitor, boolean noDefaults) throws ServiceException {
        LdapDomain ld = (LdapDomain) d;
        String filter = mDIT.filterAccountsByDomain(d, false);
        if (s != null) {
            String serverFilter = "(" + Provisioning.A_zimbraMailHost + "=" + s.getAttr(Provisioning.A_zimbraServiceHostname) + ")";
            if (StringUtil.isNullOrEmpty(filter))
                filter = serverFilter;
            else
                filter = "(&" + serverFilter + filter + ")";
        }

        int flags = Provisioning.SA_ACCOUNT_FLAG;
        if (noDefaults)
            flags |= SO_NO_ACCOUNT_DEFAULTS;
        searchObjects(filter, null, mDIT.domainDNToAccountSearchDN(ld.getDN()), flags, visitor, 0);
    }

    @Override
    public List<?> getAllCalendarResources(Domain d) throws ServiceException {
        return searchAccounts(d, mDIT.filterCalendarResourcesByDomain(d, false),
                              null, null, true, Provisioning.SA_CALENDAR_RESOURCE_FLAG);
        /*
        return searchCalendarResources(d,
                LdapEntrySearchFilter.sCalendarResourcesFilter,
                null, null, true);
        */
    }

    @Override
    public void getAllCalendarResources(Domain d, NamedEntry.Visitor visitor)
    throws ServiceException {
        LdapDomain ld = (LdapDomain) d;
        searchObjects(mDIT.filterCalendarResourcesByDomain(d, false),
                      null, mDIT.domainDNToAccountSearchDN(ld.getDN()),
                      Provisioning.SA_CALENDAR_RESOURCE_FLAG,
                      visitor, 0);
    }

    @Override
    public void getAllCalendarResources(Domain d, Server s, NamedEntry.Visitor visitor)
    throws ServiceException {
        LdapDomain ld = (LdapDomain) d;
        String filter = mDIT.filterCalendarResourcesByDomain(d, false);
        if (s != null) {
            String serverFilter = "(" + Provisioning.A_zimbraMailHost + "=" + s.getAttr(Provisioning.A_zimbraServiceHostname) + ")";
            if (StringUtil.isNullOrEmpty(filter))
                filter = serverFilter;
            else
                filter = "(&" + serverFilter + filter + ")";
        }
        searchObjects(filter, null, mDIT.domainDNToAccountSearchDN(ld.getDN()),
                      Provisioning.SA_CALENDAR_RESOURCE_FLAG, visitor, 0);
    }

    @Override
    public List<?> getAllDistributionLists(Domain d) throws ServiceException {
        return searchAccounts(d, mDIT.filterDistributionListsByDomain(d, false),
                              null, null, true, Provisioning.SA_DISTRIBUTION_LIST_FLAG);
    }

    @Override
    public List searchAccounts(Domain d, String query, String returnAttrs[], String sortAttr, boolean sortAscending, int flags) throws ServiceException
    {
        LdapDomain ld = (LdapDomain) d;
        return searchObjects(query, returnAttrs, sortAttr, sortAscending,
                             mDIT.domainDNToAccountSearchDN(ld.getDN()), flags, 0);
    }

    @Override
    public List<NamedEntry> searchDirectory(SearchOptions options) throws ServiceException {
        return searchDirectory(options, true);
    }

    private void addBase(Set<String> bases, String base) {
        boolean add = true;
        for (String b : bases) {
            if (mDIT.isUnder(b, base)) {
                add = false;
                break;
            }
        }
        if (add)
            bases.add(base);
    }

    public String[] getSearchBases(int flags) {
        Set<String> bases = new HashSet<String>();

        boolean accounts = (flags & Provisioning.SA_ACCOUNT_FLAG) != 0;
        boolean aliases = (flags & Provisioning.SA_ALIAS_FLAG) != 0;
        boolean lists = (flags & Provisioning.SA_DISTRIBUTION_LIST_FLAG) != 0;
        boolean calendarResources = (flags & Provisioning.SA_CALENDAR_RESOURCE_FLAG) != 0;
        boolean domains = (flags & Provisioning.SA_DOMAIN_FLAG) != 0;
        boolean coses = (flags & Provisioning.SD_COS_FLAG) != 0;

        if (accounts || aliases || lists || calendarResources)
            addBase(bases, mDIT.mailBranchBaseDN());

        if (accounts)
            addBase(bases, mDIT.adminBaseDN());

        if (domains)
            addBase(bases, mDIT.domainBaseDN());

        if (coses)
            addBase(bases, mDIT.cosBaseDN());

        return bases.toArray(new String[bases.size()]);
    }

    private List<NamedEntry> searchDirectory(SearchOptions options, boolean useConnPool) throws ServiceException {
        String base = null;

        LdapDomain ld = (LdapDomain) options.getDomain();
        if (ld != null)
            base = mDIT.domainDNToAccountSearchDN(ld.getDN());
        else {
            String bs = options.getBase();
            if (bs != null)
                base = bs;
        }

        String bases[];
        if (base == null)
            bases = getSearchBases(options.getFlags());
        else
            bases = new String[] {base};

        String query = options.getQuery();

        if (options.getConvertIDNToAscii() && query != null && query.length()>0)
            query = LdapEntrySearchFilter.toLdapIDNFilter(query);

        return searchObjects(query,
                             options.getReturnAttrs(),
                             options.getSortAttr(),
                             options.isSortAscending(),
                             bases,
                             options.getFlags(),
                             options.getMaxResults(),
                             useConnPool,
                             options.getOnMaster());
    }

    @Override
    public List<NamedEntry> searchCalendarResources(
        Domain d,
        EntrySearchFilter filter,
        String returnAttrs[],
        String sortAttr,
        boolean sortAscending)
    throws ServiceException {
        return searchCalendarResources(filter, returnAttrs,
                                       sortAttr, sortAscending,
                                       LdapUtil.getZimbraSearchBase(d, GalOp.search));
    }

    @Override
    public SearchGalResult searchGal(Domain d,
                                     String n,
                                     Provisioning.GAL_SEARCH_TYPE type,
                                     String token)
    throws ServiceException {
        return searchGal(d, n, type, null, token, null);
    }

    @Override
    public SearchGalResult searchGal(Domain d,
                                     String n,
                                     GAL_SEARCH_TYPE type,
                                     String token,
                                     GalContact.Visitor visitor) throws ServiceException {
        return searchGal(d, n, type, null, token, visitor);
    }

    @Override
    public SearchGalResult searchGal(Domain d,
                                     String n,
                                     Provisioning.GAL_SEARCH_TYPE type,
                                     GalMode galMode,
                                     String token) throws ServiceException {
        return searchGal(d, n, type, galMode, token, null);
    }

    private SearchGalResult searchGal(Domain d,
                                      String n,
                                      Provisioning.GAL_SEARCH_TYPE type,
                                      GalMode galMode,
                                      String token,
                                      GalContact.Visitor visitor)
    throws ServiceException {
        GalOp galOp = token != null ? GalOp.sync : GalOp.search;
        // escape user-supplied string
        n = LdapUtil.escapeSearchFilterArg(n);

        int maxResults = token != null ? 0 : d.getIntAttr(Provisioning.A_zimbraGalMaxResults, DEFAULT_GAL_MAX_RESULTS);
        if (type == Provisioning.GAL_SEARCH_TYPE.CALENDAR_RESOURCE)
            return searchResourcesGal(d, n, maxResults, token, galOp, visitor);

        GalMode mode = galMode != null ? galMode : GalMode.fromString(d.getAttr(Provisioning.A_zimbraGalMode));
        SearchGalResult results = null;
        if (mode == null || mode == GalMode.zimbra) {
            results = searchZimbraGal(d, n, maxResults, token, galOp, visitor);
        } else if (mode == GalMode.ldap) {
            results = searchLdapGal(d, n, maxResults, token, galOp, visitor);
        } else if (mode == GalMode.both) {
            results = searchZimbraGal(d, n, maxResults/2, token, galOp, visitor);
            SearchGalResult ldapResults = searchLdapGal(d, n, maxResults/2, token, galOp, visitor);
            if (ldapResults != null) {
                results.addMatches(ldapResults);
                results.setToken(LdapUtil.getLaterTimestamp(results.getToken(), ldapResults.getToken()));
            }
        } else {
            results = searchZimbraGal(d, n, maxResults, token, galOp, visitor);
        }
        if (results == null) results = SearchGalResult.newSearchGalResult(visitor);  // should really not be null by now

        if (type == Provisioning.GAL_SEARCH_TYPE.ALL) {
            SearchGalResult resourceResults = null;
            if (maxResults == 0)
                resourceResults = searchResourcesGal(d, n, 0, token, galOp, visitor);
            else {
                int room = maxResults - results.getNumMatches();
                if (room > 0)
                    resourceResults = searchResourcesGal(d, n, room, token, galOp, visitor);
            }
            if (resourceResults != null) {
                results.addMatches(resourceResults);
                results.setToken(LdapUtil.getLaterTimestamp(results.getToken(), resourceResults.getToken()));
            }
        }

        return results;
    }

    @Override
    public SearchGalResult autoCompleteGal(Domain d, String n, Provisioning.GAL_SEARCH_TYPE type, int max) throws ServiceException
    {
        GalOp galOp = GalOp.autocomplete;
        // escape user-supplied string
        n = LdapUtil.escapeSearchFilterArg(n);

        int maxResults = Math.min(max, d.getIntAttr(Provisioning.A_zimbraGalMaxResults, DEFAULT_GAL_MAX_RESULTS));
        if (type == Provisioning.GAL_SEARCH_TYPE.CALENDAR_RESOURCE)
            return searchResourcesGal(d, n, maxResults, null, galOp, null);

        GalMode mode = GalMode.fromString(d.getAttr(Provisioning.A_zimbraGalMode));
        SearchGalResult results = null;
        if (mode == null || mode == GalMode.zimbra) {
            results = searchZimbraGal(d, n, maxResults, null, galOp, null);
        } else if (mode == GalMode.ldap) {
            results = searchLdapGal(d, n, maxResults, null, galOp, null);
        } else if (mode == GalMode.both) {
            results = searchZimbraGal(d, n, maxResults/2, null, galOp, null);
            SearchGalResult ldapResults = searchLdapGal(d, n, maxResults/2, null, galOp, null);
            if (ldapResults != null) {
                results.addMatches(ldapResults);
                results.setToken(LdapUtil.getLaterTimestamp(results.getToken(), ldapResults.getToken()));
                results.setHadMore(results.getHadMore() || ldapResults.getHadMore());
            }
        } else {
            results = searchZimbraGal(d, n, maxResults, null, galOp, null);
        }
        if (results == null) results = SearchGalResult.newSearchGalResult(null);  // should really not be null by now

        if (type == Provisioning.GAL_SEARCH_TYPE.ALL) {
            SearchGalResult resourceResults = null;
            if (maxResults == 0)
                resourceResults = searchResourcesGal(d, n, 0, null, galOp, null);
            else {
                int room = maxResults - results.getNumMatches();
                if (room > 0)
                    resourceResults = searchResourcesGal(d, n, room, null, galOp, null);
            }
            if (resourceResults != null) {
                results.addMatches(resourceResults);
                results.setToken(LdapUtil.getLaterTimestamp(results.getToken(), resourceResults.getToken()));
                results.setHadMore(results.getHadMore() || resourceResults.getHadMore());
            }
        }
        Collections.sort(results.getMatches());
        return results;
    }

    public static String getFilterDef(String name) throws ServiceException {
        String queryExprs[] = Provisioning.getInstance().getConfig().getMultiAttr(Provisioning.A_zimbraGalLdapFilterDef);
        String fname = name+":";
        String queryExpr = null;
        for (int i=0; i < queryExprs.length; i++) {
            if (queryExprs[i].startsWith(fname)) {
                queryExpr = queryExprs[i].substring(fname.length());
            }
        }

        if (queryExpr == null)
            ZimbraLog.gal.warn("missing filter def " + name + " in " + Provisioning.A_zimbraGalLdapFilterDef);

        return queryExpr;
    }

    private synchronized LdapGalMapRules getGalRules(Domain d) {
        LdapGalMapRules rules = (LdapGalMapRules) d.getCachedData(DATA_GAL_RULES);
        if (rules == null) {
            String[] attrs = d.getMultiAttr(Provisioning.A_zimbraGalLdapAttrMap);
            rules = new LdapGalMapRules(attrs);
            d.setCachedData(DATA_GAL_RULES, rules);
        }
        return rules;
    }

    private SearchGalResult searchResourcesGal(Domain d, String n, int maxResults, String token, GalOp galOp, GalContact.Visitor visitor)
    throws ServiceException {
        return searchZimbraWithNamedFilter(d, galOp, GalNamedFilter.getZimbraCalendarResourceFilter(galOp), n, maxResults, token, visitor);
    }

    private SearchGalResult searchZimbraGal(Domain d, String n, int maxResults, String token, GalOp galOp, GalContact.Visitor visitor)
    throws ServiceException {
        return searchZimbraWithNamedFilter(d, galOp, GalNamedFilter.getZimbraAcountFilter(galOp), n, maxResults, token, visitor);
    }

    private SearchGalResult searchZimbraWithNamedFilter(
        Domain domain,
        GalOp galOp,
        String filterName,
        String n,
        int maxResults,
        String token,
        GalContact.Visitor visitor)
    throws ServiceException {
        GalParams.ZimbraGalParams galParams = new GalParams.ZimbraGalParams(domain, galOp);

        String queryExpr = getFilterDef(filterName);
        String query = null;
        if (queryExpr != null) {
            if (token != null)
                n = "";

            query = GalUtil.expandFilter(null, queryExpr, n, token, true);
        }

        SearchGalResult result = SearchGalResult.newSearchGalResult(visitor);
        result.setTokenizeKey(GalUtil.tokenizeKey(galParams, galOp));
        if (query == null) {
            ZimbraLog.gal.warn("searchZimbraWithNamedFilter query is null");
            return result;
        }

        // filter out hidden entries
        query = "(&("+query+")(!(zimbraHideInGal=TRUE)))";

        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext(false);
            LdapUtil.searchGal(zlc,
                               galParams.pageSize(),
                               galParams.searchBase(),
                               query,
                               maxResults,
                               getGalRules(domain),
                               token,
                               result);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }

        //Collections.sort(result);
        return result;
    }

    private SearchGalResult searchLdapGal(Domain domain,
                                          String n,
                                          int maxResults,
                                          String token,
                                          GalOp galOp,
                                          GalContact.Visitor visitor)
    throws ServiceException {

        GalParams.ExternalGalParams galParams = new GalParams.ExternalGalParams(domain, galOp);

        LdapGalMapRules rules = getGalRules(domain);
        try {
            return LdapUtil.searchLdapGal(galParams,
                                          galOp,
                                          n,
                                          maxResults,
                                          rules,
                                          token,
                                          visitor);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to search GAL", e);
        } catch (IOException e) {
            throw ServiceException.FAILURE("unable to search GAL", e);
        }
    }

    @Override
    public void addMembers(DistributionList list, String[] members) throws ServiceException {
        LdapDistributionList ldl = (LdapDistributionList) list;
        ldl.addMembers(members, this);
    }

    @Override
    public void removeMembers(DistributionList list, String[] members) throws ServiceException {
        LdapDistributionList ldl = (LdapDistributionList) list;
        ldl.removeMembers(members, this);
    }

    private List<Identity> getIdentitiesByQuery(LdapEntry entry, String query, ZimbraLdapContext initZlc) throws ServiceException {
        ZimbraLdapContext zlc = initZlc;
        List<Identity> result = new ArrayList<Identity>();
        try {
            if (zlc == null)
                zlc = new ZimbraLdapContext();
            String base = entry.getDN();
            NamingEnumeration<SearchResult> ne = zlc.searchDir(base, query, sSubtreeSC);
            while(ne.hasMore()) {
                SearchResult sr = ne.next();
                result.add(new LdapIdentity((Account)entry, sr.getNameInNamespace(), sr.getAttributes(), this));
            }
            ne.close();
        } catch (NameNotFoundException e) {
            ZimbraLog.account.warn("caught NameNotFoundException", e);
            return result;
        } catch (InvalidNameException e) {
            ZimbraLog.account.warn("caught InvalidNameException", e);
            return result;
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to lookup identity via query: "+query+ " message: "+e.getMessage(), e);
        } finally {
            if (initZlc == null)
                ZimbraLdapContext.closeContext(zlc);
        }
        return result;
    }

    private Identity getIdentityByName(LdapEntry entry, String name,  ZimbraLdapContext zlc) throws ServiceException {
        name = LdapUtil.escapeSearchFilterArg(name);
        List<Identity> result = getIdentitiesByQuery(entry, LdapFilter.identityByName(name), zlc);
        return result.isEmpty() ? null : result.get(0);
    }

    private String getIdentityDn(LdapEntry entry, String name) {
        return A_zimbraPrefIdentityName + "=" + LdapUtil.escapeRDNValue(name) + "," + entry.getDN();
    }

    private void validateIdentityAttrs(Map<String, Object> attrs) throws ServiceException {
        Set<String> validAttrs = AttributeManager.getInstance().getLowerCaseAttrsInClass(AttributeClass.identity);
        for (String key : attrs.keySet()) {
            if (!validAttrs.contains(key.toLowerCase())) {
                throw ServiceException.INVALID_REQUEST("unable to modify attr: "+key, null);
            }
        }
    }

    private static final String IDENTITY_LIST_CACHE_KEY = "LdapProvisioning.IDENTITY_CACHE";

    @Override
    public Identity createIdentity(Account account, String identityName, Map<String, Object> identityAttrs) throws ServiceException {
        return createIdentity(account, identityName, identityAttrs, false);
    }

    @Override
    public Identity restoreIdentity(Account account, String identityName, Map<String, Object> identityAttrs) throws ServiceException {
        return createIdentity(account, identityName, identityAttrs, true);
    }

    private Identity createIdentity(Account account, String identityName, Map<String, Object> identityAttrs,
            boolean restoring) throws ServiceException {
        removeAttrIgnoreCase("objectclass", identityAttrs);
        validateIdentityAttrs(identityAttrs);

        LdapEntry ldapEntry = (LdapEntry) (account instanceof LdapEntry ? account : getAccountById(account.getId()));

        if (ldapEntry == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(account.getName());

        if (identityName.equalsIgnoreCase(DEFAULT_IDENTITY_NAME))
                throw AccountServiceException.IDENTITY_EXISTS(identityName);

        List<Identity> existing = getAllIdentities(account);
        if (existing.size() >= account.getLongAttr(A_zimbraIdentityMaxNumEntries, 20))
            throw AccountServiceException.TOO_MANY_IDENTITIES();

        account.setCachedData(IDENTITY_LIST_CACHE_KEY, null);

        Map<?, ?> attrManagerContext = new HashMap<Object, Object>();
        boolean checkImmutable = !restoring;
        AttributeManager.getInstance().preModify(identityAttrs, null, attrManagerContext, true, checkImmutable);

        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext(true);

            String dn = getIdentityDn(ldapEntry, identityName);

            Attributes attrs = new BasicAttributes(true);
            LdapUtil.mapToAttrs(identityAttrs, attrs);
            LdapUtil.addAttr(attrs, A_objectClass, "zimbraIdentity");

            String identityId = LdapUtil.getAttrString(attrs, A_zimbraPrefIdentityId);
            if (identityId == null) {
                identityId = LdapUtil.generateUUID();
                attrs.put(A_zimbraPrefIdentityId, identityId);
            }
            attrs.put(Provisioning.A_zimbraCreateTimestamp, DateUtil.toGeneralizedTime(new Date()));

            zlc.createEntry(dn, attrs, "createIdentity");

            Identity identity = getIdentityByName(ldapEntry, identityName, zlc);
            AttributeManager.getInstance().postModify(identityAttrs, identity, attrManagerContext, true);

            return identity;
        } catch (NameAlreadyBoundException nabe) {
            throw AccountServiceException.IDENTITY_EXISTS(identityName);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to create identity", e);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }

    @Override
    public void modifyIdentity(Account account, String identityName, Map<String, Object> identityAttrs) throws ServiceException {
        removeAttrIgnoreCase("objectclass", identityAttrs);

        validateIdentityAttrs(identityAttrs);

        LdapEntry ldapEntry = (LdapEntry) (account instanceof LdapEntry ? account : getAccountById(account.getId()));

        if (ldapEntry == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(account.getName());

        // clear cache
        account.setCachedData(IDENTITY_LIST_CACHE_KEY, null);

        if (identityName.equalsIgnoreCase(DEFAULT_IDENTITY_NAME)) {
            modifyAttrs(account, identityAttrs);
        } else {

            LdapIdentity identity = (LdapIdentity) getIdentityByName(ldapEntry, identityName, null);
            if (identity == null)
                    throw AccountServiceException.NO_SUCH_IDENTITY(identityName);

            String name = (String) identityAttrs.get(A_zimbraPrefIdentityName);
            boolean newName = (name != null && !name.equals(identityName));
            if (newName) identityAttrs.remove(A_zimbraPrefIdentityName);

            modifyAttrs(identity, identityAttrs, true);
            if (newName) {
                // the identity cache could've been loaded again if getAllIdentities were called in pre/poseModify callback, so we clear it again
                account.setCachedData(IDENTITY_LIST_CACHE_KEY, null);
                renameIdentity(ldapEntry, identity, name);
            }

        }
    }

    private void renameIdentity(LdapEntry entry, LdapIdentity identity, String newIdentityName) throws ServiceException {

        if (identity.getName().equalsIgnoreCase(DEFAULT_IDENTITY_NAME))
            throw ServiceException.INVALID_REQUEST("can't rename default identity", null);

        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext(true);
            String newDn = getIdentityDn(entry, newIdentityName);
            zlc.renameEntry(identity.getDN(), newDn);
        } catch (InvalidNameException e) {
            throw ServiceException.INVALID_REQUEST("invalid identity name: "+newIdentityName, e);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to rename identity: "+newIdentityName, e);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }

    @Override
    public void deleteIdentity(Account account, String identityName) throws ServiceException {
        LdapEntry ldapEntry = (LdapEntry) (account instanceof LdapEntry ? account : getAccountById(account.getId()));
        if (ldapEntry == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(account.getName());

        if (identityName.equalsIgnoreCase(DEFAULT_IDENTITY_NAME))
            throw ServiceException.INVALID_REQUEST("can't delete default identity", null);

        account.setCachedData(IDENTITY_LIST_CACHE_KEY, null);

        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext(true);
            Identity identity = getIdentityByName(ldapEntry, identityName, zlc);
            if (identity == null)
                throw AccountServiceException.NO_SUCH_IDENTITY(identityName);
            String dn = getIdentityDn(ldapEntry, identityName);
            zlc.unbindEntry(dn);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to delete identity: "+identityName, e);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }

    @Override
    public List<Identity> getAllIdentities(Account account) throws ServiceException {
        LdapEntry ldapEntry = (LdapEntry) (account instanceof LdapEntry ? account : getAccountById(account.getId()));
        if (ldapEntry == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(account.getName());

        @SuppressWarnings("unchecked")
        List<Identity> result = (List<Identity>) account.getCachedData(IDENTITY_LIST_CACHE_KEY);

        if (result != null) {
            return result;
        }

        result = getIdentitiesByQuery(ldapEntry, LdapFilter.allIdentities(), null);
        for (Identity identity: result) {
            // gross hack for 4.5beta. should be able to remove post 4.5
            if (identity.getId() == null) {
                String id = LdapUtil.generateUUID();
                identity.setId(id);
                Map<String, Object> newAttrs = new HashMap<String, Object>();
                newAttrs.put(Provisioning.A_zimbraPrefIdentityId, id);
                try {
                    modifyIdentity(account, identity.getName(), newAttrs);
                } catch (ServiceException se) {
                    ZimbraLog.account.warn("error updating identity: "+account.getName()+" "+identity.getName()+" "+se.getMessage(), se);
                }
            }
        }
        result.add(getDefaultIdentity(account));
        result = Collections.unmodifiableList(result);
        account.setCachedData(IDENTITY_LIST_CACHE_KEY, result);
        return result;
    }

    @Override
    public Identity get(Account account, IdentityBy keyType, String key) throws ServiceException {
        LdapEntry ldapEntry = (LdapEntry) (account instanceof LdapEntry ? account : getAccountById(account.getId()));
        if (ldapEntry == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(account.getName());

        // this assumes getAllIdentities is cached and number of identities is reasaonble.
        // might want a per-identity cache (i.e., use "IDENTITY_BY_ID_"+id as a key, etc)
        switch(keyType) {
            case id:
                for (Identity identity : getAllIdentities(account))
                    if (identity.getId().equals(key)) return identity;
                return null;
            case name:
                for (Identity identity : getAllIdentities(account))
                    if (identity.getName().equalsIgnoreCase(key)) return identity;
                return null;
            default:
                return null;
        }
    }

    private List<Signature> getSignaturesByQuery(Account acct, LdapEntry entry, String query, ZimbraLdapContext initZlc, List<Signature> result) throws ServiceException {
        ZimbraLdapContext zlc = initZlc;
        if (result == null)
            result = new ArrayList<Signature>();
        try {
            if (zlc == null)
                zlc = new ZimbraLdapContext();
            String base = entry.getDN();
            NamingEnumeration<SearchResult> ne = zlc.searchDir(base, query, sSubtreeSC);
            while(ne.hasMore()) {
                SearchResult sr = ne.next();
                result.add(new LdapSignature(acct, sr.getNameInNamespace(), sr.getAttributes(), this));
            }
            ne.close();
        } catch (NameNotFoundException e) {
            ZimbraLog.account.warn("caught NameNotFoundException", e);
            return result;
        } catch (InvalidNameException e) {
            ZimbraLog.account.warn("caught InvalidNameException", e);
            return result;
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to lookup signature via query: "+query+ " message: "+e.getMessage(), e);
        } finally {
            if (initZlc == null)
                ZimbraLdapContext.closeContext(zlc);
        }
        return result;
    }

    private Signature getSignatureById(Account acct, LdapEntry entry, String id,  ZimbraLdapContext zlc) throws ServiceException {
        id = LdapUtil.escapeSearchFilterArg(id);
        List<Signature> result = getSignaturesByQuery(acct, entry, LdapFilter.signatureById(id), zlc, null);
        return result.isEmpty() ? null : result.get(0);
    }

    private String getSignatureDn(LdapEntry entry, String name) {
        return A_zimbraSignatureName + "=" + LdapUtil.escapeRDNValue(name) + "," + entry.getDN();
    }

    private void validateSignatureAttrs(Map<String, Object> attrs) throws ServiceException {
        Set<String> validAttrs = AttributeManager.getInstance().getLowerCaseAttrsInClass(AttributeClass.signature);
        for (String key : attrs.keySet()) {
            if (!validAttrs.contains(key.toLowerCase())) {
                throw ServiceException.INVALID_REQUEST("unable to modify attr: "+key, null);
            }
        }
    }

    private void setDefaultSignature(Account acct, String signatureId) throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPrefDefaultSignatureId, signatureId);
        modifyAttrs(acct, attrs);
    }


    private static final String SIGNATURE_LIST_CACHE_KEY = "LdapProvisioning.SIGNATURE_CACHE";

    @Override
    public Signature createSignature(Account account, String signatureName, Map<String, Object> signatureAttrs) throws ServiceException {
        return createSignature(account,  signatureName, signatureAttrs, false);
    }

    @Override
    public Signature restoreSignature(Account account, String signatureName, Map<String, Object> signatureAttrs) throws ServiceException {
        return createSignature(account,  signatureName, signatureAttrs, true);
    }

    private Signature createSignature(Account account, String signatureName, Map<String, Object> signatureAttrs,
            boolean restoring) throws ServiceException {
        signatureName = signatureName.trim();
        removeAttrIgnoreCase("objectclass", signatureAttrs);
        validateSignatureAttrs(signatureAttrs);

        LdapEntry ldapEntry = (LdapEntry) (account instanceof LdapEntry ? account : getAccountById(account.getId()));

        if (ldapEntry == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(account.getName());

        /*
         * check if the signature name already exists
         *
         * We check if the signatureName is the same as the signature on the account.
         * For signatures that are in the signature LDAP entries, JNDI will throw
         * NameAlreadyBoundException for duplicate names.
         *
         */
        Signature acctSig = LdapSignature.getAccountSignature(this, account);
        if (acctSig != null && signatureName.equalsIgnoreCase(acctSig.getName()))
            throw AccountServiceException.SIGNATURE_EXISTS(signatureName);

        boolean setAsDefault = false;
        List<Signature> existing = getAllSignatures(account);
        int numSigs = existing.size();
        if (numSigs >= account.getLongAttr(A_zimbraSignatureMaxNumEntries, 20))
            throw AccountServiceException.TOO_MANY_SIGNATURES();
        else if (numSigs == 0)
            setAsDefault = true;

        account.setCachedData(SIGNATURE_LIST_CACHE_KEY, null);

        Map<Object, Object> attrManagerContext = new HashMap<Object, Object>();
        attrManagerContext.put(MailSignature.CALLBACK_KEY_MAX_SIGNATURE_LEN, account.getAttr(Provisioning.A_zimbraMailSignatureMaxLength, "1024"));
        boolean checkImmutable = !restoring;
        AttributeManager.getInstance().preModify(signatureAttrs, null, attrManagerContext, true, checkImmutable);

        String signatureId = (String)signatureAttrs.get(Provisioning.A_zimbraSignatureId);
        if (signatureId == null) {
            signatureId = LdapUtil.generateUUID();
            signatureAttrs.put(Provisioning.A_zimbraSignatureId, signatureId);
        }

        if (acctSig == null) {
            // the slot on the account is not occupied, use it
            signatureAttrs.put(Provisioning.A_zimbraSignatureName, signatureName);
            // pass in setAsDefault as an optimization, since we are updating the account
            // entry, we can update the default attr in one LDAP write
            LdapSignature.createAccountSignature(this, account, signatureAttrs, setAsDefault);
            return LdapSignature.getAccountSignature(this, account);
        }

        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext(true);

            String dn = getSignatureDn(ldapEntry, signatureName);

            Attributes attrs = new BasicAttributes(true);
            LdapUtil.mapToAttrs(signatureAttrs, attrs);
            LdapUtil.addAttr(attrs, A_objectClass, "zimbraSignature");
            attrs.put(Provisioning.A_zimbraCreateTimestamp, DateUtil.toGeneralizedTime(new Date()));

            zlc.createEntry(dn, attrs, "createSignature");

            Signature signature = getSignatureById(account, ldapEntry, signatureId, zlc);
            AttributeManager.getInstance().postModify(signatureAttrs, signature, attrManagerContext, true);

            if (setAsDefault)
                setDefaultSignature(account, signatureId);

            return signature;
        } catch (NameAlreadyBoundException nabe) {
            throw AccountServiceException.SIGNATURE_EXISTS(signatureName);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }

    @Override
    public void modifySignature(Account account, String signatureId, Map<String, Object> signatureAttrs) throws ServiceException {
        removeAttrIgnoreCase("objectclass", signatureAttrs);

        validateSignatureAttrs(signatureAttrs);

        LdapEntry ldapEntry = (LdapEntry) (account instanceof LdapEntry ? account : getAccountById(account.getId()));

        if (ldapEntry == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(account.getName());

        String newName = (String) signatureAttrs.get(A_zimbraSignatureName);

        if (newName != null) {
            newName = newName.trim();

            // do not allow name to be wiped
            if (newName.length()==0)
                throw ServiceException.INVALID_REQUEST("empty signature name is not allowed", null);

            // check for duplicate names
            List<Signature> sigs = getAllSignatures(account);
            for (Signature sig : sigs) {
                if (sig.getName().equalsIgnoreCase(newName) && !sig.getId().equals(signatureId))
                    throw AccountServiceException.SIGNATURE_EXISTS(newName);
            }
        }

        // clear cache
        account.setCachedData(SIGNATURE_LIST_CACHE_KEY, null);

        if (LdapSignature.isAccountSignature(account, signatureId)) {
            LdapSignature.modifyAccountSignature(this, account, signatureAttrs);
        } else {

            LdapSignature signature = (LdapSignature) getSignatureById(account, ldapEntry, signatureId, null);
            if (signature == null)
                throw AccountServiceException.NO_SUCH_SIGNATURE(signatureId);

            boolean nameChanged = (newName != null && !newName.equals(signature.getName()));

            if (nameChanged)
                signatureAttrs.remove(A_zimbraSignatureName);

            modifyAttrs(signature, signatureAttrs, true);
            if (nameChanged) {
                // the signature cache could've been loaded again if getAllSignatures were called in pre/poseModify callback, so we clear it again
                account.setCachedData(SIGNATURE_LIST_CACHE_KEY, null);
                renameSignature(ldapEntry, signature, newName);
            }

        }
    }

    private void renameSignature(LdapEntry entry, LdapSignature signature, String newSignatureName) throws ServiceException {
        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext(true);
            String newDn = getSignatureDn(entry, newSignatureName);
            zlc.renameEntry(signature.getDN(), newDn);
        } catch (InvalidNameException e) {
            throw ServiceException.INVALID_REQUEST("invalid signature name: "+newSignatureName, e);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to rename signature: "+newSignatureName, e);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }

    @Override
    public void deleteSignature(Account account, String signatureId) throws ServiceException {
        LdapEntry ldapEntry = (LdapEntry) (account instanceof LdapEntry ? account : getAccountById(account.getId()));
        if (ldapEntry == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(account.getName());

        account.setCachedData(SIGNATURE_LIST_CACHE_KEY, null);

        if (LdapSignature.isAccountSignature(account, signatureId)) {
            LdapSignature.deleteAccountSignature(this, account);
            return;
        }

        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext(true);
            Signature signature = getSignatureById(account, ldapEntry, signatureId, zlc);
            if (signature == null)
                throw AccountServiceException.NO_SUCH_SIGNATURE(signatureId);
            String dn = getSignatureDn(ldapEntry, signature.getName());
            zlc.unbindEntry(dn);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to delete signarure: "+signatureId, e);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }

    @Override
    public List<Signature> getAllSignatures(Account account) throws ServiceException {
        LdapEntry ldapEntry = (LdapEntry) (account instanceof LdapEntry ? account : getAccountById(account.getId()));
        if (ldapEntry == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(account.getName());

        @SuppressWarnings("unchecked")
        List<Signature> result = (List<Signature>) account.getCachedData(SIGNATURE_LIST_CACHE_KEY);

        if (result != null) {
            return result;
        }

        result = new ArrayList<Signature>();
        Signature acctSig = LdapSignature.getAccountSignature(this, account);
        if (acctSig != null)
            result.add(acctSig);

        result = getSignaturesByQuery(account, ldapEntry, LdapFilter.allSignatures(), null, result);

        result = Collections.unmodifiableList(result);
        account.setCachedData(SIGNATURE_LIST_CACHE_KEY, result);
        return result;
    }

    @Override
    public Signature get(Account account, SignatureBy keyType, String key) throws ServiceException {
        LdapEntry ldapEntry = (LdapEntry) (account instanceof LdapEntry ? account : getAccountById(account.getId()));
        if (ldapEntry == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(account.getName());

        // this assumes getAllSignatures is cached and number of signatures is reasaonble.
        // might want a per-signature cache (i.e., use "SIGNATURE_BY_ID_"+id as a key, etc)
        switch(keyType) {
            case id:
                for (Signature signature : getAllSignatures(account))
                    if (signature.getId().equals(key)) return signature;
                return null;
            case name:
                for (Signature signature : getAllSignatures(account))
                    if (signature.getName().equalsIgnoreCase(key)) return signature;
                return null;
            default:
                return null;
        }
    }

    private static final String DATA_SOURCE_LIST_CACHE_KEY = "LdapProvisioning.DATA_SOURCE_CACHE";

    private List<DataSource> getDataSourcesByQuery(LdapEntry entry, String query, ZimbraLdapContext initZlc) throws ServiceException {
        ZimbraLdapContext zlc = initZlc;
        List<DataSource> result = new ArrayList<DataSource>();
        try {
            if (zlc == null)
                zlc = new ZimbraLdapContext();
            String base = entry.getDN();
            NamingEnumeration<SearchResult> ne = zlc.searchDir(base, query, sSubtreeSC);
            while(ne.hasMore()) {
                SearchResult sr = ne.next();
                result.add(new LdapDataSource((Account)entry, sr.getNameInNamespace(), sr.getAttributes(), this));
            }
            ne.close();
        } catch (NameNotFoundException e) {
            ZimbraLog.account.warn("caught NameNotFoundException", e);
            return result;
        } catch (InvalidNameException e) {
            ZimbraLog.account.warn("caught InvalidNameException", e);
            return result;
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to lookup data source via query: "+query+ " message: "+e.getMessage(), e);
        } finally {
            if (initZlc == null)
                ZimbraLdapContext.closeContext(zlc);
        }
        return result;
    }

    private DataSource getDataSourceById(LdapEntry entry, String id,  ZimbraLdapContext zlc) throws ServiceException {
        id= LdapUtil.escapeSearchFilterArg(id);
        List<DataSource> result = getDataSourcesByQuery(entry, LdapFilter.dataSourceById(id), zlc);
        return result.isEmpty() ? null : result.get(0);
    }

    private String getDataSourceDn(LdapEntry entry, String name) {
        return A_zimbraDataSourceName + "=" + LdapUtil.escapeRDNValue(name) + "," + entry.getDN();
    }

    protected ReplaceAddressResult replaceMailAddresses(Entry entry, String attrName, String oldAddr, String newAddr) throws ServiceException {
        String oldDomain = EmailUtil.getValidDomainPart(oldAddr);
        String newDomain = EmailUtil.getValidDomainPart(newAddr);

        String oldAddrs[] = entry.getMultiAttr(attrName);
        String newAddrs[] = new String[0];

        for (int i = 0; i < oldAddrs.length; i++) {
            String oldMail = oldAddrs[i];
            if (oldMail.equals(oldAddr)) {
                // exact match, replace the entire old addr with new addr
                newAddrs = addMultiValue(newAddrs, newAddr);
            } else {
                String[] oldParts = EmailUtil.getLocalPartAndDomain(oldMail);

                // sanity check, or should we ignore and continue?
                if (oldParts == null)
                    throw ServiceException.FAILURE("bad value for " + attrName + " " + oldMail, null);
                String oldL = oldParts[0];
                String oldD = oldParts[1];

                if (oldD.equals(oldDomain)) {
                    // old domain is the same as the domain being renamed,
                    //   - keep the local part
                    //   - replace the domain with new domain
                    String newMail = oldL + "@" + newDomain;
                    newAddrs = addMultiValue(newAddrs, newMail);
                } else {
                    // address is not in the domain being renamed, keep as is
                    newAddrs = addMultiValue(newAddrs, oldMail);
                }
            }
        }

        // returns a pair of parallel arrays of old and new addrs
        return new ReplaceAddressResult(oldAddrs, newAddrs);
     }

    protected boolean addressExists(ZimbraLdapContext zlc, String baseDN, String[] addrs) throws ServiceException {
        StringBuilder query = new StringBuilder();
        query.append("(|");
        for (int i=0; i < addrs.length; i++) {
            query.append(String.format("(%s=%s)", Provisioning.A_zimbraMailDeliveryAddress, addrs[i]));
            query.append(String.format("(%s=%s)", Provisioning.A_zimbraMailAlias, addrs[i]));
        }
        query.append(")");

        try {
            NamingEnumeration<SearchResult> ne = zlc.searchDir(baseDN, query.toString(), sSubtreeSC);
            if (ne.hasMore())
                return true;
            else
                return false;
        } catch (NameNotFoundException e) {
            return false;
        } catch (InvalidNameException e) {
            throw ServiceException.FAILURE("unable to lookup account via query: "+query.toString()+" message: "+e.getMessage(), e);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to lookup account via query: "+query.toString()+" message: "+e.getMessage(), e);
        } finally {
        }
    }

    // MOVE OVER ALL aliases. doesn't throw an exception, just logs
    // There could be a race condition that the alias might get taken
    // in the new domain post the check.  Anyone who calls this API must
    // pay attention to the warning message
    private void moveAliases(ZimbraLdapContext zlc, ReplaceAddressResult addrs, String newDomain, String primaryUid,
                             String targetOldDn, String targetNewDn,
                             String targetOldDomain, String targetNewDomain)  throws ServiceException {

        for (int i=0; i < addrs.newAddrs().length; i++) {
            String oldAddr = addrs.oldAddrs()[i];
            String newAddr = addrs.newAddrs()[i];

            String aliasNewDomain = EmailUtil.getValidDomainPart(newAddr);

            if (aliasNewDomain.equals(newDomain)) {
                String[] oldParts = EmailUtil.getLocalPartAndDomain(oldAddr);
                String oldAliasDN = mDIT.aliasDN(targetOldDn, targetOldDomain, oldParts[0], oldParts[1]);
                String newAliasDN = mDIT.aliasDNRename(targetNewDn, targetNewDomain, newAddr);

                if (oldAliasDN.equals(newAliasDN))
                    continue;

                // skip the extra alias that is the same as the primary
                String newAliasParts[] = EmailUtil.getLocalPartAndDomain(newAddr);
                String newAliasLocal = newAliasParts[0];
                if (!(primaryUid != null && newAliasLocal.equals(primaryUid))) {
                    try {
                        zlc.renameEntry(oldAliasDN, newAliasDN);
                    } catch (NameAlreadyBoundException nabe) {
                        ZimbraLog.account.warn("unable to move alias from " + oldAliasDN + " to " + newAliasDN, nabe);
                    } catch (NamingException ne) {
                        throw ServiceException.FAILURE("unable to move aliases", null);
                    } finally {
                    }
                }
             }
        }
    }


    @Override
    public DataSource createDataSource(Account account, DataSource.Type dsType, String dsName, Map<String, Object> dataSourceAttrs) throws ServiceException {
        return createDataSource(account, dsType, dsName, dataSourceAttrs, false);
    }

    @Override
    public DataSource createDataSource(Account account, DataSource.Type type, String dataSourceName, Map<String, Object> attrs,
            boolean passwdAlreadyEncrypted) throws ServiceException {
        return createDataSource(account, type, dataSourceName, attrs, passwdAlreadyEncrypted, false);
    }

    @Override
    public DataSource restoreDataSource(Account account, DataSource.Type dsType, String dsName, Map<String, Object> dataSourceAttrs) throws ServiceException {
        return createDataSource(account, dsType, dsName, dataSourceAttrs, true, true);
    }

    private DataSource createDataSource(Account account, DataSource.Type dsType, String dsName, Map<String, Object> dataSourceAttrs,
            boolean passwdAlreadyEncrypted, boolean restoring) throws ServiceException {
        removeAttrIgnoreCase("objectclass", dataSourceAttrs);
        LdapEntry ldapEntry = (LdapEntry) (account instanceof LdapEntry ? account : getAccountById(account.getId()));

        if (ldapEntry == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(account.getName());

        List<DataSource> existing = getAllDataSources(account);
        if (existing.size() >= account.getLongAttr(A_zimbraDataSourceMaxNumEntries, 20))
            throw AccountServiceException.TOO_MANY_DATA_SOURCES();

        dataSourceAttrs.put(A_zimbraDataSourceName, dsName); // must be the same
        dataSourceAttrs.put(Provisioning.A_zimbraDataSourceType, dsType.toString());

        account.setCachedData(DATA_SOURCE_LIST_CACHE_KEY, null);

        Map<?, ?> attrManagerContext = new HashMap<Object, Object>();
        boolean checkImmutable = !restoring;
        AttributeManager.getInstance().preModify(dataSourceAttrs, null, attrManagerContext, true, checkImmutable);

        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext(true);

            String dn = getDataSourceDn(ldapEntry, dsName);

            Attributes attrs = new BasicAttributes(true);
            LdapUtil.mapToAttrs(dataSourceAttrs, attrs);
            Attribute oc = LdapUtil.addAttr(attrs, A_objectClass, "zimbraDataSource");
            String extraOc = LdapDataSource.getObjectClass(dsType);
            if (extraOc != null)
                oc.add(extraOc);

            String dsId = LdapUtil.getAttrString(attrs, A_zimbraDataSourceId);
            if (dsId == null) {
                dsId = LdapUtil.generateUUID();
                attrs.put(A_zimbraDataSourceId, dsId);
            }

            String password = LdapUtil.getAttrString(attrs, A_zimbraDataSourcePassword);
            if (password != null) {
                String encrypted = passwdAlreadyEncrypted ? password : DataSource.encryptData(dsId, password);
                attrs.put(A_zimbraDataSourcePassword, encrypted);
            }
            attrs.put(Provisioning.A_zimbraCreateTimestamp, DateUtil.toGeneralizedTime(new Date()));

            zlc.createEntry(dn, attrs, "createDataSource");

            DataSource ds = getDataSourceById(ldapEntry, dsId, zlc);
            AttributeManager.getInstance().postModify(dataSourceAttrs, ds, attrManagerContext, true);
            return ds;
        } catch (NameAlreadyBoundException nabe) {
            throw AccountServiceException.DATA_SOURCE_EXISTS(dsName);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to create data source", e);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }

    @Override
    public void deleteDataSource(Account account, String dataSourceId) throws ServiceException {
        LdapEntry ldapEntry = (LdapEntry) (account instanceof LdapEntry ? account : getAccountById(account.getId()));
        if (ldapEntry == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(account.getName());

        account.setCachedData(DATA_SOURCE_LIST_CACHE_KEY, null);

        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext(true);
            DataSource dataSource = getDataSourceById(ldapEntry, dataSourceId, zlc);
            if (dataSource == null)
                throw AccountServiceException.NO_SUCH_DATA_SOURCE(dataSourceId);
            String dn = getDataSourceDn(ldapEntry, dataSource.getName());
            zlc.unbindEntry(dn);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to delete data source: "+dataSourceId, e);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }


    @Override
    public List<DataSource> getAllDataSources(Account account) throws ServiceException {

        @SuppressWarnings("unchecked")
        List<DataSource> result = (List<DataSource>) account.getCachedData(DATA_SOURCE_LIST_CACHE_KEY);

        if (result != null) {
            return result;
        }

        LdapEntry ldapEntry = (LdapEntry) (account instanceof LdapEntry ? account : getAccountById(account.getId()));
        if (ldapEntry == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(account.getName());
        result = getDataSourcesByQuery(ldapEntry, LdapFilter.allDataSources(), null);
        result = Collections.unmodifiableList(result);
        account.setCachedData(DATA_SOURCE_LIST_CACHE_KEY, result);
        return result;
    }

    public void removeAttrIgnoreCase(String attr, Map<String, Object> attrs) {
        for (String key : attrs.keySet()) {
            if (key.equalsIgnoreCase(attr)) {
                attrs.remove(key);
                return;
            }
        }
    }

    @Override
    public void modifyDataSource(Account account, String dataSourceId, Map<String, Object> attrs) throws ServiceException {
        removeAttrIgnoreCase("objectclass", attrs);
        LdapEntry ldapEntry = (LdapEntry) (account instanceof LdapEntry ? account : getAccountById(account.getId()));
        if (ldapEntry == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(account.getName());

        LdapDataSource ds = (LdapDataSource) getDataSourceById(ldapEntry, dataSourceId, null);
        if (ds == null)
            throw AccountServiceException.NO_SUCH_DATA_SOURCE(dataSourceId);

        account.setCachedData(DATA_SOURCE_LIST_CACHE_KEY, null);

        attrs.remove(A_zimbraDataSourceId);

        String name = (String) attrs.get(A_zimbraDataSourceName);
        boolean newName = (name != null && !name.equals(ds.getName()));
        if (newName) attrs.remove(A_zimbraDataSourceName);

        String password = (String) attrs.get(A_zimbraDataSourcePassword);
        if (password != null) {
            attrs.put(A_zimbraDataSourcePassword, DataSource.encryptData(ds.getId(), password));
        }

        modifyAttrs(ds, attrs, true);
        if (newName) {
            // the datasoruce cache could've been loaded again if getAllDataSources were called in pre/poseModify callback, so we clear it again
            account.setCachedData(DATA_SOURCE_LIST_CACHE_KEY, null);
            ZimbraLdapContext zlc = null;
            try {
                zlc = new ZimbraLdapContext(true);
                String newDn = getDataSourceDn(ldapEntry, name);
                zlc.renameEntry(ds.getDN(), newDn);
            } catch (NamingException e) {
                throw ServiceException.FAILURE("unable to rename datasource: "+name, e);
            } finally {
                ZimbraLdapContext.closeContext(zlc);
            }
        }
    }

    @Override
    public DataSource get(Account account, DataSourceBy keyType, String key) throws ServiceException {
        LdapEntry ldapEntry = (LdapEntry) (account instanceof LdapEntry ? account : getAccountById(account.getId()));
        if (ldapEntry == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(account.getName());

        // this assumes getAllDataSources is cached and number of data sources is reasaonble.
        // might want a per-data-source cache (i.e., use "DATA_SOURCE_BY_ID_"+id as a key, etc)

        switch(keyType) {
            case id:
                for (DataSource source : getAllDataSources(account))
                    if (source.getId().equals(key))
                        return source;
                return null;
                //return getDataSourceById(ldapEntry, key, null);
            case name:
                for (DataSource source : getAllDataSources(account))
                    if (source.getName().equalsIgnoreCase(key))
                        return source;
                return null;
                //return getDataSourceByName(ldapEntry, key, null);
            default:
                return null;
        }
    }

    private XMPPComponent getXMPPComponentByQuery(String query, ZimbraLdapContext initZlc) throws ServiceException {
        ZimbraLdapContext zlc = initZlc;
        try {
            if (zlc == null)
                zlc = new ZimbraLdapContext();
            NamingEnumeration<SearchResult> ne = zlc.searchDir(mDIT.xmppcomponentBaseDN(), query, sSubtreeSC);
            if (ne.hasMore()) {
                SearchResult sr = ne.next();
                ne.close();
                return new LdapXMPPComponent(sr.getNameInNamespace(), sr.getAttributes(), this);
            }
        } catch (NameNotFoundException e) {
            return null;
        } catch (InvalidNameException e) {
            return null;
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to lookup XMPPComponent via query: "+query+" message: "+e.getMessage(), e);
        } finally {
            if (initZlc == null)
                ZimbraLdapContext.closeContext(zlc);
        }
        return null;
    }

    private XMPPComponent getXMPPComponentByName(String name, boolean nocache) throws ServiceException {
        if (!nocache) {
            XMPPComponent x = sXMPPComponentCache.getByName(name);
            if (x != null)
                return x;
        }
        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext();
            String dn = mDIT.xmppcomponentNameToDN(name);
            Attributes attrs = zlc.getAttributes(dn);
            XMPPComponent x = new LdapXMPPComponent(dn, attrs, this);
            sXMPPComponentCache.put(x);
            return x;
        } catch (NameNotFoundException e) {
            return null;
        } catch (InvalidNameException e) {
            return null;
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to lookup xmpp component by name: "+name+" message: "+e.getMessage(), e);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }

    private XMPPComponent getXMPPComponentById(String zimbraId, ZimbraLdapContext zlc, boolean nocache) throws ServiceException {
        if (zimbraId == null)
            return null;
        XMPPComponent x = null;
        if (!nocache)
            x = sXMPPComponentCache.getById(zimbraId);
        if (x == null) {
            zimbraId = LdapUtil.escapeSearchFilterArg(zimbraId);
            x = getXMPPComponentByQuery(LdapFilter.xmppComponentById(zimbraId), zlc);
            sXMPPComponentCache.put(x);
        }
        return x;
    }

    @Override
    public List<XMPPComponent> getAllXMPPComponents() throws ServiceException {
        List<XMPPComponent> result = new ArrayList<XMPPComponent>();
        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext();
            String filter;
            filter = LdapFilter.allXMPPComponents();

            NamingEnumeration<SearchResult> ne = zlc.searchDir(mDIT.xmppcomponentBaseDN(), filter, sSubtreeSC);
            while (ne.hasMore()) {
                SearchResult sr = ne.next();
                LdapXMPPComponent x = new LdapXMPPComponent(sr.getNameInNamespace(), sr.getAttributes(), this);
                result.add(x);
            }
            ne.close();
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to list all servers", e);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
        if (result.size() > 0)
            sXMPPComponentCache.put(result, true);
        Collections.sort(result);
        return result;
    }

    @Override
    public XMPPComponent createXMPPComponent(String name, Domain domain, Server server, Map<String, Object> inAttrs) throws ServiceException {
        name = name.toLowerCase().trim();

        // sanity checking
        removeAttrIgnoreCase("objectclass", inAttrs);
        removeAttrIgnoreCase(A_zimbraDomainId, inAttrs);
        removeAttrIgnoreCase(A_zimbraServerId, inAttrs);

        Map<?, ?> attrManagerContext = new HashMap<Object, Object>();
        AttributeManager.getInstance().preModify(inAttrs, null, attrManagerContext, true, true);

        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext(true);

            Attributes attrs = new BasicAttributes(true);
            LdapUtil.mapToAttrs(inAttrs, attrs);
            LdapUtil.addAttr(attrs, A_objectClass, "zimbraXMPPComponent");

            String compId = LdapUtil.generateUUID();
            attrs.put(A_zimbraId, compId);
            attrs.put(A_zimbraCreateTimestamp, DateUtil.toGeneralizedTime(new Date()));
            attrs.put(A_cn, name);
            String dn = mDIT.xmppcomponentNameToDN(name);

            attrs.put(A_zimbraDomainId, domain.getId());
            attrs.put(A_zimbraServerId, server.getId());

            zlc.createEntry(dn, attrs, "createXMPPComponent");

            XMPPComponent comp = getXMPPComponentById(compId, zlc, true);
            AttributeManager.getInstance().postModify(inAttrs, comp, attrManagerContext, true);
            return comp;
        } catch (NameAlreadyBoundException nabe) {
            throw AccountServiceException.IM_COMPONENT_EXISTS(name);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }

    @Override
    public XMPPComponent get(XMPPComponentBy keyType, String key) throws ServiceException {
        switch(keyType) {
            case name:
                return getXMPPComponentByName(key, false);
            case id:
                return getXMPPComponentById(key, null, false);
            case serviceHostname:
                throw new UnsupportedOperationException("Writeme!");
            default:
        }
        return null;
    }

    @Override
    public void deleteXMPPComponent(XMPPComponent comp) throws ServiceException {
        String zimbraId = comp.getId();
        ZimbraLdapContext zlc = null;
        LdapXMPPComponent l = (LdapXMPPComponent)get(XMPPComponentBy.id, zimbraId);
        try {
            zlc = new ZimbraLdapContext(true);
            zlc.unbindEntry(l.getDN());
            sXMPPComponentCache.remove(l);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to purge XMPPComponent : "+zimbraId, e);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }

    // Only called from renameDomain for now
    void renameXMPPComponent(String zimbraId, String newName) throws ServiceException {
        LdapXMPPComponent comp = (LdapXMPPComponent)get(XMPPComponentBy.id, zimbraId);
        if (comp == null)
            throw AccountServiceException.NO_SUCH_XMPP_COMPONENT(zimbraId);

        newName = newName.toLowerCase().trim();
        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext(true);
            String newDn = mDIT.xmppcomponentNameToDN(newName);
            zlc.renameEntry(comp.getDN(), newDn);
            // remove old comp from cache
            sXMPPComponentCache.remove(comp);
        } catch (NameAlreadyBoundException nabe) {
            throw AccountServiceException.IM_COMPONENT_EXISTS(newName);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to rename XMPPComponent: "+zimbraId, e);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }


    //
    // rights
    //

    @Override
    /*
     * from zmprov -l, we don't expand all attrs, expandAllAttrs is ignored
     */
    public Right getRight(String rightName, boolean expandAllAttrs) throws ServiceException {
        if (expandAllAttrs)
            throw ServiceException.FAILURE("expandAllAttrs == TRUE is not supported", null);
        return RightCommand.getRight(rightName);
    }

    @Override
    /*
     * from zmprov -l, we don't expand all attrs, expandAllAttrs is ignored
     */
    public List<Right> getAllRights(String targetType, boolean expandAllAttrs) throws ServiceException {
        if (expandAllAttrs)
            throw ServiceException.FAILURE("expandAllAttrs == TRUE is not supported", null);
        return RightCommand.getAllRights(targetType);
    }

    @Override
    public boolean checkRight(String targetType, TargetBy targetBy, String target,
                              GranteeBy granteeBy, String grantee,
                              String right, Map<String, Object> attrs,
                              AccessManager.ViaGrant via) throws ServiceException {
        GuestAccount guest = null;

        try {
            GranteeType.lookupGrantee(this, GranteeType.GT_USER, granteeBy, grantee);
        } catch (ServiceException e) {
            guest = new GuestAccount(grantee, null);
        }

        return RightCommand.checkRight(this,
                                       targetType, targetBy, target,
                                       granteeBy, grantee, guest,
                                       right, attrs, via);
    }

    @Override
    public RightCommand.AllEffectiveRights getAllEffectiveRights(
            String granteeType, GranteeBy granteeBy, String grantee,
            boolean expandSetAttrs, boolean expandGetAttrs) throws ServiceException {
        return RightCommand.getAllEffectiveRights(this,
                                                  granteeType, granteeBy, grantee,
                                                  expandSetAttrs, expandGetAttrs);
    }

    @Override
    public RightCommand.EffectiveRights getEffectiveRights(
            String targetType, TargetBy targetBy, String target,
            GranteeBy granteeBy, String grantee,
            boolean expandSetAttrs, boolean expandGetAttrs) throws ServiceException {
        return RightCommand.getEffectiveRights(this,
                                               targetType, targetBy, target,
                                               granteeBy, grantee,
                                               expandSetAttrs, expandGetAttrs);
    }

    @Override
    public EffectiveRights getCreateObjectAttrs(String targetType,
                                                DomainBy domainBy, String domainStr,
                                                CosBy cosBy, String cosStr,
                                                GranteeBy granteeBy, String grantee) throws ServiceException {
        return RightCommand.getCreateObjectAttrs(this,
                                                 targetType,
                                                 domainBy, domainStr,
                                                 cosBy, cosStr,
                                                 granteeBy, grantee);
    }

    @Override
    public RightCommand.Grants getGrants(String targetType, TargetBy targetBy, String target,
            String granteeType, GranteeBy granteeBy, String grantee,
            boolean granteeIncludeGroupsGranteeBelongs) throws ServiceException {
        return RightCommand.getGrants(this, targetType, targetBy, target,
                granteeType, granteeBy, grantee, granteeIncludeGroupsGranteeBelongs);
    }

    @Override
    public void grantRight(String targetType, TargetBy targetBy, String target,
                           String granteeType, GranteeBy granteeBy, String grantee, String secret,
                           String right, RightModifier rightModifier) throws ServiceException {
        RightCommand.grantRight(this,
                                null,
                                targetType, targetBy, target,
                                granteeType, granteeBy, grantee, secret,
                                right, rightModifier);
    }

    @Override
    public void revokeRight(String targetType, TargetBy targetBy, String target,
                            String granteeType, GranteeBy granteeBy, String grantee,
                            String right, RightModifier rightModifier) throws ServiceException {
         RightCommand.revokeRight(this,
                                  null,
                                  targetType, targetBy, target,
                                  granteeType, granteeBy, grantee,
                                  right, rightModifier);
    }

    public String getNamingRdnAttr(Entry entry) throws ServiceException {
        return mDIT.getNamingRdnAttr(entry);
    }

    /*
     * only called from TestProvisioning for unittest
     */
    public String getDN(Entry entry) throws ServiceException {
        if (entry instanceof LdapMimeType)
            return ((LdapMimeType)entry).getDN();
        else if (entry instanceof LdapCalendarResource)
            return ((LdapCalendarResource)entry).getDN();
        else if (entry instanceof LdapAccount)
            return ((LdapAccount)entry).getDN();
        else if (entry instanceof LdapAlias)
            return ((LdapAlias)entry).getDN();
        else if (entry instanceof LdapCos)
            return ((LdapCos)entry).getDN();
        else if (entry instanceof LdapDataSource)
            return ((LdapDataSource)entry).getDN();
        else if (entry instanceof LdapDistributionList)
            return ((LdapDistributionList)entry).getDN();
        else if (entry instanceof LdapDomain)
            return ((LdapDomain)entry).getDN();
        else if (entry instanceof LdapIdentity)
            return ((LdapIdentity)entry).getDN();
        else if (entry instanceof LdapSignature)
            return ((LdapSignature)entry).getDN();
        else if (entry instanceof LdapServer)
            return ((LdapServer)entry).getDN();
        else if (entry instanceof LdapZimlet)
            return ((LdapZimlet)entry).getDN();
        else
            throw ServiceException.FAILURE("not a ldap entry", null);
    }

    @Override
    public void flushCache(CacheEntryType type, CacheEntry[] entries) throws ServiceException {

        switch (type) {
        case account:
            if (entries != null) {
                for (CacheEntry entry : entries) {
                    AccountBy accountBy = (entry.mEntryBy==CacheEntryBy.id)? AccountBy.id : AccountBy.name;
                    Account account = getFromCache(accountBy, entry.mEntryIdentity);
                    /*
                     * We now call removeFromCache instead of reload for flushing an account
                     * from cache.   This change was originally for bug 25028, but that would still
                     * need an extrat step to flush cache after the account's zimbraCOSId changed.
                     * (if we call reload insteasd of removeFromCache, flush cache of the account would
                     * not clear the mDefaults for inherited attrs, that was the bug.)
                     * Bug 25028 is now taken care of by the callback.  We still call removeFromCache
                     * for flushing account cache, because that does a cleaner flush.
                     *
                     * Note, we only call removeFromCache for account.
                     * We should *NOT* do removeFromCache when flushing global config and cos caches.
                     *
                     * Because the "mDefaults" Map(contains a ref to the old instance of COS.mAccountDefaults for
                     * all the accountInherited COS attrs) stored in all the cached accounts.   Same for the
                     * inherited attrs of server/domain from global config.  If we do removeFromCache for flushing
                     * cos/global config, then after FlushCache(cos) if you do a ga on a cached account, it still
                     * shows the old COS value for values that are inherited from COS.   Although, for newly loaded
                     * accounts or when a cached account is going thru auth(that'll trigger a reload) they will get
                     * the new COS values(refreshed as a result of FlushCache(cos)).
                     */
                    if (account != null)
                        removeFromCache(account);
                }
            } else
                sAccountCache.clear();
            return;
        case group:
            if (entries != null) {
                for (CacheEntry entry : entries) {
                    DistributionListBy dlBy = (entry.mEntryBy==CacheEntryBy.id)? DistributionListBy.id : DistributionListBy.name;
                    removeGroupFromCache(dlBy, entry.mEntryIdentity);
                }
            } else {
                sAclGroupCache.clear();
                sDLCache.clear();
            }
            return;
        case config:
            if (entries != null)
                throw ServiceException.INVALID_REQUEST("cannot specify entry for flushing global config", null);
            Config config = getConfig();
            reload(config, false);
            return;
        case cos:
            if (entries != null) {
                for (CacheEntry entry : entries) {
                    CosBy cosBy = (entry.mEntryBy==CacheEntryBy.id)? CosBy.id : CosBy.name;
                    Cos cos = getFromCache(cosBy, entry.mEntryIdentity);
                    if (cos != null)
                        reload(cos, false);
                }
            } else
                sCosCache.clear();
            return;
        case domain:
            if (entries != null) {
                for (CacheEntry entry : entries) {
                    DomainBy domainBy = (entry.mEntryBy==CacheEntryBy.id)? DomainBy.id : DomainBy.name;
                    Domain domain = getFromCache(domainBy, entry.mEntryIdentity, GetFromDomainCacheOption.BOTH);
                    if (domain != null) {
                        if (domain instanceof DomainCache.NonExistingDomain)
                            sDomainCache.removeFromNegativeCache(domainBy, entry.mEntryIdentity);
                        else
                            reload(domain, false);
                    }
                }
            } else
                sDomainCache.clear();
            return;
        case server:
            if (entries != null) {
                for (CacheEntry entry : entries) {
                    ServerBy serverBy = (entry.mEntryBy==CacheEntryBy.id)? ServerBy.id : ServerBy.name;
                    Server server = get(serverBy, entry.mEntryIdentity);
                    if (server != null)
                        reload(server, false);
                }
            } else
                sServerCache.clear();
            return;
        case zimlet:
            if (entries != null) {
                for (CacheEntry entry : entries) {
                    ZimletBy zimletBy = (entry.mEntryBy==CacheEntryBy.id)? ZimletBy.id : ZimletBy.name;
                    Zimlet zimlet = getFromCache(zimletBy, entry.mEntryIdentity);
                    if (zimlet != null)
                        reload(zimlet, false);
                }
            } else
                sZimletCache.clear();
            return;
        default:
            throw ServiceException.INVALID_REQUEST("invalid cache type "+type, null);
        }

    }

    public void removeFromCache(Entry entry) {
        if (entry instanceof Account)
            sAccountCache.remove((Account)entry);
        else if (entry instanceof DistributionList) {
            sAclGroupCache.remove((DistributionList)entry);
            sDLCache.remove((DistributionList)entry);
        } else
            throw new UnsupportedOperationException();
    }

    private static class CountAccountVisitor implements NamedEntry.Visitor {

        private static class Result {
            Result(String name) {
                mName = name;
                mCount= 0;
            }
            String mName;
            long mCount;
        }

        Provisioning mProv;
        Map<String, Result> mResult = new HashMap<String, Result>();

        CountAccountVisitor(Provisioning prov) {
            mProv = prov;
        }

        @Override
        public void visit(NamedEntry entry) throws ServiceException {
            if (!(entry instanceof Account))
                return;

            if (entry instanceof CalendarResource)
                return;

            Account acct = (Account)entry;
            if (acct.getBooleanAttr(Provisioning.A_zimbraIsSystemResource, false))
                return;

            Cos cos = mProv.getCOS(acct);
            Result r = mResult.get(cos.getId());
            if (r == null) {
                r = new Result(cos.getName());
                mResult.put(cos.getId(), r);
            }
            r.mCount++;
        }

        CountAccountResult getResult() {
            CountAccountResult result = new CountAccountResult();
            for (Map.Entry<String, Result> r : mResult.entrySet()) {
                result.addCountAccountByCosResult(r.getKey(), r.getValue().mName, r.getValue().mCount);
            }
            return result;
        }
    }

    @Override
    public CountAccountResult countAccount(Domain domain) throws ServiceException {
        CountAccountVisitor visitor = new CountAccountVisitor(this);
        // getAllAccounts(domain, visitor);
        searchObjects(mDIT.filterAccountsByDomain(domain, false),
                      new String[]{Provisioning.A_zimbraCOSId, Provisioning.A_zimbraIsSystemResource},
                      mDIT.domainDNToAccountSearchDN(((LdapDomain)domain).getDN()),
                      Provisioning.SA_ACCOUNT_FLAG,
                      visitor,
                      0);

        return visitor.getResult();
    }



    @Override
    public long countObjects(CountObjectsType type, Domain domain) throws ServiceException {

        String[] bases = null;
        String query = null;
        String[] attrs = null;

        // figure out bases, query, and attrs for each supported counting type
        switch (type) {
        case userAccounts:

            if (domain instanceof LdapDomain) {
                String b = mDIT.domainDNToAccountSearchDN(((LdapDomain)domain).getDN());
                bases = new String[]{b};
            } else
                bases = getSearchBases(Provisioning.SA_ACCOUNT_FLAG);

            query = LdapFilter.allNonSystemAccounts();
            attrs = new String[] {"zimbraId"};
            break;
        default:
            throw ServiceException.INVALID_REQUEST("unsupported counting type:" + type.toString(), null);
        }

        long num = 0;
        for (String base : bases) {
            num += countObjects(base, query, attrs);
        }

        return num;
    }

    private long countObjects(String base, String query, String[] attrs) throws ServiceException {
        long num = 0;

        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext();

            SearchControls searchControls =
                new SearchControls(SearchControls.SUBTREE_SCOPE, 0, 0, attrs, false, false);

            NamingEnumeration<SearchResult> ne = null;

            //Set the page size and initialize the cookie that we pass back in subsequent pages
            int maxResults = 0; // no limit
            int pageSize = LdapUtil.adjustPageSize(maxResults, 1000);
            byte[] cookie = null;

            try {
                do {
                    zlc.setPagedControl(pageSize, cookie, true);
                    ne = zlc.searchDir(base, query, searchControls);

                    while (ne != null && ne.hasMore()) {
                        ne.nextElement();
                        num++;
                    }
                    cookie = zlc.getCookie();
                } while (cookie != null);

            } finally {
                if (ne != null) ne.close();
            }
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to count users", e);
        } catch (IOException e) {
            throw ServiceException.FAILURE("unable to count users", e);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }

        return num;
    }

    @Override
    public Map<String, String> getNamesForIds(Set<String> ids, EntryType type) throws ServiceException {
        final Map<String, String> result = new HashMap<String, String>();
        Set<String> unresolvedIds;

        NamedEntry entry;
        final String nameAttr;
        final EntryType entryType = type;
        String base;
        String objectClass;

        switch (entryType) {
        case account:
            unresolvedIds = new HashSet<String>();
            for (String id : ids) {
                entry = sAccountCache.getById(id);
                if (entry != null)
                    result.put(id, entry.getName());
                else
                    unresolvedIds.add(id);
            }
            nameAttr = Provisioning.A_zimbraMailDeliveryAddress;
            base = mDIT.mailBranchBaseDN();
            objectClass = C_zimbraAccount;
            break;
        case group:
            unresolvedIds = ids;
            nameAttr = Provisioning.A_uid; // see dnToEmail
            base = mDIT.mailBranchBaseDN();
            objectClass = C_zimbraMailList;
            break;
        case cos:
            unresolvedIds = new HashSet<String>();
            for (String id : ids) {
                entry = sCosCache.getById(id);
                if (entry != null)
                    result.put(id, entry.getName());
                else
                    unresolvedIds.add(id);
            }
            nameAttr = Provisioning.A_cn;
            base = mDIT.cosBaseDN();
            objectClass = C_zimbraCOS;
            break;
        case domain:
            unresolvedIds = new HashSet<String>();
            for (String id : ids) {
                entry = getFromCache(DomainBy.id, id, GetFromDomainCacheOption.POSITIVE);
                if (entry != null)
                    result.put(id, entry.getName());
                else
                    unresolvedIds.add(id);
            }
            nameAttr = Provisioning.A_zimbraDomainName;
            base = mDIT.domainBaseDN();
            objectClass = C_zimbraDomain;
            break;
        default:
            throw ServiceException.FAILURE("unsupported entry type for getNamesForIds" + type.name(), null);
        }

        // we are done if all ids can be resolved in our cache
        if (unresolvedIds.size() == 0)
            return result;

        LdapUtil.SearchLdapVisitor visitor = new LdapUtil.SearchLdapVisitor() {
            @Override
            public void visit(String dn, Map<String, Object> attrs, Attributes ldapAttrs) {
                String id = (String)attrs.get(Provisioning.A_zimbraId);

                String name = null;
                try {
                    switch (entryType) {
                    case account:
                        name = LdapUtil.getAttrString(ldapAttrs, Provisioning.A_zimbraMailDeliveryAddress);
                        if (name == null)
                            name = mDIT.dnToEmail(dn, ldapAttrs);
                        break;
                    case group:
                        name = mDIT.dnToEmail(dn, ldapAttrs);
                        break;
                    case cos:
                        name = LdapUtil.getAttrString(ldapAttrs, Provisioning.A_cn);
                        break;
                    case domain:
                        name = LdapUtil.getAttrString(ldapAttrs, Provisioning.A_zimbraDomainName);
                        break;
                    }
                } catch (ServiceException e) {
                    name = null;
                } catch (NamingException e) {
                    name = null;
                }

                if (name != null)
                    result.put(id, name);
            }
        };

        String returnAttrs[] = new String[] {Provisioning.A_zimbraId, nameAttr};
        searchNamesForIds(unresolvedIds, base, objectClass, returnAttrs, visitor);

        return result;
    }

    public void searchNamesForIds(Set<String> unresolvedIds, String base, String objectClass, String returnAttrs[],
            LdapUtil.SearchLdapVisitor visitor) throws ServiceException {

        final int batchSize = 10;  // num ids per search
        final String queryStart = "(&(objectClass=" + objectClass + ")(";
        final String queryEnd = "))";

        StringBuilder query = new StringBuilder();
        query.append(queryStart);

        int i = 0;
        for (String id : unresolvedIds) {
            query.append("|(" + Provisioning.A_zimbraId + "=" + id + ")");
            if ((++i) % batchSize == 0) {
                query.append(queryEnd);
                LdapUtil.searchLdapOnReplica(base, query.toString(), returnAttrs, visitor);
                query.setLength(0);
                query.append(queryStart);
            }
        }

        // one more search if there are remainding
        if (query.length() != queryStart.length()) {
            query.append(queryEnd);
            LdapUtil.searchLdapOnReplica(base, query.toString(), returnAttrs, visitor);
        }
    }

    public static void testAuthDN() {
        System.out.println(LdapUtil.computeAuthDn("schemers@example.zimbra.com", null));
        System.out.println(LdapUtil.computeAuthDn("schemers@example.zimbra.com", ""));
        System.out.println(LdapUtil.computeAuthDn("schemers@example.zimbra.com", "WTF"));
        System.out.println(LdapUtil.computeAuthDn("schemers@example.zimbra.com", "%n"));
        System.out.println(LdapUtil.computeAuthDn("schemers@example.zimbra.com", "%u"));
        System.out.println(LdapUtil.computeAuthDn("schemers@example.zimbra.com", "%d"));
        System.out.println(LdapUtil.computeAuthDn("schemers@example.zimbra.com", "%D"));
        System.out.println(LdapUtil.computeAuthDn("schemers@example.zimbra.com", "uid=%u,ou=people,%D"));
        System.out.println(LdapUtil.computeAuthDn("schemers@example.zimbra.com", "n(%n)u(%u)d(%d)D(%D)(%%)"));
    }

}
