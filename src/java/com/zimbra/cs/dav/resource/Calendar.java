/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Locale;

import org.dom4j.Element;
import org.dom4j.QName;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol.Compliance;
import com.zimbra.cs.dav.property.Acl;
import com.zimbra.cs.dav.property.CalDavProperty;
import com.zimbra.cs.dav.property.ResourceProperty;
import com.zimbra.cs.mailbox.calendar.ZCalendar;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.L10nUtil;
import com.zimbra.cs.util.L10nUtil.MsgKey;

public class Calendar extends DavResource {

	public Calendar(String path, Account acct) throws DavException, ServiceException {
		super(path, acct);
		mDavCompliance.add(Compliance.one);
		mDavCompliance.add(Compliance.two);
		mDavCompliance.add(Compliance.access_control);
		mDavCompliance.add(Compliance.calendar_access);

		ResourceProperty rtype = getProperty(DavElements.E_RESOURCETYPE);
		rtype.addChild(DavElements.E_CALENDAR);
		rtype.addChild(DavElements.E_PRINCIPAL);
		
		ResourceProperty desc = new ResourceProperty(DavElements.E_CALENDAR_DESCRIPTION);
		Locale lc = acct.getLocale();
		desc.setMessageLocale(lc);
		desc.setStringValue(L10nUtil.getMessage(MsgKey.caldavCalendarDescription, lc));
		addProperty(desc);
		addProperty(CalDavProperty.getSupportedCalendarComponentSet());
		addProperty(CalDavProperty.getSupportedCalendarData());
		
		setProperty(DavElements.E_DISPLAYNAME, acct.getAttr(Provisioning.A_displayName)+"'s calendar");
		setProperty(DavElements.E_PRINCIPAL_URL, UrlNamespace.getResourceUrl(this), true);
		setProperty(DavElements.E_ALTERNATE_URI_SET, null, true);
		setProperty(DavElements.E_GROUP_MEMBER_SET, null, true);
		setProperty(DavElements.E_GROUP_MEMBERSHIP, null, true);

		addProperties(Acl.getAclProperties(this, null));
	
		// remaining recommented attributes: calendar-timezone, max-resource-size,
		// min-date-time, max-date-time, max-instances, max-attendees-per-instance,
		//
	}
	
	@Override
	public InputStream getContent() throws IOException, DavException {
		return null;
	}

	@Override
	public boolean isCollection() {
		return true;
	}

	public Element addPropertyElement(DavContext ctxt, Element parent, QName propName, boolean putValue) {
		Element e = super.addPropertyElement(ctxt, parent, propName, putValue);

		if (e != null)
			return e;
		
		return null;
	}
	
	public String getCalendar(ZCalendar cal) throws IOException {
        //ByteArrayOutputStream buf = new ByteArrayOutputStream();
        //OutputStreamWriter wout = new OutputStreamWriter(buf, Mime.P_CHARSET_UTF8);
        //cal.toICalendar(wout);
		return cal.toString();
	}
}
