package com.zimbra.common.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.security.cert.X509Certificate;

import com.zimbra.common.localconfig.LC;

public class CustomSSLSocketUtil {
    /**
     * Describe <code>verifyHostname</code> method here.
     *
     * @param socket a <code>SSLSocket</code> value
     * @exception SSLPeerUnverifiedException  If there are problems obtaining
     * the server certificates from the SSL session, or the server host name 
     * does not match with the "Common Name" in the server certificates 
     * SubjectDN.
     * @exception UnknownHostException  If we are not able to resolve
     * the SSL sessions returned server host name. 
     */
    public static void verifyHostname(SSLSocket socket) throws SSLPeerUnverifiedException, UnknownHostException, IOException {
        if (LC.ssl_allow_mismatched_certs.booleanValue()) 
            return;

        try {
        	socket.startHandshake(); //so that we can trigger certificate check early
        } catch (IOException x) {
        	try {
        		socket.close();
        	} catch (Exception e) {}
        	throw x;
        }
        
        SSLSession session = socket.getSession();
        String hostname = session.getPeerHost();
        try {
            InetAddress addr = InetAddress.getByName(hostname);
        } catch (UnknownHostException uhe) {
            throw new UnknownHostException("Could not resolve SSL sessions "
                                           + "server hostname: " + hostname);
        }
        
        X509Certificate[] certs = session.getPeerCertificateChain();
        if (certs == null || certs.length == 0) 
            throw new SSLPeerUnverifiedException("No server certificates found!");
        
        //get the servers DN in its string representation
        String dn = certs[0].getSubjectDN().getName();

        //might be useful to print out all certificates we receive from the
        //server, in case one has to debug a problem with the installed certs.
        if (ZimbraLog.security.isDebugEnabled()) {
        	ZimbraLog.security.debug("Server certificate chain:");
            for (int i = 0; i < certs.length; i++) {
            	ZimbraLog.security.debug("X509Certificate[" + i + "]=" + certs[i]);
            }
        }
        //get the common name from the first cert
        String cn = getCN(dn);

        
        if (isDomainMatched(hostname, cn)) {
            if (ZimbraLog.security.isDebugEnabled()) {
            	ZimbraLog.security.debug("Target hostname valid: " + cn);
            }
        } else {
            throw new SSLPeerUnverifiedException(
                "HTTPS hostname invalid: expected '" + hostname + "', received '" + cn + "'");
        }
    }

    /**
     * Parses a X.500 distinguished name for the value of the 
     * "Common Name" field.
     * This is done a bit sloppy right now and should probably be done a bit
     * more according to <code>RFC 2253</code>.
     *
     * @param dn  a X.500 distinguished name.
     * @return the value of the "Common Name" field.
     */
    private static String getCN(String dn) {
        int i = 0;
        i = dn.indexOf("CN=");
        if (i == -1) {
            return null;
        }
        //get the remaining DN without CN=
        dn = dn.substring(i + 3);  
        // System.out.println("dn=" + dn);
        char[] dncs = dn.toCharArray();
        for (i = 0; i < dncs.length; i++) {
            if (dncs[i] == ','  && i > 0 && dncs[i - 1] != '\\') {
                break;
            }
        }
        return dn.substring(0, i);
    }
    
    private static boolean isDomainMatched(String hostname, String cn) {
    	hostname = hostname.toLowerCase();
    	cn = cn.toLowerCase();
    	if (cn.startsWith("*."))
    		cn = cn.substring(2);
    	if (hostname.endsWith(cn))
    		return true;
    	
        String cnDomain = cn.substring(cn.indexOf('.') + 1);
        if (cnDomain.split("\\.").length > 2 && hostname.endsWith(cnDomain))
        	return true;
    	
        return false;
    }
    
    public static void main(String[] args) {
    	System.out.println(isDomainMatched("imap-ssl.mail.yahoo.com", "imap.mail.yahoo.com"));
    	System.out.println(!isDomainMatched("mail-ssl.yahoo.com", "mail.yahoo.com"));
    	System.out.println(isDomainMatched("mail-ssl.yahoo.com", "yahoo.com"));
    	System.out.println(isDomainMatched("login.yahoo.com", "login.yahoo.com"));
    	System.out.println(isDomainMatched("localhost", "localhost"));
    }
}
