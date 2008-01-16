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

import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZMailbox;

public class RemoteCollection extends Collection {

    private String mRemoteId;
    private int mItemId;
    private ArrayList<DavResource> mChildren;
    
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
        
        String authToken = null;
        try {
            authToken = new AuthToken(ctxt.getAuthAccount()).getEncoded();
        } catch (AuthTokenException e) {
            return Collections.emptyList();
        }

        mChildren = new ArrayList<DavResource>();
        /*
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
}
