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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.client.soap;

import java.util.ArrayList;
import java.util.Iterator;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.zimbra.soap.DomUtil;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.soap.SoapParseException;
import com.zimbra.cs.client.*;

public class LmcBrowseRequest extends LmcSoapRequest {

    private String mBrowseBy;
    
    public void setBrowseBy(String b) { mBrowseBy = b; }
    
    public String getBrowseBy() { return mBrowseBy; }
    
	protected Element getRequestXML() throws LmcSoapClientException {
        Element request = DocumentHelper.createElement(MailService.BROWSE_REQUEST);
        DomUtil.addAttr(request, MailService.A_BROWSE_BY, mBrowseBy);
        return request;
	}

    protected LmcBrowseData parseBrowseData(Element bdElem) {
    	LmcBrowseData bd = new LmcBrowseData();
        bd.setFlags(bdElem.attributeValue(MailService.A_BROWSE_DOMAIN_HEADER));
        bd.setData(bdElem.getText());
        return bd;
    }
    
	protected LmcSoapResponse parseResponseXML(Element parentElem)
			throws SoapParseException, ServiceException, LmcSoapClientException 
    {
		LmcBrowseResponse response = new LmcBrowseResponse();
        ArrayList bdArray = new ArrayList();
        for (Iterator ait = parentElem.elementIterator(MailService.E_BROWSE_DATA); ait.hasNext(); ) {
            Element a = (Element) ait.next();
            bdArray.add(parseBrowseData(a));
        }

        if (!bdArray.isEmpty()) {
            LmcBrowseData bds[] = new LmcBrowseData[bdArray.size()]; 
            response.setData((LmcBrowseData []) bdArray.toArray(bds));
        } 

        return response;
	}

}
