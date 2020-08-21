/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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
package com.zimbra.common.mailbox;

import java.util.Arrays;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.zclient.ZClientException;

public enum ZimbraSortBy {
    dateDesc, dateAsc, subjDesc, subjAsc, nameDesc, nameAsc, durDesc, durAsc, none,
    sizeAsc, sizeDesc, attachAsc, attachDesc, flagAsc, flagDesc, priorityAsc, priorityDesc,
    taskDueAsc, taskDueDesc, taskStatusAsc, taskStatusDesc, taskPercCompletedAsc, taskPercCompletedDesc,
    rcptAsc, rcptDesc, idAsc, idDesc, readAsc, readDesc, relevanceAsc, relevanceDesc, recentlyViewed;

    public static ZimbraSortBy fromString(String s)
    throws ServiceException {
        try {
            return ZimbraSortBy.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw ZClientException.CLIENT_ERROR(String.format("unknown 'sortBy':'%s' - valid values: ", s,
                   Arrays.asList(ZimbraSortBy.values())), null);
        }
    }

}
