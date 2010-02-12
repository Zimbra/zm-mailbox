package com.zimbra.common.net;

import javax.net.SocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.Socket;
import java.net.SocketAddress;

public class SocksSocketFactory extends SocketFactory {
    private final ProxySelector proxySelector;

    public SocksSocketFactory(ProxySelector ps) {
        proxySelector = ps;
    }

    public SocksSocketFactory() {
        this(null);
    }

    @Override
    public Socket createSocket() throws IOException {
        return new SocksSocket(proxySelector);
    }
    
    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return createSocket(new InetSocketAddress(host, port), null);
    }

    @Override
    public Socket createSocket(String host, int port,
                               InetAddress localAddress, int localPort)
        throws IOException {
        return createSocket(new InetSocketAddress(host, port),
                            new InetSocketAddress(localAddress, localPort));
    }

    @Override
    public Socket createSocket(InetAddress address, int port) throws IOException {
        return createSocket(new InetSocketAddress(address, port), null);
    }

    @Override
    public Socket createSocket(InetAddress address, int port,
                               InetAddress localAddress, int localPort)
        throws IOException {
        return createSocket(new InetSocketAddress(address, port),
                            new InetSocketAddress(localAddress, localPort));
    }

    private Socket createSocket(SocketAddress endpoint,
                                SocketAddress bindpoint) throws IOException {
        Socket sock = createSocket();
        if (bindpoint != null) {
            sock.bind(bindpoint);
        }
        if (endpoint != null) {
            sock.connect(endpoint);
        }
        return sock;
    }
}
