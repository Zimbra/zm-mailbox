/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.SearchFolder;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.ZimbraContext;
import com.zimbra.soap.WriteOpDocumentHandler;

/**
 * @author schemers
 */
public class CreateSearchFolder extends WriteOpDocumentHandler  {

	public Element handle(Element request, Map context) throws ServiceException {
		ZimbraContext lc = getZimbraContext(context);
        Mailbox mbox = getRequestedMailbox(lc);

        Element t = request.getElement(MailService.E_SEARCH);
        String name = t.getAttribute(MailService.A_NAME);
        String query = t.getAttribute(MailService.A_QUERY);
        String types = t.getAttribute(MailService.A_SEARCH_TYPES, null);
        String sort = t.getAttribute(MailService.A_SORTBY, null);
        int folderId = (int) t.getAttributeLong(MailService.A_FOLDER);
        
        SearchFolder search = mbox.createSearchFolder(null, folderId, name, query, types, sort);
        
        Element response = lc.createElement(MailService.CREATE_SEARCH_FOLDER_RESPONSE);
        if (search != null)
        	ToXML.encodeSearchFolder(response, search);
        return response;
	}
}
