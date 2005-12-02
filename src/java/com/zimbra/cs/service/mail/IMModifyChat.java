/*
 * ***** BEGIN LICENSE BLOCK *****
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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.im.IMChat;
import com.zimbra.cs.mailbox.im.IMPersona;
import com.zimbra.cs.mailbox.im.IMRouter;
import com.zimbra.soap.ZimbraContext;

public class IMModifyChat extends DocumentHandler 
{
    static enum Op {
        CLOSE;
    }

    public Element handle(Element request, Map context) throws ServiceException, SoapFaultException 
    {
        ZimbraContext lc = getZimbraContext(context);
        Mailbox mbox = super.getRequestedMailbox(lc);

        Element response = lc.createElement(MailService.IM_MODIFY_CHAT_RESPONSE);
        
        String threadId = request.getAttribute("thread");
        
        synchronized(mbox) {
            IMPersona persona = IMRouter.getInstance().findPersona(lc.getOperationContext(), mbox, true);
            
            IMChat chat = persona.lookupChatOrNull(threadId);
            
            if (chat == null)
            throw ServiceException.FAILURE("Unknown thread: "+threadId, null);
            
            String opStr = request.getAttribute("op");
            Op op = Op.valueOf(opStr);
            
            switch(op) {
            case CLOSE:
                persona.closeChat(lc.getOperationContext(), chat);
                break;
            }
        }
        
        return response;        
    }

}
