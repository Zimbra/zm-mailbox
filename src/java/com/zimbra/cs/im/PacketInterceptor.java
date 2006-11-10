package com.zimbra.cs.im;

import org.jivesoftware.wildfire.Session;
import org.xmpp.packet.Packet;

import com.zimbra.common.util.ZimbraLog;

public class PacketInterceptor implements org.jivesoftware.wildfire.interceptor.PacketInterceptor {

    public void interceptPacket(Packet packet, Session session, boolean incoming, boolean processed) /* throws PacketRejectedException */ {
//        ZimbraLog.im.info("Session\n"+ session.toString() +"\nIntercepting an " + (incoming ? "INCOMING " : "OUTGOING ") + (processed ? "PROCESSED " : "NOT PROCESSED ") +" packet:\n"+packet.toString()+"\n");
        if (processed) {
            String addr = session.getAddress().toBareJID().toString();
            
            IMXmppEvent imXmppEvent = new IMXmppEvent(new IMAddr(addr), packet);
            
            IMRouter.getInstance().postEvent(imXmppEvent);
        }
    }
}
