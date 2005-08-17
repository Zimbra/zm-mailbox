/*
 * Created on May 26, 2004
 */
package com.liquidsys.coco.service.mail;

import java.util.Map;

import com.liquidsys.coco.mailbox.Mailbox;
import com.liquidsys.coco.mailbox.SearchFolder;
import com.liquidsys.coco.mailbox.Mailbox.OperationContext;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.soap.LiquidContext;
import com.liquidsys.soap.WriteOpDocumentHandler;

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
