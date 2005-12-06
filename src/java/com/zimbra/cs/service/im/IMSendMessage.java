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
package com.zimbra.cs.service.im;

import java.util.Map;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;

import com.zimbra.cs.im.IMAddr;
import com.zimbra.cs.im.IMMessage;
import com.zimbra.cs.im.IMPersona;
import com.zimbra.cs.im.IMMessage.TextPart;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.soap.ZimbraContext;

public class IMSendMessage extends IMDocumentHandler {

    public Element handle(Element request, Map context) throws ServiceException, SoapFaultException 
    {
        ZimbraContext lc = getZimbraContext(context);

        Element response = lc.createElement(IMService.IM_SEND_MESSAGE_RESPONSE);

        Element msgElt = request.getElement(IMService.E_MESSAGE);
        
        String threadId = msgElt.getAttribute(IMService.A_THREAD_ID, null);
        String addr = null;
        
        if (threadId == null)
            addr = msgElt.getAttribute(IMService.A_ADDRESS);
        
        String subject = null;
        String body = null;
        
        Element subjElt = msgElt.getOptionalElement(IMService.E_SUBJECT);
        if (subjElt != null) {
            subject = subjElt.getText();
        }
        
        Element bodyElt = msgElt.getOptionalElement(IMService.E_BODY);
        if (bodyElt != null) {
            body = bodyElt.getText();
        }
        
        IMMessage msg = new IMMessage(subject==null?null:new TextPart(subject),
                body==null?null:new TextPart(body));
                
        OperationContext oc = lc.getOperationContext();
        
        Object lock = super.getLock(lc);
        synchronized(lock) {
            IMPersona persona = super.getRequestedPersona(lc, lock);
            
            if (threadId != null) {
                persona.sendMessage(oc, threadId, msg);
            } else {
                threadId = persona.newChat(oc, new IMAddr(addr), msg);
            }
        }
        
        response.addAttribute(IMService.A_THREAD_ID, threadId);
        
        return response;        
    }
}
