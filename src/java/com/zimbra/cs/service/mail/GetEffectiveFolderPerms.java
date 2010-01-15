/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.acl.FolderACL;
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
        
        Folder folder = mbox.getFolderById(null, folderId);
        
        // return the effective permissions - authed user dependent
        Short perms = FolderACL.getEffectivePermissionsLocal(octxt, mbox, folder);
        
        Element response = zsc.createElement(MailConstants.GET_EFFECTIVE_FOLDER_PERMS_RESPONSE);
        encodePerms(response, perms);

        return response;
    }
    
    private void encodePerms(Element response, Short perms) {
        String permsStr = ACL.rightsToString(perms);
        Element eFolder = response.addElement(MailConstants.E_FOLDER);
        eFolder.addAttribute(MailConstants.A_RIGHTS, permsStr);
    }
}
