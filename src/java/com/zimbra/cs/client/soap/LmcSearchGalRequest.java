/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2010 Zimbra, Inc.
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

package com.zimbra.cs.client.soap;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.zimbra.common.soap.DomUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.SoapParseException;
import com.zimbra.cs.client.*;

public class LmcSearchGalRequest extends LmcSoapRequest {

    private String mName;
    
    public void setName(String n) { mName = n; }
    
    public String getName() { return mName; }
    
	protected Element getRequestXML() throws LmcSoapClientException {
		Element request = DocumentHelper.createElement(AccountConstants.SEARCH_GAL_REQUEST);
		DomUtil.add(request, AccountConstants.E_NAME, mName);
        return request;
    }

	protected LmcSoapResponse parseResponseXML(Element responseXML)
	    throws SoapParseException, ServiceException, LmcSoapClientException 
    {
        LmcContact contacts[] = parseContactArray(responseXML);
        LmcSearchGalResponse sgResp = new LmcSearchGalResponse();
        sgResp.setContacts(contacts);
        return sgResp;
	}

}
