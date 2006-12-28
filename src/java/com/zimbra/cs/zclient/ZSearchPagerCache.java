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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.zclient.event.ZCreateEvent;
import com.zimbra.cs.zclient.event.ZDeleteEvent;
import com.zimbra.cs.zclient.event.ZEventHandler;
import com.zimbra.cs.zclient.event.ZModifyEvent;
import com.zimbra.cs.zclient.event.ZRefreshEvent;
import com.zimbra.cs.zclient.event.ZModifyItemEvent;
import org.apache.commons.collections.MapIterator;
import org.apache.commons.collections.map.LRUMap;

import java.util.List;

class ZSearchPagerCache extends ZEventHandler {

    LRUMap mSearchPagerCache;

    ZSearchPagerCache(int maxItems) {
        mSearchPagerCache = new LRUMap(maxItems);
    }

    public synchronized ZSearchPagerResult search(ZMailbox mailbox, ZSearchParams params, int page, boolean ignoreCached) throws ServiceException {
        ZSearchPager pager = (ZSearchPager) mSearchPagerCache.get(params);
        if (pager != null && ignoreCached)
            mSearchPagerCache.remove(params);
        if (pager == null) {
            pager = new ZSearchPager(params);
            mSearchPagerCache.put(params, pager);
        }
        ZSearchResult result = pager.search(mailbox, page);
        return new ZSearchPagerResult(result,
                page,
                page >= pager.getNumberOfPages() ?  pager.getNumberOfPages()-1 : page,
                params.getLimit());
    }

    public synchronized void handleRefresh(ZRefreshEvent refreshEvent, ZMailbox mailbox) throws ServiceException {
        // clear cache on a refresh
        mSearchPagerCache.clear();
    }

    public synchronized void handleCreate(ZCreateEvent event, ZMailbox mailbox) throws ServiceException {
        // clear cache on any creates, since they could logically belong to a cached result
        // TODO: maybe not? Just continue to use cache as long as possible?
        //mSearchPagerCache.clear();
    }


    public synchronized void handleModify(ZModifyEvent event, ZMailbox mailbox) throws ServiceException {
        if (event instanceof ZModifyItemEvent) {
            for (Object obj : mSearchPagerCache.values()) {
                ((ZSearchPager)obj).modifyNotification((ZModifyItemEvent)event);
            }
        }
    }

    public synchronized void handleDelete(ZDeleteEvent event, ZMailbox mailbox) throws ServiceException {
        // only delete cached pagers that contain an item.
        // easier to just clear the whole cache?
        List<String> ids = event.toList();
        MapIterator mi = mSearchPagerCache.mapIterator();
        while(mi.hasNext()) {
        	mi.next();
            ZSearchPager pager = (ZSearchPager) mi.getValue();
            if (pager.hasAnyItem(ids))
                mi.remove();
        }
    }
    
}
