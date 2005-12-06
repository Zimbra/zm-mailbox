package com.zimbra.cs.service.im;

import org.dom4j.Namespace;
import org.dom4j.QName;

import com.zimbra.soap.DocumentDispatcher;
import com.zimbra.soap.DocumentService;

public class IMService implements DocumentService {
    
    public static final String NAMESPACE_STR = "urn:zimbraIM";
    public static final Namespace NAMESPACE = Namespace.get(NAMESPACE_STR);
    
    public static final QName IM_GET_ROSTER_REQUEST    = QName.get("IMGetRosterRequest", NAMESPACE);    
    public static final QName IM_GET_ROSTER_RESPONSE   = QName.get("IMGetRosterResponse", NAMESPACE);    
    public static final QName IM_SUBSCRIBE_REQUEST     = QName.get("IMSubscribeRequest", NAMESPACE);    
    public static final QName IM_SUBSCRIBE_RESPONSE    = QName.get("IMSubscribeResponse", NAMESPACE);    
    public static final QName IM_SET_PRESENCE_REQUEST  = QName.get("IMSetPresenceRequest", NAMESPACE);    
    public static final QName IM_SET_PRESENCE_RESPONSE = QName.get("IMSetPresenceResponse", NAMESPACE);    
    public static final QName IM_GET_CHAT_REQUEST      = QName.get("IMGetChatRequest", NAMESPACE);    
    public static final QName IM_GET_CHAT_RESPONSE     = QName.get("IMGetChatResponse", NAMESPACE);
    public static final QName IM_MODIFY_CHAT_REQUEST   = QName.get("IMModifyChatRequest", NAMESPACE);    
    public static final QName IM_MODIFY_CHAT_RESPONSE  = QName.get("IMModifyChatResponse", NAMESPACE);    
    public static final QName IM_SEND_MESSAGE_REQUEST  = QName.get("IMSendMessageRequest", NAMESPACE);    
    public static final QName IM_SEND_MESSAGE_RESPONSE = QName.get("IMSendMessageResponse", NAMESPACE);    
    
    public static final String A_THREAD_ID      = "thread";
    public static final String A_ADDRESS        = "addr";
    public static final String A_SEQ            = "seq";
    public static final String A_NAME           = "name";
    public static final String A_SUBSCRIPTION   = "subscription";
    public static final String A_GROUPS         = "groups";
    public static final String A_OPERATION      = "op";
    public static final String A_SHOW           = "show";
    public static final String A_FROM           = "from";
    public static final String A_TO             = "to";
    public static final String A_TIMESTAMP      = "ts";

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
    public static final String E_STATUS         = "status";
    public static final String E_LEFTCHAT       = "leftchat";
    public static final String E_SUBSCRIBE      = "subscribe";
    public static final String E_SUBSCRIBED     = "subscribed";
    public static final String E_UNSUBSCRIBED   = "unsubscribed";
    
    


    public void registerHandlers(DocumentDispatcher dispatcher) {
        dispatcher.registerHandler(IM_GET_ROSTER_REQUEST, new IMGetRoster());
        dispatcher.registerHandler(IM_SET_PRESENCE_REQUEST, new IMSetPresence());
        dispatcher.registerHandler(IM_SUBSCRIBE_REQUEST, new IMSubscribe());
        dispatcher.registerHandler(IM_SEND_MESSAGE_REQUEST, new IMSendMessage());
        dispatcher.registerHandler(IM_GET_CHAT_REQUEST, new IMGetChat());
        dispatcher.registerHandler(IM_MODIFY_CHAT_REQUEST, new IMModifyChat());
        
    }

}
