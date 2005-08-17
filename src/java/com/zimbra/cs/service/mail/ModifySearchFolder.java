/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.SearchFolder;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.LiquidContext;
import com.zimbra.soap.WriteOpDocumentHandler;

/**
 * @author schemers
 */
public class ModifySearchFolder extends WriteOpDocumentHandler  {

	public Element handle(Element request, Map context) throws ServiceException {
		LiquidContext lc = getLiquidContext(context);
        Mailbox mbox = getRequestedMailbox(lc);
        OperationContext octxt = lc.getOperationContext();

        Element t = request.getElement(MailService.E_SEARCH);
        int id = (int) t.getAttributeLong(MailService.A_ID);
        String query = t.getAttribute(MailService.A_QUERY);
        String types = t.getAttribute(MailService.A_SEARCH_TYPES, null);
        String sort = t.getAttribute(MailService.A_SORTBY, null);

        mbox.modifySearchFolder(octxt, id, query, types, sort);

    	SearchFolder sf = mbox.getSearchFolderById(id);
        Element response = lc.createElement(MailService.MODIFY_SEARCH_FOLDER_RESPONSE);
    	ToXML.encodeSearchFolder(response, sf);
        return response;
	}
}
