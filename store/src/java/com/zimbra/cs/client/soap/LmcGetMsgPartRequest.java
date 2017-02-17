/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.client.soap;

import org.dom4j.Element;

import com.zimbra.common.service.ServiceException;


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
