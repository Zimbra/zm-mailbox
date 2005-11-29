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
import com.zimbra.cs.mailbox.im.IMMessage;
import com.zimbra.cs.mailbox.im.IMPersona;
import com.zimbra.cs.mailbox.im.IMChat.Participant;
import com.zimbra.cs.mailbox.im.IMMessage.Lang;
import com.zimbra.cs.mailbox.im.IMMessage.TextPart;
import com.zimbra.soap.ZimbraContext;


public class IMGetChat extends DocumentHandler {

    public Element handle(Element request, Map context) throws ServiceException, SoapFaultException 
    {
        ZimbraContext lc = getZimbraContext(context);
        Mailbox mbox = super.getRequestedMailbox(lc);

        Element response = lc.createElement(MailService.IM_GET_CHAT_RESPONSE);
        

        String threadId = request.getAttribute("thread");
        
        IMPersona persona = mbox.getIMPersona();
        
        IMChat chat = persona.lookupChatOrNull(threadId);
        
        if (chat == null)
            throw ServiceException.FAILURE("Unknown thread: "+threadId, null);

        // chat
        Element ce = response.addElement("chat");
        ce.addAttribute("thread", chat.getThreadId());
        
        // participants
        {
            Element e = ce.addElement("pcps");
            for (Participant part : chat.participants()) {
                Element pe = e.addElement("p");
                pe.addAttribute("addr", part.getAddress());
            }
        }
        
        // messages
        {
            Element messages = ce.addElement("messages");
            int curOffset = 0;
            
            for (IMMessage msg : chat.messages()) {
                Element me = messages.addElement("message");
                me.addAttribute("seq", curOffset+chat.getFirstSeqNo());

                // subject 
                {
                    TextPart subj = msg.getSubject(Lang.DEFAULT);
                    if (subj != null) {
                        Element se = me.addElement("subject");
                        se.setText(subj.getHtmlText());
                    }
                }
                
                // body
                {
                    TextPart body = msg.getBody(Lang.DEFAULT);
                    if (body != null) {
                        Element se = me.addElement("body");
                        se.setText(body.getHtmlText());
                    }
                }
                
                curOffset++;
            }
        }
        
        response.addAttribute("thread", threadId);
        
        return response;        
    }

}
