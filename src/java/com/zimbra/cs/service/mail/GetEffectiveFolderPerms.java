package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.acl.EffectiveACLCache;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.ZimbraSoapContext;

public class GetEffectiveFolderPerms extends MailDocumentHandler {
    
    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException, SoapFaultException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        OperationContext octxt = getOperationContext(zsc, context);
        
        Element eFolder = request.getElement(MailConstants.E_FOLDER);
        String fid = eFolder.getAttribute(MailConstants.A_FOLDER);
        
        ItemId iid = new ItemId(fid, zsc);
        int folderId = iid.getId();
        
        Mailbox mbox;
        String ownerAcctId = iid.getAccountId();
        if (ownerAcctId == null)
            mbox = getRequestedMailbox(zsc);
        else
            mbox = MailboxManager.getInstance().getMailboxByAccountId(ownerAcctId);
        
        // cache the effective folder ACL in memcached - independent of the authed user
        cacheEffectiveACL(mbox.getFolderById(null, folderId));
        
        // return the effective permissions - authed user dependent
        String perms = getEffectivePermissions(octxt, mbox, folderId);
        
        Element response = zsc.createElement(MailConstants.GET_EFFECTIVE_FOLDER_PERMS_RESPONSE);
        encodePerms(response, perms);

        return response;
    }
    
    String getEffectivePermissions(OperationContext octxt, Mailbox mbox, int folderId) throws ServiceException {
        short perms = mbox.getEffectivePermissions(octxt, folderId, MailItem.TYPE_FOLDER);
        return ACL.rightsToString(perms);
    }
    
    private void encodePerms(Element response, String perms) {
        Element eFolder = response.addElement(MailConstants.E_FOLDER);
        eFolder.addAttribute(MailConstants.A_RIGHTS, perms);
    }
    
    private void cacheEffectiveACL(Folder folder) throws ServiceException {
        ACL acl = folder.getEffectiveACL();
        EffectiveACLCache.set(folder.getAccount().getId(), folder.getId(), acl);
    }
}
