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
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.operation.GetItemOperation;
import com.zimbra.cs.operation.Operation.Requester;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.session.Session;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class GetItem extends MailDocumentHandler {

    private static final String[] TARGET_ITEM_PATH = new String[] { MailConstants.E_ITEM, MailConstants.A_ID };
    private static final String[] TARGET_FOLDER_PATH = new String[] { MailConstants.E_ITEM, MailConstants.A_FOLDER };
    private static final String[] RESPONSE_ITEM_PATH = new String[] { };
    protected String[] getProxiedIdPath(Element request) {
        if (getXPath(request, TARGET_ITEM_PATH) != null)    return TARGET_ITEM_PATH;
        if (getXPath(request, TARGET_FOLDER_PATH) != null)  return TARGET_FOLDER_PATH;
        return null;
    }
    protected boolean checkMountpointProxy(Element request)  { return getXPath(request, TARGET_ITEM_PATH) == null; }
    protected String[] getResponseItemPath()  { return RESPONSE_ITEM_PATH; }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = zsc.getOperationContext();
        Session session = getSession(context);

        Element target = request.getElement(MailConstants.E_ITEM);
        String id = target.getAttribute(MailConstants.A_ID, null);
        String folder = target.getAttribute(MailConstants.A_FOLDER, null);
        String path = target.getAttribute(MailConstants.A_PATH, null);

        GetItemOperation op;
        if (id != null) {
            // get item by id
            op = new GetItemOperation(session, octxt, mbox, Requester.SOAP, new ItemId(id, zsc).getId(), MailItem.TYPE_UNKNOWN);
        } else if (folder != null) {
            // get item by name within containing folder id
            String name = target.getAttribute(MailConstants.A_NAME);
            op = new GetItemOperation(session, octxt, mbox, Requester.SOAP, name, new ItemId(folder, zsc).getId(), MailItem.TYPE_UNKNOWN);
        } else if (path != null) {
            // get item by user-root-relative absolute path
            op = new GetItemOperation(session, octxt, mbox, Requester.SOAP, path, MailItem.TYPE_UNKNOWN);
        } else {
            throw ServiceException.INVALID_REQUEST("must specify 'id' or 'path' or 'l' / 'name'", null);
        }
        op.schedule();
        MailItem item = op.getItem();

        Element response = zsc.createElement(MailConstants.GET_ITEM_RESPONSE);
        ToXML.encodeItem(response, zsc, item);
        return response;
    }
}
