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

import java.util.ArrayList;
import java.util.Iterator;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.zimbra.common.soap.DomUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapParseException;
import com.zimbra.cs.client.*;

public class LmcBrowseRequest extends LmcSoapRequest {

    private String mBrowseBy;
    
    public void setBrowseBy(String b) { mBrowseBy = b; }
    
    public String getBrowseBy() { return mBrowseBy; }
    
	protected Element getRequestXML() throws LmcSoapClientException {
        Element request = DocumentHelper.createElement(MailConstants.BROWSE_REQUEST);
        DomUtil.addAttr(request, MailConstants.A_BROWSE_BY, mBrowseBy);
        return request;
	}

    protected LmcBrowseData parseBrowseData(Element bdElem) {
    	LmcBrowseData bd = new LmcBrowseData();
        bd.setFlags(bdElem.attributeValue(MailConstants.A_BROWSE_DOMAIN_HEADER));
        bd.setData(bdElem.getText());
        return bd;
    }
    
	protected LmcSoapResponse parseResponseXML(Element parentElem)
			throws SoapParseException, ServiceException, LmcSoapClientException 
    {
		LmcBrowseResponse response = new LmcBrowseResponse();
        ArrayList bdArray = new ArrayList();
        for (Iterator ait = parentElem.elementIterator(MailConstants.E_BROWSE_DATA); ait.hasNext(); ) {
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
