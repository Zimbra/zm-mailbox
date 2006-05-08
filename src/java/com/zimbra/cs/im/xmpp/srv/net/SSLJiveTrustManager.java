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

import com.zimbra.cs.im.xmpp.util.LocaleUtils;
import com.zimbra.cs.im.xmpp.util.Log;
import com.sun.net.ssl.X509TrustManager;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;

/**
 * Trust manager which accepts certificates without any validation
 * except date validation.
 * <p/>
 * A skeleton placeholder for developers wishing to implement their own custom
 * trust manager. In future revisions we may expand the skeleton code if customers
 * request assistance in creating custom trust managers.
 * <p/>
 * You only need a trust manager if your server will require clients
 * to authenticated with the server (typically only the server authenticates
 * with the client).
 *
 * @author Iain Shigeoka
 */
public class SSLJiveTrustManager implements X509TrustManager {

    public void checkClientTrusted(X509Certificate[] chain, String authType) {

    }

    public void checkServerTrusted(X509Certificate[] chain, String authType) {
    }

    public boolean isClientTrusted(X509Certificate[] x509Certificates) {
        return true;
    }

    public boolean isServerTrusted(X509Certificate[] x509Certificates) {
        boolean trusted = true;
        try {
            x509Certificates[0].checkValidity();
        }
        catch (CertificateExpiredException e) {
            Log.error(LocaleUtils.getLocalizedString("admin.error"), e);
            trusted = false;
        }
        catch (CertificateNotYetValidException e) {
            Log.error(LocaleUtils.getLocalizedString("admin.error"), e);
            trusted = false;
        }
        return trusted;
    }

    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}
