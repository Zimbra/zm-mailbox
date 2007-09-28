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

public class ZSearchPagerResult {

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

    public String toString() {
        ZSoapSB sb = new ZSoapSB();
        sb.beginStruct();
        sb.addStruct("result", mSearchResult.toString());
        sb.add("requestedPage", mRequstedPage);
        sb.add("actualPage", mActualPage);
        sb.add("offset", getOffset());
        sb.endStruct();
        return sb.toString();
    }
}
