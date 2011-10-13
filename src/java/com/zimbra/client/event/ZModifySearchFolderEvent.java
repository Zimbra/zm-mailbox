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

package com.zimbra.client.event;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.client.ToZJSONObject;
import com.zimbra.client.ZJSONObject;
import com.zimbra.soap.type.SearchSortBy;
import org.json.JSONException;

public class ZModifySearchFolderEvent extends ZModifyFolderEvent implements ToZJSONObject {


    public ZModifySearchFolderEvent(Element e) throws ServiceException {
        super(e);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new name or defaultValue if unchanged
     */
    public String getQuery(String defaultValue) {
        return mFolderEl.getAttribute(MailConstants.A_QUERY, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new name or defaultValue if unchanged
     */
    public String getTypes(String defaultValue) {
        return mFolderEl.getAttribute(MailConstants.A_SEARCH_TYPES, defaultValue);
    }

    /**
     * @param defaultValue value to return if unchanged
     * @return new name or defaultValue if unchanged
     */
    public SearchSortBy getSortBy(SearchSortBy defaultValue) {
        try {
            String newSort = mFolderEl.getAttribute(MailConstants.A_SORTBY, null);
            return newSort == null ? defaultValue : SearchSortBy.fromString(newSort);
        } catch (ServiceException se) {
            return defaultValue;
        }
    }
    
    @Override
    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject zjo = super.toZJSONObject();
        if (getQuery(null) != null) zjo.put("query", getQuery(null));
        if (getTypes(null) != null) zjo.put("types", getTypes(null));
        if (getSortBy(null) != null) zjo.put("sortBy", getSortBy(null).name());
        return zjo;
    }
}

