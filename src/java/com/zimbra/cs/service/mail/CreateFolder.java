/*
 * Created on Aug 27, 2004
 */
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.LiquidContext;
import com.zimbra.soap.WriteOpDocumentHandler;

/**
 * @author dkarp
 */
public class CreateFolder extends WriteOpDocumentHandler {

	public Element handle(Element request, Map context) throws ServiceException {
		LiquidContext lc = getLiquidContext(context);
        Mailbox mbox = getRequestedMailbox(lc);

        Element t = request.getElement(MailService.E_FOLDER);
        String name = t.getAttribute(MailService.A_NAME);
        int parentId = (int) t.getAttributeLong(MailService.A_FOLDER);
        
        Folder folder;
        try {
            folder = mbox.createFolder(null, name, parentId);
        } catch (ServiceException se) {
            if (se.getCode() == MailServiceException.ALREADY_EXISTS && t.getAttributeBool(MailService.A_FETCH_IF_EXISTS, false))
                folder = mbox.getFolderByName(parentId, name);
            else
                throw se;
        }

        Element response = lc.createElement(MailService.CREATE_FOLDER_RESPONSE);
        if (folder != null)
        	ToXML.encodeFolder(response, folder);
        return response;
	}
}
