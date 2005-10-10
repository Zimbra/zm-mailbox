/*
 * Created on Sep 23, 2005
 */
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.Element;
import com.zimbra.soap.WriteOpDocumentHandler;
import com.zimbra.soap.ZimbraContext;

/**
 * @author dkarp
 */
public class CreateMountpoint extends WriteOpDocumentHandler {

    private static final String[] TARGET_FOLDER_PATH = new String[] { MailService.E_MOUNT, MailService.A_FOLDER };
    private static final String[] RESPONSE_ITEM_PATH = new String[] { };
    protected String[] getProxiedIdPath()     { return TARGET_FOLDER_PATH; }
    protected boolean checkMountpointProxy()  { return true; }
    protected String[] getResponseItemPath()  { return RESPONSE_ITEM_PATH; }

    public Element handle(Element request, Map context) throws ServiceException {
        ZimbraContext lc = getZimbraContext(context);
        Mailbox mbox = getRequestedMailbox(lc);

        Element t = request.getElement(MailService.E_MOUNT);
        String name      = t.getAttribute(MailService.A_NAME);
        String ownerId   = t.getAttribute(MailService.A_ZIMBRA_ID);
        int    remoteId  = (int) t.getAttributeLong(MailService.A_REMOTE_ID);
        String view      = t.getAttribute(MailService.A_DEFAULT_VIEW, null);
        ItemId iidParent = new ItemId(t.getAttribute(MailService.A_FOLDER));

        Mountpoint mpt;
        try {
            mpt = mbox.createMountpoint(lc.getOperationContext(), iidParent.getId(), name, ownerId, remoteId, MailItem.getTypeForName(view));
        } catch (ServiceException se) {
            if (se.getCode() == MailServiceException.ALREADY_EXISTS && t.getAttributeBool(MailService.A_FETCH_IF_EXISTS, false)) {
                Folder folder = mbox.getFolderByName(lc.getOperationContext(), iidParent.getId(), name);
                if (folder instanceof Mountpoint)
                    mpt = (Mountpoint) folder;
                else
                    throw se;
            } else
                throw se;
        }
        
        Element response = lc.createElement(MailService.CREATE_MOUNTPOINT_RESPONSE);
        if (mpt != null)
            ToXML.encodeMountpoint(response, lc, mpt);
        return response;
    }
}
