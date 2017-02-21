/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.client.event.ZModifyEvent;
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
