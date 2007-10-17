/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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
import com.zimbra.cs.im.IMPersona;
import com.zimbra.cs.im.IMUtils;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.soap.ZimbraSoapContext;

public class IMSubscribe extends IMDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        OperationContext octxt = getOperationContext(zsc, context);

        String op = request.getAttribute(IMConstants.A_OPERATION);
        boolean add = true;
        if (op.equalsIgnoreCase("remove")) 
            add = false;

        IMAddr addr = new IMAddr(IMUtils.resolveAddress(request.getAttribute(IMConstants.A_ADDRESS)));

        Element response = zsc.createElement(IMConstants.IM_SUBSCRIBE_RESPONSE);
        response.addAttribute(IMConstants.A_ADDRESS, addr.toString());

        String name = request.getAttribute(IMConstants.A_NAME, "");
        String groupStr = request.getAttribute(IMConstants.A_GROUPS, null);
        String[] groups;
        if (groupStr != null) 
            groups = groupStr.split(",");
        else
            groups = new String[0];

        IMPersona persona = super.getRequestedPersona(zsc);
        synchronized (persona.getLock()) {
            if (add) 
                persona.addOutgoingSubscription(octxt, addr, name, groups);
            else
                persona.removeOutgoingSubscription(octxt, addr, name, groups);
        }

        return response;
    }
    
}
