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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.Header;
import org.dom4j.QName;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.dav.caldav.TimeRange;
import com.zimbra.cs.dav.client.CalDavClient;
import com.zimbra.cs.dav.service.DavServlet;
import com.zimbra.cs.httpclient.URLUtil;
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
    private ArrayList<DavResource> mChildren;
    private HashMap<String,DavResource> mAppointments;
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
    
    @Override
	public java.util.Collection<DavResource> getChildren(DavContext ctxt, java.util.Collection<String> hrefs) throws DavException {
    	boolean needCalendarData = false;
		for (QName prop : ctxt.getRequestProp().getProps()) {
			if (prop.equals(DavElements.E_CALENDAR_DATA)) {
				needCalendarData = true;
				break;
			}
		}
		
		Map<String,String> uidmap = getHrefUidMap(hrefs, false);
		
		if (needCalendarData)
			try {
				getCalendarData(ctxt, uidmap.values());
			} catch (Exception e) {
		        ZimbraLog.dav.warn("can't proxy calendar data for "+ctxt.getAuthAccount().getName(), e);
			}
		
		get(ctxt, sAllCalItems);
		ArrayList<DavResource> resp = new ArrayList<DavResource>();
		for (String href : uidmap.keySet()) {
			String uid = uidmap.get(href);
			DavResource rs = mAppointments.get(uid);
			if (rs == null)
				rs = new DavResource.InvalidResource(href, getOwner());
			resp.add(rs);
		}
		return resp;
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
        String auth = ctxt.getRequest().getHeader("Authorization");
        String userPass = new String(Base64.decodeBase64(auth.substring(6).getBytes()));
        int loc = userPass.indexOf(":"); 
        String user = userPass.substring(0, loc);
        String pass = userPass.substring(loc + 1);
        cl.setCredential(user, pass);
        appts = cl.getCalendarData(path, appts);
        mCalendarData = new HashMap<String,String>();
        for (CalDavClient.Appointment appt : appts)
        	mCalendarData.put(hrefmap.get(appt.href), appt.data);
    }
    
    @Override
    public java.util.Collection<DavResource> get(DavContext ctxt, TimeRange range) throws DavException {
        if (mChildren != null)
            return mChildren;
        
        mAppointments = new HashMap<String,DavResource>();
        ZAuthToken zat = null;
        try {
            zat = AuthProvider.getAuthToken(ctxt.getAuthAccount()).toZAuthToken();
        } catch (ServiceException e) {
            ZimbraLog.dav.warn("can't generate authToken for "+ctxt.getAuthAccount().getName(), e);
            return Collections.emptyList();
        }

        List<ZApptSummaryResult> results;
        
        try {
            ZMailbox zmbx = getRemoteMailbox(zat);
            if (zmbx == null) {
                ZimbraLog.dav.warn("remote account not found: "+mRemoteId);
                return Collections.emptyList();
            }
            String folderId = Integer.toString(mItemId);
            results = zmbx.getApptSummaries(null, range.getStart(), range.getEnd(), new String[] {folderId}, TimeZone.getDefault(), ZSearchParams.TYPE_APPOINTMENT);
        } catch (ServiceException e) {
            ZimbraLog.dav.warn("can't proxy the request for "+ctxt.getAuthAccount().getName(), e);
            return Collections.emptyList();
        }
        
        mChildren = new ArrayList<DavResource>();
        for (ZAppointmentHit appt : results.get(0).getAppointments()) {
            DavResource res = mAppointments.get(appt.getUid());
            if (res != null)
                continue;
            res = new CalendarObject.RemoteCalendarObject(mUri, mOwner, appt, this);
            mChildren.add(res);
            mAppointments.put(appt.getUid(), res);
        }
        
        return mChildren;
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
                if (!hdr.equals("Host") && !hdr.equals("Cookie"))
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
        
        UrlNamespace.invalidateApptSummariesCache(mRemoteId, mItemId);
        return new CalendarObject.RemoteCalendarObject(ctxt.getPath(), ctxt.getUser(), etag, this, status.equals("" + HttpServletResponse.SC_CREATED));
    }
    
    public DavResource getAppointment(DavContext ctxt, String uid) throws DavException {
        if (mAppointments == null) {
            getChildren(ctxt);
        }
        return mAppointments.get(uid.toLowerCase());
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
