/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.im;

import com.zimbra.soap.DocumentDispatcher;
import com.zimbra.soap.DocumentService;
import com.zimbra.common.soap.IMConstants;

/**
 * 
 * @zm-service-description		The IM Service includes commands for managing Instant Messaging
 * items such as a user's buddy list and chats.
 *
 */
public class IMService implements DocumentService {

    public void registerHandlers(DocumentDispatcher dispatcher) {
        dispatcher.registerHandler(IMConstants.IM_GET_ROSTER_REQUEST, new IMGetRoster());
        dispatcher.registerHandler(IMConstants.IM_SET_PRESENCE_REQUEST, new IMSetPresence());
        dispatcher.registerHandler(IMConstants.IM_SUBSCRIBE_REQUEST, new IMSubscribe());
        dispatcher.registerHandler(IMConstants.IM_AUTHORIZE_SUBSCRIBE_REQUEST, new IMAuthorizeSubscribe());
        dispatcher.registerHandler(IMConstants.IM_SEND_MESSAGE_REQUEST, new IMSendMessage());
        dispatcher.registerHandler(IMConstants.IM_GET_CHAT_REQUEST, new IMGetChat());
        dispatcher.registerHandler(IMConstants.IM_GET_CHAT_CONFIGURATION_REQUEST, new IMGetChatConfiguration());
        dispatcher.registerHandler(IMConstants.IM_JOIN_CONFERENCE_ROOM_REQUEST, new IMJoinConferenceRoom());
        dispatcher.registerHandler(IMConstants.IM_MODIFY_CHAT_REQUEST, new IMModifyChat());
        dispatcher.registerHandler(IMConstants.IM_GATEWAY_REGISTER_REQUEST, new IMGatewayRegister());
        dispatcher.registerHandler(IMConstants.IM_GATEWAY_LIST_REQUEST, new IMGatewayList());
        dispatcher.registerHandler(IMConstants.IM_GET_PRIVACY_LIST_REQUEST, new IMGetPrivacyList());
        dispatcher.registerHandler(IMConstants.IM_SET_PRIVACY_LIST_REQUEST, new IMSetPrivacyList());
        dispatcher.registerHandler(IMConstants.IM_SET_IDLE_REQUEST, new IMSetIdle());
        dispatcher.registerHandler(IMConstants.IM_LIST_CONFERENCE_SERVICES_REQUEST, new IMListConferenceServices());
        dispatcher.registerHandler(IMConstants.IM_LIST_CONFERENCE_ROOMS_REQUEST, new IMListConferenceRooms());
    }
}
