/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.type;

import java.util.Arrays;

import javax.xml.bind.annotation.XmlEnum;

import com.zimbra.common.mailbox.ZimbraSortBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.zclient.ZClientException;

@XmlEnum
public enum SearchSortBy {
    // case must match protocol
    dateDesc(ZimbraSortBy.dateDesc),
    dateAsc(ZimbraSortBy.dateAsc),
    idDesc(ZimbraSortBy.idDesc),
    idAsc(ZimbraSortBy.idAsc),
    subjDesc(ZimbraSortBy.subjDesc),
    subjAsc(ZimbraSortBy.subjAsc),
    nameDesc(ZimbraSortBy.nameDesc),
    nameAsc(ZimbraSortBy.nameAsc),
    durDesc(ZimbraSortBy.durDesc),
    durAsc(ZimbraSortBy.durAsc),
    none(ZimbraSortBy.none),
    taskDueAsc(ZimbraSortBy.taskDueAsc),
    taskDueDesc(ZimbraSortBy.taskDueDesc),
    taskStatusAsc(ZimbraSortBy.taskStatusAsc),
    taskStatusDesc(ZimbraSortBy.taskStatusDesc),
    taskPercCompletedAsc(ZimbraSortBy.taskPercCompletedAsc),
    taskPercCompletedDesc(ZimbraSortBy.taskPercCompletedDesc),
    rcptAsc(ZimbraSortBy.rcptAsc),
    rcptDesc(ZimbraSortBy.rcptDesc),
    readAsc(ZimbraSortBy.readAsc),
    readDesc(ZimbraSortBy.readDesc),
    relevanceAsc(ZimbraSortBy.relevanceAsc),
    relevanceDesc(ZimbraSortBy.relevanceDesc);

    private ZimbraSortBy zsb;

    private SearchSortBy(ZimbraSortBy zsb) {
        this.zsb = zsb;
    }

    public ZimbraSortBy toZimbraSortBy() {
        return zsb;
    }

    public static SearchSortBy fromZimbraSortBy(ZimbraSortBy zsb) {
        if (zsb == null) {
            return null;
        }
        for (SearchSortBy val :SearchSortBy.values()) {
            if (val.zsb == zsb) {
                return val;
            }
        }
        throw new IllegalArgumentException("Unrecognised ZimbraSortBy:" + zsb);
    }

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
