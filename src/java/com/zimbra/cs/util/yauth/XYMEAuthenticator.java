/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.util.yauth;

import com.zimbra.cs.mailclient.auth.Authenticator;
import com.zimbra.cs.mailclient.MailConfig;

import java.io.UnsupportedEncodingException;

/**
 * Support for IMAP XYMECookie authentication.
 *
 * @see <a href="http://twiki.corp.yahoo.com/view/Mail/IMAPGATEExtendedCommands#AUTHENTICATE_XYMECOOKIE">XYMECOOKIE Method</a>
 */
public class XYMEAuthenticator extends Authenticator {
    private final Auth auth;
    private final String partner;

    public static final String MECHANISM = "XYMECOOKIE";
    
    public XYMEAuthenticator(Auth auth, String partner) {
        this.auth = auth;
        this.partner = partner;
    }

    @Override
    public void init(MailConfig config, String password) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getMechanism() {
        return MECHANISM;
    }

    @Override
    public byte[] evaluateChallenge(byte[] challenge) {
        try {
            String response = String.format(
                "cookies=%s appid=%s wssid=%s src=%s",
                auth.getCookie(), auth.getAppId(), auth.getWSSID(), partner);
            return response.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new InternalError();
        }
    }

    @Override
    public boolean isComplete() {
        return true;
    }
}
