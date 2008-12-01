package com.zimbra.cs.datasource;

import com.zimbra.common.util.CustomSSLSocketUtil;
import com.zimbra.common.util.SSLSocketFactoryManager;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.SocketFactory;
import java.net.Socket;
import java.net.InetAddress;
import java.io.IOException;

/*
 * Special SSLSocketFactory implementation to support JavaMail TLS. Since
 * JavaMail only allows the configuration of one socket factory we need this
 * in order to implement basic socket factory operations using plain socket
 * (before TLS negotiation) but then delegate to an SSLSocketFactory instance
 * when wrapping an existing socket (after TLS negotiation).
 */
public class TlsSocketFactory extends SSLSocketFactory {
    private SSLSocketFactory factory;

    private static final TlsSocketFactory THE_ONE = new TlsSocketFactory();

    protected TlsSocketFactory() {
    	factory = SSLSocketFactoryManager.getDefaultSSLSocketFactory();
    }
    
    public static SocketFactory getDefault() {
        return THE_ONE;
    }

    public Socket createSocket() throws IOException {
        return new Socket();
    }

    public Socket createSocket(InetAddress address, int port) throws IOException {
        return new Socket(address, port);
    }
    
    public Socket createSocket(InetAddress address, int port,
                               InetAddress localAddress, int localPort) throws IOException {
        return new Socket(address, port, localAddress, localPort);
    }

    public Socket createSocket(String host, int port) throws IOException {
        return new Socket(host, port);
    }

    public Socket createSocket(String host, int port,
                               InetAddress localAddress, int localPort) throws IOException {
        return new Socket(host, port, localAddress, localPort);
    }

    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
    	SSLSocket sslSocket = (SSLSocket)factory.createSocket(s, host, port, autoClose);
    	CustomSSLSocketUtil.checkCertificate(host, sslSocket);
    	return sslSocket;
    }

    public String[] getDefaultCipherSuites() {
        return factory.getDefaultCipherSuites();
    }

    public String[] getSupportedCipherSuites() {
        return factory.getSupportedCipherSuites();
    }
}
