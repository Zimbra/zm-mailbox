package com.zimbra.cs.im;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.packet.Presence;
import org.xmpp.packet.StreamError;

import com.zimbra.cs.im.IMMessage.TextPart;
import com.zimbra.cs.im.xmpp.srv.ClientSession;
import com.zimbra.cs.im.xmpp.srv.Connection;
import com.zimbra.cs.im.xmpp.srv.XMPPServer;
import com.zimbra.cs.im.xmpp.srv.auth.AuthToken;
import com.zimbra.cs.im.xmpp.srv.privacy.PrivacyList;
import com.zimbra.cs.im.xmpp.srv.user.UserManager;
import com.zimbra.cs.im.xmpp.srv.user.UserNotFoundException;
import com.zimbra.cs.service.ServiceException;

public class FakeClientSession extends ClientSession {
    
    private String mAddr;
    private IMPersona mPersona;

    public FakeClientSession(String serverName, String addr, IMPersona persona) {
        super(serverName, null, XMPPServer.getInstance().getSessionManager().nextStreamID());
        mAddr = addr;
        mPersona  = persona;
    }
    
    public void addRoutes() {
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
                        conn.getWriter().write(error.toXML());
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
    
    public boolean canFloodOfflineMessages() {
        return false;
    }

    public PrivacyList getActiveList() {
        return null;
    }

    public String getAvailableStreamFeatures() {
        return null;
    }

    public int getConflictCount() {
        return 0;
    }

    public PrivacyList getDefaultList() {
        return null;
    }

    public void incrementConflictCount() {
    }

    public boolean isInitialized() {
        return true;
    }

    public boolean isOfflineFloodStopped() {
        return true;
    }

    public void process(Packet packet) {
        System.out.println("FakeClientSession received packet: "+ packet.toString());
        if (shouldBlockPacket(packet)) {
            // Communication is blocked. Drop packet.
            return;
        }

        if (packet instanceof Message) {
            Message msg = (Message)packet;
            String toAddr = msg.getTo().getNode() + '@'+  msg.getTo().getDomain();
            String fromAddr = msg.getFrom().getNode() + '@' + msg.getFrom().getDomain();
            String threadId = msg.getThread();
            
            String subject = msg.getSubject();
            String body = msg.getBody();

            IMMessage immsg = new IMMessage(subject==null?null:new TextPart(subject),
                        body==null?null:new TextPart(body));
            
            immsg.setFrom(new IMAddr(fromAddr));
            
            assert(toAddr.equals(mPersona.getAddr().getAddr()));
            
            List<IMAddr> toList = new ArrayList<IMAddr>(1);
            toList.add(mPersona.getAddr());
            
            IMSendMessageEvent sendEvt = new IMSendMessageEvent(new IMAddr(fromAddr), threadId, toList, immsg);
            
            IMRouter.getInstance().postEvent(sendEvt);
        } else if (packet instanceof Presence) {
            Presence pres = (Presence)packet;
            Presence reply = new Presence();
            reply.setTo(pres.getFrom());
            reply.setFrom(pres.getTo());
            reply.setType(Presence.Type.subscribed);
            XMPPServer.getInstance().getPresenceRouter().route(reply);
        }
    }

    public void setActiveList(PrivacyList activeList) {
    }

    public void setDefaultList(PrivacyList defaultList) {
    }

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
