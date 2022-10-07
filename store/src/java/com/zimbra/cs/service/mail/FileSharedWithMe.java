/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2022 Synacor, Inc.
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

import com.zimbra.common.mailbox.FolderConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.soap.ZimbraSoapContext;

public class FileSharedWithMe extends MailDocumentHandler implements DelegatableRequest {

    private static final String REVOKE = "revoke";
    private static final String EDIT = "edit";
    private static final String CREATE = "create";

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        try {
            ZimbraSoapContext zsc = getZimbraSoapContext(context);
            String ownerAccountId = zsc.getAuthtokenAccountId();
            String granteeAccountID = zsc.getRequestedAccountId();
            // Check in place so that API cannot be invoked directly
            if (ownerAccountId.equals(granteeAccountID)) {
                ZimbraLog.mailbox.error("File sharer and grantee cannot be same", new Throwable("invalid request source"));
                throw ServiceException.FAILURE("invalid request", new Throwable("invalid"));
            }
            OperationContext octxt = getOperationContext(zsc, context);
            Account account = getRequestedAccount(zsc);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account, false);

            String action = request.getAttribute(MailConstants.E_ACTION);
            // ZCS-11901: When A share's folder to B and B subsequent shares the file's from
            // A folder to C,
            // use actualOwnerAccountId i.e. accountId of A
            // this remains correct when file is shared from A to B , or from B to C
            String actualOwnerAccountId = request.getAttribute(MailConstants.A_REMOTE_ID);
            String fileName = request.getAttribute(MailConstants.A_CONTENT_FILENAME, "");
            int ownerFileId = request.getAttributeInt(MailConstants.A_ITEMID, -1);
            String fileUUID = request.getAttribute(MailConstants.A_REMOTE_UUID, "");
            String fileOwnerName = request.getAttribute(MailConstants.A_OWNER_NAME, "");
            String rights = request.getAttribute(MailConstants.A_RIGHTS, "");
            String contentType = request.getAttribute(MailConstants.A_CONTENT_TYPE, "");
            long size = request.getAttributeLong(MailConstants.A_SIZE, -1);
            Element response = zsc.createElement(MailConstants.FILE_SHARED_WITH_ME_RESPONSE);
            if (Provisioning.onLocalServer(account)) {
                // if file is shared, this file should be available at receivers end
                if (action != null && CREATE.equals(action)) {
                    mbox.createFileSharedWithMe(mbox, octxt, FolderConstants.ID_FOLDER_FILE_SHARED_WITH_ME, fileName,

                            actualOwnerAccountId, ownerFileId, fileUUID, fileOwnerName, contentType, size, rights);
                } // if file access is revoked/deleted at source, delete it from receiver end as well
                else if (action != null && REVOKE.equals(action)) {
                    mbox.deleteFileSharedWithMe(mbox, octxt, fileUUID, ownerFileId, actualOwnerAccountId, granteeAccountID);
                } // if file rights is changed, update file permission accordingly
                else if (action != null && EDIT.equalsIgnoreCase(action)) {
                    mbox.updateFileSharedWithMe(mbox, octxt, actualOwnerAccountId, ownerFileId, fileUUID, fileOwnerName,
                            contentType, rights, granteeAccountID);
                } else {
                    throw ServiceException.FAILURE("invalid request", new Throwable("invalid"));
                }
                return response.addAttribute(MailConstants.A_STATUS, MailConstants.A_VERIFICATION_SUCCESS);
            } else {
                response = proxyRequest(request, context, granteeAccountID);
                return response;
            }
        } catch (ServiceException ex) {
            throw ServiceException.FAILURE("Error File Shared With me operation ", ex);
        }
    }

    @Override
    public boolean isDelegatable() {
        return true;
    }

}
