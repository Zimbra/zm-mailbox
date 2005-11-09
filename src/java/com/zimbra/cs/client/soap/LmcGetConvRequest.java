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

import org.dom4j.Element;
import org.dom4j.DocumentHelper;

import com.zimbra.soap.DomUtil;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.client.*;


public class LmcGetConvRequest extends LmcSoapRequest {

    private String mConvID;
    private String mMsgsToGet[];
    
    // There is a single conversation to get.  Must be present.
    public void setConvToGet(String f) { mConvID = f; }

    // Set the ID's of the msgs within the conversation to get.  Optional.
    public void setMsgsToGet(String m[]) { mMsgsToGet = m; }
    
    public String getConvToGet() { return mConvID; }

    public String[] getMsgsToGet() { return mMsgsToGet; }


    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(MailService.GET_CONV_REQUEST);

        // set the ID of the conversation to get
        Element convElement = DomUtil.add(request, MailService.E_CONV, "");
        DomUtil.addAttr(convElement, MailService.A_ID, mConvID);  

        // add message elements within the conversation element if desired
        if (mMsgsToGet != null) {
            for (int i = 0; i < mMsgsToGet.length; i++) {
                Element m = DomUtil.add(convElement, MailService.E_MSG, "");
                DomUtil.addAttr(m, MailService.A_ID, mMsgsToGet[i]);
            }
        }

        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML) 
        throws ServiceException, LmcSoapClientException
    {
        // the response will always be exactly one conversation
        LmcConversation c = parseConversation(DomUtil.get(responseXML, MailService.E_CONV));
        LmcGetConvResponse response = new LmcGetConvResponse();
        response.setConv(c);
        return response;
    }

}
