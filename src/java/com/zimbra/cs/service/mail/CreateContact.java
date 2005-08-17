/*
 * Created on May 26, 2004
 */
package com.liquidsys.coco.service.mail;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.liquidsys.coco.mailbox.Contact;
import com.liquidsys.coco.mailbox.Mailbox;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.soap.LiquidContext;
import com.liquidsys.soap.WriteOpDocumentHandler;

/**
 * @author schemers
 */
public class CreateContact extends WriteOpDocumentHandler  {
    
    public Element handle(Element request, Map context) throws ServiceException {
        LiquidContext lc = getLiquidContext(context);
        Mailbox mbox = getRequestedMailbox(lc);

        Element cn = request.getElement(MailService.E_CONTACT);
        int folderId = (int) cn.getAttributeLong(MailService.A_FOLDER, Mailbox.ID_FOLDER_CONTACTS);
        String tagsStr = cn.getAttribute(MailService.A_TAGS, null);
        HashMap attrs = new HashMap();
        
        for (Iterator it = cn.elementIterator(MailService.E_ATTRIBUTE); it.hasNext(); ) {
            Element e = (Element) it.next();
            String name = e.getAttribute(MailService.A_ATTRIBUTE_NAME);
            String value = e.getText();
            attrs.put(name, value);
        }
        Contact con = mbox.createContact(null, attrs, folderId, tagsStr);
        Element response = lc.createElement(MailService.CREATE_CONTACT_RESPONSE);
        if (con != null)
            ToXML.encodeContact(response, con, null, true, null);
        return response;
    }
}
