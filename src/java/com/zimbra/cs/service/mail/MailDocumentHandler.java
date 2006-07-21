/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.mail;

import java.util.Iterator;
import java.util.Map;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public abstract class MailDocumentHandler extends DocumentHandler {

    protected String[] getProxiedIdPath(Element request)     { return null; }
    protected boolean checkMountpointProxy(Element request)  { return false; }
    protected String[] getResponseItemPath()  { return null; }

    protected Element proxyIfNecessary(Element request, Map<String, Object> context) throws ServiceException {
        // find the id of the item we're proxying on...
        String[] xpath = getProxiedIdPath(request);
        String id = (xpath != null ? getXPath(request, xpath) : null);

        if (id != null) {
            ZimbraSoapContext lc = getZimbraSoapContext(context);
            ItemId iid = new ItemId(id, lc);
    
            // if the "target item" is remote, proxy.
            ItemId iidTarget = getProxyTarget(lc, iid, checkMountpointProxy(request));
            if (iidTarget != null)
                return proxyRequest(request, context, iid, iidTarget);
        }

        return super.proxyIfNecessary(request, context);
    }

    protected static ItemId getProxyTarget(ZimbraSoapContext lc, ItemId iid, boolean checkMountpoint) throws ServiceException {
        if (lc == null || iid == null)
            return null;
        Account acct = getRequestedAccount(lc);
        if (!iid.belongsTo(acct))
            return iid;

        if (!checkMountpoint || !Provisioning.onLocalServer(acct))
            return null;
        Mailbox mbox = getRequestedMailbox(lc);
        MailItem item = mbox.getItemById(lc.getOperationContext(), iid.getId(), MailItem.TYPE_FOLDER);
        if (!(item instanceof Mountpoint))
            return null;
        Mountpoint mpt = (Mountpoint) item;
        return new ItemId(mpt.getOwnerId(), mpt.getRemoteId());
    }

    private void insertMountpointReferences(Element response, String[] xpath, ItemId iidMountpoint, ItemId iidLocal, ZimbraSoapContext lc) {
        int depth = 0;
        while (depth < xpath.length && response != null)
            response = response.getOptionalElement(xpath[depth++]);
        if (response == null)
            return;
        String local = iidLocal.toString(lc);
        for (Iterator it = response.elementIterator(); it.hasNext(); ) {
            Element elt = (Element) it.next();
            String folder = elt.getAttribute(MailService.A_FOLDER, null);
            if (local.equalsIgnoreCase(folder))
                elt.addAttribute(MailService.A_FOLDER, iidMountpoint.toString(lc));
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
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        String[] xpathResponse = getResponseItemPath();
        if (mountpoint && xpathResponse != null) 
            insertMountpointReferences(response, xpathResponse, iidRequested, iidResolved, lc);
        return response;
    }

    private static Element proxyRequest(Element request, Map<String, Object> context, String acctId, boolean mountpoint)
    throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        // new context for proxied request has a different "requested account"
        ZimbraSoapContext lcTarget = new ZimbraSoapContext(lc, acctId);
        if (mountpoint)
            lcTarget.recordMountpointTraversal();

        return proxyRequest(request, context, getServer(acctId), lcTarget);
    }
}
