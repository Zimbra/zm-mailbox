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
import com.zimbra.soap.LiquidContext;

/**
 * @author schemers
 */
public class GetContacts extends DocumentHandler  {

    public Element handle(Element request, Map context) throws ServiceException {
        LiquidContext lc = getLiquidContext(context);
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
