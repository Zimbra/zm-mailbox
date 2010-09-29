/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.SystemUtil;
import com.zimbra.cs.zclient.ZMailbox.SearchSortBy;
import com.zimbra.cs.zclient.event.ZModifyEvent;
import com.zimbra.cs.zclient.event.ZModifySearchFolderEvent;
import com.zimbra.soap.mail.type.SearchFolder;

import org.json.JSONException;

public class ZSearchFolder extends ZFolder {


    private String mQuery;
    private String mTypes;
    private SearchSortBy mSortBy;
    
    public ZSearchFolder(Element e, ZFolder parent, ZMailbox mailbox) throws ServiceException {
        super(e, parent, mailbox);
        mQuery = e.getAttribute(MailConstants.A_QUERY);
        mTypes = e.getAttribute(MailConstants.A_SEARCH_TYPES, null);
        try {
            mSortBy = SearchSortBy.fromString(e.getAttribute(MailConstants.A_SORTBY, SearchSortBy.dateDesc.name()));
        } catch (ServiceException se) {
            mSortBy = SearchSortBy.dateDesc;
        }
    }

    public ZSearchFolder(SearchFolder f, ZFolder parent, ZMailbox mailbox) throws ServiceException {
        super(f, parent, mailbox);
        mQuery = f.getQuery();
        mTypes = f.getTypes();
        try {
            mSortBy = SearchSortBy.fromString(
                SystemUtil.getValue(f.getSortBy(), SearchFolder.SortBy.dateDesc).toString());
        } catch (ServiceException se) {
            mSortBy = SearchSortBy.dateDesc;
        }
    }
    
    public void modifyNotification(ZModifyEvent e) throws ServiceException {
        if (e instanceof ZModifySearchFolderEvent) {
            ZModifySearchFolderEvent sfe = (ZModifySearchFolderEvent) e;
            if (sfe.getId().equals(getId())) {
                mQuery = sfe.getQuery(mQuery);
                mTypes = sfe.getTypes(mTypes);
                mSortBy = sfe.getSortBy(mSortBy);
                super.modifyNotification(e);
            }
        }
    }

    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject jo = super.toZJSONObject();
        jo.put("query", mQuery);
        jo.put("types", mTypes);
        jo.put("sortBy", mSortBy.name());
        return jo;
    }

    public String getQuery() {
        return mQuery;
    }

    public SearchSortBy getSortBy() {
        return mSortBy;
    }

    public String getTypes() {
        return mTypes;
    }

    public String toString() {
        return String.format("[ZSearchFolder %s]", getPath());
    }

    public ZSearchContext getSearchContext() {
        ZSearchParams params = new ZSearchParams(mQuery);
        params.setTypes(mTypes);
        params.setSortBy(mSortBy);
        return new ZSearchContext(params,getMailbox());
    }
    
}
