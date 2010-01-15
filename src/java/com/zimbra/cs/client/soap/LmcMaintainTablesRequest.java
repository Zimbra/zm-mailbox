/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2007, 2009, 2010 Zimbra, Inc.
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

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.zimbra.common.soap.AdminConstants;


public class LmcMaintainTablesRequest extends LmcSoapRequest {

    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(AdminConstants.MAINTAIN_TABLES_REQUEST);
        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML)
    {
        String s = responseXML.attributeValue(AdminConstants.A_NUM_TABLES);
        int numTables = Integer.parseInt(s);
        return new LmcMaintainTablesResponse(numTables);
    }
}
