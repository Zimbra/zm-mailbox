/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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
package com.zimbra.cs.service.mail;

import java.io.IOException;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.util.yauth.MetadataTokenStore;
import com.zimbra.cs.util.yauth.TokenAuthenticateV1;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * 
 */
public class GetYahooCookie extends MailDocumentHandler {
    
    private static final String APPID = "ZYMSGRINTEGRATION";

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        
        MetadataTokenStore mts = new MetadataTokenStore(mbox);
        
        String userId = request.getAttribute("user");
        
        Element response = zsc.createElement(MailConstants.GET_YAHOO_COOKIE_RESPONSE); 
        
        String token = mts.getToken(APPID, userId);
        if (token == null) {
            response.addAttribute("error", "NoToken");
        }
        
        try {
            TokenAuthenticateV1 auth = TokenAuthenticateV1.doAuth(userId, token);
            response.addAttribute("crumb", auth.getCrumb());
            response.addAttribute("Y", auth.getY());
            response.addAttribute("T", auth.getT());
        } catch (IllegalArgumentException ex) {
            response.addAttribute("error", "InvalidToken");
        } catch (IOException e) {
            throw ServiceException.FAILURE("IOException attempting to auth with token", e);
        } 
        return response;
    }
}
