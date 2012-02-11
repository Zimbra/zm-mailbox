/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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
/*
 * Created on Sep 23, 2005
 */
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.mailbox.Color;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.soap.ZimbraSoapContext;

public class CreateMountpoint extends MailDocumentHandler {

    private static final String[] TARGET_FOLDER_PATH = new String[] { MailConstants.E_MOUNT, MailConstants.A_FOLDER };
    private static final String[] RESPONSE_ITEM_PATH = new String[] { };

    @Override
    protected String[] getProxiedIdPath(Element request) {
        return TARGET_FOLDER_PATH;
    }

    @Override
    protected boolean checkMountpointProxy(Element request) {
        return true;
    }

    @Override
    protected String[] getResponseItemPath() {
        return RESPONSE_ITEM_PATH;
    }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

        Element t = request.getElement(MailConstants.E_MOUNT);
        String name = t.getAttribute(MailConstants.A_NAME);
        String view = t.getAttribute(MailConstants.A_DEFAULT_VIEW, null);
        String flags = t.getAttribute(MailConstants.A_FLAGS, null);
        byte color       = (byte) t.getAttributeLong(MailConstants.A_COLOR, MailItem.DEFAULT_COLOR);
        String rgb       = t.getAttribute(MailConstants.A_RGB, null);
        ItemId iidParent = new ItemId(t.getAttribute(MailConstants.A_FOLDER), zsc);
        boolean fetchIfExists = t.getAttributeBool(MailConstants.A_FETCH_IF_EXISTS, false);
        boolean reminderEnabled = t.getAttributeBool(MailConstants.A_REMINDER, false);

        Account target = null;
        String ownerId = t.getAttribute(MailConstants.A_ZIMBRA_ID, null);
        if (ownerId == null) {
            String ownerName = t.getAttribute(MailConstants.A_OWNER_NAME);
            target = Provisioning.getInstance().get(AccountBy.name, ownerName, zsc.getAuthToken());

            // prevent directory harvest attack, mask no such account as permission denied
            if (target == null)
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions");

            ownerId = target.getId();
            if (ownerId.equalsIgnoreCase(zsc.getRequestedAccountId()))
                throw ServiceException.INVALID_REQUEST("cannot mount your own folder", null);
        }

        Element remote = fetchRemoteFolder(zsc, context, ownerId, (int) t.getAttributeLong(MailConstants.A_REMOTE_ID, -1), t.getAttribute(MailConstants.A_PATH, null));
        int remoteId = new ItemId(remote.getAttribute(MailConstants.A_ID), zsc).getId();
        String remoteUuid = remote.getAttribute(MailConstants.A_UUID, null);
        if (view == null)
            view = remote.getAttribute(MailConstants.A_DEFAULT_VIEW, null);

        Mountpoint mpt;
        try {
            Color itemColor = rgb != null ? new Color(rgb) : new Color(color);
            mpt = mbox.createMountpoint(octxt, iidParent.getId(), name, ownerId, remoteId, remoteUuid, MailItem.Type.of(view),
                    Flag.toBitmask(flags), itemColor, reminderEnabled);
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
            Element eMount = ToXML.encodeMountpoint(response, ifmt, octxt, mpt);
            // transfer folder counts and subfolders to the serialized mountpoint from the serialized target folder
            ToXML.transferMountpointContents(eMount, remote);
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
