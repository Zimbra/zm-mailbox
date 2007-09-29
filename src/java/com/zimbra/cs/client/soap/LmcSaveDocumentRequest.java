/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */


package com.zimbra.cs.client.soap;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.soap.DomUtil;
import com.zimbra.cs.client.*;

public class LmcSaveDocumentRequest extends LmcSendMsgRequest {

	private LmcDocument mDoc;
    
    public void setDocument(LmcDocument doc) { mDoc = doc; }
    
    public LmcDocument getDocument() { return mDoc; }
    
	protected Element getRequestXML() {
		Element request = DocumentHelper.createElement(MailService.SAVE_DOCUMENT_REQUEST);
        Element doc = DomUtil.add(request, MailService.E_DOC, "");
        LmcSoapRequest.addAttrNotNull(doc, MailService.A_NAME, mDoc.getName());
        LmcSoapRequest.addAttrNotNull(doc, MailService.A_CONTENT_TYPE, mDoc.getContentType());
        LmcSoapRequest.addAttrNotNull(doc, MailService.A_FOLDER, mDoc.getFolder());
        Element upload = DomUtil.add(doc, MailService.E_UPLOAD, "");
        LmcSoapRequest.addAttrNotNull(upload, MailService.A_ID, mDoc.getAttachmentId());
        return request;
    }

	protected LmcSoapResponse parseResponseXML(Element responseXML) throws ServiceException {
		
        LmcSaveDocumentResponse response = new LmcSaveDocumentResponse();
        LmcDocument doc = parseDocument(DomUtil.get(responseXML, MailService.E_DOC));
        response.setDocument(doc);
        return response;
	}

}
