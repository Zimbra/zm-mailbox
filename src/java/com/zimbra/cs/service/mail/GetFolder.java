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
 * Created on Aug 31, 2004
 */
package com.zimbra.cs.service.mail;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author dkarp
 */
public class GetFolder extends DocumentHandler {

    private static final String[] TARGET_FOLDER_PATH = new String[] { MailService.E_FOLDER, MailService.A_FOLDER };
    private static final String[] RESPONSE_ITEM_PATH = new String[] { MailService.E_FOLDER };
    protected String[] getProxiedIdPath(Element request)     { return TARGET_FOLDER_PATH; }
    protected boolean checkMountpointProxy(Element request)  { return false; }
    protected String[] getResponseItemPath()  { return RESPONSE_ITEM_PATH; }

    static final String DEFAULT_FOLDER_ID = "" + Mailbox.ID_FOLDER_USER_ROOT;

	public Element handle(Element request, Map context) throws ServiceException, SoapFaultException {
		ZimbraSoapContext lc = getZimbraContext(context);
        Mailbox mbox = getRequestedMailbox(lc);
        Mailbox.OperationContext octxt = lc.getOperationContext();

        String parentId = DEFAULT_FOLDER_ID;
        Element eFolder = request.getOptionalElement(MailService.E_FOLDER);
        if (eFolder != null)
        	parentId = eFolder.getAttribute(MailService.A_FOLDER, DEFAULT_FOLDER_ID);
        ItemId iid = new ItemId(parentId, lc);

        Folder folder = mbox.getFolderById(octxt, iid.getId());
        if (folder == null)
        	throw MailServiceException.NO_SUCH_FOLDER(iid.getId());

        Element response = lc.createElement(MailService.GET_FOLDER_RESPONSE), eRoot;
        synchronized (mbox) {
            eRoot = handleFolder(mbox, folder, response, lc, octxt);
        }

        // if the requested root folder is a link, execute the request remotely
        if (folder instanceof Mountpoint)
            handleMountpoint(request, context, iid, (Mountpoint) folder, eRoot);

        return response;
	}

	public static Element handleFolder(Mailbox mbox, Folder folder, Element response, ZimbraSoapContext lc, OperationContext octxt)
    throws ServiceException {
		Element respFolder = ToXML.encodeFolder(response, lc, folder);

        List subfolders = folder.getSubfolders(octxt);
        if (subfolders != null)
	        for (Iterator it = subfolders.iterator(); it.hasNext(); ) {
	        	Folder subfolder = (Folder) it.next();
	        	if (subfolder != null)
	        		handleFolder(mbox, subfolder, respFolder, lc, octxt);
        }
        return respFolder;
	}

    private void handleMountpoint(Element request, Map context, ItemId iidLocal, Mountpoint mpt, Element eRoot)
    throws ServiceException, SoapFaultException {
        ItemId iidRemote = new ItemId(mpt.getOwnerId(), mpt.getRemoteId());
        Element proxied = proxyRequest(request, context, iidLocal, iidRemote);
        // return the children of the remote folder as children of the mountpoint
        proxied = proxied.getOptionalElement(MailService.E_FOLDER);
        if (proxied != null) {
            eRoot.addAttribute(MailService.A_RIGHTS, proxied.getAttribute(MailService.A_RIGHTS, null));
            for (Iterator it = proxied.elementIterator(); it.hasNext(); ) {
                Element eRemote = (Element) it.next();
                // skip the <acl> element, if any
                if (!eRemote.getName().equals(MailService.E_ACL))
                    eRoot.addElement(eRemote.detach());
            }
        }
    }
}
