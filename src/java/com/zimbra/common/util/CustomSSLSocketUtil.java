package com.zimbra.common.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import sun.security.util.HostnameChecker;

import com.zimbra.common.localconfig.LC;

public class CustomSSLSocketUtil {
	
	private static ThreadLocal<String> threadLocal = new ThreadLocal<String>();
	
	static String getCertificateHostname() {
		return threadLocal.get();
	}
	
	public static void checkCertificate(String hostname, SSLSocket socket)  throws SSLPeerUnverifiedException, UnknownHostException, IOException {
		threadLocal.set(hostname);
        try {
        	socket.startHandshake(); //so that we can trigger certificate check early
        } catch (IOException x) {
        	try {
        		socket.close();
        	} catch (Exception e) {}
        	throw x;
        } finally {
        	threadLocal.remove();
        }
       	verifyHostname(hostname, socket.getSession());
	}
	
    private static java.security.cert.X509Certificate certJavax2Java(javax.security.cert.X509Certificate cert) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(cert.getEncoded());
            java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
            return (java.security.cert.X509Certificate)cf.generateCertificate(bis);
        } catch (java.security.cert.CertificateEncodingException e) {
        } catch (javax.security.cert.CertificateEncodingException e) {
        } catch (java.security.cert.CertificateException e) {
        }
        return null;
    }
	
    private static void verifyHostname(String hostname, SSLSession session) throws SSLPeerUnverifiedException, UnknownHostException, IOException {
        if (LC.ssl_allow_mismatched_certs.booleanValue()) 
            return;
        
        try {
            InetAddress.getByName(hostname);
        } catch (UnknownHostException uhe) {
            throw new UnknownHostException("Could not resolve SSL sessions server hostname: " + hostname);
        }
        
        javax.security.cert.X509Certificate[] certs = session.getPeerCertificateChain();
        if (certs == null || certs.length == 0) 
            throw new SSLPeerUnverifiedException("No server certificates found: " + hostname);
        
        X509Certificate cert = certJavax2Java(certs[0]);
        
        if (CustomTrustManager.getInstance().isCertificateAcceptedForHostname(hostname, cert))
        	return;
        
        HostnameChecker hc = HostnameChecker.getInstance(HostnameChecker.TYPE_TLS);
        try {
        	hc.match(hostname, cert);
        } catch (CertificateException x) {
        	String certInfo = CustomTrustManager.getInstance().handleCertificateCheckFailure(hostname, cert, true);
        	throw new SSLPeerUnverifiedException(certInfo);
        }
        
//        String dn = certs[0].getSubjectDN().getName();        
//        String cn = getCN(dn);
//        if (cn == null)
//        	throw new SSLPeerUnverifiedException("Invalid certificate DN: " + dn);
//        
//        if (isDomainMatched(hostname, cn)) {
//            ZimbraLog.security.debug("hostname matches certificate: host=" + hostname + ", cert=" + dn);
//        } else {
//            throw new SSLPeerUnverifiedException("Certificate hostname mismatch: host=" + hostname + ", cert=" + dn);
//        }
    }

//	static class HostnameVerifier implements javax.net.ssl.HostnameVerifier {
//		public boolean verify(String hostname, SSLSession session) {
//			try {
//				verifyHostname(hostname, session);
//				return true;
//			} catch (Exception x) {
//				return false;
//			}
//		}
//	}
//    
//    private static String getCN(String dn) {
//        int i = dn.indexOf("CN=");
//        if (i == -1)
//            return null;
//        
//        dn = dn.substring(i + 3);  
//        char[] dncs = dn.toCharArray();
//        for (i = 0; i < dncs.length; i++)
//            if (dncs[i] == ','  && i > 0 && dncs[i - 1] != '\\')
//                break;
//        return dn.substring(0, i);
//    }
//    
//    private static boolean isDomainMatched(String hostname, String cn) {
//    	hostname = hostname.toLowerCase();
//    	cn = cn.toLowerCase();
//    	if (cn.startsWith("*."))
//    		cn = cn.substring(2);
//    	if (hostname.endsWith(cn))
//    		return true;
//    	
//        String cnDomain = cn.substring(cn.indexOf('.') + 1);
//        if (cnDomain.split("\\.").length > 2 && hostname.endsWith(cnDomain))
//        	return true;
//    	
//        return false;
//    }
//    
//    public static void main(String[] args) {
//    	System.out.println(isDomainMatched("imap-ssl.mail.yahoo.com", "*.mail.yahoo.com"));
//    	System.out.println(isDomainMatched("imap-ssl.mail.yahoo.com", "imap.mail.yahoo.com"));
//    	System.out.println(!isDomainMatched("mail-ssl.yahoo.com", "mail.yahoo.com"));
//    	System.out.println(isDomainMatched("mail-ssl.yahoo.com", "yahoo.com"));
//    	System.out.println(isDomainMatched("login.yahoo.com", "login.yahoo.com"));
//    	System.out.println(isDomainMatched("localhost", "localhost"));
//    }
}
