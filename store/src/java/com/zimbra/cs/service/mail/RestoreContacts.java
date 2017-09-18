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
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.eclipse.jetty.http.HttpStatus;

import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraCookie;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.store.file.FileBlobStore;
import com.zimbra.soap.SoapServlet;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.RestoreContactsRequest;
import com.zimbra.soap.mail.message.RestoreContactsRequest.Resolve;
import com.zimbra.soap.mail.message.RestoreContactsResponse;

public class RestoreContacts extends MailDocumentHandler {

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
        ZimbraLog.contact.debug("Backup folder exists. Searching for %s.", contactBackupFileName);
        List<MailItem> itemList = mbox.getItemList(octxt, MailItem.Type.DOCUMENT, folder.getId());
        RestoreContactsResponse response = new RestoreContactsResponse();
        boolean mailItemFound = false;
        HttpResponse httpResponse = null;
        for (MailItem item : itemList) {
            if (item instanceof Document) {
                Document doc = (Document) item;
                if (doc.getName().equals(contactBackupFileName)) {
                    mailItemFound = true;
                    Object servReq = context.get(SoapServlet.SERVLET_REQUEST);
                    String realm = "https://";
                    HttpServletRequest httpRequest = null;
                    if (servReq instanceof HttpServletRequest) {
                        httpRequest = (HttpServletRequest) servReq;
                        realm = httpRequest.isSecure() ? "https://" : "http://";
                    }
                    if(resolve == null) {
                        resolve = Resolve.reset;
                    }
                    String url = realm + getLocalHost() + "/service/home/"
                        + mbox.getAccount().getName() + "/?" + UserServlet.QP_FMT + "=tgz&"
                        + UserServlet.QP_TYPES + "=contact&" + MimeConstants.P_CHARSET + "=UTF-8";
                    CookieStore cookieStore = new BasicCookieStore();
                    if (resolve != Resolve.ignore) {
                        url = url + "&" + MailConstants.A_CONTACTS_RESTORE_RESOLVE + "=" + resolve;
                    }
                    AuthToken authToken = octxt.getAuthToken();
                    BasicClientCookie cookie = null;
                    try {
                        cookie = new BasicClientCookie(ZimbraCookie.COOKIE_ZM_AUTH_TOKEN, authToken.getEncoded());
                        cookie.setPath("/");
                        cookie.setDomain(mbox.getAccount().getDomainName());
                        cookieStore.addCookie(cookie);
                    } catch (AuthTokenException e) {
                        throw ServiceException.FAILURE("Failed to get authentication token", e);
                    }
                    File file = new File(FileBlobStore.getBlobPath(mbox, doc.getId(),
                        doc.getSavedSequence(), Short.valueOf(doc.getLocator())));
                    if (!file.exists()) {
                        throw ServiceException
                            .INVALID_REQUEST("File does not exist: " + contactBackupFileName, null);
                    }
                    ZimbraLog.contact.debug("Backup file found. Restoring contacts in %s.",
                        contactBackupFileName);
                    httpResponse = httpPostBackup(file, url, cookieStore);
                    break;
                }
            }
        }
        if (!mailItemFound) {
            throw ServiceException.INVALID_REQUEST("No such file: " + contactBackupFileName, null);
        }

        if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.OK_200) {
            ZimbraLog.contact.debug("Restore operation for %s completed successfully",
                contactBackupFileName);
        } else {
            ZimbraLog.contact.info("Restore operation for %s failed. Http respose status: %s",
                contactBackupFileName, httpResponse.getStatusLine());
            throw ServiceException
            .FAILURE("Failed to restore contacts backup " + contactBackupFileName, null);
        }
        return zsc.jaxbToElement(response);
    }

    public static HttpResponse httpPostBackup(File file, String url, CookieStore cookieStore) throws ServiceException {
        HttpResponse httpResponse = null;
        HttpClient http = HttpClientBuilder.create().setDefaultCookieStore(cookieStore)
            .build();
        HttpPost post = new HttpPost(url);
        MultipartEntity multipart = new MultipartEntity();
        multipart.addPart("file", new FileBody(file));
        post.setEntity(multipart);
        try {
            httpResponse = http.execute(post);
        } catch (IOException e) {
            throw ServiceException.FAILURE("Failed to execute contact restore request", null);
        }
        return httpResponse;
    }
}
