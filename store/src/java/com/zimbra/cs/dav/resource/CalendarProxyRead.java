/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.dav.resource;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.dav.DavElements;

/**
 *  principal resource - a group containing the list of principals for calendar users who can act as a read-only proxy.
 */
public class CalendarProxyRead extends AbstractCalendarProxy {

    public static final String CALENDAR_PROXY_READ  = "calendar-proxy-read";

    public CalendarProxyRead(Account acct, String url) throws ServiceException {
        super(acct, url, DavElements.E_CALENDAR_PROXY_READ, true);
    }

    public CalendarProxyRead(String user, String url) throws ServiceException {
        super(user, url + CALENDAR_PROXY_READ, DavElements.E_CALENDAR_PROXY_READ, true);
    }
}
