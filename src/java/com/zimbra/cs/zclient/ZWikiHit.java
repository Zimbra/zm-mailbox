/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011 Zimbra, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.zclient.event.ZModifyEvent;
import org.json.JSONException;

public class ZWikiHit implements ZSearchHit {

    private ZDocument mDoc;
    private String mId;
    private String mSortField;

    public ZWikiHit(Element e) throws ServiceException {
        mId = e.getAttribute(MailConstants.A_ID);
        mSortField = e.getAttribute(MailConstants.A_SORT_FIELD, null);
        mDoc = new ZDocument(e);
    }

    public ZDocument getDocument() {
        return mDoc;
    }

    @Override
    public String getId() {
        return mId;
    }

    @Override
    public String getSortField() {
        return mSortField;
    }

    @Override
    public void modifyNotification(ZModifyEvent event) throws ServiceException {
        // TODO Auto-generated method stub
    }

    @Override
    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject zjo = new ZJSONObject();
        zjo.put("id", mId);
        zjo.put("sortField", mSortField);
        zjo.put("document", mDoc);
        return zjo;
    }

    @Override
    public String toString() {
        return String.format("[ZWikiHit %s]", mId);
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }
}
