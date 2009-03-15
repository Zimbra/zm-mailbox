/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Header;
import org.dom4j.QName;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.dav.caldav.TimeRange;
import com.zimbra.cs.dav.client.CalDavClient;
import com.zimbra.cs.dav.service.DavServlet;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.AuthProviderException;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.zclient.ZAppointmentHit;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZSearchParams;
import com.zimbra.cs.zclient.ZMailbox.ZApptSummaryResult;

public class RemoteCalendarCollection extends CalendarCollection {

    private String mRemoteId;
    private int mItemId;
    private HashMap<String,String> mCalendarData;
    
    public RemoteCalendarCollection(DavContext ctxt, Mountpoint mp) throws DavException, ServiceException {
        super(ctxt, mp);
        mRemoteId = mp.getOwnerId();
        mItemId = mp.getRemoteId();
    }

    @Override
    public void delete(DavContext ctxt) throws DavException {
        throw new DavException("cannot delete this resource", HttpServletResponse.SC_FORBIDDEN, null);
    }

    @Override
    public boolean isCollection() {
        return true;
    }
    
	public java.util.Collection<DavResource> getChildren(DavContext ctxt, java.util.Collection<String> hrefs, TimeRange range) throws DavException {
        try {
            Account target = Provisioning.getInstance().get(Provisioning.AccountBy.id, mRemoteId);
            if (target != null && Provisioning.onLocalServer(target))
            	return super.getChildren(ctxt, hrefs, range);
        } catch (ServiceException se) {
	        ZimbraLog.dav.warn("cannot determine shared folder for "+ctxt.getAuthAccount().getName(), se);
        }
    	boolean needCalendarData = false;
		for (QName prop : ctxt.getRequestProp().getProps()) {
			if (prop.equals(DavElements.E_CALENDAR_DATA)) {
				needCalendarData = true;
				break;
			}
		}
		
		Map<String,String> uidmap = getHrefUidMap(hrefs, false);
		
		if (needCalendarData) {
			try {
				getCalendarData(ctxt, uidmap.values());
			} catch (Exception e) {
		        ZimbraLog.dav.warn("can't proxy calendar data for "+ctxt.getAuthAccount().getName(), e);
			}
		}

		return super.getChildren(ctxt, hrefs, range);
	}
	
    String getCalendarData(String uid) {
    	if (mCalendarData != null)
    		return mCalendarData.get(uid);
    	return null;
    }
    
    private void getCalendarData(DavContext ctxt, java.util.Collection<String> uids) throws ServiceException, IOException, DavException {
        AuthToken authToken = AuthProvider.getAuthToken(ctxt.getAuthAccount());
        Account target = Provisioning.getInstance().get(Provisioning.AccountBy.id, mRemoteId);
        if (target == null)
            return;
        ZMailbox zmbx = getRemoteMailbox(authToken.toZAuthToken());
        ZFolder f = zmbx.getFolderById(new ItemId(mRemoteId, mItemId).toString());
        String path = f.getPath();
        String url = DavServlet.getDavUrl(target.getName());
        CalDavClient cl = new CalDavClient(url);
    
    	java.util.Collection<CalDavClient.Appointment> appts = new ArrayList<CalDavClient.Appointment>();
    	HashMap<String,String> hrefmap = new HashMap<String,String>();
    	for (String uid : uids) {
    		StringBuilder buf = new StringBuilder();
    		buf.append(DavServlet.DAV_PATH).append("/");
    		buf.append(target.getName());
    		buf.append(path).append("/");
    		buf.append(uid);
    		buf.append(".ics");
    		hrefmap.put(buf.toString(), uid);
    		appts.add(new CalDavClient.Appointment(buf.toString(), null));
    	}
    	cl.setAuthCookie(authToken.toZAuthToken());
        appts = cl.getCalendarData(path, appts);
        mCalendarData = new HashMap<String,String>();
        for (CalDavClient.Appointment appt : appts)
        	mCalendarData.put(hrefmap.get(appt.href), appt.data);
    }
    
    protected Map<String,DavResource> getAppointmentMap(DavContext ctxt, TimeRange range) throws DavException {
        HashMap<String,DavResource> appts = new HashMap<String,DavResource>();
        ZAuthToken zat = null;
        try {
            zat = AuthProvider.getAuthToken(ctxt.getAuthAccount()).toZAuthToken();
        } catch (ServiceException e) {
            ZimbraLog.dav.warn("can't get auth token for "+ctxt.getAuthAccount().getName(), e);
            return appts;
        }

        List<ZApptSummaryResult> results;
        
        try {
            Account target = Provisioning.getInstance().get(Provisioning.AccountBy.id, mRemoteId);
            if (target == null)
            	return appts;
            if (Provisioning.onLocalServer(target))
            	return super.getAppointmentMap(ctxt, range);
            ZMailbox.Options zoptions = new ZMailbox.Options(zat, AccountUtil.getSoapUri(target));
            zoptions.setNoSession(true);
            zoptions.setTargetAccount(mRemoteId);
            zoptions.setTargetAccountBy(Provisioning.AccountBy.id);
            ZMailbox zmbx = ZMailbox.getMailbox(zoptions);
            String folderId = Integer.toString(mItemId);
        	long start = 0;
        	long end = 0;
            if (range != null) {
            	start = range.getStart();
            	end = range.getEnd();
            } else {
            	TimeRange mine = new TimeRange(getOwner());
            	Account remoteAcct = Provisioning.getInstance().get(Provisioning.AccountBy.id, mRemoteId);
            	if (remoteAcct != null) {
                	TimeRange theirs = new TimeRange(remoteAcct.getName());
            		mine.intersection(theirs);
            	}
            	start = mine.getStart();
            	end = mine.getEnd();
            }
            results = zmbx.getApptSummaries(null, start, end, new String[] {folderId}, TimeZone.getDefault(), ZSearchParams.TYPE_APPOINTMENT);
        } catch (ServiceException e) {
            ZimbraLog.dav.warn("can't proxy the request for "+ctxt.getAuthAccount().getName(), e);
            return appts;
        }
        
        for (ZAppointmentHit appt : results.get(0).getAppointments())
            appts.put(appt.getUid(), new CalendarObject.RemoteCalendarObject(mUri, mOwner, appt, this));
        
        return appts;
    }
    
    @Override
    public DavResource createItem(DavContext ctxt, String name) throws DavException, IOException {
    	Header[] respHeaders = null;
        try {
            AuthToken authToken = AuthProvider.getAuthToken(ctxt.getAuthAccount());
            Account target = Provisioning.getInstance().get(Provisioning.AccountBy.id, mRemoteId);
            if (target == null)
                throw new DavException("can't create resource", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            ZMailbox zmbx = getRemoteMailbox(authToken.toZAuthToken());
            ZFolder f = zmbx.getFolderById(new ItemId(mRemoteId, mItemId).toString());
            
            @SuppressWarnings("unchecked")
            Enumeration reqHeaders = ctxt.getRequest().getHeaderNames();
            ArrayList<Header> headerList = new ArrayList<Header>();
            while (reqHeaders.hasMoreElements()) {
                String hdr = (String)reqHeaders.nextElement();
                if (!hdr.equals("Host") && !hdr.equals("Cookie") && !hdr.equals("Authorization"))
                    headerList.add(new Header(hdr, ctxt.getRequest().getHeader(hdr)));
            }
            String url = URLUtil.urlEscape(f.getPath() + "/" + ctxt.getItem());
            url = DavServlet.getDavUrl(target.getName()) + url;
            respHeaders = UserServlet.putRemoteResource(authToken, url, target, ctxt.getUpload().getInputStream(), headerList.toArray(new Header[0])).getFirst();
        } catch (ServiceException e) {
            ZimbraLog.dav.warn("can't proxy the request for "+ctxt.getAuthAccount().getName(), e);
            throw new DavException("can't create resource", HttpServletResponse.SC_FORBIDDEN);
        }
        String status = null, etag = null;
        for (Header h : respHeaders) {
        	String hname = h.getName();
            if (hname.equals(DavProtocol.HEADER_ETAG))
            	etag = h.getValue();
            else if (hname.equals("X-Zimbra-Http-Status"))
            	status = h.getValue();
        }

        if (status == null || etag == null)
            throw new DavException("can't create resource", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        
        UrlNamespace.invalidateApptSummariesCache(mOwnerId, mRemoteId, mItemId);
        return new CalendarObject.RemoteCalendarObject(ctxt.getPath(), ctxt.getUser(), etag, this, status.equals("" + HttpServletResponse.SC_CREATED));
    }
    
    public DavResource getAppointment(DavContext ctxt, String uid) throws DavException {
    	getChildren(ctxt);
        return mAppts.get(uid);
    }
    
    public void deleteAppointment(DavContext ctxt, int id) throws DavException {
        try {
            ZAuthToken zat = AuthProvider.getAuthToken(ctxt.getAuthAccount()).toZAuthToken();
            ZMailbox zmbx = getRemoteMailbox(zat);
            zmbx.deleteItem(Integer.toString(id), null);
        } catch (AuthProviderException e) {
            throw new DavException("can't delete", HttpServletResponse.SC_FORBIDDEN, e);
        } catch (ServiceException e) {
            throw new DavException("can't delete", HttpServletResponse.SC_FORBIDDEN, e);
        }
    }
    
    protected int getId() {
    	return mItemId;
    }
    
	protected Mailbox getMailbox(DavContext ctxt) throws ServiceException, DavException {
		Provisioning prov = Provisioning.getInstance();
		Account account = prov.get(AccountBy.id, mRemoteId);
		if (account == null)
			return super.getMailbox(ctxt);
		if (Provisioning.onLocalServer(account))
			return MailboxManager.getInstance().getMailboxByAccount(account);
		return null;
	}
	
    private ZMailbox getRemoteMailbox(ZAuthToken zat) throws ServiceException {
        Account target = Provisioning.getInstance().get(Provisioning.AccountBy.id, mRemoteId);
        if (target == null)
        	return null;
        ZMailbox.Options zoptions = new ZMailbox.Options(zat, AccountUtil.getSoapUri(target));
        zoptions.setNoSession(true);
        zoptions.setTargetAccount(mRemoteId);
        zoptions.setTargetAccountBy(Provisioning.AccountBy.id);
        return ZMailbox.getMailbox(zoptions);
    }
}
