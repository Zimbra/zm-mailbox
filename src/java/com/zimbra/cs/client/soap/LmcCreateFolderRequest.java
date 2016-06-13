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

public class LmcCreateFolderRequest extends LmcSoapRequest {

    private String mName;
    private String mParentID;
    private String mView;
    

    /**
     * Set the ID of the parent of this new folder
     * @param n - the ID of the parent folder
     */
    public void setParentID(String id) { mParentID = id; }

    /**
     * Set the name of the folder to be created
     * @param n - the name of the new folder
     */
    public void setName(String n) { mName = n; }
    
    public void setView(String n) { mView = n; }

    public String getParentID() { return mParentID; }

    public String getName() { return mName; }

    public String getView() { return mView; }

    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(MailConstants.CREATE_FOLDER_REQUEST);
        Element f = DomUtil.add(request, MailConstants.E_FOLDER, "");
        DomUtil.addAttr(f, MailConstants.A_NAME, mName);
        DomUtil.addAttr(f, MailConstants.A_FOLDER, mParentID);
        if (mView != null) {
        	DomUtil.addAttr(f, MailConstants.A_DEFAULT_VIEW, mView);
        }
        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML) 
        throws ServiceException
    {
        Element folderElem = DomUtil.get(responseXML, MailConstants.E_FOLDER);
        LmcFolder f = parseFolder(folderElem);
        LmcCreateFolderResponse response = new LmcCreateFolderResponse();
        response.setFolder(f);
        return response;
    }

}
