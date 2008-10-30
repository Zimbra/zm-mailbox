/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient;

import org.json.JSONException;

public class ZSearchPagerResult implements ToZJSONObject {

    private ZSearchResult mSearchResult;
    private int mActualPage;
    private int mRequstedPage;
    private int mLimit;

    ZSearchPagerResult(ZSearchResult result, int requestedPage, int actualPage, int limit) {
        mSearchResult = result;
        mActualPage = actualPage;
        mRequstedPage = requestedPage;
        mLimit = limit;
    }

    public ZSearchResult getResult() { return mSearchResult; }
    public int getActualPage() { return mActualPage; }
    public int getRequestedPage() { return mRequstedPage; }

    public int getOffset() { return mLimit * mActualPage; }

    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject zjo = new ZJSONObject();
        zjo.put("result", mSearchResult);
        zjo.put("requestedPage", mRequstedPage);
        zjo.put("actualPage", mActualPage);
        zjo.put("offset", getOffset());
        return zjo;
    }

    public String toString() {
        return String.format("[ZSearchPagerResult %s]", mActualPage);
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }
}
