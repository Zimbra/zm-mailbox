/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import com.zimbra.common.mailbox.Color;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @since Aug 27, 2004
 */
public class CreateFolder extends MailDocumentHandler {

    private static final String[] TARGET_FOLDER_PATH = new String[] { MailConstants.E_FOLDER, MailConstants.A_FOLDER };
    private static final String[] RESPONSE_ITEM_PATH = new String[] { };
    @Override protected String[] getProxiedIdPath(Element request)     { return TARGET_FOLDER_PATH; }
    @Override protected boolean checkMountpointProxy(Element request)  { return true; }
    @Override protected String[] getResponseItemPath()  { return RESPONSE_ITEM_PATH; }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

        Element t = request.getElement(MailConstants.E_FOLDER);
        String name      = t.getAttribute(MailConstants.A_NAME);
        String view      = t.getAttribute(MailConstants.A_DEFAULT_VIEW, null);
        String flags     = t.getAttribute(MailConstants.A_FLAGS, null);
        byte color       = (byte) t.getAttributeLong(MailConstants.A_COLOR, MailItem.DEFAULT_COLOR);
        String rgb       = t.getAttribute(MailConstants.A_RGB, null);
        String url       = t.getAttribute(MailConstants.A_URL, null);
        String folderId  = t.getAttribute(MailConstants.A_FOLDER, null);
        ItemId iidParent = folderId != null ? new ItemId(folderId, zsc) : null;
        boolean fetchIfExists = t.getAttributeBool(MailConstants.A_FETCH_IF_EXISTS, false);
        boolean syncToUrl = t.getAttributeBool(MailConstants.A_SYNC, true);
        ACL acl = FolderAction.parseACL(t.getOptionalElement(MailConstants.E_ACL), MailItem.Type.of(view),
                mbox.getAccount());

        Folder folder;
        boolean alreadyExisted = false;

        try {
            Folder.FolderOptions fopt = new Folder.FolderOptions();
            fopt.setColor(rgb != null ? new Color(rgb) : new Color(color)).setUrl(url);
            fopt.setDefaultView(MailItem.Type.of(view)).setFlags(Flag.toBitmask(flags));
            if (iidParent != null) {
                folder = mbox.createFolder(octxt, name, iidParent.getId(), fopt);
            } else {
                folder = mbox.createFolder(octxt, name, fopt);
            }

            if (!folder.getUrl().equals("") && syncToUrl) {
                try {
                    mbox.synchronizeFolder(octxt, folder.getId());
                } catch (ServiceException e) {
                    // if the synchronization fails, roll back the folder create
                    rollbackFolder(folder);
                    throw e;
                } catch (RuntimeException e) {
                    // if the synchronization fails, roll back the folder create
                    rollbackFolder(folder);
                    throw ServiceException.FAILURE("could not synchronize with remote feed", e);
                }
            }
        } catch (ServiceException se) {
            if (se.getCode() == MailServiceException.ALREADY_EXISTS && fetchIfExists) {
                if (iidParent != null) {
                    folder = mbox.getFolderByName(octxt, iidParent.getId(), name);
                } else {
                    folder = mbox.getFolderByPath(octxt, name);
                }
                alreadyExisted = true;
            } else {
                throw se;
            }
        }

        // set the folder ACL as a separate operation, when appropriate
        if (acl != null && !alreadyExisted) {
            try {
                mbox.setPermissions(octxt, folder.getId(), acl);
            } catch (ServiceException e) {
                try {
                    // roll back folder creation
                    mbox.delete(null, folder.getId(), MailItem.Type.FOLDER);
                } catch (ServiceException nse) {
                    ZimbraLog.soap.warn("error ignored while rolling back folder create", nse);
                }
                throw e;
            }
        }

        Element response = zsc.createElement(MailConstants.CREATE_FOLDER_RESPONSE);
        if (folder != null)
            ToXML.encodeFolder(response, ifmt, octxt, folder);
        return response;
    }

    private void rollbackFolder(Folder folder) {
        try {
            folder.getMailbox().delete(null, folder.getId(), MailItem.Type.FOLDER);
        } catch (ServiceException nse) {
            ZimbraLog.mailbox.warn("error ignored while rolling back folder create", nse);
        }
    }
}
