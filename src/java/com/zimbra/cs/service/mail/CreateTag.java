/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.LiquidContext;
import com.zimbra.soap.WriteOpDocumentHandler;

/**
 * @author schemers
 */
public class CreateTag extends WriteOpDocumentHandler  {

	public Element handle(Element request, Map context) throws ServiceException {
		LiquidContext lc = getLiquidContext(context);
		// FIXME: need to check that account and mailbox exist
        Mailbox mbox = getRequestedMailbox(lc);

        Element t = request.getElement(MailService.E_TAG);
        String name = t.getAttribute(MailService.A_NAME);
        byte color = (byte) t.getAttributeLong(MailService.A_COLOR, Tag.DEFAULT_COLOR);
        
        Tag tag = mbox.createTag(null, name, color);
        
        Element response = lc.createElement(MailService.CREATE_TAG_RESPONSE);
        if (tag != null)
        	ToXML.encodeTag(response, tag);
        return response;
	}
}
