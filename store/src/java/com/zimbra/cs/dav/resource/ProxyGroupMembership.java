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
 * From RFC for Web Distributed Authoring and Versioning (WebDAV) Access Control Protocol
 * http://tools.ietf.org/html/rfc3744#section-4.4
 * This protected property identifies the groups in which the principal is directly a member.  Note that a server may
 * allow a group to be a member of another group, in which case the DAV:group-membership of those other groups would
 * need to be queried in order to determine the groups in which the principal is indirectly a member.
 *
 * From: http://svn.calendarserver.org/repository/calendarserver/CalendarServer/trunk/doc/Extensions/caldav-proxy.txt
 *
 * If the principal "cyrus" wishes to have the principal "red" act as a calendar user proxy on his behalf and have the
 * ability to change items on his calendar or schedule meetings on his behalf, then ...
 * The DAV:group-membership property on the resource /principals/users/red/ would be:
 *     <DAV:group-membership>
 *       <DAV:href>/principals/users/cyrus/calendar-proxy-write</DAV:href>
 *     </DAV:group-membership>
 */
public class ProxyGroupMembership extends AbstractProxyProperty {
    public ProxyGroupMembership(Account acct) {
        super(DavElements.E_GROUP_MEMBERSHIP, acct);
        setProtected(true);
    }

    @Override
    public Element toElement(DavContext ctxt, Element parent, boolean nameOnly) {
        Element group = super.toElement(ctxt, parent, true);
        if (nameOnly) {
            return group;
        }
        ArrayList<Pair<Mountpoint,ZFolder>> mps = getMountpoints(ctxt);
        for (Pair<Mountpoint,ZFolder> folder : mps) {
            try {
                short rights = ACL.stringToRights(folder.getSecond().getEffectivePerms());
                if ((rights & ACL.RIGHT_WRITE) > 0) {
                    Account owner = Provisioning.getInstance().get(AccountBy.id, folder.getFirst().getOwnerId());
                    if (owner != null) {
                        group.addElement(DavElements.E_HREF).setText(
                                UrlNamespace.getCalendarProxyWriteUrl(account, owner));
                    }
                } else if ((rights & ACL.RIGHT_READ) > 0) {
                    Account owner = Provisioning.getInstance().get(AccountBy.id, folder.getFirst().getOwnerId());
                    if (owner != null) {
                        group.addElement(DavElements.E_HREF).setText(
                                UrlNamespace.getCalendarProxyReadUrl(account, owner));
                    }
                }
            } catch (ServiceException se) {
                ZimbraLog.dav.warn("can't convert rights", se);
            }
        }
        return group;
    }
}
