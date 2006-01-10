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
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraContext;

/**
 * @author schemers
 */
public class GetContacts extends DocumentHandler  {

    public Element handle(Element request, Map context) throws ServiceException {
        ZimbraContext lc = getZimbraContext(context);
        Mailbox mbox = getRequestedMailbox(lc);
        Mailbox.OperationContext octxt = lc.getOperationContext();

        byte sort = DbMailItem.SORT_NONE;
        String sortStr = request.getAttribute(MailService.A_SORTBY, "");
        if (sortStr.equals(MailboxIndex.SortBy.NAME_ASCENDING.getName()))
            sort = DbMailItem.SORT_BY_SENDER | DbMailItem.SORT_ASCENDING;
        else if (sortStr.equals(MailboxIndex.SortBy.NAME_DESCENDING.getName()))
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
        
        Element response = lc.createElement(MailService.GET_CONTACTS_RESPONSE);
        ContactAttrCache cacache = null; //new ContactAttrCache();

        if (ids != null) {
            for (int id : ids) {
            	Contact con = mbox.getContactById(octxt, id);
                if (con != null)
                    ToXML.encodeContact(response, lc, con, cacache, false, attrs);
            }
        } else {
        	for (Contact con : mbox.getContactList(octxt, -1, sort))
                if (con != null)
                    ToXML.encodeContact(response, lc, con, cacache, false, attrs);
        }
        return response;
    }
}
