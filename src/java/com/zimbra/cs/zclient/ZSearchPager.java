/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.zclient.event.ZModifyItemEvent;
import com.zimbra.cs.zclient.event.ZModifyConversationEvent;
import com.zimbra.cs.zclient.event.ZCreateItemEvent;
import com.zimbra.cs.zclient.event.ZCreateMessageEvent;
import com.zimbra.cs.zclient.ZSearchParams.Cursor;

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
