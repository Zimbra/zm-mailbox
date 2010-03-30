/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.nio;

import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.server.ServerConfig;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class SSLInfo {
    private final SSLContext context;
    private final String[] enabledCipherSuites;

    private static SSLContext defaultContext;

    public SSLInfo(ServerConfig config) {
        try {
            context = getDefaultSSLContext();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to initialize SSLContext", e);
        }
        enabledCipherSuites = getEnabledCipherSuites(context, config);
    }

    public SSLContext getSSLContext() {
        return context;
    }

    /**
     * Our cipher config attribute zimbraSSLExcludeCipherSuites specifies a list of ciphers that should be
     * disabled instead of enabled.  This is because we want the same attribute to control all SSL protocols
     * running on mailbox servers.  For https, Jetty configuration only supports an excluded list.
     * Therefore we adapted the same scheme for zimbraSSLExcludeCipherSuites, which is written to jetty.xml
     * by config rewrite, and will be used for protocols (imaps/pop3s) handled by Zimbra code.
     *
     * For nio based servers/handlers, MinaServer uses SSLFilter for SSL communication.  SSLFilter wraps
     * an SSLEngine that actually does all the work.  SSLFilter.setEnabledCipherSuites() sets the list of
     * cipher suites to be enabled when the underlying SSLEngine is initialized.  Since we only have an
     * excluded list, we need to exclude those from the ciphers suites which are currently enabled for use
     * on a engine.
     *
     * Since we do not directly interact with a SSLEngine while sessions are handled,  and there is
     * no SSLFilter API to alter the SSLEngine it wraps, we workaround this by doing the following:
     *   - create a dummy SSLEngine from the same SSLContext instance that will be used for all SSL communication.
     *   - get the enabled ciphers from SSLEngine.getEnabledCipherSuites()
     *   - exclude the ciphers we need to exclude from the enabled ciphers, so we now have a net enabled ciphers
     *     list.
     * The above is only done once and we keep a singleton of this cipher list.  We then can pass it to
     * SSLFilter.setEnabledCipherSuites() for SSL and StartTLS session.
     *
     * @return the enabled ciphers, or null if defaults should be used
     */
    public String[] getEnabledCipherSuites() {
        return enabledCipherSuites;
    }

    private static synchronized SSLContext getDefaultSSLContext()
        throws IOException, GeneralSecurityException {
        if (defaultContext == null) {
            KeyStore ks = KeyStore.getInstance("JKS");
            char[] pass = LC.mailboxd_keystore_password.value().toCharArray();
            ks.load(new FileInputStream(LC.mailboxd_keystore.value()), pass);
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, pass);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);
            defaultContext = SSLContext.getInstance("TLS");
            defaultContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        }
        return defaultContext;
    }

    private static String[] getEnabledCipherSuites(SSLContext context, ServerConfig config) {
        String[] excluded = config.getSslExcludedCiphers();
        if (excluded != null && excluded.length > 0) {
            // create default SSLEngine to get the ciphers enabled for the engine
            String[] enabled = context.createSSLEngine().getEnabledCipherSuites();
            if (enabled != null) {
                List<String> res = new ArrayList<String>(Arrays.asList(enabled));
                res.removeAll(Arrays.asList(excluded));
                return res.toArray(new String[res.size()]);
            }
        }
        return null;
    }
}
