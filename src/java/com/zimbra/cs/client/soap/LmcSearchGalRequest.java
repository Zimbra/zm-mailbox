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

import com.zimbra.common.soap.DomUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.SoapParseException;
import com.zimbra.cs.client.*;

public class LmcSearchGalRequest extends LmcSoapRequest {

    private String mName;
    
    public void setName(String n) { mName = n; }
    
    public String getName() { return mName; }
    
	protected Element getRequestXML() throws LmcSoapClientException {
		Element request = DocumentHelper.createElement(AccountConstants.SEARCH_GAL_REQUEST);
		DomUtil.add(request, AccountConstants.E_NAME, mName);
        return request;
    }

	protected LmcSoapResponse parseResponseXML(Element responseXML)
	    throws SoapParseException, ServiceException, LmcSoapClientException 
    {
        LmcContact contacts[] = parseContactArray(responseXML);
        LmcSearchGalResponse sgResp = new LmcSearchGalResponse();
        sgResp.setContacts(contacts);
        return sgResp;
	}

}
