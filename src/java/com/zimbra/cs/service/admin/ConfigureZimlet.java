/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.io.IOException;
import java.util.Map;

import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.zimlet.ZimletException;
import com.zimbra.cs.zimlet.ZimletUtil;
import com.zimbra.soap.ZimbraSoapContext;

public class ConfigureZimlet extends AdminDocumentHandler {

	@Override
	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
		ZimbraSoapContext lc = getZimbraSoapContext(context);
        Element content = request.getElement(MailConstants.E_CONTENT);
        String attachment = content.getAttribute(MailConstants.A_ATTACHMENT_ID, null);
        Upload up = FileUploadServlet.fetchUpload(lc.getAuthtokenAccountId(), attachment, lc.getAuthToken());
        if (up == null)
            throw MailServiceException.NO_SUCH_UPLOAD(attachment);

        Element response = lc.createElement(AdminConstants.CONFIGURE_ZIMLET_RESPONSE);
		try {
			byte[] blob = ByteUtil.getContent(up.getInputStream(), 0);
			ZimletUtil.installConfig(new String(blob));
		} catch (IOException ioe) {
			throw ServiceException.FAILURE("cannot configure", ioe);
		} catch (ZimletException ze) {
			throw ServiceException.FAILURE("cannot configure", ze);
		} finally {
			FileUploadServlet.deleteUpload(up);
		}
		return response;
	}
}
