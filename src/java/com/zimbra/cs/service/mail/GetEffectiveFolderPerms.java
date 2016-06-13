/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
