package com.zimbra.cs.imap;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mina.MinaHandler;
import com.zimbra.cs.mina.MinaRequest;
import com.zimbra.cs.mina.MinaServer;
import org.apache.mina.common.IoSession;

import java.io.IOException;

/**
 * Mina-based IMAP server.
 */
public class MinaImapServer extends MinaServer {
    public static final String ENABLED_PROP = "ZimbraNioImapEnabled";
    
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
        super(host, port, numThreads, sslEnabled);
    }

    @Override public MinaHandler createHandler(IoSession session) {
        return new MinaImapHandler(this, session);
    }

    @Override public MinaRequest createRequest(MinaHandler handler) {
        return new MinaImapRequest(handler);
    }

    @Override public Log getLog() { return ZimbraLog.imap; }
    
    public static void main(String... args) throws Exception {
        System.setProperty(ENABLED_PROP, "true");
        CliUtil.toolSetup("INFO");
        bind(null, 9143);
        MinaImapServer imapServer = new MinaImapServer(null, 9143, 10, false);
        imapServer.start();
    }
}
