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
package com.zimbra.cs.dav.property;

import java.io.IOException;
import java.util.Collection;

import org.dom4j.Element;
import org.dom4j.QName;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.property.ResourceProperty;
import com.zimbra.cs.dav.resource.CalendarObject;
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
	
	public static ResourceProperty getCalendarHomeSet(String url) {
		return new CalendarHomeSet(url);
	}
	
	public static ResourceProperty getScheduleInboxURL(String url) {
		return new ScheduleInboxURL(url);
	}
	
	public static ResourceProperty getScheduleOutboxURL(String url) {
		return new ScheduleOutboxURL(url);
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
		public CalendarData(CalendarObject calobj) {
			super(DavElements.E_CALENDAR_DATA);
			try {
				setStringValue(calobj.getVcalendar(null));
			} catch (IOException e) {
				ZimbraLog.dav.warn("can't get appt data", e);
			}
		}
	}
	
	private static class CalendarHomeSet extends CalDavProperty {
		public CalendarHomeSet(String url) {
			super(DavElements.E_CALENDAR_HOME_SET);
			Element e = org.dom4j.DocumentHelper.createElement(DavElements.E_HREF);
			e.setText(url);
			mChildren.add(e);
		}
	}
	
	private static class ScheduleInboxURL extends CalDavProperty {
		public ScheduleInboxURL(String url) {
			super(DavElements.E_SCHEDULE_INBOX_URL);
			Element e = org.dom4j.DocumentHelper.createElement(DavElements.E_HREF);
			e.setText(url);
			mChildren.add(e);
		}
	}
	
	private static class ScheduleOutboxURL extends CalDavProperty {
		public ScheduleOutboxURL(String url) {
			super(DavElements.E_SCHEDULE_OUTBOX_URL);
			Element e = org.dom4j.DocumentHelper.createElement(DavElements.E_HREF);
			e.setText(url);
			mChildren.add(e);
		}
	}
	
	private static class CalendarUserAddressSet extends CalDavProperty {
		public CalendarUserAddressSet(Collection<String> addrs) {
			super(DavElements.E_CALENDAR_USER_ADDRESS_SET);
			for (String addr : addrs) {
				Element e = org.dom4j.DocumentHelper.createElement(DavElements.E_HREF);
				e.setText("mailto:"+addr);
				mChildren.add(e);
			}
		}
	}
}
