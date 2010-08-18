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
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public abstract class MailDocumentHandler extends DocumentHandler {

    protected String[] getProxiedIdPath(Element request)     { return null; }
    protected boolean checkMountpointProxy(Element request)  { return false; }
    protected String[] getResponseItemPath()  { return null; }

    @Override
    protected Element proxyIfNecessary(Element request, Map<String, Object> context) throws ServiceException {
        // find the id of the item we're proxying on...
        String[] xpath = getProxiedIdPath(request);
        String id = (xpath != null ? getXPath(request, xpath) : null);

        if (id != null) {
            ZimbraSoapContext zsc = getZimbraSoapContext(context);
            OperationContext octxt = getOperationContext(zsc, context);
            ItemId iid = new ItemId(id, zsc);

            // if the "target item" is remote, proxy.
            ItemId iidTarget = getProxyTarget(zsc, octxt, iid, checkMountpointProxy(request));
            if (iidTarget != null)
                return proxyRequest(request, context, iid, iidTarget);
        }

        return super.proxyIfNecessary(request, context);
    }

    protected static ItemId getProxyTarget(ZimbraSoapContext zsc, OperationContext octxt, ItemId iid, boolean checkMountpoint) throws ServiceException {
        if (zsc == null || iid == null)
            return null;
        Account acct = getRequestedAccount(zsc);
        if (!iid.belongsTo(acct))
            return iid;

        if (!checkMountpoint || !Provisioning.onLocalServer(acct))
            return null;
        Mailbox mbox = getRequestedMailbox(zsc);
        MailItem item = mbox.getItemById(octxt, iid.getId(), MailItem.TYPE_UNKNOWN);
        if (!(item instanceof Mountpoint))
            return null;
        return ((Mountpoint) item).getTarget();
    }

    private void insertMountpointReferences(Element response, String[] xpath, ItemId iidMountpoint, ItemId iidLocal, ZimbraSoapContext lc) {
        int depth = 0;
        while (depth < xpath.length && response != null)
            response = response.getOptionalElement(xpath[depth++]);
        if (response == null)
            return;

        ItemIdFormatter ifmt = new ItemIdFormatter(lc);
        String local = iidLocal.toString(ifmt);
        for (Element elt : response.listElements()) {
            String folder = elt.getAttribute(MailConstants.A_FOLDER, null);
            if (local.equalsIgnoreCase(folder))
                elt.addAttribute(MailConstants.A_FOLDER, iidMountpoint.toString(ifmt));
        }
    }

    protected Element proxyRequest(Element request, Map<String, Object> context, ItemId iidRequested, ItemId iidResolved)
    throws ServiceException {
        // prepare the request for re-processing
        boolean mountpoint = iidRequested != iidResolved;
        if (mountpoint)
            setXPath(request, getProxiedIdPath(request), iidResolved.toString());

        Element response = proxyRequest(request, context, iidResolved.getAccountId(), mountpoint);

        // translate remote folder IDs back into local mountpoint IDs
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        String[] xpathResponse = getResponseItemPath();
        if (mountpoint && xpathResponse != null)
            insertMountpointReferences(response, xpathResponse, iidRequested, iidResolved, zsc);
        return response;
    }

    private Element proxyRequest(Element request, Map<String, Object> context, String acctId, boolean mountpoint)
    throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        // new context for proxied request has a different "requested account"
        ZimbraSoapContext zscTarget = new ZimbraSoapContext(zsc, acctId);
        if (mountpoint)
            zscTarget.recordMountpointTraversal();

        return proxyRequest(request, context, getServer(acctId), zscTarget);
    }
}
