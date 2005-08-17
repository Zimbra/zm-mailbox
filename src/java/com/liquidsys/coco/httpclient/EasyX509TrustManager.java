package com.liquidsys.coco.httpclient;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.commons.logging.Log; 
import org.apache.commons.logging.LogFactory;

/**
 * <p>
 * EasyX509TrustManager unlike default {@link X509TrustManager} accepts 
 * self-signed certificates. 
 * </p>
 * <p>
 * This trust manager SHOULD NOT be used for productive systems 
 * due to security reasons, unless it is a concious decision and 
 * you are perfectly aware of security implications of accepting 
 * self-signed certificates
 * </p>
 * 
 * @author <a href="mailto:adrian.sutton@ephox.com">Adrian Sutton</a>
 * @author <a href="mailto:oleg@ural.ru">Oleg Kalnichevski</a>
 * 
 * <p>
 * DISCLAIMER: HttpClient developers DO NOT actively support this component.
 * The component is provided as a reference material, which may be inappropriate
 * for use without additional customization.
 * </p>
 */

public class EasyX509TrustManager implements X509TrustManager
{
    private X509TrustManager standardTrustManager = null;

    /** Log object for this class. */
    private static final Log LOG = LogFactory.getLog(EasyX509TrustManager.class);

    /**
     * Constructor for EasyX509TrustManager.
     */
    public EasyX509TrustManager(KeyStore keystore) throws NoSuchAlgorithmException, KeyStoreException {
        super();
        TrustManagerFactory factory = TrustManagerFactory.getInstance("SunX509");
        factory.init(keystore);
        TrustManager[] trustmanagers = factory.getTrustManagers();
        if (trustmanagers.length == 0) {
            throw new NoSuchAlgorithmException("SunX509 trust manager not supported");
        }
        this.standardTrustManager = (X509TrustManager)trustmanagers[0];
    }

    public X509Certificate[] getAcceptedIssuers() {
        return this.standardTrustManager.getAcceptedIssuers();
    }

    /* (non-Javadoc)
     * @see javax.net.ssl.X509TrustManager#checkClientTrusted(java.security.cert.X509Certificate[], java.lang.String)
     */
    public void checkClientTrusted(X509Certificate[] certificates, String str) throws CertificateException {
        this.standardTrustManager.checkClientTrusted(certificates, str);
    }

    /* (non-Javadoc)
     * @see javax.net.ssl.X509TrustManager#checkServerTrusted(java.security.cert.X509Certificate[], java.lang.String)
     */
    public void checkServerTrusted(X509Certificate[] certificates, String str) throws CertificateException {
        if ((certificates != null) && LOG.isDebugEnabled()) {
            LOG.debug("Server certificate chain:");
            for (int i = 0; i < certificates.length; i++) {
                LOG.debug("X509Certificate[" + i + "]=" + certificates[i]);
            }
        }
        // by using this trust manager, it is configured not to verify certificates, 
        // so return silently
    }
}
