/*
 * Created on May 26, 2004
 */
package com.liquidsys.coco.service.mail;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.liquidsys.coco.mailbox.Contact;
import com.liquidsys.coco.mailbox.MailServiceException;
import com.liquidsys.coco.mailbox.Mailbox;
import com.liquidsys.coco.mailbox.Mailbox.OperationContext;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.soap.LiquidContext;
import com.liquidsys.soap.WriteOpDocumentHandler;

/**
 * @author schemers
 */
public class ModifyContact extends WriteOpDocumentHandler  {
    
    public Element handle(Element request, Map context) throws ServiceException {
        LiquidContext lc = getLiquidContext(context);
        Mailbox mbox = getRequestedMailbox(lc);
        OperationContext octxt = lc.getOperationContext();

        boolean replace = request.getAttributeBool(MailService.A_REPLACE, false);

        Element cn = request.getElement(MailService.E_CONTACT);

        int id = (int) cn.getAttributeLong(MailService.A_ID);

        HashMap attrs = new HashMap();
        for (Iterator it = cn.elementIterator(MailService.E_ATTRIBUTE); it.hasNext(); ) {
            Element e = (Element) it.next();
            String name = e.getAttribute(MailService.A_ATTRIBUTE_NAME);
            attrs.put(name, e.getText());
        }

        mbox.modifyContact(octxt, id, attrs, replace);

        Contact con = mbox.getContactById(id);
        Element response = lc.createElement(MailService.MODIFY_CONTACT_RESPONSE);
        if (con != null)
        	ToXML.encodeContact(response, con, null, true, null);
        return response;
    }
}
