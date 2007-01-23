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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.IMConstants;
import com.zimbra.cs.im.IMBuddy;
import com.zimbra.cs.im.IMChat;
import com.zimbra.cs.im.IMGroup;
import com.zimbra.cs.im.IMPersona;
import com.zimbra.cs.im.IMPresence;
import com.zimbra.cs.im.IMChat.Participant;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.soap.ZimbraSoapContext;

public class IMGetRoster extends IMDocumentHandler {
    
    public Element handle(Element request, Map<String, Object> context) throws ServiceException, SoapFaultException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);

        Element response = lc.createElement(IMConstants.IM_GET_ROSTER_RESPONSE);

        Object lock = super.getLock(lc);
        synchronized (lock) { 
            IMPersona persona = super.getRequestedPersona(lc, lock);
            
            {
                Element pe = response.addUniqueElement(IMConstants.E_PRESENCE);
                persona.getEffectivePresence().toXml(pe);
            }

            // chats
            {
                Element chats = response.addUniqueElement(IMConstants.E_CHATS);
                for (IMChat chat : persona.chats()) {
                    Element e = chats.addElement(IMConstants.E_CHATS);
                    e.addAttribute(IMConstants.A_THREAD_ID, chat.getThreadId());
                    
                    Element participantsElt = e.addElement(IMConstants.E_PARTICIPANTS);
                    for (Participant part : chat.participants()) {
                        Element pe = participantsElt.addElement(IMConstants.E_PARTICIPANT);
                        pe.addAttribute(IMConstants.A_ADDRESS, part.getAddress().getAddr());
                    }
                    
                }
            }
            
            persona.getRoster(lc.getOperationContext());
            
//            // items (buddies)
//            {
//                Element items = response.addUniqueElement(IMConstants.E_ITEMS);
//                for (IMBuddy buddy : persona.buddies()) {
//                    Element e = items.addElement(IMConstants.E_ITEM);
//                    e.addAttribute(IMConstants.A_ADDRESS, buddy.getAddress().getAddr());
//                    
//                    String buddyName = buddy.getName();
//                    if (buddyName != null && buddyName.length()>0) 
//                        e.addAttribute(IMConstants.A_NAME, buddy.getName());
//                    
//                    e.addAttribute(IMConstants.A_SUBSCRIPTION, buddy.getSubType().toString());
//                    
//                    if (buddy.getAsk() != null) {
//                        e.addAttribute("ask", buddy.getAsk().name());
//                    }
//                    
//                    // presence
//                    IMPresence presence = buddy.getPresence();
//                    if (presence == null) 
//                        e.addUniqueElement(IMConstants.E_PRESENCE);
//                    else {
//                        Element pe = e.addUniqueElement(IMConstants.E_PRESENCE);
//                        presence.toXml(pe);
//                    }
//                    
//                    StringBuffer groupStr = null;
//                    // groups
//                    for (IMGroup grp : buddy.groups()) {
//                        if (groupStr == null)
//                            groupStr = new StringBuffer(grp.getName());
//                        else 
//                            groupStr.append(',').append(grp.getName());
//                    }
//                    if (groupStr != null) 
//                        e.addAttribute(IMConstants.A_GROUPS, groupStr.toString());
//                    
//                }
//            }
        }

        
        ZimbraLog.im.debug("GET ROSTER RESPONSE:\n"+response.toXML().asXML());
        
        return response;        
    }
}
