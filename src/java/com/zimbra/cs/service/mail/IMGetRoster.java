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

import java.util.HashMap;
import java.util.Map;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.im.IMBuddy;
import com.zimbra.cs.mailbox.im.IMChat;
import com.zimbra.cs.mailbox.im.IMGroup;
import com.zimbra.cs.mailbox.im.IMPersona;
import com.zimbra.cs.mailbox.im.IMPresence;
import com.zimbra.cs.mailbox.im.IMRouter;
import com.zimbra.cs.mailbox.im.IMChat.Participant;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.ZimbraContext;

public class IMGetRoster extends DocumentHandler {
    
    public Element handle(Element request, Map context) throws ServiceException, SoapFaultException {
        ZimbraContext lc = getZimbraContext(context);
        Mailbox mbox = super.getRequestedMailbox(lc);

        Element response = lc.createElement(MailService.IM_GET_ROSTER_RESPONSE);
        
        synchronized (mbox) { 
            IMPersona persona = IMRouter.getInstance().findPersona(lc.getOperationContext(), mbox, true);
            
            persona.getMyPresence().toXml(response);

//            Map<IMGroup, Integer> groupIdCache = new HashMap();
            
//            // groups
//            {
//                Element groups = response.addUniqueElement("groups");
//                Integer curNum = 0;
//                for (IMGroup grp : persona.groups()) {
//                    groupIdCache.put(grp, curNum);
//                    
//                    Element e = groups.addElement("group");
//                    e.addAttribute("num", curNum);
//                    e.addAttribute("name", grp.getName());
//                    curNum++;
//                }
//            }
            
            
            // chats
            {
                Element chats = response.addUniqueElement("chats");
                for (IMChat chat : persona.chats()) {
                    Element e = chats.addElement("chat");
                    e.addAttribute("threadId", chat.getThreadId());
                    
                    Element participantsElt = e.addElement("pcps");
                    for (Participant part : chat.participants()) {
                        Element pe = participantsElt.addElement("p");
                        pe.addAttribute("addr", part.getAddress().getAddr());
                    }
                    
                }
            }
            
            // items (buddies)
            {
                Element items = response.addUniqueElement("items");
                for (IMBuddy buddy : persona.buddies()) {
                    Element e = items.addElement("item");
                    e.addAttribute("addr", buddy.getAddress().getAddr());
                    
                    String buddyName = buddy.getName();
                    if (buddyName != null && buddyName.length()>0) 
                        e.addAttribute("name", buddy.getName());
                    
                    e.addAttribute("subscription", buddy.getSubType().toString());
                    
                    // presence
                    IMPresence presence = buddy.getPresence();
                    if (presence == null) 
                        e.addUniqueElement("presence");
                    else
                        presence.toXml(e);
                    
                    StringBuffer groupStr = null;
                    // groups
                    for (IMGroup grp : buddy.groups()) {
                        if (groupStr == null)
                            groupStr = new StringBuffer(grp.getName());
                        else 
                            groupStr.append(',').append(grp.getName());
                        
//                        Integer idx = groupIdCache.get(grp);
//                        if (groupStr == null)
//                            groupStr = new StringBuffer(idx.toString());
//                        else 
//                            groupStr.append(',').append(idx.toString());
                    }
                    if (groupStr != null) 
                        e.addAttribute("groups", groupStr.toString());
                    
                }
            }
        }

        return response;        
    }
}
