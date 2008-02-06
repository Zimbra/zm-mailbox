package com.zimbra.cs.mailtest;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/**
 * Various utility methods for SSL support.
 */
public final class SSLUtil {
    /**
     * Returns an SSLContext that can be used to create SSL connections without
     * certificates. This is obviously insecure and should only be used for
     * testing.
     *
     * @return an SSLContext that trusts all certificates
     */
    public static SSLContext getDummySSLContext() {
        TrustManager tm = new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] cert, String authType) {
                // trust all certs
            }
            public void checkServerTrusted(X509Certificate[] cert, String authType) {
                // trust all certs
            }
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, new TrustManager[] { tm }, null);
            return sc;
        } catch (Exception e) {
            throw new IllegalStateException("Could not create SSL context", e);
        }
    }
}
