/*
 * Created on May 26, 2004
 */
package com.liquidsys.coco.service.mail;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.liquidsys.coco.mailbox.MailItem;
import com.liquidsys.coco.mailbox.Mailbox;
import com.liquidsys.coco.mailbox.SearchFolder;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.soap.DocumentHandler;
import com.liquidsys.soap.LiquidContext;

/**
 * @author schemers
 */
public class GetSearchFolder extends DocumentHandler  {

	public Element handle(Element request, Map context) throws ServiceException {
		LiquidContext lc = getLiquidContext(context);
        Mailbox mbox = getRequestedMailbox(lc);
		
        Element response = lc.createElement(MailService.GET_SEARCH_FOLDER_RESPONSE);
        handle(response, mbox);
        return response;
	}

	/**
	 * Pass in a request that optional has &lt;pre&gt; items as a filter, and
	 * fills in the response document with gathered tags.
	 * 
	 * @param acct
	 * @param response
	 * @throws ServiceException
	 */
	public static void handle(Element response, Mailbox mbox) throws ServiceException {
        List searches = mbox.getItemList(MailItem.TYPE_SEARCHFOLDER);
		
	    if (searches != null) {
    	    for (Iterator mi = searches.iterator(); mi.hasNext(); ) {
    	        SearchFolder q = (SearchFolder) mi.next();
    	        ToXML.encodeSearchFolder(response, q);
    	    }
	    }
	}
}
