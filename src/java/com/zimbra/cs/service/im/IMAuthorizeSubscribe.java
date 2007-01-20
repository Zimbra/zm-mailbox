/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
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

        OperationContext oc = zsc.getOperationContext();
        Object lock = super.getLock(zsc);
        synchronized (lock) {
            IMPersona persona = super.getRequestedPersona(zsc, lock);
            persona.authorizeSubscribe(oc, addr, authorized, add, name, groups);
        }
        return response;
    }

}
