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
        Element request = DocumentHelper.createElement(MailService.TAG_ACTION_REQUEST);
        Element a = DomUtil.add(request, MailService.E_ACTION, "");
        DomUtil.addAttr(a, MailService.A_OPERATION, mOp);
        DomUtil.addAttr(a, MailService.A_ID, mIDList);
        DomUtil.addAttr(a, MailService.A_NAME, mName);
        DomUtil.addAttr(a, MailService.A_COLOR, mColor);
        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML) 
        throws ServiceException
    {
        LmcTagActionResponse response = new LmcTagActionResponse();
        Element a = DomUtil.get(responseXML, MailService.E_ACTION);
        response.setOp(DomUtil.getAttr(a, MailService.A_OPERATION));
        response.setTagList(DomUtil.getAttr(a, MailService.A_ID));
        return response;
    }
}
