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
import com.zimbra.common.soap.DomUtil;

import org.dom4j.DocumentHelper;

import com.zimbra.cs.client.*;


public class LmcCreateNoteRequest extends LmcSoapRequest {

    private String mPosition;
    private String mParentID;
    private String mColor;
    private String mContent;
    
    public void setParentID(String id) { mParentID = id; }
    public void setPosition(String p) { mPosition = p; }
    public void setColor(String c) { mColor = c; }
    public void setContent(String c) { mContent = c; }
    
    public String getParentID() { return mParentID; }
    public String getColor() { return mColor; }
    public String getContent() { return mContent; }
    public String getPosition() { return mPosition; }


    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(MailConstants.CREATE_NOTE_REQUEST);
        Element f = DomUtil.add(request, MailConstants.E_NOTE, "");
        Element c = DomUtil.add(f, MailConstants.E_CONTENT, mContent);
        addAttrNotNull(f, MailConstants.A_BOUNDS, mPosition);
        addAttrNotNull(f, MailConstants.A_FOLDER, mParentID);
        addAttrNotNull(f, MailConstants.A_COLOR, mColor);
        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML) 
        throws ServiceException
    {
        Element noteElem = DomUtil.get(responseXML, MailConstants.E_NOTE);
        LmcNote f = parseNote(noteElem);
        LmcCreateNoteResponse response = new LmcCreateNoteResponse();
        response.setNote(f);
        return response;
    }

}
