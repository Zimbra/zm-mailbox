/*
 * Created on Sep 23, 2005
 */
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.WriteOpDocumentHandler;
import com.zimbra.soap.ZimbraContext;

/**
 * @author dkarp
 */
public class CreateMountpoint extends WriteOpDocumentHandler {

    public Element handle(Element request, Map context) throws ServiceException {
        ZimbraContext lc = getZimbraContext(context);
        Mailbox mbox = getRequestedMailbox(lc);

        Element t = request.getElement(MailService.E_MOUNT);
        String name = t.getAttribute(MailService.A_NAME);
        int folderId = (int) t.getAttributeLong(MailService.A_FOLDER);
        String ownerId = t.getAttribute(MailService.A_ZIMBRA_ID);
        int remoteId = (int) t.getAttributeLong(MailService.A_REMOTE_ID);
        String view = t.getAttribute(MailService.A_DEFAULT_VIEW, null);
        
        Mountpoint mpt = mbox.createMountpoint(null, folderId, name, ownerId, remoteId, MailItem.getTypeForName(view));
        
        Element response = lc.createElement(MailService.CREATE_MOUNTPOINT_RESPONSE);
        if (mpt != null)
            ToXML.encodeMountpoint(response, mpt);
        return response;
    }
}
