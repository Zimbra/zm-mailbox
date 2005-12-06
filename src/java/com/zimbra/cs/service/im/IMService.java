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
    
    public void registerHandlers(DocumentDispatcher dispatcher) {
        dispatcher.registerHandler(IM_GET_ROSTER_REQUEST, new IMGetRoster());
        dispatcher.registerHandler(IM_SET_PRESENCE_REQUEST, new IMSetPresence());
        dispatcher.registerHandler(IM_SUBSCRIBE_REQUEST, new IMSubscribe());
        dispatcher.registerHandler(IM_SEND_MESSAGE_REQUEST, new IMSendMessage());
        dispatcher.registerHandler(IM_GET_CHAT_REQUEST, new IMGetChat());
        dispatcher.registerHandler(IM_MODIFY_CHAT_REQUEST, new IMModifyChat());
        
    }

}
