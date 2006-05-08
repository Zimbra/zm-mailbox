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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.im;

import java.util.Map;

import com.zimbra.cs.im.IMBuddy;
import com.zimbra.cs.im.IMChat;
import com.zimbra.cs.im.IMGroup;
import com.zimbra.cs.im.IMPersona;
import com.zimbra.cs.im.IMPresence;
import com.zimbra.cs.im.IMChat.Participant;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.ZimbraSoapContext;

public class IMGetRoster extends IMDocumentHandler {
    
    public Element handle(Element request, Map<String, Object> context) throws ServiceException, SoapFaultException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);

        Element response = lc.createElement(IMService.IM_GET_ROSTER_RESPONSE);

        Object lock = super.getLock(lc);
        synchronized (lock) { 
            IMPersona persona = super.getRequestedPersona(lc, lock);
            
            {
                Element pe = response.addUniqueElement(IMService.E_PRESENCE);
                persona.getEffectivePresence().toXml(pe);
            }

            // chats
            {
                Element chats = response.addUniqueElement(IMService.E_CHATS);
                for (IMChat chat : persona.chats()) {
                    Element e = chats.addElement(IMService.E_CHATS);
                    e.addAttribute(IMService.A_THREAD_ID, chat.getThreadId());
                    
                    Element participantsElt = e.addElement(IMService.E_PARTICIPANTS);
                    for (Participant part : chat.participants()) {
                        Element pe = participantsElt.addElement(IMService.E_PARTICIPANT);
                        pe.addAttribute(IMService.A_ADDRESS, part.getAddress().getAddr());
                    }
                    
                }
            }
            
            // items (buddies)
            {
                Element items = response.addUniqueElement(IMService.E_ITEMS);
                for (IMBuddy buddy : persona.buddies()) {
                    Element e = items.addElement(IMService.E_ITEM);
                    e.addAttribute(IMService.A_ADDRESS, buddy.getAddress().getAddr());
                    
                    String buddyName = buddy.getName();
                    if (buddyName != null && buddyName.length()>0) 
                        e.addAttribute(IMService.A_NAME, buddy.getName());
                    
                    e.addAttribute(IMService.A_SUBSCRIPTION, buddy.getSubType().toString());
                    
                    // presence
                    IMPresence presence = buddy.getPresence();
                    if (presence == null) 
                        e.addUniqueElement(IMService.E_PRESENCE);
                    else {
                        Element pe = e.addUniqueElement(IMService.E_PRESENCE);
                        presence.toXml(pe);
                    }
                    
                    StringBuffer groupStr = null;
                    // groups
                    for (IMGroup grp : buddy.groups()) {
                        if (groupStr == null)
                            groupStr = new StringBuffer(grp.getName());
                        else 
                            groupStr.append(',').append(grp.getName());
                    }
                    if (groupStr != null) 
                        e.addAttribute(IMService.A_GROUPS, groupStr.toString());
                    
                }
            }
        }

        return response;        
    }
}
