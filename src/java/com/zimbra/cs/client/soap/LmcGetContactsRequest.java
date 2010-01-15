/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.DomUtil;
import com.zimbra.cs.client.*;

public class LmcGetContactsRequest extends LmcSoapRequest {

    private LmcContactAttr mAttrs[];
    private String mIDList[];
    
    public void setContacts(String c[]) { mIDList = c; }
    public void setAttrs(LmcContactAttr attrs[]) { mAttrs = attrs; }
    
    public String[] getContacts() { return mIDList; }
    public LmcContactAttr[] getAttrs() { return mAttrs; }

    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(MailConstants.GET_CONTACTS_REQUEST);
        
        // emit contact attributes if any
        for (int i = 0; mAttrs != null && i < mAttrs.length; i++)
            addContactAttr(request, mAttrs[i]);
        
        // emit specified contacts if any
        for (int i = 0; mIDList != null && i < mIDList.length; i++) {
            Element newCN = DomUtil.add(request, MailConstants.E_CONTACT, "");
            DomUtil.addAttr(newCN, MailConstants.A_ID, mIDList[i]);
        }

        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML) 
        throws ServiceException, LmcSoapClientException 
    {
        LmcGetContactsResponse response = new LmcGetContactsResponse();
        LmcContact cons[] = parseContactArray(responseXML);
        response.setContacts(cons);
        return response;
    }
}
