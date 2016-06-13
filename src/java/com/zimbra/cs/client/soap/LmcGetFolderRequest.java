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
