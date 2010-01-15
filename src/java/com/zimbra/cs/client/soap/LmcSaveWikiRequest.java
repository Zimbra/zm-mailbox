/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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


package com.zimbra.cs.client.soap;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.DomUtil;
import com.zimbra.common.soap.SoapParseException;
import com.zimbra.cs.client.*;

public class LmcSaveWikiRequest extends LmcSoapRequest {

	private LmcWiki mWiki;
    
    public void setWiki(LmcWiki wiki) { mWiki = wiki; }
    
    public LmcWiki getWiki() { return mWiki; }
    
	protected Element getRequestXML() throws LmcSoapClientException {
		Element request = DocumentHelper.createElement(MailConstants.SAVE_WIKI_REQUEST);
        Element w = DomUtil.add(request, MailConstants.E_WIKIWORD, "");
        LmcSoapRequest.addAttrNotNull(w, MailConstants.A_NAME, mWiki.getWikiWord());
        LmcSoapRequest.addAttrNotNull(w, MailConstants.A_FOLDER, mWiki.getFolder());
        w.addText(mWiki.getContents());
        return request;
    }

	protected LmcSoapResponse parseResponseXML(Element responseXML)
			throws SoapParseException, ServiceException, LmcSoapClientException {
		
        LmcSaveDocumentResponse response = new LmcSaveDocumentResponse();
        LmcDocument doc = parseDocument(DomUtil.get(responseXML, MailConstants.E_WIKIWORD));
        response.setDocument(doc);
        return response;
	}

}
