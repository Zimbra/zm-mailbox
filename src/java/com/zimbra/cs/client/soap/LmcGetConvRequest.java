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
        Element request = DocumentHelper.createElement(MailConstants.GET_CONV_REQUEST);

        // set the ID of the conversation to get
        Element convElement = DomUtil.add(request, MailConstants.E_CONV, "");
        DomUtil.addAttr(convElement, MailConstants.A_ID, mConvID);

        // add message elements within the conversation element if desired
        if (mMsgsToGet != null) {
            for (int i = 0; i < mMsgsToGet.length; i++) {
                Element m = DomUtil.add(convElement, MailConstants.E_MSG, "");
                DomUtil.addAttr(m, MailConstants.A_ID, mMsgsToGet[i]);
            }
        }

        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML) 
        throws ServiceException, LmcSoapClientException
    {
        // the response will always be exactly one conversation
        LmcConversation c = parseConversation(DomUtil.get(responseXML, MailConstants.E_CONV));
        LmcGetConvResponse response = new LmcGetConvResponse();
        response.setConv(c);
        return response;
    }

}
