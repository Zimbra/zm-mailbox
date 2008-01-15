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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Header;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.dav.service.DavServlet;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.UserServlet.HttpInputStream;
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
    public InputStream getContent(DavContext ctxt) throws IOException,
            DavException {
        return null;
    }

    @Override
    public boolean isCollection() {
        return true;
    }

    @Override
    public java.util.Collection<DavResource> getChildren(DavContext ctxt) throws DavException {
        if (mChildren != null)
            return mChildren;
        
        mAppointments = new HashMap<String,DavResource>();
        String authToken = null;
        try {
            authToken = new AuthToken(ctxt.getAuthAccount()).getEncoded();
        } catch (AuthTokenException e) {
            return Collections.emptyList();
        }

        List<ZApptSummaryResult> results;
        
        try {
            Account target = Provisioning.getInstance().get(Provisioning.AccountBy.id, mRemoteId);
            ZMailbox.Options zoptions = new ZMailbox.Options(authToken, AccountUtil.getSoapUri(target));
            zoptions.setNoSession(true);
            zoptions.setTargetAccount(mRemoteId);
            zoptions.setTargetAccountBy(Provisioning.AccountBy.id);
            ZMailbox zmbx = ZMailbox.getMailbox(zoptions);
            long startTime = System.currentTimeMillis() - Constants.MILLIS_PER_MONTH;
            long endTime = System.currentTimeMillis() + Constants.MILLIS_PER_MONTH * 12;
            String folderId = Integer.toString(mItemId);
            results = zmbx.getApptSummaries(null, startTime, endTime, new String[] {folderId}, TimeZone.getDefault(), ZSearchParams.TYPE_APPOINTMENT);
        } catch (ServiceException e) {
            return Collections.emptyList();
        }
        
        mChildren = new ArrayList<DavResource>();
        for (ZAppointmentHit appt : results.get(0).getAppointments()) {
            DavResource res = mAppointments.get(appt.getUid().toLowerCase());
            if (res != null)
                continue;
            res = new CalendarObject.RemoteCalendarObject(mUri, mOwner, appt, this);
            mChildren.add(res);
            mAppointments.put(appt.getUid().toLowerCase(), res);
        }
        
        return mChildren;
    }
    
    @Override
    public DavResource createItem(DavContext ctxt, String name) throws DavException, IOException {
        String authToken;
        try {
            authToken = new AuthToken(ctxt.getAuthAccount()).getEncoded();
            
            Account target = Provisioning.getInstance().get(Provisioning.AccountBy.id, mRemoteId);
            ZMailbox.Options zoptions = new ZMailbox.Options(authToken, AccountUtil.getSoapUri(target));
            zoptions.setNoSession(true);
            zoptions.setTargetAccount(mRemoteId);
            zoptions.setTargetAccountBy(Provisioning.AccountBy.id);
            ZMailbox zmbx = ZMailbox.getMailbox(zoptions);
            ZFolder f = zmbx.getFolderById(new ItemId(mRemoteId, mItemId).toString());
            
            byte[] data = ctxt.getRequestData();
            Enumeration reqHeaders = ctxt.getRequest().getHeaderNames();
            ArrayList<Header> headerList = new ArrayList<Header>();
            while (reqHeaders.hasMoreElements()) {
                String hdr = (String)reqHeaders.nextElement();
                if (!hdr.equals("Host") && !hdr.equals("Cookie"))
                    headerList.add(new Header(hdr, ctxt.getRequest().getHeader(hdr)));
            }
            String url = UrlNamespace.urlEscape(f.getPath() + "/" + ctxt.getItem());
            url = DavServlet.getDavUrl(target.getName()) + url;
            Pair<Header[], HttpInputStream> response = UserServlet.putRemoteResource(authToken, url, target, data, headerList.toArray(new Header[0]));
            for (Header h : response.getFirst())
                if (h.getName().equals(DavProtocol.HEADER_ETAG)) {
                    UrlNamespace.invalidateApptSummariesCache(mRemoteId, mItemId);
                    return new CalendarObject.RemoteCalendarObject(ctxt.getPath(), ctxt.getUser(), h.getValue(), this);
                }
        } catch (AuthTokenException e) {
            ZimbraLog.dav.warn("can't generate authToken for "+ctxt.getAuthAccount().getName(), e);
        } catch (ServiceException e) {
            ZimbraLog.dav.warn("can't create resource "+name, e);
        }
        throw new DavException("can't create resource", HttpServletResponse.SC_FORBIDDEN);
    }
    
    public DavResource getAppointment(DavContext ctxt, String uid) throws DavException {
        if (mAppointments == null) {
            getChildren(ctxt);
        }
        return mAppointments.get(uid.toLowerCase());
    }
    
    public void deleteAppointment(DavContext ctxt, int id) throws DavException {
        try {
            String authToken = null;
            authToken = new AuthToken(ctxt.getAuthAccount()).getEncoded();
            Account target = Provisioning.getInstance().get(Provisioning.AccountBy.id, mRemoteId);
            ZMailbox.Options zoptions = new ZMailbox.Options(authToken, AccountUtil.getSoapUri(target));
            zoptions.setNoSession(true);
            zoptions.setTargetAccount(mRemoteId);
            zoptions.setTargetAccountBy(Provisioning.AccountBy.id);
            ZMailbox zmbx = ZMailbox.getMailbox(zoptions);
            zmbx.deleteItem(Integer.toString(id), null);
        } catch (AuthTokenException e) {
            throw new DavException("can't delete", HttpServletResponse.SC_FORBIDDEN, e);
        } catch (ServiceException e) {
            throw new DavException("can't delete", HttpServletResponse.SC_FORBIDDEN, e);
        }
    }
}
