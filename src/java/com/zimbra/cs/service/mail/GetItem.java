/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
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

    @Override public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

        Element target = request.getElement(MailConstants.E_ITEM);
        String id = target.getAttribute(MailConstants.A_ID, null);
        String folder = target.getAttribute(MailConstants.A_FOLDER, null);
        String path = target.getAttribute(MailConstants.A_PATH, null);

        MailItem item;
        if (id != null) {
            // get item by id
            item = mbox.getItemById(octxt, new ItemId(id, zsc).getId(), MailItem.TYPE_UNKNOWN);
        } else if (folder != null) {
            // get item by name within containing folder id
            String name = target.getAttribute(MailConstants.A_NAME);
            item = mbox.getItemByPath(octxt, name, new ItemId(folder, zsc).getId());
        } else if (path != null) {
            // get item by user-root-relative absolute path
            item = mbox.getItemByPath(octxt, path); 
        } else {
            throw ServiceException.INVALID_REQUEST("must specify 'id' or 'path' or 'l' / 'name'", null);
        }

        Element response = zsc.createElement(MailConstants.GET_ITEM_RESPONSE);
        ToXML.encodeItem(response, ifmt, octxt, item, ToXML.NOTIFY_FIELDS);
        return response;
    }
}
