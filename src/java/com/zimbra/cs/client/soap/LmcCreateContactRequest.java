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

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapParseException;
import com.zimbra.common.soap.DomUtil;
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
		Element request = DocumentHelper.createElement(MailConstants.CREATE_CONTACT_REQUEST);
        Element newCN = DomUtil.add(request, MailConstants.E_CONTACT, "");
        LmcSoapRequest.addAttrNotNull(newCN, MailConstants.A_FOLDER, mContact.getFolder());
        LmcSoapRequest.addAttrNotNull(newCN, MailConstants.A_TAGS, mContact.getTags());
        
        // emit contact attributes if any
        LmcContactAttr attrs[] = mContact.getAttrs();
		for (int i = 0; attrs != null && i < attrs.length; i++)
			addContactAttr(newCN, attrs[i]);
		
        return request;
    }

	protected LmcSoapResponse parseResponseXML(Element responseXML)
			throws SoapParseException, ServiceException, LmcSoapClientException {
		
        LmcCreateContactResponse response = new LmcCreateContactResponse();
        LmcContact c = parseContact(DomUtil.get(responseXML, MailConstants.E_CONTACT));
        response.setContact(c);
        return response;
	}

}
