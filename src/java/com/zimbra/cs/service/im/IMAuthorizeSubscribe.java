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
import com.zimbra.common.soap.Element;
import com.zimbra.cs.im.IMAddr;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.soap.ZimbraSoapContext;

public class IMAuthorizeSubscribe extends IMDocumentHandler {
    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException, SoapFaultException
    {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Element response = zsc.createElement(IMConstants.IM_AUTHORIZE_SUBSCRIBE_RESPONSE);
        
        IMAddr addr = new IMAddr(request.getAttribute(IMConstants.A_ADDRESS));
        boolean authorized = request.getAttributeBool(IMConstants.A_AUTHORIZED);
        boolean add = request.getAttributeBool(IMConstants.A_ADD, false);
        String name = request.getAttribute(IMConstants.A_NAME, "");
        String groupStr = request.getAttribute(IMConstants.A_GROUPS, null);
        String[] groups;
        if (groupStr != null) 
            groups = groupStr.split(",");
        else
            groups = new String[0];

        OperationContext oc = getOperationContext(zsc, context);
        
        getRequestedMailbox(zsc).getPersona().authorizeSubscribe(oc, addr, authorized, add, name, groups);

        return response;
    }

}
