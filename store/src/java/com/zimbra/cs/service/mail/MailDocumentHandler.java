/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
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
import com.zimbra.soap.ZimbraSoapContext;

public abstract class MailDocumentHandler extends DocumentHandler {

    protected String[] getProxiedIdPath(Element request) {
        return null;
    }

    protected boolean checkMountpointProxy(Element request) {
        return false;
    }

    protected String[] getResponseItemPath() {
        return null;
    }

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
            if (iidTarget != null) {
                return proxyRequest(request, context, iid, iidTarget);
            }
        }

        return super.proxyIfNecessary(request, context);
    }

    protected static ItemId getProxyTarget(ZimbraSoapContext zsc, OperationContext octxt, ItemId iid, boolean checkMountpoint) throws ServiceException {
        if (zsc == null || iid == null) {
            return null;
        }
        Account acct = getRequestedAccount(zsc);
        if (!iid.belongsTo(acct)) {
            return iid;
        }

        if (!checkMountpoint || !DocumentHandler.onLocalServer(acct)) {
            return null;
        }
        Mailbox mbox = getRequestedMailbox(zsc);
        MailItem item = mbox.getItemById(octxt, iid.getId(), MailItem.Type.UNKNOWN);
        if (!(item instanceof Mountpoint)) {
            return null;
        }
        return ((Mountpoint) item).getTarget();
    }

    private void insertMountpointReferences(Element response, String[] xpath, ItemId iidMountpoint, ItemId iidLocal, ZimbraSoapContext lc) {
        int depth = 0;
        while (depth < xpath.length && response != null) {
            response = response.getOptionalElement(xpath[depth++]);
        }
        if (response == null) {
            return;
        }

        ItemIdFormatter ifmt = new ItemIdFormatter(lc);
        String local = iidLocal.toString(ifmt);
        for (Element elt : response.listElements()) {
            String folder = elt.getAttribute(MailConstants.A_FOLDER, null);
            if (local.equalsIgnoreCase(folder)) {
                elt.addAttribute(MailConstants.A_FOLDER, iidMountpoint.toString(ifmt));
            }
        }
    }

    protected Element proxyRequest(Element request, Map<String, Object> context, ItemId iidRequested, ItemId iidResolved)
    throws ServiceException {
        // prepare the request for re-processing
        boolean mountpoint = iidRequested != iidResolved;
        if (mountpoint) {
            setXPath(request, getProxiedIdPath(request), iidResolved.toString());
        }

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
        if (mountpoint) {
            zscTarget.recordMountpointTraversal();
        }
        Provisioning prov = Provisioning.getInstance();
        Account acct = prov.getAccount(prov, AccountBy.id, acctId, zsc.getAuthToken());
        String affinityIp = Provisioning.affinityServer(acct);

        return proxyRequest(request, context, affinityIp, zscTarget);
    }
}
