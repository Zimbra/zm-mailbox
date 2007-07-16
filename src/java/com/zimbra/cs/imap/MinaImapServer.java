package com.zimbra.cs.imap;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.util.Zimbra;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.filter.LoggingFilter;
import org.apache.mina.filter.SSLFilter;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderAdapter;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Mina-based IMAP server.
 */
public class MinaImapServer {
    private final InetSocketAddress socketAddr;
    private final SocketAcceptorConfig acceptorConfig;
    private final ExecutorService executorService;
    private final SocketAcceptor socketAcceptor;
    private final boolean sslEnabled;

    private static SSLContext sslContext;
    private static final Map<SocketAddress, ServerSocketChannel> channels =
        Collections.synchronizedMap(new HashMap<SocketAddress,
                                                ServerSocketChannel>());
    
    public static final String ENABLED_PROP = "ZimbraNioImapEnabled";
    
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

    public static void bind(String host, int port) throws IOException {
        InetSocketAddress addr = address(host, port);
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        ServerSocket ss = ssc.socket();
        ss.setReuseAddress(true);
        // ss.setReceiveBufferSize(1024);
        ss.bind(addr, 1024);
        channels.put(addr, ssc);
    }

    private static InetSocketAddress address(String host, int port)
            throws IOException {
        return (host != null && host.length() > 0) ?
            new InetSocketAddress(InetAddress.getByName(host), port) :
            new InetSocketAddress(port);
    }
    
    /**
     * Checks if the MINA NIO-based IMAP service is enabled, either through
     * configuration or -DZimbraNioImapEnabled property.
     * @return true if MINA IMAP service is enabled, false otherwise
     */
    public static boolean isEnabled() {
        return LC.nio_imap_enabled.booleanValue() ||
               Boolean.getBoolean(ENABLED_PROP);
    }

    MinaImapServer(String host, int port, int numThreads, boolean sslEnabled)
            throws IOException {
        socketAddr = address(host, port);
        acceptorConfig = new SocketAcceptorConfig();
        acceptorConfig.setReuseAddress(true);
        executorService = Executors.newFixedThreadPool(numThreads);
        socketAcceptor = new SocketAcceptor();
        this.sslEnabled = sslEnabled;
    }

    public void start() throws IOException {
        DefaultIoFilterChainBuilder fc = acceptorConfig.getFilterChain();
        if (sslEnabled) {
            fc.addFirst("ssl", new SSLFilter(MinaImapServer.getSSLContext()));
        }
        fc.addLast("decoder", new ProtocolCodecFilter(new CodecFactory()));
        fc.addLast("executer", new ExecutorFilter(executorService));
        fc.addLast("logger", new LoggingFilter());
        MinaImapIoHandler handler = new MinaImapIoHandler(this);
        ServerSocketChannel ssc = channels.get(socketAddr);
        if (ssc != null) {
            socketAcceptor.register(ssc, handler, acceptorConfig);
        } else {
            socketAcceptor.bind(socketAddr, handler, acceptorConfig);
        }
    }

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
    
    private static class CodecFactory implements ProtocolCodecFactory {
        public ProtocolEncoder getEncoder() {
            return new Encoder();
        }

        public ProtocolDecoder getDecoder() {
            return new Decoder();
        }
    }

    private static class Encoder extends ProtocolEncoderAdapter {
        public void encode(IoSession session, Object msg,
                           ProtocolEncoderOutput out) {
            ByteBuffer bb;
            if (msg instanceof String) {
                bb = toByteBuffer((String) msg);
            } else if (msg instanceof ByteBuffer) {
                bb = (ByteBuffer) msg;
            } else {
                throw new AssertionError();
            }
            out.write(org.apache.mina.common.ByteBuffer.wrap(bb));
        }
    }

    private static class Decoder extends ProtocolDecoderAdapter {
        MinaImapRequest req;  // Request currently being decoded

        public void decode(IoSession session,
                           org.apache.mina.common.ByteBuffer in,
                           ProtocolDecoderOutput out) throws IOException {
            ByteBuffer bb = in.buf();
            while (bb.hasRemaining()) {
                if (req == null) req = new MinaImapRequest(session);
                req.parse(session, bb);
                if (!req.isComplete()) break;
                out.write(req);
                req = null;
            }
        }
    }

    // TODO Move this to a utility class
    private static ByteBuffer toByteBuffer(String s) {
        ByteBuffer bb = ByteBuffer.allocate(s.length());
        for (int i = 0; i < s.length(); i++) {
            bb.put(i, (byte) s.charAt(i));
        }
        return bb;
    }

    public static void main(String... args) throws Exception {
        System.setProperty(ENABLED_PROP, "true");
        CliUtil.toolSetup("INFO");
        bind(null, 9143);
        MinaImapServer imapServer = new MinaImapServer(null, 9143, 10, false);
        imapServer.start();
    }
}
