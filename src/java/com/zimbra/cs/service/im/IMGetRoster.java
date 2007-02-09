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
import com.zimbra.cs.im.IMChat;
import com.zimbra.cs.im.IMPersona;
import com.zimbra.cs.im.IMChat.Participant;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.soap.ZimbraSoapContext;

public class IMGetRoster extends IMDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException,
                SoapFaultException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);

        Element response = lc.createElement(IMConstants.IM_GET_ROSTER_RESPONSE);

        Object lock = super.getLock(lc);
        synchronized (lock) {
            IMPersona persona = super.getRequestedPersona(lc, lock);

            Element pres = response.addUniqueElement(IMConstants.E_PRESENCE);
            persona.getEffectivePresence().toXml(pres);

            // chats
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

            persona.getRoster(lc.getOperationContext());
        }

        ZimbraLog.im.debug("GET ROSTER RESPONSE:\n" + response.toXML().asXML());

        return response;
    }
}
