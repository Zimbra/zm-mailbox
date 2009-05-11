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

import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.property.Acl;
import com.zimbra.cs.dav.property.ResourceProperty;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZMailbox;

public class RemoteCollection extends Collection {

    private String mRemoteOwnerId;
    private int mRemoteId;
    private ArrayList<DavResource> mChildren;
    private String mCtag;
    
    public RemoteCollection(DavContext ctxt, Mountpoint mp) throws DavException, ServiceException {
        super(ctxt, mp);
        mRemoteOwnerId = mp.getOwnerId();
        mRemoteId = mp.getRemoteId();
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

    @Override
    public java.util.Collection<DavResource> getChildren(DavContext ctxt) throws DavException {
        if (mChildren != null)
            return mChildren;
        
        mChildren = new ArrayList<DavResource>();
        /*
        ZAuthToken authToken = null;
        try {
            authToken = AuthProvider.getAuthToken(ctxt.getAuthAccount()).toZAuthToken();
        } catch (AuthProviderException e) {
            return Collections.emptyList();
        } catch (ServiceException e) {
            return Collections.emptyList();
        }

        try {
            Account target = Provisioning.getInstance().get(Provisioning.AccountBy.id, mRemoteId);
            ZMailbox.Options zoptions = new ZMailbox.Options(authToken, AccountUtil.getSoapUri(target));
            zoptions.setNoSession(true);
            zoptions.setTargetAccount(mRemoteId);
            zoptions.setTargetAccountBy(Provisioning.AccountBy.id);
            ZMailbox zmbx = ZMailbox.getMailbox(zoptions);
            ZFolder folder = zmbx.getFolderById(Integer.toString(mItemId));
            for (ZFolder f : folder.getSubFolders()) {
            	// XXX subfolder of a Mountpoint?
            	//DavResource res = new RemoteCollection(ctxt, f.getId());
            	//mChildren.add(res);
            }
        } catch (ServiceException e) {
            return Collections.emptyList();
        }
        */
        return mChildren;
    }
    
    @Override
    public DavResource createItem(DavContext ctxt, String name) throws DavException, IOException {
        throw new DavException("can't create resource", HttpServletResponse.SC_FORBIDDEN);
    }
    static ZMailbox getRemoteMailbox(ZAuthToken zat, String ownerId) throws ServiceException {
        Account target = Provisioning.getInstance().get(Provisioning.AccountBy.id, ownerId);
        if (target == null)
        	return null;
        ZMailbox.Options zoptions = new ZMailbox.Options(zat, AccountUtil.getSoapUri(target));
        zoptions.setNoSession(true);
        zoptions.setTargetAccount(ownerId);
        zoptions.setTargetAccountBy(Provisioning.AccountBy.id);
        return ZMailbox.getMailbox(zoptions);
    }
    private void getMountpointTarget(DavContext ctxt) {
        try {
            ZAuthToken zat = AuthProvider.getAuthToken(ctxt.getAuthAccount()).toZAuthToken();
            ZMailbox zmbx = getRemoteMailbox(zat, mRemoteOwnerId);
            if (zmbx == null)
            	return;
            ZFolder folder = zmbx.getFolderById(new ItemId(mRemoteOwnerId, mRemoteId).toString(mOwnerId));
            if (folder == null)
            	return;
            mCtag = "" + folder.getImapMODSEQ();
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
}
