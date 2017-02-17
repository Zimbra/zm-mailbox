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
import com.zimbra.common.soap.MailConstants;

import org.dom4j.DocumentHelper;

import com.zimbra.common.soap.DomUtil;
import com.zimbra.cs.client.*;

public class LmcCreateTagRequest extends LmcSoapRequest {

    private String mName;
    private String mColor;
    

    public void setName(String n) { mName = n; }
    public void setColor(String c) { mColor = c; }

    public String getName() { return mName; }
    public String getColor() { return mColor; }


    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(MailConstants.CREATE_TAG_REQUEST);
        Element t = DomUtil.add(request, MailConstants.E_TAG, "");
        DomUtil.addAttr(t, MailConstants.A_NAME, mName);
        DomUtil.addAttr(t, MailConstants.A_COLOR, mColor);
        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML) 
        throws ServiceException
    {
        Element tagElem = DomUtil.get(responseXML, MailConstants.E_TAG);
        LmcTag f = parseTag(tagElem);
        LmcCreateTagResponse response = new LmcCreateTagResponse();
        response.setTag(f);
        return response;
    }

}
