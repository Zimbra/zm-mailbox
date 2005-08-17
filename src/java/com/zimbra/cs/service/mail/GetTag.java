/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.mail;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.LiquidContext;

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
