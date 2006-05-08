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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.im.xmpp.srv.net;

import com.sun.net.ssl.X509KeyManager;
import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * A skeleton placeholder for developers wishing to implement their own custom
 * key manager. In future revisions we may expand the skeleton code if customers
 * request assistance in creating custom key managers.
 * <p/>
 * The key manager is an essential part of server SSL support. Typically you
 * will implement a custom key manager to retrieve certificates from repositories
 * that are not of standard Java types (e.g. obtaining them from LDAP or a JDBC database).
 *
 * @author Iain Shigeoka
 */
public class SSLJiveKeyManager implements X509KeyManager {
    public String[] getClientAliases(String s, Principal[] principals) {
        return new String[0];
    }

    public String chooseClientAlias(String s, Principal[] principals) {
        return null;
    }

    public String chooseClientAlias(String[] strings, Principal[] principals, Socket socket) {
        return null;
    }

    public String[] getServerAliases(String s, Principal[] principals) {
        return new String[0];
    }

    public String chooseServerAlias(String s, Principal[] principals) {
        return null;
    }

    public String chooseServerAlias(String s, Principal[] principals, Socket socket) {
        return null;
    }

    public X509Certificate[] getCertificateChain(String s) {
        return new X509Certificate[0];
    }

    public PrivateKey getPrivateKey(String s) {
        return null;
    }
}
