package com.zimbra.cs.im;

import java.net.InetAddress;

import org.jivesoftware.wildfire.auth.UnauthorizedException;
import org.jivesoftware.wildfire.net.VirtualConnection;
import org.xmpp.packet.Packet;

public class FakeClientConnection extends VirtualConnection {
    
    IMAddr mAddr;
    
    FakeClientConnection(IMPersona persona) {
        mAddr = persona.getAddr();
    }

    public void closeVirtualConnection() {
    }

    public void deliver(Packet packet) throws UnauthorizedException {
        IMXmppEvent imXmppEvent = new IMXmppEvent(mAddr, packet);
        IMRouter.getInstance().postEvent(imXmppEvent);
    }

    public void deliverRawText(String text) {
        IMXmppTextEvent imXmppEvent = new IMXmppTextEvent(mAddr, text);
        IMRouter.getInstance().postEvent(imXmppEvent);
    }

    public InetAddress getInetAddress() {
        return null;
    }

    public void systemShutdown() {
    }
}
