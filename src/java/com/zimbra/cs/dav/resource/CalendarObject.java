/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.resource;

import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.caldav.Filter;
import com.zimbra.cs.dav.caldav.TimeRange;
import com.zimbra.cs.dav.property.CalDavProperty;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.TimeZoneMap;
import com.zimbra.cs.mailbox.calendar.ZAttendee;
import com.zimbra.cs.mailbox.calendar.ZCalendar;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZComponent;

/**
 * CalendarObject is a single instance of iCalendar (RFC 2445) object, such as
 * VEVENT or VTODO.
 * 
 * @author jylee
 *
 */
public interface CalendarObject {

    public static final String CAL_EXTENSION = ".ics";

    public String getUid();
    public boolean match(Filter filter);
    public String getVcalendar(DavContext ctxt, Filter filter) throws IOException, DavException;

    public static class Vcalendar {

        private static Invite getFixedUpCopy(Invite inv, Account acct) throws ServiceException {
            ZAttendee attendee = inv.getMatchingAttendee(acct);
            if (attendee != null && attendee.hasRsvp() && attendee.getRsvp() && IcalXmlStrMap.PARTSTAT_NEEDS_ACTION.equals(attendee.getPartStat())) {
                Invite copy = inv.newCopy();
                ZCalendar.ZProperty prop = new ZCalendar.ZProperty("X-APPLE-NEEDS-REPLY");
                prop.setValue("TRUE");
                copy.addXProp(prop);
                return copy;
            } else {
                return inv;
            }
        }

        public static String toString(Account acct, Invite inv, Filter filter, boolean allowPrivateAccess) throws IOException {
            CharArrayWriter wr = null;
            try {
                wr = new CharArrayWriter();
                Invite fixedInv = getFixedUpCopy(inv, acct);
                ZComponent comp = fixedInv.newToVComponent(false, allowPrivateAccess);
                if (filter == null || filter.match(comp))
                    comp.toICalendar(wr, true);
                wr.flush();
                return wr.toString();
            } catch (ServiceException se) {
                ZimbraLog.dav.warn("cannot convert to iCalendar", se);
                return "";
            } finally {
                if (wr != null)
                    wr.close();
            }
        }

        public static String toString(Account acct, Invite[] invites, Filter filter, boolean allowPrivateAccess) throws IOException {
            CharArrayWriter wr = null;
            try {
                wr = new CharArrayWriter();
                if (!LC.calendar_apple_ical_compatible_canceled_instances.booleanValue()) {
                    for (Invite inv : invites) {
                        Invite fixedInv = getFixedUpCopy(inv, acct);
                        ZComponent comp = fixedInv.newToVComponent(false, allowPrivateAccess);
                        if (filter == null || filter.match(comp))
                            comp.toICalendar(wr, true);
                    }
                } else {
                    Invite[] fixedInvs = new Invite[invites.length];
                    for (int i = 0; i < invites.length; ++i) {
                        fixedInvs[i] = getFixedUpCopy(invites[i], acct);
                    }
                    boolean appleICalExdateHack = LC.calendar_apple_ical_compatible_canceled_instances.booleanValue();
                    ZComponent[] vcomps = Invite.toVComponents(fixedInvs, allowPrivateAccess, false, appleICalExdateHack);
                    if (vcomps != null) {
                        for (ZComponent vcomp : vcomps) {
                            if (filter == null || filter.match(vcomp))
                                vcomp.toICalendar(wr, true);
                        }
                    }
                }
                wr.flush();
                return wr.toString();
            } catch (ServiceException se) {
                ZimbraLog.dav.warn("cannot convert to iCalendar", se);
                return "";
            } finally {
                if (wr != null)
                    wr.close();
            }
        }
    }

    public static class CalendarPath {
        public static String generate(DavContext ctxt, String itemPath, String uid, int extra) {
            if (ctxt != null) {
                if (ctxt.getCollectionPath() != null)
                    itemPath = ctxt.getCollectionPath();
                else if (ctxt.getPathInfo() != null)
                    itemPath += ctxt.getPathInfo();
            }
            // escape uid
            StringBuilder path = new StringBuilder();
            path.append(itemPath);
            if (path.charAt(path.length()-1) != '/')
                path.append("/");
            path.append(uid);
            if (extra >= 0)
                path.append(",").append(extra);
            path.append(CAL_EXTENSION);
            return path.toString();
        }
    }
    public static class ScheduleMessage extends MailItemResource implements CalendarObject {
        public ScheduleMessage(DavContext ctxt, String path, String owner, Invite inv, Message msg) throws ServiceException {
            super(ctxt, path, msg);
            mInvite = inv;
            addProperty(CalDavProperty.getCalendarData(this));
        }
        public String getUid() {
            return mInvite.getUid();
        }
        public boolean match(Filter filter) {
            return true;
        }
        public String getVcalendar(DavContext ctxt, Filter filter) throws IOException, DavException {
            StringBuilder buf = new StringBuilder();
            buf.append("BEGIN:VCALENDAR\r\n");
            buf.append("VERSION:").append(ZCalendar.sIcalVersion).append("\r\n");
            buf.append("PRODID:").append(ZCalendar.sZimbraProdID).append("\r\n");
            buf.append("METHOD:").append(mInvite.getMethod()).append("\r\n");
            Account acct = ctxt.getAuthAccount();
            boolean allowPrivateAccess = false;
            try {
                Mailbox mbox = getMailbox(ctxt);
                OperationContext octxt = ctxt.getOperationContext();
                Folder folder = mbox.getFolderById(octxt, mFolderId);
                allowPrivateAccess = CalendarItem.allowPrivateAccess(
                        folder, ctxt.getAuthAccount(), octxt.isUsingAdminPrivileges());
            } catch (ServiceException se) {
                ZimbraLog.dav.warn("cannot determine private access status", se);
            }
            buf.append(Vcalendar.toString(acct, mInvite, filter, allowPrivateAccess));
            buf.append("END:VCALENDAR\r\n");
            return buf.toString();
        }
        @Override public InputStream getContent(DavContext ctxt) throws IOException, DavException {
            return new ByteArrayInputStream(getVcalendar(ctxt, null).getBytes("UTF-8"));
        }
        @Override public boolean isCollection() {
            return false;
        }
        // work around for bug 45241
        // mark appointment invite email read instead of moving to trash.
        @Override public void delete(DavContext ctxt) throws DavException {
            try {
                Mailbox mbox = getMailbox(ctxt);
                mbox.alterTag(ctxt.getOperationContext(), mId, MailItem.TYPE_MESSAGE, Flag.ID_FLAG_UNREAD, false);
            } catch (ServiceException se) {
                int resCode = se instanceof MailServiceException.NoSuchItemException ?
                        HttpServletResponse.SC_NOT_FOUND : HttpServletResponse.SC_FORBIDDEN;
                throw new DavException("cannot delete item", resCode, se);
            }
        }
        private Invite mInvite;
    }
    public static class LightWeightCalendarObject extends DavResource implements CalendarObject {
        private long mMailboxId;
        private int mId;
        private String mUid;
        private String mEtag;
        private long mStart;
        private long mEnd;

        public LightWeightCalendarObject(String path, String owner, CalendarItem.CalendarMetadata data) {
            super(CalendarPath.generate(null, path, data.uid, -1), owner);
            mMailboxId = data.mailboxId;
            mId = data.itemId;
            mUid = data.uid;
            mStart = data.start_time;
            mEnd = data.end_time;
            mEtag = MailItemResource.getEtag(Integer.toString(data.mod_metadata), Integer.toString(data.mod_content));
            setProperty(DavElements.P_GETETAG, mEtag);
            addProperty(CalDavProperty.getCalendarData(this));
        }
        public String getUid() {
            return mUid;
        }
        public boolean match(Filter filter) {
            TimeRange range = filter.getTimeRange();
            if (range == null)
                return true;
            return range.matches(mMailboxId, mId, mStart, mEnd);
        }
        @Override public String getEtag() {
            return mEtag;
        }
        public String getVcalendar(DavContext ctxt, Filter filter) throws IOException, DavException {
            ZimbraLog.dav.debug("constructing full resource");
            return getFullResource(ctxt).getVcalendar(ctxt, filter);
        }
        @Override public InputStream getContent(DavContext ctxt) throws IOException, DavException {
            return new ByteArrayInputStream(getVcalendar(ctxt, null).getBytes("UTF-8"));
        }
        @Override public boolean isCollection() {
            return false;
        }
        @Override public void delete(DavContext ctxt) throws DavException {
        }
        private CalendarObject getFullResource(DavContext ctxt) throws DavException {
            String user = null;
            Account acct = ctxt.getOperationContext().getAuthenticatedUser();
            if (acct != null)
                user = acct.getName();
            try {
                DavResource rs = UrlNamespace.getResourceByItemId(ctxt, user, mId);
                if (rs instanceof LocalCalendarObject)
                    return (LocalCalendarObject)rs;
                else
                    throw new DavException("not a calendar item", HttpServletResponse.SC_BAD_REQUEST);
            } catch (ServiceException se) {
                throw new DavException("can't fetch item", se);
            }
        }
        @Override public boolean hasContent(DavContext ctxt) {
            return true;
        }
    }
    public static class LocalCalendarObject extends MailItemResource implements CalendarObject {

        public LocalCalendarObject(DavContext ctxt, CalendarItem calItem) throws ServiceException {
            this(ctxt, calItem, false);
        }

        public LocalCalendarObject(DavContext ctxt, CalendarItem calItem, boolean newItem) throws ServiceException {
            this(ctxt, CalendarPath.generate(ctxt, calItem.getPath(), calItem.getUid(), -1), calItem);
            mNewlyCreated = newItem;
        }

        public LocalCalendarObject(DavContext ctxt, String path, CalendarItem calItem) throws ServiceException {
            super(ctxt, path, calItem);
            mUid = calItem.getUid();
            mInvites = calItem.getInvites();
            mTzmap = calItem.getTimeZoneMap();
            Invite defInv = calItem.getDefaultInviteOrNull();
            if (defInv != null)
                setProperty(DavElements.P_DISPLAYNAME, defInv.getName());
            setProperty(DavElements.P_GETCONTENTTYPE, MimeConstants.CT_TEXT_CALENDAR);
            setProperty(DavElements.P_GETCONTENTLENGTH, Long.toString(calItem.getSize()));
            addProperty(CalDavProperty.getCalendarData(this));
            if (mInvites[0].hasRecurId() && mInvites.length > 1) {
                // put the main series to be the first invite, otherwise iCal won't like it.
                ArrayList<Invite> newList = new ArrayList<Invite>();
                ArrayList<Invite> exceptions = new ArrayList<Invite>();
                for (Invite i : mInvites) {
                    if (i.hasRecurId())
                        exceptions.add(i);
                    else
                        newList.add(i);
                }
                newList.addAll(exceptions);
                mInvites = newList.toArray(new Invite[0]);
            }
            mMailboxId = calItem.getMailboxId();
            mStart = calItem.getStartTime();
            mEnd = calItem.getEndTime();
        }

        private String mUid;
        private Invite[] mInvites;
        private TimeZoneMap mTzmap;
        private long mMailboxId;
        private long mStart;
        private long mEnd;

        /* Returns true if the supplied Filter matches this calendar object. */
        public boolean match(Filter filter) {
            TimeRange range = filter.getTimeRange();
            if (range != null && !range.matches(mMailboxId, mId, mStart, mEnd))
                return false;
            for (Invite inv : mInvites) {
                try {
                    ZCalendar.ZComponent vcomp = inv.newToVComponent(false, false);
                    if (filter.match(vcomp))
                        return true;
                } catch (ServiceException se) {
                    ZimbraLog.dav.warn("cannot convert to ICalendar", se);
                    continue;
                }
            }

            return false;
        }

        /* Returns iCalendar representation of events that matches
         * the supplied filter.
         */
        public String getVcalendar(DavContext ctxt, Filter filter) throws IOException, DavException {
            StringBuilder buf = new StringBuilder();

            buf.append("BEGIN:VCALENDAR\r\n");
            buf.append("VERSION:").append(ZCalendar.sIcalVersion).append("\r\n");
            buf.append("PRODID:").append(ZCalendar.sZimbraProdID).append("\r\n");
            Iterator<ICalTimeZone> iter = mTzmap.tzIterator();
            while (iter.hasNext()) {
                ICalTimeZone tz = iter.next();
                CharArrayWriter wr = new CharArrayWriter();
                tz.newToVTimeZone().toICalendar(wr, true);
                wr.flush();
                buf.append(wr.toCharArray());
                wr.close();
            }
            Account acct = ctxt.getAuthAccount();
            boolean allowPrivateAccess = false;
            try {
                Mailbox mbox = getMailbox(ctxt);
                OperationContext octxt = ctxt.getOperationContext();
                Folder folder = mbox.getFolderById(octxt, mFolderId);
                allowPrivateAccess = CalendarItem.allowPrivateAccess(
                        folder, ctxt.getAuthAccount(), octxt.isUsingAdminPrivileges());
            } catch (ServiceException se) {
                ZimbraLog.dav.warn("cannot determine private access status", se);
            }
            buf.append(Vcalendar.toString(acct, mInvites, filter, allowPrivateAccess));
            buf.append("END:VCALENDAR\r\n");
            return buf.toString();
        }

        @Override
        public InputStream getContent(DavContext ctxt) throws IOException, DavException {
            return new ByteArrayInputStream(getVcalendar(ctxt, null).getBytes("UTF-8"));
        }

        @Override
        public boolean isCollection() {
            return false;
        }

        public String getUid() {
            return mUid;
        }

        public boolean hasContent(DavContext ctxt) {
            return true;
        }
    }
}
