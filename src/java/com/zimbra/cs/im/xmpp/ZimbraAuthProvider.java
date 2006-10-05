/* ***** BEGIN LICENSE BLOCK *****
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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
 
package com.zimbra.cs.im.xmpp;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.im.xmpp.srv.auth.AuthProvider;
import com.zimbra.cs.im.xmpp.srv.auth.UnauthorizedException;
import com.zimbra.cs.service.ServiceException;

public class ZimbraAuthProvider implements AuthProvider {

    public ZimbraAuthProvider() {

    }

    public void authenticate(String username, String password) throws UnauthorizedException {
        try {
            Account acct = ZimbraUserProvider.getInstance().lookupAccount(username);
            Provisioning.getInstance().authAccount(acct, password, "im"); 
        } catch (ServiceException e) {
            throw new UnauthorizedException(e);
        }
    }

    public void authenticate(String username, String token, String digest) throws UnauthorizedException {
        throw new UnsupportedOperationException("Digest authentication not currently supported.");
    }

    public boolean isDigestSupported() {
        return false;
    }

    public boolean isPlainSupported() {
        return true;
    }

}
