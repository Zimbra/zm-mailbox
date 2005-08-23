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

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.soap.DomUtil;
import com.zimbra.soap.SoapParseException;
import com.zimbra.cs.client.*;

public class LmcCreateContactRequest extends LmcSoapRequest {

	private LmcContact mContact;
    
    /**
     * This method only sends the parameters from contact that the SOAP
     * protocol will accept.  That means folder ID, tags, and attributes.
     * Flags are currently ignored.
     * @param c - contact to create
     */
    public void setContact(LmcContact c) { mContact = c; }
    
    public LmcContact getContact() { return mContact; }
    
	protected Element getRequestXML() throws LmcSoapClientException {
		Element request = DocumentHelper.createElement(MailService.CREATE_CONTACT_REQUEST);
        Element newCN = DomUtil.add(request, MailService.E_CONTACT, "");
        LmcSoapRequest.addAttrNotNull(newCN, MailService.A_FOLDER, mContact.getFolder());
        LmcSoapRequest.addAttrNotNull(newCN, MailService.A_TAGS, mContact.getTags());
        
        // emit contact attributes if any
        LmcContactAttr attrs[] = mContact.getAttrs();
		for (int i = 0; attrs != null && i < attrs.length; i++)
			addContactAttr(newCN, attrs[i]);
		
        return request;
    }

	protected LmcSoapResponse parseResponseXML(Element responseXML)
			throws SoapParseException, ServiceException, LmcSoapClientException {
		
        LmcCreateContactResponse response = new LmcCreateContactResponse();
        LmcContact c = parseContact(DomUtil.get(responseXML, MailService.E_CONTACT));
        response.setContact(c);
        return response;
	}

}
