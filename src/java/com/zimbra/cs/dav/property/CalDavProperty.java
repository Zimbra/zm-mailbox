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
package com.zimbra.cs.dav.property;

import java.io.IOException;
import java.util.Collection;

import org.dom4j.Element;
import org.dom4j.QName;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.property.ResourceProperty;
import com.zimbra.cs.dav.resource.CalendarObject;
import com.zimbra.cs.dav.resource.UrlNamespace;
import com.zimbra.cs.dav.service.DavServlet;
import com.zimbra.cs.mailbox.calendar.ZCalendar;
import com.zimbra.cs.mime.Mime;

/**
 * CALDAV:supported-calendar-component-set - draft-dusseault-caldav section 5.2.3
 * CALDAV:supported-calendar-data - draft-dusseault-caldav section 5.2.4
 * CALDAV:supported-collation-set - draft-dusseault-caldav section 7.5.1
 * CALDAV:calendar-data - draft-dusseault-caldav section 9.6
 * CALDAV:calendar-home-set - draft-dusseault-caldav section 6.2.1
 * 
 * @author jylee
 *
 */
public class CalDavProperty extends ResourceProperty {

	public static ResourceProperty getSupportedCalendarComponentSet() {
		return new SupportedCalendarComponentSet();
	}
	
	public static ResourceProperty getSupportedCalendarData() {
		return new SupportedCalendarData();
	}
	
	public static ResourceProperty getSupportedCollationSet() {
		return new SupportedCollationSet();
	}

	public static ResourceProperty getCalendarData(CalendarObject obj) {
		return new CalendarData(obj);
	}
	
	public static ResourceProperty getCalendarHomeSet(String user) {
		return new CalendarHomeSet(user);
	}
	
	public static ResourceProperty getScheduleInboxURL(String user) {
		return new ScheduleInboxURL(user);
	}
	
	public static ResourceProperty getScheduleOutboxURL(String user) {
		return new ScheduleOutboxURL(user);
	}
	
	public static ResourceProperty getCalendarUserAddressSet(Collection<String> addrs) {
		return new CalendarUserAddressSet(addrs);
	}
	
	protected CalDavProperty(QName name) {
		super(name);
		setProtected(true);
	}

	public enum CalComponent {
		VCALENDAR, VEVENT, VTODO, VJOURNAL, VTIMEZONE, VFREEBUSY
	}

	public static CalComponent getCalComponent(String comp) {
		return CalComponent.valueOf(comp);
	}
	
	private static final CalComponent[] sSUPPORTED_COMPONENTS = {
		CalComponent.VCALENDAR,
		CalComponent.VEVENT,    CalComponent.VTODO, 
		CalComponent.VTIMEZONE, CalComponent.VFREEBUSY
	};
	
	private static class SupportedCalendarComponentSet extends CalDavProperty {
		public SupportedCalendarComponentSet() {
			super(DavElements.E_SUPPORTED_CALENDAR_COMPONENT_SET);
			for (CalComponent comp : sSUPPORTED_COMPONENTS) {
				Element e = org.dom4j.DocumentHelper.createElement(DavElements.E_COMP);
				e.addAttribute(DavElements.P_NAME, comp.name());
				mChildren.add(e);
			}
		}
	}
	
	private static class SupportedCalendarData extends CalDavProperty {
		public SupportedCalendarData() {
			super(DavElements.E_SUPPORTED_CALENDAR_DATA);
			Element e = org.dom4j.DocumentHelper.createElement(DavElements.E_CALENDAR_DATA);
			e.addAttribute(DavElements.P_CONTENT_TYPE, Mime.CT_TEXT_CALENDAR);
			e.addAttribute(DavElements.P_VERSION, ZCalendar.sIcalVersion);
			mChildren.add(e);
		}
	}
	
	private static class SupportedCollationSet extends CalDavProperty {
		public static final String ASCII = "i;ascii-casemap";  // case insensitive
		public static final String OCTET = "i;octet";
		
		public SupportedCollationSet() {
			super(DavElements.E_SUPPORTED_COLLATION_SET);
			Element e = org.dom4j.DocumentHelper.createElement(DavElements.E_SUPPORTED_COLLATION);
			e.setText(ASCII);
			mChildren.add(e);
			e = org.dom4j.DocumentHelper.createElement(DavElements.E_SUPPORTED_COLLATION);
			e.setText(OCTET);
			mChildren.add(e);
		}
	}
	
	private static class CalendarData extends CalDavProperty {
		CalendarObject rs;
		public CalendarData(CalendarObject calobj) {
			super(DavElements.E_CALENDAR_DATA);
			rs = calobj;
		}
		public Element toElement(DavContext ctxt, Element parent, boolean nameOnly) {
			if (getStringValue() == null)
				try {
					setStringValue(rs.getVcalendar(ctxt, null));
				} catch (IOException e) {
					setStringValue("");
					ZimbraLog.dav.warn("can't get appt data", e);
				}
			return super.toElement(ctxt, parent, nameOnly);
		}
	}
	
	private static class CalendarHomeSet extends CalDavProperty {
		public CalendarHomeSet(String user) {
			super(DavElements.E_CALENDAR_HOME_SET);
			Element e = org.dom4j.DocumentHelper.createElement(DavElements.E_HREF);
			e.setText(DavServlet.DAV_PATH + "/" + user + "/");
			mChildren.add(e);
		}
	}
	
	private static class ScheduleInboxURL extends CalDavProperty {
		public ScheduleInboxURL(String user) {
			super(DavElements.E_SCHEDULE_INBOX_URL);
			Element e = org.dom4j.DocumentHelper.createElement(DavElements.E_HREF);
			e.setText(DavServlet.DAV_PATH + "/" + user + "/Inbox/");
			mChildren.add(e);
		}
	}
	
	private static class ScheduleOutboxURL extends CalDavProperty {
		public ScheduleOutboxURL(String user) {
			super(DavElements.E_SCHEDULE_OUTBOX_URL);
			Element e = org.dom4j.DocumentHelper.createElement(DavElements.E_HREF);
			e.setText(DavServlet.DAV_PATH + "/" + user + "/Sent/");
			mChildren.add(e);
		}
	}
	
	private static class CalendarUserAddressSet extends CalDavProperty {
		public CalendarUserAddressSet(Collection<String> addrs) {
			super(DavElements.E_CALENDAR_USER_ADDRESS_SET);
			for (String addr : addrs) {
				Element e = org.dom4j.DocumentHelper.createElement(DavElements.E_HREF);
				if (!addr.startsWith("http:") && !addr.startsWith("mailto:") && !addr.startsWith("/"))
				    addr = "mailto:" + addr;
				e.setText(addr);
				mChildren.add(e);
			}
		}
	}
}
