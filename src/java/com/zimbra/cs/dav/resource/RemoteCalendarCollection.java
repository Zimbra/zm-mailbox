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
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.caldav.TimeRange;
import com.zimbra.cs.dav.property.Acl;
import com.zimbra.cs.dav.property.ResourceProperty;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.calendar.cache.CtagInfo;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZMailbox;

public class RemoteCalendarCollection extends CalendarCollection implements DavResource.RemoteResource {

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

    public ItemId getTarget() {
        return new ItemId(mRemoteOwnerId, mRemoteId);
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
    public java.util.Collection<DavResource> getChildren(DavContext ctxt, java.util.Collection<String> hrefs, TimeRange range) throws DavException {
        throw new DavException("request should be proxied", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);
    }

    String getCalendarData(String uid) {
        if (mCalendarData != null)
            return mCalendarData.get(uid);
        return null;
    }

    @Override
    protected Map<String,DavResource> getAppointmentMap(DavContext ctxt, TimeRange range) throws DavException {
        throw new DavException("request should be proxied", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);
    }

    @Override
    public DavResource createItem(DavContext ctxt, String name) throws DavException, IOException {
        throw new DavException("request should be proxied", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);
    }

    public DavResource getAppointment(DavContext ctxt, String uid) throws DavException {
        return getAppointmentMap(ctxt, null).get(uid);
    }

    public void deleteAppointment(DavContext ctxt, int id) throws DavException {
        throw new DavException("request should be proxied", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);
    }

    @Override
    public int getId() {
        return mRemoteId;
    }

    @Override
    protected Mailbox getCalendarMailbox(DavContext ctxt) throws ServiceException, DavException {
        if (isLocal())
            return MailboxManager.getInstance().getMailboxById(mMailboxId);
        return super.getMailbox(ctxt);
    }

    private void getMountpointTarget(DavContext ctxt) {
        try {
            ZAuthToken zat = AuthProvider.getAuthToken(ctxt.getAuthAccount()).toZAuthToken();
            ZMailbox zmbx = RemoteCollection.getRemoteMailbox(zat, mRemoteOwnerId);
            ZFolder folder = zmbx.getFolder(new ItemId(mRemoteOwnerId, mRemoteId).toString(mOwnerId));
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

    @Override
    public String getFreeBusyReport(DavContext ctxt, TimeRange range) throws ServiceException, DavException {
        return "";  // XXX implement free/busy check on shared calendars.
    }
}
