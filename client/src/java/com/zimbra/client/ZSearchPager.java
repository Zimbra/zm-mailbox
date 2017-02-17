/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.client;

import com.zimbra.common.service.ServiceException;
import com.zimbra.client.event.ZModifyItemEvent;
import com.zimbra.client.event.ZModifyConversationEvent;
import com.zimbra.client.event.ZCreateItemEvent;
import com.zimbra.client.event.ZCreateMessageEvent;
import com.zimbra.client.ZSearchParams.Cursor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ZSearchPager {

    private List<ZSearchResult> mResults;
    private ZSearchParams mParams;
    private Map<String, ZSearchHit> mHitMap;
    private String mConvId;
    private boolean mDirty;

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
            ZSearchResult result = mParams.getConvId() == null ? mailbox.search(mParams) : mailbox.searchConversation(mParams.getConvId(), mParams);
            
            mResults.add(result);
            if (result.getConversationSummary() != null) {
                mConvId = result.getConversationSummary().getId();
            }
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

    public boolean isDirty() {
        return mDirty;
    }

    /**
     *
     * @return conversation id if this pager holds result of a SearchConv
     */
    public String getConversationId() {
        return mConvId;
    }

    /**
     * @param event modify event
     * @throws com.zimbra.common.service.ServiceException on error
     */
    void modifyNotification(ZModifyItemEvent event) throws ServiceException {
        if (mConvId != null && event instanceof ZModifyConversationEvent) {
            ZModifyConversationEvent mce = (ZModifyConversationEvent) event;
            if (mce.getMessageCount(-1) != -1) {
                mDirty = true;
            }
            if (mce.getId().equals(mConvId)) {
                for (ZSearchResult result : mResults) {
                    result.getConversationSummary().modifyNotification(event);
                }
            }
        }
        ZSearchHit hit = mHitMap.get(event.getId());
        if (hit != null)
            hit.modifyNotification(event);
        }

    /**
     * @param event create item event
     * @throws com.zimbra.common.service.ServiceException on error
     */
    void createNotification(ZCreateItemEvent event) throws ServiceException {
        if (mConvId != null && event instanceof ZCreateMessageEvent) {
            ZCreateMessageEvent cme = (ZCreateMessageEvent) event;
            if (mConvId.equals(cme.getConversationId(null)))
                mDirty = true;
        }
    }
}
