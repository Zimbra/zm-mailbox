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
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.mail;

import java.util.ArrayList;
import java.util.Map;

import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class GetContacts extends DocumentHandler  {

    private static final int ALL_FOLDERS = -1;

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        Mailbox.OperationContext octxt = zsc.getOperationContext();

        boolean sync = request.getAttributeBool(MailService.A_SYNC, false);
        int folderId = (int) request.getAttributeLong(MailService.A_FOLDER, ALL_FOLDERS);

        byte sort = DbMailItem.SORT_NONE;
        String sortStr = request.getAttribute(MailService.A_SORTBY, "");
        if (sortStr.equals(MailboxIndex.SortBy.NAME_ASCENDING.toString()))
            sort = DbMailItem.SORT_BY_SENDER | DbMailItem.SORT_ASCENDING;
        else if (sortStr.equals(MailboxIndex.SortBy.NAME_DESCENDING.toString()))
            sort = DbMailItem.SORT_BY_SENDER | DbMailItem.SORT_DESCENDING;

        ArrayList<String> attrs = null;
        ArrayList<Integer> ids = null;
        for (Element e : request.listElements())
            if (e.getName().equals(MailService.E_ATTRIBUTE)) {
                String name = e.getAttribute(MailService.A_ATTRIBUTE_NAME);
                if (attrs == null)
                    attrs = new ArrayList<String>();
                attrs.add(name);
            } else if (e.getName().equals(MailService.E_CONTACT)) {
                int id = (int) e.getAttributeLong(MailService.A_ID);
                if (ids == null)
                    ids = new ArrayList<Integer>();
                ids.add(id);
            }
        
        Element response = zsc.createElement(MailService.GET_CONTACTS_RESPONSE);
        ContactAttrCache cacache = null;

        // want to return modified date only on sync-related requests
        int fields = ToXML.NOTIFY_FIELDS;
        if (sync)
            fields |= Change.MODIFIED_CONFLICT;

        if (ids != null) {
            for (int id : ids) {
            	Contact con = mbox.getContactById(octxt, id);
                if (con != null && (folderId == ALL_FOLDERS || folderId == con.getFolderId()))
                    ToXML.encodeContact(response, zsc, con, cacache, false, attrs, fields);
            }
        } else {
        	for (Contact con : mbox.getContactList(octxt, folderId, sort))
                if (con != null)
                    ToXML.encodeContact(response, zsc, con, cacache, false, attrs, fields);
        }
        return response;
    }
}
