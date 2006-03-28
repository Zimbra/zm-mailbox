package com.zimbra.cs.service.wiki;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.wiki.Wiki;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraContext;

public abstract class WikiDocumentHandler extends DocumentHandler {

	protected String getAuthor(ZimbraContext lc) throws ServiceException {
		return lc.getAuthtokenAccount().getName();
	}
	
	protected Wiki getRequestedWiki(Element request, ZimbraContext lc) throws ServiceException {
		
		/*
		Wiki wiki;
        String type = request.getAttribute(MailService.A_TYPE, "wiki");
        if (type.equals("wiki")) {
        	wiki = Wiki.getInstance();
        } else if (type.equals("note")){
        	wiki = Wiki.getInstance(lc.getAuthtokenAccount().getName());
        } else {
        	throw ServiceException.INVALID_REQUEST("unknown type "+type, null);
        }
        */
		
		return Wiki.getInstance(lc.getAuthtokenAccount().getName());
	}
}
