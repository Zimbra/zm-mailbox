package com.zimbra.cs.im;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.jivesoftware.wildfire.ClientSession;
import org.jivesoftware.wildfire.Connection;
import org.jivesoftware.wildfire.XMPPServer;
import org.jivesoftware.wildfire.auth.AuthToken;
import org.jivesoftware.wildfire.auth.UnauthorizedException;
import org.jivesoftware.wildfire.net.VirtualConnection;
import org.jivesoftware.wildfire.user.UserNotFoundException;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.packet.Presence;
import org.xmpp.packet.Roster;
import org.xmpp.packet.StreamError;
import org.xmpp.packet.IQ.Type;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.im.IMMessage.TextPart;
import com.zimbra.cs.service.ServiceException;

public class FakeClientSession extends ClientSession {
    
    
    static class FakeClientConnection extends VirtualConnection {
        
        FakeClientSession mSession;

        public void setSession(FakeClientSession session) {
            mSession = session;
        }

        @Override
        public void closeVirtualConnection() {
        }

        public void deliver(Packet packet) throws UnauthorizedException {
            mSession.process(packet);
        }

        public void deliverRawText(String text) {
        }

        public InetAddress getInetAddress() {
            return null;
        }

        public void systemShutdown() {
        }
        
    }
    
    private String mAddr;
    private IMPersona mPersona;

    public FakeClientSession(String serverName, String addr, IMPersona persona) {
        super(serverName, new FakeClientConnection(), XMPPServer.getInstance().getSessionManager().nextStreamID());
        mAddr = addr;
        mPersona  = persona;
        ((FakeClientConnection)this.getConnection()).setSession(this);
    }
    
    void addRoutes() {
        setAuthToken(new AuthToken(mAddr));
        try {
            // if there is already a "zcs" session, kick it off
            if (sessionManager.isActiveRoute(mAddr, "zcs")) {
                ClientSession oldSession = null;
                
                oldSession = sessionManager.getSession(mAddr, "timsmac.local", "zcs");
                Connection conn = oldSession.getConnection();
                if (conn != null) {
                    // Kick out the old connection that is conflicting with the new one
                    StreamError error = new StreamError(StreamError.Condition.conflict);
                    
                    try {
                        conn.deliverRawText(error.toXML());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    conn.close();
                } else {
                    // this is the common case: there is no connection, since it is a FakeClientSession
                    sessionManager.removeSession(oldSession);
                }
            }
            
            setAuthToken(new AuthToken(mAddr), XMPPServer.getInstance().getUserManager(), "zcs");
        } catch (UserNotFoundException ex) {
            System.out.println(ex.toString());
            ex.printStackTrace();
        }
//        SessionManager.getInstance().addSession(this);
//        SessionManager.getInstance().sessionAvailable(this);
//        int domainIdx = mAddr.indexOf('@'); 
//        String namePart = mAddr.substring(0, domainIdx);
//        String domainPart = mAddr.substring(domainIdx+1);
//        JID jid = new JID(namePart, domainPart, "zcs");
//        
//        XMPPServer.getInstance().getRoutingTable().addRoute(jid, this);
//
//        JID jid2 = new JID(namePart, domainPart, null);
//        XMPPServer.getInstance().getRoutingTable().addRoute(jid2, this);
//        
//        
    }
    
    public boolean isInitialized() {
        return true;
    }

    public boolean isOfflineFloodStopped() {
        return true;
    }

    public void process(Packet packet) {
        if (shouldBlockPacket(packet)) {
            // Communication is blocked. Drop packet.
            return;
        }
        
//        ZimbraLog.im.info("FakeClientSession processing packet: "+packet.toXML());
        IMXmppEvent imXmppEvent = new IMXmppEvent(mPersona.getAddr(), packet);
        IMRouter.getInstance().postEvent(imXmppEvent);
    }

//    public boolean canFloodOfflineMessages() {
//        return false;
//    }
//
//    public PrivacyList getActiveList() {
//        return null;
//    }
//
//    public String getAvailableStreamFeatures() {
//        return null;
//    }
//
//    public int getConflictCount() {
//        return 0;
//    }
//
//    public PrivacyList getDefaultList() {
//        return null;
//    }
//
//    public void incrementConflictCount() {
//    }
//
//    public void setActiveList(PrivacyList activeList) {
//    }
//
//    public void setDefaultList(PrivacyList defaultList) {
//    }

    public void setInitialized(boolean isInit) {
    }

    public void setOfflineFloodStopped(boolean offlineFloodStopped) {
    }
    
//    Presence mPresence;
//
//    public Presence setPresence(Presence presence) {
//        mPresence = presence;
//        return mPresence;
//    }
//    
//    public Presence getPresence() {
//        return mPresence;
//    }

    

    public boolean shouldBlockPacket(Packet packet) {
        return false;
    }

    public String toString() {
        return "FakeClientSession(" + this.getStreamID() +")";
    }

    public boolean wasAvailable() {
        return false;
    }
}
