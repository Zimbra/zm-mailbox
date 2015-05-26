/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.ldap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AlwaysOnCluster;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ShareLocator;
import com.zimbra.cs.account.UCService;
import com.zimbra.cs.account.XMPPComponent;
import com.zimbra.cs.account.cache.AccountCache;
import com.zimbra.cs.account.cache.DomainCache;
import com.zimbra.cs.account.cache.DomainCache.GetFromDomainCacheOption;
import com.zimbra.cs.account.cache.IAccountCache;
import com.zimbra.cs.account.cache.IDomainCache;
import com.zimbra.cs.account.cache.IMimeTypeCache;
import com.zimbra.cs.account.cache.INamedEntryCache;
import com.zimbra.cs.account.cache.NamedEntryCache;
import com.zimbra.cs.account.ldap.entry.LdapCos;
import com.zimbra.cs.account.ldap.entry.LdapDomain;
import com.zimbra.cs.account.ldap.entry.LdapEntry;
import com.zimbra.cs.account.ldap.entry.LdapZimlet;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.unboundid.UBIDAttributes;
import com.zimbra.cs.mime.MimeTypeInfo;

/**
 * @author pshao
 */
abstract public class LdapCache {
    abstract IAccountCache accountCache();
    abstract INamedEntryCache<LdapCos> cosCache();
    abstract INamedEntryCache<ShareLocator> shareLocatorCache();
    abstract IDomainCache domainCache();
    abstract IMimeTypeCache mimeTypeCache();
    abstract INamedEntryCache<Server> serverCache();
    abstract INamedEntryCache<UCService> ucServiceCache();
    abstract INamedEntryCache<LdapZimlet> zimletCache();
    abstract INamedEntryCache<Group> groupCache();
    abstract INamedEntryCache<XMPPComponent> xmppComponentCache();
    abstract INamedEntryCache<AlwaysOnCluster> alwaysOnClusterCache();
    abstract boolean isServerCacheSetup();

    private static Server localServer = null;
    private static long ldapCacheFreshnessCheckLimitMs = -1;
    private static boolean gotldapCacheFreshnessCheckLimitMs = false;

    public static long ldapCacheFreshnessCheckLimitMs() {
        if (!gotldapCacheFreshnessCheckLimitMs) {
            if (localServer != null) {
                ldapCacheFreshnessCheckLimitMs = localServer.getLdapCacheFreshnessCheckLimit();
                gotldapCacheFreshnessCheckLimitMs = true;
            } else {
                ldapCacheFreshnessCheckLimitMs = 100;
            }
        }
        return ldapCacheFreshnessCheckLimitMs;
    }

    /**
     * Diagnostic code to find any places where LDAP entries are not being created in the best way.
     */
    public static void validateCacheEntry(NamedEntry entry) {
        if (!ZimbraLog.ldap.isDebugEnabled() || (null == entry)) {
            return;
        }
        if (entry instanceof LdapEntry) {
            if (null == ((LdapEntry)entry).getEntryCSN()) {
                ZimbraLog.ldap.debug("LDAP cache put entry has no 'entryCSN'\n%s", ZimbraLog.getStackTrace(12));
            }
        }
    }

    public static boolean isEntryStale(String dn, String oldEntryCSN, LdapHelper helper, ZLdapContext zlc) {
        if (oldEntryCSN == null) {
            ZimbraLog.ldap.debug("isEntryStale EntyCSN not set for %s - assume stale", dn);
            return true;
        }
        try {
            return ! helper.compare(dn, UBIDAttributes.ENTRY_CSN, oldEntryCSN, zlc, false);
        } catch (ServiceException e) {
            ZimbraLog.ldap.debug("isEntryStale LDAP compare on entryCSN for %s failed - assume stale", dn, e);
            return true;
        }
    }

    public static boolean isEntryStale(NamedEntry entry, LdapHelper helper, ZLdapContext zlc) {
        if (entry instanceof LdapEntry) {
            LdapEntry ldapEntry = (LdapEntry) entry;
            return isEntryStale(ldapEntry.getDN(), ldapEntry.getEntryCSN(), helper, zlc);
        }
        ZimbraLog.ldap.debug("isEntryStale used with non LdapEntry class=%s", entry.getClass().getName());
        return false;
    }


    /**
     *
     * LRUMapCache
     *
     */
    static class LRUMapCache extends LdapCache {

        private IAccountCache accountCache = null;
        private INamedEntryCache<LdapCos> cosCache = null;
        private INamedEntryCache<ShareLocator> shareLocatorCache = null;
        private IDomainCache domainCache = null;
        private IMimeTypeCache mimeTypeCache = null;
        private INamedEntryCache<Server> serverCache = null;
        private INamedEntryCache<UCService> ucServiceCache = null;
        private INamedEntryCache<LdapZimlet> zimletCache = null;
        private INamedEntryCache<Group> groupCache = null;
        private INamedEntryCache<XMPPComponent> xmppComponentCache = null;
        private INamedEntryCache<AlwaysOnCluster> alwaysOnClusterCache = null;
        static class AccountFreshnessChecker implements IAccountCache.FreshnessChecker {
            private final LdapHelper helper;

            AccountFreshnessChecker(LdapHelper helper) {
                this.helper = helper;
            }

            @Override
            public boolean isStale(Account acct) {
                ZLdapContext zlc = null;
                try {
                    zlc = LdapClient.getContext(LdapServerType.REPLICA, LdapUsage.COMPARE);
                    if (isEntryStale(acct, helper, zlc)) {
                        return true;
                    }
                    String entryCSNforCos = acct.getEntryCSNforCos();
                    String cosDN = acct.getDNforCos();
                    if (entryCSNforCos != null && cosDN != null && isEntryStale(cosDN, entryCSNforCos, helper, zlc)) {
                        return true;
                    } else if (null != acct.getCOS()) {
                        return true;
                    }
                    String entryCSNforDomain = acct.getEntryCSNforDomain();
                    String domainDN = acct.getDNforDomain();
                    if (entryCSNforDomain != null && domainDN != null &&
                            isEntryStale(domainDN, entryCSNforDomain, helper, zlc)) {
                        return true;
                    } else if (null != acct.getProvisioning().getDomain(acct)) {
                        return true;
                    }
                    return false;
                } catch (ServiceException se) {
                    ZimbraLog.ldap.debug("AccountFreshnessChecker unable to check related objects %s - assume stale",
                            acct.getName(), se);
                    return true;
                } finally {
                    LdapClient.closeContext(zlc);
                }
            }
        }

        static class ServerFreshnessChecker implements INamedEntryCache.FreshnessChecker<NamedEntry> {
            private final LdapHelper helper;

            ServerFreshnessChecker(LdapHelper helper) {
                this.helper = helper;
            }

            @Override
            public boolean isStale(NamedEntry ne) {
                if (!(ne instanceof Server)) {
                    return false;
                }
                Server server = (Server) ne;
                ZLdapContext zlc = null;
                try {
                    zlc = LdapClient.getContext(LdapServerType.REPLICA, LdapUsage.COMPARE);
                    if (isEntryStale(server, helper, zlc)) {
                        return true;
                    }
                    String entryCSNforConfig = server.getConfigEntryCSN();
                    if (null != entryCSNforConfig) {
                        String configDN  = helper.getProv().getDIT().configDN();
                        if (null != configDN) {
                            if (isEntryStale(configDN, entryCSNforConfig, helper, zlc)) {
                                return true;
                            }
                        }
                    }
                    String entryCSNforAlwaysOnCluster = server.getAlwaysOnClusterEntryCSN();
                    String dnForAlwaysOnCluster = server.getAlwaysOnClusterDN();
                    if ((null != entryCSNforAlwaysOnCluster) && (null != dnForAlwaysOnCluster)) {
                        if (isEntryStale(dnForAlwaysOnCluster, entryCSNforAlwaysOnCluster, helper, zlc)) {
                            return true;
                        }
                    }
                    return false;
                } catch (ServiceException se) {
                    ZimbraLog.ldap.debug("ServerFreshnessChecker unable to check related objects %s - assume stale",
                            server.getName(), se);
                    return true;
                } finally {
                    LdapClient.closeContext(zlc);
                }
            }
        }

        static class DomainFreshnessChecker implements IDomainCache.FreshnessChecker {
            private final LdapHelper helper;
            DomainFreshnessChecker(LdapHelper helper) {
                this.helper = helper;
            }
            @Override
            public boolean isStale(Domain entry) {
                if (!(entry instanceof Domain)) {
                    return false;
                }
                Domain domain = entry;
                ZLdapContext zlc = null;
                try {
                    zlc = LdapClient.getContext(LdapServerType.REPLICA, LdapUsage.COMPARE);
                    if (isEntryStale(domain, helper, zlc)) {
                        return true;
                    }
                    if (!(domain instanceof LdapDomain)) {
                        return false;
                    }
                    LdapDomain ldapDomain = (LdapDomain) domain;
                    String entryCSNforConfig = ldapDomain.getConfigEntryCSN();
                    if (null != entryCSNforConfig) {
                        String configDN  = helper.getProv().getDIT().configDN();
                        if (null != configDN) {
                            if (isEntryStale(configDN, entryCSNforConfig, helper, zlc)) {
                                return true;
                            }
                        }
                    }
                    return false;
                } catch (ServiceException se) {
                    ZimbraLog.ldap.debug("DomainFreshnessChecker unable to check related objects %s - assume stale",
                            domain.getName(), se);
                    return true;
                } finally {
                    LdapClient.closeContext(zlc);
                }
            }
        }

        static class NamedEntryFreshnessChecker implements INamedEntryCache.FreshnessChecker<NamedEntry> {
            private final LdapHelper helper;
            NamedEntryFreshnessChecker(LdapHelper helper) {
                this.helper = helper;
            }
            @Override
            public boolean isStale(NamedEntry entry) {
                return isEntryStale(entry, helper, null);
            }
        }

        private AccountFreshnessChecker acctFreshnessChecker = null;
        private NamedEntryFreshnessChecker neFreshnessChecker = null;
        private DomainFreshnessChecker domFreshnessChecker = null;
        private ServerFreshnessChecker svrFreshnessChecker = null;
        /* Cache config depends on server, alwaysoncluster and config - objects for 2 of which are cached.  The
         * caches for those can therefore be setup too early.
         * serverCacheSetup and alwaysOnClusterCacheSetup track when that has happened.
         */
        private boolean serverCacheSetup = false;
        private boolean alwaysOnClusterCacheSetup = false;

        public LRUMapCache(LdapHelper helper) {
            if (helper.getProv().supportsEntryCSN()) {
                neFreshnessChecker = new NamedEntryFreshnessChecker(helper);
                acctFreshnessChecker = new AccountFreshnessChecker(helper);
                domFreshnessChecker = new DomainFreshnessChecker(helper);
                svrFreshnessChecker = new ServerFreshnessChecker(helper);
            }

            /* Not planning to worry about whether the mime type cache is absolutely accurate.  It can be
             * flushed if needed and don't imagine mime types change much.
             */
            mimeTypeCache = new LdapMimeTypeCache();
        }

        @Override
        IAccountCache accountCache() {
            if (accountCache != null) {
                return accountCache;
            }
            Server svr = getLocalServer();
            synchronized(this) {
                if (accountCache == null) {
                    if (svr != null) {
                        accountCache = new AccountCache(svr.getLdapCacheAccountMaxSize(),
                                svr.getLdapCacheAccountMaxAge(), acctFreshnessChecker);
                    } else {
                        accountCache = new AccountCache(20000, 15 * Constants.MILLIS_PER_MINUTE, acctFreshnessChecker);
                    }
                }
            }
            return accountCache;
        }

        @Override
        IDomainCache domainCache() {
            if (domainCache != null) {
                return domainCache;
            }
            Server svr = getLocalServer();
            synchronized(this) {
                if (svr != null) {
                    domainCache = new DomainCache(svr.getLdapCacheDomainMaxSize(), svr.getLdapCacheDomainMaxAge(),
                            svr.getLdapCacheExternalDomainMaxSize(), svr.getLdapCacheExternalDomainMaxAge(),
                            domFreshnessChecker);
                } else {
                    domainCache = new DomainCache(500, 15 * Constants.MILLIS_PER_MINUTE,
                            10000, 15 * Constants.MILLIS_PER_MINUTE, domFreshnessChecker);
                }
            }
            return domainCache;
        }

        @Override
        IMimeTypeCache mimeTypeCache() {
            return mimeTypeCache;
        }

        @Override
        INamedEntryCache<LdapCos> cosCache() {
            if (cosCache != null) {
                return cosCache;
            }
            Server svr = getLocalServer();
            synchronized(this) {
                if (svr != null) {
                    cosCache = new NamedEntryCache<LdapCos>(svr.getLdapCacheCosMaxSize(),
                            svr.getLdapCacheCosMaxAge(), neFreshnessChecker);
                } else {
                    ZimbraLog.ldap.debug("CosCache setup using defaults");
                    cosCache = new NamedEntryCache<LdapCos>(100, 15 * Constants.MILLIS_PER_MINUTE, neFreshnessChecker);
                }
            }
            return cosCache;
        }

        /** Note: groups in this cache are expected to only contains minimal group attrs
         *       in particular, they do not contain members (the member or zimbraMailForwardingAddress attribute)
         */
        @Override
        INamedEntryCache<Group> groupCache() {
            if (groupCache != null) {
                return groupCache;
            }
            Server svr = getLocalServer();
            synchronized(this) {
                if (svr != null) {
                    groupCache = new NamedEntryCache<Group>(svr.getLdapCacheGroupMaxSize(),
                            svr.getLdapCacheGroupMaxAge(), neFreshnessChecker);
                } else {
                    ZimbraLog.ldap.debug("GroupCache setup using defaults");
                    groupCache = new NamedEntryCache<Group>(2000, 15 * Constants.MILLIS_PER_MINUTE, neFreshnessChecker);
                }
            }
            return groupCache;
        }

        @Override
        INamedEntryCache<Server> serverCache() {
            if (serverCacheSetup && (serverCache != null)) {
                return serverCache;
            }
            Server svr = localServer;
            if (svr != null) {
                if (serverCache != null) {
                    ZimbraLog.ldap.debug("ServerCache setup using server=%s", svr.getName());
                }
                synchronized(this) {
                    if (!serverCacheSetup) {
                        serverCache = new NamedEntryCache<Server>(svr.getLdapCacheServerMaxSize(),
                                svr.getLdapCacheServerMaxAge(), svrFreshnessChecker);
                        serverCacheSetup = true;
                    }
                }
            } else {
                synchronized(this) {
                    if (serverCache == null) {
                        ZimbraLog.ldap.debug("ServerCache setup using defaults");
                        serverCache = new NamedEntryCache<Server>(100, 15 * Constants.MILLIS_PER_MINUTE,
                                svrFreshnessChecker);
                    }
                }
            }
            return serverCache;
        }

        @Override
        INamedEntryCache<UCService> ucServiceCache() {
            if (ucServiceCache != null) {
                return ucServiceCache;
            }
            Server svr = getLocalServer();
            synchronized(this) {
                if (svr != null) {
                    ucServiceCache = new NamedEntryCache<UCService>(svr.getLdapCacheUCServiceMaxSize(),
                            svr.getLdapCacheUCServiceMaxAge(), neFreshnessChecker);
                } else {
                    ZimbraLog.ldap.debug("UCServiceCache setup using defaults");
                    ucServiceCache = new NamedEntryCache<UCService>(100,
                            15 * Constants.MILLIS_PER_MINUTE, neFreshnessChecker);
                }
            }
            return ucServiceCache;
        }

        @Override
        INamedEntryCache<ShareLocator> shareLocatorCache() {
            if (shareLocatorCache != null) {
                return shareLocatorCache;
            }
            Server svr = getLocalServer();
            synchronized(this) {
                if (svr != null) {
                    shareLocatorCache = new NamedEntryCache<ShareLocator>(svr.getLdapCacheShareLocatorMaxSize(),
                            svr.getLdapCacheShareLocatorMaxAge(), neFreshnessChecker);
                } else {
                    ZimbraLog.ldap.debug("ShareLocatorCache setup using defaults");
                    shareLocatorCache = new NamedEntryCache<ShareLocator>(5000, 15 * Constants.MILLIS_PER_MINUTE,
                            neFreshnessChecker);
                }
            }
            return shareLocatorCache;
        }

        @Override
        INamedEntryCache<XMPPComponent> xmppComponentCache() {
            if (xmppComponentCache != null) {
                return xmppComponentCache;
            }
            Server svr = getLocalServer();
            synchronized(this) {
                if (svr != null) {
                    xmppComponentCache = new NamedEntryCache<XMPPComponent>(svr.getLdapCacheXMPPComponentMaxSize(),
                            svr.getLdapCacheXMPPComponentMaxAge(), neFreshnessChecker);
                } else {
                    ZimbraLog.ldap.debug("XMPPComponentCache setup using defaults");
                    xmppComponentCache = new NamedEntryCache<XMPPComponent>(2000, 15 * Constants.MILLIS_PER_MINUTE,
                            neFreshnessChecker);
                }
            }
            return xmppComponentCache;
        }

        @Override
        INamedEntryCache<LdapZimlet> zimletCache() {
            if (zimletCache != null) {
                return zimletCache;
            }
            Server svr = getLocalServer();
            synchronized(this) {
                if (svr != null) {
                    zimletCache = new NamedEntryCache<LdapZimlet>(svr.getLdapCacheZimletMaxSize(),
                            svr.getLdapCacheZimletMaxAge(), neFreshnessChecker);
                } else {
                    ZimbraLog.ldap.debug("ZimletCache setup using defaults");
                    zimletCache = new NamedEntryCache<LdapZimlet>(100, 15 * Constants.MILLIS_PER_MINUTE, neFreshnessChecker);
                }
            }
            return zimletCache;
        }

        @Override
        INamedEntryCache<AlwaysOnCluster> alwaysOnClusterCache() {
            if (alwaysOnClusterCacheSetup && (alwaysOnClusterCache != null)) {
                return alwaysOnClusterCache;
            }
            Server svr = localServer;
            if (svr != null) {
                if (alwaysOnClusterCache != null) {
                    ZimbraLog.ldap.debug("AlwaysOnClusterCache setup using server=%s", svr.getName());
                }
                synchronized(this) {
                    if (!alwaysOnClusterCacheSetup) {
                        alwaysOnClusterCache = new NamedEntryCache<AlwaysOnCluster>(
                                svr.getLdapCacheAlwaysOnClusterMaxSize(),
                                svr.getLdapCacheAlwaysOnClusterMaxAge(), neFreshnessChecker);
                        alwaysOnClusterCacheSetup = true;
                    }
                }
            } else {
                synchronized(this) {
                    if (alwaysOnClusterCache == null) {
                        ZimbraLog.ldap.debug("AlwaysOnClusterCache setup using defaults");
                        alwaysOnClusterCache = new NamedEntryCache<AlwaysOnCluster>(100,
                                15 * Constants.MILLIS_PER_MINUTE, neFreshnessChecker);
                    }
                }
            }
            return alwaysOnClusterCache;
        }

        @Override
        boolean isServerCacheSetup() {
            return serverCacheSetup;
        }

        public void setLocalServer(Server server) {
            localServer = server;
        }

        private static Server getLocalServer() {
            if (localServer == null) {
                try {
                    localServer = Provisioning.getInstance().getLocalServer();
                } catch (ServiceException e) {
                    localServer = null;
                }
            }
            return localServer;
        }
    }

    /**
     *
     * NoopCache
     *
     */
    static class NoopCache extends LdapCache {

        private final IAccountCache accountCache = new NoopAccountCache();
        private final INamedEntryCache<LdapCos> cosCache = new NoopNamedEntryCache<LdapCos>();
        private final INamedEntryCache<ShareLocator> shareLocatorCache = new NoopNamedEntryCache<ShareLocator>();
        private final IDomainCache domainCache = new NoopDomainCache();
        private final IMimeTypeCache mimeTypeCache = new NoopMimeTypeCache();
        private final INamedEntryCache<Server> serverCache = new NoopNamedEntryCache<Server>();
        private final INamedEntryCache<UCService> ucServiceCache = new NoopNamedEntryCache<UCService>();
        private final INamedEntryCache<LdapZimlet> zimletCache = new NoopNamedEntryCache<LdapZimlet>();
        private final INamedEntryCache<Group> groupCache = new NoopNamedEntryCache<Group>();
        private final INamedEntryCache<XMPPComponent> xmppComponentCache = new NoopNamedEntryCache<XMPPComponent>();
        private final INamedEntryCache<AlwaysOnCluster> alwaysOnClusterCache = new NoopNamedEntryCache<AlwaysOnCluster>();


        static class NoopAccountCache implements IAccountCache {
            @Override
            public void clear() {}

            @Override
            public void remove(Account entry) {}

            @Override
            public void put(Account entry) {}

            @Override
            public void replace(Account entry) {}

            @Override
            public Account getById(String key) { return null; }

            @Override
            public Account getByName(String key) { return null; }

            @Override
            public Account getByForeignPrincipal(String key) { return null; }

            @Override
            public int getSize() { return 0; }

            @Override
            public double getHitRate() { return 0; }
        }

        static class NoopDomainCache implements IDomainCache {

            @Override
            public void clear() {}

            @Override
            public Domain getByForeignName(String key, GetFromDomainCacheOption option) { return null; }

            @Override
            public Domain getById(String key, GetFromDomainCacheOption option) { return null; }

            @Override
            public Domain getByKrb5Realm(String key, GetFromDomainCacheOption option) { return null; }

            @Override
            public Domain getByName(String key, GetFromDomainCacheOption option) { return null; }

            @Override
            public Domain getByVirtualHostname(String key, GetFromDomainCacheOption option) { return null; }

            @Override
            public void put(DomainBy domainBy, String key, Domain entry) {}

            @Override
            public void remove(Domain entry) {}

            @Override
            public void removeFromNegativeCache(DomainBy domainBy, String key) {}

            @Override
            public void replace(Domain entry) {}

            @Override
            public double getHitRate() { return 0; }

            @Override
            public int getSize() { return 0; }

        }

        static class NoopNamedEntryCache<E extends NamedEntry> implements INamedEntryCache<E> {

            @Override
            public void clear() {}

            @Override
            public E getById(String key) { return null; }

            @Override
            public E getByName(String key) { return null; }

            @Override
            public double getHitRate() { return 0; }

            @Override
            public int getSize() { return 0; }

            @Override
            public void put(E entry) {}

            @Override
            public void put(List<E> entries, boolean clear) {}

            @Override
            public void remove(String name, String id) {}

            @Override
            public void remove(E entry) {}

            @Override
            public void replace(E entry) {}
        }

        static class NoopMimeTypeCache implements IMimeTypeCache {

            private final List<MimeTypeInfo> mimeTypes =
                Collections.unmodifiableList(new ArrayList<MimeTypeInfo>());

            @Override
            public void flushCache(Provisioning prov) throws ServiceException {}

            @Override
            public List<MimeTypeInfo> getAllMimeTypes(Provisioning prov)
                    throws ServiceException {
                return mimeTypes;
            }

            @Override
            public List<MimeTypeInfo> getMimeTypes(Provisioning prov,
                    String mimeType) throws ServiceException {
                return mimeTypes;
            }

        }

        @Override
        IAccountCache accountCache() {
            return accountCache;
        }

        @Override
        INamedEntryCache<LdapCos> cosCache() {
            return cosCache;
        }

        @Override
        IDomainCache domainCache() {
            return domainCache;
        }

        @Override
        INamedEntryCache<Group> groupCache() {
            return groupCache;
        }

        @Override
        IMimeTypeCache mimeTypeCache() {
            return mimeTypeCache;
        }

        @Override
        INamedEntryCache<Server> serverCache() {
            return serverCache;
        }

        @Override
        INamedEntryCache<UCService> ucServiceCache() {
            return ucServiceCache;
        }

        @Override
        INamedEntryCache<ShareLocator> shareLocatorCache() {
            return shareLocatorCache;
        }

        @Override
        INamedEntryCache<XMPPComponent> xmppComponentCache() {
            return xmppComponentCache;
        }

        @Override
        INamedEntryCache<LdapZimlet> zimletCache() {
            return zimletCache;
        }

        @Override
        INamedEntryCache<AlwaysOnCluster> alwaysOnClusterCache() {
            return alwaysOnClusterCache;
        }

        @Override
        boolean isServerCacheSetup() {
            return (null != serverCache);
        }
    }
}
