/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.zclient;

import java.util.Iterator;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.MapUtil;
import com.zimbra.cs.zclient.event.ZCreateEvent;
import com.zimbra.cs.zclient.event.ZCreateItemEvent;
import com.zimbra.cs.zclient.event.ZCreateMessageEvent;
import com.zimbra.cs.zclient.event.ZDeleteEvent;
import com.zimbra.cs.zclient.event.ZEventHandler;
import com.zimbra.cs.zclient.event.ZModifyEvent;
import com.zimbra.cs.zclient.event.ZModifyItemEvent;
import com.zimbra.cs.zclient.event.ZModifyItemFolderEvent;
import com.zimbra.cs.zclient.event.ZRefreshEvent;

class ZSearchPagerCache extends ZEventHandler {

    Map<ZSearchParams, ZSearchPager> mSearchPagerCache;
    private boolean mClearOnModifyItemFolder;

    ZSearchPagerCache(int maxItems, boolean clearOnModifyItemFolder) {
        mSearchPagerCache = MapUtil.newLruMap(maxItems);
        mClearOnModifyItemFolder = clearOnModifyItemFolder;
    }
    
    public synchronized ZSearchPagerResult search(ZMailbox mailbox, ZSearchParams params, int page, boolean useCache, boolean useCursor) throws ServiceException {
        ZSearchPager pager = mSearchPagerCache.get(params);
        if (pager != null && (!useCache || pager.isDirty())) {
            mSearchPagerCache.remove(params);
            pager = null;
        }
        if (pager == null) {
            pager = new ZSearchPager(params);
            mSearchPagerCache.put(params, pager);
        }
        ZSearchResult result = pager.search(mailbox, page, useCursor);
        return new ZSearchPagerResult(result,
                page,
                page >= pager.getNumberOfPages() ?  pager.getNumberOfPages()-1 : page,
                params.getLimit());
    }

    public synchronized void clear(String type) {
        if (type == null) {
            mSearchPagerCache.clear();
        } else {
            Iterator<Map.Entry<ZSearchParams, ZSearchPager>> mi = mSearchPagerCache.entrySet().iterator();
            while(mi.hasNext()) {
                ZSearchParams params = mi.next().getKey();
                if (params.getTypes() != null && params.getTypes().contains(type))
                    mi.remove();
            }
        }
    }
    
    public synchronized void handleRefresh(ZRefreshEvent refreshEvent, ZMailbox mailbox) throws ServiceException {
        // clear cache on a refresh
        mSearchPagerCache.clear();
    }

    public synchronized void handleCreate(ZCreateEvent event, ZMailbox mailbox) throws ServiceException {
        // TODO: continue to use cache as long as possible?
        // For now, just mark conv searches dirty when
        // a new message gets created in a cached search conv.
        //mSearchPagerCache.clear();
        if (event instanceof ZCreateMessageEvent) {
            for (Object obj : mSearchPagerCache.values()) {
                ((ZSearchPager)obj).createNotification((ZCreateItemEvent)event);
            }
        }
    }


    public synchronized void handleModify(ZModifyEvent event, ZMailbox mailbox) throws ServiceException {
		if (mClearOnModifyItemFolder && event instanceof ZModifyItemFolderEvent) {
			ZModifyItemFolderEvent mif = (ZModifyItemFolderEvent) event;
			if (mif.getFolderId(null) != null)
				mSearchPagerCache.clear();
		}
		if (event instanceof ZModifyItemEvent) {
            for (Object obj : mSearchPagerCache.values()) {
                ((ZSearchPager)obj).modifyNotification((ZModifyItemEvent)event);
            }
        }
    }

    public synchronized void handleDelete(ZDeleteEvent event, ZMailbox mailbox) throws ServiceException {
        // only delete cached pagers that contain an item?
        // easier to just clear the whole cache?
        mSearchPagerCache.clear();
    }
    
}
