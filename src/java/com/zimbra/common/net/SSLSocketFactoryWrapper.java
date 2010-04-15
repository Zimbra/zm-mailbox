package com.zimbra.common.net;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class SSLSocketFactoryWrapper extends SSLSocketFactory  {
    private final SSLSocketFactory delegate;

    public SSLSocketFactoryWrapper(SSLSocketFactory delegate) {
        this.delegate = delegate;
    }

    @Override
    public Socket createSocket() throws IOException {
        return delegate.createSocket();
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return delegate.createSocket(host, port);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localAddress, int localPort) throws IOException, UnknownHostException {
        return delegate.createSocket(host, port, localAddress, localPort);
    }

    @Override
    public Socket createSocket(InetAddress address, int port) throws IOException {
        return delegate.createSocket(address, port);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return delegate.createSocket(address, port, localAddress, localPort);
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean flag) throws IOException {
        return delegate.createSocket(socket, host, port, flag);
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }
}
