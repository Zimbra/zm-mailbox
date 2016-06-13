/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.admin;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.FileUploadServlet.Upload;
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
        if (up == null) {
            throw MailServiceException.NO_SUCH_UPLOAD(attachment);
        }

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
