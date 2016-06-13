/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.Locale;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.L10nUtil.MsgKey;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.property.CalDavProperty;
import com.zimbra.cs.dav.property.ResourceProperty;
import com.zimbra.cs.mailbox.Mountpoint;

public class RemoteCalendarCollection extends RemoteCollection {

    public RemoteCalendarCollection(DavContext ctxt, Mountpoint mp) throws DavException, ServiceException {
        super(ctxt, mp);
		Account acct = mp.getAccount();

		addResourceType(DavElements.E_CALENDAR);

		Locale lc = acct.getLocale();
		String description = L10nUtil.getMessage(MsgKey.caldavCalendarDescription, lc, acct.getAttr(Provisioning.A_displayName), mp.getName());
		ResourceProperty desc = new ResourceProperty(DavElements.E_CALENDAR_DESCRIPTION);
		desc.setMessageLocale(lc);
		desc.setStringValue(description);
		desc.setVisible(false);
		addProperty(desc);
		addProperty(CalDavProperty.getSupportedCalendarComponentSet(mp.getDefaultView()));
		addProperty(CalDavProperty.getSupportedCalendarData());
		addProperty(CalDavProperty.getSupportedCollationSet());
		
        addProperty(getIcalColorProperty());
		setProperty(DavElements.E_ALTERNATE_URI_SET, null, true);
		setProperty(DavElements.E_GROUP_MEMBER_SET, null, true);
		setProperty(DavElements.E_GROUP_MEMBERSHIP, null, true);
    }
    
    public short getRights() {
    	return mRights;
    }
}
