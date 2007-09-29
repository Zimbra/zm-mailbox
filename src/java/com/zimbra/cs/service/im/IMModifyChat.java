/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.im;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;

import com.zimbra.cs.im.IMAddr;
import com.zimbra.cs.im.IMChat;
import com.zimbra.cs.im.IMPersona;
import com.zimbra.soap.ZimbraSoapContext;

public class IMModifyChat extends IMDocumentHandler 
{
    static enum Op {
        CLOSE, ADDUSER;
    }

    public Element handle(Element request, Map<String, Object> context) throws ServiceException, SoapFaultException 
    {
        ZimbraSoapContext lc = getZimbraSoapContext(context);

        Element response = lc.createElement(IMService.IM_MODIFY_CHAT_RESPONSE);
        
        String threadId = request.getAttribute(IMService.A_THREAD_ID);
        
        Object lock = super.getLock(lc);
        
        synchronized(lock) {
            IMPersona persona = super.getRequestedPersona(lc, lock);
            
            IMChat chat = persona.lookupChatOrNull(threadId);
            
            if (chat == null) {
                throw ServiceException.FAILURE("Unknown thread: "+threadId, null);
            } else {
                String opStr = request.getAttribute(IMService.A_OPERATION);
                Op op = Op.valueOf(opStr.toUpperCase());
                
                switch(op) {
                    case CLOSE:
                        persona.closeChat(lc.getOperationContext(), chat);
                        break;
                    case ADDUSER:
                        String newUser = request.getAttribute(IMService.A_ADDRESS);
                        persona.addUserToChat(chat, new IMAddr(newUser));
                        break;
                }
            }
        }
        
        return response;        
    }

}
