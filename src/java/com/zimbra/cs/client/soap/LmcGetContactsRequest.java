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
import com.zimbra.common.soap.DomUtil;
import com.zimbra.cs.client.*;

public class LmcGetContactsRequest extends LmcSoapRequest {

    private LmcContactAttr mAttrs[];
    private String mIDList[];
    
    public void setContacts(String c[]) { mIDList = c; }
    public void setAttrs(LmcContactAttr attrs[]) { mAttrs = attrs; }
    
    public String[] getContacts() { return mIDList; }
    public LmcContactAttr[] getAttrs() { return mAttrs; }

    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(MailConstants.GET_CONTACTS_REQUEST);
        
        // emit contact attributes if any
        for (int i = 0; mAttrs != null && i < mAttrs.length; i++)
            addContactAttr(request, mAttrs[i]);
        
        // emit specified contacts if any
        for (int i = 0; mIDList != null && i < mIDList.length; i++) {
            Element newCN = DomUtil.add(request, MailConstants.E_CONTACT, "");
            DomUtil.addAttr(newCN, MailConstants.A_ID, mIDList[i]);
        }

        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML) 
        throws ServiceException, LmcSoapClientException 
    {
        LmcGetContactsResponse response = new LmcGetContactsResponse();
        LmcContact cons[] = parseContactArray(responseXML);
        response.setContacts(cons);
        return response;
    }
}
