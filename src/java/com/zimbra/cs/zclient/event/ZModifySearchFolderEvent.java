/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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

package com.zimbra.cs.zclient.event;

import com.zimbra.common.soap.Element;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.zclient.ZSoapSB;
import com.zimbra.cs.zclient.ZMailbox.SearchSortBy;

public class ZModifySearchFolderEvent extends ZModifyFolderEvent {


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
    
    public String toString() {
        try {
            ZSoapSB sb = new ZSoapSB();
            sb.beginStruct();
            toStringCommon(sb);
            if (getQuery(null) != null) sb.add("query", getQuery(null));
            if (getTypes(null) != null) sb.add("types", getTypes(null));
            if (getSortBy(null) != null) sb.add("sortBy", getSortBy(null).name());
            sb.endStruct();
            return sb.toString();
        } catch (ServiceException se) {
            return "";
        }
    }
}

