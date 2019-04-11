/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.dav.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.QName;

import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.soap.W3cDomUtil;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.dav.DavContext.Depth;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.service.UserServlet.HttpInputStream;

public class CalDavClient extends WebDavClient {

    public static class Appointment {
        public Appointment(String h, String e) {
            href = h; etag = e;
        }
        public Appointment(String h, String e, String d) {
            this(h, e);
            data = d;
        }
        public Appointment(String h, String e, String d, Collection<String> r) {
            this(h, e, d);
            recipients = r;
        }
        public String href;
        public String etag;
        public String data;
        public Collection<String> recipients;
    }

    public CalDavClient(String baseUrl) {
        super(baseUrl);
        mSchedulingEnabled = false;
    }

    public void enableScheduling(boolean enable) {
        mSchedulingEnabled = enable;
    }

    private String getCurrentUserPrincipal() {
        DavRequest propfind = DavRequest.PROPFIND("/.well-known/caldav");
        propfind.addRequestProp(DavElements.E_CURRENT_USER_PRINCIPAL);
        HttpResponse response = null;
        try {
            response = executeFollowRedirect(propfind);
            int status = response.getStatusLine().getStatusCode();
            if (status >= 400) {
                return null;
            }
            Document doc = W3cDomUtil.parseXMLToDom4jDocUsingSecureProcessing(response.getEntity().getContent());
            Element top = doc.getRootElement();
            for (Object obj : top.elements(DavElements.E_RESPONSE)) {
                if (obj instanceof Element) {
                    DavObject davObject = new DavObject((Element)obj);
                    Element e = davObject.getProperty(DavElements.E_CURRENT_USER_PRINCIPAL);
                    if (e != null) {
                        return e.getStringValue().trim();
                    }
                }
            }
        } catch (Exception e) {
            ZimbraLog.dav.debug("Exception thrown getting Current User Principal", e);
            return null;
        } finally {
            if (response != null) {
                try {
                    EntityUtils.consume(response.getEntity());
                } catch (IOException e) {
                    ZimbraLog.dav.debug("Exception thrown while releasing connection", e);
                }
            }
        }
        return null;
    }

    public void login(String defaultPrincipalUrl) throws IOException, DavException, HttpException {
        String principalUrl = getCurrentUserPrincipal();
        if (principalUrl == null) {
            principalUrl = defaultPrincipalUrl;
        }
        DavRequest propfind = DavRequest.PROPFIND(principalUrl);
        propfind.addRequestProp(DavElements.E_DISPLAYNAME);
        propfind.addRequestProp(DavElements.E_CALENDAR_HOME_SET);
        propfind.addRequestProp(DavElements.E_SCHEDULE_INBOX_URL);
        propfind.addRequestProp(DavElements.E_SCHEDULE_OUTBOX_URL);
        Collection<DavObject> response = sendMultiResponseRequest(propfind);
        if (response.size() != 1) {
            throw new DavException(
                    String.format("invalid response to propfind on principal url '%s'", principalUrl), null);
        }
        DavObject resp = response.iterator().next();
        mCalendarHomeSet = new HashSet<String>();
        Element homeSet = resp.getProperty(DavElements.E_CALENDAR_HOME_SET);
        if (homeSet != null) {
            for (Object href : homeSet.elements(DavElements.E_HREF)) {
                String hrefVal = ((Element)href).getText();
                mCalendarHomeSet.add(hrefVal);
            }
        }
        if (mCalendarHomeSet.isEmpty()) {
            throw new DavException("dav response from principal url does not contain calendar-home-set", null);
        }
        Element elem = resp.getProperty(DavElements.E_SCHEDULE_INBOX_URL);
        if (elem != null && elem.element(DavElements.E_HREF) != null) {
            mScheduleInbox = elem.element(DavElements.E_HREF).getText();
        }
        elem = resp.getProperty(DavElements.E_SCHEDULE_OUTBOX_URL);
        if (elem != null && elem.element(DavElements.E_HREF) != null) {
            mScheduleOutbox = elem.element(DavElements.E_HREF).getText();
        }
    }

    public Collection<String> getCalendarHomeSet() {
        return mCalendarHomeSet;
    }

    private static Collection<QName> CALENDAR_PROPS;
    static {
        CALENDAR_PROPS = new ArrayList<QName>();
        CALENDAR_PROPS.add(DavElements.E_DISPLAYNAME);
        CALENDAR_PROPS.add(DavElements.E_RESOURCETYPE);
        CALENDAR_PROPS.add(DavElements.E_GETCTAG);
    }
    public Map<String,DavObject> getCalendars() throws IOException, DavException, HttpException {
        HashMap<String,DavObject> calendars = new HashMap<String,DavObject>();
        for (String calHome : mCalendarHomeSet) {
            for (DavObject obj : listObjects(calHome, CALENDAR_PROPS)) {
                String href = obj.getHref();
                String displayName = obj.getDisplayName();
                if (obj.isCalendarFolder() && displayName != null && href != null)
                    calendars.put(displayName, obj);
            }
        }
        return calendars;
    }

    public Collection<Appointment> getEtags(String calendarUri) throws IOException, DavException, HttpException {
        ArrayList<Appointment> etags = new ArrayList<Appointment>();
        DavRequest propfind = DavRequest.PROPFIND(calendarUri);
        propfind.setDepth(Depth.one);
        propfind.addRequestProp(DavElements.E_GETETAG);
        propfind.addRequestProp(DavElements.E_RESOURCETYPE);
        Collection<DavObject> response = sendMultiResponseRequest(propfind);
        for (DavObject obj : response) {
            String href = obj.getHref();
            String etag = obj.getPropertyText(DavElements.E_GETETAG);
            if (!obj.isFolder() && etag != null && href != null)
                etags.add(new Appointment(href, etag));
        }
        return etags;
    }

    public Appointment getCalendarData(Appointment appt) throws IOException, HttpException {
        HttpInputStream resp = sendGet(appt.href);
        appt.data = null;
        if (resp.getStatusCode() == 200) {
            byte[] res = ByteUtil.getContent(resp, resp.getContentLength());
            appt.data = new String(res, "UTF-8");
        }
        return appt;
    }

    public Appointment getEtag(String url) throws IOException, DavException, HttpException {
        Appointment appt = new Appointment(url, null);
        DavRequest propfind = DavRequest.PROPFIND(url);
        propfind.setDepth(Depth.zero);
        propfind.addRequestProp(DavElements.E_GETETAG);
        propfind.addRequestProp(DavElements.E_RESOURCETYPE);
        Collection<DavObject> response = sendMultiResponseRequest(propfind);
        for (DavObject obj : response) {
            String href = obj.getHref();
            if (href.equals(url)) {
                appt.etag = obj.getPropertyText(DavElements.E_GETETAG);
                return appt;
            }
        }
        return appt;
    }

    public String sendCalendarData(Appointment appt) throws IOException, DavException, HttpException {
        HttpInputStream resp = sendPut(appt.href, appt.data.getBytes("UTF-8"), MimeConstants.CT_TEXT_CALENDAR, appt.etag, null);
        String etag = resp.getHeader(DavProtocol.HEADER_ETAG);
        ZimbraLog.dav.debug("ETags: "+appt.etag+", "+etag);
        int status = resp.getStatusCode();
        if (status != HttpStatus.SC_OK && status != HttpStatus.SC_CREATED && status != HttpStatus.SC_NO_CONTENT) {
            throw new DavException("Can't send calendar data (status="+status+")", status);
        }
        if (mSchedulingEnabled)
            sendSchedulingMessage(appt);
        return etag;
    }

    private void sendSchedulingMessage(Appointment appt) throws IOException, DavException {
    }

    public Collection<Appointment> getCalendarData(String url, Collection<Appointment> hrefs) throws IOException, DavException, HttpException {
        ArrayList<Appointment> appts = new ArrayList<Appointment>();

        DavRequest multiget = DavRequest.CALENDARMULTIGET(url);
        multiget.addRequestProp(DavElements.E_GETETAG);
        multiget.addRequestProp(DavElements.E_CALENDAR_DATA);
        for (Appointment appt : hrefs)
            multiget.addHref(appt.href);
        Collection<DavObject> response = sendMultiResponseRequest(multiget);
        for (DavObject obj : response) {
            String href = obj.getHref();
            String etag = obj.getPropertyText(DavElements.E_GETETAG);
            String calData = obj.getPropertyText(DavElements.E_CALENDAR_DATA);
            if (href != null && calData != null)
                appts.add(new Appointment(href, etag, calData));
        }
        return appts;
    }

    public Collection<Appointment> getAllCalendarData(String url) throws IOException, DavException, HttpException {
        ArrayList<Appointment> appts = new ArrayList<Appointment>();

        DavRequest query = DavRequest.CALENDARQUERY(url);
        query.addRequestProp(DavElements.E_GETETAG);
        query.addRequestProp(DavElements.E_CALENDAR_DATA);
        Collection<DavObject> response = sendMultiResponseRequest(query);
        for (DavObject obj : response) {
            String href = obj.getHref();
            String etag = obj.getPropertyText(DavElements.E_GETETAG);
            String calData = obj.getPropertyText(DavElements.E_CALENDAR_DATA);
            if (href != null && calData != null)
                appts.add(new Appointment(href, etag, calData));
        }
        return appts;
    }

    private HashSet<String> mCalendarHomeSet;
    private boolean mSchedulingEnabled;
    private String mScheduleInbox;
    private String mScheduleOutbox;

    protected String accessToken;
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
