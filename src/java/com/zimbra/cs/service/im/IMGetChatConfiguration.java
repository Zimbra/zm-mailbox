/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
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
import com.zimbra.common.soap.IMConstants;
import com.zimbra.common.soap.Element;

import com.zimbra.cs.im.IMConferenceRoom;
import com.zimbra.cs.im.IMPersona;
import com.zimbra.cs.im.IMServiceException;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * 
 */
public class IMGetChatConfiguration extends IMDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        Element response = zsc.createElement(IMConstants.IM_GET_CHAT_CONFIGURATION_RESPONSE);
        String threadId = request.getAttribute(IMConstants.A_THREAD_ID, null);
        String addr = request.getAttribute(IMConstants.A_ADDRESS, null);
        
        if (threadId == null && addr == null) {
            throw ServiceException.INVALID_REQUEST("Missing required argument -- one of (thread, addr) must be specified", null);
        }
        
        boolean requestOwnerConfig = request.getAttributeBool("requestOwnerConfig", false);
        
        response.addAttribute(IMConstants.A_THREAD_ID, threadId);

        IMPersona persona = super.getRequestedPersona(zsc);
        
        try {
            IMConferenceRoom room = persona.getConferenceRoom(threadId, addr, requestOwnerConfig);
            if (room != null) 
                response = room.toXML(response);
        } catch (IMServiceException e) {
            if (e.getCode().equals(IMServiceException.NOT_A_CONFERENCE_ROOM)) {
                response.addAttribute(IMConstants.A_ERROR, "not_a_conference_room");
            } else if (e.getCode().equals(IMServiceException.NO_RESPONSE_FROM_REMOTE)) {
                response.addAttribute(IMConstants.A_ERROR, "no_response_from_remote");
            } else if (e.getCode().equals(IMServiceException.NOT_ALLOWED)) { 
                response.addAttribute(IMConstants.A_ERROR, "not_allowed");
            } else
                throw e;
        } catch (MailServiceException e) {
            if (e.getCode().equals(MailServiceException.NO_SUCH_CHAT)) {
                response.addAttribute(IMConstants.A_ERROR, "not_found");
            } else {
                throw e;
            }
        }
        return response;
    }
}
