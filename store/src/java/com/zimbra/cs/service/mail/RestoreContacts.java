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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.zimbra.client.ZMailbox;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.ZimbraAuthToken;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.store.file.FileBlobStore;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.RestoreContactsRequest;
import com.zimbra.soap.mail.message.RestoreContactsRequest.Resolve;
import com.zimbra.soap.mail.message.RestoreContactsResponse;

public class RestoreContacts extends MailDocumentHandler {

    private static final String FILE_NOT_FOUND = "File not found: ";

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        RestoreContactsRequest req = zsc.elementToJaxb(request);
        String contactBackupFileName = req.getContactsBackupFileName();
        Resolve resolve = req.getResolve();
        // if folder does not exist, SoapFault is thrown by getFolderByName() itself.
        Folder folder = mbox.getFolderByName(octxt, Mailbox.ID_FOLDER_BRIEFCASE,
            MailConstants.A_CONTACTS_BACKUP_FOLDER_NAME);
        ZimbraLog.contactbackup.debug("Backup folder exists. Searching for %s.", contactBackupFileName);
        List<MailItem> itemList = mbox.getItemList(octxt, MailItem.Type.DOCUMENT, folder.getId());
        RestoreContactsResponse response = new RestoreContactsResponse();
        boolean mailItemFound = false;
        for (MailItem item : itemList) {
            if (item instanceof Document) {
                Document doc = (Document) item;
                if (doc.getName().equals(contactBackupFileName)) {
                    mailItemFound = true;
                    if(resolve == null) {
                        resolve = Resolve.reset;
                    }
                    String CONTACT_RES_URL = "//?" + UserServlet.QP_FMT + "=tgz&"
                        + UserServlet.QP_TYPES + "=contact&" + MimeConstants.P_CHARSET + "=UTF-8";
                    if (resolve != Resolve.ignore) {
                        CONTACT_RES_URL = CONTACT_RES_URL + "&"
                            + MailConstants.A_CONTACTS_RESTORE_RESOLVE + "=" + resolve;
                    }
                    File file = new File(FileBlobStore.getBlobPath(mbox, doc.getId(),
                        doc.getSavedSequenceLong(), Short.valueOf(doc.getLocator())));
                    if (!file.exists()) {
                        throw ServiceException
                            .INVALID_REQUEST(FILE_NOT_FOUND + contactBackupFileName, null);
                    }
                    Account account = mbox.getAccount();
                    ZimbraAuthToken token = new ZimbraAuthToken(account);
                    ZMailbox.Options zoptions = new ZMailbox.Options(token.toZAuthToken(),
                        AccountUtil.getSoapUri(account));
                    zoptions.setNoSession(true);
                    zoptions.setTargetAccount(account.getId());
                    zoptions.setTargetAccountBy(AccountBy.id);
                    ZMailbox zmbx = ZMailbox.getMailbox(zoptions);
                    try {
                        InputStream is = new FileInputStream(file);
                        zmbx.postRESTResource(CONTACT_RES_URL, is, true, file.length(), null,
                            LC.httpclient_internal_connmgr_so_timeout.intValue());
                    } catch (FileNotFoundException e) {
                        throw ServiceException
                            .INVALID_REQUEST(FILE_NOT_FOUND + contactBackupFileName, e);
                    }
                    ZimbraLog.contactbackup.debug("Backup file found. Restoring contacts in %s.",
                        contactBackupFileName);
                    break;
                }
            }
        }
        if (!mailItemFound) {
            throw ServiceException.INVALID_REQUEST(FILE_NOT_FOUND + contactBackupFileName, null);
        }
        return zsc.jaxbToElement(response);
    }
}
