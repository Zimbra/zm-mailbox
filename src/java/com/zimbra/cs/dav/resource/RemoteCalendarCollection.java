/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
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
import com.zimbra.cs.dav.property.Acl;
import com.zimbra.cs.dav.property.ResourceProperty;
import com.zimbra.cs.dav.service.DavServlet;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.calendar.cache.CtagInfo;
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

    private HashMap<String,String> mCalendarData;
	private String mRemoteOwnerId;
	private int mRemoteId;
    
    public RemoteCalendarCollection(DavContext ctxt, Mountpoint mp) throws DavException, ServiceException {
        super(ctxt, mp);
		
		mRemoteOwnerId = mp.getOwnerId();
		mRemoteId = mp.getRemoteId();
		mMailboxId = 0;
		Account target = Provisioning.getInstance().get(Provisioning.AccountBy.id, mRemoteOwnerId);
		if (target != null && Provisioning.onLocalServer(target))
			mMailboxId = MailboxManager.getInstance().getMailboxByAccount(target).getId();

		addResourceType(DavElements.E_MOUNTPOINT);
		getMountpointTarget(ctxt);
		setProperty(DavElements.E_GETCTAG, mCtag);
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
        if (isLocal())
        	return super.getChildren(ctxt, hrefs, range);
		Map<String,String> uidmap = getHrefUidMap(hrefs, false);
		
		if (needCalendarData(ctxt)) {
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
        Account target = Provisioning.getInstance().get(Provisioning.AccountBy.id, mRemoteOwnerId);
        if (target == null)
            return;
        ZMailbox zmbx = RemoteCollection.getRemoteMailbox(authToken.toZAuthToken(), mRemoteOwnerId);
        ZFolder f = zmbx.getFolderById(new ItemId(mRemoteOwnerId, mRemoteId).toString());
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
    		hrefmap.put(URLUtil.urlEscape(buf.toString()), uid);
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
            if (isLocal())
            	return super.getAppointmentMap(ctxt, range);
            Account target = Provisioning.getInstance().get(Provisioning.AccountBy.id, mRemoteOwnerId);
            if (target == null)
            	return appts;
            ZMailbox.Options zoptions = new ZMailbox.Options(zat, AccountUtil.getSoapUri(target));
            zoptions.setNoSession(true);
            zoptions.setTargetAccount(mRemoteOwnerId);
            zoptions.setTargetAccountBy(Provisioning.AccountBy.id);
            ZMailbox zmbx = ZMailbox.getMailbox(zoptions);
            String folderId = Integer.toString(mRemoteId);
        	long start = 0;
        	long end = 0;
            if (range != null) {
            	start = range.getStart();
            	end = range.getEnd();
            } else {
            	TimeRange mine = new TimeRange(getOwner());
            	Account remoteAcct = Provisioning.getInstance().get(Provisioning.AccountBy.id, mRemoteOwnerId);
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
            Account target = Provisioning.getInstance().get(Provisioning.AccountBy.id, mRemoteOwnerId);
            if (target == null)
                throw new DavException("can't create resource", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            ZMailbox zmbx = RemoteCollection.getRemoteMailbox(authToken.toZAuthToken(), mRemoteOwnerId);
            ZFolder f = zmbx.getFolderById(new ItemId(mRemoteOwnerId, mRemoteId).toString());
            
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
        
        UrlNamespace.invalidateApptSummariesCache(mOwnerId, mRemoteOwnerId, mRemoteId);
        return new CalendarObject.RemoteCalendarObject(ctxt.getPath(), ctxt.getUser(), etag, this, status.equals("" + HttpServletResponse.SC_CREATED));
    }
    
    public DavResource getAppointment(DavContext ctxt, String uid) throws DavException {
    	return getAppointmentMap(ctxt, null).get(uid);
    }
    
    public void deleteAppointment(DavContext ctxt, int id) throws DavException {
        try {
            ZAuthToken zat = AuthProvider.getAuthToken(ctxt.getAuthAccount()).toZAuthToken();
            ZMailbox zmbx = RemoteCollection.getRemoteMailbox(zat, mRemoteOwnerId);
            zmbx.deleteItem(Integer.toString(id), null);
        } catch (AuthProviderException e) {
            throw new DavException("can't delete", HttpServletResponse.SC_FORBIDDEN, e);
        } catch (ServiceException e) {
            throw new DavException("can't delete", HttpServletResponse.SC_FORBIDDEN, e);
        }
    }
    
    public int getId() {
    	return mRemoteId;
    }
    
	protected Mailbox getCalendarMailbox(DavContext ctxt) throws ServiceException, DavException {
		if (isLocal())
			return MailboxManager.getInstance().getMailboxById(mMailboxId);
		return super.getMailbox(ctxt);
	}
	
    private void getMountpointTarget(DavContext ctxt) {
        try {
            ZAuthToken zat = AuthProvider.getAuthToken(ctxt.getAuthAccount()).toZAuthToken();
            ZMailbox zmbx = RemoteCollection.getRemoteMailbox(zat, mRemoteOwnerId);
            ZFolder folder = zmbx.getFolderById(new ItemId(mRemoteOwnerId, mRemoteId).toString(mOwnerId));
            if (folder == null)
            	return;
            mCtag = CtagInfo.makeCtag(folder);
            short rights = ACL.stringToRights(folder.getEffectivePerms());
            addProperty(Acl.getCurrentUserPrivilegeSet(rights));
            addProperty(Acl.getMountpointTargetPrivilegeSet(rights));
            String targetUrl = UrlNamespace.getResourceUrl(Provisioning.getInstance().get(Provisioning.AccountBy.id, mRemoteOwnerId), folder.getPath() + "/");
            ResourceProperty mp = new ResourceProperty(DavElements.E_MOUNTPOINT_TARGET_URL);
            mp.addChild(DavElements.E_HREF).setText(targetUrl);
            addProperty(mp);
        } catch (Exception e) {
        	ZimbraLog.dav.warn("can't get mountpoint target", e);
        }
    }
    
	public String getFreeBusyReport(DavContext ctxt, TimeRange range) throws ServiceException, DavException {
		return "";  // XXX implement free/busy check on shared calendars.
	}
}
