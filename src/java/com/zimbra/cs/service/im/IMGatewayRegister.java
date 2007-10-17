/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.im;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.IMConstants;
import com.zimbra.cs.im.IMPersona;
import com.zimbra.cs.im.interop.Interop.ServiceName;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class IMGatewayRegister extends IMDocumentHandler 
{
    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        
        Element response = zsc.createElement(IMConstants.IM_GATEWAY_REGISTER_RESPONSE);
        IMPersona persona = super.getRequestedPersona(zsc);
        
        String op = request.getAttribute("op");
        String serviceStr = request.getAttribute("service");
        boolean result = true;
        if ("reg".equals(op)) {
            String nameStr = request.getAttribute("name");
            String pwStr = request.getAttribute("password");
            persona.gatewayRegister(ServiceName.valueOf(serviceStr), nameStr, pwStr);
        } else if ("reconnect".equals(op)) {
            persona.gatewayReconnect(ServiceName.valueOf(serviceStr));
        } else {
            persona.gatewayUnRegister(ServiceName.valueOf(serviceStr));
        }
        response.addAttribute("result", result);
        
        return response;
    }
}
