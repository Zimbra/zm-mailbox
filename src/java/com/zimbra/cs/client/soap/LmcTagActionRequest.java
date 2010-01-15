/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.client.soap;

import org.dom4j.Element;
import org.dom4j.DocumentHelper;

import com.zimbra.common.soap.DomUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;

public class LmcTagActionRequest extends LmcSoapRequest {

    private String mIDList;
    private String mOp;
    private String mColor;
    private String mName;


    public void setTagList(String idList) { mIDList = idList; }
    public void setOp(String op) { mOp = op; }
    public void setName(String t) { mName = t; }
    public void setColor(String c) { mColor = c; }

    public String getTagList() { return mIDList; }
    public String getOp() { return mOp; }
    public String getColor() { return mColor; }
    public String getName() { return mName; }


    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(MailConstants.TAG_ACTION_REQUEST);
        Element a = DomUtil.add(request, MailConstants.E_ACTION, "");
        DomUtil.addAttr(a, MailConstants.A_OPERATION, mOp);
        DomUtil.addAttr(a, MailConstants.A_ID, mIDList);
        DomUtil.addAttr(a, MailConstants.A_NAME, mName);
        DomUtil.addAttr(a, MailConstants.A_COLOR, mColor);
        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML)
        throws ServiceException
    {
        LmcTagActionResponse response = new LmcTagActionResponse();
        Element a = DomUtil.get(responseXML, MailConstants.E_ACTION);
        response.setOp(DomUtil.getAttr(a, MailConstants.A_OPERATION));
        response.setTagList(DomUtil.getAttr(a, MailConstants.A_ID));
        return response;
    }
}
