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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Aug 27, 2004
 */
package com.zimbra.cs.service.mail;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.WriteOpDocumentHandler;

/**
 * @author dkarp
 */
public class CreateFolder extends WriteOpDocumentHandler {

    private static Log sLog = LogFactory.getLog(SendMsg.class);

    private static final String[] TARGET_FOLDER_PATH = new String[] { MailService.E_FOLDER, MailService.A_FOLDER };
    private static final String[] RESPONSE_ITEM_PATH = new String[] { };
    protected String[] getProxiedIdPath(Element request)     { return TARGET_FOLDER_PATH; }
    protected boolean checkMountpointProxy(Element request)  { return true; }
    protected String[] getResponseItemPath()  { return RESPONSE_ITEM_PATH; }

    public Element handle(Element request, Map context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraContext(context);
        Mailbox mbox = getRequestedMailbox(lc);
        Mailbox.OperationContext octxt = lc.getOperationContext();

        Element t = request.getElement(MailService.E_FOLDER);
        String name      = t.getAttribute(MailService.A_NAME);
        String view      = t.getAttribute(MailService.A_DEFAULT_VIEW, null);
        String url       = t.getAttribute(MailService.A_URL, null);
        ItemId iidParent = new ItemId(t.getAttribute(MailService.A_FOLDER), lc);

        Folder folder;
        try {
            folder = mbox.createFolder(octxt, name, iidParent.getId(), MailItem.getTypeForName(view), url);
            if (!folder.getUrl().equals(""))
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
        } catch (ServiceException se) {
            if (se.getCode() == MailServiceException.ALREADY_EXISTS && t.getAttributeBool(MailService.A_FETCH_IF_EXISTS, false))
                folder = mbox.getFolderByName(lc.getOperationContext(), iidParent.getId(), name);
            else
                throw se;
        }

        Element response = lc.createElement(MailService.CREATE_FOLDER_RESPONSE);
        if (folder != null)
            ToXML.encodeFolder(response, lc, folder);
        return response;
    }

    private void rollbackFolder(Folder folder) {
        try {
            folder.getMailbox().delete(null, folder.getId(), MailItem.TYPE_FOLDER);
        } catch (ServiceException nse) {
            sLog.warn("error ignored while rolling back folder create", nse);
        }
    }
}
