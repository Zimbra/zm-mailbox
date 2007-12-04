/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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
/*
 * Created on Sep 23, 2005
 */
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author dkarp
 */
public class CreateMountpoint extends MailDocumentHandler {

    private static final String[] TARGET_FOLDER_PATH = new String[] { MailConstants.E_MOUNT, MailConstants.A_FOLDER };
    private static final String[] RESPONSE_ITEM_PATH = new String[] { };
    protected String[] getProxiedIdPath(Element request)     { return TARGET_FOLDER_PATH; }
    protected boolean checkMountpointProxy(Element request)  { return true; }
    protected String[] getResponseItemPath()  { return RESPONSE_ITEM_PATH; }

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

        Element t = request.getElement(MailConstants.E_MOUNT);
        String name = t.getAttribute(MailConstants.A_NAME);
        String view = t.getAttribute(MailConstants.A_DEFAULT_VIEW, null);
        String flags = t.getAttribute(MailConstants.A_FLAGS, null);
        byte color = (byte) t.getAttributeLong(MailConstants.A_COLOR, MailItem.DEFAULT_COLOR);
        ItemId iidParent = new ItemId(t.getAttribute(MailConstants.A_FOLDER), zsc);
        boolean fetchIfExists = t.getAttributeBool(MailConstants.A_FETCH_IF_EXISTS, false);

        Account target = null;
        String ownerId = t.getAttribute(MailConstants.A_ZIMBRA_ID, null);
        if (ownerId == null) {
            String ownerName = t.getAttribute(MailConstants.A_OWNER_NAME);
            target = Provisioning.getInstance().get(AccountBy.name, ownerName);
            if (target == null)
                throw AccountServiceException.NO_SUCH_ACCOUNT(ownerName);

            ownerId = target.getId();
            if (ownerId.equalsIgnoreCase(zsc.getRequestedAccountId()))
                throw ServiceException.INVALID_REQUEST("cannot mount your own folder", null);
        }

        Element remote = fetchRemoteFolder(zsc, context, ownerId, (int) t.getAttributeLong(MailConstants.A_REMOTE_ID, -1), t.getAttribute(MailConstants.A_PATH, null));
        int remoteId = new ItemId(remote.getAttribute(MailConstants.A_ID), zsc).getId();
        if (view == null)
            view = remote.getAttribute(MailConstants.A_DEFAULT_VIEW, null);

        Mountpoint mpt;
        try {
            mpt = mbox.createMountpoint(octxt, iidParent.getId(), name, ownerId, remoteId, MailItem.getTypeForName(view), Flag.flagsToBitmask(flags), color);
        } catch (ServiceException se) {
            if (se.getCode() == MailServiceException.ALREADY_EXISTS && fetchIfExists) {
                Folder folder = mbox.getFolderByName(octxt, iidParent.getId(), name);
                if (folder instanceof Mountpoint)
                    mpt = (Mountpoint) folder;
                else
                    throw se;
            } else {
                throw se;
            }
        }

        Element response = zsc.createElement(MailConstants.CREATE_MOUNTPOINT_RESPONSE);
        if (mpt != null) {
            Element eMount = ToXML.encodeMountpoint(response, ifmt, mpt);
            eMount.addAttribute(MailConstants.A_UNREAD, remote.getAttribute(MailConstants.A_UNREAD, null));
            eMount.addAttribute(MailConstants.A_NUM, remote.getAttribute(MailConstants.A_NUM, null));
            eMount.addAttribute(MailConstants.A_SIZE, remote.getAttribute(MailConstants.A_SIZE, null));
            eMount.addAttribute(MailConstants.A_URL, remote.getAttribute(MailConstants.A_URL, null));
            eMount.addAttribute(MailConstants.A_RIGHTS, remote.getAttribute(MailConstants.A_RIGHTS, null));
            if (remote.getAttribute(MailConstants.A_FLAGS, "").indexOf("u") != -1)
                eMount.addAttribute(MailConstants.A_FLAGS, "u" + eMount.getAttribute(MailConstants.A_FLAGS, "").replace("u", ""));
        }
        return response;
    }

    private Element fetchRemoteFolder(ZimbraSoapContext zsc, Map<String, Object> context, String ownerId, int remoteId, String remotePath)
    throws ServiceException {
        Element request = zsc.createRequestElement(MailConstants.GET_FOLDER_REQUEST);
        if (remoteId > 0)
            request.addElement(MailConstants.E_FOLDER).addAttribute(MailConstants.A_FOLDER, remoteId);
        else if (remotePath != null)
            request.addElement(MailConstants.E_FOLDER).addAttribute(MailConstants.A_PATH, remotePath);
        else
            throw ServiceException.INVALID_REQUEST("must specify one of rid/path", null);

        Element response = proxyRequest(request, context, ownerId);
        Element remote = response.getOptionalElement(MailConstants.E_FOLDER);
        if (remote == null)
            throw ServiceException.INVALID_REQUEST("cannot mount a search or mountpoint", null);
        return remote;
    }
}
