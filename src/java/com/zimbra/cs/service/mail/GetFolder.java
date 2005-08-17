/*
 * Created on Aug 31, 2004
 */
package com.zimbra.cs.service.mail;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ZimbraContext;

/**
 * @author dkarp
 */
public class GetFolder extends DocumentHandler {

    private static final int DEFAULT_FOLDER_ID = Mailbox.ID_FOLDER_USER_ROOT;

	public Element handle(Element request, Map context) throws ServiceException {
		ZimbraContext lc = getZimbraContext(context);
        Mailbox mbox = getRequestedMailbox(lc);

        int parentId = DEFAULT_FOLDER_ID;
        Element eFolder = request.getOptionalElement(MailService.E_FOLDER);
        if (eFolder != null)
        	parentId = (int) request.getAttributeLong(MailService.A_FOLDER, DEFAULT_FOLDER_ID);

        Folder folder = mbox.getFolderById(parentId);
        if (folder == null)
        	throw MailServiceException.NO_SUCH_FOLDER(parentId);

        Element response = lc.createElement(MailService.GET_FOLDER_RESPONSE);
        synchronized (mbox) {
        	handleFolder(mbox, folder, response);
        }
        return response;
	}

	public static void handleFolder(Mailbox mbox, Folder folder, Element response) {
		Element respFolder = ToXML.encodeFolder(response, folder);

        List subfolders = folder.getSubfolders();
        if (subfolders != null)
	        for (Iterator it = subfolders.iterator(); it.hasNext(); ) {
	        	Folder subfolder = (Folder) it.next();
	        	if (subfolder != null)
	        		handleFolder(mbox, subfolder, respFolder);
        }
	}
}
