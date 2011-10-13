/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

package com.zimbra.soap.type;

import java.util.Arrays;
import javax.xml.bind.annotation.XmlEnum;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.zclient.ZClientException;

@XmlEnum
public enum SearchSortBy {
    // case must match protocol
    dateDesc, dateAsc, subjDesc, subjAsc, nameDesc, nameAsc, durDesc, durAsc, none,
    taskDueAsc, taskDueDesc, taskStatusAsc, taskStatusDesc, taskPercCompletedAsc, taskPercCompletedDesc;

    public static SearchSortBy fromString(String s)
    throws ServiceException {
        try {
            return SearchSortBy.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw ZClientException.CLIENT_ERROR("unknown 'sortBy' key: " + s + ", valid values: " +
                   Arrays.asList(SearchSortBy.values()), null);
        }
    }
}
