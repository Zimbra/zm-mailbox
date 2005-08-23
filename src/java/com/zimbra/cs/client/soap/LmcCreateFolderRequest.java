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
import com.zimbra.cs.client.*;

public class LmcCreateFolderRequest extends LmcSoapRequest {

    private String mName;
    private String mParentID;
    

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
    
    public String getParentID() { return mParentID; }

    public String getName() { return mName; }

    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(MailService.CREATE_FOLDER_REQUEST);
        Element f = DomUtil.add(request, MailService.E_FOLDER, "");  
        DomUtil.addAttr(f, MailService.A_NAME, mName);
        DomUtil.addAttr(f, MailService.A_FOLDER, mParentID);
        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML) 
        throws ServiceException
    {
        Element folderElem = DomUtil.get(responseXML, MailService.E_FOLDER);
        LmcFolder f = parseFolder(folderElem);
        LmcCreateFolderResponse response = new LmcCreateFolderResponse();
        response.setFolder(f);
        return response;
    }

}
