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

import com.zimbra.cs.im.IMChat;
import com.zimbra.cs.im.IMMessage;
import com.zimbra.cs.im.IMPersona;
import com.zimbra.cs.im.IMChat.Participant;
import com.zimbra.cs.im.IMMessage.Lang;
import com.zimbra.cs.im.IMMessage.TextPart;
import com.zimbra.soap.ZimbraContext;

public class IMGetChat extends IMDocumentHandler {
    
    public Element handle(Element request, Map context) throws ServiceException, SoapFaultException 
    {
        ZimbraContext lc = getZimbraContext(context);
        
        Element response = lc.createElement(IMService.IM_GET_CHAT_RESPONSE);
        
        String threadId = request.getAttribute(IMService.A_THREAD_ID); 
        
        Object lock = super.getLock(lc);
        synchronized (lock) {
            IMPersona persona = super.getRequestedPersona(lc, lock);
            
            IMChat chat = persona.lookupChatOrNull(threadId);
            
            if (chat == null)
                throw ServiceException.FAILURE("Unknown thread: "+threadId, null);
            
            // chat
            Element ce = response.addElement(IMService.E_CHAT);
            ce.addAttribute(IMService.A_THREAD_ID, chat.getThreadId());
            
            // participants
            {
                Element e = ce.addElement(IMService.E_PARTICIPANTS);
                for (Participant part : chat.participants()) {
                    Element pe = e.addElement(IMService.E_PARTICIPANT);
                    pe.addAttribute(IMService.A_ADDRESS, part.getAddress().getAddr());
                }
            }
            
            // messages
            {
                Element messages = ce.addElement(IMService.E_MESSAGES);
                int curOffset = 0;
                
                for (IMMessage msg : chat.messages()) {
                    Element me = messages.addElement(IMService.E_MESSAGE);
                    me.addAttribute(IMService.A_SEQ, curOffset+chat.getFirstSeqNo());
                    me.addAttribute(IMService.A_TIMESTAMP, msg.getTimestamp());
                    me.addAttribute(IMService.A_FROM, msg.getFrom().getAddr());
                    
                    // subject 
                    {
                        TextPart subj = msg.getSubject(Lang.DEFAULT);
                        if (subj != null) {
                            Element se = me.addElement(IMService.E_SUBJECT);
                            se.setText(subj.getHtmlText());
                        }
                    }
                    
                    // body
                    {
                        TextPart body = msg.getBody(Lang.DEFAULT);
                        if (body != null) {
                            Element se = me.addElement(IMService.E_BODY);
                            se.setText(body.getHtmlText());
                        }
                    }
                    
                    curOffset++;
                }
            }
        }
        
        response.addAttribute(IMService.A_THREAD_ID, threadId);
        
        return response;        
    }
    
}
