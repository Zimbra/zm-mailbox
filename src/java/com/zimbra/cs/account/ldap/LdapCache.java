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
import com.zimbra.common.localconfig.LC;
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

    public static long ldapCacheFreshnessCheckLimitMs() {
        return LC.ldap_cache_freshness_check_limit_ms.intValue();
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

        private final IAccountCache accountCache;
        private final INamedEntryCache<LdapCos> cosCache;
        private final INamedEntryCache<ShareLocator> shareLocatorCache;
        private final IDomainCache domainCache;
        private final IMimeTypeCache mimeTypeCache;
        private final INamedEntryCache<Server> serverCache;
        private final INamedEntryCache<UCService> ucServiceCache;
        private final INamedEntryCache<LdapZimlet> zimletCache;
        private final INamedEntryCache<Group> groupCache;
        private final INamedEntryCache<XMPPComponent> xmppComponentCache;
        private final INamedEntryCache<AlwaysOnCluster> alwaysOnClusterCache;

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

        public LRUMapCache(LdapHelper helper) {
            NamedEntryFreshnessChecker neFreshnessChecker = null;
            AccountFreshnessChecker acctFreshnessChecker = null;
            DomainFreshnessChecker domFreshnessChecker = null;
            ServerFreshnessChecker svrFreshnessChecker = null;
            if (helper.getProv().supportsEntryCSN()) {
                neFreshnessChecker = new NamedEntryFreshnessChecker(helper);
                acctFreshnessChecker = new AccountFreshnessChecker(helper);
                domFreshnessChecker = new DomainFreshnessChecker(helper);
                svrFreshnessChecker = new ServerFreshnessChecker(helper);
            }

            accountCache = new AccountCache(LC.ldap_cache_account_maxsize.intValue(),
                    LC.ldap_cache_account_maxage.intValue() * Constants.MILLIS_PER_MINUTE, acctFreshnessChecker);

            cosCache = new NamedEntryCache<LdapCos>(LC.ldap_cache_cos_maxsize.intValue(),
                    LC.ldap_cache_cos_maxage.intValue() * Constants.MILLIS_PER_MINUTE, neFreshnessChecker);
            shareLocatorCache = new NamedEntryCache<ShareLocator>(
                    LC.ldap_cache_share_locator_maxsize.intValue(),
                    LC.ldap_cache_share_locator_maxage.intValue() * Constants.MILLIS_PER_MINUTE, neFreshnessChecker);
            domainCache = new DomainCache(
                    LC.ldap_cache_domain_maxsize.intValue(),
                    LC.ldap_cache_domain_maxage.intValue() * Constants.MILLIS_PER_MINUTE,
                    LC.ldap_cache_external_domain_maxsize.intValue(),
                    LC.ldap_cache_external_domain_maxage.intValue() * Constants.MILLIS_PER_MINUTE, domFreshnessChecker);

            /* Not planning to worry about whether the mime type cache is absolutely accurate.  It can be
             * flushed if needed and don't imagine mime types change much.
             */
            mimeTypeCache = new LdapMimeTypeCache();
            serverCache = new NamedEntryCache<Server>(
                    LC.ldap_cache_server_maxsize.intValue(),
                    LC.ldap_cache_server_maxage.intValue() * Constants.MILLIS_PER_MINUTE, svrFreshnessChecker);
            ucServiceCache = new NamedEntryCache<UCService>(
                    LC.ldap_cache_ucservice_maxsize.intValue(),
                    LC.ldap_cache_ucservice_maxage.intValue() * Constants.MILLIS_PER_MINUTE, neFreshnessChecker);
            zimletCache = new NamedEntryCache<LdapZimlet>(
                    LC.ldap_cache_zimlet_maxsize.intValue(),
                    LC.ldap_cache_zimlet_maxage.intValue() * Constants.MILLIS_PER_MINUTE, neFreshnessChecker);

            /* Note: groups in this cache are expected to only contains minimal group attrs
             *       in particular, they do not contain members (the member or zimbraMailForwardingAddress attribute)
             */
            groupCache = new NamedEntryCache<Group>(
                    LC.ldap_cache_group_maxsize.intValue(),
                    LC.ldap_cache_group_maxage.intValue() * Constants.MILLIS_PER_MINUTE, neFreshnessChecker);
            xmppComponentCache = new NamedEntryCache<XMPPComponent>(
                    LC.ldap_cache_xmppcomponent_maxsize.intValue(),
                    LC.ldap_cache_xmppcomponent_maxage.intValue() * Constants.MILLIS_PER_MINUTE, neFreshnessChecker);
            alwaysOnClusterCache = new NamedEntryCache<AlwaysOnCluster>(
                    LC.ldap_cache_alwaysoncluster_maxsize.intValue(),
                    LC.ldap_cache_alwaysoncluster_maxage.intValue() * Constants.MILLIS_PER_MINUTE, neFreshnessChecker);
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

    }
}
