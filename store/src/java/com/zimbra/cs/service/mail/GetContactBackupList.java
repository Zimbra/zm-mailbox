/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Synacor, Inc.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.GetContactBackupListResponse;

public final class GetContactBackupList extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);

        List<String> backup = getContactBackupList(mbox);
        GetContactBackupListResponse res = new GetContactBackupListResponse(backup);
        return zsc.jaxbToElement(res);
    }

    private List<String> getContactBackupList(Mailbox mbox) throws ServiceException {
        OperationContext octxt = mbox.getOperationContext();
        List<String> returnList = null;
        Folder folder = null;
        try {
            folder = mbox.getFolderByName(octxt, Mailbox.ID_FOLDER_BRIEFCASE, MailConstants.A_CONTACTS_BACKUP_FOLDER_NAME);
        } catch (ServiceException e) {
            ZimbraLog.contactbackup.warn("Failed to get Contact Backup folder.", e);
            throw e;
        }
        if (folder != null) {
            List<MailItem> items = null;
            try {
                items = mbox.getItemList(octxt, MailItem.Type.DOCUMENT, folder.getId(), SortBy.DATE_ASC);
            } catch (ServiceException e) {
                ZimbraLog.contactbackup.warn("Failed to get items from Contact Backup folder.", e);
                throw e;
            }
            if (items != null && !items.isEmpty()) {
                returnList = new ArrayList<String>();
                for (MailItem item : items) {
                    returnList.add(item.getName());
                }
            }
        }
        return returnList;
    }
}
