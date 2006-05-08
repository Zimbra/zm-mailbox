/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */


package com.zimbra.cs.client.soap;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.soap.DomUtil;
import com.zimbra.soap.SoapParseException;
import com.zimbra.cs.client.*;

public class LmcSaveWikiRequest extends LmcSoapRequest {

	private LmcWiki mWiki;
    
    public void setWiki(LmcWiki wiki) { mWiki = wiki; }
    
    public LmcWiki getWiki() { return mWiki; }
    
	protected Element getRequestXML() throws LmcSoapClientException {
		Element request = DocumentHelper.createElement(MailService.SAVE_WIKI_REQUEST);
        Element w = DomUtil.add(request, MailService.E_WIKIWORD, "");
        LmcSoapRequest.addAttrNotNull(w, MailService.A_NAME, mWiki.getWikiWord());
        LmcSoapRequest.addAttrNotNull(w, MailService.A_FOLDER, mWiki.getFolder());
        w.addText(mWiki.getContents());
        return request;
    }

	protected LmcSoapResponse parseResponseXML(Element responseXML)
			throws SoapParseException, ServiceException, LmcSoapClientException {
		
        LmcSaveDocumentResponse response = new LmcSaveDocumentResponse();
        LmcDocument doc = parseDocument(DomUtil.get(responseXML, MailService.E_WIKIWORD));
        response.setDocument(doc);
        return response;
	}

}
