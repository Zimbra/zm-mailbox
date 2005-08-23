/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.  Portions
 * created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
 * Reserved.
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


public class LmcGetMsgRequest extends LmcSoapRequest {

    private String mMsgID;
    private String mRead;
    
    // There is a single msg to get.  Must be present.
    public void setMsgToGet(String f) { mMsgID = f; }

    // Optionally set read
    public void setRead(String r) { mRead = r; }
    
    public String getMsgToGet() { return mMsgID; }

    public String getRead() { return mRead; }


    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(MailService.GET_MSG_REQUEST);  
        Element m = DomUtil.add(request, MailService.E_MSG, "");
        DomUtil.addAttr(m, MailService.A_ID, mMsgID);
        addAttrNotNull(m, MailService.A_MARK_READ, mRead);
        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML) 
        throws ServiceException, LmcSoapClientException
    {
        LmcGetMsgResponse response = new LmcGetMsgResponse();
        response.setMsg(parseMessage(DomUtil.get(responseXML, MailService.E_MSG)));
        return response;
    }
}
