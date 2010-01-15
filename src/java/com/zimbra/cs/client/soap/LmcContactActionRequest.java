/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.DomUtil;

public class LmcContactActionRequest extends LmcSoapRequest {

    private String mIDList;

    private String mOp;

    private String mFolder;

    private String mTag;

    /**
     * Set the list of Contact ID's to operate on
     * @param idList - a list of the contacts to operate on
     */
    public void setIDList(String idList) {
    	mIDList = idList;
    }
    
    /**
     * Set the operation
     * @param op - the operation (delete, read, etc.).  It's up to the client
     * to put a "!" in front of the operation if negation is desired.
     */
    public void setOp(String op) {
    	mOp = op;
    }
    
    public void setTag(String t) {
    	mTag = t;
    }
    
    public void setFolder(String f) {
    	mFolder = f;
    }
    
    public String getIDList() {
    	return mIDList;
    }
    
    public String getOp() {
    	return mOp;
    }
    
    public String getFolder() {
    	return mFolder;
    }
    
    public String getTage() {
    	return mTag;
    }
    
    protected Element getRequestXML() {
    	Element request = DocumentHelper.createElement(MailConstants.CONTACT_ACTION_REQUEST);
    	Element a = DomUtil.add(request, MailConstants.E_ACTION, "");
    	DomUtil.addAttr(a, MailConstants.A_ID, mIDList);
    	DomUtil.addAttr(a, MailConstants.A_OPERATION, mOp);
    	addAttrNotNull(a, MailConstants.A_TAG, mTag);
    	addAttrNotNull(a, MailConstants.A_FOLDER, mFolder);
    	return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML)
        throws ServiceException 
    {
    	LmcContactActionResponse response = new LmcContactActionResponse();
    	Element a = DomUtil.get(responseXML, MailConstants.E_ACTION);
    	response.setIDList(DomUtil.getAttr(a, MailConstants.A_ID));
    	response.setOp(DomUtil.getAttr(a, MailConstants.A_OPERATION));
    	return response;
    }
}
