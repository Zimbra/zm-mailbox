/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
