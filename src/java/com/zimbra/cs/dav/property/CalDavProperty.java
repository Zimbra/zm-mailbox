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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.dom4j.Element;
import org.dom4j.QName;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.property.ResourceProperty;
import com.zimbra.cs.dav.resource.CalendarObject;
import com.zimbra.cs.dav.resource.Principal;
import com.zimbra.cs.dav.service.DavServlet;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
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

	public static ResourceProperty getSupportedCalendarComponentSet(byte view) {
		return new SupportedCalendarComponentSet(view);
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
	
	public static ResourceProperty getCalendarFreeBusySet(String user, Collection<Folder> folders) {
		return new CalendarFreeBusySet(user, folders);
	}
	
	public static ResourceProperty getCalendarUserType(Principal p) {
		return new CalendarUserType(p);
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
		CalComponent.VTIMEZONE, CalComponent.VFREEBUSY
	};
	
	private static class SupportedCalendarComponentSet extends CalDavProperty {
		public SupportedCalendarComponentSet(byte view) {
			super(DavElements.E_SUPPORTED_CALENDAR_COMPONENT_SET);
			ArrayList<CalComponent> comps = new ArrayList<CalComponent>();
			Collections.addAll(comps, sSUPPORTED_COMPONENTS);
			Provisioning prov = Provisioning.getInstance();
			boolean useDistinctCollectionType = false;
			try {
				useDistinctCollectionType = prov.getConfig().getBooleanAttr(Provisioning.A_zimbraCalendarCalDavUseDistinctAppointmentAndToDoCollection, false);
			} catch (ServiceException se) {
				ZimbraLog.dav.warn("can't get zimbraCalendarCalDavUseDistinctAppointmentAndToDoCollection in globalConfig", se);
			}
			if (!useDistinctCollectionType) {
				comps.add(CalComponent.VEVENT);
				comps.add(CalComponent.VTODO);
			} else if (view == MailItem.TYPE_APPOINTMENT) {
				comps.add(CalComponent.VEVENT);
			} else if (view == MailItem.TYPE_TASK) {
				comps.add(CalComponent.VTODO);
			}
			
			for (CalComponent comp : comps) {
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
		
		public SupportedCollationSet() {
			super(DavElements.E_SUPPORTED_COLLATION_SET);
			Element e = org.dom4j.DocumentHelper.createElement(DavElements.E_SUPPORTED_COLLATION);
			e.setText(DavElements.ASCII);
			mChildren.add(e);
			e = org.dom4j.DocumentHelper.createElement(DavElements.E_SUPPORTED_COLLATION);
			e.setText(DavElements.OCTET);
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
                } catch (DavException e) {
                    setStringValue("");
                    ZimbraLog.dav.warn("can't get appt data", e);
				}
			return super.toElement(ctxt, parent, nameOnly);
		}
	}
	
	private static class CalendarHomeSet extends CalDavProperty {
		public CalendarHomeSet(String user) {
			super(DavElements.E_CALENDAR_HOME_SET);
			String[] homeSets = null;
			try {
				homeSets = Provisioning.getInstance().getConfig().getMultiAttr(Provisioning.A_zimbraCalendarCalDavAlternateCalendarHomeSet);
			} catch (ServiceException se) {
				
			}
			if (homeSets == null || homeSets.length == 0) {
				// iCal doesn't recognize properly encoded calendar-home-set
				// see bug 37508
				//mChildren.add(createHref(DavServlet.DAV_PATH + "/" + user + "/"));
				Element e = org.dom4j.DocumentHelper.createElement(DavElements.E_HREF);
				e.setText(DavServlet.DAV_PATH + "/" + user + "/");
				mChildren.add(e);
			} else {
				for (String calHome : homeSets) {
					//mChildren.add(createHref(DavServlet.DAV_PATH + "/" + user + "/" + calHome + "/"));
					Element e = org.dom4j.DocumentHelper.createElement(DavElements.E_HREF);
					e.setText(DavServlet.DAV_PATH + "/" + user + "/" + calHome + "/");
					mChildren.add(e);
				}
			}
		}
	}
	
	private static class ScheduleInboxURL extends CalDavProperty {
		public ScheduleInboxURL(String user) {
			super(DavElements.E_SCHEDULE_INBOX_URL);
			// iCal doesn't recognize properly encoded calendar-home-set
			// see bug 37508
			//mChildren.add(createHref(DavServlet.DAV_PATH + "/" + user + "/Inbox/"));
			Element e = org.dom4j.DocumentHelper.createElement(DavElements.E_HREF);
			e.setText(DavServlet.DAV_PATH + "/" + user + "/Inbox/");
			mChildren.add(e);
		}
	}
	
	private static class ScheduleOutboxURL extends CalDavProperty {
		public ScheduleOutboxURL(String user) {
			super(DavElements.E_SCHEDULE_OUTBOX_URL);
			// iCal doesn't recognize properly encoded calendar-home-set
			// see bug 37508
			//mChildren.add(createHref(DavServlet.DAV_PATH + "/" + user + "/Sent/"));
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
	
	private static class CalendarFreeBusySet extends CalDavProperty {
		public CalendarFreeBusySet(String user, Collection<Folder> folders) {
			super(DavElements.E_CALENDAR_FREE_BUSY_SET);
			setProtected(false);
			setVisible(false);
			
			ArrayList<Integer> parentIds = new ArrayList<Integer>();
			try {
				String[] homeSets = Provisioning.getInstance().getConfig().getMultiAttr(Provisioning.A_zimbraCalendarCalDavAlternateCalendarHomeSet);
				if (homeSets != null) {
					for (Folder f : folders) {
						String name = f.getName();
						for (String homeset : homeSets) {
							if (name.compareTo(homeset) == 0) {
								parentIds.add(f.getId());
							}
						}
					}
				}
			} catch (ServiceException se) {
				ZimbraLog.dav.warn("can't generate calendar home set", se);
			}
			for (Folder f : folders) {
				if (f.getDefaultView() != MailItem.TYPE_APPOINTMENT && f.getDefaultView() != MailItem.TYPE_TASK)
					continue;
				if (parentIds.isEmpty() && f.getFolderId() != Mailbox.ID_FOLDER_USER_ROOT)
					continue;
				if (!parentIds.isEmpty() && !parentIds.contains(f.getFolderId()))
					continue;
	            if ((f.getFlagBitmask() & Flag.BITMASK_EXCLUDE_FREEBUSY) != 0)
	                continue;
				mChildren.add(createHref(DavServlet.DAV_PATH + "/" + user + f.getPath() + "/"));
			}
		}
	}
	
	/*
	 * rfc 2445 section 4.2.3
	 */
	private static final String INDIVIDUAL = "INDIVIDUAL";
	//private static final String GROUP = "GROUP";
	private static final String RESOURCE = "RESOURCE";
	private static final String ROOM = "ROOM";
	private static final String UNKNOWN = "UNKNOWN";
	
	private static class CalendarUserType extends CalDavProperty {
		public CalendarUserType(Principal p) {
			super(DavElements.E_CALENDAR_USER_TYPE);
			String resType = p.getAccount().getAttr(Provisioning.A_zimbraCalResType);
			if (resType == null)
				setStringValue(INDIVIDUAL);
			else if (resType.compareTo("Equipment") == 0)
				setStringValue(RESOURCE);
			else if (resType.compareTo("Location") == 0)
				setStringValue(ROOM);
			else
				setStringValue(UNKNOWN);
		}
	}
	
}
