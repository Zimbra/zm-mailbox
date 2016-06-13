/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
