/*
 * Created on Apr 9, 2005
 */
package com.liquidsys.coco.imap;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.account.Server;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.tcpserver.ProtocolHandler;
import com.liquidsys.coco.tcpserver.TcpServer;
import com.liquidsys.coco.util.LiquidLog;

/**
 * @author dkarp
 */
public class ImapServer extends TcpServer {

    // Config idle. should be at least 30 minutes, per IMAP4 RFC 3501.
    public static final int DEFAULT_MAX_IDLE_SECONDS = 1800;
    private int mConfigMaxIdleMilliSeconds = DEFAULT_MAX_IDLE_SECONDS * 1000;

    private static ImapServer sImapServer;
    private static ImapServer sImapSSLServer;

    private String  mBanner;
    private String  mGoodbye;
    private boolean mAllowCleartextLogins;
    private boolean mConnectionSSL;


	public ImapServer(int numThreads, int port, InetAddress bindAddress, boolean loginOK, boolean ssl) {
        super(ssl ? "ImapSSLServer" : "ImapServer", numThreads, port, bindAddress);
        mAllowCleartextLogins = loginOK;
        mConnectionSSL = ssl;
    }

	protected ProtocolHandler newProtocolHandler() {
        return new ImapHandler(this);
	}

    boolean allowCleartextLogins()  { return mAllowCleartextLogins; }
    boolean isConnectionSSL()       { return mConnectionSSL; }

	public int getConfigMaxIdleMilliSeconds() {
		return mConfigMaxIdleMilliSeconds;
	}

    private void setHostname(Server server) {
        String name = server.getAttr(Provisioning.A_liquidImapAdvertisedName, null);
        if (name == null || name.trim().equals(""))
            name = server.getAttr(Provisioning.A_liquidServiceHostname, null);
        if (name == null || name.trim().equals(""))
            name = "localhost";

        mBanner  = "OK " + name + " Liquid IMAP4rev1 service ready";
        mGoodbye = "BYE Liquid IMAP4rev1 server terminating connection";
    }

    public String getBanner()  { return mBanner; }
    public String getGoodbye() { return mGoodbye; }

    private static InetAddress getBindAddress(String address) throws ServiceException {
        InetAddress bindAddress = null;
        if (address != null && address.length() > 0)
            try {
                bindAddress = InetAddress.getByName(address);
            } catch (UnknownHostException uhe) {
                throw ServiceException.FAILURE(uhe.getMessage(), uhe);
            }
        return bindAddress;
    }

    public synchronized static void startupImapServer() throws ServiceException {
        if (sImapServer != null)
            return;
        
        Server server = Provisioning.getInstance().getLocalServer();
        int threads = server.getIntAttr(Provisioning.A_liquidImapNumThreads, 10);
        boolean loginOK = server.getBooleanAttr(Provisioning.A_liquidImapCleartextLoginEnabled, false);
        InetAddress address = getBindAddress(server.getAttr(Provisioning.A_liquidImapBindAddress, null));
        int port = server.getIntAttr(Provisioning.A_liquidImapBindPort, 7143);

        sImapServer = new ImapServer(threads, port, address, loginOK, false);
        sImapServer.setSSL(false);
        sImapServer.setHostname(server);
        
        Thread imap = new Thread(sImapServer);
        imap.setName("ImapServer");
        imap.start();            
    }

    public synchronized static void startupImapSSLServer() throws ServiceException {
        if (sImapSSLServer != null)
            return;

        Server server = Provisioning.getInstance().getLocalServer();
        int threads = server.getIntAttr(Provisioning.A_liquidImapNumThreads, 10);
        InetAddress address = getBindAddress(server.getAttr(Provisioning.A_liquidImapSSLBindAddress, null));
        int port = server.getIntAttr(Provisioning.A_liquidImapSSLBindPort, 7993);

        sImapSSLServer = new ImapServer(threads, port, address, true, true);
        sImapSSLServer.setSSL(true);
        sImapSSLServer.setHostname(server);

        Thread imaps = new Thread(sImapSSLServer);
        imaps.setName("ImapSSLServer");
        imaps.start();            
    }

    public synchronized static void shutdownImapServers() {
        if (sImapServer != null)
            sImapServer.shutdown(10); // TODO shutdown grace period from config
        sImapServer = null;

        if (sImapSSLServer != null)
            sImapSSLServer.shutdown(10); // TODO shutdown grace period from config
        sImapSSLServer = null;
    }
    
    public static void main(String args[]) throws ServiceException {
        LiquidLog.toolSetupLog4j("INFO");
        startupImapServer();
    }
}
