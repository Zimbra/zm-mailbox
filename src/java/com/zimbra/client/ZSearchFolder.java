/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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

package com.zimbra.client;

import com.google.common.base.Joiner;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.SystemUtil;
import com.zimbra.client.event.ZModifyEvent;
import com.zimbra.client.event.ZModifySearchFolderEvent;
import com.zimbra.soap.mail.type.SearchFolder;
import com.zimbra.soap.type.SearchSortBy;

import org.json.JSONException;

public final class ZSearchFolder extends ZFolder {

    private String query;
    private String types;
    private SearchSortBy sortBy;

    public ZSearchFolder(Element e, ZFolder parent, ZMailbox mailbox) throws ServiceException {
        super(e, parent, mailbox);
        query = e.getAttribute(MailConstants.A_QUERY);
        types = e.getAttribute(MailConstants.A_SEARCH_TYPES, null);
        try {
            sortBy = SearchSortBy.fromString(e.getAttribute(MailConstants.A_SORTBY, SearchSortBy.dateDesc.name()));
        } catch (ServiceException se) {
            sortBy = SearchSortBy.dateDesc;
        }
    }

    public ZSearchFolder(SearchFolder f, ZFolder parent, ZMailbox mailbox) throws ServiceException {
        super(f, parent, mailbox);
        query = f.getQuery();
        types = Joiner.on(',').join(f.getTypes());
        try {
            sortBy = SearchSortBy.fromString(SystemUtil.coalesce(f.getSortBy(), SearchSortBy.dateDesc).toString());
        } catch (ServiceException se) {
            sortBy = SearchSortBy.dateDesc;
        }
    }

    @Override
    public void modifyNotification(ZModifyEvent e) throws ServiceException {
        if (e instanceof ZModifySearchFolderEvent) {
            ZModifySearchFolderEvent sfe = (ZModifySearchFolderEvent) e;
            if (sfe.getId().equals(getId())) {
                query = sfe.getQuery(query);
                types = sfe.getTypes(types);
                sortBy = sfe.getSortBy(sortBy);
                super.modifyNotification(e);
            }
        }
    }

    @Override
    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject jo = super.toZJSONObject();
        jo.put("query", query);
        jo.put("types", types);
        jo.put("sortBy", sortBy.name());
        return jo;
    }

    public String getQuery() {
        return query;
    }

    public SearchSortBy getSortBy() {
        return sortBy;
    }

    public String getTypes() {
        return types;
    }

    @Override
    public String toString() {
        return String.format("[ZSearchFolder %s]", getPath());
    }

    @Override
    public ZSearchContext getSearchContext() {
        ZSearchParams params = new ZSearchParams(query);
        params.setTypes(types);
        params.setSortBy(sortBy);
        return new ZSearchContext(params,getMailbox());
    }

}
