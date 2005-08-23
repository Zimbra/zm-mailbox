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
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.client.soap;

import org.dom4j.Element;
import org.dom4j.DocumentHelper;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.soap.DomUtil;

public class LmcNoteActionRequest extends LmcSoapRequest {

    private String mIDList;
    private String mOp;
    private String mTargetFolder;
    private String mColor;
    private String mTag;
    

    /**
     * Set the list of Note ID's to operate on
     * @param idList - a list of the notes to operate on
     */
    public void setNoteList(String idList) { mIDList = idList; }

    /**
     * Set the operation
     * @param op - the operation (delete, read, etc.)
     */
    public void setOp(String op) { mOp = op; }

    public void setTag(String t) { mTag = t; }
    public void setTargetFolder(String f) { mTargetFolder = f; }
    public void setColor(String c) { mColor = c; }

    
    public String getNoteList() { return mIDList; }
    public String getOp() { return mOp; }
    public String getTargetFolder() { return mTargetFolder; }
    public String getColor() { return mColor; }
    public String getTag() { return mTag; }


    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(MailService.NOTE_ACTION_REQUEST);
        Element a = DomUtil.add(request, MailService.E_ACTION, "");
        DomUtil.addAttr(a, MailService.A_ID, mIDList);
        DomUtil.addAttr(a, MailService.A_OPERATION, mOp);
        DomUtil.addAttr(a, MailService.A_TAG, mTag);
        DomUtil.addAttr(a, MailService.A_FOLDER, mTargetFolder);
        DomUtil.addAttr(a, MailService.A_COLOR, mColor);
        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML) 
        throws ServiceException
    {
        LmcNoteActionResponse response = new LmcNoteActionResponse();
        Element a = DomUtil.get(responseXML, MailService.E_ACTION);
        response.setNoteList(DomUtil.getAttr(a, MailService.A_ID));
        response.setOp(DomUtil.getAttr(a, MailService.A_OPERATION));
        return response;
    }

}
