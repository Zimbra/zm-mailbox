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

import org.dom4j.DocumentException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.IMConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;

import com.zimbra.cs.im.IMAddr;
import com.zimbra.cs.im.IMMessage;
import com.zimbra.cs.im.IMPersona;
import com.zimbra.cs.im.IMUtils;
import com.zimbra.cs.im.IMMessage.TextPart;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.soap.ZimbraSoapContext;

public class IMSendMessage extends IMDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException, SoapFaultException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        OperationContext octxt = getOperationContext(zsc, context);

        Element response = zsc.createElement(IMConstants.IM_SEND_MESSAGE_RESPONSE);

        Element msgElt = request.getElement(IMConstants.E_MESSAGE);

        String threadId = msgElt.getAttribute(IMConstants.A_THREAD_ID, null);
        String addr = IMUtils.resolveAddress(msgElt.getAttribute(IMConstants.A_ADDRESS));

        String subject = null;
        TextPart bodyPart = null;

        Element subjElt = msgElt.getOptionalElement(IMConstants.E_SUBJECT);
        if (subjElt != null) {
            subject = subjElt.getText();
        }

        Element bodyElt = msgElt.getOptionalElement(IMConstants.E_BODY);
        if (bodyElt != null) {
            try {
                String s = bodyElt.getText();
                org.dom4j.Element root = org.dom4j.DocumentHelper.createElement("root");
                org.dom4j.Element parsed = org.dom4j.DocumentHelper.parseText(s).getRootElement();
                org.dom4j.Element xhtmlBody = root.addElement("body", "http://www.w3.org/1999/xhtml");
                xhtmlBody.add(parsed);
                xhtmlBody.detach();
                bodyPart = new TextPart(xhtmlBody);
            } catch (DocumentException e) {
                bodyPart = new TextPart(bodyElt.getText());
            }
        }

        boolean isTyping = false;
        if (msgElt.getOptionalElement(IMConstants.E_TYPING) != null)
            isTyping = true;

        IMMessage msg = new IMMessage(subject==null?null:new TextPart(subject),
            bodyPart, isTyping);

        IMPersona persona = super.getRequestedPersona(zsc);
        synchronized(persona.getLock()) {
            persona.sendMessage(octxt, new IMAddr(addr), threadId, msg);
        }

        response.addAttribute(IMConstants.A_THREAD_ID, threadId);

        return response;        
    }
}
