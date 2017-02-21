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
import org.dom4j.DocumentHelper;

import com.zimbra.common.soap.DomUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;


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
        Element request = DocumentHelper.createElement(MailConstants.GET_MSG_REQUEST);
        Element m = DomUtil.add(request, MailConstants.E_MSG, "");
        DomUtil.addAttr(m, MailConstants.A_ID, mMsgID);
        addAttrNotNull(m, MailConstants.A_MARK_READ, mRead);
        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML)
        throws ServiceException, LmcSoapClientException
    {
        LmcGetMsgResponse response = new LmcGetMsgResponse();
        response.setMsg(parseMessage(DomUtil.get(responseXML, MailConstants.E_MSG)));
        return response;
    }
}
