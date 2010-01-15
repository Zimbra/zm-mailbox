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
package com.zimbra.cs.im;

import org.xmpp.packet.Packet;

import com.zimbra.common.service.ServiceException;

/**
 * 
 */
public class IMServiceException extends ServiceException {
    private static final long serialVersionUID = 8303045261247860050L;
    
    public static final String INVALID_ADDRESS = "im.INVALID_ADDRESS";
    public static final String NOT_ALLOWED = "im.NOT_ALLOWED";
    public static final String NOT_A_CONFERENCE_SERVICE = "im.NOT_A_CONFERENCE_SERVICE";
    public static final String NOT_A_CONFERENCE_ROOM = "im.NOT_A_CONFERENCE_ROOM";
    public static final String NO_RESPONSE_FROM_REMOTE = "im.NO_RESPONSE";
    public static final String XMPP_ERROR = "im.XMPP_ERROR";
    public static final String NOT_A_MUC_CHAT = "im.NOT_A_MUC_CHAT";
    
    public static final String ADDR = "addr";
    public static final String THREAD = "thread";
    public static final String PACKET = "packet";
    
    IMServiceException(String message, String code, boolean isReceiversFault, Argument... args) {
        super(message, code, isReceiversFault, args);
    }
    
    public static IMServiceException INVALID_ADDRESS(String addr) {
        return new IMServiceException("address is invalid: "+addr, INVALID_ADDRESS, SENDERS_FAULT, new Argument(ADDR, addr, Argument.Type.STR));
    }
    
    public static IMServiceException NOT_ALLOWED(String addr) {
        return new IMServiceException("Permission denied trying to access: "+addr, NOT_ALLOWED, SENDERS_FAULT, new Argument(ADDR, addr, Argument.Type.STR));
    }
    
    public static IMServiceException NOT_A_CONFERENCE_SERVICE(String addr) {
        return new IMServiceException(addr+" is not a conference service", NOT_A_CONFERENCE_SERVICE, SENDERS_FAULT, new Argument(ADDR, addr, Argument.Type.STR));
    }
    
    public static IMServiceException NOT_A_CONFERENCE_ROOM(String addr) {
        return new IMServiceException(addr+" is not a conference room", NOT_A_CONFERENCE_ROOM, SENDERS_FAULT, new Argument(ADDR, addr, Argument.Type.STR));
    }
    
    public static IMServiceException NO_RESPONSE_FROM_REMOTE(String info, String addr) {
        return new IMServiceException("No response from addr: "+addr+" while "+info, NO_RESPONSE_FROM_REMOTE, SENDERS_FAULT, new Argument(ADDR, addr, Argument.Type.STR));
    }
    
    public static IMServiceException XMPP_ERROR(String info, Packet packet) {
        return new IMServiceException("XMPP error: "+info+" - "+packet.toXML(), XMPP_ERROR, SENDERS_FAULT, new Argument(PACKET, packet.toXML(), Argument.Type.STR));  
    }
    
    public static IMServiceException NOT_A_MUC_CHAT(String thread) {
        return new IMServiceException(thread+" is not a MUC chat and cannot be configured", NOT_A_MUC_CHAT, SENDERS_FAULT, new Argument(THREAD, thread, Argument.Type.STR));
    }
    
    
}
