/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.IMConstants;
import com.zimbra.cs.im.IMPersona;
import com.zimbra.cs.im.interop.Interop.ServiceName;
import com.zimbra.cs.im.interop.Interop.UserStatus;
import com.zimbra.cs.session.Session;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.Pair;
import com.zimbra.soap.ZimbraSoapContext;

public class IMGatewayList extends IMDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        
        Element response = zsc.createElement(IMConstants.IM_GATEWAY_LIST_RESPONSE);
        
        IMPersona persona = getRequestedPersona(zsc);
        synchronized (persona.getLock()) {
            
            // hacky side-effect: the client requests the gateway list first, so this
            // is currently where we create the client session
            List<Session> sessions = getReferencedSessions(zsc);
            for (Session s : sessions) {
                s.registerWithIM(persona);
            }
            
            List<Pair<ServiceName, UserStatus>> types = persona.getAvailableGateways();
            String domain = persona.getDomain();
            
            for (Pair<ServiceName, UserStatus> p : types) {
                Element typeElt = response.addElement("service");
                typeElt.addAttribute("type", p.getFirst().name());
                typeElt.addAttribute("domain", p.getFirst().name()+"."+domain);
                if (p.getSecond() != null) {
                    Element rElt = typeElt.addElement("registration");
                    rElt.addAttribute("name", p.getSecond().username);
                    rElt.addAttribute("state", p.getSecond().state.name().toLowerCase());
                    if (p.getSecond().nextConnectAttemptTime > 0) {
                        rElt.addAttribute("timeUntilNextConnect", p.getSecond().nextConnectAttemptTime-System.currentTimeMillis());
                    }
                } 
            }
        }
        return response;
    }
}
