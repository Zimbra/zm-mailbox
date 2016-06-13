/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.dav.property;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.dom4j.Element;
import org.dom4j.QName;

import com.zimbra.common.calendar.ICalTimeZone;
import com.zimbra.common.calendar.ZCalendar;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.resource.CalendarObject;
import com.zimbra.cs.dav.resource.Principal;
import com.zimbra.cs.dav.resource.UrlNamespace;
import com.zimbra.cs.dav.service.DavServlet;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.calendar.Util;

/**
 * CALDAV:supported-calendar-component-set  - http://tools.ietf.org/html/rfc4791#section-5.2.3
 * CALDAV:supported-calendar-component-sets - http://tools.ietf.org/html/draft-daboo-caldav-extensions-01#section-4
 * CALDAV:supported-calendar-data - draft-dusseault-caldav section 5.2.4
 * CALDAV:supported-collation-set - draft-dusseault-caldav section 7.5.1
 * CALDAV:calendar-data - draft-dusseault-caldav section 9.6
 * CALDAV:calendar-home-set - draft-dusseault-caldav section 6.2.1
 *
 * @author jylee
 *
 */
public class CalDavProperty extends ResourceProperty {

    public static ResourceProperty getSupportedCalendarComponentSet(MailItem.Type view) {
        return new SupportedCalendarComponentSet(view);
    }

    public static ResourceProperty getSupportedCalendarComponentSets() {
        return new SupportedCalendarComponentSets();
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

    public static ResourceProperty getCalendarTimezone(Account a) {
        return new CalendarTimezone(a);
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

    /* http://tools.ietf.org/html/rfc4791#section-5.2.3
     * Purpose:  Specifies the calendar component types (e.g., VEVENT, VTODO, etc.) that calendar object resources can
     * contain in the calendar collection.
     * Conformance:  This property MAY be defined on any calendar collection.  If defined, it MUST be protected and
     *     SHOULD NOT be returned by a PROPFIND DAV:allprop request (as defined in Section 12.14.1 of [RFC2518]).
     * Description:  The CALDAV:supported-calendar-component-set property is used to specify restrictions on the
     *     calendar component types that calendar object resources may contain in a calendar collection. Any attempt by
     *     the client to store calendar object resources with component types not listed in this property, if it
     *     exists, MUST result in an error, with the CALDAV:supported-calendar-component precondition (Section 5.3.2.1)
     *     being violated.  Since this property is protected, it cannot be changed by clients using a
     *     PROPPATCH request.  However, clients can initialize the value of this property when creating a new calendar
     *     collection with MKCALENDAR.
     *     The empty-element tag <C:comp name="VTIMEZONE"/> MUST only be specified if support for calendar object
     *     resources that only contain VTIMEZONE components is provided or desired.  Support for VTIMEZONE components
     *     in calendar object resources that contain VEVENT or VTODO components is always assumed.  In the absence of
     *     this property, the server MUST accept all component types, and the client can assume that all component
     *     types are accepted.
     * Example:
     *   <C:supported-calendar-component-set
     *       xmlns:C="urn:ietf:params:xml:ns:caldav">
     *     <C:comp name="VEVENT"/>
     *     <C:comp name="VTODO"/>
     *     <C:comp name="VFREEBUSY"/>
     *   </C:supported-calendar-component-set>
     *
     * Earlier versions of Zimbra always included VCALENDAR, VTIMEZONE, VFREEBUSY
     * My reading of the above text implies that none of these should be included - however, added VFREEBUSY back in
     * as that affected whether Mac OSX Calendar offers a checkbox for "Events affect availability" in calendar info.
     */
    private static class SupportedCalendarComponentSet extends CalDavProperty {
        public SupportedCalendarComponentSet(MailItem.Type view) {
            this(view, true);
        }
        public SupportedCalendarComponentSet(MailItem.Type view, boolean includeFreeBusy) {
            super(DavElements.E_SUPPORTED_CALENDAR_COMPONENT_SET);
            super.setAllowSetOnCreate(true);
            ArrayList<CalComponent> comps = new ArrayList<CalComponent>();
            if (view == MailItem.Type.APPOINTMENT) {
                comps.add(CalComponent.VEVENT);
            } else if (view == MailItem.Type.TASK) {
                comps.add(CalComponent.VTODO);
            } else {
                /* assuming inbox or outbox */
                comps.add(CalComponent.VEVENT);
                comps.add(CalComponent.VTODO);
            }
            // Apple Mac OSX Mavericks Calendar's GetInfo for a calendar doesn't offer a checkbox for
            // "Events affect availability" unless VFREEBUSY is present in this list.
            if (includeFreeBusy) {
                comps.add(CalComponent.VFREEBUSY);
            }

            for (CalComponent comp : comps) {
                Element e = org.dom4j.DocumentHelper.createElement(DavElements.E_COMP);
                e.addAttribute(DavElements.P_NAME, comp.name());
                mChildren.add(e);
            }
        }
    }

    /**
     * From : http://tools.ietf.org/html/draft-daboo-caldav-extensions-01#section-4.1
     * Purpose:  Enumerates the sets of component restrictions the server is willing to allow the client to specify
     * in MKCALENDAR or extended MKCOL requests.
     * Description:  If servers apply restrictions on the allowed calendar component sets used when creating a
     * calendar, then those servers SHOULD advertise this property on each calendar home collection within which the
     * restrictions apply.  In the absence of this property, clients cannot assume anything about whether the
     * server will enforce a set of restrictions or not - in that case clients need to handle the server rejecting
     * certain combinations of restricted component sets.  If this property is present, but contains no child XML
     * elements, then clients can assume that the server imposes no restrictions on the combinations of component
     * types it is willing to accept.  If present, each CALDAV:supported-calendar-component-set element represents a
     * valid restriction the client can use in an MKCALENDAR or extended MKCOL request when creating a calendar.
     *
     * <C:supported-calendar-component-sets xmlns:C="urn:ietf:params:xml:ns:caldav">
     * <C:supported-calendar-component-set>
     *   <C:comp name="VEVENT" />
     * </C:supported-calendar-component-set>
     * <C:supported-calendar-component-set>
     *   <C:comp name="VTODO" />
     * </C:supported-calendar-component-set>
     * </C:supported-calendar-component-sets>
     */
    private static class SupportedCalendarComponentSets extends CalDavProperty {
        public SupportedCalendarComponentSets() {
            super(DavElements.E_SUPPORTED_CALENDAR_COMPONENT_SETS);
            mChildren.add(new SupportedCalendarComponentSet(MailItem.Type.APPOINTMENT, false).toElement(false));
            mChildren.add(new SupportedCalendarComponentSet(MailItem.Type.TASK, false).toElement(false));
        }
    }

    private static class SupportedCalendarData extends CalDavProperty {
        public SupportedCalendarData() {
            super(DavElements.E_SUPPORTED_CALENDAR_DATA);
            Element e = org.dom4j.DocumentHelper.createElement(DavElements.E_CALENDAR_DATA);
            e.addAttribute(DavElements.P_CONTENT_TYPE, MimeConstants.CT_TEXT_CALENDAR);
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

        @Override
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
                mChildren.add(createHref(DavServlet.DAV_PATH + "/" + user + "/"));
            } else {
                for (String calHome : homeSets) {
                    mChildren.add(createHref(DavServlet.DAV_PATH + "/" + user + "/" + calHome + "/"));
                }
            }
        }
    }

    private static class ScheduleInboxURL extends CalDavProperty {
        private final String mUser;
        public ScheduleInboxURL(String user) {
            super(DavElements.E_SCHEDULE_INBOX_URL);
            mUser = user;
        }

        @Override
        public Element toElement(DavContext ctxt, Element parent, boolean nameOnly) {
            Element inboxUrl = super.toElement(ctxt, parent, nameOnly);
            String authUser = ctxt.getAuthAccount().getName();
            try {
                authUser = Principal.getOwner(ctxt.getAuthAccount(), ctxt.getUri());
            } catch (ServiceException se) {
            }
            inboxUrl.add(createHref(UrlNamespace.getSchedulingInboxUrl(authUser, mUser)));
            return inboxUrl;
        }
    }

    private static class ScheduleOutboxURL extends CalDavProperty {
        private final String mUser;
        public ScheduleOutboxURL(String user) {
            super(DavElements.E_SCHEDULE_OUTBOX_URL);
            mUser = user;
        }

        @Override
        public Element toElement(DavContext ctxt, Element parent, boolean nameOnly) {
            Element outboxUrl = super.toElement(ctxt, parent, nameOnly);
            String authUser = ctxt.getAuthAccount().getName();
            try {
                authUser = Principal.getOwner(ctxt.getAuthAccount(), ctxt.getUri());
            } catch (ServiceException se) {
            }
            outboxUrl.add(createHref(UrlNamespace.getSchedulingOutboxUrl(authUser, mUser)));
            return outboxUrl;
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
                if (f.getDefaultView() != MailItem.Type.APPOINTMENT && f.getDefaultView() != MailItem.Type.TASK) {
                    continue;
                }
                if (parentIds.isEmpty() && f.getFolderId() != Mailbox.ID_FOLDER_USER_ROOT) {
                    continue;
                }
                if (!parentIds.isEmpty() && !parentIds.contains(f.getFolderId())) {
                    continue;
                }
                if ((f.getFlagBitmask() & Flag.BITMASK_EXCLUDE_FREEBUSY) != 0) {
                    continue;
                }
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

    private static class CalendarTimezone extends CalDavProperty {
        private final Account account;
        public CalendarTimezone(Account a) {
            super(DavElements.E_CALENDAR_TIMEZONE);
            account = a;
        }

        @Override
        public Element toElement(DavContext ctxt, Element parent, boolean nameOnly) {
            Element timezone = super.toElement(ctxt, parent, nameOnly);
            ICalTimeZone tz = Util.getAccountTimeZone(account);
            CharArrayWriter wr = new CharArrayWriter();
            try {
                tz.newToVTimeZone().toICalendar(wr, true);
                wr.flush();
                timezone.addCDATA(new String(wr.toCharArray()));
                wr.close();
            } catch (IOException e) {
            }
            return timezone;
        }
    }
}
