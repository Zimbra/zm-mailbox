/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009 Zimbra, Inc.
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

public class LmcGetWikiRequest extends LmcSoapRequest {

	private String mName;
	private String mId;
	private String mFolder;
	private String mTraverse;
	
	private LmcWiki mWiki;

	public void setName(String name) { mName = name; }
	public void setId(String id)     { mId = id; }
	public void setFolder(String f)  { mFolder = f; }
	public void setTraverse(String t) { mTraverse = t; }
	
    public void setWiki(LmcWiki wiki) { mWiki = wiki; }
    
    public LmcWiki getWiki() { return mWiki; }
    
	protected Element getRequestXML() throws LmcSoapClientException {
		Element request = DocumentHelper.createElement(MailConstants.GET_WIKI_REQUEST);
        Element w = DomUtil.add(request, MailConstants.E_WIKIWORD, "");
        if (mName != null)
        	LmcSoapRequest.addAttrNotNull(w, MailConstants.A_NAME, mName);
        else if (mId != null)
        	LmcSoapRequest.addAttrNotNull(w, MailConstants.A_ID, mId);
        else
        	return request;
        LmcSoapRequest.addAttrNotNull(w, MailConstants.A_FOLDER, mFolder);
        LmcSoapRequest.addAttrNotNull(w, MailConstants.A_TRAVERSE, mTraverse);
        return request;
    }

	protected LmcSoapResponse parseResponseXML(Element responseXML)
			throws SoapParseException, ServiceException, LmcSoapClientException {
		
        LmcGetWikiResponse response = new LmcGetWikiResponse();
        LmcWiki wiki = parseWiki(DomUtil.get(responseXML, MailConstants.E_WIKIWORD));
        response.setWiki(wiki);
        return response;
	}

}
