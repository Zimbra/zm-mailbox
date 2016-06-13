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

import java.util.HashMap;
import java.util.Iterator;

import org.dom4j.Element;
import org.dom4j.DocumentHelper;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;


public class LmcGetInfoRequest extends LmcSoapRequest {


    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(AccountConstants.GET_INFO_REQUEST);
        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML)
        throws ServiceException
    {
        HashMap prefMap = new HashMap();
        LmcGetInfoResponse response = new LmcGetInfoResponse();

        // iterate over all the elements we received
        for (Iterator it = responseXML.elementIterator(); it.hasNext(); ) {
            Element e = (Element) it.next();

            // find out what element it is and go process that
            String elementType = e.getQName().getName();
            if (elementType.equals(AccountConstants.E_NAME)) {
                response.setAcctName(e.getText());
            } else if (elementType.equals(AccountConstants.E_LIFETIME)) {
                response.setLifetime(e.getText());
            } else if (elementType.equals(AccountConstants.E_PREF)) {
                // add this preference to our map
                addPrefToMultiMap(prefMap, e);
            }
        }

        if (!prefMap.isEmpty())
            response.setPrefMap(prefMap);

        return response;
    }

}
