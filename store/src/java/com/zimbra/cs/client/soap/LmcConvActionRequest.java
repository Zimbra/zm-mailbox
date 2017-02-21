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
        Element request = DocumentHelper.createElement(MailConstants.CONV_ACTION_REQUEST);
        Element a = DomUtil.add(request, MailConstants.E_ACTION, "");
        DomUtil.addAttr(a, MailConstants.A_ID, mIDList);
        DomUtil.addAttr(a, MailConstants.A_OPERATION, mOp);
        DomUtil.addAttr(a, MailConstants.A_TAG, mTag);
        DomUtil.addAttr(a, MailConstants.A_FOLDER, mFolder);
        if (mContent != null)
            DomUtil.add(a, MailConstants.E_CONTENT, mContent);
        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML)
        throws ServiceException
    {
        LmcConvActionResponse response = new LmcConvActionResponse();
        Element a = DomUtil.get(responseXML, MailConstants.E_ACTION);
        response.setConvList(DomUtil.getAttr(a, MailConstants.A_ID));
        response.setOp(DomUtil.getAttr(a, MailConstants.A_OPERATION));
        return response;
    }

}
