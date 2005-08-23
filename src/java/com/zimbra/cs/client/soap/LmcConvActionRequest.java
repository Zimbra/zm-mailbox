/*
***** BEGIN LICENSE BLOCK *****
Version: ZPL 1.1

The contents of this file are subject to the Zimbra Public License
Version 1.1 ("License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.zimbra.com/license

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
the License for the specific language governing rights and limitations
under the License.

The Original Code is: Zimbra Collaboration Suite.

The Initial Developer of the Original Code is Zimbra, Inc.  Portions
created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
Reserved.

Contributor(s): 

***** END LICENSE BLOCK *****
*/

package com.zimbra.cs.client.soap;

import org.dom4j.Element;
import org.dom4j.DocumentHelper;

import com.zimbra.soap.DomUtil;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;


public class LmcConvActionRequest extends LmcSoapRequest {

    private String mIDList;
    private String mOp;
    private String mTag;
    private String mFolder;
    private String mPosition;
    private String mContent;
    

    /**
     * Set the list of Conv ID's to operate on
     * @param idList - a list of the messages to operate on
     */
    public void setConvList(String idList) { mIDList = idList; }

    /**
     * Set the operation
     * @param op - the operation (delete, read, etc.)
     */
    public void setOp(String op) { mOp = op; }

    public void setTag(String t) { mTag = t; }
    public void setFolder(String f) { mFolder = f; }
    public void setPosition(String p) { mPosition = p; }
    public void setContent(String c) { mContent = c; }
    
    public String getConvList() { return mIDList; }
    public String getOp() { return mOp; }
    public String getTag() { return mTag; }
    public String getFolder() { return mFolder; }
    public String getContent() { return mContent; }
    public String getPosition() { return mPosition; }

    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(MailService.CONV_ACTION_REQUEST);
        Element a = DomUtil.add(request, MailService.E_ACTION, "");
        DomUtil.addAttr(a, MailService.A_ID, mIDList);
        DomUtil.addAttr(a, MailService.A_OPERATION, mOp);
        DomUtil.addAttr(a, MailService.A_TAG, mTag);
        DomUtil.addAttr(a, MailService.A_FOLDER, mFolder);
        if (mContent != null)
        	DomUtil.add(a, MailService.E_CONTENT, mContent);
        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML) 
        throws ServiceException
    {
        LmcConvActionResponse response = new LmcConvActionResponse();
        Element a = DomUtil.get(responseXML, MailService.E_ACTION);
        response.setConvList(DomUtil.getAttr(a, MailService.A_ID));
        response.setOp(DomUtil.getAttr(a, MailService.A_OPERATION));
        return response;
    }

}
