/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ZimbraContext;

/**
 * @author schemers
 */
public class GetContacts extends DocumentHandler  {

    public Element handle(Element request, Map context) throws ServiceException {
        ZimbraContext lc = getZimbraContext(context);
        Mailbox mbox = getRequestedMailbox(lc);

        ArrayList attrs = null;
        ArrayList ids = null;        
        for (Iterator it = request.elementIterator(); it.hasNext(); ) {
            Element e = (Element) it.next();
            if (e.getName().equals(MailService.E_ATTRIBUTE)) {
                String name = e.getAttribute(MailService.A_ATTRIBUTE_NAME);
                if (attrs == null)
                    attrs = new ArrayList();
                attrs.add(name);
            } else if (e.getName().equals(MailService.E_CONTACT)) {
                int id = (int) e.getAttributeLong(MailService.A_ID);
                if (ids == null)
                    ids = new ArrayList();
                ids.add(new Integer(id));
            }
        }
        
        Element response = lc.createElement(MailService.GET_CONTACTS_RESPONSE);
        ContactAttrCache cacache = null; //new ContactAttrCache();

        if (ids != null) {
            for (Iterator it = ids.iterator(); it.hasNext(); ) {
            	Contact con = mbox.getContactById(((Integer) it.next()).intValue());
                if (con != null)
                    ToXML.encodeContact(response, con, cacache, false, attrs);
            }
        } else {
        	List contacts = mbox.getContactList(-1);
            for (Iterator it = contacts.iterator(); it.hasNext(); ) {
                Contact con = (Contact) it.next();
                if (con != null)
                    ToXML.encodeContact(response, con, cacache, false, attrs);
            }
        }
        return response;
    }
}
