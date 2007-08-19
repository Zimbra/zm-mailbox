package com.zimbra.cs.mina;

import com.zimbra.cs.util.Zimbra;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.Log;

import javax.net.ssl.SSLContext;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.filter.SSLFilter;
import org.apache.mina.filter.LoggingFilter;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoSession;

/**
 * Base class for MINA-baqsed server implementations.
 */
public abstract class MinaServer {
    protected final ServerSocket serverSocket;
    protected final SocketAcceptorConfig acceptorConfig;
    protected final ExecutorService executorService;
    protected final SocketAcceptor socketAcceptor;
    protected final boolean sslEnabled;

    private static SSLContext sslContext;
    
    public static synchronized SSLContext getSSLContext() {
        if (sslContext == null) {
            try {
                sslContext = initSSLContext();
            } catch (Exception e) {
                Zimbra.halt("exception initializing SSL context", e);
            }
        }
        return sslContext;
    }

    private static SSLContext initSSLContext() throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        char[] pass = LC.mailboxd_keystore_password.value().toCharArray();
        ks.load(new FileInputStream(LC.mailboxd_keystore.value()), pass);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, pass);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return context;
    }
    
    protected MinaServer(ServerSocket serverSocket, int numThreads, boolean sslEnabled)
            throws IOException {
    	this.serverSocket = serverSocket;
        acceptorConfig = new SocketAcceptorConfig();
        acceptorConfig.setReuseAddress(true);
        executorService = Executors.newFixedThreadPool(numThreads);
        socketAcceptor = new SocketAcceptor();
        this.sslEnabled = sslEnabled;
    }

    public void start() throws IOException {
        DefaultIoFilterChainBuilder fc = acceptorConfig.getFilterChain();
        if (sslEnabled) {
            fc.addFirst("ssl", new SSLFilter(getSSLContext()));
        }
        fc.addLast("codec", new ProtocolCodecFilter(new MinaCodecFactory(this)));
        fc.addLast("executer", new ExecutorFilter(executorService));
        fc.addLast("logger", new LoggingFilter());
        IoHandler handler = new MinaIoHandler(this);
        
        ServerSocketChannel ssc = serverSocket.getChannel();
        assert(ssc != null);  //the socket should have been created using a channel
        assert(!ssc.isBlocking()); //and the channel should be NIO
        socketAcceptor.register(ssc, handler, acceptorConfig);
    }

    protected abstract MinaHandler createHandler(IoSession session);

    protected abstract MinaRequest createRequest(MinaHandler handler);

    public abstract Log getLog();
    
    public void shutdown(int graceSecs) {
        socketAcceptor.unbindAll();
        executorService.shutdown();
        try {
            executorService.awaitTermination(graceSecs, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // Fall through
        }
        executorService.shutdownNow();
    }

    public boolean isSSLEnabled() {
        return sslEnabled;
    }
}
