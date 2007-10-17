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
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.IMConstants;
import com.zimbra.cs.im.IMPersona;
import com.zimbra.cs.service.im.IMDocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;

public class IMJoinChat extends IMDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Element response = zsc.createElement(IMConstants.IM_JOIN_CHAT_RESPONSE);
        
        String threadId = request.getAttribute(IMConstants.A_THREAD_ID);
        String addr = request.getAttribute(IMConstants.A_ADDRESS);
        
        IMPersona persona = super.getRequestedPersona(zsc);
        synchronized(persona.getLock()) {
            persona.joinChat(addr, threadId);
        }
        return response;
    }

}
