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
import com.zimbra.cs.client.*;
import com.zimbra.cs.service.mail.MailService;

public class LmcAddMsgRequest extends LmcSoapRequest {

    private LmcMessage mMsg;
    
    /**
     * Set the message that will be added
     * @param m - the message to be added
     */
    public void setMsg(LmcMessage m) { mMsg = m; }

    public LmcMessage getMsg() { return mMsg; }

    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(MailService.ADD_MSG_REQUEST);
        addMsg(request, mMsg, null, null, null);
        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML) 
        throws ServiceException
    {
        Element m = DomUtil.get(responseXML, MailService.E_MSG);
        LmcAddMsgResponse response = new LmcAddMsgResponse();
        response.setID(DomUtil.getAttr(m, MailService.A_ID));
        return response;
    }

}

