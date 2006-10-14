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

import java.io.IOException;
import java.io.InputStream;

import org.dom4j.Element;
import org.dom4j.QName;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol.Compliance;
import com.zimbra.cs.mailbox.calendar.ZCalendar;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.ServiceException;

public class Calendar extends DavResource {

	private static final String[] sSUPPORTED_COMPONENTS = {
		"VEVENT", "VTODO", "VTIMEZONE", "VFREEBUSY"
	};

	private String mDisplayName;
	
	public Calendar(String path, Account acct) throws ServiceException {
		super(path, acct);
		mDavCompliance.add(Compliance.one);
		mDavCompliance.add(Compliance.two);
		mDavCompliance.add(Compliance.access_control);
		mDavCompliance.add(Compliance.calendar_access);

		mDisplayName = acct.getAttr(Provisioning.A_displayName)+"'s calendar";
		

		// XXX add calendar-timezone, max-resource-size,
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

	public Element addPropertyElement(Element parent, QName propName, boolean putValue) {
		Element e = super.addPropertyElement(parent, propName, putValue);

		if (e != null)
			return e;
		
		if (propName.equals(DavElements.P_CALENDAR_DESCRIPTION))
			return addCalendarDescription(parent, putValue);
		else if (propName.equals(DavElements.P_SUPPORTED_CALENDAR_COMPONENT_SET))
			return addCalendarComponentSet(parent, putValue);
		else if (propName.equals(DavElements.P_SUPPORTED_CALENDAR_DATA))
			return addSupportedCalendarData(parent, putValue);
		return null;
	}
	
	private String getCalendarDescription() {
		return mDisplayName;
	}
	
	private Element addCalendarDescription(Element parent, boolean nameOnly) {
		Element desc = parent.addElement(DavElements.E_CALENDAR_DESCRIPTION);
		if (nameOnly)
			return desc;
		// XXX need to provide the correct language
		desc.addAttribute(DavElements.E_LANG.getQualifiedName(), "en-us");
		desc.setText(getCalendarDescription());
		return desc;
	}

	private Element addCalendarComponentSet(Element parent, boolean nameOnly) {
		Element compSet = parent.addElement(DavElements.E_SUPPORTED_CALENDAR_COMPONENT_SET);
		if (nameOnly)
			return compSet;
		for (String comp : sSUPPORTED_COMPONENTS)
			compSet.addElement(DavElements.E_COMP).addAttribute(DavElements.P_NAME, comp);
		return compSet;
	}
	
	private Element addSupportedCalendarData(Element parent, boolean nameOnly) {
		Element calData = parent.addElement(DavElements.E_SUPPORTED_CALENDAR_DATA);
		if (nameOnly)
			return calData;
		Element e = calData.addElement(DavElements.E_CALENDAR_DATA);
		e.addAttribute(DavElements.P_CONTENT_TYPE, Mime.CT_TEXT_CALENDAR);
		e.addAttribute(DavElements.P_VERSION, ZCalendar.sIcalVersion);
		return calData;
	}
	
	public String getCalendar(ZCalendar cal) {
		return cal.toString();
	}
	
	public Element addResourceTypeElement(Element parent, boolean nameOnly) {
		Element el = super.addResourceTypeElement(parent, nameOnly);
		if (nameOnly)
			return el;
		el.addElement(DavElements.E_CALENDAR);
		el.addElement(DavElements.E_PRINCIPAL);
		return el;
	}
}
