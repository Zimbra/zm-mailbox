/*
 * Created on May 26, 2004
 */
package com.liquidsys.coco.service.mail;

import java.util.Map;

import com.liquidsys.coco.mailbox.Mailbox;
import com.liquidsys.coco.mailbox.Tag;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.soap.LiquidContext;
import com.liquidsys.soap.WriteOpDocumentHandler;

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
