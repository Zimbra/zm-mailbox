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
import com.zimbra.cs.client.*;


public class LmcGetFolderRequest extends LmcSoapRequest {

    private String mFolderID;
    
    public void setFolderToGet(String f) { mFolderID = f; }
    
    public String getFolderToGet() { return mFolderID; }

    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(MailConstants.GET_FOLDER_REQUEST);
        if (mFolderID != null) {
            Element folder = DomUtil.add(request, MailConstants.E_FOLDER, "");
            DomUtil.addAttr(folder, MailConstants.A_FOLDER, mFolderID);
        }
        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML) 
        throws ServiceException
    {
        // LmcGetFolderResponse always has the 1 top level folder
        Element fElem = DomUtil.get(responseXML, MailConstants.E_FOLDER);
        LmcFolder f = parseFolder(fElem);
        
        LmcGetFolderResponse response = new LmcGetFolderResponse();
        response.setRootFolder(f);
        return response;
    }

}
