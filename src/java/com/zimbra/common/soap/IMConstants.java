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
package com.zimbra.common.soap;

import org.dom4j.Namespace;
import org.dom4j.QName;

public class IMConstants {

    public static final String NAMESPACE_STR = "urn:zimbraIM";
    public static final Namespace NAMESPACE = Namespace.get(NAMESPACE_STR);

    public static final QName IM_GET_ROSTER_REQUEST    = QName.get("IMGetRosterRequest", NAMESPACE);
    public static final QName IM_GET_ROSTER_RESPONSE   = QName.get("IMGetRosterResponse", NAMESPACE);
    public static final QName IM_SUBSCRIBE_REQUEST     = QName.get("IMSubscribeRequest", NAMESPACE);
    public static final QName IM_SUBSCRIBE_RESPONSE    = QName.get("IMSubscribeResponse", NAMESPACE);
    public static final QName IM_AUTHORIZE_SUBSCRIBE_REQUEST     = QName.get("IMAuthorizeSubscribeRequest", NAMESPACE);
    public static final QName IM_AUTHORIZE_SUBSCRIBE_RESPONSE    = QName.get("IMAuthorizeSubscribeResponse", NAMESPACE);
    public static final QName IM_SET_PRESENCE_REQUEST  = QName.get("IMSetPresenceRequest", NAMESPACE);
    public static final QName IM_SET_PRESENCE_RESPONSE = QName.get("IMSetPresenceResponse", NAMESPACE);
    public static final QName IM_GET_CHAT_REQUEST      = QName.get("IMGetChatRequest", NAMESPACE);
    public static final QName IM_GET_CHAT_RESPONSE     = QName.get("IMGetChatResponse", NAMESPACE);
    public static final QName IM_MODIFY_CHAT_REQUEST   = QName.get("IMModifyChatRequest", NAMESPACE);
    public static final QName IM_MODIFY_CHAT_RESPONSE  = QName.get("IMModifyChatResponse", NAMESPACE);
    public static final QName IM_SEND_MESSAGE_REQUEST  = QName.get("IMSendMessageRequest", NAMESPACE);
    public static final QName IM_SEND_MESSAGE_RESPONSE = QName.get("IMSendMessageResponse", NAMESPACE);
    public static final QName IM_JOIN_CHAT_REQUEST = QName.get("IMJoinChatRequest", NAMESPACE);
    public static final QName IM_JOIN_CHAT_RESPONSE = QName.get("IMJoinChatResponse", NAMESPACE);
    
    public static final QName IM_GET_PRIVACY_LIST_REQUEST = QName.get("IMGetPrivacyListRequest", NAMESPACE);
    public static final QName IM_GET_PRIVACY_LIST_RESPONSE = QName.get("IMGetPrivacyListResponse", NAMESPACE);
    
    public static final QName IM_SET_PRIVACY_LIST_REQUEST = QName.get("IMSetPrivacyListRequest", NAMESPACE);
    public static final QName IM_SET_PRIVACY_LIST_RESPONSE = QName.get("IMSetPrivacyListResponse", NAMESPACE);

    public static final QName IM_GATEWAY_LIST_REQUEST = QName.get("IMGatewayListRequest", NAMESPACE);
    public static final QName IM_GATEWAY_LIST_RESPONSE = QName.get("IMGatewayListResponse", NAMESPACE);
    public static final QName IM_GATEWAY_REGISTER_REQUEST = QName.get("IMGatewayRegisterRequest", NAMESPACE);
    public static final QName IM_GATEWAY_REGISTER_RESPONSE = QName.get("IMGatewayRegisterResponse", NAMESPACE);
    
    public static final QName IM_SET_IDLE_REQUEST = QName.get("IMSetIdleRequest", NAMESPACE);
    public static final QName IM_SET_IDLE_RESPONSE = QName.get("IMSetIdleResponse", NAMESPACE);

    public static final String A_ACTION         = "action";
    public static final String A_AUTHORIZED     = "authorized";
    public static final String A_THREAD_ID      = "thread";
    public static final String A_ADDRESS        = "addr";
    public static final String A_ADD            = "add";
    public static final String A_ASK            = "ask";
    public static final String A_SEQ            = "seq";
    public static final String A_NAME           = "name";
    public static final String A_SUBSCRIPTION   = "subscription";
    public static final String A_GROUPS         = "groups";
    public static final String A_OPERATION      = "op";
    public static final String A_SHOW           = "show";
    public static final String A_FROM           = "from";
    public static final String A_TO             = "to";
    public static final String A_TIMESTAMP      = "ts";
    public static final String A_ORDER          = "order";
    public static final String A_SERVICE        = "service";
    public static final String A_STATUS         = "status";
    public static final String A_STATE          = "state";
    public static final String A_TIME_UNTIL_NEXT_CONNECT = "timeUntilNextConnect";
    public static final String A_USERNAME       = "username";
    public static final String A_ERROR          = "error";
    public static final String A_IS_IDLE        = "isIdle";
    public static final String A_IDLE_TIME      = "idleTime";

    public static final String E_LIST           = "list";
    public static final String E_MESSAGES       = "messages";
    public static final String E_MESSAGE        = "message";
    public static final String E_SUBJECT        = "subject";
    public static final String E_BODY           = "body";
    public static final String E_CHATS          = "chats";
    public static final String E_CHAT           = "chat";
    public static final String E_PARTICIPANT    = "p";
    public static final String E_PARTICIPANTS   = "pcps";
    public static final String E_ITEMS          = "items";
    public static final String E_ITEM           = "item";
    public static final String E_PRESENCE       = "presence";
    public static final String E_LEFTCHAT       = "leftchat";
    public static final String E_ENTEREDCHAT    = "enteredchat";
    public static final String E_INVITED        = "invited";
    public static final String E_SUBSCRIBE      = "subscribe";
    public static final String E_SUBSCRIBED     = "subscribed";
    public static final String E_UNSUBSCRIBED   = "unsubscribed";
    public static final String E_GATEWAY_STATUS = "gwStatus";
    public static final String E_OTHER_LOCATION = "otherLocation";
    public static final String E_TYPING         = "typing";
}
