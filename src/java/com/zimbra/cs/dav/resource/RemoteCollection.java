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
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.zclient.ZAppointmentHit;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZSearchParams;
import com.zimbra.cs.zclient.ZMailbox.ZApptSummaryResult;

public class RemoteCollection extends CalendarCollection {

    private String mRemoteId;
    private int mItemId;
    private ArrayList<DavResource> mChildren;
    private HashMap<String,DavResource> mAppointments;
    
    public RemoteCollection(DavContext ctxt, Mountpoint mp) throws DavException, ServiceException {
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
        String authtoken = null;
        try {
            authtoken = new AuthToken(ctxt.getAuthAccount()).getEncoded();
        } catch (AuthTokenException e) {
            return Collections.emptyList();
        }

        List<ZApptSummaryResult> results;
        
        try {
            Account target = Provisioning.getInstance().get(Provisioning.AccountBy.id, mRemoteId);
            ZMailbox.Options zoptions = new ZMailbox.Options(authtoken, AccountUtil.getSoapUri(target));
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
            res = new CalendarObject.RemoteCalendarObject(mUri, mOwner, appt);
            mChildren.add(res);
            mAppointments.put(appt.getUid().toLowerCase(), res);
        }
        
        return mChildren;
    }
    
    public DavResource getAppointment(DavContext ctxt, String uid) throws DavException {
        if (mAppointments == null) {
            getChildren(ctxt);
        }
        return mAppointments.get(uid.toLowerCase());
    }
}
