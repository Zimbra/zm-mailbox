/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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


package com.zimbra.cs.client.soap;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.DomUtil;
import com.zimbra.cs.client.*;

public class LmcSaveDocumentRequest extends LmcSendMsgRequest {

	private LmcDocument mDoc;
    
    public void setDocument(LmcDocument doc) { mDoc = doc; }
    
    public LmcDocument getDocument() { return mDoc; }
    
	protected Element getRequestXML() {
		Element request = DocumentHelper.createElement(MailConstants.SAVE_DOCUMENT_REQUEST);
        Element doc = DomUtil.add(request, MailConstants.E_DOC, "");
        LmcSoapRequest.addAttrNotNull(doc, MailConstants.A_NAME, mDoc.getName());
        LmcSoapRequest.addAttrNotNull(doc, MailConstants.A_CONTENT_TYPE, mDoc.getContentType());
        LmcSoapRequest.addAttrNotNull(doc, MailConstants.A_FOLDER, mDoc.getFolder());
        Element upload = DomUtil.add(doc, MailConstants.E_UPLOAD, "");
        LmcSoapRequest.addAttrNotNull(upload, MailConstants.A_ID, mDoc.getAttachmentId());
        return request;
    }

	protected LmcSoapResponse parseResponseXML(Element responseXML) throws ServiceException {
		
        LmcSaveDocumentResponse response = new LmcSaveDocumentResponse();
        LmcDocument doc = parseDocument(DomUtil.get(responseXML, MailConstants.E_DOC));
        response.setDocument(doc);
        return response;
	}

}
