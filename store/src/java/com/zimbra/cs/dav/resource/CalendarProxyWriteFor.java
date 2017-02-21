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

import java.util.ArrayList;
import java.util.HashSet;

import org.dom4j.Element;

import com.zimbra.client.ZFolder;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Mountpoint;

/**
 * From: http://svn.calendarserver.org/repository/calendarserver/CalendarServer/trunk/doc/Extensions/caldav-proxy.txt
 * 5.3.2.  CS:calendar-proxy-write-for Property
 *  Name:  calendar-proxy-write-for Namespace:  http://calendarserver.org/ns/
 *
 * Purpose:  Lists principals for whom the current principal is a read-write proxy for.
 * Protected:  This property MUST be protected.
 * Description:  This property allows a client to quickly determine the principal for whom the current principal is a
 * read-write proxy for.  The server MUST account for any group memberships of the current principal that are either
 * direct or indirect members of a proxy group. e.g., if principal "A" assigns a group "G" as a read-write proxy, and
 * principal "B" is a member of group "G", then principal "B" will see principal "A" listed in the
 * CS:calendar-proxy-write-for property on their principal resource.
 * Definition:
 *     <!ELEMENT calendar-proxy-write-for (DAV:href*)>
 */
public class CalendarProxyWriteFor extends AbstractProxyProperty {
    public CalendarProxyWriteFor(Account acct) {
        super(DavElements.E_CALENDAR_PROXY_WRITE_FOR, acct);
    }
    @Override
    public Element toElement(DavContext ctxt, Element parent, boolean nameOnly) {
        Element proxy = super.toElement(ctxt, parent, true);
        if (nameOnly) {
            return proxy;
        }
        ArrayList<Pair<Mountpoint,ZFolder>> mps = getMountpoints(ctxt);
        HashSet<Account> proxies = new HashSet<Account>();
        for (Pair<Mountpoint,ZFolder> folder : mps) {
            try {
                short rights = ACL.stringToRights(folder.getSecond().getEffectivePerms());
                if ((rights & ACL.RIGHT_WRITE) > 0) {
                    Account owner = Provisioning.getInstance().get(AccountBy.id, folder.getFirst().getOwnerId());
                    if (owner == null) {
                        continue;
                    }
                    if (!proxies.contains(owner)) {
                        proxy.addElement(DavElements.E_HREF).setText(UrlNamespace.getPrincipalUrl(account, owner));
                        proxies.add(owner);
                    }
                }
            } catch (ServiceException se) {
                ZimbraLog.dav.warn("can't convert rights", se);
            }
        }
        return proxy;
    }
}
