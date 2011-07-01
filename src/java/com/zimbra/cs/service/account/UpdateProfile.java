/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.service.account;

import java.io.IOException;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedDocument;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.service.ProfileServlet;
import com.zimbra.soap.ZimbraSoapContext;

public class UpdateProfile extends AccountDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Element profile = request.getElement(AccountConstants.E_PROFILE);
        String imageId = profile.getAttribute(AccountConstants.A_IMAGE, null);
        if (imageId != null) {
            Upload up = null;
            try {
                up = FileUploadServlet.fetchUpload(zsc.getAuthtokenAccountId(), imageId, zsc.getAuthToken());
                Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(zsc.getRequestedAccountId());
                OperationContext octxt = getOperationContext(zsc, context);
                ParsedDocument pd = new ParsedDocument(up.getInputStream(), ProfileServlet.IMAGE_URI, up.getContentType(), System.currentTimeMillis(),
                        getAuthenticatedAccount(zsc).getName(), null);
                Document image = null;
                try {
                    image = (Document)mbox.getItemByPath(octxt, ProfileServlet.IMAGE_URI, Mailbox.ID_FOLDER_PROFILE);
                } catch (ServiceException se) {
                    if (!(se instanceof MailServiceException.NoSuchItemException))
                        throw se;
                }
                if (image == null) {
                    mbox.createDocument(octxt, Mailbox.ID_FOLDER_PROFILE, pd, MailItem.Type.DOCUMENT, 0);
                } else {
                    mbox.addDocumentRevision(octxt, image.getId(), pd);
                }
            } catch (IOException e) {
                ZimbraLog.account.warn("can't save profile image", e);
            } finally {
                if (up != null) {
                    FileUploadServlet.deleteUpload(up);
                }
            }
        }
        Element response = zsc.createElement(AccountConstants.E_UPDATE_PROFILE_RESPONSE);
        return response;
    }

}
