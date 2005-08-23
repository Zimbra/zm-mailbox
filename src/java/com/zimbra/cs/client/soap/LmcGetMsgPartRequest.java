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

import com.zimbra.cs.service.ServiceException;


public class LmcGetMsgPartRequest extends LmcSoapRequest {

    private String mMsgID;
    private String mPartName;
    

    /**
     * Set the ID of the msg that has the target MIME part
     * @param n - the ID of the msg
     */
    public void setMsgID(String id) { mMsgID = id; }

    /**
     * Set the name of the message part to retrieve.
     * @param n - the name of the message part.
     */
    public void setPartName(String n) { mPartName = n; }
    
    public String getMsgID() { return mMsgID; }

    public String getPartName() { return mPartName; }

    protected Element getRequestXML() throws LmcSoapClientException {
        throw new LmcSoapClientException("this command not implemented by server");
        /*
        Element request = DocumentHelper.createElement("XXX there is no GetMsgPartRequest");
        Element m = DomUtil.add(request, MailService.E_MSG, "");
        DomUtil.addAttr(m, MailService.A_ID, mMsgID);
        Element mp = DomUtil.add(m, MailService.E_MIMEPART, "");
        DomUtil.addAttr(mp, MailService.A_PART, mPartName);
        return request;
        */
  
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML) 
        throws ServiceException, LmcSoapClientException
    {
        LmcGetMsgPartResponse response = new LmcGetMsgPartResponse();
        response.setMessage(parseMessage(responseXML));
        return response;
    }
}
