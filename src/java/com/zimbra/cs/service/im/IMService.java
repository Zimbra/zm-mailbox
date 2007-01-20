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

import com.zimbra.soap.DocumentDispatcher;
import com.zimbra.soap.DocumentService;
import com.zimbra.common.soap.IMConstants;

public class IMService implements DocumentService {

    public void registerHandlers(DocumentDispatcher dispatcher) {
        dispatcher.registerHandler(IMConstants.IM_GET_ROSTER_REQUEST, new IMGetRoster());
        dispatcher.registerHandler(IMConstants.IM_SET_PRESENCE_REQUEST, new IMSetPresence());
        dispatcher.registerHandler(IMConstants.IM_SUBSCRIBE_REQUEST, new IMSubscribe());
        dispatcher.registerHandler(IMConstants.IM_AUTHORIZE_SUBSCRIBE_REQUEST, new IMAuthorizeSubscribe());
        dispatcher.registerHandler(IMConstants.IM_SEND_MESSAGE_REQUEST, new IMSendMessage());
        dispatcher.registerHandler(IMConstants.IM_GET_CHAT_REQUEST, new IMGetChat());
        dispatcher.registerHandler(IMConstants.IM_MODIFY_CHAT_REQUEST, new IMModifyChat());
        dispatcher.registerHandler(IMConstants.IM_GATEWAY_REGISTER_REQUEST, new IMGatewayRegister());
        dispatcher.registerHandler(IMConstants.IM_GATEWAY_LIST_REQUEST, new IMGatewayList());
    }
}
