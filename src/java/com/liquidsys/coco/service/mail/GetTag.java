/*
 * Created on May 26, 2004
 */
package com.liquidsys.coco.service.mail;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.liquidsys.coco.mailbox.Flag;
import com.liquidsys.coco.mailbox.Mailbox;
import com.liquidsys.coco.mailbox.Tag;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.soap.DocumentHandler;
import com.liquidsys.soap.LiquidContext;

/**
 * @author schemers
 */
public class GetTag extends DocumentHandler  {

	public Element handle(Element request, Map context) throws ServiceException {
		LiquidContext lc = getLiquidContext(context);
		// FIXME: need to check that mailbox exists
        Mailbox mbox = getRequestedMailbox(lc);
		
        List tags = mbox.getTagList();

        Element response = lc.createElement(MailService.GET_TAG_RESPONSE);
	    if (tags != null) {
	    	for (Iterator it = tags.iterator(); it.hasNext(); ) {
	    		Tag tag = (Tag) it.next();
	    		if (tag == null || tag instanceof Flag)
	    			continue;
	    		ToXML.encodeTag(response, tag);
	    	}
	    }
        return response;
	}
}
