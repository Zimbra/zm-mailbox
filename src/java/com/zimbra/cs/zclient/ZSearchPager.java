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
import com.zimbra.cs.zclient.event.ZModifyItemEvent;
import com.zimbra.cs.zclient.ZSearchParams.Cursor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ZSearchPager {

    private List<ZSearchResult> mResults;
    private ZSearchParams mParams;
    private Map<String, ZSearchHit> mHitMap;

    ZSearchPager(ZSearchParams params) {
        mParams = new ZSearchParams(params);
        mResults = new ArrayList<ZSearchResult>();
        mHitMap = new HashMap<String, ZSearchHit>();
    }
    
    ZSearchResult search(ZMailbox mailbox, int page, boolean useCursor) throws ServiceException {
        while(mResults.size() <= page) {
            if (mResults.size() == 0){
                mParams.setCursor(null);
                mParams.setOffset(0);
            } else {
                ZSearchResult lastResult = mResults.get(mResults.size()-1);
                if (!lastResult.hasMore()) break;
                if (useCursor) {
                    List<ZSearchHit> lastHits = lastResult.getHits();
                    ZSearchHit lastHit = lastHits.get(lastHits.size()-1);
                    mParams.setCursor(new Cursor(lastHit.getId(), lastHit.getSortField()));
                } else {
                    mParams.setCursor(null);
                    mParams.setOffset(page*mParams.getLimit());
                }
            }
            ZSearchResult result = mailbox.search(mParams);
            mResults.add(result);
            for (ZSearchHit hit : result.getHits())
                mHitMap.put(hit.getId(), hit);
            if (!result.hasMore()) break;
        }
        if (page < mResults.size())
            return mResults.get(page);
        else
            return mResults.get(mResults.size()-1);
    }
    
    public List<ZSearchResult> getResults() {
        return mResults;
    }

    public int getNumberOfPages() {
        return mResults.size();
    }
    
    public ZSearchResult get(int page) {
        return mResults.get(page);
    }
    
    /**
     * @param id item id
     * @return true if this search result contains the given item
     */
    boolean hasItem(String id) {
        return mHitMap.get(id) != null;
    }
    
    /**
     * @param ids item ids
     * @return true if this search result contains any of the given items
     */
    boolean hasAnyItem(List<String> ids) {
        for (String id: ids) {
            if (mHitMap.get(id) != null) return true;
        }
        return false;
    }

    /**
     * @param event modify event
     * @throws com.zimbra.common.service.ServiceException on error
     */
    void modifyNotification(ZModifyItemEvent event) throws ServiceException {
        ZSearchHit hit = mHitMap.get(event.getId());
        if (hit != null)
            hit.modifyNotification(event);
    }
}
