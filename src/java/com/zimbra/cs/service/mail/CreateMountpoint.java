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
/*
 * Created on Sep 23, 2005
 */
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.operation.CreateMountpointOperation;
import com.zimbra.cs.operation.Operation.Requester;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.session.Session;
import com.zimbra.soap.Element;
import com.zimbra.soap.WriteOpDocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author dkarp
 */
public class CreateMountpoint extends WriteOpDocumentHandler {

    private static final String[] TARGET_FOLDER_PATH = new String[] { MailService.E_MOUNT, MailService.A_FOLDER };
    private static final String[] RESPONSE_ITEM_PATH = new String[] { };
    protected String[] getProxiedIdPath(Element request)     { return TARGET_FOLDER_PATH; }
    protected boolean checkMountpointProxy(Element request)  { return true; }
    protected String[] getResponseItemPath()  { return RESPONSE_ITEM_PATH; }

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(lc);
        Mailbox.OperationContext octxt = lc.getOperationContext();
        Session session = getSession(context);

        Element t = request.getElement(MailService.E_MOUNT);
        String name      = t.getAttribute(MailService.A_NAME);
        String ownerId   = t.getAttribute(MailService.A_ZIMBRA_ID);
        int    remoteId  = (int) t.getAttributeLong(MailService.A_REMOTE_ID);
        String view      = t.getAttribute(MailService.A_DEFAULT_VIEW, null);
        ItemId iidParent = new ItemId(t.getAttribute(MailService.A_FOLDER), lc);
        boolean fetchIfExists = t.getAttributeBool(MailService.A_FETCH_IF_EXISTS, false);
        
        CreateMountpointOperation op = new CreateMountpointOperation(session, octxt, mbox, Requester.SOAP,
        			iidParent, name, ownerId, remoteId, view, fetchIfExists);
        op.schedule();
        Mountpoint mpt = op.getMountpoint();
        
        Element response = lc.createElement(MailService.CREATE_MOUNTPOINT_RESPONSE);
        if (mpt != null)
            ToXML.encodeMountpoint(response, lc, mpt);
        return response;
    }
}
