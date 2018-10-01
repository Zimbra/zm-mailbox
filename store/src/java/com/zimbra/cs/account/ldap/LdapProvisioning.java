/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

package com.zimbra.cs.account.ldap;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zimbra.common.account.ForgetPasswordEnums.CodeConstants;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.DistributionListBy;
import com.zimbra.common.account.Key.UCServiceBy;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.service.ServiceException.Argument;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.EmailUtil;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.SetUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.SystemUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.Alias;
import com.zimbra.cs.account.AliasedEntry;
import com.zimbra.cs.account.AlwaysOnCluster;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.AttributeInfo;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.CacheAwareProvisioning;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.DynamicGroup;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.EntryCacheDataKey;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.GlobalGrant;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.GroupedEntry;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.IDNUtil;
import com.zimbra.cs.account.Identity;
import com.zimbra.cs.account.MailTarget;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.PreAuthKey;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ProvisioningExt;
import com.zimbra.cs.account.ProvisioningExt.PostCreateAccountListener;
import com.zimbra.cs.account.SearchAccountsOptions;
import com.zimbra.cs.account.SearchAccountsOptions.IncludeType;
import com.zimbra.cs.account.SearchDirectoryOptions;
import com.zimbra.cs.account.SearchDirectoryOptions.MakeObjectOpt;
import com.zimbra.cs.account.SearchDirectoryOptions.ObjectType;
import com.zimbra.cs.account.SearchDirectoryOptions.SortOpt;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ShareLocator;
import com.zimbra.cs.account.Signature;
import com.zimbra.cs.account.UCService;
import com.zimbra.cs.account.XMPPComponent;
import com.zimbra.cs.account.Zimlet;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.PermissionCache;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightCommand;
import com.zimbra.cs.account.accesscontrol.RightCommand.EffectiveRights;
import com.zimbra.cs.account.accesscontrol.RightModifier;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.account.auth.AuthMechanism;
import com.zimbra.cs.account.auth.AuthMechanism.AuthMech;
import com.zimbra.cs.account.auth.PasswordUtil;
import com.zimbra.cs.account.cache.DomainCache;
import com.zimbra.cs.account.cache.DomainCache.GetFromDomainCacheOption;
import com.zimbra.cs.account.cache.IAccountCache;
import com.zimbra.cs.account.cache.IDomainCache;
import com.zimbra.cs.account.cache.IMimeTypeCache;
import com.zimbra.cs.account.cache.INamedEntryCache;
import com.zimbra.cs.account.callback.CallbackContext;
import com.zimbra.cs.account.callback.CallbackContext.DataKey;
import com.zimbra.cs.account.gal.GalNamedFilter;
import com.zimbra.cs.account.gal.GalOp;
import com.zimbra.cs.account.gal.GalParams;
import com.zimbra.cs.account.gal.GalUtil;
import com.zimbra.cs.account.krb5.Krb5Principal;
import com.zimbra.cs.account.ldap.entry.LdapAccount;
import com.zimbra.cs.account.ldap.entry.LdapAlias;
import com.zimbra.cs.account.ldap.entry.LdapAlwaysOnCluster;
import com.zimbra.cs.account.ldap.entry.LdapCalendarResource;
import com.zimbra.cs.account.ldap.entry.LdapConfig;
import com.zimbra.cs.account.ldap.entry.LdapCos;
import com.zimbra.cs.account.ldap.entry.LdapDataSource;
import com.zimbra.cs.account.ldap.entry.LdapDistributionList;
import com.zimbra.cs.account.ldap.entry.LdapDomain;
import com.zimbra.cs.account.ldap.entry.LdapDynamicGroup;
import com.zimbra.cs.account.ldap.entry.LdapEntry;
import com.zimbra.cs.account.ldap.entry.LdapGlobalGrant;
import com.zimbra.cs.account.ldap.entry.LdapIdentity;
import com.zimbra.cs.account.ldap.entry.LdapMimeType;
import com.zimbra.cs.account.ldap.entry.LdapServer;
import com.zimbra.cs.account.ldap.entry.LdapShareLocator;
import com.zimbra.cs.account.ldap.entry.LdapSignature;
import com.zimbra.cs.account.ldap.entry.LdapUCService;
import com.zimbra.cs.account.ldap.entry.LdapXMPPComponent;
import com.zimbra.cs.account.ldap.entry.LdapZimlet;
import com.zimbra.cs.account.names.NameUtil;
import com.zimbra.cs.account.names.NameUtil.EmailAddress;
import com.zimbra.cs.datasource.DataSourceManager;
import com.zimbra.cs.ephemeral.EphemeralInput;
import com.zimbra.cs.ephemeral.EphemeralKey;
import com.zimbra.cs.ephemeral.EphemeralLocation;
import com.zimbra.cs.ephemeral.EphemeralStore;
import com.zimbra.cs.ephemeral.LdapEntryLocation;
import com.zimbra.cs.ephemeral.LdapEphemeralStore;
import com.zimbra.cs.ephemeral.migrate.AttributeConverter;
import com.zimbra.cs.ephemeral.migrate.AttributeMigration;
import com.zimbra.cs.gal.GalSearchConfig;
import com.zimbra.cs.gal.GalSearchControl;
import com.zimbra.cs.gal.GalSearchParams;
import com.zimbra.cs.gal.GalSearchResultCallback;
import com.zimbra.cs.gal.GalSyncToken;
import com.zimbra.cs.ldap.IAttributes;
import com.zimbra.cs.ldap.IAttributes.CheckBinary;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.ldap.LdapDateUtil;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapException.LdapContextNotEmptyException;
import com.zimbra.cs.ldap.LdapException.LdapEntryAlreadyExistException;
import com.zimbra.cs.ldap.LdapException.LdapEntryNotFoundException;
import com.zimbra.cs.ldap.LdapException.LdapInvalidAttrNameException;
import com.zimbra.cs.ldap.LdapException.LdapInvalidAttrValueException;
import com.zimbra.cs.ldap.LdapException.LdapInvalidSearchFilterException;
import com.zimbra.cs.ldap.LdapException.LdapMultipleEntriesMatchedException;
import com.zimbra.cs.ldap.LdapException.LdapSizeLimitExceededException;
import com.zimbra.cs.ldap.LdapServerConfig.ExternalLdapConfig;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapTODO.TODO;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.ldap.SearchLdapOptions;
import com.zimbra.cs.ldap.SearchLdapOptions.SearchLdapVisitor;
import com.zimbra.cs.ldap.SearchLdapOptions.StopIteratingException;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.ldap.ZLdapFilterFactory.FilterId;
import com.zimbra.cs.ldap.ZLdapSchema;
import com.zimbra.cs.ldap.ZMutableEntry;
import com.zimbra.cs.ldap.ZSearchControls;
import com.zimbra.cs.ldap.ZSearchResultEntry;
import com.zimbra.cs.ldap.ZSearchResultEnumeration;
import com.zimbra.cs.ldap.ZSearchScope;
import com.zimbra.cs.ldap.unboundid.InMemoryLdapServer;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mime.MimeTypeInfo;
import com.zimbra.cs.service.util.JWEUtil;
import com.zimbra.cs.service.util.ResetPasswordUtil;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.zimlet.ZimletException;
import com.zimbra.cs.zimlet.ZimletUtil;
import com.zimbra.soap.admin.type.CacheEntryType;
import com.zimbra.soap.admin.type.CountObjectsType;
import com.zimbra.soap.admin.type.DataSourceType;
import com.zimbra.soap.admin.type.GranteeSelector.GranteeBy;
import com.zimbra.soap.type.AutoProvPrincipalBy;
import com.zimbra.soap.type.GalSearchType;
import com.zimbra.soap.type.TargetBy;


/**
 * LDAP implementation of {@link Provisioning}.
 *
 * @since Sep 23, 2004
 * @author schemers
 */
public class LdapProvisioning extends LdapProv implements CacheAwareProvisioning {

    private static final Log mLog = LogFactory.getLog(LdapProvisioning.class);

    private static final String[] sInvalidAccountCreateModifyAttrs = {
            Provisioning.A_uid,
            Provisioning.A_zimbraMailAlias,
            Provisioning.A_zimbraMailDeliveryAddress,
    };


    private boolean useCache;
    private LdapCache cache;

    private final IAccountCache accountCache;
    private final INamedEntryCache<LdapCos> cosCache;
    private final IDomainCache domainCache;
    private final INamedEntryCache<Group> groupCache;
    private final IMimeTypeCache mimeTypeCache;
    private final INamedEntryCache<Server> serverCache;
    private final INamedEntryCache<AlwaysOnCluster> alwaysOnClusterCache;
    private final INamedEntryCache<UCService> ucServiceCache;
    private final INamedEntryCache<ShareLocator> shareLocatorCache;
    private final INamedEntryCache<XMPPComponent> xmppComponentCache;
    private final INamedEntryCache<LdapZimlet> zimletCache;

    private LdapConfig cachedGlobalConfig = null;
    private GlobalGrant cachedGlobalGrant = null;
    private static final Random sPoolRandom = new Random();
    private final Groups allDLs; // email addresses of all distribution lists on the system
    private final ZLdapFilterFactory filterFactory;

    private String[] BASIC_DL_ATTRS;
    private String[] BASIC_DYNAMIC_GROUP_ATTRS;
    private String[] BASIC_GROUP_ATTRS;

    private static LdapProvisioning singleton = null;

    private static synchronized void ensureSingleton(LdapProvisioning prov) {
        if (singleton != null) {
            // pass an exception to have the stack logged
            Zimbra.halt("Only one instance of LdapProvisioning can be created",
                    ServiceException.FAILURE("failed to instantiate LdapProvisioning", null));
        }
        singleton = prov;
    }

    public LdapProvisioning() {
        this(CacheMode.DEFAULT);
    }

    public LdapProvisioning(CacheMode cacheMode) {
        ensureSingleton(this);

        useCache = true;
        if (cacheMode == CacheMode.OFF) {
            useCache = false;
        }

        if (this.useCache) {
            cache = new LdapCache.LRUMapCache();
        } else {
            cache = new LdapCache.NoopCache();
        }

        accountCache = cache.accountCache();
        cosCache = cache.cosCache();
        domainCache = cache.domainCache();
        groupCache = cache.groupCache();
        mimeTypeCache = cache.mimeTypeCache();
        serverCache = cache.serverCache();
        ucServiceCache = cache.ucServiceCache();
        shareLocatorCache = cache.shareLocatorCache();
        xmppComponentCache = cache.xmppComponentCache();
        zimletCache = cache.zimletCache();
        alwaysOnClusterCache = cache.alwaysOnClusterCache();

        setDIT();
        setHelper(new ZLdapHelper(this));
        allDLs = new Groups(this);

        filterFactory = ZLdapFilterFactory.getInstance();

        try {
            BASIC_DL_ATTRS = getBasicDLAttrs();
            BASIC_DYNAMIC_GROUP_ATTRS = getBasicDynamicGroupAttrs();
            BASIC_GROUP_ATTRS = getBasicGroupAttrs();
        } catch (ServiceException e) {
            Zimbra.halt("failed to initialize LdapProvisioning", e);
        }

        register(new Validators.DomainAccountValidator());
        register(new Validators.DomainMaxAccountsValidator());
    }


    @Override
    public int getAccountCacheSize() { return accountCache.getSize(); }

    @Override
    public double getAccountCacheHitRate() { return accountCache.getHitRate(); }

    @Override
    public int getCosCacheSize() { return cosCache.getSize(); }

    @Override
    public double getCosCacheHitRate() { return cosCache.getHitRate(); }

    @Override
    public int getDomainCacheSize() { return domainCache.getSize(); }

    @Override
    public double getDomainCacheHitRate() { return domainCache.getHitRate(); }

    @Override
    public int getServerCacheSize() { return serverCache.getSize(); }

    @Override
    public double getServerCacheHitRate() { return serverCache.getHitRate(); }

    @Override
    public int getUCServiceCacheSize() { return ucServiceCache.getSize(); }

    @Override
    public double getUCServiceCacheHitRate() { return ucServiceCache.getHitRate(); }

    @Override
    public int getZimletCacheSize() { return zimletCache.getSize(); }

    @Override
    public double getZimletCacheHitRate() { return zimletCache.getHitRate(); }

    @Override
    public int getGroupCacheSize() { return groupCache.getSize(); }

    @Override
    public double getGroupCacheHitRate() { return groupCache.getHitRate(); }

    @Override
    public int getXMPPCacheSize() { return xmppComponentCache.getSize(); }

    @Override
    public double getXMPPCacheHitRate() { return xmppComponentCache.getHitRate(); }

    private String[] getBasicDLAttrs() throws ServiceException {
        AttributeManager attrMgr = AttributeManager.getInstance();
        Set<String> dlAttrs = attrMgr.getAllAttrsInClass(AttributeClass.distributionList);

        Set<String> attrs = Sets.newHashSet(dlAttrs);
        attrs.add(Provisioning.A_objectClass);
        attrs.remove(Provisioning.A_zimbraMailForwardingAddress);  // the member attr
        attrs.remove(Provisioning.A_zimbraMailTransport);          // does not apply to DL

        // remove deprecated attrs
        for (Iterator<String> iter = attrs.iterator(); iter.hasNext();) {
            String attr = iter.next();
            AttributeInfo ai = attrMgr.getAttributeInfo(attr);
            if (ai != null && ai.isDeprecated()) {
                iter.remove();
            }
        }

        return Lists.newArrayList(attrs).toArray(new String[attrs.size()]);
    }

    private String[] getBasicDynamicGroupAttrs() throws ServiceException {
        AttributeManager attrMgr = AttributeManager.getInstance();
        Set<String> dynGroupAttrs = attrMgr.getAllAttrsInClass(AttributeClass.group);

        Set<String> attrs = Sets.newHashSet(dynGroupAttrs);
        attrs.add(Provisioning.A_objectClass);

        // remove deprecated attrs
        for (Iterator<String> iter = attrs.iterator(); iter.hasNext();) {
            String attr = iter.next();
            AttributeInfo ai = attrMgr.getAttributeInfo(attr);
            if (ai != null && ai.isDeprecated()) {
                iter.remove();
            }
        }

        return Lists.newArrayList(attrs).toArray(new String[attrs.size()]);
    }

    private String[] getBasicGroupAttrs() throws ServiceException {
        Set<String> attrs = Sets.newHashSet();

        Set<String> dlAttrs = Sets.newHashSet(getBasicDLAttrs());
        Set<String> dynGroupAttrs = Sets.newHashSet(getBasicDynamicGroupAttrs());
        SetUtil.union(attrs, dlAttrs, dynGroupAttrs);
        return Lists.newArrayList(attrs).toArray(new String[attrs.size()]);
    }

    /*
     * Contains parallel arrays of old addrs and new addrs as a result of domain change
     */
    protected static class ReplaceAddressResult {
        ReplaceAddressResult(String oldAddrs[], String newAddrs[]) {
            mOldAddrs = oldAddrs;
            mNewAddrs = newAddrs;
        }
        private final String mOldAddrs[];
        private final String mNewAddrs[];

        public String[] oldAddrs() { return mOldAddrs; }
        public String[] newAddrs() { return mNewAddrs; }
    }

    /**
     * Modifies this entry. If any of the provided attributes are ephemeral,
     * they will be stored in LDAP instead of routed to EphemeralStore.
     * Used by LdapEphemeralStore.
     * @param e
     * @param attrs
     * @throws ServiceException
     */
    public void modifyEphemeralAttrsInLdap(Entry e, Map<String, ? extends Object> attrs)
    throws ServiceException {
        CallbackContext callbackContext = new CallbackContext(CallbackContext.Op.MODIFY);
        AttributeManager.getInstance().preModify(attrs, e, callbackContext, false, true);
        modifyAttrsInternal(e, null, attrs, true);
        AttributeManager.getInstance().postModify(attrs, e, callbackContext, true);
    }

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
    public void modifyAttrs(Entry e, Map<String, ? extends Object> attrs,
            boolean checkImmutable, boolean allowCallback)
    throws ServiceException {
        CallbackContext callbackContext = new CallbackContext(CallbackContext.Op.MODIFY);
        AttributeManager.getInstance().preModify(attrs, e, callbackContext, checkImmutable, allowCallback);
        modifyAttrsInternal(e, null, attrs);
        AttributeManager.getInstance().postModify(attrs, e, callbackContext, allowCallback);
    }

    @Override
    public void restoreAccountAttrs(Account acct, Map<String, ? extends Object> backupAttrs)
    throws ServiceException {
        Map<String, Object> attrs = Maps.newHashMap(backupAttrs);

        Object ocsInBackupObj = backupAttrs.get(A_objectClass);
        String[] ocsInBackup = StringUtil.toStringArray(ocsInBackupObj);

        String[] ocsOnAcct = acct.getMultiAttr(A_objectClass);

        // replace A_objectClass in backupAttrs with only OCs that is not a
        // super OC of LdapObjectClass.ZIMBRA_DEFAULT_PERSON_OC, then merge them with
        // OCs added during restoreAccount.
        List<String> needOCs = Lists.newArrayList(ocsOnAcct);
        for (String oc : ocsInBackup) {
            if (!LdapObjectClassHierarchy.isSuperiorOC(this, LdapObjectClass.ZIMBRA_DEFAULT_PERSON_OC, oc)) {
                if (!needOCs.contains(oc)) {
                    needOCs.add(oc);
                }
            }
        }

        attrs.put(A_objectClass, needOCs.toArray(new String[needOCs.size()]));

        modifyAttrs(acct, attrs, false, false);
    }

    protected void modifyAttrsInternal(Entry entry, ZLdapContext initZlc,
            Map<String, ? extends Object> attrs)
            throws ServiceException {
        modifyAttrsInternal(entry, initZlc, attrs, false);
    }
    /**
     * should only be called internally.
     *
     * @param initCtxt
     * @param attrs
     * @throws ServiceException
     */
    protected void modifyAttrsInternal(Entry entry, ZLdapContext initZlc,
            Map<String, ? extends Object> attrs, boolean storeEphemeralInLdap)
            throws ServiceException {
        if (entry instanceof Account && !(entry instanceof CalendarResource)) {
            Account acct = (Account) entry;
            validate(ProvisioningValidator.MODIFY_ACCOUNT_CHECK_DOMAIN_COS_AND_FEATURE,
                    acct.getAttr(A_zimbraMailDeliveryAddress), attrs, acct);
        }
        if (!storeEphemeralInLdap) {
            Map<String, AttributeInfo> ephemeralAttrMap = AttributeManager.getInstance().getEphemeralAttrs();
            Map<String, Object> ephemeralAttrs = new HashMap<String, Object>();
            List<String> toRemove = null; // remove after iteration to avoid ConcurrentModificationException
            for (Map.Entry<String, ? extends Object> e: attrs.entrySet()) {
                String key = e.getKey();
                String attrName;
                if (key.startsWith("+") || key.startsWith("-")) {
                    attrName = key.substring(1);
                } else {
                    attrName = key;
                }
                if (ephemeralAttrMap.containsKey(attrName.toLowerCase())) {
                    ephemeralAttrs.put(key, e.getValue());
                    if (null == toRemove) {
                        toRemove = Lists.newArrayListWithExpectedSize(3); // only 3 ephemeral attrs currently
                    }
                    toRemove.add(key);
                }
            }
            if (null != toRemove) {
                for (String key : toRemove) {
                    attrs.remove(key);
                }
            }
            if (!ephemeralAttrs.isEmpty()) {
                modifyEphemeralAttrs(entry, ephemeralAttrs, ephemeralAttrMap);
            }
        }
        modifyLdapAttrs(entry, initZlc, attrs);
    }

    private void modifyEphemeralAttrs(Entry entry, Map<String, Object> attrs, Map<String, AttributeInfo> ephemeralAttrMap) throws ServiceException {
        EphemeralLocation location = new LdapEntryLocation(entry);
        EphemeralStore store = EphemeralStore.getFactory().getStore();
        for (Map.Entry<String, Object> e: attrs.entrySet()) {
            String key = e.getKey();
            Object value = e.getValue();
            boolean doAdd = key.charAt(0) == '+';
            boolean doRemove = key.charAt(0) == '-';
            if (doAdd || doRemove) {
                key = key.substring(1);
                if (attrs.containsKey(key))
                    throw ServiceException.INVALID_REQUEST("can't mix +attrName/-attrName with attrName", null);
            }
            AttributeInfo ai = ephemeralAttrMap.get(key.toLowerCase());
            AttributeConverter converter = null;
            if (ai == null) { continue; }
            if (ai.isDynamic()) {
                converter = AttributeMigration.getConverter(key);
                if (converter == null) {
                    ZimbraLog.ephemeral.warn("Dynamic ephemeral attribute %s has no registered AttributeConverter, so cannot be modified with modifyAttrs", key);
                    continue;
                }
            }
            if (value instanceof Collection) {
                Collection values = (Collection) value;
                if (values.size() == 0) {
                    ZimbraLog.ephemeral.warn("Ephemeral attribute %s doesn't support deletion by key; only deletion by key+value is supported", key);
                    continue;
                } else {
                    for (Object v : values) {
                        if (v == null) { continue; }
                        String s = v.toString();
                        handleEphemeralAttrChange(store, key, s, location, ai, converter, doAdd, doRemove);
                    }
                }
            } else if (value instanceof Map) {
                throw ServiceException.FAILURE("Map is not a supported value type", null);
            } else if (value != null) {
                String s = value.toString();
                handleEphemeralAttrChange(store, key, s, location, ai, converter, doAdd, doRemove);
            } else {
                ZimbraLog.ephemeral.warn("Ephemeral attribute %s doesn't support deletion by key; only deletion by key+value is supported", key);
            }
        }
    }

    private void handleEphemeralAttrChange(EphemeralStore store, String key, String value, EphemeralLocation location,
            AttributeInfo ai, AttributeConverter converter, boolean doAdd, boolean doRemove)
    throws ServiceException {
        EphemeralInput input;
        if (ai.isDynamic()) {
            input = converter.convert(key, value);
            if (input == null) {
                ZimbraLog.ephemeral.warn("LDAP-formatted value %s for ephemeral attribute %s cannot be converted to an ephemeral input", value, key);
                return;
            }
        } else {
            input = new EphemeralInput(new EphemeralKey(key), value.toString());
        }
        if (doAdd) {
            store.update(input, location);
        }
        else if (doRemove) {
            store.delete(input.getEphemeralKey(), (String) input.getValue(), location);
        }
        else {
            store.set(input, location);
        }
    }

    private void modifyLdapAttrs(Entry entry, ZLdapContext initZlc,
            Map<String, ? extends Object> attrs)
            throws ServiceException {
        if (null == entry) {
            throw ServiceException.FAILURE("unable to modify attrs on null Entry", null);
        }
        ZLdapContext zlc = initZlc;
        try {
            if (zlc == null) {
                zlc = LdapClient.getContext(LdapServerType.MASTER,
                        LdapUsage.modifyEntryfromEntryType(entry.getEntryType()));
            }
            helper.modifyAttrs(zlc, ((LdapEntry)entry).getDN(), attrs, entry);
        } catch (LdapInvalidAttrNameException e) {
            throw AccountServiceException.INVALID_ATTR_NAME(
                    "invalid attr name: " + e.getMessage(), e);
        } catch (LdapInvalidAttrValueException e) {
            throw AccountServiceException.INVALID_ATTR_VALUE(
                    "invalid attr value: " + e.getMessage(), e);
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to modify attrs: " + e.getMessage(), e);
        } finally {
            if (zlc != null) {
                refreshEntry(entry, zlc);
            }
            if (initZlc == null) {
                LdapClient.closeContext(zlc);
            }
        }
    }

    private void setLdapPassword(Entry entry, ZLdapContext initZlc, String newPassword)
            throws ServiceException {
        ZLdapContext zlc = initZlc;
        try {
            if (zlc == null) {
                zlc = LdapClient.getContext(LdapServerType.MASTER,
                        LdapUsage.SET_PASSWORD);
            }
            zlc.setPassword(((LdapEntry)entry).getDN(), newPassword);
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to set password: "
                    + e.getMessage(), e);
        } finally {
            if (zlc != null) {
                refreshEntry(entry, zlc);
            }
            if (initZlc == null) {
                LdapClient.closeContext(zlc);
            }
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

        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.get(master), LdapUsage.GET_ENTRY);
            refreshEntry(e, zlc);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    void refreshEntry(Entry entry, ZLdapContext initZlc) throws ServiceException {

        try {
            String dn = ((LdapEntry)entry).getDN();
            ZAttributes attributes = helper.getAttributes(initZlc, dn);

            Map<String, Object> attrs = attributes.getAttrs();

            Map<String,Object> defaults = null;
            Map<String,Object> secondaryDefaults = null;
            Map<String,Object> overrideDefaults = null;

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
                Cos cos = getCOS(temp);
                if (cos != null)
                    defaults = cos.getAccountDefaults();
                Domain domain = getDomain((Account)entry);
                if (domain != null)
                    secondaryDefaults = domain.getAccountDefaults();
            } else if (entry instanceof Domain) {
                defaults = getConfig().getDomainDefaults();
            } else if (entry instanceof Server) {
                defaults = getConfig().getServerDefaults();
                AlwaysOnCluster aoc = getAlwaysOnCluster((Server) entry);
                if (aoc != null) {
                    overrideDefaults = aoc.getServerOverrides();
                }
            }

            if (defaults == null && secondaryDefaults == null)
                entry.setAttrs(attrs);
            else
                entry.setAttrs(attrs, defaults, secondaryDefaults, overrideDefaults);

            extendLifeInCacheOrFlush(entry);

        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to refresh entry", e);
        }
    }

    public void extendLifeInCacheOrFlush(Entry entry) {
        if (entry instanceof Account) {
            accountCache.replace((Account)entry);
        } else if (entry instanceof LdapCos) {
            cosCache.replace((LdapCos)entry);
        } else if (entry instanceof Domain) {
            domainCache.replace((Domain)entry);
        } else if (entry instanceof Server) {
            serverCache.replace((Server)entry);
        } else if (entry instanceof UCService) {
            ucServiceCache.replace((UCService)entry);
        } else if (entry instanceof XMPPComponent) {
            xmppComponentCache.replace((XMPPComponent)entry);
        } else if (entry instanceof LdapZimlet) {
            zimletCache.replace((LdapZimlet)entry);
        } else if (entry instanceof LdapAlwaysOnCluster) {
            alwaysOnClusterCache.replace((AlwaysOnCluster) entry);
        } else if (entry instanceof Group) {
            /*
             * DLs returned by Provisioning.get(DistributionListBy) and
             * DLs/dynamic groups returned by Provisioning.getGroup(DistributionListBy)
             * are "not" cached.
             *
             * DLs returned by Provisioning.getDLBasic(DistributionListBy) and
             * DLs/dynamic groups returned by Provisioning.getGroupBasic(DistributionListBy)
             * "are" cached.
             *
             * Need to flush out the cached entries if the instance being modified is not
             * in cache. (i.e. the instance being modified was obtained by get/getGroup)
             */
            Group modifiedInstance = (Group) entry;
            Group cachedInstance = getGroupFromCache(DistributionListBy.id, modifiedInstance.getId());
            if (cachedInstance != null && modifiedInstance != cachedInstance) {
                groupCache.remove(cachedInstance);
            }
        }
    }

    /**
     * Status check on LDAP connection.  Search for global config entry.
     */
    @Override
    public boolean healthCheck() throws ServiceException {
        boolean result = false;

        try {
            ZAttributes attrs = helper.getAttributes(LdapUsage.HEALTH_CHECK, mDIT.configDN());
            result = attrs != null; // not really needed, getAttributes should never return null
        } catch (ServiceException e) {
            mLog.warn("LDAP health check error", e);
        }
        return result;
    }

    @Override
    public synchronized Config getConfig() throws ServiceException
    {
        if (cachedGlobalConfig == null) {
            String configDn = mDIT.configDN();
            try {
                ZAttributes attrs = helper.getAttributes(LdapUsage.GET_GLOBALCONFIG, configDn);
                LdapConfig config = new LdapConfig(configDn, attrs, this);

                if (useCache) {
                    cachedGlobalConfig = config;
                } else {
                    return config;
                }
            } catch (ServiceException e) {
                throw ServiceException.FAILURE("unable to get config", e);
            }
        }
        return cachedGlobalConfig;
    }

    @Override
    public synchronized GlobalGrant getGlobalGrant() throws ServiceException
    {
        if (cachedGlobalGrant == null) {
            String globalGrantDn = mDIT.globalGrantDN();
            try {
                ZAttributes attrs = helper.getAttributes(LdapUsage.GET_GLOBALGRANT, globalGrantDn);
                LdapGlobalGrant globalGrant = new LdapGlobalGrant(globalGrantDn, attrs, this);

                if (useCache) {
                    cachedGlobalGrant = globalGrant;
                } else {
                    return globalGrant;
                }
            } catch (ServiceException e) {
                throw ServiceException.FAILURE("unable to get globalgrant", e);
            }
        }
        return cachedGlobalGrant;
    }

    @Override
    public List<MimeTypeInfo> getMimeTypes(String mimeType) throws ServiceException {
        return mimeTypeCache.getMimeTypes(this, mimeType);
    }

    @Override
    public List<MimeTypeInfo> getMimeTypesByQuery(String mimeType) throws ServiceException {
        List<MimeTypeInfo> mimeTypes = new ArrayList<MimeTypeInfo>();

        try {
            ZSearchResultEnumeration ne = helper.searchDir(mDIT.mimeBaseDN(),
                    filterFactory.mimeEntryByMimeType(mimeType), ZSearchControls.SEARCH_CTLS_SUBTREE());
            while (ne.hasMore()) {
                ZSearchResultEntry sr = ne.next();
                mimeTypes.add(new LdapMimeType(sr, this));
            }
            ne.close();
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to get mime types for " + mimeType, e);
        }
        return mimeTypes;
    }

    @Override
    public List<MimeTypeInfo> getAllMimeTypes() throws ServiceException {
        return mimeTypeCache.getAllMimeTypes(this);
    }

    @Override
    public List<MimeTypeInfo> getAllMimeTypesByQuery() throws ServiceException {
        List<MimeTypeInfo> mimeTypes = new ArrayList<MimeTypeInfo>();

        try {
            ZSearchResultEnumeration ne = helper.searchDir(
                    mDIT.mimeBaseDN(), filterFactory.allMimeEntries(),
                    ZSearchControls.SEARCH_CTLS_SUBTREE());
            while (ne.hasMore()) {
                ZSearchResultEntry sr = ne.next();
                mimeTypes.add(new LdapMimeType(sr, this));
            }
            ne.close();
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to get mime types", e);
        }
        return mimeTypes;
    }

    private ZSearchResultEntry getSearchResultForAccountByQuery(String base, ZLdapFilter filter, ZLdapContext initZlc,
            boolean loadFromMaster, String[] returnAttrs)
    throws ServiceException {
        try {
            ZSearchResultEntry sr = helper.searchForEntry(base, filter, initZlc, loadFromMaster, returnAttrs);
            return sr;
        } catch (LdapMultipleEntriesMatchedException e) {
            // duped entries are not in the exception, log it
            ZimbraLog.account.debug(e.getMessage());
            throw AccountServiceException.MULTIPLE_ACCOUNTS_MATCHED(
                    String.format("multiple entries are returned by query: base=%s, query=%s",
                    e.getQueryBase(), e.getQuery()));
        }
    }

    private Account getAccountByQuery(String base, ZLdapFilter filter, ZLdapContext initZlc,
            boolean loadFromMaster)
    throws ServiceException {
        try {
            ZSearchResultEntry sr = getSearchResultForAccountByQuery(base, filter, initZlc, loadFromMaster, null);
            if (sr != null) {
                return makeAccount(sr.getDN(), sr.getAttributes());
            }
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to lookup account via query: " +
                    filter.toFilterString() + " message: "+e.getMessage(), e);
        }
        return null;
    }

    private Account getAccountById(String zimbraId, ZLdapContext zlc, boolean loadFromMaster)
    throws ServiceException {
        if (zimbraId == null)
            return null;
        Account a = accountCache.getById(zimbraId);
        if (a == null) {
            ZLdapFilter filter = filterFactory.accountById(zimbraId);

            a = getAccountByQuery(mDIT.mailBranchBaseDN(), filter, zlc, loadFromMaster);

            // search again under the admin base if not found and admin base is not under mail base
            if (a == null && !mDIT.isUnder(mDIT.mailBranchBaseDN(), mDIT.adminBaseDN()))
                a = getAccountByQuery(mDIT.adminBaseDN(), filter, zlc, loadFromMaster);

            accountCache.put(a);
        }
        return a;
    }

    public String getDNforAccount(Account acct, ZLdapContext zlc, boolean loadFromMaster) {
        if (acct == null) {
            return null;
        }
        if (acct instanceof LdapAccount) {
            return ((LdapAccount) acct).getDN();
        }
        return getDNforAccountById(acct.getId(), zlc, loadFromMaster);
    }

    public String getDNforAccountById(String zimbraId, ZLdapContext zlc, boolean loadFromMaster) {
        if (zimbraId == null) {
            return null;
        }
        ZLdapFilter filter = filterFactory.accountById(zimbraId);
        try {
            String [] retAttrs = new String[] {Provisioning.A_zimbraId};  /* Just 1 attr to save bandwidth */
            ZSearchResultEntry sr;
            sr = getSearchResultForAccountByQuery(mDIT.mailBranchBaseDN(), filter, zlc, loadFromMaster, retAttrs);
            // search again under the admin base if not found and admin base is not under mail base
            if (sr == null && !mDIT.isUnder(mDIT.mailBranchBaseDN(), mDIT.adminBaseDN())) {
                sr = getSearchResultForAccountByQuery(mDIT.adminBaseDN(), filter, zlc, loadFromMaster, retAttrs);
            }
            if (sr != null) {
                return sr.getDN();
            }
        } catch (ServiceException e) {
            ZimbraLog.search.debug("unable to lookup DN for account via query: %s", filter.toFilterString(), e);
        }
        return null;
    }

    @Override
    public Account get(AccountBy keyType, String key) throws ServiceException {
        return get(keyType, key, false);
    }

    @Override
    public Account get(AccountBy keyType, String key, boolean loadFromMaster)
    throws ServiceException {
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
            return accountCache.getByName(key);
        case id:
            return accountCache.getById(key);
        case foreignPrincipal:
            return accountCache.getByForeignPrincipal(key);
        case name:
            return accountCache.getByName(key);
        case krb5Principal:
            throw ServiceException.FAILURE("key type krb5Principal is not supported by getFromCache", null);
        default:
            return null;
        }
    }

    private Account getAccountByForeignPrincipal(String foreignPrincipal, boolean loadFromMaster)
    throws ServiceException {
        Account a = accountCache.getByForeignPrincipal(foreignPrincipal);

        // bug 27966, always do a search so dup entries can be thrown
        Account acct = getAccountByQuery(
                mDIT.mailBranchBaseDN(),
                filterFactory.accountByForeignPrincipal(foreignPrincipal),
                null, loadFromMaster);

        // all is well, put the account in cache if it was not in cache
        // this is so we don't change our caching behavior - the above search was just to check for dup - we did that anyway
        // before bug 23372 was fixed.
        if (a == null) {
            a = acct;
            accountCache.put(a);
        }
        return a;
    }

    private Account getAdminAccountByName(String name, boolean loadFromMaster)
    throws ServiceException {
        Account a = accountCache.getByName(name);
        if (a == null) {
            a = getAccountByQuery(
                    mDIT.adminBaseDN(),
                    filterFactory.adminAccountByRDN(mDIT.accountNamingRdnAttr(), name),
                    null, loadFromMaster);
            accountCache.put(a);
        }
        return a;
    }

    private Account getAppAdminAccountByName(String name, boolean loadFromMaster)
    throws ServiceException {
        Account a = accountCache.getByName(name);
        if (a == null) {
            a = getAccountByQuery(
                    mDIT.appAdminBaseDN(),
                    filterFactory.adminAccountByRDN(mDIT.accountNamingRdnAttr(), name),
                    null, loadFromMaster);
            accountCache.put(a);
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

    Account getAccountByName(String emailAddress, boolean loadFromMaster)
    throws ServiceException {

        return getAccountByName(emailAddress, loadFromMaster, true);
    }

    Account getAccountByName(String emailAddress, boolean loadFromMaster, boolean checkAliasDomain)
    throws ServiceException {

        Account account = getAccountByNameInternal(emailAddress, loadFromMaster);

        // if not found, see if the domain is an alias domain and if so try to
        // get account by the alias domain target
        if (account == null) {
            if (checkAliasDomain) {
                String addrByDomainAlias = getEmailAddrByDomainAlias(emailAddress);
                if (addrByDomainAlias != null) {
                    account = getAccountByNameInternal(addrByDomainAlias, loadFromMaster);
                }
            }
        }

        return account;
    }

    private Account getAccountByNameInternal(String emailAddress, boolean loadFromMaster)
    throws ServiceException {

        emailAddress = fixupAccountName(emailAddress);

        Account account = accountCache.getByName(emailAddress);
        if (account == null) {
            account = getAccountByQuery(
                    mDIT.mailBranchBaseDN(),
                    filterFactory.accountByName(emailAddress),
                    null, loadFromMaster);
            accountCache.put(account);
        }
        return account;
    }

    @Override
    public Account getAccountByForeignName(String foreignName, String application, Domain domain)
    throws ServiceException {
        // first try direct match
        Account acct = getAccountByForeignPrincipal(application + ":" + foreignName);

        if (acct != null)
            return acct;

        if (domain == null) {
            String parts[] = foreignName.split("@");
            if (parts.length != 2)
                return null;

            String domainName = parts[1];
            domain = getDomain(Key.DomainBy.foreignName, application + ":" + domainName, true);
        }

        if (domain == null)
            return null;

        // see if there is a custom hander on the domain
        DomainNameMappingHandler.HandlerConfig handlerConfig =
            DomainNameMappingHandler.getHandlerConfig(domain, application);

        String acctName;
        if (handlerConfig != null) {
            // invoke the custom handler
            acctName = DomainNameMappingHandler.mapName(handlerConfig, foreignName, domain.getName());
        } else {
            // do our builtin mapping of {localpart}@{zimbra domain name}
            acctName = foreignName.split("@")[0] + "@" + domain.getName();
        }

        return get(AccountBy.name, acctName);
    }

    private Cos lookupCos(String key, ZLdapContext zlc) throws ServiceException {
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
    public void autoProvAccountEager(EagerAutoProvisionScheduler scheduler)
    throws ServiceException {
        AutoProvisionEager.handleScheduledDomains(this, scheduler);
    }

    @Override
    public Account autoProvAccountLazy(Domain domain, String loginName,
            String loginPassword, AutoProvAuthMech authMech)
    throws ServiceException {
        AutoProvisionLazy autoPorv =
            new AutoProvisionLazy(this, domain, loginName, loginPassword, authMech);
        return autoPorv.handle();
    }

    @Override
    public Account autoProvAccountManual(Domain domain, AutoProvPrincipalBy by,
            String principal, String password)
    throws ServiceException {
        AutoProvisionManual autoProv =
            new AutoProvisionManual(this, domain, by, principal, password);
        return autoProv.handle();
    }

    @Override
    public void searchAutoProvDirectory(Domain domain, String filter, String name,
            String[] returnAttrs, int maxResults, DirectoryEntryVisitor visitor)
    throws ServiceException {
        AutoProvision.searchAutoProvDirectory(this, domain, filter, name, null,
                returnAttrs, maxResults, visitor);
    }

    @Override
    public Account createAccount(String emailAddress, String password, Map<String, Object> attrs)
    throws ServiceException {
        return createAccount(emailAddress, password, attrs,
                mDIT.handleSpecialAttrs(attrs), null, false, null);
    }

    @Override
    public Account restoreAccount(String emailAddress, String password,
            Map<String, Object> attrs, Map<String, Object> origAttrs)
    throws ServiceException {
        return createAccount(emailAddress, password, attrs,
                mDIT.handleSpecialAttrs(attrs), null, true, origAttrs);
    }

    private Account createAccount(String emailAddress, String password,
            Map<String, Object> acctAttrs, SpecialAttrs specialAttrs,
            String[] additionalObjectClasses,
            boolean restoring, Map<String, Object> origAttrs)
    throws ServiceException {

        String uuid = specialAttrs.getZimbraId();
        String baseDn = specialAttrs.getLdapBaseDn();

        emailAddress = emailAddress.toLowerCase().trim();
        String parts[] = emailAddress.split("@");
        if (parts.length != 2) {
            throw ServiceException.INVALID_REQUEST("must be valid email address: " +
                    emailAddress, null);
        }

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

        if (acctAttrs == null) {
            acctAttrs = new HashMap<String, Object>();
        }
        CallbackContext callbackContext = new CallbackContext(CallbackContext.Op.CREATE);
        callbackContext.setCreatingEntryName(emailAddress);
        AttributeManager.getInstance().preModify(acctAttrs, null, callbackContext, true);

        Account acct = null;
        String dn = null;
        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.CREATE_ACCOUNT);

            Domain d = getDomainByAsciiName(domain, zlc);
            if (d == null) {
                throw AccountServiceException.NO_SUCH_DOMAIN(domain);
            }

            if (!d.isLocal()) {
                throw ServiceException.INVALID_REQUEST("domain type must be local", null);
            }

            ZMutableEntry entry = LdapClient.createMutableEntry();
            entry.mapToAttrs(acctAttrs);

            for (int i=0; i < sInvalidAccountCreateModifyAttrs.length; i++) {
                String a = sInvalidAccountCreateModifyAttrs[i];
                if (entry.hasAttribute(a))
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

            boolean skipCountingLicenseQuota = false;

            /* bug 48226
             *
             * Check if any of the OCs in the backup is a structural OC that subclasses
             * our default OC (defined in ZIMBRA_DEFAULT_PERSON_OC).
             * If so, add that OC now while creating the account, because it cannot be modified later.
             */
            if (restoring && origAttrs != null) {
                Object ocsInBackupObj = origAttrs.get(A_objectClass);
                String[] ocsInBackup = StringUtil.toStringArray(ocsInBackupObj);

                String mostSpecificOC = LdapObjectClassHierarchy.getMostSpecificOC(
                        this, ocsInBackup, LdapObjectClass.ZIMBRA_DEFAULT_PERSON_OC);

                if (!LdapObjectClass.ZIMBRA_DEFAULT_PERSON_OC.equalsIgnoreCase(mostSpecificOC)) {
                    ocs.add(mostSpecificOC);
                }

                //calendar resource doesn't count against license quota
                if (origAttrs.get(A_zimbraCalResType) != null) {
                    skipCountingLicenseQuota = true;
                }
                if (origAttrs.get(A_zimbraIsSystemResource) != null) {
                    entry.setAttr(A_zimbraIsSystemResource, "TRUE");
                    skipCountingLicenseQuota = true;
                }
                if (origAttrs.get(A_zimbraIsExternalVirtualAccount) != null) {
                    entry.setAttr(A_zimbraIsExternalVirtualAccount, "TRUE");
                    skipCountingLicenseQuota = true;
                }
            }

            entry.addAttr(A_objectClass, ocs);

            String zimbraIdStr;
            if (uuid == null) {
                zimbraIdStr = LdapUtil.generateUUID();
            } else {
                zimbraIdStr = uuid;
            }
            entry.setAttr(A_zimbraId, zimbraIdStr);
            entry.setAttr(A_zimbraCreateTimestamp, LdapDateUtil.toGeneralizedTime(new Date()));

            // default account status is active
            if (!entry.hasAttribute(Provisioning.A_zimbraAccountStatus)) {
                entry.setAttr(A_zimbraAccountStatus, Provisioning.ACCOUNT_STATUS_ACTIVE);
            }

            Cos cos = null;
            String cosId = entry.getAttrString(Provisioning.A_zimbraCOSId);

            if (cosId != null) {
                cos = lookupCos(cosId, zlc);
                if (!cos.getId().equals(cosId)) {
                    cosId = cos.getId();
                }
                entry.setAttr(Provisioning.A_zimbraCOSId, cosId);
            } else {
                String domainCosId =
                        domain != null ?
                        isExternalVirtualAccount(entry) ?
                        d.getDomainDefaultExternalUserCOSId() :
                        d.getDomainDefaultCOSId() :
                        null;

                if (domainCosId != null) {
                    cos = get(Key.CosBy.id, domainCosId);
                }
                if (cos == null) {
                    cos = getCosByName(isExternalVirtualAccount(entry) ?
                            Provisioning.DEFAULT_EXTERNAL_COS_NAME : Provisioning.DEFAULT_COS_NAME,
                            zlc);
                }
            }

            boolean hasMailTransport = entry.hasAttribute(Provisioning.A_zimbraMailTransport);

            // if zimbraMailTransport is NOT provided, pick a server and add
            // zimbraMailHost(and zimbraMailTransport) if it is not specified
            if (!hasMailTransport) {
                addMailHost(entry, cos, true);
            }

            // set all the mail-related attrs if zimbraMailHost or zimbraMailTransport was specified
            if (entry.hasAttribute(Provisioning.A_zimbraMailHost) ||
                entry.hasAttribute(Provisioning.A_zimbraMailTransport)) {
                // default mail status is enabled
                if (!entry.hasAttribute(Provisioning.A_zimbraMailStatus)) {
                    entry.setAttr(A_zimbraMailStatus, MAIL_STATUS_ENABLED);
                }

                // default account mail delivery address is email address
                if (!entry.hasAttribute(Provisioning.A_zimbraMailDeliveryAddress)) {
                    entry.setAttr(A_zimbraMailDeliveryAddress, emailAddress);
                }
            } else {
                throw ServiceException.INVALID_REQUEST("missing " +
                        Provisioning.A_zimbraMailHost + " or " + Provisioning.A_zimbraMailTransport +
                        " for CreateAccount: " + emailAddress, null);
            }

            // amivisAccount requires the mail attr, so we always add it
            entry.setAttr(A_mail, emailAddress);

            // required for ZIMBRA_DEFAULT_PERSON_OC class
            if (!entry.hasAttribute(Provisioning.A_cn)) {
                String displayName = entry.getAttrString(Provisioning.A_displayName);
                if (displayName != null) {
                    entry.setAttr(A_cn, displayName);
                } else {
                    entry.setAttr(A_cn, localPart);
                }
            }

            // required for ZIMBRA_DEFAULT_PERSON_OC class
            if (!entry.hasAttribute(Provisioning.A_sn)) {
                entry.setAttr(A_sn, localPart);
            }

            entry.setAttr(A_uid, localPart);
            String entryPassword = entry.getAttrString(Provisioning.A_userPassword);
            if (entryPassword != null) {
                //password is a hash i.e. from autoprov; do not set with modify password
                password = null;
            } else if (password != null) {
                //user entered
                checkPasswordStrength(password, null, cos, entry);
            }
            entry.setAttr(Provisioning.A_zimbraPasswordModifiedTime, LdapDateUtil.toGeneralizedTime(new Date()));

            String ucPassword = entry.getAttrString(Provisioning.A_zimbraUCPassword);
            if (ucPassword != null) {
                String encryptedPassword = Account.encrypytUCPassword(
                        entry.getAttrString(Provisioning.A_zimbraId), ucPassword);
                entry.setAttr(Provisioning.A_zimbraUCPassword, encryptedPassword);
            }

            dn = mDIT.accountDNCreate(baseDn, entry.getAttributes(), localPart, domain);
            entry.setDN(dn);

            zlc.createEntry(entry);
            acct = getAccountById(zimbraIdStr, zlc, true);
            if (acct == null) {
                throw ServiceException.FAILURE(
                        "unable to get account after creating LDAP account entry: " +
                        emailAddress + ", check ldap log for possible error", null);
            }

            AttributeManager.getInstance().postModify(acctAttrs, acct, callbackContext);
            removeExternalAddrsFromAllDynamicGroups(acct.getAllAddrsSet(), zlc);
            validate(ProvisioningValidator.CREATE_ACCOUNT_SUCCEEDED, emailAddress, acct, skipCountingLicenseQuota);
            if (password != null) {
                setLdapPassword(acct, zlc, password);
            }
            return acct;
        } catch (LdapEntryAlreadyExistException e) {
            throw AccountServiceException.ACCOUNT_EXISTS(emailAddress, dn, e);
        } catch (LdapException e) {
            throw e;
        } catch (AccountServiceException e) {
            throw e;
        } catch (ServiceException e) {
           throw ServiceException.FAILURE("unable to create account: "+emailAddress, e);
        } finally {
            LdapClient.closeContext(zlc);

            if (!restoring && acct != null) {
                for (PostCreateAccountListener listener : ProvisioningExt.getPostCreateAccountListeners()) {
                    if (listener.enabled()) {
                        listener.handle(acct);
                    }
                }
            }
        }
    }

    private boolean isExternalVirtualAccount(ZMutableEntry entry) throws LdapException {
        return entry.hasAttribute(Provisioning.A_zimbraIsExternalVirtualAccount) &&
                ProvisioningConstants.TRUE.equals(entry.getAttrString(Provisioning.A_zimbraIsExternalVirtualAccount));
    }

    @Override
    public void searchOCsForSuperClasses(Map<String, Set<String>> ocs) {
        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.GET_SCHEMA);
            ZLdapSchema schema = zlc.getSchema();

            for (Map.Entry<String, Set<String>> entry : ocs.entrySet()) {
                String oc = entry.getKey();
                Set<String> superOCs = entry.getValue();

                try {
                    ZimbraLog.account.debug("Looking up OC: " + oc);
                    ZLdapSchema.ZObjectClassDefinition ocSchema = schema.getObjectClass(oc);

                    if (ocSchema != null) {
                        List<String> superClasses = ocSchema.getSuperiorClasses();
                        for (String superOC : superClasses) {
                            superOCs.add(superOC.toLowerCase());
                        }
                    }

                } catch (ServiceException e) {
                    ZimbraLog.account.debug("unable to load LDAP schema extension for objectclass: " + oc, e);
                }

            }

        } catch (ServiceException e) {
            ZimbraLog.account.warn("unable to get LDAP schema", e);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }


    @Override
    public void getAttrsInOCs(String[] ocs, Set<String> attrsInOCs)
            throws ServiceException {

        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.GET_SCHEMA);
            ZLdapSchema schema = zlc.getSchema();

            for (String oc : ocs) {
                try {
                    ZLdapSchema.ZObjectClassDefinition ocSchema = schema.getObjectClass(oc);

                    if (ocSchema != null) {
                        List<String> optAttrs = ocSchema.getOptionalAttributes();
                        for (String attr : optAttrs) {
                            attrsInOCs.add(attr);
                        }
                        List<String> reqAttrs = ocSchema.getRequiredAttributes();
                        for (String attr : reqAttrs) {
                            attrsInOCs.add(attr);
                        }
                    }

                } catch (ServiceException e) {
                    ZimbraLog.account.debug("unable to lookup attributes for objectclass: " + oc, e);
                }
            }

        } catch (ServiceException e) {
            ZimbraLog.account.warn("unable to get LDAP schema", e);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    private void setMailHost(ZMutableEntry entry, Server server, boolean setMailTransport) {
        String serviceHostname = server.getAttr(Provisioning.A_zimbraServiceHostname);
        entry.setAttr(Provisioning.A_zimbraMailHost, serviceHostname);

        if (setMailTransport) {
            int lmtpPort = server.getIntAttr(Provisioning.A_zimbraLmtpBindPort,
                    com.zimbra.cs.util.Config.D_LMTP_BIND_PORT);
            String transport = "lmtp:" + serviceHostname + ":" + lmtpPort;
            entry.setAttr(Provisioning.A_zimbraMailTransport, transport);
        }
    }

    private boolean addDefaultMailHost(ZMutableEntry entry, Server server, boolean setMailTransport)
    throws ServiceException {
        String serviceHostname = server.getAttr(Provisioning.A_zimbraServiceHostname);
        if (server.hasMailClientService() && serviceHostname != null) {
            setMailHost(entry,  server, setMailTransport);
            return true;
        }
        return false;
    }

    private void addDefaultMailHost(ZMutableEntry entry, boolean setMailTransport)
    throws ServiceException {
        if (!addDefaultMailHost(entry, getLocalServer(), setMailTransport)) {
            for (Server server: getAllServers()) {
                if (addDefaultMailHost(entry, server, setMailTransport)) {
                    return;
                }
            }
        }
    }

    private void addMailHost(ZMutableEntry entry, Cos cos, boolean setMailTransport)
    throws ServiceException {
        // if zimbraMailHost is not specified, and we have a COS, see if there is
        // a pool to pick from.
        if (cos != null && !entry.hasAttribute(Provisioning.A_zimbraMailHost)) {
            String mailHostPool[] = cos.getMultiAttr(Provisioning.A_zimbraMailHostPool);
            addMailHost(entry, mailHostPool, cos.getName(), setMailTransport);
        }

        // if zimbraMailHost still not specified, default to local server's
        // zimbraServiceHostname if it has the mailbox service enabled, otherwise
        // look through all servers and pick first with the service enabled.
        // this means every account will always have a mailbox
        if (!entry.hasAttribute(Provisioning.A_zimbraMailHost)) {
            addDefaultMailHost(entry, setMailTransport);
        }
    }

    private String addMailHost(ZMutableEntry entry, String[] mailHostPool, String cosName,
            boolean setMailTransport)
    throws ServiceException {
        if (mailHostPool.length == 0) {
            return null;
        } else if (mailHostPool.length > 1) {
            // copy it, since we are dealing with a cached String[]
            String pool[] = new String[mailHostPool.length];
            System.arraycopy(mailHostPool, 0, pool, 0, mailHostPool.length);
            mailHostPool = pool;
        }

        // Shuffle up and deal
        int max = mailHostPool.length;
        while (max > 0) {
            int i = sPoolRandom.nextInt(max);
            String mailHostId = mailHostPool[i];
            Server s = (mailHostId == null) ? null : getServerByIdInternal(mailHostId);
            if (s != null) {
                String mailHost = s.getAttr(Provisioning.A_zimbraServiceHostname);
                if (mailHost != null) {
                    if (s.hasMailClientService()) {
                        setMailHost(entry, s, setMailTransport);
                        return mailHost;
                    } else {
                        ZimbraLog.account.warn("cos("+cosName+") mailHostPool server(" +
                                s.getName()+") is not a mailclient server with service webapp enabled");
                    }
                } else {
                    ZimbraLog.account.warn("cos("+cosName+") mailHostPool server(" +
                            s.getName()+") has no service hostname");
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
        SearchAccountsOptions opts = new SearchAccountsOptions();
        opts.setFilter(filterFactory.allAdminAccounts());
        opts.setIncludeType(IncludeType.ACCOUNTS_ONLY);
        opts.setSortOpt(SortOpt.SORT_ASCENDING);
        return (List<Account>) searchDirectoryInternal(opts);
    }

    @Override
    public void searchAccountsOnServer(Server server, SearchAccountsOptions opts,
            NamedEntry.Visitor visitor)
    throws ServiceException {
        searchAccountsOnServerInternal(server, opts, visitor);
    }

    @Override
    public List<NamedEntry> searchAccountsOnServer(Server server, SearchAccountsOptions opts)
    throws ServiceException {
        return searchAccountsOnServerInternal(server, opts, null);
    }

    private List<NamedEntry> searchAccountsOnServerInternal(Server server,
            SearchAccountsOptions options, NamedEntry.Visitor visitor)
    throws ServiceException {
        // filter cannot be set
        if (options.getFilter() != null || options.getFilterString() != null) {
            throw ServiceException.INVALID_REQUEST(
                    "cannot set filter for searchAccountsOnServer", null);
        }

        if (server == null) {
            throw ServiceException.INVALID_REQUEST(
                    "missing server", null);
        }

        IncludeType includeType = options.getIncludeType();

        /*
         * This is the ONLY place where search filter can be affected by domain, because
         * we have to support custom DIT where account/cr entries are NOT populated under
         * the domain tree.  In our default LdapDIT implementation, domain is always
         * ignored in the filterXXX(domain, server) calls.
         *
         * Would be great if we don't have to support custom DIT someday.
         */
        Domain domain = options.getDomain();
        ZLdapFilter filter;
        if (includeType == IncludeType.ACCOUNTS_AND_CALENDAR_RESOURCES) {
            filter = mDIT.filterAccountsByDomainAndServer(domain, server);
        } else if (includeType == IncludeType.ACCOUNTS_ONLY) {
            filter = mDIT.filterAccountsOnlyByDomainAndServer(domain, server);
        } else {
            filter = mDIT.filterCalendarResourceByDomainAndServer(domain, server);
        }

        options.setFilter(filter);
        return searchDirectoryInternal(options, visitor);
    }

    @Override
    public List<NamedEntry> searchDirectory(SearchDirectoryOptions options)
    throws ServiceException {
        return searchDirectoryInternal(options, null);
    }

    @Override
    public void searchDirectory(SearchDirectoryOptions options, NamedEntry.Visitor visitor)
    throws ServiceException {
        searchDirectoryInternal(options, visitor);
    }

    protected List<?> searchDirectoryInternal(SearchDirectoryOptions options)
    throws ServiceException{
        return searchDirectoryInternal(options, null);
    }

    public String[] getSearchBases(Domain domain, Set<ObjectType> types)
    throws ServiceException {
        String[] bases;

        if (domain != null) {
            String domainDN = ((LdapDomain) domain).getDN();

            boolean domainsTree = false;
            boolean groupsTree = false;
            boolean peopleTree = false;

            if (types.contains(ObjectType.dynamicgroups)) {
                groupsTree = true;
            }
            if (types.contains(ObjectType.accounts) ||
                types.contains(ObjectType.aliases) ||
                types.contains(ObjectType.distributionlists) ||
                types.contains(ObjectType.resources)) {
                peopleTree = true;
            }
            if (types.contains(ObjectType.domains)) {
                domainsTree = true;
            }

            /*
             * error if a domain is specified but non of domain-ed object types is specified.
             */
            if (!groupsTree && !peopleTree && !domainsTree) {
                throw ServiceException.INVALID_REQUEST(
                        "domain is specified but non of domain-ed types is specified", null);
            }

            /*
             * error if both domain type and one on account/resource/group types are also
             * requested.  Because, for domains, we *do* want to return sub-domains;
             * but for accounts/resources/groups, we only want entries in the specified
             * domain, not sub-domains.
             *
             * e.g. if domains and accounts are requested, the search base would be the
             * domains DN, then all accounts in sub-domains will also be returned.   This
             * can be worked out by the DN Subtree Match Filter:
             * (||(objectClass=zimbraDomain)(&(objectClass=zimbraAccount)(DN-Subtree-Match-Filter)))
             * but it will need some work in the existing code.  This use case is not needed
             * for now - just throw.
             */
            if (domainsTree && (groupsTree || peopleTree)) {
                throw ServiceException.FAILURE("specifying domains type and one of " +
                        "accounts/resources/groups type is not supported by in-memory LDAP server", null);
            }

            /*
             * InMemoryDirectoryServer does not support EXTENSIBLE-MATCH filter.
             */
            if (InMemoryLdapServer.isOn()) {
                /*
                 * unit test path: DN Subtree Match Filter is not supported by InMemoryLdapServer
                 *
                 * If search for domains, case is the domains DN.
                 *
                 * If search for accounts/resources/groups:
                 * Search twice: once under the people tree, once under the groups tree,
                 * so entries under sub-domains are not returned.
                 *
                 */
                if (domainsTree) {
                    bases = new String[]{domainDN};
                } else {
                    List<String> baseList = Lists.newArrayList();

                    if (groupsTree) {
                        baseList.add(mDIT.domainDNToDynamicGroupsBaseDN(domainDN));
                    }

                    if (peopleTree) {
                        baseList.add(mDIT.domainDNToAccountSearchDN(domainDN));
                    }

                    bases = baseList.toArray(new String[baseList.size()]);
                }

            } else {
                /*
                 * production path
                 *
                 * use DN Subtree Match Filter and domain DN or base if objects in both
                 * people tree and groups tree are needed
                 */
                String searchBase;
                if (domainsTree) {
                    searchBase = domainDN;
                } else {
                    if ((groupsTree && peopleTree)) {
                        searchBase = domainDN;
                    } else if (groupsTree) {
                        searchBase = mDIT.domainDNToDynamicGroupsBaseDN(domainDN);
                    } else {
                        searchBase = mDIT.domainDNToAccountSearchDN(domainDN);
                    }
                }
                bases = new String[]{searchBase};
            }

        } else {
            int flags = SearchDirectoryOptions.getTypesAsFlags(types);
            bases = mDIT.getSearchBases(flags);
        }

        return bases;
    }

    private List<NamedEntry> searchDirectoryInternal(SearchDirectoryOptions options,
            NamedEntry.Visitor visitor)
    throws ServiceException {
        Set<ObjectType> types = options.getTypes();

        if (types == null) {
            throw ServiceException.INVALID_REQUEST("missing types", null);
        }

        /*
         * base
         */
        Domain domain = options.getDomain();
        String[] bases = getSearchBases(domain, types);

        /*
         * filter
         */
        int flags = options.getTypesAsFlags();
        ZLdapFilter filter = options.getFilter();
        String filterStr = options.getFilterString();

        // exact one of filter or filterString has to be set
        if (filter != null && filterStr != null) {
            throw ServiceException.INVALID_REQUEST("only one of filter or filterString can be set", null);
        }

        if (filter == null) {
            if (options.getConvertIDNToAscii() && !Strings.isNullOrEmpty(filterStr)) {
                filterStr = LdapEntrySearchFilter.toLdapIDNFilter(filterStr);
            }

            // prepend objectClass filters
            String objectClass = getObjectClassQuery(flags);

            if (filterStr == null || filterStr.equals("")) {
                filterStr = objectClass;
            } else {
                if (filterStr.startsWith("(") && filterStr.endsWith(")")) {
                    filterStr = "(&" + objectClass + filterStr + ")";
                } else {
                    filterStr = "(&" + objectClass + "(" + filterStr + ")" + ")";
                }
            }

            FilterId filterId = options.getFilterId();
            if (filterId == null) {
                throw ServiceException.INVALID_REQUEST("missing filter id", null);
            }
            filter = filterFactory.fromFilterString(options.getFilterId(), filterStr);
        }

        if (domain != null && !InMemoryLdapServer.isOn()) {
            boolean groupsTree = false;
            boolean peopleTree = false;

            if (types.contains(ObjectType.dynamicgroups)) {
                groupsTree = true;
            }
            if (types.contains(ObjectType.accounts) ||
                types.contains(ObjectType.aliases) ||
                types.contains(ObjectType.distributionlists) ||
                types.contains(ObjectType.resources)) {
                peopleTree = true;
            }

            if (groupsTree && peopleTree) {
                ZLdapFilter dnSubtreeMatchFilter = ((LdapDomain) domain).getDnSubtreeMatchFilter();
                filter = filterFactory.andWith(filter, dnSubtreeMatchFilter);
            }
        }


        /*
         * return attrs
         */
        String[] returnAttrs = fixReturnAttrs(options.getReturnAttrs(), flags);

        return searchObjects(bases, filter, returnAttrs, options, visitor);
    }

    private static String getObjectClassQuery(int flags) {
        boolean accounts = (flags & Provisioning.SD_ACCOUNT_FLAG) != 0;
        boolean aliases = (flags & Provisioning.SD_ALIAS_FLAG) != 0;
        boolean lists = (flags & Provisioning.SD_DISTRIBUTION_LIST_FLAG) != 0;
        boolean groups = (flags & Provisioning.SD_DYNAMIC_GROUP_FLAG) != 0;
        boolean calendarResources = (flags & Provisioning.SD_CALENDAR_RESOURCE_FLAG) != 0;
        boolean domains = (flags & Provisioning.SD_DOMAIN_FLAG) != 0;
        boolean coses = (flags & Provisioning.SD_COS_FLAG) != 0;

        int num = (accounts ? 1 : 0) +
                  (aliases ? 1 : 0) +
                  (lists ? 1 : 0) +
                  (groups ? 1 : 0) +
                  (domains ? 1 : 0) +
                  (coses ? 1 : 0) +
                  (calendarResources ? 1 : 0);
        if (num == 0) {
            accounts = true;
        }

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

        if (accounts && !calendarResources) {
            oc.append("(&");
        }

        if (num > 1) {
            oc.append("(|");
        }

        if (accounts) oc.append("(objectclass=zimbraAccount)");
        if (aliases) oc.append("(objectclass=zimbraAlias)");
        if (lists) oc.append("(objectclass=zimbraDistributionList)");
        if (groups) oc.append("(objectclass=zimbraGroup)");
        if (domains) oc.append("(objectclass=zimbraDomain)");
        if (coses) oc.append("(objectclass=zimbraCos)");
        if (calendarResources) oc.append("(objectclass=zimbraCalendarResource)");

        if (num > 1) {
            oc.append(")");
        }

        if (accounts && !calendarResources) {
            oc.append("(!(objectclass=zimbraCalendarResource)))");
        }

        return oc.toString();
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
            mByName = sortAttr == null || sortAttr.length() == 0 || sortAttr.equals("name");
        }

        @Override
        public int compare(NamedEntry a, NamedEntry b) {
            int comp = 0;

            if (mByName)
                comp = a.getName().compareToIgnoreCase(b.getName());
            else {
                String sa = null;
                String sb = null;
                if (SearchDirectoryOptions.SORT_BY_TARGET_NAME.equals(mSortAttr) &&
                        (a instanceof Alias) && (b instanceof Alias)) {
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

    @TODO
    private static class SearchObjectsVisitor extends SearchLdapVisitor {

        private final LdapProvisioning prov;
        private final ZLdapContext zlc;
        private final String configBranchBaseDn;
        private final NamedEntry.Visitor visitor;
        private final int maxResults;
        private final MakeObjectOpt makeObjOpt;
        private final String returnAttrs[];

        private final int total = 0;

        private SearchObjectsVisitor(LdapProvisioning prov, ZLdapContext zlc,
                NamedEntry.Visitor visitor, int maxResults, MakeObjectOpt makeObjOpt,
                String returnAttrs[]) {
            super(false);

            this.prov = prov;
            this.zlc = zlc;
            configBranchBaseDn = prov.getDIT().configBranchBaseDN();
            this.visitor = visitor;
            this.maxResults = maxResults;
            this.makeObjOpt = makeObjOpt;
            this.returnAttrs = returnAttrs;
        }

        @Override
        public void visit(String dn, IAttributes ldapAttrs) {
            try {
                doVisit(dn, ldapAttrs);
            } catch (ServiceException e) {
                ZimbraLog.account.warn("entry skipped, encountered error while processing entry at:" + dn, e);
            }
        }

        @TODO
        private void doVisit(String dn, IAttributes ldapAttrs) throws ServiceException {
            /* can this happen?  TODO: check
            if (maxResults > 0 && total++ > maxResults) {
                throw AccountServiceException.TOO_MANY_SEARCH_RESULTS("exceeded limit of "+maxResults, null);
            }
            */

            List<String> objectclass =
                ldapAttrs.getMultiAttrStringAsList(Provisioning.A_objectClass, CheckBinary.NOCHECK);

            // skip admin accounts
            // if we are looking for domains or coses, they can be under config branch in non default DIT impl.
            if (dn.endsWith(configBranchBaseDn) &&
                    !objectclass.contains(AttributeClass.OC_zimbraDomain) &&
                    !objectclass.contains(AttributeClass.OC_zimbraCOS)) {
                return;
            }

            ZAttributes attrs = (ZAttributes)ldapAttrs;

            if (objectclass == null || objectclass.contains(AttributeClass.OC_zimbraAccount)
                || objectclass.contains(AttributeClass.OC_zimbraCalendarResource)) {
                visitor.visit(prov.makeAccount(dn, attrs, makeObjOpt));
            } else if (objectclass.contains(AttributeClass.OC_zimbraAlias)) {
                visitor.visit(prov.makeAlias(dn, attrs));
            } else if (objectclass.contains(AttributeClass.OC_zimbraDistributionList)) {
                boolean isBasic = returnAttrs != null;
                visitor.visit(prov.makeDistributionList(dn, attrs, isBasic));
            } else if (objectclass.contains(AttributeClass.OC_zimbraGroup)) {
                visitor.visit(prov.makeDynamicGroup(zlc, dn, attrs));
            } else if (objectclass.contains(AttributeClass.OC_zimbraDomain)) {
                visitor.visit(new LdapDomain(dn, attrs, prov.getConfig().getDomainDefaults(), prov));
            } else if (objectclass.contains(AttributeClass.OC_zimbraCOS)) {
                visitor.visit(new LdapCos(dn, attrs, prov));
            }
        }

    }

    /**
     *
     * @param base
     * @param filter
     * @param returnAttrs
     * @param opts
     * @param visitor
     * @return null if visitor is not null, List<NamedEntry> if visitor is null
     * @throws ServiceException
     */
    private List<NamedEntry> searchObjects(String[] bases, ZLdapFilter filter, String returnAttrs[],
            SearchDirectoryOptions opts, NamedEntry.Visitor visitor)
    throws ServiceException {

        if (visitor != null) {
            if (opts.getSortOpt() != SortOpt.NO_SORT) {
                throw ServiceException.INVALID_REQUEST("Sorting is not supported with visitor interface", null);
            }
            for (String base : bases) {
                searchLdapObjects(base, filter, returnAttrs, opts, visitor);
            }
            return null;
        } else {
            final List<NamedEntry> result = new ArrayList<NamedEntry>();

            NamedEntry.Visitor listBackedVisitor = new NamedEntry.Visitor() {
                @Override
                public void visit(NamedEntry entry) {
                    result.add(entry);
                }
            };

            for (String base : bases) {
                searchLdapObjects(base, filter, returnAttrs, opts, listBackedVisitor);
            }

            if (opts.getSortOpt() == SortOpt.NO_SORT) {
                return result;
            } else {
                NamedEntryComparator comparator =
                    new NamedEntryComparator(this,
                            opts.getSortAttr(), opts.getSortOpt()==SortOpt.SORT_ASCENDING);
                Collections.sort(result, comparator);
                return result;
            }
        }
    }

    private void searchLdapObjects(String base, ZLdapFilter filter, String returnAttrs[],
            SearchDirectoryOptions opts, NamedEntry.Visitor visitor)
    throws ServiceException {
        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.get(opts.getOnMaster()), opts.getUseConnPool(), LdapUsage.SEARCH);

            SearchObjectsVisitor searchObjectsVisitor =
                new SearchObjectsVisitor(this, zlc, visitor, opts.getMaxResults(),
                        opts.getMakeObjectOpt(), returnAttrs);

            SearchLdapOptions searchObjectsOptions = new SearchLdapOptions(base, filter,
                    returnAttrs, opts.getMaxResults(), null, ZSearchScope.SEARCH_SCOPE_SUBTREE,
                    searchObjectsVisitor);
            searchObjectsOptions.setUseControl(opts.isUseControl());
            searchObjectsOptions.setManageDSAit(opts.isManageDSAit());
            zlc.searchPaged(searchObjectsOptions);
        } catch (LdapSizeLimitExceededException e) {
            throw AccountServiceException.TOO_MANY_SEARCH_RESULTS("too many search results returned", e);
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to list all objects", e);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    /*
     * add required attrs to list of return attrs if not specified,
     * since we need them to construct an Entry
     *
     * TODO: return flags and use a cleaner API
     */
    private String[] fixReturnAttrs(String[] returnAttrs, int flags) {
        if (returnAttrs == null || returnAttrs.length == 0)
            return null;

        boolean needUID = true;
        boolean needID = true;
        boolean needCOSId = true;
        boolean needObjectClass = true;
        boolean needAliasTargetId = (flags & Provisioning.SD_ALIAS_FLAG) != 0;
        boolean needCalendarUserType = (flags & Provisioning.SD_CALENDAR_RESOURCE_FLAG) != 0;
        boolean needDomainName = true;
        boolean needZimbraACE = true;
        boolean needCn = ((flags & Provisioning.SD_COS_FLAG) != 0) || ((flags & Provisioning.SD_DYNAMIC_GROUP_FLAG) != 0);
        boolean needIsExternalVirtualAccount = (flags & Provisioning.SD_ACCOUNT_FLAG) != 0;
        boolean needExternalUserMailAddress = (flags & Provisioning.SD_ACCOUNT_FLAG) != 0;

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
            else if (Provisioning.A_zimbraIsExternalVirtualAccount.equalsIgnoreCase(returnAttrs[i]))
                needIsExternalVirtualAccount = false;
            else if (Provisioning.A_zimbraExternalUserMailAddress.equalsIgnoreCase(returnAttrs[i]))
                needExternalUserMailAddress = false;
        }

        int num = (needUID ? 1 : 0) +
                  (needID ? 1 : 0) +
                  (needCOSId ? 1 : 0) +
                  (needAliasTargetId ? 1 : 0) +
                  (needObjectClass ? 1 :0) +
                  (needCalendarUserType ? 1 : 0) +
                  (needDomainName ? 1 : 0) +
                  (needZimbraACE ? 1 : 0) +
                  (needCn ? 1 : 0) +
                  (needIsExternalVirtualAccount ? 1 : 0) +
                  (needExternalUserMailAddress ? 1 : 0);

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
        if (needIsExternalVirtualAccount) result[i++] = Provisioning.A_zimbraIsExternalVirtualAccount;
        if (needExternalUserMailAddress) result[i++] = Provisioning.A_zimbraExternalUserMailAddress;
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

    @Override
    public void addAlias(Account acct, String alias) throws ServiceException {
        addAliasInternal(acct, alias);
    }

    @Override
    public void removeAlias(Account acct, String alias) throws ServiceException {
        accountCache.remove(acct);
        removeAliasInternal(acct, alias);
    }

    @Override
    public void addAlias(DistributionList dl, String alias) throws ServiceException {
        addAliasInternal(dl, alias);
        allDLs.addGroup(dl);
    }

    @Override
    public void removeAlias(DistributionList dl, String alias) throws ServiceException {
        groupCache.remove(dl);
        removeAliasInternal(dl, alias);
        allDLs.removeGroup(alias);
    }

    private boolean isEntryAlias(ZAttributes attrs) throws ServiceException {

        Map<String, Object> entryAttrs = attrs.getAttrs();
        Object ocs = entryAttrs.get(Provisioning.A_objectClass);
        if (ocs instanceof String)
            return ((String)ocs).equalsIgnoreCase(AttributeClass.OC_zimbraAlias);
        else if (ocs instanceof String[]) {
            for (String oc : (String[])ocs) {
                if (oc.equalsIgnoreCase(AttributeClass.OC_zimbraAlias))
                    return true;
            }
        }
        return false;
    }

    private void addAliasInternal(NamedEntry entry, String alias) throws ServiceException {

        LdapUsage ldapUsage = null;

        String targetDomainName = null;
        AliasedEntry aliasedEntry = null;
        if (entry instanceof Account) {
            aliasedEntry = (AliasedEntry)entry;
            targetDomainName = ((Account)entry).getDomainName();
            ldapUsage = LdapUsage.ADD_ALIAS_ACCOUNT;
        } else if (entry instanceof Group) {
            aliasedEntry = (AliasedEntry)entry;
            ldapUsage = LdapUsage.ADD_ALIAS_DL;
            targetDomainName = ((Group)entry).getDomainName();
        } else {
            throw ServiceException.FAILURE("invalid entry type for alias", null);
        }

        alias = alias.toLowerCase().trim();
        alias = IDNUtil.toAsciiEmail(alias);

        validEmailAddress(alias);

        String parts[] = alias.split("@");
        String aliasName = parts[0];
        String aliasDomain = parts[1];

        ZLdapContext zlc = null;
        String aliasDn = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, ldapUsage);

            Domain domain = getDomainByAsciiName(aliasDomain, zlc);
            if (domain == null)
                throw AccountServiceException.NO_SUCH_DOMAIN(aliasDomain);

            aliasDn = mDIT.aliasDN(((LdapEntry)entry).getDN(), targetDomainName, aliasName, aliasDomain);
            // the create and addAttr ideally would be in the same transaction

            String aliasUuid = LdapUtil.generateUUID();
            String targetEntryId = entry.getId();
            try {
                zlc.createEntry(aliasDn, "zimbraAlias",
                    new String[] { Provisioning.A_uid, aliasName,
                                   Provisioning.A_zimbraId, aliasUuid,
                                   Provisioning.A_zimbraCreateTimestamp, LdapDateUtil.toGeneralizedTime(new Date()),
                                   Provisioning.A_zimbraAliasTargetId, targetEntryId} );
            } catch (LdapEntryAlreadyExistException e) {
                /*
                 * check if the alias is a dangling alias.  If so remove the dangling alias
                 * and create a new one.
                 */
                ZAttributes attrs = helper.getAttributes(zlc, aliasDn);

                // see if the entry is an alias
                if (!isEntryAlias(attrs))
                    throw e;

                Alias aliasEntry = makeAlias(aliasDn, attrs);
                NamedEntry targetEntry = searchAliasTarget(aliasEntry, false);
                if (targetEntry == null) {
                    // remove the dangling alias
                    try {
                        removeAliasInternal(null, alias);
                    } catch (ServiceException se) {
                        // ignore
                    }

                    // try creating the alias again
                    zlc.createEntry(aliasDn, "zimbraAlias",
                            new String[] {
                            Provisioning.A_uid, aliasName,
                            Provisioning.A_zimbraId, aliasUuid,
                            Provisioning.A_zimbraCreateTimestamp, LdapDateUtil.toGeneralizedTime(new Date()),
                            Provisioning.A_zimbraAliasTargetId, targetEntryId} );
                } else if (targetEntryId.equals(targetEntry.getId())) {
                    // the alias target points to this account/DL
                    Set<String> mailAliases = entry.getMultiAttrSet(Provisioning.A_zimbraMailAlias);
                    Set<String> mails = entry.getMultiAttrSet(Provisioning.A_mail);

                    if (mailAliases != null && mailAliases.contains(alias) &&
                        mails != null && mails.contains(alias)) {
                        throw e;
                    } else {
                        ZimbraLog.account.warn("alias entry exists at " + aliasDn +
                                ", but either mail or zimbraMailAlias of the target does not contain " +
                                alias + ", adding " + alias + " to entry " + entry.getName());
                    }
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
            removeExternalAddrsFromAllDynamicGroups(aliasedEntry.getAllAddrsSet(), zlc);
        } catch (LdapEntryAlreadyExistException nabe) {
            throw AccountServiceException.ACCOUNT_EXISTS(alias, aliasDn, nabe);
        } catch (LdapException e) {
            throw e;
        } catch (AccountServiceException e) {
            throw e;
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to create alias: "+e.getMessage(), e);
        } finally {
            LdapClient.closeContext(zlc);
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

        LdapUsage ldapUsage = null;
        if (entry instanceof Account) {
            ldapUsage = LdapUsage.REMOVE_ALIAS_ACCOUNT;
        } else if (entry instanceof Group) {
            ldapUsage = LdapUsage.REMOVE_ALIAS_DL;
        } else {
            ldapUsage = LdapUsage.REMOVE_ALIAS;
        }

        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, ldapUsage);

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
                if (entry instanceof Account) {
                    targetDomainName = ((Account)entry).getDomainName();
                } else if (entry instanceof Group) {
                    targetDomainName = ((Group)entry).getDomainName();
                } else {
                    throw ServiceException.INVALID_REQUEST("invalid entry type for alias", null);
                }
            }
            String aliasDn = mDIT.aliasDN(targetDn, targetDomainName, aliasName, aliasDomain);

            ZAttributes aliasAttrs = null;
            Alias aliasEntry = null;
            try {
                aliasAttrs = helper.getAttributes(zlc, aliasDn);

                // see if the entry is an alias
                if (!isEntryAlias(aliasAttrs))
                    throw AccountServiceException.NO_SUCH_ALIAS(alias);

                aliasEntry = makeAlias(aliasDn, aliasAttrs);
            } catch (ServiceException e) {
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
            if (!aliasPointsToOtherExistingEntry) {
                removeAddressFromAllDistributionLists(alias);
            }

            // 3. remove the alias entry
            if (aliasPointsToEntry || aliasPointsToNonExistingEntry) {
                try {
                    zlc.deleteEntry(aliasDn);
                } catch (ServiceException e) {
                    // should not happen, log it
                    ZimbraLog.account.warn("unable to remove alias entry at : " + aliasDn);
                }
            }

            // throw NO_SUCH_ALIAS if necessary
            if (((entry != null) && (aliasEntry == null)) ||
                ((entry != null) && (aliasEntry != null) && !aliasPointsToEntry))
                throw AccountServiceException.NO_SUCH_ALIAS(alias);

        } finally {
            LdapClient.closeContext(zlc);
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

        // see it's an account/cr
        target = get(AccountBy.id, targetId);
        if (target != null)
            return target;


        // see if it's a group
        target = getGroupBasic(Key.DistributionListBy.id, targetId);

        return target;
    }

    @Override
    public Domain createDomain(String name, Map<String, Object> domainAttrs)
    throws ServiceException {
        name = name.toLowerCase().trim();
        name = IDNUtil.toAsciiDomainName(name);

        NameUtil.validNewDomainName(name);

        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.CREATE_DOMAIN);

            LdapDomain d = (LdapDomain) getDomainByAsciiName(name, zlc);
            if (d != null) {
                throw AccountServiceException.DOMAIN_EXISTS(name);
            }

            // Attribute checking can not express "allow setting on
            // creation, but do not allow modifies afterwards"
            String domainType = (String) domainAttrs.get(A_zimbraDomainType);
            if (domainType == null) {
                domainType = DomainType.local.name();
            } else {
                domainAttrs.remove(A_zimbraDomainType); // add back later
            }

            String domainStatus = (String) domainAttrs.get(A_zimbraDomainStatus);
            if (domainStatus == null) {
                domainStatus = DOMAIN_STATUS_ACTIVE;
            } else {
                domainAttrs.remove(A_zimbraDomainStatus); // add back later
            }

            String smimeLdapURL = (String) domainAttrs.get(A_zimbraSMIMELdapURL);
            if (!StringUtil.isNullOrEmpty(smimeLdapURL)) {
                domainAttrs.remove(A_zimbraSMIMELdapURL); // add back later
            }
            String smimeLdapStartTlsEnabled = (String) domainAttrs.get(A_zimbraSMIMELdapStartTlsEnabled);
            if (!StringUtil.isNullOrEmpty(smimeLdapStartTlsEnabled)) {
                domainAttrs.remove(A_zimbraSMIMELdapStartTlsEnabled); // add back later
            }
            String smimeLdapBindDn = (String) domainAttrs.get(A_zimbraSMIMELdapBindDn);
            if (!StringUtil.isNullOrEmpty(smimeLdapBindDn)) {
                domainAttrs.remove(A_zimbraSMIMELdapBindDn); // add back later
            }
            String smimeLdapBindPassword = (String) domainAttrs.get(A_zimbraSMIMELdapBindPassword);
            if (!StringUtil.isNullOrEmpty(smimeLdapBindPassword)) {
                domainAttrs.remove(A_zimbraSMIMELdapBindPassword); // add back later
            }
            String smimeLdapSearchBase = (String) domainAttrs.get(A_zimbraSMIMELdapSearchBase);
            if (!StringUtil.isNullOrEmpty(smimeLdapSearchBase)) {
                domainAttrs.remove(A_zimbraSMIMELdapSearchBase); // add back later
            }
            String smimeLdapFilter = (String) domainAttrs.get(A_zimbraSMIMELdapFilter);
            if (!StringUtil.isNullOrEmpty(smimeLdapFilter)) {
                domainAttrs.remove(A_zimbraSMIMELdapFilter); // add back later
            }
            String smimeLdapAttribute = (String) domainAttrs.get(A_zimbraSMIMELdapAttribute);
            if (!StringUtil.isNullOrEmpty(smimeLdapAttribute)) {
                domainAttrs.remove(A_zimbraSMIMELdapAttribute); // add back later
            }

            CallbackContext callbackContext = new CallbackContext(CallbackContext.Op.CREATE);
            AttributeManager.getInstance().preModify(domainAttrs, null, callbackContext, true);

            // Add back attrs we circumvented from attribute checking
            domainAttrs.put(A_zimbraDomainType, domainType);
            domainAttrs.put(A_zimbraDomainStatus, domainStatus);
            domainAttrs.put(A_zimbraSMIMELdapURL, smimeLdapURL);
            domainAttrs.put(A_zimbraSMIMELdapStartTlsEnabled, smimeLdapStartTlsEnabled);
            domainAttrs.put(A_zimbraSMIMELdapBindDn, smimeLdapBindDn);
            domainAttrs.put(A_zimbraSMIMELdapBindPassword, smimeLdapBindPassword);
            domainAttrs.put(A_zimbraSMIMELdapSearchBase, smimeLdapSearchBase);
            domainAttrs.put(A_zimbraSMIMELdapFilter, smimeLdapFilter);
            domainAttrs.put(A_zimbraSMIMELdapAttribute, smimeLdapAttribute);

            String parts[] = name.split("\\.");
            String dns[] = mDIT.domainToDNs(parts);
            createParentDomains(zlc, parts, dns);

            ZMutableEntry entry = LdapClient.createMutableEntry();
            entry.mapToAttrs(domainAttrs);

            Set<String> ocs = LdapObjectClass.getDomainObjectClasses(this);
            entry.addAttr(A_objectClass, ocs);

            String zimbraIdStr = LdapUtil.generateUUID();
            entry.setAttr(A_zimbraId, zimbraIdStr);
            entry.setAttr(A_zimbraCreateTimestamp, LdapDateUtil.toGeneralizedTime(new Date()));
            entry.setAttr(A_zimbraDomainName, name);

            String mailStatus = (String) domainAttrs.get(A_zimbraMailStatus);
            if (mailStatus == null)
                entry.setAttr(A_zimbraMailStatus, MAIL_STATUS_ENABLED);

            if (domainType.equalsIgnoreCase(DomainType.alias.name())) {
                entry.setAttr(A_zimbraMailCatchAllAddress, "@" + name);
            }

            entry.setAttr(A_o, name+" domain");
            entry.setAttr(A_dc, parts[0]);

            String dn = dns[0];
            entry.setDN(dn);
            //NOTE: all four of these should be in a transaction...
            try {
                zlc.createEntry(entry);
            } catch (LdapEntryAlreadyExistException e) {
                zlc.replaceAttributes(dn, entry.getAttributes());
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
                zlc.createEntry(mDIT.domainDNToAccountBaseDN(dn),
                                 "organizationalRole",
                                 new String[] { A_ou, "people", A_cn, "people"});

                // create the base DN for dynamic groups
                zlc.createEntry(mDIT.domainDNToDynamicGroupsBaseDN(dn),
                        "organizationalRole",
                        new String[] { A_cn, "groups", A_description, "dynamic groups base"});
            }

            Domain domain = getDomainById(zimbraIdStr, zlc);

            AttributeManager.getInstance().postModify(domainAttrs, domain, callbackContext);
            return domain;

        } catch (LdapEntryAlreadyExistException nabe) {
            throw AccountServiceException.DOMAIN_EXISTS(name);
        } catch (LdapException e) {
            throw e;
        } catch (AccountServiceException e) {
            throw e;
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to create domain: "+name, e);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    private LdapDomain getDomainByQuery(ZLdapFilter filter, ZLdapContext initZlc)
    throws ServiceException {
        try {
            ZSearchResultEntry sr = helper.searchForEntry(mDIT.domainBaseDN(), filter, initZlc, false);
            if (sr != null) {
                return new LdapDomain(sr.getDN(), sr.getAttributes(), getConfig().getDomainDefaults(), this);
            }
        } catch (LdapMultipleEntriesMatchedException e) {
            throw AccountServiceException.MULTIPLE_DOMAINS_MATCHED("getDomainByQuery: " + e.getMessage());
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to lookup domain via query: " +
                    filter.toFilterString() + " message:"+e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Domain get(Key.DomainBy keyType, String key) throws ServiceException {
        return getDomain(keyType, key, false);
    }

    @Override
    public Domain getDomain(Key.DomainBy keyType, String key, boolean checkNegativeCache)
    throws ServiceException {

        // note: *always* use negative cache for keys from external source
        //       - virtualHostname, foreignName, krb5Realm

        GetFromDomainCacheOption option = checkNegativeCache ?
                GetFromDomainCacheOption.BOTH : GetFromDomainCacheOption.POSITIVE;

        switch(keyType) {
            case name:
                return getDomainByNameInternal(key, option);
            case id:
                return getDomainByIdInternal(key, null, option);
            case virtualHostname:
                return getDomainByVirtualHostnameInternal(key, GetFromDomainCacheOption.BOTH);
            case foreignName:
                return getDomainByForeignNameInternal(key, GetFromDomainCacheOption.BOTH);
            case krb5Realm:
                return getDomainByKrb5RealmInternal(key, GetFromDomainCacheOption.BOTH);
            default:
                return null;
        }
    }

    private Domain getFromCache(Key.DomainBy keyType, String key, GetFromDomainCacheOption option) {
        switch(keyType) {
            case name:
                String asciiName = IDNUtil.toAsciiDomainName(key);
                return domainCache.getByName(asciiName, option);
            case id:
                return domainCache.getById(key, option);
            case virtualHostname:
                return domainCache.getByVirtualHostname(key, option);
            case krb5Realm:
                return domainCache.getByKrb5Realm(key, option);
            default:
                return null;
        }
    }

    private Domain getDomainById(String zimbraId, ZLdapContext zlc) throws ServiceException {
        return getDomainByIdInternal(zimbraId, zlc, GetFromDomainCacheOption.POSITIVE);
    }

    private Domain getDomainByIdInternal(String zimbraId, ZLdapContext zlc, GetFromDomainCacheOption option)
    throws ServiceException {
        if (zimbraId == null) {
            return null;
        }

        Domain d = domainCache.getById(zimbraId, option);
        if (d instanceof DomainCache.NonExistingDomain) {
            return null;
        }

        LdapDomain domain = (LdapDomain)d;
        if (domain == null) {
            domain = getDomainByQuery(filterFactory.domainById(zimbraId), zlc);
            domainCache.put(Key.DomainBy.id, zimbraId, domain);
        }
        return domain;
    }

    /**
     * @return The Domain from the cache, if present.
     * @throws ServiceException
     */
    private Domain getDomainByIdFromCache(String zimbraId, ZLdapContext zlc, GetFromDomainCacheOption option) {
        if (zimbraId == null) {
            return null;
        }
        Domain d = domainCache.getById(zimbraId, option);
        if (d instanceof DomainCache.NonExistingDomain) {
            return null;
        }
        LdapDomain domain = (LdapDomain)d;
        return domain;
    }

    private Domain getDomainByNameInternal(String name, GetFromDomainCacheOption option)
    throws ServiceException {
        String asciiName = IDNUtil.toAsciiDomainName(name);
        return getDomainByAsciiNameInternal(asciiName, null, option);
    }

    private Domain getDomainByAsciiName(String name, ZLdapContext zlc)
    throws ServiceException {
        return getDomainByAsciiNameInternal(name, zlc, GetFromDomainCacheOption.POSITIVE);
    }

    private Domain getDomainByAsciiNameInternal(String name, ZLdapContext zlc,
            GetFromDomainCacheOption option)
    throws ServiceException {
        Domain d = domainCache.getByName(name, option);
        if (d instanceof DomainCache.NonExistingDomain)
            return null;

        LdapDomain domain = (LdapDomain)d;
        if (domain == null) {
            domain = getDomainByQuery(filterFactory.domainByName(name), zlc);
            domainCache.put(Key.DomainBy.name, name, domain);
        }
        return domain;
    }

    private Domain getDomainByVirtualHostnameInternal(String virtualHostname,
            GetFromDomainCacheOption option)
    throws ServiceException {
        Domain d = domainCache.getByVirtualHostname(virtualHostname, option);
        if (d instanceof DomainCache.NonExistingDomain)
            return null;

        LdapDomain domain = (LdapDomain)d;
        if (domain == null) {
            domain = getDomainByQuery(filterFactory.domainByVirtualHostame(virtualHostname), null);
            domainCache.put(Key.DomainBy.virtualHostname, virtualHostname, domain);
        }
        return domain;
    }

    private Domain getDomainByForeignNameInternal(String foreignName,
            GetFromDomainCacheOption option)
    throws ServiceException {
        Domain d = domainCache.getByForeignName(foreignName, option);
        if (d instanceof DomainCache.NonExistingDomain)
            return null;

        LdapDomain domain = (LdapDomain)d;
        if (domain == null) {
            domain = getDomainByQuery(filterFactory.domainByForeignName(foreignName), null);
            domainCache.put(Key.DomainBy.foreignName, foreignName, domain);
        }
        return domain;
    }

    private Domain getDomainByKrb5RealmInternal(String krb5Realm,
            GetFromDomainCacheOption option)
    throws ServiceException {
        Domain d = domainCache.getByKrb5Realm(krb5Realm, option);
        if (d instanceof DomainCache.NonExistingDomain)
            return null;

        LdapDomain domain = (LdapDomain)d;
        if (domain == null) {
            domain = getDomainByQuery(filterFactory.domainByKrb5Realm(krb5Realm), null);
            domainCache.put(Key.DomainBy.krb5Realm, krb5Realm, domain);
        }
        return domain;
    }

    private static final String [] ZIMBRA_ID_ATTR = { Provisioning.A_zimbraId };
    private static final Set<ObjectType> DOMAINS_OBJECT_TYPE = Sets.newHashSet(ObjectType.domains);

    @Override
    public List<Domain> getAllDomains() throws ServiceException {
        final List<Domain> result = new ArrayList<Domain>();
        AllDomainIdsCollector collector = new AllDomainIdsCollector();
        BySearchResultEntrySearcher searcher = new BySearchResultEntrySearcher(this, null, (Domain) null,
                ZIMBRA_ID_ATTR, collector);
        searcher.doSearch(filterFactory.allDomains(), DOMAINS_OBJECT_TYPE);
        List<String> cacheMisses = Lists.newArrayList();
        for (String zimbraId : collector.domains) {
            Domain cachedDom = getDomainByIdFromCache(zimbraId, null, GetFromDomainCacheOption.POSITIVE);
            if (cachedDom == null) {
                cacheMisses.add(zimbraId);
            } else {
                result.add(cachedDom);
            }
        }
        if (!cacheMisses.isEmpty()) {
            boolean cacheDomains = (LC.ldap_cache_domain_maxsize.intValue() >= collector.domains.size());
            if (!cacheDomains) {
                ZimbraLog.search.info(
                        "localconfig ldap_cache_domain_maxsize=%d < number of domains=%d.  Consider increasing it.",
                        LC.ldap_cache_domain_maxsize.intValue(), collector.domains.size());
            }
            getDomainsByIds(new DomainsByIdsVisitor(result, cacheDomains), cacheMisses, null);
        }
        return result;
    }

    private static class AllDomainIdsCollector implements BySearchResultEntrySearcher.SearchEntryProcessor {
        public final List<String> domains = Lists.newArrayList();

        @Override
        public void processSearchEntry(ZSearchResultEntry sr) {
            ZAttributes attrs = sr.getAttributes();
            try {
                domains.add(attrs.getAttrString(Provisioning.A_zimbraId));
            } catch (ServiceException e) {
                ZimbraLog.search.debug("Problem processing search result entry - ignoring", e);
                return;
            }
        }
    }

    private class DomainsByIdsVisitor implements NamedEntry.Visitor {
        private final List<Domain> result;
        private final boolean cacheResults;
        private DomainsByIdsVisitor(List<Domain> result, boolean cacheResults) {
            this.result = result;
            this.cacheResults = cacheResults;
        }
        @Override
        public void visit(NamedEntry entry) {
            result.add((LdapDomain)entry);
            if (cacheResults) {
                domainCache.put(Key.DomainBy.id, entry.getId(), (Domain)entry);
            }
        }
    };

    public void getDomainsByIds(NamedEntry.Visitor visitor, Collection<String> domains, String[] retAttrs)
    throws ServiceException {
        SearchDirectoryOptions opts = new SearchDirectoryOptions(retAttrs);
        opts.setFilter(filterFactory.domainsByIds(domains));
        opts.setTypes(ObjectType.domains);
        searchDirectoryInternal(opts, visitor);
    }

    @Override
    public void getAllDomains(NamedEntry.Visitor visitor, String[] retAttrs)
    throws ServiceException {
        SearchDirectoryOptions opts = new SearchDirectoryOptions(retAttrs);
        opts.setFilter(filterFactory.allDomains());
        opts.setTypes(ObjectType.domains);
        searchDirectoryInternal(opts, visitor);
    }

    private boolean domainDnExists(ZLdapContext zlc, String dn) throws ServiceException {
        try {
            ZSearchResultEnumeration ne = helper.searchDir(dn, filterFactory.domainLabel(),
                    ZSearchControls.SEARCH_CTLS_SUBTREE(), zlc, LdapServerType.MASTER);
            boolean result = ne.hasMore();
            ne.close();
            return result;
        } catch (ServiceException e) {  // or should we throw?  TODO
            return false;
        }
    }

    private void createParentDomains(ZLdapContext zlc, String parts[], String dns[])
    throws ServiceException {
        for (int i=dns.length-1; i > 0; i--) {
            if (!domainDnExists(zlc, dns[i])) {
                String dn = dns[i];
                String domain = parts[i];
                // don't create ZimbraDomain objects, since we don't want them to show up in list domains
                zlc.createEntry(dn, new String[] {"dcObject", "organization"},
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

    private Cos copyCos(String srcCosId, String destCosName, Map<String, Object> cosAttrs)
    throws ServiceException {
        destCosName = destCosName.toLowerCase().trim();

        Cos srcCos = getCosById(srcCosId, null);
        if (srcCos == null)
            throw AccountServiceException.NO_SUCH_COS(srcCosId);

        // bug 67716, use a case insensitive map because provided attr names may not be
        // the canonical name and that will cause multiple entries in the map
        Map<String, Object> allAttrs = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);

        allAttrs.putAll(srcCos.getAttrs());

        allAttrs.remove(Provisioning.A_objectClass);
        allAttrs.remove(Provisioning.A_zimbraId);
        allAttrs.remove(Provisioning.A_zimbraCreateTimestamp);
        allAttrs.remove(Provisioning.A_zimbraACE);
        allAttrs.remove(Provisioning.A_cn);
        allAttrs.remove(Provisioning.A_description);
        if (cosAttrs != null) {
            for (Map.Entry<String, Object> e : cosAttrs.entrySet()) {
                String attr = e.getKey();
                Object value = e.getValue();
                if (value instanceof String && Strings.isNullOrEmpty((String)value)) {
                    allAttrs.remove(attr);
                } else {
                    allAttrs.put(attr, value);
                }
            }
        }

        CallbackContext callbackContext = new CallbackContext(CallbackContext.Op.CREATE);

        //get rid of deprecated attrs
        Map<String, Object> allNewAttrs = new HashMap<String, Object>(allAttrs);
        for (String attr : allAttrs.keySet()) {
            AttributeInfo info = AttributeManager.getInstance().getAttributeInfo(attr);
            if (info != null && info.isDeprecated()) {
                allNewAttrs.remove(attr);
            }
        }
        allAttrs = allNewAttrs;
        AttributeManager.getInstance().preModify(allAttrs, null, callbackContext, true);

        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.CREATE_COS);

            ZMutableEntry entry = LdapClient.createMutableEntry();
            entry.mapToAttrs(allAttrs);

            Set<String> ocs = LdapObjectClass.getCosObjectClasses(this);
            entry.addAttr(A_objectClass, ocs);

            String zimbraIdStr = LdapUtil.generateUUID();
            entry.setAttr(A_zimbraId, zimbraIdStr);
            entry.setAttr(A_zimbraCreateTimestamp, LdapDateUtil.toGeneralizedTime(new Date()));
            entry.setAttr(A_cn, destCosName);
            String dn = mDIT.cosNametoDN(destCosName);
            entry.setDN(dn);
            zlc.createEntry(entry);

            Cos cos = getCosById(zimbraIdStr, zlc);
            AttributeManager.getInstance().postModify(allAttrs, cos, callbackContext);
            return cos;
        } catch (LdapEntryAlreadyExistException nabe) {
            throw AccountServiceException.COS_EXISTS(destCosName);
        } catch (LdapException e) {
            throw e;
        } catch (AccountServiceException e) {
            throw e;
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to create cos: " + destCosName, e);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    @Override
    public void renameCos(String zimbraId, String newName) throws ServiceException {
        LdapCos cos = (LdapCos) get(Key.CosBy.id, zimbraId);
        if (cos == null)
            throw AccountServiceException.NO_SUCH_COS(zimbraId);

        if (cos.isDefaultCos())
            throw ServiceException.INVALID_REQUEST("unable to rename default cos", null);

        newName = newName.toLowerCase().trim();
        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.RENAME_COS);
            String newDn = mDIT.cosNametoDN(newName);
            zlc.renameEntry(cos.getDN(), newDn);
            // remove old cos from cache
            cosCache.remove(cos);
        } catch (LdapEntryAlreadyExistException nabe) {
            throw AccountServiceException.COS_EXISTS(newName);
        } catch (LdapException e) {
            throw e;
        } catch (AccountServiceException e) {
            throw e;
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to rename cos: "+zimbraId, e);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    private LdapCos getCOSByQuery(ZLdapFilter filter, ZLdapContext initZlc)
    throws ServiceException {
        try {
            ZSearchResultEntry sr = helper.searchForEntry(mDIT.cosBaseDN(), filter, initZlc, false);
            if (sr != null) {
                return new LdapCos(sr.getDN(), sr.getAttributes(), this);
            }
        } catch (LdapMultipleEntriesMatchedException e) {
            throw AccountServiceException.MULTIPLE_ENTRIES_MATCHED("getCOSByQuery", e);
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to lookup cos via query: "+
                    filter.toFilterString() + " message:" + e.getMessage(), e);
        }
        return null;
    }

    private Cos getCosById(String zimbraId, ZLdapContext zlc) throws ServiceException {
        if (zimbraId == null)
            return null;

        LdapCos cos = cosCache.getById(zimbraId);
        if (cos == null) {
            cos = getCOSByQuery(filterFactory.cosById(zimbraId), zlc);
            cosCache.put(cos);
        }
        return cos;
    }

    @Override
    public Cos get(Key.CosBy keyType, String key) throws ServiceException {
        switch(keyType) {
            case name:
                return getCosByName(key, null);
            case id:
                return getCosById(key, null);
            default:
                    return null;
        }
    }

    private Cos getFromCache(Key.CosBy keyType, String key) {
        switch(keyType) {
            case name:
                return cosCache.getByName(key);
            case id:
                return cosCache.getById(key);
            default:
                return null;
        }
    }

    private Cos getCosByName(String name, ZLdapContext initZlc) throws ServiceException {
        LdapCos cos = cosCache.getByName(name);
        if (cos != null)
            return cos;

        try {
            String dn = mDIT.cosNametoDN(name);
            ZAttributes attrs = helper.getAttributes(
                    initZlc, LdapServerType.REPLICA, LdapUsage.GET_COS, dn, null);
            cos = new LdapCos(dn, attrs, this);
            cosCache.put(cos);
            return cos;
        } catch (LdapEntryNotFoundException e) {
            return null;
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to lookup COS by name: " + name +
                    " message: "+e.getMessage(), e);
        }
    }

    @Override
    public List<Cos> getAllCos() throws ServiceException {
        List<Cos> result = new ArrayList<Cos>();

        try {
            ZSearchResultEnumeration ne = helper.searchDir(mDIT.cosBaseDN(),
                    filterFactory.allCoses(), ZSearchControls.SEARCH_CTLS_SUBTREE());
            while (ne.hasMore()) {
                ZSearchResultEntry sr = ne.next();
                result.add(new LdapCos(sr.getDN(), sr.getAttributes(), this));
            }
            ne.close();
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to list all COS", e);
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
        if (aliases != null) {
            for (int i=0; i < aliases.length; i++) {
                try {
                    removeAlias(acc, aliases[i]); // this also removes each alias from any DLs
                } catch (ServiceException se) {
                    if (AccountServiceException.NO_SUCH_ALIAS.equals(se.getCode())) {
                        ZimbraLog.account.warn("got no such alias from removeAlias call when deleting account; likely alias was previously in a bad state");
                    } else {
                        throw se;
                    }
                }
            }
        }
        // delete all grants granted to the account
        try {
            RightCommand.revokeAllRights(this, GranteeType.GT_USER, zimbraId);
        } catch (ServiceException e) {
            // eat the exception and continue
            ZimbraLog.account.warn("cannot revoke grants", e);
        }
        // if ephemeral backend is not LDAP, need to explicitly delete ephemeral data
        EphemeralStore.Factory factory = EphemeralStore.getFactory();
        if (!(factory instanceof LdapEphemeralStore.Factory)) {
            factory.getStore().deleteData(new LdapEntryLocation(acc));
        }
        final Map<String, Object> attrs = new HashMap<String, Object>(acc.getAttrs());
        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.DELETE_ACCOUNT);

            zlc.deleteChildren(entry.getDN());
            zlc.deleteEntry(entry.getDN());
            validate(ProvisioningValidator.DELETE_ACCOUNT_SUCCEEDED, attrs);
            accountCache.remove(acc);
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to purge account: "+zimbraId, e);
        } finally {
            LdapClient.closeContext(zlc);
        }

    }

    @Override
    public void renameAccount(String zimbraId, String newName) throws ServiceException {
        newName = IDNUtil.toAsciiEmail(newName);
        validEmailAddress(newName);

        ZLdapContext zlc = null;

        Account acct = getAccountById(zimbraId, zlc, true);

        // prune cache
        accountCache.remove(acct);

        LdapEntry entry = (LdapEntry) acct;
        if (acct == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(zimbraId);
        String oldEmail = acct.getName();

        boolean domainChanged = false;
        Account oldAccount = acct;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.RENAME_ACCOUNT);

            String oldDn = entry.getDN();
            String[] parts = EmailUtil.getLocalPartAndDomain(oldEmail);
            String oldLocal = parts[0];
            String oldDomain = parts[1];

            newName = newName.toLowerCase().trim();
            parts = EmailUtil.getLocalPartAndDomain(newName);
            if (parts == null) {
                throw ServiceException.INVALID_REQUEST("bad value for newName", null);
            }
            String newLocal = parts[0];
            String newDomain = parts[1];

            Domain domain = getDomainByAsciiName(newDomain, zlc);
            if (domain == null) {
                throw AccountServiceException.NO_SUCH_DOMAIN(newDomain);
            }

            domainChanged = !newDomain.equals(oldDomain);

            if (domainChanged) {
                validate(ProvisioningValidator.RENAME_ACCOUNT, newName, acct.getMultiAttr(Provisioning.A_objectClass, false), acct.getAttrs(false));
                validate(ProvisioningValidator.RENAME_ACCOUNT_CHECK_DOMAIN_COS_AND_FEATURE, newName, acct.getAttrs(false));

                // make sure the new domain is a local domain
                if (!domain.isLocal()) {
                    throw ServiceException.INVALID_REQUEST("domain type must be local", null);
                }
            }

            String newDn = mDIT.accountDNRename(oldDn, newLocal, domain.getName());
            boolean dnChanged = (!newDn.equals(oldDn));

            Map<String,Object> newAttrs = acct.getAttrs(false);

            if (dnChanged) {
                // uid will be changed during renameEntry, so no need to modify it
                // OpenLDAP is OK modifying it, as long as it matches the new DN, but
                // InMemoryDirectoryServer does not like it.
                newAttrs.remove(Provisioning.A_uid);
            } else {
                newAttrs.put(Provisioning.A_uid, newLocal);
            }

            newAttrs.put(Provisioning.A_zimbraMailDeliveryAddress, newName);
            if (oldEmail.equals(newAttrs.get(
                    Provisioning.A_zimbraPrefFromAddress))) {
                newAttrs.put(Provisioning.A_zimbraPrefFromAddress, newName);
            }

            ReplaceAddressResult replacedMails = replaceMailAddresses(
                    acct, Provisioning.A_mail, oldEmail, newName);
            if (replacedMails.newAddrs().length == 0) {
                // Set mail to newName if the account currently does not have a mail
                newAttrs.put(Provisioning.A_mail, newName);
            } else {
                newAttrs.put(Provisioning.A_mail, replacedMails.newAddrs());
            }

            ReplaceAddressResult replacedAliases = replaceMailAddresses(
                    acct, Provisioning.A_zimbraMailAlias, oldEmail, newName);
            if (replacedAliases.newAddrs().length > 0) {
                newAttrs.put(Provisioning.A_zimbraMailAlias, replacedAliases.newAddrs());

                String newDomainDN = mDIT.domainToAccountSearchDN(newDomain);

                String[] aliasNewAddrs = replacedAliases.newAddrs();
                // check up front if any of renamed aliases already exists in the new domain
                // (if domain also got changed)
                if (domainChanged && addressExistsUnderDN(zlc, newDomainDN, aliasNewAddrs)) {
                    throw AccountServiceException.ALIAS_EXISTS(Arrays.toString(aliasNewAddrs));
                }

                // if any of the renamed aliases clashes with the account's new name,
                // it won't be caught by the above check, do a separate check.
                for (int i=0; i < aliasNewAddrs.length; i++) {
                    if (newName.equalsIgnoreCase(aliasNewAddrs[i])) {
                        throw AccountServiceException.ACCOUNT_EXISTS(newName);
                    }
                }
            }

            ReplaceAddressResult replacedAllowAddrForDelegatedSender =
                    replaceMailAddresses(acct, Provisioning.A_zimbraPrefAllowAddressForDelegatedSender,
                    oldEmail, newName);
            if (replacedAllowAddrForDelegatedSender.newAddrs().length > 0) {
                newAttrs.put(Provisioning.A_zimbraPrefAllowAddressForDelegatedSender,
                        replacedAllowAddrForDelegatedSender.newAddrs());
            }

            if (newAttrs.get(Provisioning.A_displayName) == null && oldLocal.equals(newAttrs.get(Provisioning.A_cn)))
                newAttrs.put(Provisioning.A_cn, newLocal);

            /*
            ZMutableEntry mutableEntry = LdapClient.createMutableEntry();
            mutableEntry.mapToAttrs(newAttrs);
            mutableEntry.setDN(newDn);
            */

            if (dnChanged) {
                zlc.renameEntry(oldDn, newDn);

                // re-get the acct object, make sure we don't get it from cache
                // Note: this account object contains the old address, it should never
                // be cached
                acct = getAccountByQuery(mDIT.mailBranchBaseDN(), filterFactory.accountById(zimbraId), zlc, true);
                if (acct == null) {
                    throw ServiceException.FAILURE("cannot find account by id after modrdn", null);
                }
            }

            // rename the account and all it's renamed aliases to the new name in all
            // distribution lists.  Doesn't throw exceptions, just logs
            renameAddressesInAllDistributionLists(oldEmail, newName, replacedAliases);

            // MOVE OVER ALL aliases
            // doesn't throw exceptions, just logs
            if (domainChanged) {
                moveAliases(zlc, replacedAliases, newDomain, null, oldDn, newDn, oldDomain, newDomain);
            }

            modifyLdapAttrs(acct, zlc, newAttrs);

            // re-get the account again, now attrs contains new addrs
            acct = getAccountByQuery(mDIT.mailBranchBaseDN(), filterFactory.accountById(zimbraId), zlc, true);
            if (acct == null) {
                throw ServiceException.FAILURE("cannot find account by id after modrdn", null);
            }
            removeExternalAddrsFromAllDynamicGroups(acct.getAllAddrsSet(), zlc);
        } catch (LdapEntryAlreadyExistException nabe) {
            throw AccountServiceException.ACCOUNT_EXISTS(newName);
        } catch (LdapException e) {
            throw e;
        } catch (AccountServiceException e) {
            throw e;
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to rename account: "+newName, e);
        } finally {
            LdapClient.closeContext(zlc);
            // prune cache
            accountCache.remove(oldAccount);
        }

        // reload it to cache using the master, bug 45736
        Account renamedAcct = getAccountById(zimbraId, null, true);

        if (domainChanged) {
            PermissionCache.invalidateCache(renamedAcct);
        }
    }

    @Override
    public Domain getDefaultZMGDomain() throws ServiceException {
        String domainId = getConfig().getMobileGatewayDefaultAppAccountDomainId();
        return domainId == null ? null : getDomainById(domainId);
    }

    private static final String ZMG_APP_CREDS_FOREIGN_PRINCIPAL_PREFIX = "zmgappcreds:";

    @Override
    public Account autoProvZMGAppAccount(String accountId, String appCredsDigest) throws ServiceException {
        Account acct = getAccountById(accountId);
        if (acct != null) {
            return acct;
        }
        Domain domain = getDefaultZMGDomain();
        if (domain == null) {
            ZimbraLog.account.info(A_zimbraMobileGatewayDefaultAppAccountDomainId + " has not been configured.");
            throw ServiceException.FAILURE("Missing server configuration", null);
        }

        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsMobileGatewayAppAccount, ProvisioningConstants.TRUE);
        attrs.put(Provisioning.A_zimbraId, accountId);
        attrs.put(Provisioning.A_zimbraForeignPrincipal, ZMG_APP_CREDS_FOREIGN_PRINCIPAL_PREFIX + appCredsDigest);
        attrs.put(Provisioning.A_zimbraHideInGal, ProvisioningConstants.TRUE);
        attrs.put(Provisioning.A_zimbraMailStatus, Provisioning.MailStatus.disabled.toString());
        attrs.put(Provisioning.A_zimbraMailHost, getLocalServer().getServiceHostname());
        return createDummyAccount(attrs, null, domain);
    }

    private Account createDummyAccount(Map<String, Object> attrs, String password, Domain domain)
            throws ServiceException {
        SecureRandom random = new SecureRandom();
        byte[] keyBytes = new byte[10];
        random.nextBytes(keyBytes);
        String dummyEmailAddr = String.valueOf(Hex.encodeHex(keyBytes)) + "@" + domain.getName();
        return createAccount(dummyEmailAddr, password, attrs);
    }

    private static final String ZMG_PROXY_ACCT_FOREIGN_PRINCIPAL_PREFIX = "zmgproxyacct:";

    @Override
    public Pair<Account, Boolean> autoProvZMGProxyAccount(String emailAddr, String password) throws ServiceException {
        Account acct = getAccountByForeignPrincipal(ZMG_PROXY_ACCT_FOREIGN_PRINCIPAL_PREFIX + emailAddr);
        if (acct != null) {
            return new Pair<>(acct, false);
        }
        String domainId = getConfig().getMobileGatewayDefaultProxyAccountDomainId();
        if (domainId == null) {
            return new Pair<>(null, false) ;
        }
        Domain domain = getDomainById(domainId);
        if (domain == null) {
            ZimbraLog.account.error("Domain corresponding to" + A_zimbraMobileGatewayDefaultProxyAccountDomainId + "not found ");
            throw ServiceException.FAILURE("Missing server configuration", null);
        }

        String testDsId = "TestId";
        Map<String, Object> testAttrs = getZMGProxyDataSourceAttrs(emailAddr, password, true, testDsId);
        DataSource dsToTest = new DataSource(null, DataSourceType.imap, "Test", testDsId, testAttrs, this);
        try {
            DataSourceManager.test(dsToTest);
        } catch (ServiceException e) {
            ZimbraLog.account.debug("ZMG Proxy account auto provisioning failed", e);
            throw AuthFailedServiceException.AUTH_FAILED(emailAddr, SystemUtil.getInnermostException(e).getMessage(), e);
        }

        Map<String, Object> acctAttrs = new HashMap<String, Object>();
        acctAttrs.put(Provisioning.A_zimbraIsMobileGatewayProxyAccount, ProvisioningConstants.TRUE);
        acctAttrs.put(Provisioning.A_zimbraForeignPrincipal, ZMG_PROXY_ACCT_FOREIGN_PRINCIPAL_PREFIX + emailAddr);
        acctAttrs.put(Provisioning.A_zimbraHideInGal, ProvisioningConstants.TRUE);
        acctAttrs.put(Provisioning.A_zimbraMailStatus, Provisioning.MailStatus.disabled.toString());
        acctAttrs.put(Provisioning.A_zimbraMailHost, getLocalServer().getServiceHostname());
        acct = createDummyAccount(acctAttrs, password, domain);

        DataSource ds;
        try {
            Map<String, Object> dsAttrs = getZMGProxyDataSourceAttrs(emailAddr, password, false, null);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
            dsAttrs.put(Provisioning.A_zimbraDataSourceFolderId,
                    Integer.toString(mbox.createFolder(null, "/" + emailAddr, new Folder.FolderOptions()).getId()));
            ds = createDataSource(acct, DataSourceType.imap, emailAddr, dsAttrs);
        } catch (ServiceException e) {
            try {
                deleteAccount(acct.getId());
            } catch (ServiceException e1) {
                if (!AccountServiceException.NO_SUCH_ACCOUNT.equals(e1.getCode())) {
                    throw e1;
                }
            }
            throw e;
        }

        DataSourceManager.asyncImportData(ds);

        return new Pair<>(acct, true);
    }

    private Map<String, Object> getZMGProxyDataSourceAttrs(String emailAddr, String password,
            boolean encryptPassword, String dsId) throws ServiceException {
        Map<String, Object> testAttrs = new HashMap<String, Object>();
        Config config = getConfig();
        testAttrs.put(Provisioning.A_zimbraDataSourceEnabled, LdapConstants.LDAP_TRUE);
        testAttrs.put(Provisioning.A_zimbraDataSourceIsZmgProxy, LdapConstants.LDAP_TRUE);
        testAttrs.put(Provisioning.A_zimbraDataSourceHost, config.getMobileGatewayProxyImapHost());
        testAttrs.put(Provisioning.A_zimbraDataSourcePort, Integer.toString(config.getMobileGatewayProxyImapPort()));
        testAttrs.put(Provisioning.A_zimbraDataSourceConnectionType, config.getMobileGatewayProxyImapConnectionType().toString());
        testAttrs.put(Provisioning.A_zimbraDataSourceUsername, emailAddr);
        testAttrs.put(Provisioning.A_zimbraDataSourcePassword, encryptPassword ?
                DataSource.encryptData(dsId, password) : password);
        testAttrs.put(Provisioning.A_zimbraDataSourceSmtpEnabled, LdapConstants.LDAP_TRUE);
        testAttrs.put(Provisioning.A_zimbraDataSourceSmtpHost, config.getMobileGatewayProxySmtpHost());
        testAttrs.put(Provisioning.A_zimbraDataSourceSmtpPort, Integer.toString(config.getMobileGatewayProxySmtpPort()));
        testAttrs.put(Provisioning.A_zimbraDataSourceSmtpConnectionType, config.getMobileGatewayProxySmtpConnectionType().toString());
        testAttrs.put(Provisioning.A_zimbraDataSourceSmtpAuthRequired, LdapConstants.LDAP_TRUE);
        return testAttrs;
    }

    private void deleteDomain(String zimbraId, boolean deleteDomainAliases) throws ServiceException {
        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.DELETE_DOMAIN);

            Domain domain = getDomainById(zimbraId, zlc);
            if (domain == null) {
                throw AccountServiceException.NO_SUCH_DOMAIN(zimbraId);
            }

            if (deleteDomainAliases) {
                List<String> aliasDomainIds = null;
                if (domain.isLocal()) {
                    aliasDomainIds = getEmptyAliasDomainIds(zlc, domain, true);
                }

               // delete all alias domains if any
                if (aliasDomainIds != null) {
                    for (String aliasDomainId : aliasDomainIds) {
                        deleteDomainInternal(zlc, aliasDomainId);
                    }
                }
            }
            // delete the domain;
            deleteDomainInternal(zlc, zimbraId);

        } catch (ServiceException e) {
            throw e;
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    @Override
    public void deleteDomain(String zimbraId) throws ServiceException {
        deleteDomain(zimbraId, true);
    }

	@Override
    public void deleteDomainAfterRename(String zimbraId) throws ServiceException {
        deleteDomain(zimbraId, false);
    }

    public List<String> getEmptyAliasDomainIds(ZLdapContext zlc, Domain targetDomain, boolean suboridinateCheck)
    throws ServiceException {
        List<String> aliasDomainIds = new ArrayList<String>();

        ZSearchResultEnumeration ne = null;
        try {
            ZSearchControls searchControls = ZSearchControls.createSearchControls(
                    ZSearchScope.SEARCH_SCOPE_SUBTREE, ZSearchControls.SIZE_UNLIMITED,
                    new String[]{Provisioning.A_zimbraId, Provisioning.A_zimbraDomainName});

            ne = helper.searchDir(mDIT.domainBaseDN(), filterFactory.domainAliases(targetDomain.getId()),
                    searchControls, zlc, LdapServerType.MASTER);

            while (ne.hasMore()) {
                ZSearchResultEntry sr = ne.next();

                String aliasDomainId = sr.getAttributes().getAttrString(Provisioning.A_zimbraId);
                String aliasDomainName = sr.getAttributes().getAttrString(Provisioning.A_zimbraDomainName);

                // make sure the alias domain is ready to be deleted
                String aliasDomainDn = sr.getDN();
                String acctBaseDn = mDIT.domainDNToAccountBaseDN(aliasDomainDn);
                String dynGroupsBaseDn = mDIT.domainDNToDynamicGroupsBaseDN(aliasDomainDn);

                if (suboridinateCheck && (hasSubordinates(zlc, acctBaseDn) || hasSubordinates(zlc, dynGroupsBaseDn))) {
                    throw ServiceException.FAILURE("alias domain " + aliasDomainName +
                            " of doamin " + targetDomain.getName() + " is not empty", null);
                }

                if (aliasDomainId != null) {
                    aliasDomainIds.add(aliasDomainId);
                }
            }
        } finally {
            ne.close();
        }

        return aliasDomainIds;
    }

    private boolean hasSubordinates(ZLdapContext zlc, String dn) throws ServiceException {
        boolean hasSubordinates = false;

        ZSearchResultEnumeration ne = null;
        try {
            ne = helper.searchDir(dn,
                    filterFactory.hasSubordinates(), ZSearchControls.SEARCH_CTLS_SUBTREE(),
                    zlc, LdapServerType.MASTER);
            hasSubordinates = ne.hasMore();
        } finally {
            if (ne != null) {
                ne.close();
            }
        }

        return hasSubordinates;
    }

    public void deleteDomainInternal(ZLdapContext zlc, String zimbraId) throws ServiceException {
        // TODO: should only allow a domain delete to succeed if there are no people
        // if there aren't, we need to delete the people trees first, then delete the domain.
        LdapDomain domain = null;
        String acctBaseDn = null;
        String dynGroupsBaseDn = null;
        try {
            domain = (LdapDomain) getDomainById(zimbraId, zlc);
            if (domain == null) {
                throw AccountServiceException.NO_SUCH_DOMAIN(zimbraId);
            }

            String name = domain.getName();

            // delete account base DN
            acctBaseDn = mDIT.domainDNToAccountBaseDN(domain.getDN());
            if (!acctBaseDn.equals(domain.getDN())) {
                try {
                    zlc.deleteEntry(acctBaseDn);
                } catch (LdapEntryNotFoundException e) {
                    ZimbraLog.account.info("entry %s not found", acctBaseDn);
                }
            }

            // delete dynamic groups base DN
            dynGroupsBaseDn = mDIT.domainDNToDynamicGroupsBaseDN(domain.getDN());
            if (!dynGroupsBaseDn.equals(domain.getDN())) {
                try {
                    zlc.deleteEntry(dynGroupsBaseDn);
                } catch (LdapEntryNotFoundException e) {
                    ZimbraLog.account.info("entry %s not found", dynGroupsBaseDn);
                }
            }

            try {
                zlc.deleteEntry(domain.getDN());
                domainCache.remove(domain);
            } catch (LdapContextNotEmptyException e) {
                // remove from cache before nuking all attrs
                domainCache.remove(domain);
                // assume subdomains exist and turn into plain dc object
                Map<String, String> attrs = new HashMap<String, String>();
                attrs.put("-"+A_objectClass, "zimbraDomain");
                // remove all zimbra attrs
                for (String key : domain.getAttrs(false).keySet()) {
                    if (key.startsWith("zimbra"))
                        attrs.put(key, "");
                }
                // cannot invoke callback here.  If another domain attr is added in a callback,
                // e.g. zimbraDomainStatus would add zimbraMailStatus, then we will get a LDAP
                // schema violation naming error(zimbraDomain is removed, thus there cannot be
                // any zimbraAttrs left) and the modify will fail.
                modifyAttrs(domain, attrs, false, false);
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
        } catch (LdapContextNotEmptyException e) {
            // get a few entries to include in the error message
            int maxEntriesToGet = 5;

            final String doNotReportThisDN = acctBaseDn;
            final StringBuilder sb = new StringBuilder();
            sb.append(" (remaining entries: ");

            SearchLdapOptions.SearchLdapVisitor visitor = new SearchLdapOptions.SearchLdapVisitor() {
                @Override
                public void visit(String dn, Map<String, Object> attrs, IAttributes ldapAttrs) {
                    if (!dn.equals(doNotReportThisDN)) {
                        sb.append("[" + dn + "] ");
                    }
                }
            };

            SearchLdapOptions searchOptions = new SearchLdapOptions(
                    acctBaseDn, filterFactory.anyEntry(),
                    new String[]{Provisioning.A_objectClass}, maxEntriesToGet, null,
                    ZSearchScope.SEARCH_SCOPE_SUBTREE, visitor);
            try {
                zlc.searchPaged(searchOptions);
            } catch (LdapSizeLimitExceededException lslee) {
                // quietly ignore
            } catch (ServiceException se) {
                ZimbraLog.account.warn("unable to get sample entries in non-empty domain "
                        + domain.getName() + " for reporting", se);
            }
            sb.append("...)");
            throw AccountServiceException.DOMAIN_NOT_EMPTY(domain.getName() + sb.toString(), e);
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to purge domain: "+zimbraId, e);
        }
    }

    @Override  // LdapProv
    public void renameDomain(String zimbraId, String newDomainName) throws ServiceException {
        newDomainName = newDomainName.toLowerCase().trim();
        newDomainName = IDNUtil.toAsciiDomainName(newDomainName);
        NameUtil.validNewDomainName(newDomainName);

        ZLdapContext zlc = null;

        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.RENAME_DOMAIN);

            RenameDomain.RenameDomainLdapHelper helper =
                new RenameDomain.RenameDomainLdapHelper(this, zlc) {

                private ZLdapContext toZLdapContext() {
                    return LdapClient.toZLdapContext(mProv, mZlc);
                }

                @Override
                public void createEntry(String dn, Map<String, Object> attrs)
                throws ServiceException {

                    ZMutableEntry entry = LdapClient.createMutableEntry();
                    entry.mapToAttrs(attrs);
                    entry.setDN(dn);

                    ZLdapContext ldapContext = toZLdapContext();
                    ldapContext.createEntry(entry);
                }

                @Override
                public void deleteEntry(String dn) throws ServiceException {
                    ZLdapContext ldapContext = toZLdapContext();
                    ldapContext.deleteEntry(dn);
                }

                @Override
                public void renameEntry(String oldDn, String newDn)
                throws ServiceException {
                    ZLdapContext ldapContext = toZLdapContext();
                    ldapContext.renameEntry(oldDn, newDn);
                }

                @Override
                public void searchDirectory(SearchDirectoryOptions options,
                        NamedEntry.Visitor visitor) throws ServiceException {
                    ((LdapProvisioning) mProv).searchDirectory(options, visitor);
                }

                @Override
                public void renameAddressesInAllDistributionLists(Map<String, String> changedPairs) {
                    ((LdapProvisioning) mProv).renameAddressesInAllDistributionLists(changedPairs);
                }

                @Override
                public void renameXMPPComponent(String zimbraId, String newName)
                throws ServiceException {
                    ((LdapProvisioning) mProv).renameXMPPComponent(zimbraId, newName);
                }

                @Override
                public Account getAccountById(String id) throws ServiceException {
                    // note: we do NOT want to get a cached entry
                    return ((LdapProvisioning) mProv).getAccountByQuery(
                        mProv.getDIT().mailBranchBaseDN(),
                        ZLdapFilterFactory.getInstance().accountById(id), toZLdapContext(), true);
                }

                @Override
                public DistributionList getDistributionListById(String id)
                throws ServiceException {
                    // note: we do NOT want to get a cahed entry
                    return ((LdapProvisioning) mProv).getDistributionListByQuery(
                            mDIT.mailBranchBaseDN(),
                            filterFactory.distributionListById(id),
                            toZLdapContext(), false);
                }

                @Override
                public DynamicGroup getDynamicGroupById(String id) throws ServiceException {
                    // note: we do NOT want to get a cahed entry
                    return ((LdapProvisioning) mProv).getDynamicGroupByQuery(
                            filterFactory.dynamicGroupById(id),
                            toZLdapContext(), false);
                }

                @Override
                public void modifyLdapAttrs(Entry entry,
                        Map<String, ? extends Object> attrs)
                        throws ServiceException {
                    ((LdapProvisioning) mProv).modifyLdapAttrs(entry, toZLdapContext(), attrs);
                }

            };

            Domain oldDomain = getDomainById(zimbraId, zlc);
            if (oldDomain == null)
               throw AccountServiceException.NO_SUCH_DOMAIN(zimbraId);

            RenameDomain rd = new RenameDomain(this, helper, oldDomain, newDomainName);
            rd.execute();
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    @Override
    public void deleteCos(String zimbraId) throws ServiceException {
        LdapCos c = (LdapCos) get(Key.CosBy.id, zimbraId);
        if (c == null)
            throw AccountServiceException.NO_SUCH_COS(zimbraId);

        if (c.isDefaultCos())
            throw ServiceException.INVALID_REQUEST("unable to delete default cos", null);

        // TODO: should we go through all accounts with this cos and remove the zimbraCOSId attr?
        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.DELETE_COS);
            zlc.deleteEntry(c.getDN());
            cosCache.remove(c);
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to purge cos: "+zimbraId, e);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    @Override
    public ShareLocator get(Key.ShareLocatorBy keyType, String key) throws ServiceException {
        switch(keyType) {
        case id:
            return getShareLocatorById(key, null, false);
        default:
                return null;
        }
    }

    @Override
    public ShareLocator createShareLocator(String id, Map<String, Object> attrs) throws ServiceException {
        CallbackContext callbackContext = new CallbackContext(CallbackContext.Op.CREATE);
        AttributeManager.getInstance().preModify(attrs, null, callbackContext, true);

        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.CREATE_SHARELOCATOR);

            ZMutableEntry entry = LdapClient.createMutableEntry();
            entry.mapToAttrs(attrs);

            Set<String> ocs = LdapObjectClass.getShareLocatorObjectClasses(this);
            entry.addAttr(A_objectClass, ocs);

            entry.setAttr(A_cn, id);
            String dn = mDIT.shareLocatorIdToDN(id);

            entry.setDN(dn);
            zlc.createEntry(entry);

            ShareLocator shloc = getShareLocatorById(id, zlc, true);
            AttributeManager.getInstance().postModify(attrs, shloc, callbackContext);
            return shloc;

        } catch (LdapEntryAlreadyExistException nabe) {
            throw AccountServiceException.SHARE_LOCATOR_EXISTS(id);
        } catch (LdapException e) {
            throw e;
        } catch (AccountServiceException e) {
            throw e;
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to create share locator: " + id, e);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    @Override
    public void deleteShareLocator(String id) throws ServiceException {
        LdapShareLocator shloc = (LdapShareLocator) get(Key.ShareLocatorBy.id, id);
        if (shloc == null)
            throw AccountServiceException.NO_SUCH_SHARE_LOCATOR(id);

        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.DELETE_SHARELOCATOR);
            zlc.deleteEntry(shloc.getDN());
            shareLocatorCache.remove(shloc);
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to delete share locator: "+id, e);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    @Override
    public Server createServer(String name, Map<String, Object> serverAttrs)
    throws ServiceException {
        name = name.toLowerCase().trim();

        CallbackContext callbackContext = new CallbackContext(CallbackContext.Op.CREATE);
        AttributeManager.getInstance().preModify(serverAttrs, null, callbackContext, true);

        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.CREATE_SERVER);

            ZMutableEntry entry = LdapClient.createMutableEntry();
            entry.mapToAttrs(serverAttrs);

            Set<String> ocs = LdapObjectClass.getServerObjectClasses(this);
            entry.addAttr(A_objectClass, ocs);

            String zimbraIdStr = LdapUtil.generateUUID();
            entry.setAttr(A_zimbraId, zimbraIdStr);
            entry.setAttr(A_zimbraCreateTimestamp, LdapDateUtil.toGeneralizedTime(new Date()));
            entry.setAttr(A_cn, name);
            String dn = mDIT.serverNameToDN(name);

            if (!entry.hasAttribute(Provisioning.A_zimbraServiceHostname)) {
                entry.setAttr(Provisioning.A_zimbraServiceHostname, name);
            }

            entry.setDN(dn);
            zlc.createEntry(entry);

            Server server = getServerById(zimbraIdStr, zlc, true);
            AttributeManager.getInstance().postModify(serverAttrs, server, callbackContext);
            return server;

        } catch (LdapEntryAlreadyExistException nabe) {
            throw AccountServiceException.SERVER_EXISTS(name);
        } catch (LdapException e) {
            throw e;
        } catch (AccountServiceException e) {
            throw e;
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to create server: " + name, e);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    private Server getServerByQuery(ZLdapFilter filter, ZLdapContext initZlc)
    throws ServiceException {
        try {
            ZSearchResultEntry sr = helper.searchForEntry(mDIT.serverBaseDN(), filter, initZlc, false);
            if (sr != null) {
                return new LdapServer(sr.getDN(), sr.getAttributes(), getConfig().getServerDefaults(), this);
            }
        } catch (LdapMultipleEntriesMatchedException e) {
            throw AccountServiceException.MULTIPLE_ENTRIES_MATCHED("getServerByQuery", e);
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to lookup server via query: "+
                    filter.toFilterString() + " message:" + e.getMessage(), e);
        }
        return null;
    }

    private Server getServerById(String zimbraId, ZLdapContext zlc, boolean nocache)
    throws ServiceException {
        if (zimbraId == null)
            return null;
        Server s = null;
        if (!nocache)
            s = serverCache.getById(zimbraId);
        if (s == null) {
            s = getServerByQuery(filterFactory.serverById(zimbraId), zlc);
            serverCache.put(s);
        }
        return s;
    }

    private AlwaysOnCluster getAlwaysOnClusterByQuery(ZLdapFilter filter, ZLdapContext initZlc)
    throws ServiceException {
        try {
            ZSearchResultEntry sr = helper.searchForEntry(mDIT.alwaysOnClusterBaseDN(), filter, initZlc, false);
            if (sr != null) {
                return new LdapAlwaysOnCluster(sr.getDN(), sr.getAttributes(), null, this);
            }
        } catch (LdapMultipleEntriesMatchedException e) {
            throw AccountServiceException.MULTIPLE_ENTRIES_MATCHED("getAlwaysOnClusterByQuery", e);
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to lookup alwaysOnCluster via query: "+
                    filter.toFilterString() + " message:" + e.getMessage(), e);
        }
        return null;
    }

    private AlwaysOnCluster getAlwaysOnClusterById(String zimbraId, ZLdapContext zlc, boolean nocache)
    throws ServiceException {
        if (zimbraId == null)
            return null;
        AlwaysOnCluster c = null;
        if (!nocache)
            c = alwaysOnClusterCache.getById(zimbraId);
        if (c == null) {
            c = getAlwaysOnClusterByQuery(filterFactory.alwaysOnClusterById(zimbraId), zlc);
            alwaysOnClusterCache.put(c);
        }
        return c;
    }

    private ShareLocator getShareLocatorByQuery(ZLdapFilter filter, ZLdapContext initZlc)
    throws ServiceException {
        try {
            ZSearchResultEntry sr = helper.searchForEntry(mDIT.shareLocatorBaseDN(), filter, initZlc, false);
            if (sr != null) {
                return new LdapShareLocator(sr.getDN(), sr.getAttributes(), this);
            }
        } catch (LdapMultipleEntriesMatchedException e) {
            throw AccountServiceException.MULTIPLE_ENTRIES_MATCHED("getShareLocatorByQuery", e);
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to lookup share locator via query: "+
                    filter.toFilterString() + " message:" + e.getMessage(), e);
        }
        return null;
    }

    private ShareLocator getShareLocatorById(String id, ZLdapContext zlc, boolean nocache)
    throws ServiceException {
        if (id == null)
            return null;
        ShareLocator shloc = null;
        if (!nocache)
            shloc = shareLocatorCache.getById(id);
        if (shloc == null) {
            shloc = getShareLocatorByQuery(filterFactory.shareLocatorById(id), zlc);
            shareLocatorCache.put(shloc);
        }
        return shloc;
    }

    @Override
    public Server get(Key.ServerBy keyType, String key) throws ServiceException {
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
            Server s = serverCache.getByName(name);
            if (s != null)
                return s;
        }

        try {
            String dn = mDIT.serverNameToDN(name);
            ZAttributes attrs = helper.getAttributes(LdapUsage.GET_SERVER, dn);
            LdapServer s = new LdapServer(dn, attrs, getConfig().getServerDefaults(), this);
            serverCache.put(s);
            return s;
        } catch (LdapEntryNotFoundException e) {
            return null;
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to lookup server by name: "+name+" message: "+e.getMessage(), e);
        }
    }

    private AlwaysOnCluster getAlwaysOnClusterByNameInternal(String name) throws ServiceException {
        return getAlwaysOnClusterByName(name, false);
    }

    private AlwaysOnCluster getAlwaysOnClusterByName(String name, boolean nocache) throws ServiceException {
        if (!nocache) {
            AlwaysOnCluster c = alwaysOnClusterCache.getByName(name);
            if (c != null)
                return c;
        }

        try {
            String dn = mDIT.alwaysOnClusterNameToDN(name);
            ZAttributes attrs = helper.getAttributes(LdapUsage.GET_ALWAYSONCLUSTER, dn);
            LdapAlwaysOnCluster c = new LdapAlwaysOnCluster(dn, attrs, null, this);
            alwaysOnClusterCache.put(c);
            return c;
        } catch (LdapEntryNotFoundException e) {
            return null;
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to lookup alwaysOnCluster by name: "+name+" message: "+e.getMessage(), e);
        }
    }

    @Override
    public List<Server> getAllServers() throws ServiceException {
        return getAllServers((String)null);
    }

    @Override
    public List<Server> getAllServers(String service) throws ServiceException {
        List<Server> result = new ArrayList<Server>();

        ZLdapFilter filter;
        if (service != null) {
            filter = filterFactory.serverByService(service);
        } else {
            filter = filterFactory.allServers();
        }

        try {
            Map<String, Object> serverDefaults = getConfig().getServerDefaults();

            ZSearchResultEnumeration ne = helper.searchDir(mDIT.serverBaseDN(),
                    filter, ZSearchControls.SEARCH_CTLS_SUBTREE());
            while (ne.hasMore()) {
                ZSearchResultEntry sr = ne.next();
                LdapServer s = new LdapServer(sr.getDN(), sr.getAttributes(),
                        serverDefaults, this);
                result.add(s);
            }
            ne.close();
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to list all servers", e);
        }

        if (result.size() > 0)
            serverCache.put(result, true);
        Collections.sort(result);
        return result;
    }

    @Override
    public List<Server> getAllServers(String service, String clusterId) throws ServiceException {
        List<Server> result = new ArrayList<Server>();

        ZLdapFilter filter = filterFactory.serverByServiceAndAlwaysOnCluster(service, clusterId);

        try {
            Map<String, Object> serverDefaults = getConfig().getServerDefaults();

            ZSearchResultEnumeration ne = helper.searchDir(mDIT.serverBaseDN(),
                    filter, ZSearchControls.SEARCH_CTLS_SUBTREE());
            while (ne.hasMore()) {
                ZSearchResultEntry sr = ne.next();
                LdapServer s = new LdapServer(sr.getDN(), sr.getAttributes(),
                        serverDefaults, this);
                result.add(s);
            }
            ne.close();
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to list all servers by cluster id", e);
        }

        Collections.sort(result);
        return result;
    }

    @Override
    public AlwaysOnCluster createAlwaysOnCluster(String name, Map<String, Object> clusterAttrs)
    throws ServiceException {
        name = name.toLowerCase().trim();

        CallbackContext callbackContext = new CallbackContext(CallbackContext.Op.CREATE);
        AttributeManager.getInstance().preModify(clusterAttrs, null, callbackContext, true);

        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.CREATE_SERVER);

            ZMutableEntry entry = LdapClient.createMutableEntry();
            entry.mapToAttrs(clusterAttrs);

            Set<String> ocs = LdapObjectClass.getAlwaysOnClusterObjectClasses(this);
            entry.addAttr(A_objectClass, ocs);

            String zimbraIdStr = LdapUtil.generateUUID();
            entry.setAttr(A_zimbraId, zimbraIdStr);
            entry.setAttr(A_zimbraCreateTimestamp, LdapDateUtil.toGeneralizedTime(new Date()));
            entry.setAttr(A_cn, name);
            String dn = mDIT.alwaysOnClusterNameToDN(name);
            entry.setDN(dn);
            zlc.createEntry(entry);

            AlwaysOnCluster cluster = getAlwaysOnClusterById(zimbraIdStr, zlc, true);
            AttributeManager.getInstance().postModify(clusterAttrs, cluster, callbackContext);
            return cluster;

        } catch (LdapEntryAlreadyExistException nabe) {
            throw AccountServiceException.ALWAYSONCLUSTER_EXISTS(name);
        } catch (LdapException e) {
            throw e;
        } catch (AccountServiceException e) {
            throw e;
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to create akwaysOnCluster: " + name, e);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    @Override
    public List<AlwaysOnCluster> getAllAlwaysOnClusters() throws ServiceException {
        List<AlwaysOnCluster> result = new ArrayList<AlwaysOnCluster>();

        ZLdapFilter filter = filterFactory.allAlwaysOnClusters();

        try {

            ZSearchResultEnumeration ne = helper.searchDir(mDIT.alwaysOnClusterBaseDN(),
                    filter, ZSearchControls.SEARCH_CTLS_SUBTREE());
            while (ne.hasMore()) {
                ZSearchResultEntry sr = ne.next();
                LdapAlwaysOnCluster c = new LdapAlwaysOnCluster(sr.getDN(), sr.getAttributes(),
                        null, this);
                result.add(c);
            }
            ne.close();
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to list all alwaysOnClusters", e);
        }

        if (result.size() > 0)
            alwaysOnClusterCache.put(result, true);
        Collections.sort(result);
        return result;
    }

    private AlwaysOnCluster getAlwaysOnClusterByIdInternal(String zimbraId) throws ServiceException {
        return getAlwaysOnClusterById(zimbraId, null, false);
    }

    @Override
    public AlwaysOnCluster get(Key.AlwaysOnClusterBy keyType, String key) throws ServiceException {
        switch(keyType) {
            case id:
                return getAlwaysOnClusterByIdInternal(key);
            case name:
                return getAlwaysOnClusterByNameInternal(key);
            default:
                    return null;
        }
    }

    private List<Cos> searchCOS(ZLdapFilter filter, ZLdapContext initZlc)
    throws ServiceException {
        List<Cos> result = new ArrayList<Cos>();
        try {
            ZSearchResultEnumeration ne = helper.searchDir(mDIT.cosBaseDN(), filter,
                    ZSearchControls.SEARCH_CTLS_SUBTREE(), initZlc, LdapServerType.REPLICA);
            while (ne.hasMore()) {
                ZSearchResultEntry sr = ne.next();
                result.add(new LdapCos(sr.getDN(), sr.getAttributes(), this));
            }
            ne.close();
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to lookup cos via query: "+
                    filter.toFilterString() + " message: " + e.getMessage(), e);
        }
        return result;
    }

    private void removeServerFromAllCOSes(String serverId, String serverName, ZLdapContext initZlc) {
        List<Cos> coses = null;
        try {
            coses = searchCOS(filterFactory.cosesByMailHostPool(serverId), initZlc);
            for (Cos cos: coses) {
                Map<String, String> attrs = new HashMap<String, String>();
                attrs.put("-"+Provisioning.A_zimbraMailHostPool, serverId);
                ZimbraLog.account.info("Removing " + Provisioning.A_zimbraMailHostPool + " " +
                        serverId + "(" + serverName + ") from cos " + cos.getName());
                modifyAttrs(cos, attrs);
                // invalidate cached cos
                cosCache.remove((LdapCos)cos);
            }
        } catch (ServiceException se) {
            ZimbraLog.account.warn("unable to remove "+serverId+" from all COSes ", se);
            return;
        }

     }

    private void removeAttrsForServer(String[] attributes, String serverName) throws ServiceException {
        // Remove attrs from global config
        Map<String, Object> configAttrs = new HashMap<String, Object>();
        for (String attribute: attributes) {
            StringUtil.addToMultiMap(configAttrs, "-" + attribute, serverName);
        }
        modifyAttrs(getConfig(), configAttrs);
        // Remove attrs from all proxy servers
        for (Server server: getAllServers(Provisioning.SERVICE_PROXY)) {
            Map<String, Object> serverAttrs = new HashMap<String, Object>();
            for (String attribute: attributes) {
                StringUtil.addToMultiMap(serverAttrs, "-" + attribute, serverName);
            }
            modifyAttrs(server, serverAttrs);
        }
     }

    private static class CountingVisitor extends SearchLdapVisitor {
        long numAccts = 0;

        CountingVisitor() {
            super(false);
        }

        @Override
        public void visit(String dn, IAttributes ldapAttrs) {
            numAccts++;
        }

        long getNumAccts() {
            return numAccts;
        }
    };

    private long getNumAccountsOnServer(Server server) throws ServiceException {
        ZLdapFilter filter = filterFactory.accountsHomedOnServer(server.getServiceHostname());
        String base = mDIT.mailBranchBaseDN();
        String attrs[] = new String[] {Provisioning.A_zimbraId};

        CountingVisitor visitor = new CountingVisitor();

        searchLdapOnMaster(base, filter, attrs, visitor);

        return visitor.getNumAccts();
    }

    @Override
    public void deleteServer(String zimbraId) throws ServiceException {
        LdapServer server = (LdapServer) getServerByIdInternal(zimbraId);
        if (server == null)
            throw AccountServiceException.NO_SUCH_SERVER(zimbraId);

        // check that no account is still on this server
        long numAcctsOnServer = getNumAccountsOnServer(server);
        if (numAcctsOnServer != 0) {
            throw ServiceException.INVALID_REQUEST("There are " + numAcctsOnServer +
                    " account(s) on this server.", null);
        }

        String monitorHost = getConfig().getAttr(Provisioning.A_zimbraLogHostname);
        String serverName = server.getName();
        if (monitorHost != null && monitorHost.trim().equals(serverName)) {
            throw ServiceException.INVALID_REQUEST("zimbraLogHostname is set to " + serverName, null);
        }
        String[] attributesToRemove = {
            Provisioning.A_zimbraReverseProxyAvailableLookupTargets,
            Provisioning.A_zimbraReverseProxyUpstreamEwsServers,
            Provisioning.A_zimbraReverseProxyUpstreamLoginServers
        };
        removeAttrsForServer(attributesToRemove, server.getName());

        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.DELETE_SERVER);
            removeServerFromAllCOSes(zimbraId, server.getName(), zlc);
            zlc.deleteEntry(server.getDN());
            serverCache.remove(server);
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to purge server: "+zimbraId, e);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    @Override
    public void deleteAlwaysOnCluster(String zimbraId) throws ServiceException {
        LdapAlwaysOnCluster cluster = (LdapAlwaysOnCluster) getAlwaysOnClusterByIdInternal(zimbraId);
        if (cluster == null)
            throw AccountServiceException.NO_SUCH_ALWAYSONCLUSTER(zimbraId);

        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.DELETE_ALWAYSONCLUSTER);
            zlc.deleteEntry(cluster.getDN());
            alwaysOnClusterCache.remove(cluster);
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to purge alwaysOnCluster: "+zimbraId, e);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    /*
     *  Distribution lists.
     */
    @Override
    public DistributionList createDistributionList(String listAddress,
            Map<String, Object> listAttrs)
    throws ServiceException {
        return createDistributionList(listAddress, listAttrs, null);
    }

    private DistributionList createDistributionList(String listAddress,
            Map<String, Object> listAttrs, Account creator)
    throws ServiceException {

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

        CallbackContext callbackContext = new CallbackContext(CallbackContext.Op.CREATE);
        callbackContext.setCreatingEntryName(listAddress);
        AttributeManager.getInstance().preModify(listAttrs, null, callbackContext, true);

        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.CREATE_DISTRIBUTIONLIST);

            Domain d = getDomainByAsciiName(domain, zlc);
            if (d == null)
                throw AccountServiceException.NO_SUCH_DOMAIN(domain);

            if (!d.isLocal()) {
                throw ServiceException.INVALID_REQUEST("domain type must be local", null);
            }

            ZMutableEntry entry = LdapClient.createMutableEntry();
            entry.mapToAttrs(listAttrs);

            Set<String> ocs = LdapObjectClass.getDistributionListObjectClasses(this);
            entry.addAttr(A_objectClass, ocs);

            String zimbraIdStr = LdapUtil.generateUUID();
            entry.setAttr(A_zimbraId, zimbraIdStr);
            entry.setAttr(A_zimbraCreateTimestamp, LdapDateUtil.toGeneralizedTime(new Date()));
            entry.setAttr(A_mail, listAddress);

            // unlike accounts (which have a zimbraMailDeliveryAddress for the primary,
            // and zimbraMailAliases only for aliases), DLs use zibraMailAlias for both.
            // Postfix uses these two attributes to route mail, and zimbraMailDeliveryAddress
            // indicates that something has a physical mailbox, which DLs don't.
            entry.setAttr(A_zimbraMailAlias, listAddress);

            // by default a distribution list is always created enabled
            if (!entry.hasAttribute(Provisioning.A_zimbraMailStatus)) {
                entry.setAttr(A_zimbraMailStatus, MAIL_STATUS_ENABLED);
            }

            String displayName = entry.getAttrString(Provisioning.A_displayName);
            if (displayName != null) {
                entry.setAttr(A_cn, displayName);
            }

            entry.setAttr(A_uid, localPart);

            setGroupHomeServer(entry, creator);

            String dn = mDIT.distributionListDNCreate(baseDn, entry.getAttributes(), localPart, domain);
            entry.setDN(dn);

            zlc.createEntry(entry);

            DistributionList dlist = getDLBasic(DistributionListBy.id, zimbraIdStr, zlc);

            if (dlist != null) {
                AttributeManager.getInstance().postModify(listAttrs, dlist, callbackContext);
                removeExternalAddrsFromAllDynamicGroups(dlist.getAllAddrsSet(), zlc);
                allDLs.addGroup(dlist);
            } else {
                throw ServiceException.FAILURE("unable to get distribution list after creating LDAP entry: "+
                        listAddress, null);
            }
            return dlist;

        } catch (LdapEntryAlreadyExistException nabe) {
            throw AccountServiceException.DISTRIBUTION_LIST_EXISTS(listAddress);
        } catch (LdapException e) {
            throw e;
        } catch (AccountServiceException e) {
            throw e;
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to create distribution list: "+listAddress, e);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    @Override
    public List<DistributionList> getDistributionLists(DistributionList list,
            boolean directOnly, Map<String, String> via) throws ServiceException {
        return getContainingDistributionLists(list, directOnly, via);
    }

    private DistributionList getDistributionListByQuery(String base, ZLdapFilter filter,
            ZLdapContext initZlc, boolean basicAttrsOnly) throws ServiceException {

        String[] returnAttrs = basicAttrsOnly ? BASIC_DL_ATTRS : null;

        DistributionList dl = null;
        try {
            ZSearchControls searchControls = ZSearchControls.createSearchControls(
                    ZSearchScope.SEARCH_SCOPE_SUBTREE, ZSearchControls.SIZE_UNLIMITED, returnAttrs);

            ZSearchResultEnumeration ne = helper.searchDir(base, filter,
                    searchControls, initZlc, LdapServerType.REPLICA);
            if (ne.hasMore()) {
                ZSearchResultEntry sr = ne.next();
                dl = makeDistributionList(sr.getDN(), sr.getAttributes(), basicAttrsOnly);
            }
            ne.close();
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to lookup distribution list via query: "+
                    filter.toFilterString() + " message: "+e.getMessage(), e);
        }
        return dl;
    }

    @Override
    public void renameDistributionList(String zimbraId, String newEmail) throws ServiceException {
        newEmail = IDNUtil.toAsciiEmail(newEmail);
        validEmailAddress(newEmail);

        boolean domainChanged = false;
        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.RENAME_DISTRIBUTIONLIST);

            LdapDistributionList dl = (LdapDistributionList) getDistributionListById(zimbraId, zlc);
            if (dl == null) {
                throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(zimbraId);
            }

            groupCache.remove(dl);

            String oldEmail = dl.getName();
            String oldDomain = EmailUtil.getValidDomainPart(oldEmail);

            newEmail = newEmail.toLowerCase().trim();
            String[] parts = EmailUtil.getLocalPartAndDomain(newEmail);
            if (parts == null)
                throw ServiceException.INVALID_REQUEST("bad value for newName", null);
            String newLocal = parts[0];
            String newDomain = parts[1];

            domainChanged = !oldDomain.equals(newDomain);

            Domain domain = getDomainByAsciiName(newDomain, zlc);
            if (domain == null) {
                throw AccountServiceException.NO_SUCH_DOMAIN(newDomain);
            }

            if (domainChanged) {
                // make sure the new domain is a local domain
                if (!domain.isLocal()) {
                    throw ServiceException.INVALID_REQUEST("domain type must be local", null);
                }
            }

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
                if (domainChanged && addressExistsUnderDN(zlc, newDomainDN, replacedAliases.newAddrs())) {
                    throw AccountServiceException.DISTRIBUTION_LIST_EXISTS(newEmail);
                }
            }

            ReplaceAddressResult replacedAllowAddrForDelegatedSender =
                replaceMailAddresses(dl, Provisioning.A_zimbraPrefAllowAddressForDelegatedSender,
                oldEmail, newEmail);
            if (replacedAllowAddrForDelegatedSender.newAddrs().length > 0) {
                attrs.put(Provisioning.A_zimbraPrefAllowAddressForDelegatedSender,
                        replacedAllowAddrForDelegatedSender.newAddrs());
            }

            String oldDn = dl.getDN();
            String newDn = mDIT.distributionListDNRename(oldDn, newLocal, domain.getName());
            boolean dnChanged = (!oldDn.equals(newDn));

            if (dnChanged) {
                // uid will be changed during renameEntry, so no need to modify it
                // OpenLDAP is OK modifying it, as long as it matches the new DN, but
                // InMemoryDirectoryServer does not like it.
                attrs.remove(A_uid);
            } else {
                /*
                 * always reset uid to the local part, because in non default DIT the naming RDN might not
                 * be uid, and ctxt.rename won't change the uid to the new localpart in that case.
                 */
                attrs.put(A_uid, newLocal);
            }

            // move over the distribution list entry
            if (dnChanged) {
                zlc.renameEntry(oldDn, newDn);
            }

            dl = (LdapDistributionList) getDistributionListById(zimbraId, zlc);

            // rename the distribution list and all it's renamed aliases to the new name
            // in all distribution lists.
            // Doesn't throw exceptions, just logs.
            renameAddressesInAllDistributionLists(oldEmail, newEmail, replacedAliases);

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
                throw e;
            }

            removeExternalAddrsFromAllDynamicGroups(dl.getAllAddrsSet(), zlc);

        } catch (LdapEntryAlreadyExistException nabe) {
            throw AccountServiceException.DISTRIBUTION_LIST_EXISTS(newEmail);
        } catch (LdapException e) {
            throw e;
        } catch (AccountServiceException e) {
            throw e;
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to rename distribution list: " + zimbraId, e);
        } finally {
            LdapClient.closeContext(zlc);
        }

        if (domainChanged) {
            PermissionCache.invalidateCache();
        }
    }

    @Override
    public DistributionList get(Key.DistributionListBy keyType, String key)
    throws ServiceException {
        switch(keyType) {
            case id:
                return getDistributionListByIdInternal(key);
            case name:
                DistributionList dl = getDistributionListByNameInternal(key);
                if (dl == null) {
                    String localDomainAddr = getEmailAddrByDomainAlias(key);
                    if (localDomainAddr != null) {
                        dl = getDistributionListByNameInternal(localDomainAddr);
                    }
                }
                return dl;
            default:
                return null;
        }
    }

    private DistributionList getDistributionListById(String zimbraId, ZLdapContext zlc)
    throws ServiceException {
        return getDistributionListByQuery(mDIT.mailBranchBaseDN(),
                                          filterFactory.distributionListById(zimbraId),
                                          zlc, false);
    }

    private DistributionList getDistributionListByIdInternal(String zimbraId)
    throws ServiceException {
        return getDistributionListById(zimbraId, null);
    }

    @Override
    public void deleteDistributionList(String zimbraId) throws ServiceException {
        LdapDistributionList dl = (LdapDistributionList) getDistributionListByIdInternal(zimbraId);
        if (dl == null) {
            throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(zimbraId);
        }

        deleteDistributionList(dl);
    }

    private void deleteDistributionList(LdapDistributionList dl) throws ServiceException {
        String zimbraId = dl.getId();

        // make a copy of all addrs of this DL, after the delete all aliases on this dl
        // object will be gone, but we need to remove them from the allgroups cache after the DL is deleted
        Set<String> addrs = new HashSet<String>(dl.getMultiAttrSet(Provisioning.A_mail));

        // remove the DL from all DLs
        removeAddressFromAllDistributionLists(dl.getName()); // this doesn't throw any exceptions

        // delete all aliases of the DL
        String aliases[] = dl.getAliases();
        if (aliases != null) {
            String dlName = dl.getName();
            for (int i=0; i < aliases.length; i++) {
                // the primary name shows up in zimbraMailAlias on the entry, don't bother to remove
                // this "alias" if it is the primary name, the entire entry will be deleted anyway.
                if (!dlName.equalsIgnoreCase(aliases[i])) {
                    removeAlias(dl, aliases[i]); // this also removes each alias from any DLs
                }
            }
        }

        // delete all grants granted to the DL
        try {
             RightCommand.revokeAllRights(this, GranteeType.GT_GROUP, zimbraId);
        } catch (ServiceException e) {
            // eat the exception and continue
            ZimbraLog.account.warn("cannot revoke grants", e);
        }

        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.DELETE_DISTRIBUTIONLIST);
            zlc.deleteEntry(dl.getDN());
            groupCache.remove(dl);
            allDLs.removeGroup(addrs);
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to purge distribution list: "+zimbraId, e);
        } finally {
            LdapClient.closeContext(zlc);
        }

        PermissionCache.invalidateCache();
    }

    private DistributionList getDistributionListByNameInternal(String listAddress)
    throws ServiceException {
        listAddress = IDNUtil.toAsciiEmail(listAddress);

        return getDistributionListByQuery(mDIT.mailBranchBaseDN(),
                filterFactory.distributionListByName(listAddress), null, false);
    }

    @Override
    public boolean isDistributionList(String addr) {
        boolean isDL = allDLs.isGroup(addr);
        if (!isDL) {
            try {
                addr = getEmailAddrByDomainAlias(addr);
                if (addr != null) {
                    isDL = allDLs.isGroup(addr);
                }
            } catch (ServiceException e) {
                ZimbraLog.account.warn("unable to get local domain address of " + addr, e);
            }
        }
        return isDL;
    }

    private Group getGroupFromCache(Key.DistributionListBy keyType, String key) {
        switch(keyType) {
        case id:
            return groupCache.getById(key);
        case name:
            return groupCache.getByName(key);
        default:
            return null;
        }
    }

    private void putInGroupCache(Group group) {
        groupCache.put(group);
    }

    private DistributionList getDLFromCache(Key.DistributionListBy keyType, String key) {
        Group group =  getGroupFromCache(keyType, key);
        if (group instanceof DistributionList) {
            return (DistributionList) group;
        } else {
            return null;
        }
    }

    void removeGroupFromCache(Key.DistributionListBy keyType, String key) {
        Group group = getGroupFromCache(keyType, key);
        if (group != null) {
            removeFromCache(group);
        }
    }

    // filter out non-admin groups from an AclGroups instance
    private GroupMembership getAdminAclGroups(GroupMembership aclGroups) {
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

        return new GroupMembership(groups, groupIds);
    }

    /**
     * @return distribution lists and both custom and normal dynamic groups the account is a member of
     */
    @Override
    public GroupMembership getGroupMembership(Account acct, boolean adminGroupsOnly)
    throws ServiceException {
        EntryCacheDataKey cacheKey = adminGroupsOnly ?
                EntryCacheDataKey.GROUPEDENTRY_MEMBERSHIP_ADMINS_ONLY :
                    EntryCacheDataKey.GROUPEDENTRY_MEMBERSHIP;

        GroupMembership cacheableGroups = (GroupMembership)acct.getCachedData(cacheKey);
        if (cacheableGroups == null) {
            cacheableGroups = setupGroupedEntryCacheData(acct, adminGroupsOnly);
        }
        return cacheableGroups.clone();
    }

    /**
     * Note this is over-ridden in CustomLdapProvisioning to always return an empty list
     */
    public GroupMembership getCustomDynamicGroupMembership(Account acct, boolean adminGroupsOnly)
    throws ServiceException {
        GroupMembership groups = new GroupMembership();
        return LdapDynamicGroup.updateGroupMembershipForCustomDynamicGroups(this,
            groups, acct, (Domain) null, adminGroupsOnly);
    }

    /**
     * @param rights - the rights to check.  null or empty means "any rights"
     * @return Groups which {@code acct} is a member of which have been granted one or more or the {@code rights}
     */
    @Override
    public GroupMembership getGroupMembershipWithRights(Account acct, Set<Right> rights, boolean adminGroupsOnly)
    throws ServiceException {
        SearchForGroupsWithRightsVisitor visitor = new SearchForGroupsWithRightsVisitor(rights);
        Set<String> groupsWithRights = searchForGroupsWhichUseRights(getDIT().zimbraBaseDN(), rights, visitor);
        if (groupsWithRights.size() == 0) {
            return new GroupMembership();
        }
        GroupMembership membership = getGroupMembership(acct, adminGroupsOnly);
        return restrictMembershipByIds(membership, groupsWithRights);
    }

    private GroupMembership restrictMembershipByIds(GroupMembership groups, Collection<String> ids) {
        GroupMembership membership = new GroupMembership();
        if (ids.size() == 0) {
            return membership;
        }
        for (MemberOf memberOf : groups.memberOf()) {
            if (ids.contains(memberOf.getId())) {
                membership.append(memberOf, memberOf.getId());
            }
        }
        return membership;
    }

    /**
     * Visits entries which have zimbraACE
     * Mine search results for groups which have been assigned certain rights
     */
    private static class SearchForGroupsWithRightsVisitor extends SearchLdapVisitor {
        private final Set<String> results = Sets.newHashSet(); // group IDs
        private final Set<String> rightNames;

        SearchForGroupsWithRightsVisitor(Set<Right> rights) {
            if (rights == null || rights.isEmpty()) {
                rightNames = null;
            } else {
                rightNames = Sets.newHashSet();
                for (Right right :rights) {
                    rightNames.add(right.getName());
                }
            }
        }

        @Override
        public void visit(String dn, Map<String, Object> attrs, IAttributes ldapAttrs) {
            String[] zimbraACE = getMultiAttrString(attrs, Provisioning.A_zimbraACE);
            if (zimbraACE != null) {
                for (String ace : zimbraACE) {
                    // expect form : <grantee> <granteeType> <right>
                    String[] components = ace.split(" ");
                    if (components.length < 3) {
                        continue; // unexpected
                    }
                    if ("grp".equals(components[1])) {
                        if ((rightNames == null) || rightNames.contains(components[2])) {
                            results.add(components[0]);
                        }
                    }
                }
            }
        }

        private String[] getMultiAttrString(Map<String, Object> attrs, String attrName) {
            Object obj = attrs.get(attrName);
            if (obj instanceof String) {
                return new String[] {(String) obj};
            } else {
                return (String[]) obj;
            }
        }
    }

    /**
     * @param base - search base
     * @param rights - the rights to search for.  null or empty implies ALL rights
     * @return the IDs of the groups found
     * @throws ServiceException
     */
    private Set<String> searchForGroupsWhichUseRights(String base, Set<Right> rights,
            SearchForGroupsWithRightsVisitor visitor)
    throws ServiceException {
        String[] fetchAttrs = { LdapProvisioning.A_zimbraACE };
        StringBuilder query = new StringBuilder("(|");
        if ((rights == null) || rights.isEmpty()) {
            query.append('(').append(Provisioning.A_zimbraACE).append('=').append("* grp *)");
        } else {
            for (Right right : rights) {
                query.append('(').append(Provisioning.A_zimbraACE).append('=').append("* grp ")
                .append(right.getName()).append(")");
            }
        }
        query.append(")");

        searchLdapOnReplica(base, query.toString(), fetchAttrs, visitor);
        return Collections.unmodifiableSet(visitor.results);
    }

    private GroupMembership setupGroupedEntryCacheData(Account acct, boolean adminGroupsOnly)
    throws ServiceException {
        //
        // static groups
        //
        GroupMembership groups = new GroupMembership();
        DistributionList.updateGroupMembership(this, (ZLdapContext) null, groups, acct, null /* via */,
                false /* adminGroupsOnly */, false /* directOnly */);
        //
        // append non-custom dynamic groups
        //
        Set<String>allGroupIDs = getAllContainingDynamicGroupIDs(acct);
        groups = LdapDynamicGroup.updateGroupMembershipForDynamicGroups(this, groups, acct, allGroupIDs,
            false /* adminGroupsOnly - false because need them all for the cache */,
            false /* customGroupsOnly */, true /* nonCustomGroupsOnly */);

        GroupMembership customMembership = getCustomDynamicGroupMembership(acct, false /* need them all for cache */);
        groups.mergeFrom(customMembership);

        // cache it
        acct.setCachedData(EntryCacheDataKey.GROUPEDENTRY_MEMBERSHIP, groups);

        // filter out non-admin groups
        if (adminGroupsOnly) {
            groups = getAdminAclGroups(groups);
            acct.setCachedData(EntryCacheDataKey.GROUPEDENTRY_MEMBERSHIP_ADMINS_ONLY, groups);
        }
        return groups;
    }

    @Override
    public GroupMembership getGroupMembership(DistributionList dl, boolean adminGroupsOnly)
    throws ServiceException {

        EntryCacheDataKey cacheKey = adminGroupsOnly ?
                EntryCacheDataKey.GROUPEDENTRY_MEMBERSHIP_ADMINS_ONLY :
                EntryCacheDataKey.GROUPEDENTRY_MEMBERSHIP;

        GroupMembership groups = (GroupMembership)dl.getCachedData(cacheKey);
        if (groups != null) {
            return groups;
        }

        groups = new GroupMembership();
        DistributionList.updateGroupMembership(this, (ZLdapContext) null, groups, dl, null /* via */,
                false /* adminGroupsOnly */, false /* directOnly */);

        dl.setCachedData(EntryCacheDataKey.GROUPEDENTRY_MEMBERSHIP, groups);

        if (adminGroupsOnly) {
            groups = getAdminAclGroups(groups); // filter out non-admin groups
            dl.setCachedData(EntryCacheDataKey.GROUPEDENTRY_MEMBERSHIP_ADMINS_ONLY, groups);
        }

        return groups;
    }

    @Override
    public Server getLocalServer() throws ServiceException {
        return getLocalServer(false /* allowAbsent */);
    }

    @Override
    public Server getLocalServerIfDefined() {
        try {
            return getLocalServer(true /* allowAbsent */);
        } catch (Exception e) {
            return null;
        }
    }

    private Server getLocalServer(boolean allowAbsent) throws ServiceException {
        String hostname = LC.zimbra_server_hostname.value();
        if (hostname == null) {
            Zimbra.halt("zimbra_server_hostname not specified in localconfig.xml");
        }
        Server local = getServerByNameInternal(hostname);
        if (allowAbsent) {
            return local;
        }
        if (local == null) {
            Zimbra.halt("Could not find an LDAP entry for server '" + hostname + "'");
        }
        return local;
    }

    public static final long TIMESTAMP_WINDOW = Constants.MILLIS_PER_MINUTE * 5;

    private void checkAccountStatus(Account acct, Map<String, Object> authCtxt)
    throws ServiceException {
        checkAccountStatus(acct, authCtxt, Boolean.FALSE);
    }

    private void checkAccountStatus(Account acct, Map<String, Object> authCtxt, boolean ignoreLockout)
    throws ServiceException {
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
        if (!onLocalServer(acct)) {
            reload(acct, false);  // reload from the replica
        }

        String accountStatus = acct.getAccountStatus(Provisioning.getInstance());
        if (accountStatus == null) {
            throw AuthFailedServiceException.AUTH_FAILED(acct.getName(),
                    AuthMechanism.namePassedIn(authCtxt), "missing account status");
        }

        if (accountStatus.equals(Provisioning.ACCOUNT_STATUS_MAINTENANCE)) {
            throw AccountServiceException.MAINTENANCE_MODE();
        }

        if (!ignoreLockout && !(accountStatus.equals(Provisioning.ACCOUNT_STATUS_ACTIVE) ||
              accountStatus.equals(Provisioning.ACCOUNT_STATUS_LOCKOUT))) {
            throw AuthFailedServiceException.AUTH_FAILED(acct.getName(),
                    AuthMechanism.namePassedIn(authCtxt), "account(or domain) status is " +
                    accountStatus);
        }
    }

    @Override
    public void preAuthAccount(Account acct, String acctValue, String acctBy,
            long timestamp, long expires, String preAuth, Map<String, Object> authCtxt)
    throws ServiceException {
        preAuthAccount(acct, acctValue, acctBy, timestamp, expires, preAuth, false, authCtxt);
    }

    @Override
    public void preAuthAccount(Account acct, String acctValue, String acctBy,
            long timestamp, long expires, String preAuth, boolean admin,
            Map<String, Object> authCtxt)
    throws ServiceException {
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

    @Override
    public void preAuthAccount(Domain domain, String acctValue, String acctBy,
            long timestamp, long expires, String preAuth, Map<String, Object> authCtxt)
    throws ServiceException {
        verifyPreAuth(domain, null, acctValue, acctBy, timestamp, expires, preAuth, false, authCtxt);
        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "PreAuth","account", acctValue}));
    }

    private void verifyPreAuth(Account acct, String acctValue, String acctBy, long timestamp, long expires,
            String preAuth, boolean admin, Map<String, Object> authCtxt) throws ServiceException {
        checkAccountStatus(acct, authCtxt);
        verifyPreAuth(Provisioning.getInstance().getDomain(acct), acct.getName(),
                acctValue, acctBy, timestamp, expires, preAuth, admin,authCtxt);
    }

    void verifyPreAuth(Domain domain, String acctNameForLogging,
            String acctValue, String acctBy, long timestamp, long expires,
            String preAuth, boolean admin, Map<String, Object> authCtxt)
    throws ServiceException {

         if (preAuth == null || preAuth.length() == 0)
            throw ServiceException.INVALID_REQUEST("preAuth must not be empty", null);

         if (acctNameForLogging == null) {
             acctNameForLogging = acctValue;
         }

        // see if domain is configured for preauth
        String domainPreAuthKey = domain.getAttr(Provisioning.A_zimbraPreAuthKey, null);
        if (domainPreAuthKey == null)
            throw ServiceException.INVALID_REQUEST("domain is not configured for preauth", null);

        // see if request is recent
        long now = System.currentTimeMillis();
        long diff = Math.abs(now-timestamp);
        if (diff > TIMESTAMP_WINDOW) {
            Date nowDate = new Date(now);
            Date preauthDate = new Date(timestamp);
            throw AuthFailedServiceException.AUTH_FAILED(acctNameForLogging,
                    AuthMechanism.namePassedIn(authCtxt),
                    "preauth timestamp is too old, server time: " + nowDate.toString() +
                    ", preauth timestamp: " + preauthDate.toString());
        }

        // compute expected preAuth
        HashMap<String,String> params = new HashMap<String,String>();
        params.put("account", acctValue);
        if (admin) params.put("admin", "1");
        params.put("by", acctBy);
        params.put("timestamp", timestamp+"");
        params.put("expires", expires+"");
        String computedPreAuth = PreAuthKey.computePreAuth(params, domainPreAuthKey);
        if (!computedPreAuth.equalsIgnoreCase(preAuth)) {
            throw AuthFailedServiceException.AUTH_FAILED(acctNameForLogging,
                    AuthMechanism.namePassedIn(authCtxt), "preauth mismatch");
        }

    }

    private void preAuth(Account acct, String acctValue, String acctBy, long timestamp, long expires,
            String preAuth, boolean admin, Map<String, Object> authCtxt) throws ServiceException {

        LdapLockoutPolicy lockoutPolicy = new LdapLockoutPolicy(this, acct);
        try {
            if (lockoutPolicy.isLockedOut()) {
                throw AuthFailedServiceException.AUTH_FAILED(acct.getName(),
                        AuthMechanism.namePassedIn(authCtxt), "account lockout");
            }

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
    public void authAccount(Account acct, String password, AuthContext.Protocol proto)
    throws ServiceException {
        authAccount(acct, password, proto, null);
    }

    @Override
    public void authAccount(Account acct, String password, AuthContext.Protocol proto,
            Map<String, Object> authCtxt)
    throws ServiceException {
        try {
            if (password == null || password.equals("")) {
                throw AuthFailedServiceException.AUTH_FAILED(acct.getName(),
                        AuthMechanism.namePassedIn(authCtxt), "empty password");
            }

            if (authCtxt == null)
                authCtxt = new HashMap<String, Object>();

            // add proto to the auth context
            authCtxt.put(AuthContext.AC_PROTOCOL, proto);

            if (isRecoveryCodeBasedAuth(authCtxt)) {
                recoveryCodeBasedAuthAccount(acct, password, authCtxt);
            } else {
                authAccount(acct, password, true, authCtxt);
            }

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

    private void authAccount(Account acct, String password,
            boolean checkPasswordPolicy, Map<String, Object> authCtxt)
    throws ServiceException {
        checkAccountStatus(acct, authCtxt);

        AuthMechanism authMech = AuthMechanism.newInstance(acct, authCtxt);
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
                    if ((last+(Constants.MILLIS_PER_DAY * maxAge)) < curr)
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

    private void recoveryCodeBasedAuthAccount(Account acct, String recoveryCode, Map<String, Object> authCtxt)
            throws ServiceException {
        checkAccountStatus(acct, authCtxt, Boolean.TRUE);
        ResetPasswordUtil.validateFeatureResetPasswordStatus(acct);
        verifyPassword(acct, recoveryCode, null, authCtxt);
        acct.unsetResetPasswordRecoveryCode();
        if (AccountStatus.lockout.equals(acct.getAccountStatus())) {
            acct.setAccountStatus(AccountStatus.active);
            acct.unsetPasswordLockoutLockedTime();
            ZimbraLog.security.info(ZimbraLog
                    .encodeAttrs(new String[] { "cmd", "Auth", "account", acct.getName(), "mode", "recoveryCode - account unlocked" }));
        } else {
            ZimbraLog.security.info(ZimbraLog
                    .encodeAttrs(new String[] { "cmd", "Auth", "account", acct.getName(), "mode", "recoveryCode" }));
        }
        updateLastLogon(acct);
    }

    private void reportException(Account acct, String message, Map<String, Object> authCtxt)
            throws AuthFailedServiceException {
        ZimbraLog.security.warn(
                ZimbraLog.encodeAttrs(new String[] { "cmd", "Auth", "account", acct.getName(), "error", message }));
        throw AuthFailedServiceException.AUTH_FAILED(acct.getName(), AuthMechanism.namePassedIn(authCtxt), message);
    }

    private boolean isRecoveryCodeBasedAuth(Map<String, Object> authCtxt) {
        return authCtxt == null ? false : AuthMode.RECOVERY_CODE == authCtxt.get(Provisioning.AUTH_MODE_KEY);
    }

    private void verifyRecoveryCode(Account acct, String recoveryCode, Map<String, Object> authCtxt) throws ServiceException {
        Map<String, String> codeMap = JWEUtil.getDecodedJWE(acct.getResetPasswordRecoveryCode());
        String lockoutMsg = AccountStatus.lockout.equals(acct.getAccountStatus()) ? " - account lockout" : "";
        if (codeMap == null) {
            reportException(acct, "recovery code not found" + lockoutMsg, authCtxt);
        }
        String code = codeMap.get(CodeConstants.CODE.toString());
        if (recoveryCode.equals(code)) {
            String expiryTimeStr = codeMap.get(CodeConstants.EXPIRY_TIME.toString());
            if (StringUtils.isEmpty(expiryTimeStr)) {
                reportException(acct, "invalid recovery code map, expiry time not found" + lockoutMsg, authCtxt);
            }
            long expiryTime = Long.parseLong(expiryTimeStr);
            Date now = new Date();
            if (expiryTime < now.getTime()) {
                reportException(acct, "recovery code expired" + lockoutMsg, authCtxt);
            }
        } else {
            reportException(acct, "recovery code mismatch" + lockoutMsg, authCtxt);
        }
    }

    @Override
    public void accountAuthed(Account acct) throws ServiceException {
        updateLastLogon(acct);
    }

    @Override
    public void ssoAuthAccount(Account acct, AuthContext.Protocol proto,
            Map<String, Object> authCtxt)
    throws ServiceException {
        try {
            ssoAuth(acct, authCtxt);
            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "SSOAuth","account", acct.getName(), "protocol", proto.toString()}));
        } catch (AuthFailedServiceException e) {
            ZimbraLog.security.warn(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "SSOAuth","account", acct.getName(), "protocol", proto.toString(), "error", e.getMessage() + e.getReason(", %s")}));
            throw e;
        } catch (ServiceException e) {
            ZimbraLog.security.warn(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "SSOAuth","account", acct.getName(), "protocol", proto.toString(), "error", e.getMessage()}));
            throw e;
        }
    }

    private void ssoAuth(Account acct, Map<String, Object> authCtxt) throws ServiceException {

        checkAccountStatus(acct, authCtxt);

        LdapLockoutPolicy lockoutPolicy = new LdapLockoutPolicy(this, acct);
        try {
            if (lockoutPolicy.isLockedOut()) {
                throw AuthFailedServiceException.AUTH_FAILED(acct.getName(),
                        AuthMechanism.namePassedIn(authCtxt), "account lockout");
            }

            // yes, SSO can unlock the acount
            lockoutPolicy.successfulLogin();
        } catch (AccountServiceException e) {
            lockoutPolicy.failedLogin();
            throw e;
        }

        updateLastLogon(acct);
    }

    private void updateLastLogon(Account acct) throws ServiceException {
        if (EphemeralStore.getFactory() instanceof LdapEphemeralStore.Factory) {
            Config config = Provisioning.getInstance().getConfig();
            long freq = config.getLastLogonTimestampFrequency();
            // never update timestamp if frequency is 0
            if (freq == 0) {
                return;
            }
            Date lastLogon = acct.getLastLogonTimestamp();
            // don't update if duration specified in zimbraLastLogonTimestampFrequency
            // hasn't passed since the last timestamp was logged
            if (lastLogon != null && (lastLogon.getTime() + freq > System.currentTimeMillis())) {
                return;
            }
        }
        try {
            acct.setLastLogonTimestamp(new Date());
        } catch (ServiceException e) {
            ZimbraLog.account.warn("error updating zimbraLastLogonTimestamp", e);
        }
    }

    private static Provisioning.Result toResult(ServiceException e, String dn) {
        Throwable cause = e.getCause();

        if (cause instanceof IOException) {
            return Check.toResult((IOException) cause, dn);
        } else if (e instanceof LdapException) {
            LdapException ldapException = (LdapException) e;
            Throwable detail = ldapException.getDetail();
            if (detail instanceof IOException) {
                return Check.toResult((IOException) detail, dn);
            } else if (ldapException instanceof LdapEntryNotFoundException ||
                       detail instanceof LdapEntryNotFoundException) {
                return new Provisioning.Result(Check.STATUS_NAME_NOT_FOUND, e, dn);
            } else if (ldapException instanceof LdapInvalidSearchFilterException ||
                       detail instanceof LdapInvalidSearchFilterException) {
                return new Provisioning.Result(Check.STATUS_INVALID_SEARCH_FILTER, e, dn);
            }
        }

        // return a generic error for all other causes
        return new Provisioning.Result(Check.STATUS_AUTH_FAILED, e, dn);
    }

    private void ldapAuthenticate(String urls[], boolean requireStartTLS, String principal, String password)
    throws ServiceException {
        if (password == null || password.equals("")) {
            throw AccountServiceException.AuthFailedServiceException.AUTH_FAILED("empty password");
        }

        LdapClient.externalLdapAuthenticate(urls, requireStartTLS, principal, password, "external LDAP auth");
    }

    /*
     * search for the auth DN for the user, authneticate to the result DN
     */
    private void ldapAuthenticate(String url[], boolean wantStartTLS, String password,
            String searchBase, String searchFilter, String searchDn, String searchPassword)
    throws ServiceException {

        if (password == null || password.equals("")) {
            throw AccountServiceException.AuthFailedServiceException.AUTH_FAILED("empty password");
        }

        ExternalLdapConfig config = new ExternalLdapConfig(url, wantStartTLS,
                null, searchDn, searchPassword, null, "external LDAP auth");

        String resultDn = null;
        String tooMany = null;

        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getExternalContext(config, LdapUsage.LDAP_AUTH_EXTERNAL);
            ZSearchResultEnumeration ne = zlc.searchDir(searchBase,
                    filterFactory.fromFilterString(FilterId.LDAP_AUTHENTICATE, searchFilter),
                    ZSearchControls.SEARCH_CTLS_SUBTREE());

            while (ne.hasMore()) {
                ZSearchResultEntry sr = ne.next();
                if (resultDn == null) {
                    resultDn = sr.getDN();
                } else {
                    tooMany = sr.getDN();
                    break;
                }
            }
            ne.close();
        } finally {
            LdapClient.closeContext(zlc);
        }

        if (tooMany != null) {
            ZimbraLog.account.warn(String.format(
                    "ldapAuthenticate searchFilter returned more then one result: (dn1=%s, dn2=%s, filter=%s)",
                    resultDn, tooMany, searchFilter));
            throw AccountServiceException.AuthFailedServiceException.AUTH_FAILED("too many results from search filter!");
        } else if (resultDn == null) {
            throw AccountServiceException.AuthFailedServiceException.AUTH_FAILED("empty search");
        }
        if (ZimbraLog.account.isDebugEnabled()) ZimbraLog.account.debug("search filter matched: "+resultDn);
        ldapAuthenticate(url, wantStartTLS, resultDn, password);
    }

    @Override
    public Provisioning.Result checkAuthConfig(Map attrs, String name, String password)
    throws ServiceException {
        AuthMech mech = AuthMech.fromString(Check.getRequiredAttr(attrs, Provisioning.A_zimbraAuthMech));
        if (!(mech == AuthMech.ldap || mech == AuthMech.ad)) {
            throw ServiceException.INVALID_REQUEST("auth mech must be: "+
                    AuthMech.ldap.name() + " or " + AuthMech.ad.name(), null);
        }

        String url[] = Check.getRequiredMultiAttr(attrs, Provisioning.A_zimbraAuthLdapURL);

        // TODO, need admin UI work for zimbraAuthLdapStartTlsEnabled
        String startTLSEnabled = (String) attrs.get(Provisioning.A_zimbraAuthLdapStartTlsEnabled);
        boolean requireStartTLS = startTLSEnabled == null ? false : ProvisioningConstants.TRUE.equals(startTLSEnabled);

        try {
            String searchFilter = (String) attrs.get(Provisioning.A_zimbraAuthLdapSearchFilter);
            if (searchFilter != null) {
                String searchPassword = (String) attrs.get(Provisioning.A_zimbraAuthLdapSearchBindPassword);
                String searchDn = (String) attrs.get(Provisioning.A_zimbraAuthLdapSearchBindDn);
                String searchBase = (String) attrs.get(Provisioning.A_zimbraAuthLdapSearchBase);
                if (searchBase == null) searchBase = "";
                searchFilter = LdapUtil.computeDn(name, searchFilter);
                if (ZimbraLog.account.isDebugEnabled()) ZimbraLog.account.debug("auth with search filter of "+searchFilter);
                ldapAuthenticate(url, requireStartTLS, password, searchBase, searchFilter, searchDn, searchPassword);
                return new Provisioning.Result(Check.STATUS_OK, "", searchFilter);
            }

            String bindDn = (String) attrs.get(Provisioning.A_zimbraAuthLdapBindDn);
            if (bindDn != null) {
                String dn = LdapUtil.computeDn(name, bindDn);
                if (ZimbraLog.account.isDebugEnabled()) ZimbraLog.account.debug("auth with bind dn template of "+dn);
                ldapAuthenticate(url, requireStartTLS, dn, password);
                return new Provisioning.Result(Check.STATUS_OK, "", dn);
            }

            throw ServiceException.INVALID_REQUEST("must specify "+Provisioning.A_zimbraAuthLdapSearchFilter + " or " +
                    Provisioning.A_zimbraAuthLdapBindDn, null);
        } catch (ServiceException e) {
            return toResult(e, "");
        }
    }

    @Override
    public Provisioning.Result checkGalConfig(Map attrs, String query, int limit, GalOp galOp)
    throws ServiceException {

        GalMode mode = GalMode.fromString(Check.getRequiredAttr(attrs, Provisioning.A_zimbraGalMode));
        if (mode != GalMode.ldap)
            throw ServiceException.INVALID_REQUEST("gal mode must be: "+GalMode.ldap.toString(), null);

        GalParams.ExternalGalParams galParams = new GalParams.ExternalGalParams(attrs, galOp);

        LdapGalMapRules rules = new LdapGalMapRules(Provisioning.getInstance().getConfig(), false);

        try {
            SearchGalResult result = null;
            if (galOp == GalOp.autocomplete)
                result = LdapGalSearch.searchLdapGal(galParams, GalOp.autocomplete, query, limit, rules, null, null);
            else if (galOp == GalOp.search)
                result = LdapGalSearch.searchLdapGal(galParams, GalOp.search, query, limit, rules, null, null);
            else if (galOp == GalOp.sync)
                result = LdapGalSearch.searchLdapGal(galParams, GalOp.sync, query, limit, rules, "", null);
            else
                throw ServiceException.INVALID_REQUEST("invalid GAL op: "+galOp.toString(), null);

            return new Provisioning.GalResult(Check.STATUS_OK, "", result.getMatches());
        } catch (ServiceException e) {
            return toResult(e, "");
        }
    }

    @Override
    public void externalLdapAuth(Domain domain, AuthMech authMech,
            Account acct, String password, Map<String, Object> authCtxt)
    throws ServiceException {
        externalLdapAuth(domain, authMech, acct, null, password, authCtxt);
    }

    @Override
    public void externalLdapAuth(Domain d, AuthMech authMech,
            String principal, String password, Map<String, Object> authCtxt)
    throws ServiceException {
        externalLdapAuth(d, authMech, null, principal, password, authCtxt);
    }

    void externalLdapAuth(Domain d, AuthMech authMech, Account acct, String principal,
            String password, Map<String, Object> authCtxt) throws ServiceException {
        // exactly one of acct or principal is not null
        // when acct is null, we are from the auto provisioning path
        assert((acct == null) != (principal == null));

        String url[] = d.getMultiAttr(Provisioning.A_zimbraAuthLdapURL);

        if (url == null || url.length == 0) {
            String msg = "attr not set "+Provisioning.A_zimbraAuthLdapURL;
            ZimbraLog.account.fatal(msg);
            throw ServiceException.FAILURE(msg, null);
        }

        boolean requireStartTLS = d.getBooleanAttr(Provisioning.A_zimbraAuthLdapStartTlsEnabled, false);

        try {
            // try explicit externalDn first
            if (acct != null) {
                String externalDn = acct.getAttr(Provisioning.A_zimbraAuthLdapExternalDn);
                if (externalDn != null) {
                    ZimbraLog.account.debug("auth with explicit dn of "+externalDn);
                    ldapAuthenticate(url, requireStartTLS, externalDn, password);
                    return;
                }

                // principal must be null, user account's name for principal
                principal = acct.getName();
            }

            // principal must not be null by now

            String searchFilter = d.getAttr(Provisioning.A_zimbraAuthLdapSearchFilter);
            if (searchFilter != null && AuthMech.ad != authMech) {
                String searchPassword = d.getAttr(Provisioning.A_zimbraAuthLdapSearchBindPassword);
                String searchDn = d.getAttr(Provisioning.A_zimbraAuthLdapSearchBindDn);
                String searchBase = d.getAttr(Provisioning.A_zimbraAuthLdapSearchBase);
                if (searchBase == null) {
                    searchBase = "";
                }
                searchFilter = LdapUtil.computeDn(principal, searchFilter);
                ZimbraLog.account.debug("auth with search filter of "+searchFilter);
                ldapAuthenticate(url, requireStartTLS, password, searchBase, searchFilter, searchDn, searchPassword);
                return;
            }

            String bindDn = d.getAttr(Provisioning.A_zimbraAuthLdapBindDn);
            if (bindDn != null) {
                String dn = LdapUtil.computeDn(principal, bindDn);
                ZimbraLog.account.debug("auth with bind dn template of "+dn);
                ldapAuthenticate(url, requireStartTLS, dn, password);
                return;
            }
        } catch (ServiceException e) {
            throw AuthFailedServiceException.AUTH_FAILED(principal,
                    AuthMechanism.namePassedIn(authCtxt), "external LDAP auth failed, "+e.getMessage(), e);
        }

        String msg = "one of the following attrs must be set "+
                Provisioning.A_zimbraAuthLdapBindDn+", "+Provisioning.A_zimbraAuthLdapSearchFilter;
        ZimbraLog.account.fatal(msg);
        throw ServiceException.FAILURE(msg, null);
    }

    @Override
    public void zimbraLdapAuthenticate(Account acct, String password,
            Map<String, Object> authCtxt) throws ServiceException {

        try {
            LdapClient.zimbraLdapAuthenticate(((LdapEntry)acct).getDN(), password);
        } catch (ServiceException e) {
            throw AuthFailedServiceException.AUTH_FAILED(acct.getName(),
                    AuthMechanism.namePassedIn(authCtxt), e.getMessage(), e);
        }
    }

    private void verifyPassword(Account acct, String password, AuthMechanism authMech,
            Map<String, Object> authCtxt)
    throws ServiceException {
        synchronized (acct) {
            LdapLockoutPolicy lockoutPolicy = new LdapLockoutPolicy(this, acct);
            if (lockoutPolicy.isLockedOut() && !isRecoveryCodeBasedAuth(authCtxt)) {
                try {
                    verifyPasswordInternal(acct, password, authMech, authCtxt);
                } catch (ServiceException e) {
                    lockoutPolicy.failedLogin();
                }
                throw AuthFailedServiceException.AUTH_FAILED(acct.getName(),
                        AuthMechanism.namePassedIn(authCtxt), "account lockout");
            }
            try {
                // attempt to verify the password
                verifyPasswordInternal(acct, password, authMech, authCtxt);
                lockoutPolicy.successfulLogin();
            } catch (AccountServiceException e) {
                String protocol = null;
                if (authCtxt != null) {
                    if (authCtxt.get(AuthContext.AC_PROTOCOL) != null) {
                        protocol = authCtxt.get(AuthContext.AC_PROTOCOL).toString();
                    }
                }

                boolean mCaptchaEnabled = acct.getBooleanAttr(Provisioning.A_zimbraCAPTCHAEnabled, false);
                if (mCaptchaEnabled) {
                    int loginFailCount = acct.getIntAttr(Provisioning.A_zimbraCAPTCHALoginFailedCount, 0);
                    int showCaptchaOnLoginfailCount = Integer.parseInt(Provisioning.getInstance().getConfig().getAttr(Provisioning.A_zimbraShowCaptchaOnLoginFailedCount, "2"));
                    if (loginFailCount >= showCaptchaOnLoginfailCount - 1) {
                        throw AuthFailedServiceException.NEED_CAPTCHA();
                    }
                }

                lockoutPolicy.failedLogin(protocol, password);
                // re-throw original exception
                throw e;
            }
        }
    }

    /*
     * authAccount does all the status/mustChange checks, this just takes the
     * password and auths the user
     */
    private void verifyPasswordInternal(Account acct, String password,
            AuthMechanism authMech, Map<String, Object> context)
    throws ServiceException {
        if (isRecoveryCodeBasedAuth(context)) {
            verifyRecoveryCode(acct, password, context);
        } else {
            Domain domain = Provisioning.getInstance().getDomain(acct);

            boolean allowFallback = true;
            if (!authMech.isZimbraAuth()) {
                allowFallback =
                    domain.getBooleanAttr(Provisioning.A_zimbraAuthFallbackToLocal, false) ||
                    acct.getBooleanAttr(Provisioning.A_zimbraIsAdminAccount, false) ||
                    acct.getBooleanAttr(Provisioning.A_zimbraIsDomainAdminAccount, false);
            }

            try {
                authMech.doAuth(this, domain, acct, password, context);
                AuthMech authedByMech = authMech.getMechanism();
                // indicate the authed by mech in the auth context
                // context.put(AuthContext.AC_AUTHED_BY_MECH, authedByMech); TODO
                return;
            } catch (ServiceException e) {
                if (!allowFallback || authMech.isZimbraAuth())
                    throw e;
                ZimbraLog.account.warn(authMech.getMechanism() + " auth for domain " +
                    domain.getName() + " failed, fall back to zimbra default auth mechanism", e);
            }

            // fall back to zimbra default auth
            AuthMechanism.doZimbraAuth(this, domain, acct, password, context);
            // context.put(AuthContext.AC_AUTHED_BY_MECH, Provisioning.AM_ZIMBRA); // TODO
        }
    }

    @Override
    public void changePassword(Account acct, String currentPassword, String newPassword)
    throws ServiceException {
        authAccount(acct, currentPassword, false, null);
        boolean locked = acct.getBooleanAttr(Provisioning.A_zimbraPasswordLocked, false);
        if (locked)
            throw AccountServiceException.PASSWORD_LOCKED();
        setPassword(acct, newPassword, true, false);
    }

    /**
     * @param newPassword
     * @throws AccountServiceException
     */
    private void checkHistory(String newPassword, String[] history)
    throws AccountServiceException {
        if (history == null)
            return;
        for (int i=0; i < history.length; i++) {
            int sepIndex = history[i].indexOf(':');
            if (sepIndex != -1)  {
                String encoded = history[i].substring(sepIndex+1);
                if (PasswordUtil.SSHA.isSSHA(encoded)) {
                    if (PasswordUtil.SSHA.verifySSHA(encoded, newPassword))
                        throw AccountServiceException.PASSWORD_RECENTLY_USED();
                } else if (PasswordUtil.SSHA512.isSSHA512(encoded)) {
                    if (PasswordUtil.SSHA512.verifySSHA512(encoded, newPassword))
                        throw AccountServiceException.PASSWORD_RECENTLY_USED();
                } else {
                    ZimbraLog.account.debug("password hash neither SSHA nor SSHA512; ignored for password history");
                }
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
        if (currentPassword == null)
            return null;

        ArrayList<String> newHistory = new ArrayList<String>();
        String currentHistory = System.currentTimeMillis() + ":"+currentPassword;
        if (history != null) {
            newHistory.addAll(Arrays.asList(history));
            Collections.sort(newHistory);
        }
        while (newHistory.size() >= maxHistory) {
            newHistory.remove(0);
        }
        newHistory.add(currentHistory);

        return newHistory.toArray(new String[newHistory.size()]);
    }

    @Override
    public SetPasswordResult setPassword(Account acct, String newPassword)
    throws ServiceException {
        return setPassword(acct, newPassword, false);
    }

    @Override
    public SetPasswordResult setPassword(Account acct, String newPassword,
            boolean enforcePasswordPolicy)
    throws ServiceException {
        SetPasswordResult result = new SetPasswordResult();
        String msg = null;

        try {
            // dry run to pick up policy violation, if any
            setPassword(acct, newPassword, false, true);
        } catch (ServiceException e) {
            msg = e.getMessage();
        }

        setPassword(acct, newPassword, enforcePasswordPolicy, false);

        if (msg != null) {
            msg = L10nUtil.getMessage(L10nUtil.MsgKey.passwordViolation,
                    acct.getLocale(), acct.getName(), msg);

            result.setMessage(msg);
        }

        return result;
    }

    @Override
    public void checkPasswordStrength(Account acct, String password)
    throws ServiceException {
        checkPasswordStrength(password, acct, null, null);
    }

    private int getInt(Account acct, Cos cos, ZMutableEntry entry, String name, int defaultValue)
    throws ServiceException {
        if (acct != null) {
            return acct.getIntAttr(name, defaultValue);
        }

        try {
            String v = entry.getAttrString(name);
            if (v != null) {
                try {
                    return Integer.parseInt(v);
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            }
        } catch (ServiceException ne) {
            throw ServiceException.FAILURE(ne.getMessage(), ne);
        }

        return cos.getIntAttr(name, defaultValue);
    }

    private String getString(Account acct, Cos cos, ZMutableEntry entry, String name)
    throws ServiceException {
        if (acct != null) {
            return acct.getAttr(name);
        }

        try {
            String v = entry.getAttrString(name);
            if (v != null) {
                return v;
            }
        } catch (ServiceException ne) {
            throw ServiceException.FAILURE(ne.getMessage(), ne);
        }

        return cos.getAttr(name);
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
    private void checkPasswordStrength(String password, Account acct, Cos cos, ZMutableEntry entry)
    throws ServiceException {
        int minLength = getInt(acct, cos, entry, Provisioning.A_zimbraPasswordMinLength, 0);
        if (minLength > 0 && password.length() < minLength) {
            throw AccountServiceException.INVALID_PASSWORD("too short",
                    new Argument(Provisioning.A_zimbraPasswordMinLength, minLength, Argument.Type.NUM));
        }

        int maxLength = getInt(acct, cos, entry, Provisioning.A_zimbraPasswordMaxLength, 0);
        if (maxLength > 0 && password.length() > maxLength) {
            throw AccountServiceException.INVALID_PASSWORD("too long",
                    new Argument(Provisioning.A_zimbraPasswordMaxLength, maxLength, Argument.Type.NUM));
        }

        int minUpperCase = getInt(acct, cos, entry, Provisioning.A_zimbraPasswordMinUpperCaseChars, 0);
        int minLowerCase = getInt(acct, cos, entry, Provisioning.A_zimbraPasswordMinLowerCaseChars, 0);
        int minNumeric = getInt(acct, cos, entry, Provisioning.A_zimbraPasswordMinNumericChars, 0);
        int minPunctuation = getInt(acct, cos, entry, Provisioning.A_zimbraPasswordMinPunctuationChars, 0);
        int minAlpha = getInt(acct, cos, entry, Provisioning.A_zimbraPasswordMinAlphaChars, 0);
        int minNumOrPunc = getInt(acct, cos, entry, Provisioning.A_zimbraPasswordMinDigitsOrPuncs, 0);

        String allowedChars = getString(acct, cos, entry, Provisioning.A_zimbraPasswordAllowedChars);
        Pattern allowedCharsPattern = null;
        if (allowedChars != null) {
            try {
                allowedCharsPattern = Pattern.compile(allowedChars);
            } catch (PatternSyntaxException e) {
                throw AccountServiceException.INVALID_PASSWORD(Provisioning.A_zimbraPasswordAllowedChars +
                        " is not valid regex: " + e.getMessage());
            }
        }
        String allowedPuncChars = getString(acct, cos, entry, Provisioning.A_zimbraPasswordAllowedPunctuationChars);
        Pattern allowedPuncCharsPattern = null;
        if (allowedPuncChars != null) {
            try {
                allowedPuncCharsPattern = Pattern.compile(allowedPuncChars);
            } catch (PatternSyntaxException e) {
                throw AccountServiceException.INVALID_PASSWORD(Provisioning.A_zimbraPasswordAllowedPunctuationChars +
                        " is not valid regex: " + e.getMessage());
            }
        }

        boolean hasPolicies = minUpperCase > 0 || minLowerCase > 0 || minNumeric > 0 || minPunctuation > 0 ||
                minAlpha > 0 || minNumOrPunc > 0 || allowedCharsPattern != null || allowedPuncCharsPattern != null;

        if (!hasPolicies) {
            return;
        }

        int upper = 0;
        int lower = 0;
        int numeric = 0;
        int punctuation = 0;
        int alpha = 0;

        for (int i=0; i < password.length(); i++) {
            char ch = password.charAt(i);

            if (allowedCharsPattern != null) {
                if (!allowedCharsPattern.matcher(Character.toString(ch)).matches()) {
                    throw AccountServiceException.INVALID_PASSWORD(ch + " is not an allowed character",
                            new Argument(Provisioning.A_zimbraPasswordAllowedChars, allowedChars, Argument.Type.STR));
                }
            }

            boolean isAlpha = true;
            if (Character.isUpperCase(ch)) {
                upper++;
            } else if (Character.isLowerCase(ch)) {
                lower++;
            } else if (Character.isDigit(ch)) {
                numeric++;
                isAlpha = false;
            } else if (allowedPuncCharsPattern != null) {
                if (allowedPuncCharsPattern.matcher(Character.toString(ch)).matches()) {
                    punctuation++;
                    isAlpha = false;
                }
            } else if (isAsciiPunc(ch)) {
                punctuation++;
                isAlpha = false;
            }
            if (isAlpha) {
                alpha++;
            }
        }

        if (upper < minUpperCase) {
            throw AccountServiceException.INVALID_PASSWORD("not enough upper case characters",
                    new Argument(Provisioning.A_zimbraPasswordMinUpperCaseChars, minUpperCase, Argument.Type.NUM));
        }
        if (lower < minLowerCase) {
            throw AccountServiceException.INVALID_PASSWORD("not enough lower case characters",
                    new Argument(Provisioning.A_zimbraPasswordMinLowerCaseChars, minLowerCase, Argument.Type.NUM));
        }
        if (numeric < minNumeric) {
            throw AccountServiceException.INVALID_PASSWORD("not enough numeric characters",
                    new Argument(Provisioning.A_zimbraPasswordMinNumericChars, minNumeric, Argument.Type.NUM));
        }
        if (punctuation < minPunctuation) {
            throw AccountServiceException.INVALID_PASSWORD("not enough punctuation characters",
                    new Argument(Provisioning.A_zimbraPasswordMinPunctuationChars, minPunctuation, Argument.Type.NUM));
        }
        if (alpha < minAlpha) {
            throw AccountServiceException.INVALID_PASSWORD("not enough alpha characters",
                    new Argument(Provisioning.A_zimbraPasswordMinAlphaChars, minAlpha, Argument.Type.NUM));
        }
        if (numeric + punctuation < minNumOrPunc) {
            throw AccountServiceException.INVALID_PASSWORD("not enough numeric or punctuation characters",
                    new Argument(Provisioning.A_zimbraPasswordMinDigitsOrPuncs, minNumOrPunc, Argument.Type.NUM));
        }
    }

    private boolean isAsciiPunc(char ch) {
        return
            (ch >= 33 && ch <= 47) || // ! " # $ % & ' ( ) * + , - . /
            (ch >= 58 && ch <= 64) || // : ; < = > ? @
            (ch >= 91 && ch <= 96) || // [ \ ] ^ _ `
            (ch >=123 && ch <= 126);  // { | } ~
    }

    void setPassword(Account acct, String newPassword, boolean enforcePolicy, boolean dryRun)
    throws ServiceException {

        boolean mustChange = acct.getBooleanAttr(Provisioning.A_zimbraPasswordMustChange, false);

        if (enforcePolicy || dryRun) {
            checkPasswordStrength(newPassword, acct, null, null);

            // skip min age checking if mustChange is set
            if (!mustChange) {
                int minAge = acct.getIntAttr(Provisioning.A_zimbraPasswordMinAge, 0);
                if (minAge > 0) {
                    Date lastChange = acct.getGeneralizedTimeAttr(Provisioning.A_zimbraPasswordModifiedTime, null);
                    if (lastChange != null) {
                        long last = lastChange.getTime();
                        long curr = System.currentTimeMillis();
                        if ((last+(Constants.MILLIS_PER_DAY * minAge)) > curr)
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

            if (enforcePolicy || dryRun)
                checkHistory(newPassword, newHistory);
        }

        if (dryRun) {
            return;
        }

        // unset it so it doesn't take up space...
        if (mustChange)
            attrs.put(Provisioning.A_zimbraPasswordMustChange, "");

        attrs.put(Provisioning.A_zimbraPasswordModifiedTime, LdapDateUtil.toGeneralizedTime(new Date()));

        // update the validity value to invalidate auto-standing auth tokens
        int tokenValidityValue = acct.getAuthTokenValidityValue();
        acct.setAuthTokenValidityValue(tokenValidityValue == Integer.MAX_VALUE ? 0 : tokenValidityValue + 1, attrs);

        ChangePasswordListener.ChangePasswordListenerContext ctxts = new ChangePasswordListener.ChangePasswordListenerContext();
        ChangePasswordListener.invokePreModify(acct, newPassword, ctxts, attrs);

        try{
            setLdapPassword(acct, null, newPassword);
            // modify the password
            modifyAttrs(acct, attrs);
        } catch(ServiceException se){
            ChangePasswordListener.invokeOnException(acct, newPassword, ctxts, se);
            throw se;
        }

        ChangePasswordListener.invokePostModify(acct, newPassword, ctxts);
    }

    @Override
    public Zimlet getZimlet(String name) throws ServiceException {
        return getZimlet(name, null, true);
    }

    private Zimlet getFromCache(Key.ZimletBy keyType, String key) {
        switch(keyType) {
        case name:
            return zimletCache.getByName(key);
        case id:
            return zimletCache.getById(key);
        default:
            return null;
        }
    }

    Zimlet lookupZimlet(String name, ZLdapContext zlc) throws ServiceException {
        return getZimlet(name, zlc, false);
    }

    private Zimlet getZimlet(String name, ZLdapContext initZlc, boolean useZimletCache)
    throws ServiceException {

        LdapZimlet zimlet = null;
        if (useZimletCache) {
            zimlet = zimletCache.getByName(name);
        }

        if (zimlet != null) {
            return zimlet;
        }

        try {
            String dn = mDIT.zimletNameToDN(name);
            ZAttributes attrs = helper.getAttributes(
                    initZlc, LdapServerType.REPLICA, LdapUsage.GET_ZIMLET, dn, null);
            zimlet = new LdapZimlet(dn, attrs, this);
            if (useZimletCache) {
                ZimletUtil.reloadZimlet(name);
                zimletCache.put(zimlet);  // put LdapZimlet into the cache after successful ZimletUtil.reloadZimlet()
            }
            return zimlet;
        } catch (LdapEntryNotFoundException e) {
            return null;
        } catch (ServiceException ne) {
            throw ServiceException.FAILURE("unable to get zimlet: "+name, ne);
        } catch (ZimletException ze) {
            throw ServiceException.FAILURE("unable to load zimlet: "+name, ze);
        }
    }

    @Override
    public List<Zimlet> listAllZimlets() throws ServiceException {
        List<Zimlet> result = new ArrayList<Zimlet>();

        try {
            ZSearchResultEnumeration ne = helper.searchDir(mDIT.zimletBaseDN(),
                    filterFactory.allZimlets(), ZSearchControls.SEARCH_CTLS_SUBTREE());
            while (ne.hasMore()) {
                ZSearchResultEntry sr = ne.next();
             result.add(new LdapZimlet(sr.getDN(), sr.getAttributes(), this));
            }
            ne.close();
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to list all zimlets", e);
        }

        Collections.sort(result);
        return result;
    }

    @Override
    public Zimlet createZimlet(String name, Map<String, Object> zimletAttrs)
    throws ServiceException {
        name = name.toLowerCase().trim();

        CallbackContext callbackContext = new CallbackContext(CallbackContext.Op.CREATE);
        AttributeManager.getInstance().preModify(zimletAttrs, null, callbackContext, true);

        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.CREATE_ZIMLET);

            String hasKeyword = LdapConstants.LDAP_FALSE;
            if (zimletAttrs.containsKey(A_zimbraZimletKeyword)) {
                hasKeyword = ProvisioningConstants.TRUE;
            }

            ZMutableEntry entry = LdapClient.createMutableEntry();
            entry.mapToAttrs(zimletAttrs);

            entry.setAttr(A_objectClass, "zimbraZimletEntry");
            entry.setAttr(A_zimbraZimletEnabled, ProvisioningConstants.FALSE);
            entry.setAttr(A_zimbraZimletIndexingEnabled, hasKeyword);
            entry.setAttr(A_zimbraCreateTimestamp, LdapDateUtil.toGeneralizedTime(new Date()));

            String dn = mDIT.zimletNameToDN(name);
            entry.setDN(dn);
            zlc.createEntry(entry);

            Zimlet zimlet = lookupZimlet(name, zlc);
            AttributeManager.getInstance().postModify(zimletAttrs, zimlet, callbackContext);
            return zimlet;
        } catch (LdapEntryAlreadyExistException nabe) {
            throw AccountServiceException.ZIMLET_EXISTS(name);
        } catch (LdapException e) {
            throw e;
        } catch (AccountServiceException e) {
            throw e;
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to create zimlet: "+name, e);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    @Override
    public void deleteZimlet(String name) throws ServiceException {
        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.DELETE_ZIMLET);
            LdapZimlet zimlet = (LdapZimlet)getZimlet(name, zlc, true);
            if (zimlet != null) {
                zimletCache.remove(zimlet);
                zlc.deleteEntry(zimlet.getDN());
            }
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to delete zimlet: "+name, e);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    @Override
    public CalendarResource createCalendarResource(String emailAddress,String password,
            Map<String, Object> calResAttrs)
    throws ServiceException {
        emailAddress = emailAddress.toLowerCase().trim();

        calResAttrs.put(Provisioning.A_zimbraAccountCalendarUserType,
                        AccountCalendarUserType.RESOURCE.toString());

        SpecialAttrs specialAttrs = mDIT.handleSpecialAttrs(calResAttrs);

        CallbackContext callbackContext = new CallbackContext(CallbackContext.Op.CREATE);

        Set<String> ocs = LdapObjectClass.getCalendarResourceObjectClasses(this);
        Account acct = createAccount(emailAddress, password, calResAttrs, specialAttrs,
                ocs.toArray(new String[0]), false, null);

        LdapCalendarResource resource =
            (LdapCalendarResource) getCalendarResourceById(acct.getId(), true);
        AttributeManager.getInstance().
            postModify(calResAttrs, resource, callbackContext);
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
    public CalendarResource get(Key.CalendarResourceBy keyType, String key)
    throws ServiceException {
        return get(keyType, key, false);
    }

    @Override
    public CalendarResource get(Key.CalendarResourceBy keyType, String key, boolean loadFromMaster)
    throws ServiceException {
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
        Account acct = accountCache.getById(zimbraId);
        if (acct != null) {
            if (acct instanceof LdapCalendarResource) {
                return (LdapCalendarResource) acct;
            } else {
                // could be a non-resource Account
                return null;
            }
        }
        LdapCalendarResource resource = (LdapCalendarResource) getAccountByQuery(
            mDIT.mailBranchBaseDN(),
            filterFactory.calendarResourceById(zimbraId),
            null, loadFromMaster);
        accountCache.put(resource);
        return resource;
    }

    private CalendarResource getCalendarResourceByName(String emailAddress, boolean loadFromMaster)
    throws ServiceException {
        emailAddress = fixupAccountName(emailAddress);
        Account acct = accountCache.getByName(emailAddress);
        if (acct != null) {
            if (acct instanceof LdapCalendarResource) {
                return (LdapCalendarResource) acct;
            } else {
                // could be a non-resource Account
                return null;
            }
        }
        LdapCalendarResource resource = (LdapCalendarResource) getAccountByQuery(
            mDIT.mailBranchBaseDN(),
            filterFactory.calendarResourceByName(emailAddress),
            null, loadFromMaster);
        accountCache.put(resource);
        return resource;
    }

    private CalendarResource getCalendarResourceByForeignPrincipal(String foreignPrincipal, boolean loadFromMaster)
    throws ServiceException {
        LdapCalendarResource resource =
            (LdapCalendarResource) getAccountByQuery(
                mDIT.mailBranchBaseDN(),
                filterFactory.calendarResourceByForeignPrincipal(foreignPrincipal),
                null, loadFromMaster);
        accountCache.put(resource);
        return resource;
    }

    private Account makeAccount(String dn, ZAttributes attrs) throws ServiceException {
        return makeAccount(dn, attrs, null);
    }

    private Account makeAccountNoDefaults(String dn, ZAttributes attrs) throws ServiceException {
        return makeAccount(dn, attrs, MakeObjectOpt.NO_DEFAULTS);
    }

    private Account makeAccount(String dn, ZAttributes attrs, MakeObjectOpt makeObjOpt)
    throws ServiceException {
        String userType = attrs.getAttrString(Provisioning.A_zimbraAccountCalendarUserType);
        boolean isAccount = (userType == null) || userType.equals(AccountCalendarUserType.USER.toString());

        String emailAddress = attrs.getAttrString(Provisioning.A_zimbraMailDeliveryAddress);
        if (emailAddress == null)
            emailAddress = mDIT.dnToEmail(dn, attrs);

        Account acct = (isAccount) ? new LdapAccount(dn, emailAddress, attrs, null, this) :
            new LdapCalendarResource(dn, emailAddress, attrs, null, this);

        setAccountDefaults(acct, makeObjOpt);

        return acct;
    }

    private void setAccountDefaults(Account acct, MakeObjectOpt makeObjOpt)
    throws ServiceException {
        if (makeObjOpt == MakeObjectOpt.NO_DEFAULTS) {
            // don't set any default
        } else if (makeObjOpt == MakeObjectOpt.NO_SECONDARY_DEFAULTS) {
            acct.setAccountDefaults(false);
        } else {
            acct.setAccountDefaults(true);
        }
    }

    private Alias makeAlias(String dn, ZAttributes attrs) throws ServiceException {
        String emailAddress = mDIT.dnToEmail(dn, attrs);
        Alias alias = new LdapAlias(dn, emailAddress, attrs, this);
        return alias;
    }

    private DistributionList makeDistributionList(String dn, ZAttributes attrs, boolean isBasic)
    throws ServiceException {
        String emailAddress = mDIT.dnToEmail(dn, attrs);
        DistributionList dl = new LdapDistributionList(dn, emailAddress, attrs, isBasic, this);
        return dl;
    }

    /**
     * Note - can be a bit expensive (because of loading the units) if called for a lot of groups.  Some
     * LDAP search code uses this to return NamedEntry hits which happen to be Dynamic groups.
     */
    private DynamicGroup makeDynamicGroup(ZLdapContext initZlc, String dn, ZAttributes attrs)
    throws ServiceException {
        String emailAddress = mDIT.dnToEmail(dn, mDIT.dynamicGroupNamingRdnAttr(), attrs);

        LdapDynamicGroup group = new LdapDynamicGroup(dn, emailAddress, attrs, this);

        if (!group.isMembershipDefinedByCustomURL()) {
            // load dynamic unit
            String dynamicUnitDN = mDIT.dynamicGroupUnitNameToDN(DYNAMIC_GROUP_DYNAMIC_UNIT_NAME, dn);
            ZAttributes dynamicUnitAttrs = helper.getAttributes(
                    initZlc, LdapServerType.REPLICA, LdapUsage.GET_GROUP_UNIT, dynamicUnitDN, null);

            LdapDynamicGroup.DynamicUnit dynamicUnit = new LdapDynamicGroup.DynamicUnit(
                    dynamicUnitDN, DYNAMIC_GROUP_DYNAMIC_UNIT_NAME, dynamicUnitAttrs, this);

            // load static unit
            String staticUnitDN = mDIT.dynamicGroupUnitNameToDN(DYNAMIC_GROUP_STATIC_UNIT_NAME, dn);
            ZAttributes staticUnitAttrs = helper.getAttributes(
                    initZlc, LdapServerType.REPLICA, LdapUsage.GET_GROUP_UNIT, staticUnitDN, null);

            LdapDynamicGroup.StaticUnit staticUnit = new LdapDynamicGroup.StaticUnit(
                    staticUnitDN, DYNAMIC_GROUP_STATIC_UNIT_NAME, staticUnitAttrs, this);

            group.setSubUnits(dynamicUnit, staticUnit);
        }
        return group;
    }


    /**
     *  called when an account/dl is renamed
     *
     */
    protected void renameAddressesInAllDistributionLists(String oldName, String newName,
            ReplaceAddressResult replacedAliasPairs) {
        Map<String, String> changedPairs = new HashMap<String, String>();

        changedPairs.put(oldName, newName);
        for (int i=0 ; i < replacedAliasPairs.oldAddrs().length; i++) {
            String oldAddr = replacedAliasPairs.oldAddrs()[i];
            String newAddr = replacedAliasPairs.newAddrs()[i];
            if (!oldAddr.equals(newAddr)) {
                changedPairs.put(oldAddr, newAddr);
            }
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
                ZimbraLog.account.warn("unable to rename "+oldAddrs.toString()+" to " +
                        newAddrs.toString()+" in DL "+list.getName(), se);
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

    @SuppressWarnings("unchecked")
    private List<DistributionList> getAllDistributionListsForAddresses(String addrs[],
            boolean basicAttrsOnly)
    throws ServiceException {
        if (addrs == null || addrs.length == 0)
            return new ArrayList<DistributionList>();
        String [] attrs = basicAttrsOnly ? BASIC_DL_ATTRS : null;

        SearchDirectoryOptions searchOpts = new SearchDirectoryOptions(attrs);
        searchOpts.setFilter(filterFactory.distributionListsByMemberAddrs(addrs));
        searchOpts.setTypes(ObjectType.distributionlists);
        searchOpts.setSortOpt(SortOpt.SORT_ASCENDING);

        return (List<DistributionList>) searchDirectoryInternal(searchOpts);
    }

    /**
     * -  Get list of ids from EntryCacheDataKey.GROUPEDENTRY_DIRECT_GROUPIDS for "entry"
     *    -  Entry not cached:
     *       -  Get all addresses of this entry that can be identified as a member in a static group.
     *       -  Get direct groups for those addresses using filterFactory.distributionListsByMemberAddrs
     *          i.e. using an OR filter on Provisioning.A_zimbraMailForwardingAddress
     *       -  See if prov's Group Cache has this group already.  If so, use that! otherwise add it
     *       -  Put list of ids to EntryCacheDataKey.GROUPEDENTRY_DIRECT_GROUPIDS for "entry"
     *    -  Entry is cached:
     *       -  Get all the direct groups.  If any have been deleted, update the cache to reflect that
     * @param prov
     * @param entry
     * @return
     * @throws ServiceException
     */
    private List<DistributionList> getAllDirectDLs(LdapProvisioning prov, Entry entry)
    throws ServiceException {
        if (!(entry instanceof GroupedEntry)) {
            throw ServiceException.FAILURE("internal error", null);
        }

        EntryCacheDataKey cacheKey = EntryCacheDataKey.GROUPEDENTRY_DIRECT_GROUPIDS;
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
            //   might already have all its direct group ids cached on it.
            // - if the group is already in cache, return the cached instance
            //   instead of the instance we just fetched, because the cached
            //   instance might have its direct group ids cached on it.
            directGroupIds = new ArrayList<String>(directGroups.size());
            List<DistributionList> directGroupsToReturn = new ArrayList<DistributionList>(directGroups.size());
            for (DistributionList group : directGroups) {
                String groupId = group.getId();
                directGroupIds.add(groupId);
                DistributionList cached = getDLFromCache(Key.DistributionListBy.id, groupId);
                if (cached == null) {
                    putInGroupCache(group);
                    directGroupsToReturn.add(group);
                } else {
                    directGroupsToReturn.add(cached);
                }
            }
            entry.setCachedData(cacheKey, directGroupIds);
            return directGroupsToReturn;

        } else {
            /*
             * The entry already have direct group ids cached.
             * Go through each of them and fetch the groups,
             * eithr from cache or from LDAP (prov.getDLBasic).
             */
            directGroups = new ArrayList<DistributionList>();
            Set<String> idsToRemove = null;
            for (String groupId : directGroupIds) {
                DistributionList group = prov.getDLBasic(Key.DistributionListBy.id, groupId);
                if (group == null) {
                    // the group could have been deleted
                    // remove it from our direct group id cache on the entry
                    if (idsToRemove == null) {
                        idsToRemove = new HashSet<String>();
                    }
                    idsToRemove.add(groupId);
                } else {
                    directGroups.add(group);
                }
            }

            // update our direct group id cache if needed
            if (idsToRemove != null) {
                // create a new object, do *not* update directly on the cached copy
                List<String> updatedDirectGroupIds = new ArrayList<String>();
                for (String id : directGroupIds) {
                    if (!idsToRemove.contains(id)) {
                        updatedDirectGroupIds.add(id);
                    }
                }

                // swap in the new data
                entry.setCachedData(cacheKey, updatedDirectGroupIds);
            }
        }

        return directGroups;
    }

    /*
     * like get(DistributionListBy keyType, String key)
     * the difference are:
     *     - cached
     *     - entry returned only contains minimal DL attrs
     *     - entry returned does not contain members (zimbraMailForwardingAddress)
     */
    @Override
    public DistributionList getDLBasic(Key.DistributionListBy keyType, String key)
    throws ServiceException {
        return getDLBasic(keyType, key, null);
    }

    private DistributionList getDLBasic(Key.DistributionListBy keyType, String key,
            ZLdapContext zlc)
    throws ServiceException {

        Group group = getGroupFromCache(keyType, key);

        if (group instanceof DistributionList) {
            return (DistributionList) group;
        } else if (group instanceof DynamicGroup) {
            return null;
        }

        // not in cache, fetch from LDAP
        DistributionList dl = null;

        switch(keyType) {
            case id:
                dl = getDistributionListByQuery(mDIT.mailBranchBaseDN(),
                        filterFactory.distributionListById(key), zlc, true);
                break;
            case name:
                dl = getDistributionListByQuery(mDIT.mailBranchBaseDN(),
                        filterFactory.distributionListByName(key), zlc, true);
                break;
            default:
               return null;
        }

        if (dl != null) {
            putInGroupCache(dl);
        }

        return dl;
    }

    private List<DistributionList> getContainingDistributionLists(Entry entry,
            boolean directOnly, Map<String, String> via)
    throws ServiceException {
        List<DistributionList> directDLs = getAllDirectDLs(this, entry);
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

            List<DistributionList> newLists = getAllDirectDLs(this, dl);

            for (DistributionList newDl: newLists) {
                if (!directDLSet.contains(newDl.getName())) {
                    if (via != null) {
                        via.put(newDl.getName(), dl.getName());
                    }
                    dlsToCheck.push(newDl);
                }
            }
        }
        Collections.sort(result);
        return result;
    }

    @Override
    public Set<String> getDistributionLists(Account acct) throws ServiceException {
        @SuppressWarnings("unchecked")
        Set<String> dls = (Set<String>) acct.getCachedData(EntryCacheDataKey.ACCOUNT_DLS);
        if (dls != null) {
            return dls;
        }
        dls = getDistributionListIds(acct, false);
        acct.setCachedData(EntryCacheDataKey.ACCOUNT_DLS, dls);
        return dls;
    }

    @Override
    public Set<String> getDirectDistributionLists(Account acct) throws ServiceException {
        @SuppressWarnings("unchecked")
        Set<String> dls = (Set<String>) acct.getCachedData(EntryCacheDataKey.ACCOUNT_DIRECT_DLS);
        if (dls != null) {
            return dls;
        }
        dls = getDistributionListIds(acct, true);
        acct.setCachedData(EntryCacheDataKey.ACCOUNT_DIRECT_DLS, dls);
        return dls;
    }

    private Set<String> getDistributionListIds(Account acct, boolean directOnly)
    throws ServiceException {

        Set<String> dls = new HashSet<String>();

        List<DistributionList> lists = getDistributionLists(acct, directOnly, null);

        for (DistributionList dl : lists) {
            dls.add(dl.getId());
        }
        dls = Collections.unmodifiableSet(dls);
        return dls;
    }

    @Override
    public boolean inDistributionList(Account acct, String zimbraId) throws ServiceException {
        return getDistributionLists(acct).contains(zimbraId);
    }


    @Override
    public boolean inDistributionList(DistributionList list, String zimbraId)
    throws ServiceException {
        GroupMembership groupMembership = getGroupMembership(list, false);
        return groupMembership.groupIds().contains(zimbraId);
    }

    @Override
    public List<DistributionList> getDistributionLists(Account acct, boolean directOnly,
            Map<String, String> via) throws ServiceException {
        return getContainingDistributionLists(acct, directOnly, via);
    }

    private static final int DEFAULT_GAL_MAX_RESULTS = 100;

    private static final String DATA_GAL_RULES = "GAL_RULES";



    @Override
    public List<?> getAllAccounts(Domain domain) throws ServiceException {
        SearchAccountsOptions opts = new SearchAccountsOptions(domain);
        opts.setFilter(filterFactory.allAccountsOnly());
        opts.setIncludeType(IncludeType.ACCOUNTS_ONLY);
        opts.setSortOpt(SortOpt.SORT_ASCENDING);
        return searchDirectory(opts);
    }

    @Override
    public void getAllAccounts(Domain domain, NamedEntry.Visitor visitor)
    throws ServiceException {
        SearchAccountsOptions opts = new SearchAccountsOptions(domain);
        opts.setFilter(filterFactory.allAccountsOnly());
        opts.setIncludeType(IncludeType.ACCOUNTS_ONLY);
        searchDirectory(opts, visitor);
    }

    @Override
    public void getAllAccounts(Domain domain, Server server, NamedEntry.Visitor visitor)
    throws ServiceException {
        if (server != null) {
            SearchAccountsOptions searchOpts = new SearchAccountsOptions(domain);
            searchOpts.setIncludeType(IncludeType.ACCOUNTS_ONLY);
            searchAccountsOnServerInternal(server, searchOpts, visitor);
        } else {
            getAllAccounts(domain, visitor);
        }
    }

    @Override
    public List<?> getAllCalendarResources(Domain domain) throws ServiceException {
        SearchDirectoryOptions searchOpts = new SearchDirectoryOptions(domain);
        searchOpts.setFilter(mDIT.filterCalendarResourcesByDomain(domain));
        searchOpts.setTypes(ObjectType.resources);
        searchOpts.setSortOpt(SortOpt.SORT_ASCENDING);
        return searchDirectoryInternal(searchOpts);
    }

    @Override
    public void getAllCalendarResources(Domain domain, NamedEntry.Visitor visitor)
    throws ServiceException {
        SearchDirectoryOptions searchOpts = new SearchDirectoryOptions(domain);
        searchOpts.setFilter(mDIT.filterCalendarResourcesByDomain(domain));
        searchOpts.setTypes(ObjectType.resources);
        searchDirectoryInternal(searchOpts, visitor);
    }

    @Override
    public void getAllCalendarResources(Domain domain, Server server, NamedEntry.Visitor visitor)
    throws ServiceException {
        if (server != null) {
            SearchAccountsOptions searchOpts = new SearchAccountsOptions(domain);
            searchOpts.setIncludeType(IncludeType.CALENDAR_RESOURCES_ONLY);
            searchAccountsOnServerInternal(server, searchOpts, visitor);
        } else {
            getAllCalendarResources(domain, visitor);
        }
    }

    @Override
    public List<?> getAllDistributionLists(Domain domain) throws ServiceException {
        SearchDirectoryOptions searchOpts = new SearchDirectoryOptions(domain);
        searchOpts.setFilter(mDIT.filterDistributionListsByDomain(domain));
        searchOpts.setTypes(ObjectType.distributionlists);
        searchOpts.setSortOpt(SortOpt.SORT_ASCENDING);
        return searchDirectoryInternal(searchOpts);
    }

    @Override
    public SearchGalResult autoCompleteGal(Domain domain, String query,
            GalSearchType type, int limit, GalContact.Visitor visitor)
    throws ServiceException {
        SearchGalResult searchResult = SearchGalResult.newSearchGalResult(visitor);

        GalSearchParams params = new GalSearchParams(domain, null);
        params.setQuery(query);
        params.setType(type);
        params.setOp(GalOp.autocomplete);
        params.setLimit(limit);
        params.setGalResult(searchResult);

        LdapOnlyGalSearchResultCallback callback =
            new LdapOnlyGalSearchResultCallback(params, visitor);
        params.setResultCallback(callback);

        GalSearchControl gal = new GalSearchControl(params);
        gal.ldapSearch();
        // Collections.sort(searchResult.getMatches());  sort?
        return searchResult;
    }

    @Override
    public SearchGalResult searchGal(Domain domain, String query, GalSearchType type,
            int limit, GalContact.Visitor visitor)
    throws ServiceException {
        SearchGalResult searchResult = SearchGalResult.newSearchGalResult(visitor);

        GalSearchParams params = new GalSearchParams(domain, null);
        params.setQuery(query);
        params.setType(type);
        params.setOp(GalOp.search);
        params.setLimit(limit);
        params.setGalResult(searchResult);

        LdapOnlyGalSearchResultCallback callback =
            new LdapOnlyGalSearchResultCallback(params, visitor);
        params.setResultCallback(callback);

        GalSearchControl gal = new GalSearchControl(params);
        gal.ldapSearch();
        return searchResult;
    }

    @Override
    public SearchGalResult syncGal(Domain domain, String token, GalContact.Visitor visitor)
    throws ServiceException {
        SearchGalResult searchResult = SearchGalResult.newSearchGalResult(visitor);

        GalSearchParams params = new GalSearchParams(domain, null);
        params.setQuery("");
        params.setToken(token);
        params.setType(GalSearchType.all);
        params.setOp(GalOp.sync);
        params.setFetchGroupMembers(true);
        params.setNeedSMIMECerts(true);
        params.setGalResult(searchResult);

        LdapOnlyGalSearchResultCallback callback =
            new LdapOnlyGalSearchResultCallback(params, visitor);
        params.setResultCallback(callback);

        GalSearchControl gal = new GalSearchControl(params);
        gal.ldapSearch();
        return searchResult;
    }

    private static class LdapOnlyGalSearchResultCallback extends GalSearchResultCallback {
        GalContact.Visitor visitor;
        String newToken;
        boolean hasMore;

        LdapOnlyGalSearchResultCallback(GalSearchParams params, GalContact.Visitor visitor) {
            super(params);
            this.visitor = visitor;
        }

        private String getNewToken() {
            return newToken;
        }

        @Override
        public void reset(GalSearchParams params) {
            // do nothing
        }

        @Override
        public void visit(GalContact c) throws ServiceException {
            visitor.visit(c);
        }

        @Override
        public void setNewToken(String newToken) {
            this.newToken = newToken;
        }

        @Override
        public void setHasMoreResult(boolean more) {
            this.hasMore = more;
        }

        @Override
        public void setGalDefinitionLastModified(String timestamp) {
            // do nothing
        }

        @Override
        public Element getResponse() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean passThruProxiedGalAcctResponse() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void handleProxiedResponse(Element resp) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setNewToken(GalSyncToken newToken) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSortBy(String sortBy) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setQueryOffset(int offset) {
            throw new UnsupportedOperationException();
        }

    }

    @Override
    public void searchGal(GalSearchParams params) throws ServiceException {
        LdapGalSearch.galSearch(params);
    }

    @Override
    public SearchGalResult searchGal(Domain d,
                                     String n,
                                     GalSearchType type,
                                     GalMode galMode,
                                     String token) throws ServiceException {
        return searchGal(d, n, type, galMode, token, null);
    }


    private SearchGalResult searchGal(Domain d,
                                      String n,
                                      GalSearchType type,
                                      GalMode galMode,
                                      String token,
                                      GalContact.Visitor visitor)
    throws ServiceException {
        GalOp galOp = token != null ? GalOp.sync : GalOp.search;
        // escape user-supplied string
        n = LdapUtil.escapeSearchFilterArg(n);

        int maxResults = token != null ? 0 : d.getIntAttr(Provisioning.A_zimbraGalMaxResults, DEFAULT_GAL_MAX_RESULTS);
        if (type == GalSearchType.resource)
            return searchResourcesGal(d, n, maxResults, token, galOp, visitor);
        else if (type == GalSearchType.group)
            return searchGroupsGal(d, n, maxResults, null, galOp, null);

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

        if (type == GalSearchType.all) {
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

    private synchronized LdapGalMapRules getGalRules(Domain d, boolean isZimbraGal) {
        LdapGalMapRules rules = (LdapGalMapRules) d.getCachedData(DATA_GAL_RULES);
        if (rules == null) {
            rules = new LdapGalMapRules(d, isZimbraGal);
            d.setCachedData(DATA_GAL_RULES, rules);
        }
        return rules;
    }

    private SearchGalResult searchResourcesGal(Domain d, String n, int maxResults, String token, GalOp galOp, GalContact.Visitor visitor)
    throws ServiceException {
        return searchZimbraWithNamedFilter(d, galOp, GalNamedFilter.getZimbraCalendarResourceFilter(galOp), n, maxResults, token, visitor);
    }

    private SearchGalResult searchGroupsGal(Domain d, String n, int maxResults, String token, GalOp galOp, GalContact.Visitor visitor)
    throws ServiceException {
        return searchZimbraWithNamedFilter(d, galOp, GalNamedFilter.getZimbraGroupFilter(galOp), n, maxResults, token, visitor);
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

        String tokenize = GalUtil.tokenizeKey(galParams, galOp);
        if (queryExpr != null) {
            if (token != null)
                n = "";

            query = GalUtil.expandFilter(tokenize, queryExpr, n, token);
        }

        SearchGalResult result = SearchGalResult.newSearchGalResult(visitor);
        result.setTokenizeKey(tokenize);
        if (query == null) {
            ZimbraLog.gal.warn("searchZimbraWithNamedFilter query is null");
            return result;
        }

        // filter out hidden entries
        if (!query.startsWith("(")) {
            query = "(" + query + ")";
        }
        query = "(&"+query+"(!(zimbraHideInGal=TRUE)))";

        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapUsage.fromGalOpLegacy(galOp));
            LdapGalSearch.searchGal(zlc,
                               GalSearchConfig.GalType.zimbra,
                               galParams.pageSize(),
                               galParams.searchBase(),
                               query,
                               maxResults,
                               getGalRules(domain, true),
                               token,
                               result);
        } finally {
            LdapClient.closeContext(zlc);
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

        LdapGalMapRules rules = getGalRules(domain, false);
        try {
            return LdapGalSearch.searchLdapGal(galParams,
                                          galOp,
                                          n,
                                          maxResults,
                                          rules,
                                          token,
                                          visitor);
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to search GAL", e);
        }
    }

    @Override
    public void addMembers(DistributionList list, String[] members) throws ServiceException {
        LdapDistributionList ldl = (LdapDistributionList) list;
        addDistributionListMembers(ldl, members);
    }

    @Override
    public void removeMembers(DistributionList list, String[] members) throws ServiceException {
        LdapDistributionList ldl = (LdapDistributionList) list;
        removeDistributionListMembers(ldl, members);
    }

    private void addDistributionListMembers(DistributionList dl, String[] members)
    throws ServiceException {
        Set<String> existing = dl.getMultiAttrSet(Provisioning.A_zimbraMailForwardingAddress);
        Set<String> mods = new HashSet<String>();

        // all addrs of this DL
        AddrsOfEntry addrsOfDL = getAllAddressesOfEntry(dl.getName());

        for (int i = 0; i < members.length; i++) {
            String memberName = members[i].toLowerCase();
            memberName = IDNUtil.toAsciiEmail(memberName);

            if (addrsOfDL.isIn(memberName))
                throw ServiceException.INVALID_REQUEST("Cannot add self as a member: " + memberName, null);

            // cannot add a dynamic group as member
            if (getDynamicGroupBasic(Key.DistributionListBy.name, memberName, null) != null) {
                throw ServiceException.INVALID_REQUEST("Cannot add dynamic group as a member: " + memberName, null);
            }

            if (!existing.contains(memberName)) {
                mods.add(memberName);

                // clear the DL cache on accounts/dl

                // can't do prov.getFromCache because it only caches by primary name
                Account acct = get(AccountBy.name, memberName);
                if (acct != null) {
                    clearUpwardMembershipCache(acct);
                } else {
                    // for DistributionList/ACLGroup, get it from cache because
                    // if the dl is not in cache, after loading it prov.getAclGroup
                    // always compute the upward membership.  Sounds silly if we are
                    // about to clean the cache.  If memberName is indeed an alias
                    // of one of the cached DL/ACLGroup, it will get expired after 15
                    // mins, just like the multi-node case.
                    //
                    // Note: do not call clearUpwardMembershipCache for AclGroup because
                    // the upward membership cache for that is computed and cache only when
                    // the entry is loaded/being cached, instead of lazily computed like we
                    // do for account.
                    removeGroupFromCache(Key.DistributionListBy.name, memberName);
                }
            }
        }

        if (mods.isEmpty()) {
            // nothing to do...
            return;
        }

        PermissionCache.invalidateCache();
        cleanGroupMembersCache(dl);

        Map<String,String[]> modmap = new HashMap<String,String[]>();
        modmap.put("+" + Provisioning.A_zimbraMailForwardingAddress, mods.toArray(new String[0]));
        modifyAttrs(dl, modmap, true);
    }

    private void removeDistributionListMembers(DistributionList dl, String[] members)
    throws ServiceException {
        Set<String> curMembers = dl.getMultiAttrSet(Provisioning.A_zimbraMailForwardingAddress);

        // bug 46219, need a case insentitive Set
        Set<String> existing = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        existing.addAll(curMembers);

        Set<String> mods = new HashSet<String>();
        HashSet<String> failed = new HashSet<String>();

        for (int i = 0; i < members.length; i++) {
            String memberName = members[i].toLowerCase();
            memberName = IDNUtil.toAsciiEmail(memberName);
            if (memberName.length() == 0) {
                throw ServiceException.INVALID_REQUEST("invalid member email address: " + memberName, null);
            }
            // We do not do any further validation of the remove address for
            // syntax - removes should be liberal so any bad entries added by
            // some other means can be removed
            //
            // members[] can contain:
            //   - the primary address of an account or another DL
            //   - an alias of an account or another DL
            //   - junk (allAddrs will be returned as null)
            AddrsOfEntry addrsOfEntry = getAllAddressesOfEntry(memberName);
            List<String> allAddrs = addrsOfEntry.getAll();

            if (mods.contains(memberName)) {
                // already been added in mods (is the primary or alias of previous entries in members[])
            } else if (existing.contains(memberName)) {
                if (!allAddrs.isEmpty()) {
                    mods.addAll(allAddrs);
                } else {
                    mods.add(memberName);  // just get rid of it regardless what it is
                }
            } else {
                boolean inList = false;
                if (allAddrs.size() > 0) {
                    // go through all addresses of the entry see if any is on the DL
                    for (String addr : allAddrs) {
                        if (!inList) {
                            break;
                        }
                        if (existing.contains(addr)) {
                            mods.addAll(allAddrs);
                            inList = true;
                        }
                    }
                }
                if (!inList) {
                    failed.add(memberName);
                }
            }

            // clear the DL cache on accounts/dl
            String primary = addrsOfEntry.getPrimary();
            if (primary != null) {
                if (addrsOfEntry.isAccount()) {
                    Account acct = getFromCache(AccountBy.name, primary);
                    if (acct != null)
                        clearUpwardMembershipCache(acct);
                } else {
                    removeGroupFromCache(Key.DistributionListBy.name, primary);
                }
            }
        }

        if (!failed.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            Iterator<String> iter = failed.iterator();
            while (true) {
                sb.append(iter.next());
                if (!iter.hasNext())
                    break;
                sb.append(",");
            }
            throw AccountServiceException.NO_SUCH_MEMBER(dl.getName(), sb.toString());
        }

        if (mods.isEmpty()) {
            throw ServiceException.INVALID_REQUEST("empty remove set", null);
        }

        PermissionCache.invalidateCache();
        cleanGroupMembersCache(dl);

        Map<String,String[]> modmap = new HashMap<String,String[]>();
        modmap.put("-" + Provisioning.A_zimbraMailForwardingAddress, mods.toArray(new String[0]));
        modifyAttrs(dl, modmap);

    }

    private void clearUpwardMembershipCache(Account acct) {
        acct.setCachedData(EntryCacheDataKey.ACCOUNT_DLS, null);
        acct.setCachedData(EntryCacheDataKey.ACCOUNT_DIRECT_DLS, null);
        acct.setCachedData(EntryCacheDataKey.GROUPEDENTRY_MEMBERSHIP, null);
        acct.setCachedData(EntryCacheDataKey.GROUPEDENTRY_MEMBERSHIP_ADMINS_ONLY, null);
        acct.setCachedData(EntryCacheDataKey.GROUPEDENTRY_DIRECT_GROUPIDS.getKeyName(), null);
    }

    private class AddrsOfEntry {
        List<String> mAllAddrs = new ArrayList<String>(); // including primary
        String mPrimary = null;  // primary addr
        boolean mIsAccount = false;

        void setPrimary(String primary) {
            mPrimary = primary;
            add(primary);
        }

        void setIsAccount(boolean isAccount) {
            mIsAccount = isAccount;
        }

        void add(String addr) {
            mAllAddrs.add(addr);
        }

        void addAll(String[] addrs) {
            mAllAddrs.addAll(Arrays.asList(addrs));
        }

        List<String> getAll() {
            return mAllAddrs;
        }

        String getPrimary() {
            return mPrimary;
        }

        boolean isAccount() {
            return mIsAccount;
        }

        boolean isIn(String addr) {
            return mAllAddrs.contains(addr.toLowerCase());
        }
    }


    //
    // returns the primary address and all aliases of the named account or DL
    //
    private AddrsOfEntry getAllAddressesOfEntry(String name) {

        String primary = null;
        String aliases[] = null;
        AddrsOfEntry addrs = new AddrsOfEntry();

        try {
            // bug 56621.  Do not count implicit aliases (aliases resolved by alias domain)
            // when dealing with distribution list members.
            Account acct = getAccountByName(name, false, false);
            if (acct != null) {
                addrs.setIsAccount(true);
                primary = acct.getName();
                aliases = acct.getMailAlias();
            } else {
                DistributionList dl = get(Key.DistributionListBy.name, name);
                if (dl != null) {
                    primary = dl.getName();
                    aliases = dl.getAliases();
                }
            }
        } catch (ServiceException se) {
            // swallow any exception and go on
        }

        if (primary != null)
            addrs.setPrimary(primary);
        if (aliases != null)
            addrs.addAll(aliases);

        return addrs;
    }

    private List<Identity> getIdentitiesByQuery(LdapEntry entry, ZLdapFilter filter, ZLdapContext initZlc)
    throws ServiceException {
        List<Identity> result = new ArrayList<Identity>();

        try {
            String base = entry.getDN();
            ZSearchResultEnumeration ne = helper.searchDir(base, filter,
                    ZSearchControls.SEARCH_CTLS_SUBTREE(), initZlc, LdapServerType.REPLICA);
            while(ne.hasMore()) {
                ZSearchResultEntry sr = ne.next();
                result.add(new LdapIdentity((Account)entry, sr.getDN(), sr.getAttributes(), this));
            }
            ne.close();
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to lookup identity via query: "+
                    filter.toFilterString() + " message: "+e.getMessage(), e);
        }
        return result;
    }

    private Identity getIdentityByName(LdapEntry entry, String name,  ZLdapContext zlc) throws ServiceException {
        List<Identity> result = getIdentitiesByQuery(entry, filterFactory.identityByName(name), zlc);
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
    public Identity createIdentity(Account account, String identityName,
            Map<String, Object> identityAttrs)
    throws ServiceException {
        return createIdentity(account, identityName, identityAttrs, false);
    }

    @Override
    public Identity restoreIdentity(Account account, String identityName,
            Map<String, Object> identityAttrs)
    throws ServiceException {
        return createIdentity(account, identityName, identityAttrs, true);
    }

    private Identity createIdentity(Account account, String identityName,
            Map<String, Object> identityAttrs, boolean restoring)
    throws ServiceException {
        removeAttrIgnoreCase("objectclass", identityAttrs);
        validateIdentityAttrs(identityAttrs);

        LdapEntry ldapEntry = (LdapEntry) (account instanceof LdapEntry ? account : getAccountById(account.getId()));

        if (ldapEntry == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(account.getName());

        if (identityName.equalsIgnoreCase(ProvisioningConstants.DEFAULT_IDENTITY_NAME))
                throw AccountServiceException.IDENTITY_EXISTS(identityName);

        List<Identity> existing = getAllIdentities(account);
        if (existing.size() >= account.getLongAttr(A_zimbraIdentityMaxNumEntries, 20))
            throw AccountServiceException.TOO_MANY_IDENTITIES();

        account.setCachedData(IDENTITY_LIST_CACHE_KEY, null);

        boolean checkImmutable = !restoring;
        CallbackContext callbackContext = new CallbackContext(CallbackContext.Op.CREATE);
        AttributeManager.getInstance().preModify(identityAttrs, null, callbackContext, checkImmutable);

        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.CREATE_IDENTITY);

            String dn = getIdentityDn(ldapEntry, identityName);

            ZMutableEntry entry = LdapClient.createMutableEntry();
            entry.setDN(dn);
            entry.mapToAttrs(identityAttrs);

            entry.setAttr(A_objectClass, "zimbraIdentity");

            if (!entry.hasAttribute(A_zimbraPrefIdentityId)) {
                String identityId = LdapUtil.generateUUID();
                entry.setAttr(A_zimbraPrefIdentityId, identityId);
            }
            entry.setAttr(Provisioning.A_zimbraCreateTimestamp, LdapDateUtil.toGeneralizedTime(new Date()));

            zlc.createEntry(entry);

            Identity identity = getIdentityByName(ldapEntry, identityName, zlc);
            AttributeManager.getInstance().postModify(identityAttrs, identity, callbackContext);

            return identity;
        } catch (LdapEntryAlreadyExistException nabe) {
            throw AccountServiceException.IDENTITY_EXISTS(identityName);
        } catch (LdapException e) {
            throw e;
        } catch (AccountServiceException e) {
            throw e;
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to create identity " + identityName, e);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    @Override
    public void modifyIdentity(Account account, String identityName,
            Map<String, Object> identityAttrs)
    throws ServiceException {
        removeAttrIgnoreCase("objectclass", identityAttrs);

        validateIdentityAttrs(identityAttrs);

        LdapEntry ldapEntry = (LdapEntry) (account instanceof LdapEntry ? account : getAccountById(account.getId()));

        if (ldapEntry == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(account.getName());

        // clear cache
        account.setCachedData(IDENTITY_LIST_CACHE_KEY, null);

        if (identityName.equalsIgnoreCase(ProvisioningConstants.DEFAULT_IDENTITY_NAME)) {
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

    private void renameIdentity(LdapEntry entry, LdapIdentity identity, String newIdentityName)
    throws ServiceException {

        if (identity.getName().equalsIgnoreCase(ProvisioningConstants.DEFAULT_IDENTITY_NAME))
            throw ServiceException.INVALID_REQUEST("can't rename default identity", null);

        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.RENAME_IDENTITY);
            String newDn = getIdentityDn(entry, newIdentityName);
            zlc.renameEntry(identity.getDN(), newDn);
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to rename identity: "+newIdentityName, e);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    @Override
    public void deleteIdentity(Account account, String identityName) throws ServiceException {
        LdapEntry ldapEntry = (LdapEntry) (account instanceof LdapEntry ? account : getAccountById(account.getId()));
        if (ldapEntry == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(account.getName());

        if (identityName.equalsIgnoreCase(ProvisioningConstants.DEFAULT_IDENTITY_NAME))
            throw ServiceException.INVALID_REQUEST("can't delete default identity", null);

        account.setCachedData(IDENTITY_LIST_CACHE_KEY, null);

        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.DELETE_IDENTITY);
            Identity identity = getIdentityByName(ldapEntry, identityName, zlc);
            if (identity == null)
                throw AccountServiceException.NO_SUCH_IDENTITY(identityName);
            String dn = getIdentityDn(ldapEntry, identityName);
            zlc.deleteEntry(dn);
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to delete identity: "+identityName, e);
        } finally {
            LdapClient.closeContext(zlc);
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

        result = getIdentitiesByQuery(ldapEntry, filterFactory.allIdentities(), null);
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
    public Identity get(Account account, Key.IdentityBy keyType, String key)
    throws ServiceException {
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

    private List<Signature> getSignaturesByQuery(Account acct, LdapEntry entry, ZLdapFilter filter,
            ZLdapContext initZlc, List<Signature> result) throws ServiceException {
        if (result == null) {
            result = new ArrayList<Signature>();
        }

        try {
            String base = entry.getDN();
            ZSearchResultEnumeration ne = helper.searchDir(base, filter,
                    ZSearchControls.SEARCH_CTLS_SUBTREE(), initZlc, LdapServerType.REPLICA);
            while(ne.hasMore()) {
                ZSearchResultEntry sr = ne.next();
                result.add(new LdapSignature(acct, sr.getDN(), sr.getAttributes(), this));
            }
            ne.close();
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to lookup signature via query: "+
                    filter.toFilterString() + " message: " + e.getMessage(), e);
        }
        return result;
    }

    private Signature getSignatureById(Account acct, LdapEntry entry, String id,  ZLdapContext zlc)
    throws ServiceException {
        List<Signature> result = getSignaturesByQuery(acct, entry, filterFactory.signatureById(id), zlc, null);
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

        // If the signature id is supplied with the request, check that it
        // is not associated with an existing signature
        String signatureId = (String)signatureAttrs.get(Provisioning.A_zimbraSignatureId);
        if (signatureId != null) {
           for (Signature signature : existing) {
               if (signatureId.equals(signature.getAttr(Provisioning.A_zimbraSignatureId))) {
                   throw AccountServiceException.SIGNATURE_EXISTS(signatureId);
               }
           }
        }


        int numSigs = existing.size();
        if (numSigs >= account.getLongAttr(A_zimbraSignatureMaxNumEntries, 20))
            throw AccountServiceException.TOO_MANY_SIGNATURES();
        else if (numSigs == 0)
            setAsDefault = true;

        account.setCachedData(SIGNATURE_LIST_CACHE_KEY, null);

        boolean checkImmutable = !restoring;
        CallbackContext callbackContext = new CallbackContext(CallbackContext.Op.CREATE);
        callbackContext.setData(DataKey.MAX_SIGNATURE_LEN,
                String.valueOf(account.getMailSignatureMaxLength()));
        AttributeManager.getInstance().preModify(signatureAttrs, null,
                callbackContext, checkImmutable);


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

        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.CREATE_SIGNATURE);

            String dn = getSignatureDn(ldapEntry, signatureName);

            ZMutableEntry entry = LdapClient.createMutableEntry();
            entry.mapToAttrs(signatureAttrs);

            entry.setAttr(A_objectClass, "zimbraSignature");
            entry.setAttr(Provisioning.A_zimbraCreateTimestamp, LdapDateUtil.toGeneralizedTime(new Date()));

            entry.setDN(dn);
            zlc.createEntry(entry);

            Signature signature = getSignatureById(account, ldapEntry, signatureId, zlc);
            AttributeManager.getInstance().postModify(signatureAttrs, signature, callbackContext);

            if (setAsDefault)
                setDefaultSignature(account, signatureId);

            return signature;
        } catch (LdapEntryAlreadyExistException nabe) {
            throw AccountServiceException.SIGNATURE_EXISTS(signatureName);
        } catch (LdapException e) {
            throw e;
        } catch (AccountServiceException e) {
            throw e;
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to create signature: " + signatureName, e);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    @Override
    public void modifySignature(Account account, String signatureId, Map<String, Object> signatureAttrs)
    throws ServiceException {
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
                // the signature cache could've been loaded again if getAllSignatures
                // were called in pre/poseModify callback, so we clear it again
                account.setCachedData(SIGNATURE_LIST_CACHE_KEY, null);
                renameSignature(ldapEntry, signature, newName);
            }

        }
    }

    private void renameSignature(LdapEntry entry, LdapSignature signature, String newSignatureName)
    throws ServiceException {
        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.RENAME_SIGNATURE);
            String newDn = getSignatureDn(entry, newSignatureName);
            zlc.renameEntry(signature.getDN(), newDn);
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to rename signature: "+newSignatureName, e);
        } finally {
            LdapClient.closeContext(zlc);
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
        } else {
            ZLdapContext zlc = null;
            try {
                zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.DELETE_SIGNATURE);
                Signature signature = getSignatureById(account, ldapEntry, signatureId, zlc);
                if (signature == null)
                    throw AccountServiceException.NO_SUCH_SIGNATURE(signatureId);
                String dn = getSignatureDn(ldapEntry, signature.getName());
                zlc.deleteEntry(dn);
            } catch (ServiceException e) {
                throw ServiceException.FAILURE("unable to delete signarure: " + signatureId, e);
            } finally {
                LdapClient.closeContext(zlc);
            }
        }

        if (signatureId.equals(account.getPrefDefaultSignatureId())) {
            account.unsetPrefDefaultSignatureId();
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

        result = getSignaturesByQuery(account, ldapEntry, filterFactory.allSignatures(), null, result);

        result = Collections.unmodifiableList(result);
        account.setCachedData(SIGNATURE_LIST_CACHE_KEY, result);
        return result;
    }

    @Override
    public Signature get(Account account, Key.SignatureBy keyType, String key)
    throws ServiceException {
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

    private List<DataSource> getDataSourcesByQuery(LdapEntry entry, ZLdapFilter filter, ZLdapContext initZlc)
    throws ServiceException {
        List<DataSource> result = new ArrayList<DataSource>();

        try {
            String base = entry.getDN();
            ZSearchResultEnumeration ne = helper.searchDir(base, filter,
                    ZSearchControls.SEARCH_CTLS_SUBTREE(), initZlc, LdapServerType.REPLICA);
            while(ne.hasMore()) {
                ZSearchResultEntry sr = ne.next();
                result.add(new LdapDataSource((Account)entry, sr.getDN(), sr.getAttributes(), this));
            }
            ne.close();
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to lookup data source via query: "+
                    filter.toFilterString() + " message: " + e.getMessage(), e);
        }
        return result;
    }

    private DataSource getDataSourceById(LdapEntry entry, String id,  ZLdapContext zlc)
    throws ServiceException {
        List<DataSource> result = getDataSourcesByQuery(entry, filterFactory.dataSourceById(id), zlc);
        return result.isEmpty() ? null : result.get(0);
    }

    private String getDataSourceDn(LdapEntry entry, String name) {
        return A_zimbraDataSourceName + "=" + LdapUtil.escapeRDNValue(name) + "," + entry.getDN();
    }

    protected ReplaceAddressResult replaceMailAddresses(Entry entry, String attrName,
            String oldAddr, String newAddr)
    throws ServiceException {
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

    // MOVE OVER ALL aliases. doesn't throw an exception, just logs
    // There could be a race condition that the alias might get taken
    // in the new domain post the check.  Anyone who calls this API must
    // pay attention to the warning message
    private void moveAliases(ZLdapContext zlc, ReplaceAddressResult addrs,
            String newDomain, String primaryUid,
            String targetOldDn, String targetNewDn,
            String targetOldDomain, String targetNewDomain)
    throws ServiceException {

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
                    } catch (LdapEntryAlreadyExistException nabe) {
                        ZimbraLog.account.warn("unable to move alias from " + oldAliasDN + " to " + newAliasDN, nabe);
                    } catch (ServiceException ne) {
                        throw ServiceException.FAILURE("unable to move aliases", null);
                    } finally {
                    }
                }
             }
        }
    }


    @Override
    public DataSource createDataSource(Account account, DataSourceType dsType,
            String dsName, Map<String, Object> dataSourceAttrs)
    throws ServiceException {
        return createDataSource(account, dsType, dsName, dataSourceAttrs, false);
    }

    @Override
    public DataSource createDataSource(Account account, DataSourceType type,
            String dataSourceName, Map<String, Object> attrs,
            boolean passwdAlreadyEncrypted)
    throws ServiceException {
        return createDataSource(account, type, dataSourceName, attrs, passwdAlreadyEncrypted, false);
    }

    @Override
    public DataSource restoreDataSource(Account account, DataSourceType dsType,
            String dsName, Map<String, Object> dataSourceAttrs)
    throws ServiceException {
        return createDataSource(account, dsType, dsName, dataSourceAttrs, true, true);
    }

    private DataSource createDataSource(Account account, DataSourceType dsType,
            String dsName, Map<String, Object> dataSourceAttrs,
            boolean passwdAlreadyEncrypted, boolean restoring)
    throws ServiceException {
        removeAttrIgnoreCase("objectclass", dataSourceAttrs);
        LdapEntry ldapEntry = (LdapEntry) (account instanceof LdapEntry ? account : getAccountById(account.getId()));

        if (ldapEntry == null) {
            throw AccountServiceException.NO_SUCH_ACCOUNT(account.getName());
        }

        List<DataSource> existing = getAllDataSources(account);
        if (existing.size() >= account.getLongAttr(A_zimbraDataSourceMaxNumEntries, 20)) {
            throw AccountServiceException.TOO_MANY_DATA_SOURCES();
        }
        String dsEmailAddr = (String) dataSourceAttrs.get(A_zimbraDataSourceEmailAddress);
        if (!StringUtil.isNullOrEmpty(dsEmailAddr)) {
            for (DataSource ds : existing) {
                if (dsEmailAddr.equals(ds.getEmailAddress())) {
                    throw AccountServiceException.DATA_SOURCE_EXISTS(dsEmailAddr);
                }
            }
        }

        dataSourceAttrs.put(A_zimbraDataSourceName, dsName); // must be the same
        dataSourceAttrs.put(Provisioning.A_zimbraDataSourceType, dsType.toString());

        account.setCachedData(DATA_SOURCE_LIST_CACHE_KEY, null);

        boolean checkImmutable = !restoring;
        CallbackContext callbackContext = new CallbackContext(CallbackContext.Op.CREATE);
        AttributeManager.getInstance().preModify(dataSourceAttrs, null, callbackContext, checkImmutable);

        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.CREATE_DATASOURCE);

            String dn = getDataSourceDn(ldapEntry, dsName);

            ZMutableEntry entry = LdapClient.createMutableEntry();
            entry.setDN(dn);
            entry.mapToAttrs(dataSourceAttrs);

            entry.setAttr(A_objectClass, "zimbraDataSource");
            String extraOc = LdapDataSource.getObjectClass(dsType);
            if (extraOc != null) {
                entry.addAttr(A_objectClass, Sets.newHashSet(extraOc));
            }

            String dsId = entry.getAttrString(A_zimbraDataSourceId);
            if (dsId == null) {
                dsId = LdapUtil.generateUUID();
                entry.setAttr(A_zimbraDataSourceId, dsId);
            }

            String password = entry.getAttrString(A_zimbraDataSourcePassword);
            if (password != null) {
                String encrypted = passwdAlreadyEncrypted ? password : DataSource.encryptData(dsId, password);
                entry.setAttr(A_zimbraDataSourcePassword, encrypted);
            }
            String oauthToken = entry.getAttrString(A_zimbraDataSourceOAuthToken);
            if (oauthToken != null) {
                String encrypted = passwdAlreadyEncrypted ? oauthToken : DataSource.encryptData(dsId, oauthToken);
                entry.setAttr(A_zimbraDataSourceOAuthToken, encrypted);
            }
            String clientSecret = entry.getAttrString(A_zimbraDataSourceOAuthClientSecret);
            if (clientSecret != null) {
                String encrypted = passwdAlreadyEncrypted ? clientSecret : DataSource.encryptData(
                    dsId, clientSecret);
                entry.setAttr(A_zimbraDataSourceOAuthClientSecret, encrypted);
            }
            String smtpPassword = entry.getAttrString(A_zimbraDataSourceSmtpAuthPassword);
            if (smtpPassword != null) {
                String encrypted = passwdAlreadyEncrypted ? smtpPassword : DataSource.encryptData(dsId, smtpPassword);
                entry.setAttr(A_zimbraDataSourceSmtpAuthPassword, encrypted);
            }
            entry.setAttr(Provisioning.A_zimbraCreateTimestamp, LdapDateUtil.toGeneralizedTime(new Date()));

            zlc.createEntry(entry);

            DataSource ds = getDataSourceById(ldapEntry, dsId, zlc);
            AttributeManager.getInstance().postModify(dataSourceAttrs, ds, callbackContext);
            return ds;
        } catch (LdapEntryAlreadyExistException nabe) {
            throw AccountServiceException.DATA_SOURCE_EXISTS(dsName);
        } catch (LdapException e) {
            throw e;
        } catch (AccountServiceException e) {
            throw e;
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to create data source: " + dsName, e);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    @Override
    public void deleteDataSource(Account account, String dataSourceId) throws ServiceException {
        LdapEntry ldapEntry = (LdapEntry) (account instanceof LdapEntry ? account : getAccountById(account.getId()));
        if (ldapEntry == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(account.getName());

        account.setCachedData(DATA_SOURCE_LIST_CACHE_KEY, null);

        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.DELETE_DATASOURCE);
            DataSource dataSource = getDataSourceById(ldapEntry, dataSourceId, zlc);
            if (dataSource == null)
                throw AccountServiceException.NO_SUCH_DATA_SOURCE(dataSourceId);
            String dn = getDataSourceDn(ldapEntry, dataSource.getName());
            zlc.deleteEntry(dn);
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to delete data source: "+dataSourceId, e);
        } finally {
            LdapClient.closeContext(zlc);
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
        result = getDataSourcesByQuery(ldapEntry, filterFactory.allDataSources(), null);
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
    public void modifyDataSource(Account account, String dataSourceId, Map<String, Object> attrs)
    throws ServiceException {
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
        String oauthToken = (String) attrs.get(A_zimbraDataSourceOAuthToken);
        if (oauthToken != null) {
            attrs.put(A_zimbraDataSourceOAuthToken, DataSource.encryptData(ds.getId(), oauthToken));
        }
        String clientSecret = (String) attrs.get(A_zimbraDataSourceOAuthClientSecret);
        if (clientSecret != null) {
            attrs.put(A_zimbraDataSourceOAuthClientSecret,
                DataSource.encryptData(ds.getId(), clientSecret));
        }
        String smtpPassword = (String) attrs.get(A_zimbraDataSourceSmtpAuthPassword);
        if (smtpPassword != null) {
            attrs.put(A_zimbraDataSourceSmtpAuthPassword, DataSource.encryptData(ds.getId(), smtpPassword));
        }

        modifyAttrs(ds, attrs, true);
        if (newName) {
            // the datasoruce cache could've been loaded again if getAllDataSources were called in pre/poseModify callback, so we clear it again
            account.setCachedData(DATA_SOURCE_LIST_CACHE_KEY, null);
            ZLdapContext zlc = null;
            try {
                zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.RENAME_DATASOURCE);
                String newDn = getDataSourceDn(ldapEntry, name);
                zlc.renameEntry(ds.getDN(), newDn);
            } catch (ServiceException e) {
                throw ServiceException.FAILURE("unable to rename datasource: "+name, e);
            } finally {
                LdapClient.closeContext(zlc);
            }
        }
    }

    @Override
    public DataSource get(Account account, Key.DataSourceBy keyType, String key)
    throws ServiceException {
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

    private XMPPComponent getXMPPComponentByQuery(ZLdapFilter filter, ZLdapContext initZlc)
    throws ServiceException {
        try {
            ZSearchResultEntry sr = helper.searchForEntry(mDIT.xmppcomponentBaseDN(), filter, initZlc, false);
            if (sr != null) {
                return new LdapXMPPComponent(sr.getDN(), sr.getAttributes(), this);
            }
        } catch (LdapMultipleEntriesMatchedException e) {
            throw AccountServiceException.MULTIPLE_ENTRIES_MATCHED("getXMPPComponentByQuery", e);
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to lookup XMPP component via query: "+
                    filter.toFilterString() + " message:" + e.getMessage(), e);
        }
        return null;
    }

    private XMPPComponent getXMPPComponentByName(String name, boolean nocache)
    throws ServiceException {
        if (!nocache) {
            XMPPComponent x = xmppComponentCache.getByName(name);
            if (x != null)
                return x;
        }

        try {
            String dn = mDIT.xmppcomponentNameToDN(name);
            ZAttributes attrs = helper.getAttributes(LdapUsage.GET_XMPPCOMPONENT, dn);
            XMPPComponent x = new LdapXMPPComponent(dn, attrs, this);
            xmppComponentCache.put(x);
            return x;
        } catch (LdapEntryNotFoundException e) {
            return null;
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to lookup xmpp component by name: "+name+" message: "+e.getMessage(), e);
        }
    }

    private XMPPComponent getXMPPComponentById(String zimbraId, ZLdapContext zlc, boolean nocache)
    throws ServiceException {
        if (zimbraId == null)
            return null;
        XMPPComponent x = null;
        if (!nocache)
            x = xmppComponentCache.getById(zimbraId);
        if (x == null) {
            x = getXMPPComponentByQuery(filterFactory.xmppComponentById(zimbraId), zlc);
            xmppComponentCache.put(x);
        }
        return x;
    }

    @Override
    public List<XMPPComponent> getAllXMPPComponents() throws ServiceException {
        List<XMPPComponent> result = new ArrayList<XMPPComponent>();

        try {
            String base = mDIT.xmppcomponentBaseDN();
            ZLdapFilter filter = filterFactory.allXMPPComponents();

            ZSearchResultEnumeration ne = helper.searchDir(base, filter, ZSearchControls.SEARCH_CTLS_SUBTREE());
            while (ne.hasMore()) {
                ZSearchResultEntry sr = ne.next();
                LdapXMPPComponent x = new LdapXMPPComponent(sr.getDN(), sr.getAttributes(), this);
                result.add(x);
            }
            ne.close();
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to list all XMPPComponents", e);
        }

        if (result.size() > 0) {
            xmppComponentCache.put(result, true);
        }
        Collections.sort(result);
        return result;
    }

    @Override
    public XMPPComponent createXMPPComponent(String name, Domain domain, Server server,
            Map<String, Object> inAttrs)
    throws ServiceException {
        name = name.toLowerCase().trim();

        // sanity checking
        removeAttrIgnoreCase("objectclass", inAttrs);
        removeAttrIgnoreCase(A_zimbraDomainId, inAttrs);
        removeAttrIgnoreCase(A_zimbraServerId, inAttrs);

        CallbackContext callbackContext = new CallbackContext(CallbackContext.Op.CREATE);
        AttributeManager.getInstance().preModify(inAttrs, null, callbackContext, true);

        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.CREATE_XMPPCOMPONENT);

            ZMutableEntry entry = LdapClient.createMutableEntry();
            entry.mapToAttrs(inAttrs);

            entry.setAttr(A_objectClass, "zimbraXMPPComponent");

            String compId = LdapUtil.generateUUID();
            entry.setAttr(A_zimbraId, compId);
            entry.setAttr(A_zimbraCreateTimestamp, LdapDateUtil.toGeneralizedTime(new Date()));
            entry.setAttr(A_cn, name);
            String dn = mDIT.xmppcomponentNameToDN(name);
            entry.setDN(dn);

            entry.setAttr(A_zimbraDomainId, domain.getId());
            entry.setAttr(A_zimbraServerId, server.getId());

            zlc.createEntry(entry);

            XMPPComponent comp = getXMPPComponentById(compId, zlc, true);
            AttributeManager.getInstance().postModify(inAttrs, comp, callbackContext);
            return comp;
        } catch (LdapEntryAlreadyExistException nabe) {
            throw AccountServiceException.IM_COMPONENT_EXISTS(name);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    @Override
    public XMPPComponent get(Key.XMPPComponentBy keyType, String key) throws ServiceException {
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
        ZLdapContext zlc = null;
        LdapXMPPComponent l = (LdapXMPPComponent)get(Key.XMPPComponentBy.id, zimbraId);
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.DELETE_XMPPCOMPONENT);
            zlc.deleteEntry(l.getDN());
            xmppComponentCache.remove(l);
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to purge XMPPComponent : "+zimbraId, e);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    // Only called from renameDomain for now
    void renameXMPPComponent(String zimbraId, String newName) throws ServiceException {
        LdapXMPPComponent comp = (LdapXMPPComponent)get(Key.XMPPComponentBy.id, zimbraId);
        if (comp == null)
            throw AccountServiceException.NO_SUCH_XMPP_COMPONENT(zimbraId);

        newName = newName.toLowerCase().trim();
        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.RENAME_XMPPCOMPONENT);
            String newDn = mDIT.xmppcomponentNameToDN(newName);
            zlc.renameEntry(comp.getDN(), newDn);
            // remove old comp from cache
            xmppComponentCache.remove(comp);
        } catch (LdapEntryAlreadyExistException nabe) {
            throw AccountServiceException.IM_COMPONENT_EXISTS(newName);
        } catch (LdapException e) {
            throw e;
        } catch (AccountServiceException e) {
            throw e;
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to rename XMPPComponent: "+zimbraId, e);
        } finally {
            LdapClient.closeContext(zlc);
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
            throw ServiceException.FAILURE("expandAllAttrs is not supported", null);
        return RightCommand.getRight(rightName);
    }

    @Override
    /*
     * from zmprov -l, we don't expand all attrs, expandAllAttrs is ignored
     */
    public List<Right> getAllRights(String targetType, boolean expandAllAttrs, String rightClass)
    throws ServiceException {
        if (expandAllAttrs)
            throw ServiceException.FAILURE("expandAllAttrs == TRUE is not supported", null);
        return RightCommand.getAllRights(targetType, rightClass);
    }

    @Override
    public boolean checkRight(String targetType, TargetBy targetBy, String target,
                              GranteeBy granteeBy, String granteeVal,
                              String right, Map<String, Object> attrs,
                              AccessManager.ViaGrant via) throws ServiceException {
        MailTarget grantee = null;

        try {
            NamedEntry ne = GranteeType.lookupGrantee(this, GranteeType.GT_EMAIL, granteeBy, granteeVal);
            if (ne instanceof MailTarget) {
                grantee = (MailTarget) ne;
            }
        } catch (ServiceException e) {
        }
        if (grantee == null) {
            grantee = new GuestAccount(granteeVal, null);
        }

        return RightCommand.checkRight(this, targetType, targetBy, target, grantee, right, attrs, via);
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
            Key.DomainBy domainBy, String domainStr,
            Key.CosBy cosBy, String cosStr,
            GranteeBy granteeBy, String grantee)
    throws ServiceException {
        return RightCommand.getCreateObjectAttrs(this,
                                                 targetType,
                                                 domainBy, domainStr,
                                                 cosBy, cosStr,
                                                 granteeBy, grantee);
    }

    @Override
    public RightCommand.Grants getGrants(String targetType, TargetBy targetBy, String target,
            String granteeType, GranteeBy granteeBy, String grantee,
            boolean granteeIncludeGroupsGranteeBelongs)
    throws ServiceException {
        return RightCommand.getGrants(this, targetType, targetBy, target,
                granteeType, granteeBy, grantee, granteeIncludeGroupsGranteeBelongs);
    }

    @Override
    public void grantRight(String targetType, TargetBy targetBy, String target,
            String granteeType, GranteeBy granteeBy, String grantee, String secret,
            String right, RightModifier rightModifier)
    throws ServiceException {
        RightCommand.grantRight(this,
                                null,
                                targetType, targetBy, target,
                                granteeType, granteeBy, grantee, secret,
                                right, rightModifier);
    }

    @Override
    public void revokeRight(String targetType, TargetBy targetBy, String target,
            String granteeType, GranteeBy granteeBy, String grantee,
            String right, RightModifier rightModifier)
    throws ServiceException {
         RightCommand.revokeRight(this,
                                  null,
                                  targetType, targetBy, target,
                                  granteeType, granteeBy, grantee,
                                  right, rightModifier);
    }

    @Override
    public void flushCache(CacheEntryType type, CacheEntry[] entries) throws ServiceException {

        switch (type) {
        case all:
            if (entries != null) {
                throw ServiceException.INVALID_REQUEST("cannot specify entry for flushing all", null);
            }
            ZimbraLog.account.info("Flushing all LDAP entry caches");
            flushCache(CacheEntryType.account, null);
            flushCache(CacheEntryType.group, null);
            flushCache(CacheEntryType.config, null);
            flushCache(CacheEntryType.globalgrant, null);
            flushCache(CacheEntryType.cos, null);
            flushCache(CacheEntryType.domain, null);
            flushCache(CacheEntryType.mime, null);
            flushCache(CacheEntryType.server, null);
            flushCache(CacheEntryType.alwaysOnCluster, null);
            flushCache(CacheEntryType.zimlet, null);
            break;
        case account:
            if (entries != null) {
                for (CacheEntry entry : entries) {
                    AccountBy accountBy = (entry.mEntryBy==Key.CacheEntryBy.id)? AccountBy.id : AccountBy.name;
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
                    if (account != null) {
                        removeFromCache(account);
                    }
                }
            } else {
                accountCache.clear();
            }
            return;
        case group:
            if (entries != null) {
                for (CacheEntry entry : entries) {
                    Key.DistributionListBy dlBy = (entry.mEntryBy==Key.CacheEntryBy.id)?
                            Key.DistributionListBy.id : Key.DistributionListBy.name;
                    removeGroupFromCache(dlBy, entry.mEntryIdentity);
                }
            } else {
                allDLs.clear();
                groupCache.clear();
            }
            return;
        case config:
            if (entries != null) {
                throw ServiceException.INVALID_REQUEST("cannot specify entry for flushing global config", null);
            }
            Config config = getConfig();
            reload(config, false);
            EphemeralStore.clearFactory();
            return;
        case globalgrant:
            if (entries != null) {
                throw ServiceException.INVALID_REQUEST("cannot specify entry for flushing global grant", null);
            }
            GlobalGrant globalGrant = getGlobalGrant();
            reload(globalGrant, false);
            return;
        case cos:
            if (entries != null) {
                for (CacheEntry entry : entries) {
                    Key.CosBy cosBy = (entry.mEntryBy==Key.CacheEntryBy.id)? Key.CosBy.id : Key.CosBy.name;
                    Cos cos = getFromCache(cosBy, entry.mEntryIdentity);
                    if (cos != null)
                        reload(cos, false);
                }
            } else
                cosCache.clear();
            return;
        case domain:
            if (entries != null) {
                for (CacheEntry entry : entries) {
                    Key.DomainBy domainBy = (entry.mEntryBy==Key.CacheEntryBy.id)? Key.DomainBy.id : Key.DomainBy.name;
                    Domain domain = getFromCache(domainBy, entry.mEntryIdentity, GetFromDomainCacheOption.BOTH);
                    if (domain != null) {
                        if (domain instanceof DomainCache.NonExistingDomain)
                            domainCache.removeFromNegativeCache(domainBy, entry.mEntryIdentity);
                        else
                            reload(domain, false);
                    }
                }
            } else
                domainCache.clear();
            return;
        case mime:
            mimeTypeCache.flushCache(this);
            return;
        case server:
            if (entries != null) {
                for (CacheEntry entry : entries) {
                    Key.ServerBy serverBy = (entry.mEntryBy==Key.CacheEntryBy.id)? Key.ServerBy.id : Key.ServerBy.name;
                    Server server = get(serverBy, entry.mEntryIdentity);
                    if (server != null)
                        reload(server, false);
                }
            } else
                serverCache.clear();
            return;
        case alwaysOnCluster:
            if (entries != null) {
                for (CacheEntry entry : entries) {
                    Key.AlwaysOnClusterBy clusterBy = Key.AlwaysOnClusterBy.id;
                    AlwaysOnCluster cluster = get(clusterBy, entry.mEntryIdentity);
                    if (cluster != null)
                        reload(cluster, false);
                }
            } else
                alwaysOnClusterCache.clear();
            return;
        case zimlet:
            if (entries != null) {
                for (CacheEntry entry : entries) {
                    Key.ZimletBy zimletBy = (entry.mEntryBy==Key.CacheEntryBy.id)? Key.ZimletBy.id : Key.ZimletBy.name;
                    Zimlet zimlet = getFromCache(zimletBy, entry.mEntryIdentity);
                    if (zimlet != null)
                        reload(zimlet, false);
                }
            } else
                zimletCache.clear();
            return;
        default:
            throw ServiceException.INVALID_REQUEST("invalid cache type "+type, null);
        }

    }

    @Override // LdapProv
    public void removeFromCache(Entry entry) {
        if (entry instanceof Account) {
            accountCache.remove((Account)entry);
        } else if (entry instanceof Group) {
            groupCache.remove((Group)entry);
        } else {
            throw new UnsupportedOperationException();
        }
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

        SearchAccountsOptions option = new SearchAccountsOptions(domain,
                new String[]{Provisioning.A_zimbraCOSId,
                Provisioning.A_zimbraIsSystemResource});
        option.setIncludeType(IncludeType.ACCOUNTS_ONLY);
        option.setFilter(mDIT.filterAccountsOnlyByDomain(domain));
        searchDirectoryInternal(option, visitor);

        return visitor.getResult();
    }

    @Override
    public long countObjects(CountObjectsType type, Domain domain, UCService ucService)
    throws ServiceException {

        if (domain != null && !type.allowsDomain()) {
            throw ServiceException.INVALID_REQUEST(
                    "domain cannot be specified for counting type: " + type.toString(), null);
        }

        if (ucService != null && !type.allowsUCService()) {
            throw ServiceException.INVALID_REQUEST(
                    "UCService cannot be specified for counting type: " + type.toString(), null);
        }


        ZLdapFilter filter;

        // setup types for finding bases
        Set<ObjectType> types = Sets.newHashSet();

        switch (type) {
            case userAccount:
                types.add(ObjectType.accounts);
                filter = filterFactory.allNonSystemAccounts();
                break;
            case internalUserAccount:
                types.add(ObjectType.accounts);
                filter = filterFactory.allNonSystemInternalAccounts();
                break;
            case internalArchivingAccount:
                types.add(ObjectType.accounts);
                filter = filterFactory.allNonSystemArchivingAccounts();
                break;
            case account:
                types.add(ObjectType.accounts);
                types.add(ObjectType.resources);
                filter = filterFactory.allAccounts();
                break;
            case alias:
                types.add(ObjectType.aliases);
                filter = filterFactory.allAliases();
                break;
            case dl:
                types.add(ObjectType.distributionlists);
                types.add(ObjectType.dynamicgroups);
                filter = mDIT.filterGroupsByDomain(domain);
                if (domain != null && !InMemoryLdapServer.isOn()) {
                    ZLdapFilter dnSubtreeMatchFilter = ((LdapDomain) domain).getDnSubtreeMatchFilter();
                    filter = filterFactory.andWith(filter, dnSubtreeMatchFilter);
                }
                break;
            case calresource:
                types.add(ObjectType.resources);
                filter = filterFactory.allCalendarResources();
                break;
            case domain:
                types.add(ObjectType.domains);
                filter = filterFactory.allDomains();
                break;
            case cos:
                types.add(ObjectType.coses);
                filter = filterFactory.allCoses();
                break;
            case server:
                types.add(ObjectType.servers);
                filter = filterFactory.allServers();
                break;
            case accountOnUCService:
                if (ucService == null) {
                    throw ServiceException.INVALID_REQUEST(
                            "UCService is required for counting type: " + type.toString(), null);
                }
                types.add(ObjectType.accounts);
                types.add(ObjectType.resources);
                filter = filterFactory.accountsOnUCService(ucService.getId());
                break;
            case cosOnUCService:
                if (ucService == null) {
                    throw ServiceException.INVALID_REQUEST(
                            "UCService is required for counting type: " + type.toString(), null);
                }
                types.add(ObjectType.coses);
                filter = filterFactory.cosesOnUCService(ucService.getId());
                break;
            case domainOnUCService:
                if (ucService == null) {
                    throw ServiceException.INVALID_REQUEST(
                            "UCService is required for counting type: " + type.toString(), null);
                }
                types.add(ObjectType.domains);
                filter = filterFactory.domainsOnUCService(ucService.getId());
                break;
            default:
                throw ServiceException.INVALID_REQUEST("unsupported counting type:" + type.toString(), null);
        }

        String[] bases = getSearchBases(domain, types);

        long num = 0;
        for (String base : bases) {
            num += countObjects(base, filter);
        }

        return num;
    }

    private long countObjects(String base, ZLdapFilter filter)
    throws ServiceException {
        ZSearchControls searchControls = ZSearchControls.createSearchControls(
                ZSearchScope.SEARCH_SCOPE_SUBTREE, ZSearchControls.SIZE_UNLIMITED, null);
        return helper.countEntries(base, filter, searchControls);
    }

    @Override
    public Map<String, String> getNamesForIds(Set<String> ids, EntryType type)
    throws ServiceException {
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
                entry = accountCache.getById(id);
                if (entry != null)
                    result.put(id, entry.getName());
                else
                    unresolvedIds.add(id);
            }
            nameAttr = Provisioning.A_zimbraMailDeliveryAddress;
            base = mDIT.mailBranchBaseDN();
            objectClass = AttributeClass.OC_zimbraAccount;
            break;
        case group:
            unresolvedIds = ids;
            nameAttr = Provisioning.A_uid; // see dnToEmail
            base = mDIT.mailBranchBaseDN();
            objectClass = AttributeClass.OC_zimbraDistributionList;
            break;
        case cos:
            unresolvedIds = new HashSet<String>();
            for (String id : ids) {
                entry = cosCache.getById(id);
                if (entry != null)
                    result.put(id, entry.getName());
                else
                    unresolvedIds.add(id);
            }
            nameAttr = Provisioning.A_cn;
            base = mDIT.cosBaseDN();
            objectClass = AttributeClass.OC_zimbraCOS;
            break;
        case domain:
            unresolvedIds = new HashSet<String>();
            for (String id : ids) {
                entry = getFromCache(Key.DomainBy.id, id, GetFromDomainCacheOption.POSITIVE);
                if (entry != null)
                    result.put(id, entry.getName());
                else
                    unresolvedIds.add(id);
            }
            nameAttr = Provisioning.A_zimbraDomainName;
            base = mDIT.domainBaseDN();
            objectClass = AttributeClass.OC_zimbraDomain;
            break;
        default:
            throw ServiceException.FAILURE("unsupported entry type for getNamesForIds" + type.name(), null);
        }

        // we are done if all ids can be resolved in our cache
        if (unresolvedIds.size() == 0)
            return result;

        SearchLdapVisitor visitor = new SearchLdapVisitor() {
            @Override
            public void visit(String dn, Map<String, Object> attrs, IAttributes ldapAttrs) {
                String id = (String)attrs.get(Provisioning.A_zimbraId);

                String name = null;
                try {
                    switch (entryType) {
                    case account:
                        name = ldapAttrs.getAttrString(Provisioning.A_zimbraMailDeliveryAddress);
                        if (name == null)
                            name = mDIT.dnToEmail(dn, ldapAttrs);
                        break;
                    case group:
                        name = mDIT.dnToEmail(dn, ldapAttrs);
                        break;
                    case cos:
                        name = ldapAttrs.getAttrString(Provisioning.A_cn);
                        break;
                    case domain:
                        name = ldapAttrs.getAttrString(Provisioning.A_zimbraDomainName);
                        break;
                    }
                } catch (ServiceException e) {
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

    public void searchNamesForIds(Set<String> unresolvedIds, String base,
            String objectClass, String returnAttrs[],
            SearchLdapOptions.SearchLdapVisitor visitor)
    throws ServiceException {

        final int batchSize = 10;  // num ids per search
        final String queryStart = "(&(objectClass=" + objectClass + ")(|";
        final String queryEnd = "))";

        StringBuilder query = new StringBuilder();
        query.append(queryStart);

        int i = 0;
        for (String id : unresolvedIds) {
            query.append("(" + Provisioning.A_zimbraId + "=" + id + ")");
            if ((++i) % batchSize == 0) {
                query.append(queryEnd);
                searchLdapOnReplica(base, query.toString(), returnAttrs, visitor);
                query.setLength(0);
                query.append(queryStart);
            }
        }

        // one more search if there are remainding
        if (query.length() != queryStart.length()) {
            query.append(queryEnd);
            searchLdapOnReplica(base, query.toString(), returnAttrs, visitor);
        }
    }

    @Override
    public Map<String, Map<String, Object>> getDomainSMIMEConfig(Domain domain, String configName)
    throws ServiceException {
        LdapSMIMEConfig smime = LdapSMIMEConfig.getInstance(domain);
        return smime.get(configName);
    }

    @Override
    public void modifyDomainSMIMEConfig(Domain domain, String configName, Map<String, Object> attrs)
    throws ServiceException {
        LdapSMIMEConfig smime = LdapSMIMEConfig.getInstance(domain);
        smime.modify(configName, attrs);
    }

    @Override
    public void removeDomainSMIMEConfig(Domain domain, String configName) throws ServiceException {
        LdapSMIMEConfig smime = LdapSMIMEConfig.getInstance(domain);
        smime.remove(configName);
    }

    @Override
    public Map<String, Map<String, Object>> getConfigSMIMEConfig(String configName)
    throws ServiceException {
        LdapSMIMEConfig smime = LdapSMIMEConfig.getInstance(getConfig());
        return smime.get(configName);
    }

    @Override
    public void modifyConfigSMIMEConfig(String configName, Map<String, Object> attrs)
    throws ServiceException {
        LdapSMIMEConfig smime = LdapSMIMEConfig.getInstance(getConfig());
        smime.modify(configName, attrs);
    }

    @Override
    public void removeConfigSMIMEConfig(String configName) throws ServiceException {
        LdapSMIMEConfig smime = LdapSMIMEConfig.getInstance(getConfig());
        smime.remove(configName);
    }

    @Override
    public void searchLdapOnMaster(String base, String filter,
            String[] returnAttrs, SearchLdapVisitor visitor)
            throws ServiceException {
        searchZimbraLdap(base, filter, returnAttrs, true, visitor);
    }

    @Override
    public void searchLdapOnReplica(String base, String filter,
            String[] returnAttrs, SearchLdapVisitor visitor)
            throws ServiceException {
        searchZimbraLdap(base, filter, returnAttrs, false, visitor);
    }


    @Override
    @TODO  // modify SearchLdapOptions to take a ZLdapFilter
    public void searchLdapOnMaster(String base, ZLdapFilter filter,
            String[] returnAttrs, SearchLdapVisitor visitor)
            throws ServiceException {
        searchLdapOnMaster(base, filter.toFilterString(), returnAttrs, visitor);
    }

    @Override
    @TODO  // modify SearchLdapOptions to take a ZLdapFilter
    public void searchLdapOnReplica(String base, ZLdapFilter filter,
            String[] returnAttrs, SearchLdapVisitor visitor)
            throws ServiceException {
        searchLdapOnReplica(base, filter.toFilterString(), returnAttrs, visitor);
    }

    private void searchZimbraLdap(String base, String query, String[] returnAttrs,
            boolean useMaster, SearchLdapVisitor visitor) throws ServiceException {

        SearchLdapOptions searchOptions = new SearchLdapOptions(base, query,
                returnAttrs, SearchLdapOptions.SIZE_UNLIMITED, null,
                ZSearchScope.SEARCH_SCOPE_SUBTREE, visitor);

        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.get(useMaster), LdapUsage.SEARCH);
            zlc.searchPaged(searchOptions);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    @Override
    public void waitForLdapServer() {
        LdapClient.waitForLdapServer();
    }

    @Override
    public void alwaysUseMaster() {
        LdapClient.masterOnly();
    }

    @Override
    public void dumpLdapSchema(PrintWriter writer) throws ServiceException {
        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.GET_SCHEMA);
            ZLdapSchema schema = zlc.getSchema();

            for (ZLdapSchema.ZObjectClassDefinition oc : schema.getObjectClasses()) {
                writer.println(oc.getName());
            }

            // TODO print more stuff

        } catch (ServiceException e) {
            ZimbraLog.account.warn("unable to get LDAP schema", e);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    /*
     * returns if any one of addrs is an email address on the system
     */
    private boolean addressExists(ZLdapContext zlc, String[] addrs)
    throws ServiceException {
        return addressExistsUnderDN(zlc, LdapConstants.DN_ROOT_DSE, addrs);
    }

    /*
     * returns if any one of addrs is an email address under the specified baseDN
     */
    private boolean addressExistsUnderDN(ZLdapContext zlc, String baseDN, String[] addrs)
    throws ServiceException {
        ZLdapFilter filter = filterFactory.addrsExist(addrs);
        ZSearchControls searchControls = ZSearchControls.createSearchControls(
                ZSearchScope.SEARCH_SCOPE_SUBTREE, 1, null);

        try {
            long count = helper.countEntries(baseDN, filter,
                    searchControls, zlc, LdapServerType.MASTER);
            return count > 0;
        } catch (LdapSizeLimitExceededException e) {
            return true;
        }
    }

    /*
     * ==================
     *   Groups (static/dynamic neutral)
     * ==================
     */
    @Override
    public Group createGroup(String name, Map<String, Object> attrs,
            boolean dynamic) throws ServiceException{
        return dynamic? createDynamicGroup(name, attrs) : createDistributionList(name, attrs);
    }

    @Override
    public Group createDelegatedGroup(String name, Map<String, Object> attrs,
            boolean dynamic, Account creator)
    throws ServiceException {
        if (creator == null) {
            throw ServiceException.INVALID_REQUEST("must have a creator account", null);
        }

        Group group = dynamic?
                createDynamicGroup(name, attrs, creator) :
                createDistributionList(name, attrs, creator);

        grantRight(TargetType.dl.getCode(), TargetBy.id, group.getId(),
                GranteeType.GT_USER.getCode(), GranteeBy.id, creator.getId(), null,
                Group.GroupOwner.GROUP_OWNER_RIGHT.getName(), null);

        return group;
    }

    @Override
    public void deleteGroup(String zimbraId) throws ServiceException {
        Group group = getGroup(Key.DistributionListBy.id, zimbraId, true);
        if (group == null) {
            throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(zimbraId);
        }

        if (group.isDynamic()) {
            deleteDynamicGroup((LdapDynamicGroup) group);
        } else {
            deleteDistributionList((LdapDistributionList) group);
        }
    }

    @Override
    public void renameGroup(String zimbraId, String newName) throws ServiceException {
        Group group = getGroup(Key.DistributionListBy.id, zimbraId, true);
        if (group == null) {
            throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(zimbraId);
        }

        if (group.isDynamic()) {
            renameDynamicGroup(zimbraId, newName);
        } else {
            renameDistributionList(zimbraId, newName);
        }
    }

    @Override
    public Group getGroup(Key.DistributionListBy keyType, String key) throws ServiceException {
        return getGroup(keyType, key, false);
    }

    @Override
    public Group getGroup(Key.DistributionListBy keyType, String key, boolean loadFromMaster)
    throws ServiceException {
        return getGroupInternal(keyType, key, false, loadFromMaster);
    }

    /*
     * like getGroup(DistributionListBy keyType, String key)
     * the difference are:
     *     - cached
     *     - entry returned only contains minimal group attrs
     *     - entry returned does not contain members (the member or zimbraMailForwardingAddress attribute)
     */
    @Override
    public Group getGroupBasic(Key.DistributionListBy keyType, String key)
    throws ServiceException {
        Group group = getGroupFromCache(keyType, key);
        if (group != null) {
            return group;
        }

        // fetch from LDAP
        group = getGroupInternal(keyType, key, true, false);

        // cache it
        if (group != null) {
            putInGroupCache(group);
        }

        return group;
    }

    /**
     * Get all static distribution lists and dynamic groups
     */
    @SuppressWarnings("unchecked")
    @Override
    public List getAllGroups(Domain domain) throws ServiceException {
        SearchDirectoryOptions searchOpts = new SearchDirectoryOptions(domain);
        searchOpts.setFilter(mDIT.filterGroupsByDomain(domain));

        searchOpts.setTypes(ObjectType.distributionlists, ObjectType.dynamicgroups);
        searchOpts.setSortOpt(SortOpt.SORT_ASCENDING);
        List<NamedEntry> groups = (List<NamedEntry>) searchDirectoryInternal(searchOpts);

        return groups;
    }

    @Override
    public void addGroupMembers(Group group, String[] members) throws ServiceException {
        if (group.isDynamic()) {
            addDynamicGroupMembers((LdapDynamicGroup) group, members);
        } else {
            addDistributionListMembers((LdapDistributionList) group, members);
        }
    }

    @Override
    public void removeGroupMembers(Group group, String[] members) throws ServiceException {
        if (group.isDynamic()) {
            removeDynamicGroupMembers((LdapDynamicGroup)group, members, false);
        } else {
            removeDistributionListMembers((LdapDistributionList) group, members);
        }
    }

    @Override
    public void addGroupAlias(Group group, String alias) throws ServiceException {
        addAliasInternal(group, alias);
        allDLs.addGroup(group);
    }

    @Override
    public void removeGroupAlias(Group group, String alias) throws ServiceException {
        groupCache.remove(group);
        removeAliasInternal(group, alias);
        allDLs.removeGroup(alias);
    }

    @Override
    public Set<String> getGroups(Account acct) throws ServiceException {
        GroupMembership groupMembership = getGroupMembership(acct, false);
        return Sets.newHashSet(groupMembership.groupIds());
    }

    /*
     * only called from ProvUtil and GetAccountMembership SOAP handler.
     * can't use getGroupMembership because it needs via.
     *
     * i.e. currently excludes dynamic groups this account is a member of because it satisfies the LDAP filter
     *      specified in the group's MemberURL.
     */
    @Override
    public List<Group> getGroups(Account acct, boolean directOnly, Map<String,String> via)
    throws ServiceException {
        // get static groups
        List<DistributionList> dls = getDistributionLists(acct, directOnly, via);

        // get dynamic groups
        List<DynamicGroup> dynGroups = getContainingDynamicGroups(acct);

        List<Group> groups = Lists.newArrayList();
        groups.addAll(dls);
        groups.addAll(dynGroups);

        return groups;
    }

    @Override
    public boolean inACLGroup(Account acct, String zimbraId) throws ServiceException {
        // This will include expanded membership for non-custom groups and distribution lists
        GroupMembership membership = getGroupMembership(acct, false);
        return (membership.groupIds().contains(zimbraId));
    }

    @Override
    public String[] getGroupMembers(Group group) throws ServiceException {
        EntryCacheDataKey cacheKey = EntryCacheDataKey.GROUP_MEMBERS;

        String[] members = (String[])group.getCachedData(cacheKey);
        if (members != null) {
            return members;
        }

        members = group.getAllMembers();  // should never be null
        assert(members != null);
        Arrays.sort(members);

        // catch it
        group.setCachedData(cacheKey, members);

        return members;
    }

    @Override
    public GroupMemberEmailAddrs getMemberAddrs(Group group) throws ServiceException {
        // this is for mail delivery, always relaod the full group from LDAP
        group = getGroup(DistributionListBy.id, group.getId());

        GroupMemberEmailAddrs addrs = new GroupMemberEmailAddrs();
        if (group.isDynamic()) {
            LdapDynamicGroup ldapDynGroup = (LdapDynamicGroup)group;
            if (ldapDynGroup.isMembershipDefinedByCustomURL()) {
                addrs.setGroupAddr(group.getName());
            } else {
                if (ldapDynGroup.hasExternalMembers()) {
                    // internal members
                    final List<String> internalMembers = Lists.newArrayList();
                    searchDynamicGroupInternalMemberDeliveryAddresses(null, group.getId(), internalMembers);
                    if (!internalMembers.isEmpty()) {
                        addrs.setInternalAddrs(internalMembers);
                    }

                    // external members
                    addrs.setExternalAddrs(ldapDynGroup.getStaticUnit().getMembersSet());
                } else {
                    addrs.setGroupAddr(group.getName());
                }
            }
        } else {
            String[] members = getGroupMembers(group);
            Set<String> internalMembers = Sets.newHashSet();
            Set<String> externalMembers = Sets.newHashSet();
            ZLdapContext zlc = LdapClient.getContext(LdapServerType.REPLICA, LdapUsage.SEARCH);
            try {
                for (String member : members) {
                    if (addressExists(zlc, new String[]{member})) {
                        internalMembers.add(member);
                    } else {
                        externalMembers.add(member);
                    }
                }
            } finally {
                LdapClient.closeContext(zlc);
            }
            if (!externalMembers.isEmpty()) {
                if (!internalMembers.isEmpty()) {
                    addrs.setInternalAddrs(internalMembers);
                }
                addrs.setExternalAddrs(externalMembers);
            } else {
                addrs.setGroupAddr(group.getName());
            }
        }
        return addrs;
    }


    private void cleanGroupMembersCache(Group group) {
        /*
         * Fully loaded DLs(containing members attribute) are not cached
         * (those obtained via Provisioning.getGroup().
         *
         * if the modified instance (the instance being passed in) is not the same
         * instance in cache, clean the group members cache on the cached instance
         */
        Group cachedInstance = getGroupFromCache(DistributionListBy.id, group.getId());
        if (cachedInstance != null && group != cachedInstance) {
            cachedInstance.removeCachedData(EntryCacheDataKey.GROUP_MEMBERS);
        }

        // also always clean it on the modified instance
        group.removeCachedData(EntryCacheDataKey.GROUP_MEMBERS);
    }

    private Group getGroupInternal(Key.DistributionListBy keyType, String key,
            boolean basicAttrsOnly, boolean loadFromMaster)
    throws ServiceException {
        switch(keyType) {
            case id:
                return getGroupById(key, null, basicAttrsOnly, loadFromMaster);
            case name:
                Group group = getGroupByName(key, null, basicAttrsOnly, loadFromMaster);
                if (group == null) {
                    String localDomainAddr = getEmailAddrByDomainAlias(key);
                    if (localDomainAddr != null) {
                        group = getGroupByName(localDomainAddr, null, basicAttrsOnly, loadFromMaster);
                    }
                }
                return group;
            default:
                return null;
        }
    }

    private Group getGroupById(String zimbraId, ZLdapContext zlc,
            boolean basicAttrsOnly, boolean loadFromMaster)
    throws ServiceException {
        return getGroupByQuery(filterFactory.groupById(zimbraId), zlc, basicAttrsOnly, loadFromMaster);
    }

    private Group getGroupByName(String groupAddress, ZLdapContext zlc,
            boolean basicAttrsOnly, boolean loadFromMaster)
    throws ServiceException {
        groupAddress = IDNUtil.toAsciiEmail(groupAddress);
        return getGroupByQuery(filterFactory.groupByName(groupAddress), zlc, basicAttrsOnly, loadFromMaster);
    }

    private Group getGroupByQuery(ZLdapFilter filter, ZLdapContext initZlc,
            boolean basicAttrsOnly, boolean loadFromMaster)
    throws ServiceException {
        try {
            String[] returnAttrs = basicAttrsOnly ? BASIC_GROUP_ATTRS : null;
            ZSearchResultEntry sr = helper.searchForEntry(
                    mDIT.mailBranchBaseDN(), filter, initZlc, loadFromMaster, returnAttrs);
            if (sr != null) {
                ZAttributes attrs = sr.getAttributes();
                List<String> objectclass =
                    attrs.getMultiAttrStringAsList(Provisioning.A_objectClass, CheckBinary.NOCHECK);

                if (objectclass.contains(AttributeClass.OC_zimbraDistributionList)) {
                    return makeDistributionList(sr.getDN(), attrs, basicAttrsOnly);
                } else if (objectclass.contains(AttributeClass.OC_zimbraGroup)) {
                    return makeDynamicGroup(initZlc, sr.getDN(), attrs);
                }
            }
        } catch (LdapMultipleEntriesMatchedException e) {
            throw AccountServiceException.MULTIPLE_ENTRIES_MATCHED("getGroupByQuery", e);
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to lookup group via query: "+
                    filter.toFilterString() + " message:" + e.getMessage(), e);
        }
        return null;
    }

    /*
     * Set a home server for proxying purpose
     * we now do this for all newly created groups instead of only for delegated groups
     * For existing groups that don't have a zimbraMailHost:
     *   - admin soaps: just execute on the local server as before
     *   - user soap: throw exception
     */
    private void setGroupHomeServer(ZMutableEntry entry, Account creator)
    throws ServiceException {
        //
        Cos cosOfCreator = null;
        if (creator != null) {
            cosOfCreator = getCOS(creator);
        }
        addMailHost(entry, cosOfCreator, false);
    }


    /* ==================
     *   Dynamic Groups
     * ==================
     */
    private static final String DYNAMIC_GROUP_DYNAMIC_UNIT_NAME = "internal";
    private static final String DYNAMIC_GROUP_STATIC_UNIT_NAME = "external";

    @Override
    public DynamicGroup createDynamicGroup(String groupAddress,
            Map<String, Object> groupAttrs) throws ServiceException {
        return createDynamicGroup(groupAddress, groupAttrs, null);
    }

    private String dynamicGroupDynamicUnitLocalpart(String dynamicGroupLocalpart) {
        return dynamicGroupLocalpart + ".__internal__";
    }

    private String dynamicGroupStaticUnitLocalpart(String dynamicGroupLocalpart) {
        return dynamicGroupLocalpart + ".__external__";
    }

    private DynamicGroup createDynamicGroup(String groupAddress,
            Map<String, Object> groupAttrs, Account creator)
    throws ServiceException {

        SpecialAttrs specialAttrs = mDIT.handleSpecialAttrs(groupAttrs);
        String baseDn = specialAttrs.getLdapBaseDn();

        groupAddress = groupAddress.toLowerCase().trim();
        EmailAddress addr = new EmailAddress(groupAddress);

        String localPart = addr.getLocalPart();
        String domainName = addr.getDomain();
        domainName = IDNUtil.toAsciiDomainName(domainName);
        groupAddress = EmailAddress.getAddress(localPart, domainName);

        validEmailAddress(groupAddress);

        CallbackContext callbackContext = new CallbackContext(CallbackContext.Op.CREATE);
        callbackContext.setCreatingEntryName(groupAddress);

        // remove zimbraIsACLGroup from attrs if provided, to avoid the immutable check
        Object providedZimbraIsACLGroup = groupAttrs.get(A_zimbraIsACLGroup);
        if (providedZimbraIsACLGroup != null) {
            groupAttrs.remove(A_zimbraIsACLGroup);
        }

        AttributeManager.getInstance().preModify(groupAttrs, null, callbackContext, true);

        // put zimbraIsACLGroup back
        if (providedZimbraIsACLGroup != null) {
            groupAttrs.put(A_zimbraIsACLGroup, providedZimbraIsACLGroup);
        }

        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.CREATE_DYNAMICGROUP);

            Domain domain = getDomainByAsciiName(domainName, zlc);
            if (domain == null) {
                throw AccountServiceException.NO_SUCH_DOMAIN(domainName);
            }

            if (!domain.isLocal()) {
                throw ServiceException.INVALID_REQUEST("domain type must be local", null);
            }

            String domainDN = ((LdapDomain) domain).getDN();

            /*
             * ====================================
             * create the main dynamic group entry
             * ====================================
             */
            ZMutableEntry entry = LdapClient.createMutableEntry();
            entry.mapToAttrs(groupAttrs);

            Set<String> ocs = LdapObjectClass.getGroupObjectClasses(this);
            entry.addAttr(A_objectClass, ocs);

            String zimbraId = LdapUtil.generateUUID();

            // create a UUID for the static unit entry
            String staticUnitZimbraId = LdapUtil.generateUUID();

            String createTimestamp = LdapDateUtil.toGeneralizedTime(new Date());
            entry.setAttr(A_zimbraId, zimbraId);
            entry.setAttr(A_zimbraCreateTimestamp, createTimestamp);
            entry.setAttr(A_mail, groupAddress);
            entry.setAttr(A_dgIdentity, LC.zimbra_ldap_userdn.value());

            // unlike accounts (which have a zimbraMailDeliveryAddress for the primary,
            // and zimbraMailAliases only for aliases), DLs use zimbraMailAlias for both.
            // Postfix uses these two attributes to route mail, and zimbraMailDeliveryAddress
            // indicates that something has a physical mailbox, which DLs don't.
            entry.setAttr(A_zimbraMailAlias, groupAddress);

            /*
            // allow only users in the same domain
            String memberURL = String.format("ldap:///%s??one?(zimbraMemberOf=%s)",
                    mDIT.domainDNToAccountBaseDN(domainDN), groupAddress);
            */

            String specifiedIsACLGroup = entry.getAttrString(A_zimbraIsACLGroup);
            boolean isACLGroup;
            if (!entry.hasAttribute(A_memberURL)) {
                String memberURL = LdapDynamicGroup.getDefaultMemberURL(zimbraId, staticUnitZimbraId);
                entry.setAttr(Provisioning.A_memberURL, memberURL);

                // memberURL is not provided, zimbraIsACLGroup must be either not specified,
                // or specified to be TRUE;
                if (specifiedIsACLGroup == null) {
                    entry.setAttr(A_zimbraIsACLGroup, ProvisioningConstants.TRUE);
                } else if (ProvisioningConstants.FALSE.equals(specifiedIsACLGroup)) {
                    throw ServiceException.INVALID_REQUEST(
                            "No custom " + A_memberURL + " is provided, " +
                            A_zimbraIsACLGroup + " cannot be set to FALSE", null);
                }
                isACLGroup = true;
            } else {
                // We want to be able to use dynamic groups as ACLs, for instance when sharing a folder with a group
                // This used to be disallowed via a requirement that zimbraIsACLGroup be specified and set to FALSE.
                // That requirement has been dropped.
                isACLGroup = !ProvisioningConstants.FALSE.equals(specifiedIsACLGroup);
            }

            // by default a dynamic group is always created enabled
            if (!entry.hasAttribute(Provisioning.A_zimbraMailStatus)) {
                entry.setAttr(A_zimbraMailStatus, MAIL_STATUS_ENABLED);
            }
            String mailStatus = entry.getAttrString(A_zimbraMailStatus);

            entry.setAttr(A_cn, localPart);
            // entry.setAttr(A_uid, localPart); need to use uid if we move dynamic groups to the ou=people tree

            setGroupHomeServer(entry, creator);

            String dn = mDIT.dynamicGroupNameLocalPartToDN(localPart, domainDN);
            entry.setDN(dn);

            zlc.createEntry(entry);

            if (isACLGroup) {
                /*
                 * ===========================================================
                 * create the dynamic group unit entry, for internal addresses
                 * ===========================================================
                 */
                String dynamicUnitLocalpart = dynamicGroupDynamicUnitLocalpart(localPart);
                String dynamicUnitAddr = EmailAddress.getAddress(dynamicUnitLocalpart, domainName);
                entry = LdapClient.createMutableEntry();
                ocs = LdapObjectClass.getGroupDynamicUnitObjectClasses(this);
                entry.addAttr(A_objectClass, ocs);

                String dynamicUnitZimbraId = LdapUtil.generateUUID();
                entry.setAttr(A_cn, DYNAMIC_GROUP_DYNAMIC_UNIT_NAME);
                entry.setAttr(A_zimbraId, dynamicUnitZimbraId);
                entry.setAttr(A_zimbraGroupId, zimbraId);  // id of the main group
                entry.setAttr(A_zimbraCreateTimestamp, createTimestamp);
                entry.setAttr(A_mail, dynamicUnitAddr);
                entry.setAttr(A_zimbraMailAlias, dynamicUnitAddr);
                entry.setAttr(A_zimbraMailStatus, mailStatus);

                entry.setAttr(A_dgIdentity, LC.zimbra_ldap_userdn.value());
                String memberURL = LdapDynamicGroup.getDefaultDynamicUnitMemberURL(zimbraId); // id of the main group
                entry.setAttr(Provisioning.A_memberURL, memberURL);

                String dynamicUnitDN = mDIT.dynamicGroupUnitNameToDN(
                        DYNAMIC_GROUP_DYNAMIC_UNIT_NAME, dn);
                entry.setDN(dynamicUnitDN);

                zlc.createEntry(entry);

                /*
                 * ==========================================================
                 * create the static group unit entry, for external addresses
                 * ==========================================================
                 */
                entry = LdapClient.createMutableEntry();
                ocs = LdapObjectClass.getGroupStaticUnitObjectClasses(this);
                entry.addAttr(A_objectClass, ocs);

                entry.setAttr(A_cn, DYNAMIC_GROUP_STATIC_UNIT_NAME);
                entry.setAttr(A_zimbraId, staticUnitZimbraId);
                entry.setAttr(A_zimbraGroupId, zimbraId);  // id of the main group
                entry.setAttr(A_zimbraCreateTimestamp, createTimestamp);

                String staticUnitDN = mDIT.dynamicGroupUnitNameToDN(
                        DYNAMIC_GROUP_STATIC_UNIT_NAME, dn);
                entry.setDN(staticUnitDN);

                zlc.createEntry(entry);
            }

            /*
             * all is well, get the group by id
             */
            DynamicGroup group = getDynamicGroupBasic(DistributionListBy.id, zimbraId, zlc);

            if (group != null) {
                AttributeManager.getInstance().postModify(groupAttrs, group, callbackContext);
                removeExternalAddrsFromAllDynamicGroups(group.getAllAddrsSet(), zlc);
                allDLs.addGroup(group);
            } else {
                throw ServiceException.FAILURE("unable to get dynamic group after creating LDAP entry: "+
                        groupAddress, null);
            }
            return group;

        } catch (LdapEntryAlreadyExistException nabe) {
            throw AccountServiceException.DISTRIBUTION_LIST_EXISTS(groupAddress);
        } catch (LdapException e) {
            throw e;
        } catch (AccountServiceException e) {
            throw e;
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    private void deleteDynamicGroup(LdapDynamicGroup group) throws ServiceException {
        String zimbraId = group.getId();

        // make a copy of all addrs of this DL, after the delete all aliases on this dl
        // object will be gone, but we need to remove them from the allgroups cache after the DL is deleted
        Set<String> addrs = new HashSet<String>(group.getMultiAttrSet(Provisioning.A_mail));

        /*   ============ handle me ??
        // remove the DL from all DLs
        removeAddressFromAllDistributionLists(dl.getName()); // this doesn't throw any exceptions
        */

        // delete all aliases of the group
        String aliases[] = group.getAliases();
        if (aliases != null) {
            String groupName = group.getName();
            for (int i=0; i < aliases.length; i++) {
                // the primary name shows up in zimbraMailAlias on the entry, don't bother to remove
                // this "alias" if it is the primary name, the entire entry will be deleted anyway.
                if (!groupName.equalsIgnoreCase(aliases[i])) {
                    removeGroupAlias(group, aliases[i]); // this also removes each alias from any DLs
                }
            }
        }

        /*
        // delete all grants granted to the DL
        try {
             RightCommand.revokeAllRights(this, GranteeType.GT_GROUP, zimbraId);
        } catch (ServiceException e) {
            // eat the exception and continue
            ZimbraLog.account.warn("cannot revoke grants", e);
        }

        */

        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.DELETE_DYNAMICGROUP);

            String dn = group.getDN();
            zlc.deleteChildren(dn);
            zlc.deleteEntry(dn);

            // remove zimbraMemberOf if this group from all accounts
            deleteMemberOfOnAccounts(zlc, zimbraId);
            groupCache.remove(group);
            allDLs.removeGroup(addrs);
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to purge group: "+zimbraId, e);
        } finally {
            LdapClient.closeContext(zlc);
        }

        PermissionCache.invalidateCache();
    }

    private void searchDynamicGroupInternalMembers(ZLdapContext zlc, String dynGroupId,
            SearchLdapVisitor visitor)
    throws ServiceException {
        String base = mDIT.mailBranchBaseDN();
        ZLdapFilter filter = filterFactory.accountByMemberOf(dynGroupId);

        SearchLdapOptions searchOptions = new SearchLdapOptions(base, filter,
                new String[]{A_zimbraMailDeliveryAddress, Provisioning.A_zimbraMemberOf},
                SearchLdapOptions.SIZE_UNLIMITED,
                null, ZSearchScope.SEARCH_SCOPE_SUBTREE, visitor);

        zlc.searchPaged(searchOptions);
    }

    // TODO: change to ldif and do in background
    private void deleteMemberOfOnAccounts(ZLdapContext zlc, String dynGroupId)
    throws ServiceException {
        final List<Account> accts = new ArrayList<Account>();
        SearchLdapVisitor visitor = new SearchLdapVisitor(false) {
            @Override
            public void visit(String dn, IAttributes ldapAttrs) throws StopIteratingException {
                Account acct;
                try {
                    acct = makeAccountNoDefaults(dn, (ZAttributes) ldapAttrs);
                    accts.add(acct);
                } catch (ServiceException e) {
                    ZimbraLog.account.warn("unable to make account " + dn, e);
                }
            }
        };
        searchDynamicGroupInternalMembers(zlc, dynGroupId, visitor);

        // go through each DN and remove the zimbraMemberOf={dynGroupId} on the entry
        // do in background?
        for (Account acct : accts) {
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put("-" + Provisioning.A_zimbraMemberOf, dynGroupId);
            modifyLdapAttrs(acct, zlc, attrs);

            // remove the account from cache
            // note: cannnot just removeFromCache(acct) because acct only
            // contains the name, so id/alias/foreignPrincipal cached in NamedCache
            // won't be cleared.
            Account cached = getFromCache(AccountBy.name, acct.getName());
            if (cached != null) {
                removeFromCache(cached);
            }
        }
    }

    private void renameDynamicGroup(String zimbraId, String newEmail) throws ServiceException {
        newEmail = IDNUtil.toAsciiEmail(newEmail);
        validEmailAddress(newEmail);

        boolean domainChanged = false;
        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.RENAME_DYNAMICGROUP);

            LdapDynamicGroup group = (LdapDynamicGroup) getDynamicGroupById(zimbraId, zlc, false);
            if (group == null) {
                throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(zimbraId);
            }

            // prune cache
            groupCache.remove(group);

            String oldEmail = group.getName();
            String oldDomain = EmailUtil.getValidDomainPart(oldEmail);

            newEmail = newEmail.toLowerCase().trim();
            String[] parts = EmailUtil.getLocalPartAndDomain(newEmail);
            if (parts == null) {
                throw ServiceException.INVALID_REQUEST("bad value for newName", null);
            }
            String newLocal = parts[0];
            String newDomain = parts[1];

            domainChanged = !oldDomain.equals(newDomain);

            Domain domain = getDomainByAsciiName(newDomain, zlc);
            if (domain == null) {
                throw AccountServiceException.NO_SUCH_DOMAIN(newDomain);
            }

            if (domainChanged) {
                // make sure the new domain is a local domain
                if (!domain.isLocal()) {
                    throw ServiceException.INVALID_REQUEST("domain type must be local", null);
                }
            }

            Map<String, Object> attrs = new HashMap<String, Object>();

            ReplaceAddressResult replacedMails = replaceMailAddresses(group, Provisioning.A_mail, oldEmail, newEmail);
            if (replacedMails.newAddrs().length == 0) {
                // Set mail to newName if the account currently does not have a mail
                attrs.put(Provisioning.A_mail, newEmail);
            } else {
                attrs.put(Provisioning.A_mail, replacedMails.newAddrs());
            }

            ReplaceAddressResult replacedAliases = replaceMailAddresses(group, Provisioning.A_zimbraMailAlias, oldEmail, newEmail);
            if (replacedAliases.newAddrs().length > 0) {
                attrs.put(Provisioning.A_zimbraMailAlias, replacedAliases.newAddrs());

                String newDomainDN = mDIT.domainToAccountSearchDN(newDomain);

                // check up front if any of renamed aliases already exists in the new domain (if domain also got changed)
                if (domainChanged && addressExistsUnderDN(zlc, newDomainDN, replacedAliases.newAddrs())) {
                    throw AccountServiceException.DISTRIBUTION_LIST_EXISTS(newEmail);
                }
            }

            ReplaceAddressResult replacedAllowAddrForDelegatedSender =
                replaceMailAddresses(group, Provisioning.A_zimbraPrefAllowAddressForDelegatedSender,
                oldEmail, newEmail);
            if (replacedAllowAddrForDelegatedSender.newAddrs().length > 0) {
                attrs.put(Provisioning.A_zimbraPrefAllowAddressForDelegatedSender,
                        replacedAllowAddrForDelegatedSender.newAddrs());
            }

            // the naming rdn
            String rdnAttrName = mDIT.dynamicGroupNamingRdnAttr();
            attrs.put(rdnAttrName, newLocal);

            // move over the distribution list entry
            String oldDn = group.getDN();
            String newDn = mDIT.dynamicGroupDNRename(oldDn, newLocal, domain.getName());
            boolean dnChanged = (!oldDn.equals(newDn));

            if (dnChanged) {
                // cn will be changed during renameEntry, so no need to modify it
                // OpenLDAP is OK modifying it, as long as it matches the new DN, but
                // InMemoryDirectoryServer does not like it.
                attrs.remove(A_cn);

                zlc.renameEntry(oldDn, newDn);
            }

            // re-get the entry after move
            group = (LdapDynamicGroup) getDynamicGroupById(zimbraId, zlc, false);

            // rename the distribution list and all it's renamed aliases to the new name in all distribution lists
            // doesn't throw exceptions, just logs
            /* should we do this for dyanmic groups?
            renameAddressesInAllDistributionLists(oldEmail, newEmail, replacedAliases); // doesn't throw exceptions, just logs
            */

            // MOVE OVER ALL aliases
            // doesn't throw exceptions, just logs
            if (domainChanged) {
                String newUid = group.getAttr(rdnAttrName);
                moveAliases(zlc, replacedAliases, newDomain, newUid, oldDn, newDn, oldDomain, newDomain);
            }

            // this is non-atomic. i.e., rename could succeed and updating A_mail
            // could fail. So catch service exception here and log error
            try {
                // modify attrs on the mail entry
                modifyAttrsInternal(group, zlc, attrs);
                if (group.isIsACLGroup()) {
                    // modify attrs on the units (which are only present when group is an ACL Group)
                    String dynamicUnitNewLocal = dynamicGroupDynamicUnitLocalpart(newLocal);
                    String dynamicUnitNewEmail = dynamicUnitNewLocal + "@" + newDomain;
                    String dynamicUnitDN = mDIT.dynamicGroupUnitNameToDN(DYNAMIC_GROUP_DYNAMIC_UNIT_NAME, newDn);

                    ZMutableEntry entry = LdapClient.createMutableEntry();
                    entry.setAttr(A_mail, dynamicUnitNewEmail);
                    entry.setAttr(A_zimbraMailAlias, dynamicUnitNewEmail);
                    zlc.replaceAttributes(dynamicUnitDN, entry.getAttributes());
                }

            } catch (ServiceException e) {
                ZimbraLog.account.error("dynamic group renamed to " + newLocal +
                        " but failed to move old name's LDAP attributes", e);
                throw e;
            }

            removeExternalAddrsFromAllDynamicGroups(group.getAllAddrsSet(), zlc);

        } catch (LdapEntryAlreadyExistException nabe) {
            throw AccountServiceException.DISTRIBUTION_LIST_EXISTS(newEmail);
        } catch (LdapException e) {
            throw e;
        } catch (AccountServiceException e) {
            throw e;
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to rename dynamic group: " + zimbraId, e);
        } finally {
            LdapClient.closeContext(zlc);
        }

        if (domainChanged) {
            PermissionCache.invalidateCache();
        }
    }

    private DynamicGroup getDynamicGroupFromCache(Key.DistributionListBy keyType, String key) {
        Group group =  getGroupFromCache(keyType, key);
        if (group instanceof DynamicGroup) {
            return (DynamicGroup) group;
        } else {
            return null;
        }
    }

    private DynamicGroup getDynamicGroupBasic(Key.DistributionListBy keyType, String key,
            ZLdapContext zlc) throws ServiceException {
        DynamicGroup dynGroup = getDynamicGroupFromCache(keyType, key);
        if (dynGroup != null) {
            return dynGroup;
        }

        switch(keyType) {
        case id:
            dynGroup = getDynamicGroupById(key, zlc, true);
            break;
        case name:
            dynGroup = getDynamicGroupByName(key, zlc, true);
            break;
        }

        if (dynGroup != null) {
            putInGroupCache(dynGroup);
        }
        return dynGroup;
    }

    private DynamicGroup getDynamicGroupById(String zimbraId, ZLdapContext zlc, boolean basicAttrsOnly)
    throws ServiceException {
        return getDynamicGroupByQuery(filterFactory.dynamicGroupById(zimbraId), zlc, basicAttrsOnly);
    }

    private DynamicGroup getDynamicGroupByName(String groupAddress, ZLdapContext zlc,
            boolean basicAttrsOnly)
    throws ServiceException {
        groupAddress = IDNUtil.toAsciiEmail(groupAddress);
        return getDynamicGroupByQuery(filterFactory.dynamicGroupByName(groupAddress), zlc, basicAttrsOnly);
    }

    private DynamicGroup getDynamicGroupByQuery(ZLdapFilter filter, ZLdapContext initZlc, boolean basicAttrsOnly)
    throws ServiceException {
        try {
            String[] returnAttrs = basicAttrsOnly ? BASIC_DYNAMIC_GROUP_ATTRS : null;
            ZSearchResultEntry sr = helper.searchForEntry(mDIT.mailBranchBaseDN(), filter, initZlc, false, returnAttrs);
            if (sr != null) {
                return makeDynamicGroup(initZlc, sr.getDN(), sr.getAttributes());
            }
        } catch (LdapMultipleEntriesMatchedException e) {
            throw AccountServiceException.MULTIPLE_ENTRIES_MATCHED("getDynamicGroupByQuery", e);
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to lookup group via query: "+
                    filter.toFilterString() + " message:" + e.getMessage(), e);
        }
        return null;
    }

    private void addDynamicGroupMembers(LdapDynamicGroup group, String[] members)
    throws ServiceException {
        if (group.isMembershipDefinedByCustomURL()) {
            throw ServiceException.INVALID_REQUEST(
                    "cannot add members to dynamic group with custom memberURL", null);
        }

        String groupId = group.getId();

        List<Account> accts = new ArrayList<Account>();
        List<String> externalAddrs = new ArrayList<String>();

        // check for errors, and put valid accts to the queue
        for (String member : members) {
            String memberName = member.toLowerCase();
            memberName = IDNUtil.toAsciiEmail(memberName);

            Account acct = get(AccountBy.name, memberName);
            if (acct == null) {
                // addr is not an account (could still be a group or group unit address
                // on the system), will check by addressExists.
                externalAddrs.add(memberName);
            } else {
                // is an account
                Set<String> memberOf = acct.getMultiAttrSet(Provisioning.A_zimbraMemberOf);
                if (!memberOf.contains(groupId)) {
                    accts.add(acct);
                }
                // else the addr is already in the group, just skip it, do not throw
            }
        }

        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.ADD_GROUP_MEMBER);

            // check non of the addrs in externalAddrs can be an email address
            // on the system
            if (!externalAddrs.isEmpty()) {
                if (addressExists(zlc, externalAddrs.toArray(new String[externalAddrs.size()]))) {
                    throw ServiceException.INVALID_REQUEST("address cannot be a group: " +
                            Arrays.deepToString(externalAddrs.toArray()), null);
                }
            }

            /*
             * add internal members
             */
            for (Account acct : accts) {
                Map<String, Object> attrs = new HashMap<String, Object>();
                attrs.put("+" + Provisioning.A_zimbraMemberOf, groupId);
                modifyLdapAttrs(acct, zlc, attrs);
                clearUpwardMembershipCache(acct);
            }

            /*
             * add external members on the static unit
             */
            LdapDynamicGroup.StaticUnit staticUnit = group.getStaticUnit();
            Set<String> existingAddrs = staticUnit.getMembersSet();
            List<String> addrsToAdd = Lists.newArrayList();
            for (String addr : externalAddrs) {
                if (!existingAddrs.contains(addr)) {
                    addrsToAdd.add(addr);
                }
            }

            if (!addrsToAdd.isEmpty()) {
                Map<String,String[]> attrs = new HashMap<String,String[]>();
                attrs.put("+" + LdapDynamicGroup.StaticUnit.MEMBER_ATTR,
                        addrsToAdd.toArray(new String[addrsToAdd.size()]));
                modifyLdapAttrs(staticUnit, zlc, attrs);
            }

        } finally {
            LdapClient.closeContext(zlc);
        }
        PermissionCache.invalidateCache();
        cleanGroupMembersCache(group);
    }

    private void removeDynamicGroupExternalMembers(LdapDynamicGroup group, String[] members)
    throws ServiceException {
        removeDynamicGroupMembers(group, members, true);
    }

    private void removeDynamicGroupMembers(LdapDynamicGroup group, String[] members, boolean externalOnly)
    throws ServiceException {
        if (group.isMembershipDefinedByCustomURL()) {
            throw ServiceException.INVALID_REQUEST(
                    String.format("cannot remove members from dynamic group '%s' with custom memberURL",
                            group.getName()), null);
        }

        String groupId = group.getId();

        List<Account> accts = new ArrayList<Account>();
        List<String> externalAddrs = new ArrayList<String>();
        HashSet<String> failed = new HashSet<String>();

        // check for errors, and put valid accts to the queue
        for (String member : members) {
            String memberName = member.toLowerCase();

            boolean isBadAddr = false;
            try {
                memberName = IDNUtil.toAsciiEmail(memberName);
            } catch (ServiceException e) {
                // if the addr is not a valid email address, maybe they want to
                // remove a bogus addr that somehow got in, just let it through.
                memberName = member;
                isBadAddr = true;
            }

            // always add all addrs to "externalAddrs".
            externalAddrs.add(memberName);

            if (!externalOnly) {
                Account acct = isBadAddr ? null : get(AccountBy.name, member);
                if (acct != null) {
                    Set<String> memberOf = acct.getMultiAttrSet(Provisioning.A_zimbraMemberOf);
                    if (memberOf.contains(groupId)) {
                        accts.add(acct);
                    } else {
                        // else the addr is not in the group, throw exception
                        failed.add(memberName);
                    }
                }
            }
        }

        if (!failed.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            Iterator<String> iter = failed.iterator();
            while (true) {
                sb.append(iter.next());
                if (!iter.hasNext())
                    break;
                sb.append(",");
            }
            throw AccountServiceException.NO_SUCH_MEMBER(group.getName(), sb.toString());
        }

        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.REMOVE_GROUP_MEMBER);

            /*
             * remove internal members
             */
            for (Account acct : accts) {
                Map<String, Object> attrs = new HashMap<String, Object>();
                attrs.put("-" + Provisioning.A_zimbraMemberOf, groupId);
                modifyLdapAttrs(acct, zlc, attrs);
                clearUpwardMembershipCache(acct);
            }

            /*
             * remove external members on the static unit
             */
            LdapDynamicGroup.StaticUnit staticUnit = group.getStaticUnit();
            Set<String> existingAddrs = staticUnit.getMembersSet();
            List<String> addrsToRemove = Lists.newArrayList();
            for (String addr : externalAddrs) {
                if (existingAddrs.contains(addr)) {
                    addrsToRemove.add(addr);
                }
            }

            if (!addrsToRemove.isEmpty()) {
                Map<String,String[]> attrs = new HashMap<String,String[]>();
                attrs.put("-" + LdapDynamicGroup.StaticUnit.MEMBER_ATTR,
                        addrsToRemove.toArray(new String[addrsToRemove.size()]));
                modifyLdapAttrs(staticUnit, zlc, attrs);
            }

        } finally {
            LdapClient.closeContext(zlc);
        }
        PermissionCache.invalidateCache();
        cleanGroupMembersCache(group);
    }

    /**
     * Get Dynamic Groups this account is a member of due to the value of the "zimbraMemberOf" attribute.
     * i.e. excludes groups this account is a member of because it satisfies the LDAP filter specified in the
     *      group's MemberURL where that MemberURL is a custom one.
     * @param acct
     * @return
     * @throws ServiceException
     */
    private List<DynamicGroup> getContainingDynamicGroups(Account acct) throws ServiceException {
        List<DynamicGroup> groups = new ArrayList<DynamicGroup>();

        Set<String> memberOf = getAllContainingDynamicGroupIDs(acct);
        for (String groupId : memberOf) {
            DynamicGroup dynGroup = getDynamicGroupBasic(Key.DistributionListBy.id, groupId, null);
            if (dynGroup != null && !dynGroup.isMembershipDefinedByCustomURL()) {
                groups.add(dynGroup);
            }
        }

        return groups;
    }

    /**
     * @return the IDs of all dynamic groups (including those defined by Custom URL)
     */
    private Set<String> getAllContainingDynamicGroupIDs(Account acct) throws ServiceException {
        Set<String> memberOf;
        if (acct instanceof GuestAccount) {
            memberOf = searchContainingDynamicGroupIdsForExternalAddress(acct.getName(), null);
        } else if (acct.isIsExternalVirtualAccount()) {
            memberOf = searchContainingDynamicGroupIdsForExternalAddress(acct.getExternalUserMailAddress(), null);
        } else {
            memberOf = acct.getMultiAttrSet(Provisioning.A_zimbraMemberOf);
        }
        return memberOf;
    }

    /*
     * returns zimbraId of dynamic groups containing addr as an external member.
     */
    private Set<String> searchContainingDynamicGroupIdsForExternalAddress(
            String addr, ZLdapContext initZlc) {
        final Set<String> groupIds = Sets.newHashSet();

        SearchLdapVisitor visitor = new SearchLdapVisitor(false) {
            @Override
            public void visit(String dn, IAttributes ldapAttrs)
            throws StopIteratingException {
                String groupId = null;
                try {
                    groupId = ldapAttrs.getAttrString(A_zimbraGroupId);
                } catch (ServiceException e) {
                    ZimbraLog.account.warn("unable to get attr", e);
                }
                if (groupId != null) {
                    groupIds.add(groupId);
                }
            }
        };

        ZLdapContext zlc = initZlc;
        try {
            if (zlc == null) {
                zlc = LdapClient.getContext(LdapServerType.REPLICA, LdapUsage.SEARCH);
            }
            String base = mDIT.mailBranchBaseDN();
            ZLdapFilter filter =
                    filterFactory.dynamicGroupsStaticUnitByMemberAddr(addr);

            SearchLdapOptions searchOptions = new SearchLdapOptions(base, filter,
                    new String[]{A_zimbraGroupId},
                    SearchLdapOptions.SIZE_UNLIMITED,
                    null, ZSearchScope.SEARCH_SCOPE_SUBTREE, visitor);

            zlc.searchPaged(searchOptions);

        } catch (ServiceException e) {
            ZimbraLog.account.warn("unable to search dynamic groups for guest acct", e);
        } finally {
            if (initZlc == null) {
                LdapClient.closeContext(zlc);
            }
        }

        return groupIds;
    }

    /*
     * remove addrs from all dynamic groups (i.e. static units of dynamic groups)
     *
     * Called whenever a new email address is created on the system
     * (create/rename/add-alias of accounts and static/dynamic groups)
     */
    private void removeExternalAddrsFromAllDynamicGroups(Set<String> addrs, ZLdapContext zlc)
    throws ServiceException {
        for (String addr : addrs) {
            Set<String> memberOf = searchContainingDynamicGroupIdsForExternalAddress(addr, zlc);

            for (String groupId : memberOf) {
                DynamicGroup group = getDynamicGroupBasic(Key.DistributionListBy.id, groupId, zlc);
                if (group != null) {  // sanity check, should not be null
                    removeDynamicGroupExternalMembers((LdapDynamicGroup)group, new String[]{addr});
                }
            }
        }
    }

    /*
     * returns all internal and external member addresses of the DynamicGroup
     */
    private List<String> searchDynamicGroupMembers(DynamicGroup group) throws ServiceException {
        if (group.isMembershipDefinedByCustomURL()) {
            throw ServiceException.INVALID_REQUEST(
                    "cannot search members to dynamic group with custom memberURL", null);
        }

        final List<String> members = Lists.newArrayList();

        ZLdapContext zlc = null;
        try {
            // always use master to search for dynamic group members
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.SEARCH);

            // search internal members
            searchDynamicGroupInternalMemberDeliveryAddresses(zlc, group.getId(), members);

            // add external members
            LdapDynamicGroup.StaticUnit staticUnit = ((LdapDynamicGroup)group).getStaticUnit();
            // need to refresh, the StaticUnit instance updated by add/remove
            // dynamic group members may be the cached instance.
            refreshEntry(staticUnit, zlc);
            for (String extAddr : staticUnit.getMembers()) {
                members.add(extAddr);
            }
        } catch (ServiceException e) {
            ZimbraLog.account.warn("unable to search dynamic group members", e);
        } finally {
            LdapClient.closeContext(zlc);
        }


        return members;
    }

    private void searchDynamicGroupInternalMemberDeliveryAddresses(
            ZLdapContext initZlc, String dynGroupId, final Collection<String> result) {

        SearchLdapVisitor visitor = new SearchLdapVisitor(false) {
            @Override
            public void visit(String dn, IAttributes ldapAttrs) throws StopIteratingException {
                String addr = null;
                try {
                    addr = ldapAttrs.getAttrString(Provisioning.A_zimbraMailDeliveryAddress);
                } catch (ServiceException e) {
                    ZimbraLog.account.warn("unable to get attr", e);
                }
                if (addr != null) {
                    result.add(addr);
                }
            }
        };

        ZLdapContext zlc = initZlc;
        try {
            if (zlc == null) {
                // always use master to search for dynamic group members
                zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.SEARCH);
            }
            searchDynamicGroupInternalMembers(zlc, dynGroupId, visitor);
        } catch (ServiceException e) {
            ZimbraLog.account.warn("unable to search dynamic group members", e);
        } finally {
            if (initZlc == null) {
                LdapClient.closeContext(zlc);
            }
        }
    }

    public String[] getNonDefaultDynamicGroupMembers(DynamicGroup group) {
        final List<String> members = Lists.newArrayList();

        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.REPLICA, LdapUsage.GET_GROUP_MEMBER);

            /*
             * this DynamicGroup object must not be a basic group with minimum
             * attrs, we need the member attribute
             */
            String[] memberDNs = group.getMultiAttr(Provisioning.A_member);

            final String[] attrsToGet = new String[] { Provisioning.A_zimbraMailDeliveryAddress,
                Provisioning.A_zimbraIsExternalVirtualAccount };
            for (String memberDN : memberDNs) {
                ZAttributes memberAttrs = zlc.getAttributes(memberDN, attrsToGet);
                String memberAddr = memberAttrs
                    .getAttrString(Provisioning.A_zimbraMailDeliveryAddress);
                boolean isVirtualAcct = memberAttrs.hasAttributeValue(
                    Provisioning.A_zimbraIsExternalVirtualAccount, "TRUE");
                if (memberAddr != null && !isVirtualAcct) {
                    members.add(memberAddr);
                }
            }

        } catch (ServiceException e) {
            ZimbraLog.account.warn("unable to get dynamic group members", e);
        } finally {
            LdapClient.closeContext(zlc);
        }

        return members.toArray(new String[members.size()]);
    }

    public String[] getDynamicGroupMembers(DynamicGroup group) throws ServiceException {
        List<String> members = searchDynamicGroupMembers(group);
        return members.toArray(new String[members.size()]);
    }

    public Set<String> getDynamicGroupMembersSet(DynamicGroup group) throws ServiceException {
        List<String> members = searchDynamicGroupMembers(group);
        return Sets.newHashSet(members);
    }


    /*
     *
     * UC service methods
     *
     */
    private UCService getUCServiceByQuery(ZLdapFilter filter, ZLdapContext initZlc)
    throws ServiceException {
        try {
            ZSearchResultEntry sr = helper.searchForEntry(mDIT.ucServiceBaseDN(), filter, initZlc, false);
            if (sr != null) {
                return new LdapUCService(sr.getDN(), sr.getAttributes(), this);
            }
        } catch (LdapMultipleEntriesMatchedException e) {
            throw AccountServiceException.MULTIPLE_ENTRIES_MATCHED("getUCServiceByQuery", e);
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to lookup ucservice via query: "+
                    filter.toFilterString() + " message:" + e.getMessage(), e);
        }
        return null;
    }

    private UCService getUCServiceById(String zimbraId, ZLdapContext zlc, boolean nocache)
    throws ServiceException {
        if (zimbraId == null) {
            return null;
        }
        UCService s = null;
        if (!nocache) {
            s = ucServiceCache.getById(zimbraId);
        }
        if (s == null) {
            s = getUCServiceByQuery(filterFactory.ucServiceById(zimbraId), zlc);
            ucServiceCache.put(s);
        }
        return s;
    }

    private UCService getUCServiceByName(String name, boolean nocache) throws ServiceException {
        if (!nocache) {
            UCService s = ucServiceCache.getByName(name);
            if (s != null) {
                return s;
            }
        }

        try {
            String dn = mDIT.ucServiceNameToDN(name);
            ZAttributes attrs = helper.getAttributes(LdapUsage.GET_UCSERVICE, dn);
            LdapUCService s = new LdapUCService(dn, attrs, this);
            ucServiceCache.put(s);
            return s;
        } catch (LdapEntryNotFoundException e) {
            return null;
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to lookup ucservice by name: "+name+" message: "+e.getMessage(), e);
        }
    }

    @Override
    public UCService createUCService(String name, Map<String, Object> attrs)
            throws ServiceException {
        name = name.toLowerCase().trim();

        CallbackContext callbackContext = new CallbackContext(CallbackContext.Op.CREATE);
        AttributeManager.getInstance().preModify(attrs, null, callbackContext, true);

        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.CREATE_UCSERVICE);

            ZMutableEntry entry = LdapClient.createMutableEntry();
            entry.mapToAttrs(attrs);

            Set<String> ocs = LdapObjectClass.getUCServiceObjectClasses(this);
            entry.addAttr(A_objectClass, ocs);

            String zimbraIdStr = LdapUtil.generateUUID();
            entry.setAttr(A_zimbraId, zimbraIdStr);
            entry.setAttr(A_zimbraCreateTimestamp, LdapDateUtil.toGeneralizedTime(new Date()));
            entry.setAttr(A_cn, name);
            String dn = mDIT.ucServiceNameToDN(name);

            entry.setDN(dn);
            zlc.createEntry(entry);

            UCService ucService = getUCServiceById(zimbraIdStr, zlc, true);
            AttributeManager.getInstance().postModify(attrs, ucService, callbackContext);
            return ucService;

        } catch (LdapEntryAlreadyExistException nabe) {
            throw AccountServiceException.SERVER_EXISTS(name);
        } catch (LdapException e) {
            throw e;
        } catch (AccountServiceException e) {
            throw e;
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to create ucservice: " + name, e);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    @Override
    public void deleteUCService(String zimbraId) throws ServiceException {
        LdapUCService ucService = (LdapUCService) getUCServiceById(zimbraId, null, false);
        if (ucService == null) {
            throw AccountServiceException.NO_SUCH_UC_SERVICE(zimbraId);
        }

        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.DELETE_UCSERVICE);
            zlc.deleteEntry(ucService.getDN());
            ucServiceCache.remove(ucService);
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to purge ucservice: "+zimbraId, e);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    @Override
    public UCService get(UCServiceBy keyType, String key) throws ServiceException {
        switch(keyType) {
            case name:
                return getUCServiceByName(key, false);
            case id:
                return getUCServiceById(key, null, false);
            default:
                return null;
        }
    }

    @Override
    public List<UCService> getAllUCServices() throws ServiceException {
        List<UCService> result = new ArrayList<UCService>();

        ZLdapFilter filter = filterFactory.allUCServices();

        try {
            ZSearchResultEnumeration ne = helper.searchDir(mDIT.ucServiceBaseDN(),
                    filter, ZSearchControls.SEARCH_CTLS_SUBTREE());
            while (ne.hasMore()) {
                ZSearchResultEntry sr = ne.next();
                LdapUCService s = new LdapUCService(sr.getDN(), sr.getAttributes(), this);
                result.add(s);
            }
            ne.close();
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to list all servers", e);
        }

        if (result.size() > 0) {
            ucServiceCache.put(result, true);
        }
        Collections.sort(result);
        return result;
    }

    @Override
    public void renameUCService(String zimbraId, String newName) throws ServiceException {
        LdapUCService ucService = (LdapUCService) getUCServiceById(zimbraId, null, false);
        if (ucService == null) {
            throw AccountServiceException.NO_SUCH_UC_SERVICE(zimbraId);
        }

        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.RENAME_UCSERVICE);
            String newDn = mDIT.ucServiceNameToDN(newName);
            zlc.renameEntry(ucService.getDN(), newDn);
            ucServiceCache.remove(ucService);
        } catch (LdapEntryAlreadyExistException nabe) {
            throw AccountServiceException.UC_SERVICE_EXISTS(newName);
        } catch (LdapException e) {
            throw e;
        } catch (AccountServiceException e) {
            throw e;
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("unable to rename ucservice: "+zimbraId, e);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    @Override
    public boolean isCacheEnabled() {
        return useCache;
    }

    @Override
    public void refreshUserCredentials(Account account) throws ServiceException {
        ZLdapContext zlc = null;

        try {
            zlc = LdapClient.getContext(LdapServerType.REPLICA, LdapUsage.GET_ENTRY);
            String[] returnAttrs = {"userPassword", "zimbraAuthTokens", "zimbraAuthTokenValidityValue"};
            ZAttributes attrs = helper.getAttributes(zlc, ((LdapEntry) account).getDN(), returnAttrs);
            Map<String, Object> finalAttrs = account.getAttrs(false, false);
            finalAttrs.putAll(attrs.getAttrs());
            account.setAttrs(finalAttrs);
            extendLifeInCacheOrFlush(account);  // Put this back into the cache
        } catch (ServiceException e) {
            throw ServiceException.FAILURE(String.format("unable to refresh userPassword for '%s'", account.getName()), e);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }
}
