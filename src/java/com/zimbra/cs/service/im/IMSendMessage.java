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
import com.zimbra.cs.im.IMMessage;
import com.zimbra.cs.im.IMPersona;
import com.zimbra.cs.im.IMMessage.TextPart;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.soap.ZimbraSoapContext;

public class IMSendMessage extends IMDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException, SoapFaultException 
    {
        ZimbraSoapContext lc = getZimbraSoapContext(context);

        Element response = lc.createElement(IMService.IM_SEND_MESSAGE_RESPONSE);

        Element msgElt = request.getElement(IMService.E_MESSAGE);
        
        String threadId = msgElt.getAttribute(IMService.A_THREAD_ID, null);
        String addr = msgElt.getAttribute(IMService.A_ADDRESS);
        
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
            
            persona.sendMessage(oc, new IMAddr(addr), threadId, msg);
        }
        
        response.addAttribute(IMService.A_THREAD_ID, threadId);
        
        return response;        
    }
}
