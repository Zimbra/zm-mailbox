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
package com.zimbra.cs.dav.resource;

import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.caldav.Filter;
import com.zimbra.cs.dav.property.CalDavProperty;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.TimeZoneMap;
import com.zimbra.cs.mailbox.calendar.ZCalendar;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.zclient.ZAppointmentHit;

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
    public String getVcalendar(DavContext ctxt, Filter filter) throws IOException;    

    public static class CalendarPath {
        public static String generate(String itemPath, String uid) {
            // escape uid
            StringBuilder path = new StringBuilder();
            path.append(itemPath);
            if (path.charAt(path.length()-1) != '/')
                path.append("/");
            path.append(uid);
            path.append(CAL_EXTENSION);
            // XXX iCal doesn't handle unescaped URL in some cases
            return UrlNamespace.urlEscape(path.toString());
        }
    }
    public static class LocalCalendarObject extends MailItemResource implements CalendarObject {

        public LocalCalendarObject(DavContext ctxt, CalendarItem calItem) throws ServiceException {
            this(ctxt, getCalendarPath(calItem), calItem);
        }

        protected LocalCalendarObject(DavContext ctxt, String path, CalendarItem calItem) throws ServiceException {
            super(ctxt, path, calItem);
            mUid = calItem.getUid();
            mInvites = calItem.getInvites();
            mTzmap = calItem.getTimeZoneMap();
            Invite defInv = calItem.getDefaultInviteOrNull();
            if (defInv != null)
                setProperty(DavElements.P_DISPLAYNAME, defInv.getName());
            setProperty(DavElements.P_GETCONTENTTYPE, Mime.CT_TEXT_CALENDAR);
            setProperty(DavElements.P_GETCONTENTLENGTH, Long.toString(calItem.getSize()));
            addProperty(CalDavProperty.getCalendarData(this));
            mAccount = calItem.getAccount();
            if (mInvites[0].hasRecurId()) {
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
        }

        private String mUid;
        private Invite[] mInvites;
        private TimeZoneMap mTzmap;
        private Account mAccount;

        protected static String getCalendarPath(CalendarItem calItem) throws ServiceException {
            return CalendarPath.generate(calItem.getPath(), calItem.getUid());
        }

        /* Returns true if the supplied Filter matches this calendar object. */
        public boolean match(Filter filter) {
            for (Invite inv : mInvites)
                if (filter.match(inv))
                    return true;

            return false;
        }

        /* Returns iCalendar representation of events that matches
         * the supplied filter.
         */
        public String getVcalendar(DavContext ctxt, Filter filter) throws IOException {
            StringBuilder buf = new StringBuilder();

            buf.append("BEGIN:VCALENDAR\r\n");
            buf.append("VERSION:").append(ZCalendar.sIcalVersion).append("\r\n");
            buf.append("PRODID:").append(ZCalendar.sZimbraProdID).append("\r\n");
            Iterator<ICalTimeZone> iter = mTzmap.tzIterator();
            while (iter.hasNext()) {
                ICalTimeZone tz = (ICalTimeZone) iter.next();
                CharArrayWriter wr = new CharArrayWriter();
                tz.newToVTimeZone().toICalendar(wr);
                wr.flush();
                buf.append(wr.toCharArray());
                wr.close();
            }
            for (Invite inv : mInvites) {
                if (filter != null && !filter.match(inv))
                    continue;
                CharArrayWriter wr = new CharArrayWriter();
                try {
                    boolean allowPrivateAccess = Account.allowPrivateAccess(ctxt.getAuthAccount(), mAccount);
                    inv.newToVComponent(false, allowPrivateAccess).toICalendar(wr);
                } catch (ServiceException se) {
                    ZimbraLog.dav.error("cannot convert to ICalendar", se);
                }
                wr.flush();
                buf.append(wr.toCharArray());
                wr.close();
            }
            buf.append("END:VCALENDAR\r\n");
            return buf.toString();
        }

        @Override
        public InputStream getContent(DavContext ctxt) throws IOException {
            return new ByteArrayInputStream(getVcalendar(ctxt, null).getBytes());
        }

        @Override
        public boolean isCollection() {
            return false;
        }

        public String getUid() {
            return mUid;
        }
    }
	
	public static class RemoteCalendarObject extends DavResource implements CalendarObject {

	    public RemoteCalendarObject(String uri, String owner, ZAppointmentHit appt, RemoteCalendarCollection parent) {
	        super(CalendarPath.generate(uri, appt.getUid()), owner);
	        mParent = parent;
	        mUid = appt.getUid();
            ItemId iid;
            try {
                iid = new ItemId(appt.getId(), (String)null);
                mRemoteId = iid.getAccountId();
                mItemId = iid.getId();
            } catch (ServiceException e) {
                ZimbraLog.dav.warn("can't generate itemId from "+appt.getId(), e);
            }
	        mEtag = getEtag(appt);
	        setProperty(DavElements.P_GETCONTENTTYPE, Mime.CT_TEXT_CALENDAR);
	        setProperty(DavElements.P_GETETAG, mEtag);
            addProperty(CalDavProperty.getCalendarData(this));
	    }

	    public RemoteCalendarObject(String uri, String owner, String etag, RemoteCalendarCollection parent) {
	        super(uri, owner);
	        mParent = parent;
	        mEtag = etag;
            setProperty(DavElements.P_GETCONTENTTYPE, Mime.CT_TEXT_CALENDAR);
            setProperty(DavElements.P_GETETAG, mEtag);
	    }
	    
        public static String getEtag(ZAppointmentHit item) {
            return "\""+Long.toString(item.getModifiedSeq())+"-"+Long.toString(item.getModifiedDate())+"\"";
        }
		private RemoteCalendarCollection mParent;
	    private String mRemoteId;
	    private int mItemId;
	    private String mUid;
	    private String mEtag;
	    
	    @Override
	    public void delete(DavContext ctxt) throws DavException {
	    	mParent.deleteAppointment(ctxt, mItemId);
	    }

	    @Override
	    public InputStream getContent(DavContext ctxt) throws IOException, DavException {
            byte[] result = getRawContent(ctxt);
            if (result != null)
                return new ByteArrayInputStream(result);
	        return null;
	    }

	    @Override
	    public boolean isCollection() {
	        return false;
	    }
	    
	    @Override
	    public boolean hasContent(DavContext ctxt) {
	        return true;
	    }
	    
	    public String getUid() {
	        return mUid;
	    }
	    
	    public boolean match(Filter filter) {
	        return true;
	    }
	    
	    public String getVcalendar(DavContext ctxt, Filter filter) throws IOException {
            byte[] result = getRawContent(ctxt);
            if (result != null)
                return new String(result, "UTF-8");
            return "";
	    }
	    
        public byte[] getRawContent(DavContext ctxt) throws IOException {
            AuthToken authToken;
            authToken = AuthToken.getAuthToken(ctxt.getAuthAccount());
            
            try {
                ItemId iid = new ItemId(mRemoteId, mItemId);
                HashMap<String,String> params = new HashMap<String,String>();
                return UserServlet.getRemoteContent(authToken, iid, params);
            } catch (ServiceException e) {
                ZimbraLog.dav.warn("can't get remote contents for "+mRemoteId+", "+mItemId);
            }
            return null;
        }
	}
}
