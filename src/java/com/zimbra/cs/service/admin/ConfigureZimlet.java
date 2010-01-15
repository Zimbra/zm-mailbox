/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
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
	    
	    checkRightTODO();
	    
		ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Element content = request.getElement(MailConstants.E_CONTENT);
        String attachment = content.getAttribute(MailConstants.A_ATTACHMENT_ID, null);
        Upload up = FileUploadServlet.fetchUpload(zsc.getAuthtokenAccountId(), attachment, zsc.getAuthToken());
        if (up == null)
            throw MailServiceException.NO_SUCH_UPLOAD(attachment);

        Element response = zsc.createElement(AdminConstants.CONFIGURE_ZIMLET_RESPONSE);
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
	
	@Override
	public void docRights(List<AdminRight> relatedRights, List<String> notes) {
	    notes.add(AdminRightCheckPoint.Notes.TODO);
	    
	    notes.add("Currently the soap gets a uploaded blob containing metadata. " + 
	            "The zimlet name is encoded in in the blob and is decoded in ZimletUtil. " +
	            "We need a way to know the zimlet name (and cos name if any, currently it " +
	            "seems to always only update the default cos) in the SOAP handler in order to " +
	            "check right.");
    }
}
