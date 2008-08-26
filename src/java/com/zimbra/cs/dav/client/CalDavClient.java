/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
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
package com.zimbra.cs.dav.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.dom4j.Element;

import com.zimbra.cs.dav.DavContext.Depth;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;

public class CalDavClient extends WebDavClient {

	public static class Appointment {
		public Appointment(String h, String e) {
			href = h; etag = e;
		}
		public Appointment(String h, String e, String d) {
			this(h, e);
			data = d;
		}
		public String href;
		public String etag;
		public String data;
	}
	
	public CalDavClient(String baseUrl) {
		super(baseUrl);
	}
	
	public void login(String principalUrl) throws IOException, DavException {
		DavRequest propfind = DavRequest.PROPFIND(principalUrl);
		propfind.addRequestProp(DavElements.E_DISPLAYNAME);
		propfind.addRequestProp(DavElements.E_CALENDAR_HOME_SET);
		Collection<DavObject> response = sendMultiResponseRequest(propfind);
		if (response.size() != 1)
			throw new DavException("too many response to propfind on principal url", null);
		DavObject resp = response.iterator().next();
		mCalendarHomeSet = new HashSet<String>();
		Element homeSet = resp.getProperty(DavElements.E_CALENDAR_HOME_SET);
		for (Object href : homeSet.elements(DavElements.E_HREF))
			mCalendarHomeSet.add(((Element)href).getText());
		if (mCalendarHomeSet.isEmpty())
			throw new DavException("dav response from principal url does not contain calendar-home-set", null);
	}

	public Collection<String> getCalendarHomeSet() {
		return mCalendarHomeSet;
	}
	
	public Map<String,String> getCalendars() throws IOException, DavException {
		HashMap<String,String> calendars = new HashMap<String,String>();
		for (String calHome : mCalendarHomeSet) {
			DavRequest propfind = DavRequest.PROPFIND(calHome);
			propfind.setDepth(Depth.one);
			propfind.addRequestProp(DavElements.E_DISPLAYNAME);
			propfind.addRequestProp(DavElements.E_RESOURCETYPE);
			Collection<DavObject> response = sendMultiResponseRequest(propfind);
			for (DavObject obj : response) {
				String href = obj.getHref();
				String displayName = obj.getPropertyText(DavElements.E_DISPLAYNAME);
				boolean isCalendar = obj.isResourceType(DavElements.E_CALENDAR);
				if (isCalendar && displayName != null && href != null)
					calendars.put(displayName, href);
			}
		}
		return calendars;
	}

	public Collection<Appointment> getEtags(String calendarUri) throws IOException, DavException {
		ArrayList<Appointment> etags = new ArrayList<Appointment>();
		DavRequest propfind = DavRequest.PROPFIND(calendarUri);
		propfind.setDepth(Depth.one);
		propfind.addRequestProp(DavElements.E_GETETAG);
		propfind.addRequestProp(DavElements.E_RESOURCETYPE);
		Collection<DavObject> response = sendMultiResponseRequest(propfind);
		for (DavObject obj : response) {
			String href = obj.getHref();
			String etag = obj.getPropertyText(DavElements.E_GETETAG);
			boolean isCollection = obj.isResourceType(DavElements.E_COLLECTION);
			if (!isCollection && etag != null && href != null)
				etags.add(new Appointment(href, etag));
		}
		return etags;
	}
	
	public Collection<Appointment> getCalendarData(String url, Collection<Appointment> hrefs) throws IOException, DavException {
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
	
	public Collection<Appointment> getAllCalendarData(String url) throws IOException, DavException {
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
	
	public static void main(String[] args) throws Exception {
		CalDavClient client = new CalDavClient("http://localhost:7070");
		client.setCredential("user1", "test123");
		client.login("/principals/users/user1");
		Map<String,String> calendars = client.getCalendars();
		for (String key : calendars.keySet()) {
			String url = calendars.get(key);
			System.out.println("name: "+key+", \turl: "+url);
			
			if (false) {
				Collection<Appointment> calData = client.getAllCalendarData(url);
				for (Appointment a : calData) {
					System.out.println("\tappt: "+a.href+", \t etag: "+a.etag+", \t data: "+a.data);
				}
			}
			

			if (true) {
				Collection<Appointment> etags = client.getEtags(url);
				for (Appointment a : etags) {
					System.out.println("\tappt: "+a.href+", \t etag: "+a.etag);
				}
				Collection<Appointment> calData = client.getCalendarData(url, etags);
				for (Appointment a : calData) {
					System.out.println("\tappt: "+a.href+", \t data: "+a.data);
				}
			}

		}
	}
}
