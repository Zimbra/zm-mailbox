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
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.Element;

import com.zimbra.cs.im.IMAddr;
import com.zimbra.cs.im.IMChat;
import com.zimbra.cs.im.IMPersona;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.soap.ZimbraSoapContext;

public class IMModifyChat extends IMDocumentHandler {

    static enum Op {
        CLOSE, ADDUSER;
    }

    public Element handle(Element request, Map<String, Object> context) throws ServiceException, SoapFaultException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        OperationContext octxt = getOperationContext(zsc, context);

        Element response = zsc.createElement(IMConstants.IM_MODIFY_CHAT_RESPONSE);

        String threadId = request.getAttribute(IMConstants.A_THREAD_ID);
        response.addAttribute(IMConstants.A_THREAD_ID, threadId);
        
        IMPersona persona = super.getRequestedPersona(zsc);
        synchronized(persona.getLock()) {
            
            IMChat chat = null;
            
            if (persona != null)
                chat = persona.getChat(threadId);
            
            if (chat == null) {
                response.addAttribute(IMConstants.A_ERROR, "not_found");
            } else {
                String opStr = request.getAttribute(IMConstants.A_OPERATION);
                Op op = Op.valueOf(opStr.toUpperCase());
                
                switch (op) {
                    case CLOSE:
                        persona.closeChat(octxt, chat);
                        break;
                    case ADDUSER: {
                        String newUser = request.getAttribute(IMConstants.A_ADDRESS);
                        String inviteMessage = request.getText();
                        if (inviteMessage == null || inviteMessage.length() == 0)
                            inviteMessage = "Please join my chat";
                        persona.addUserToChat(octxt, chat, new IMAddr(newUser), inviteMessage);
                    }
                    break;
                }
            }
        }
        return response;        
    }

}
