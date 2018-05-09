/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.admin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.CacheExtension;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.CacheEntry;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.PermissionCache;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.gal.GalGroup;
import com.zimbra.cs.pubsub.PubSubService;
import com.zimbra.cs.pubsub.message.FlushCacheMsg;
import com.zimbra.cs.util.WebClientL10nUtil;
import com.zimbra.cs.zimlet.ZimletUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.FlushCacheRequest;
import com.zimbra.soap.admin.message.FlushCacheResponse;
import com.zimbra.soap.admin.type.CacheEntrySelector;
import com.zimbra.soap.admin.type.CacheEntryType;
import com.zimbra.soap.admin.type.CacheSelector;

public class FlushCache extends AdminDocumentHandler {

    public static final String FLUSH_CACHE = "flushCache";

    /**
     * must be careful and only allow deletes domain admin has access to
     */
    @Override
    public boolean domainAuthSufficient(Map<String, Object> context) {
        return true;
    }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        FlushCacheRequest req = zsc.elementToJaxb(request);
        doFlushCache(this, context, req);
        return zsc.jaxbToElement(new FlushCacheResponse());
    }

    public static void doFlushCache(AdminDocumentHandler handler, Map<String, Object> context, FlushCacheRequest req)
    throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Server localServer = Provisioning.getInstance().getLocalServer();
        handler.checkRight(zsc, context, localServer, Admin.R_flushCache);

        PubSubService.getInstance().publish(PubSubService.BROADCAST, new FlushCacheMsg(req.getCache()));
    }

    public static void doFlush(CacheSelector cacheSelector) throws ServiceException {
        String[] types = cacheSelector.getTypes().split(",");
        for (String type : types) {
            CacheEntryType cacheType = null;
            try {
                cacheType = CacheEntryType.fromString(type);
                doFlush(null, cacheType, cacheSelector);
            } catch (ServiceException e) {
                if (cacheType == null) {
                    // see if it is a registered extension
                    CacheExtension ce = CacheExtension.getHandler(type);
                    if (ce != null) {
                        ce.flushCache();
                    } else {
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        }
    }

    public static void doFlush(Map<String, Object> context, CacheEntryType cacheType, CacheSelector cacheSelector)
    throws ServiceException {

        String mailURL = Provisioning.getInstance().getLocalServer().getMailURL();
        switch (cacheType) {
        case acl:
            PermissionCache.invalidateCache();
            break;
        case all:
            flushLdapCache(cacheType, cacheSelector);
            Provisioning.getInstance().refreshValidators(); // refresh other bits of cached license data
            break;
        case galgroup:
            GalGroup.flushCache(getCacheEntries(cacheSelector));
            break;
        case uistrings:
            // Note: This was removed because it uses HTTP requests to all the UI nodes from LDAP
            //       Making requests to the /service - ServiceServlet endpoint in the zm-ajax / zm-admin-ajax packages
            ZimbraLog.mailbox.debug("FlushCache.doFlush: Cowardly refusing to flush the 'uistrings'.");
            break;
        case skin:
            ZimbraLog.mailbox.debug("FlushCache.doFlush: Cowardly refusing to flush the 'skin'.");
            // Note: This was removed because it uses HTTP requests to all the UI nodes from LDAP
            //       Making requests to the /service - ServiceServlet endpoint in the zm-ajax / zm-admin-ajax packages
            break;
        case locale:
            WebClientL10nUtil.flushCache();
            break;
        case license:
            flushLdapCache(CacheEntryType.config, cacheSelector); // refresh global config for parsed license
            Provisioning.getInstance().refreshValidators(); // refresh other bits of cached license data
            break;
        case zimlet:
            ZimletUtil.flushDiskCache(context);
            // fall through to also flush ldap entries
        default:
            flushLdapCache(cacheType, cacheSelector);
        }
    }

    private static CacheEntry[] getCacheEntries(CacheSelector cacheSelector) throws ServiceException {
        List<CacheEntrySelector> cacheEntrySelectors = cacheSelector.getEntries();
        if (cacheEntrySelectors.size() < 1) {
            return null;
        }
        CacheEntry[] entries = new CacheEntry[cacheEntrySelectors.size()];
            int i = 0;
            for (CacheEntrySelector cacheEntrySelector : cacheEntrySelectors) {
                entries[i++] = new CacheEntry(
                        cacheEntrySelector.getBy().toKeyCacheEntryBy(), cacheEntrySelector.getKey());
            }
        return entries;
    }

    private static void flushLdapCache(CacheEntryType cacheType, CacheSelector cacheSelector)
    throws ServiceException {
        CacheEntry[] entries = getCacheEntries(cacheSelector);
        Provisioning.getInstance().flushCache(cacheType, entries);
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_flushCache);
    }

    private static void flushCacheOnImapDaemons(FlushCacheRequest req, ZimbraSoapContext zsc) throws ServiceException {
        CacheSelector selector = req.getCache();
        String cacheTypes = selector.getTypes();
        CacheEntry[] cacheEntries = getCacheEntries(selector);
        Account acct = Provisioning.getInstance().get(AccountBy.id, zsc.getAuthtokenAccountId(), zsc.getAuthToken());
        flushCacheOnImapDaemons(cacheTypes, cacheEntries, acct.getName(), zsc.getAuthToken());
    }

    public static void flushCacheOnImapDaemons(String cacheTypes, CacheEntry[] entries, AuthToken authToken) {
        flushCacheOnImapDaemons(cacheTypes, entries, LC.zimbra_ldap_user.value(), authToken);
    }
    public static void flushCacheOnImapDaemons(String cacheTypes, CacheEntry[] entries, String acctName, AuthToken authToken) {
        List<Server> imapServers;
        try {
            imapServers = Provisioning.getIMAPDaemonServersForLocalServer();
        } catch (ServiceException e) {
            ZimbraLog.imap.warn("unable to fetch list of imapd servers", e);
            return;
        }
        for (Server server: imapServers) {
            flushCacheOnImapDaemon(server, cacheTypes, entries, acctName, authToken);
        }
    }

    public static void flushCacheOnImapDaemon(Server server, String cacheTypes, CacheEntry[] entries) {
        try {
            flushCacheOnImapDaemon(server, cacheTypes, entries, LC.zimbra_ldap_user.value(), AuthProvider.getAdminAuthToken());
        } catch (ServiceException e) {
            ZimbraLog.imap.warn("unable to generate admin auth token to issue X-ZIMBRA-FLUSHCACHE request to imapd server '%s'", server.getServiceHostname(), e);
        }
    }

    public static void flushCacheOnImapDaemon(Server server, String cacheTypes, CacheEntry[] entries, String userName, AuthToken authToken) {
        if (server == null) {
            return;
        }
        ImapConnection connection = null;
        try {
            connection = ImapConnection.getZimbraConnection(server, userName, authToken);
        } catch (ServiceException e) {
            ZimbraLog.imap.warn("unable to connect to imapd server '%s' to issue X-ZIMBRA-FLUSHCACHE request", server.getServiceHostname(), e);
            return;
        }
        try {
            String imapTypes = sanitizeImapCacheTypes(cacheTypes);
            if (imapTypes != null) {
                ZimbraLog.imap.debug("issuing X-ZIMBRA-FLUSHCACHE request to imapd server '%s' for cache types '%s'", server.getServiceHostname(), imapTypes);
                if (entries == null || entries.length == 0) {
                    connection.flushCache(imapTypes);
                } else {
                    connection.flushCache(imapTypes, entries);
                }
            }
        } catch (IOException e) {
            ZimbraLog.imap.warn("unable to issue X-ZIMBRA-FLUSHCACHE request to imapd server '%s'", server.getServiceHostname(), e);
        } finally {
            connection.close();
        }
    }

    private static String sanitizeImapCacheTypes(String cacheTypes) {
        List<String> imapTypes = new ArrayList<String>();
        for (String typeStr: cacheTypes.split(",")) {
            try {
                CacheEntryType cacheType = CacheEntryType.fromString(typeStr);
                if (ImapHandler.IMAP_CACHE_TYPES.contains(cacheType)) {
                    //filter out cache types that don't need to be flushed on imapd servers
                    imapTypes.add(typeStr);
                }
            } catch (ServiceException e) {
                //shouldn't encounter invalid cache types
            }
        }
        return imapTypes.isEmpty() ? null : Joiner.on(",").join(imapTypes);
    }
}
