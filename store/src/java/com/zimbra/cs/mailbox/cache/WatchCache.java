/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2020 Synacor, Inc.
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
package com.zimbra.cs.mailbox.cache;

import java.io.IOException;
import java.util.Collection;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.OctopusXmlConstants;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.LruMap;
import com.zimbra.common.util.MapUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ZimbraAuthToken;
import com.zimbra.cs.db.DbEvent;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.iochannel.MessageChannel;
import com.zimbra.cs.iochannel.WatchMessage;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.OperationContextData;
import com.zimbra.cs.service.util.ItemId;

public abstract class WatchCache extends OperationContextData {

    public static final String KEY = "WatchCache";

    public static WatchCache get(Account account) {
        try {
            if (account.getServer() != null && !Provisioning.onLocalServer(account)) {
                return new RemoteCache(account);
            }
        } catch (ServiceException e) {
        }
        return new LocalCache(account);
    }

    public static WatchCache get(OperationContext octxt) {
        OperationContextData cache = octxt.getCtxtData(KEY);
        if (cache != null && cache instanceof WatchCache) {
            return (WatchCache) cache;
        }
        WatchCache watchCache = get(octxt.getAuthenticatedUser());
        watchCache.addTo(octxt);
        return watchCache;
    }

    public abstract void watch(String accountId, int itemId) throws ServiceException;
    public abstract void unwatch(String accountId, int itemId) throws ServiceException;

    private void addTo(OperationContext octxt) {
        octxt.setCtxtData(KEY, this);
    }

    public boolean hasMapping(MailItem item) {
        try {
            return hasMapping(item.getAccount().getId(), item.getId());
        } catch (ServiceException e) {
            return false;
        }
    }

    public boolean hasMapping(String accountId, int itemId) {
        synchronized (items) {
            if (items.containsKey(accountId)) {
                Collection<Integer> itemIds = items.get(accountId);
                return itemIds.contains(itemId);
            }
        }
        return false;
    }

    public Multimap<String, Integer> getMap() {
        return ImmutableMultimap.copyOf(items);
    }


    protected static Log LOG = LogFactory.getLog(WatchCache.class);
    protected static LruMap<String,HashMultimap<String,Integer>> watchMappings = MapUtil.newLruMap(100);
    protected HashMultimap<String,Integer> items;
    protected final Account account;
    protected ZimbraAuthToken authToken;

    protected WatchCache(Account account) {
        super(null);
        this.account = account;
        synchronized (watchMappings) {
            items = watchMappings.get(account.getId());
        }
        if (items == null) {
            try {
                load();
            } catch (ServiceException e) {
                LOG.error("can't load watch mapping for account %s", account.getName(), e);
            }
        }
    }

    protected abstract void load() throws ServiceException;

    private static class LocalCache extends WatchCache {
        LocalCache(Account account) {
            super(account);
        }

        @Override
        public void watch(String accountId, int itemId) throws ServiceException {
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            DbEvent.addWatch(mbox, accountId, itemId);
            synchronized (items) {
                items.put(accountId, itemId);
            }
        }

        @Override
        public void unwatch(String accountId, int itemId) throws ServiceException {
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            DbEvent.removeWatch(mbox, accountId, itemId);
            synchronized (items) {
                items.remove(accountId, itemId);
            }
        }
        @Override
        protected void load() throws ServiceException {
            items = HashMultimap.create();
            if (account.getServer() == null) {
                // system admin accounts have no server or mailbox
                return;
            }
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            for (Pair<String,Integer> mapping : DbEvent.getWatchingItems(mbox)) {
                items.put(mapping.getFirst(), mapping.getSecond());
            }

            synchronized (watchMappings) {
                watchMappings.put(account.getId(), items);
            }
        }
    }

    private static class RemoteCache extends WatchCache {
        RemoteCache(Account account) {
            super(account);
        }

        @Override
        public void watch(String accountId, int itemId) throws ServiceException {
            WatchMessage msg = WatchMessage.watch(account.getId(), accountId, itemId);
            MessageChannel.getInstance().sendMessage(msg);
        }

        @Override
        public void unwatch(String accountId, int itemId) throws ServiceException {
            WatchMessage msg = WatchMessage.unwatch(account.getId(), accountId, itemId);
            MessageChannel.getInstance().sendMessage(msg);
        }

        @Override
        protected void load() throws ServiceException {
            items = HashMultimap.create();
            SoapHttpTransport transport = null;
            try {
                if (authToken == null) {
                    authToken = new ZimbraAuthToken(account);
                }
                String url = URLUtil.getSoapURL(account.getServer(), true);
                transport = new SoapHttpTransport(url);
                transport.setTargetAcctId(account.getId());
                transport.setAuthToken(authToken.toZAuthToken());
                transport.setTimeout(LC.httpclient_soaphttptransport_so_timeout.intValue());
                transport.setResponseProtocol(SoapProtocol.Soap12);
                XMLElement req = new XMLElement(OctopusXmlConstants.GET_WATCHING_ITEMS_REQUEST);
                Element body = transport.invokeWithoutSession(req);
                for (Element target : body.listElements(MailConstants.E_TARGET)) {
                    String accountId = target.getAttribute(MailConstants.A_ID);
                    for (Element item : target.listElements(MailConstants.E_ITEM)) {
                        ItemId iid = new ItemId(item.getAttribute(MailConstants.A_ID), accountId);
                        items.put(iid.getAccountId(), iid.getId());
                    }
                }
            } catch (IOException e) {
                throw ServiceException.PROXY_ERROR(e, account.getName());
            } finally {
                if (transport != null)
                    transport.shutdown();
            }
        }
    }
}
