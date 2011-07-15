/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.service.method;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.dav.resource.Collection;
import com.zimbra.cs.dav.resource.DavResource;
import com.zimbra.cs.dav.resource.MailItemResource;
import com.zimbra.cs.dav.resource.Notebook;
import com.zimbra.cs.dav.resource.UrlNamespace;
import com.zimbra.cs.dav.resource.UrlNamespace.UrlComponents;
import com.zimbra.cs.dav.service.DavMethod;
import com.zimbra.cs.dav.service.DavServlet;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZMailbox;

public class Move extends DavMethod {
    public static final String MOVE  = "MOVE";
    public String getName() {
        return MOVE;
    }

    public void handle(DavContext ctxt) throws DavException, IOException, ServiceException {
        DavResource rs = ctxt.getRequestedResource();
        if (!(rs instanceof MailItemResource))
            throw new DavException("cannot move", HttpServletResponse.SC_BAD_REQUEST, null);
        Collection col = getDestinationCollection(ctxt);
        MailItemResource mir = (MailItemResource) rs;
        String newName = getNewName(ctxt, mir);
        if (ctxt.isOverwriteSet()) {
            mir.moveORcopyWithOverwrite(ctxt, col, newName, true);
        } else {
            mir.move(ctxt, col, newName);
        }
        ctxt.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    protected String getNewName(DavContext ctxt, DavResource rs) throws DavException {        
        if (!(rs instanceof Collection) && !(rs instanceof Notebook))
            return null;
        String oldName = ctxt.getItem();
        String dest = getDestination(ctxt);
        int begin, end;
        end = dest.length();
        if (dest.endsWith("/"))
            end--;
        begin = dest.lastIndexOf("/", end-1);
        String newName = dest.substring(begin+1, end);
        try {
            newName = URLDecoder.decode(newName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            ZimbraLog.dav.warn("can't decode URL ", dest, e);
        }
        if (oldName.equals(newName) == false)
            return newName;
        else 
            return null;
    }

    protected String getDestination(DavContext ctxt) throws DavException {
        String destination = ctxt.getRequest().getHeader(DavProtocol.HEADER_DESTINATION);
        if (destination == null)
            throw new DavException("no destination specified", HttpServletResponse.SC_BAD_REQUEST, null);
        return destination;
    }
    protected Collection getDestinationCollection(DavContext ctxt) throws DavException {
        String destinationUrl = getDestination(ctxt);
        if (!destinationUrl.endsWith("/")) {
            int slash = destinationUrl.lastIndexOf('/');
            destinationUrl = destinationUrl.substring(0, slash+1);
        }
        try {
            destinationUrl = getInternalDestinationUrl(ctxt, destinationUrl);
            DavResource r = UrlNamespace.getResourceAtUrl(ctxt, destinationUrl);
            if (r instanceof Collection)
                return ((Collection)r);
            return UrlNamespace.getCollectionAtUrl(ctxt, destinationUrl);
        } catch (Exception e) {
            throw new DavException("can't get destination collection", DavProtocol.STATUS_FAILED_DEPENDENCY);
        }
    }
    
    private static String getInternalDestinationUrl(DavContext ctxt, String destinationUrl) throws ServiceException, DavException {
        UrlComponents uc = UrlNamespace.parseUrl(destinationUrl);
        Account targetAcct = Provisioning.getInstance().getAccountByName(uc.user);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(targetAcct);

        Pair<Folder, String> match = mbox.getFolderByPathLongestMatch(ctxt.getOperationContext(), Mailbox.ID_FOLDER_USER_ROOT, uc.path);
        Folder targetFolder = match.getFirst();
        if (targetFolder instanceof Mountpoint) {
            Mountpoint mp = (Mountpoint) targetFolder;
            ItemId target = new ItemId(mp.getOwnerId(), mp.getRemoteId());
            Account acct = Provisioning.getInstance().getAccountById(mp.getOwnerId());
            
            AuthToken authToken = AuthProvider.getAuthToken(ctxt.getAuthAccount());
            ZMailbox.Options zoptions = new ZMailbox.Options(authToken.toZAuthToken(), AccountUtil.getSoapUri(acct));
            zoptions.setNoSession(true);
            zoptions.setTargetAccount(target.getAccountId());
            zoptions.setTargetAccountBy(Key.AccountBy.id);
            
            ZMailbox zmbx = ZMailbox.getMailbox(zoptions);
            ZFolder f = zmbx.getFolderById("" + target.toString());
            String extraPath = match.getSecond();
            destinationUrl = HttpUtil.urlEscape(DavServlet.DAV_PATH + "/" + acct.getName() + f.getPath() + ((extraPath != null) ? "/" + extraPath : ""));
        }
        return destinationUrl;
    }
}
