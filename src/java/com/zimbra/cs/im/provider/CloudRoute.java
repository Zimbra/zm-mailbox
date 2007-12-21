/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.im.provider;

import java.net.InetSocketAddress;
import java.net.Socket;

import org.dom4j.Element;
import org.jivesoftware.wildfire.ChannelHandler;
import org.jivesoftware.wildfire.Connection;
import org.jivesoftware.wildfire.PacketDeliverer;
import org.jivesoftware.wildfire.PacketException;
import org.jivesoftware.wildfire.RoutingTable;
import org.jivesoftware.wildfire.Session;
import org.jivesoftware.wildfire.StreamID;
import org.jivesoftware.wildfire.XMPPServer;
import org.jivesoftware.wildfire.auth.UnauthorizedException;
import org.jivesoftware.wildfire.net.CloudRoutingSocketReader;
import org.jivesoftware.wildfire.net.SocketConnection;
import org.jivesoftware.wildfire.net.StdSocketConnection;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.packet.PacketError;
import org.xmpp.packet.Presence;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;

/**
 * This class represents a route in the *LOCAL* cloud, ie between trusted servers
 * in the same backend.
 */
public class CloudRoute extends Session {

    static public CloudRoute create(String hostname, CloudRoutingSocketReader reader, SocketConnection connection, Element streamElt) {
        CloudRoute toRet = new CloudRoute(connection, XMPPServer.getInstance().getSessionManager().nextStreamID());
        return toRet;
    }

    static CloudRoute connect(Server targetServer) throws Exception {
        String hostname = targetServer.getAttr(Provisioning.A_zimbraServiceHostname);
        int port = 7335;
        
        Socket socket = new Socket();
        try {
            
            ZimbraLog.im.info("LocalCloudRoute: Trying to connect (3)  " + hostname + ":" + port);
            
            // Establish a TCP connection to the Receiving Server
            socket.connect(new InetSocketAddress(hostname, port), 10000);
            ZimbraLog.im.debug("LocalCloudRoute: Plain connection to " + hostname + ":" + port + " successful");
            
            SocketConnection connection = new StdSocketConnection(new ErrorPacketDeliverer(), socket, false); 
            
            // Send the stream header
            StringBuilder openingStream = new StringBuilder();
            openingStream.append("<stream:stream");
            openingStream.append(" xmlns:stream=\"http://etherx.jabber.org/streams\"");
            openingStream.append(" xmlns=\"jabber:cloudrouting\"");
            openingStream.append(" to=\"").append(hostname).append("\"");
            openingStream.append(" version=\"1.0\">\n");
            ZimbraLog.im.debug("LocalCloudRoute - Sending stream header: "+openingStream.toString()); 
            connection.deliverRawText(openingStream.toString());

            CloudRoutingSocketReader ssReader = new CloudRoutingSocketReader(XMPPServer.getInstance().getPacketRouter(), 
                XMPPServer.getInstance().getRoutingTable(), socket, connection);
            
            Thread readerThread = new Thread(ssReader, "LocalCloudRouterReader-"+connection.toString());
            readerThread.setDaemon(true);
            readerThread.start();
             
            CloudRoute toRet = new CloudRoute(targetServer, connection, XMPPServer.getInstance().getSessionManager().nextStreamID());
            ssReader.setSession(toRet);
            return toRet;
        }
        catch (Exception e) {
            ZimbraLog.im.warn("Error trying to connect to remote server: " + hostname + ":" + port, e);
            throw e;
        }
    }
    
    private static void remoteDeliveryFailed(Packet packet) {
        JID from = packet.getFrom();
        JID to = packet.getTo();
        
        // TODO Send correct error condition: timeout or not_found depending on the real error
        try {
            if (packet instanceof IQ) {
                IQ reply = new IQ();
                reply.setID(((IQ) packet).getID());
                reply.setTo(from);
                reply.setFrom(to);
                reply.setChildElement(((IQ) packet).getChildElement().createCopy());
                reply.setError(PacketError.Condition.remote_server_not_found);
                ChannelHandler route = sRoutingTable.getRoute(reply.getTo());
                if (route != null) {
                    route.process(reply);
                }
            }
            else if (packet instanceof Presence) {
                Presence reply = new Presence();
                reply.setID(packet.getID());
                reply.setTo(from);
                reply.setFrom(to);
                reply.setError(PacketError.Condition.remote_server_not_found);
                ChannelHandler route = sRoutingTable.getRoute(reply.getTo());
                if (route != null) {
                    route.process(reply);
                }
            }
            else if (packet instanceof Message) {
                Message reply = new Message();
                reply.setID(packet.getID());
                reply.setTo(from);
                reply.setFrom(to);
                reply.setType(((Message)packet).getType());
                reply.setThread(((Message)packet).getThread());
                reply.setError(PacketError.Condition.remote_server_not_found);
                ChannelHandler route = sRoutingTable.getRoute(reply.getTo());
                if (route != null) {
                    route.process(reply);
                }
            }
        }
        catch (UnauthorizedException e) {
        }
        catch (Exception e) {
            ZimbraLog.im.warn("Error returning error to sender. Original packet: " + packet, e);
        }
    } 

    private CloudRoute(Connection conn, StreamID streamID) {
        super("cloudroute.fake.domain.com", conn, streamID);
        mServer = null;
    }
    /**
     * 
     */
    private CloudRoute(Server targetServer, Connection conn, StreamID streamID) {
        super(targetServer.getName(), conn, streamID);
        mServer = targetServer;
    }
    
    @Override
    public JID getAddress() {
        return null;
    }
    
    @Override
    public String getAvailableStreamFeatures() {
        return null;
    }
    
    public void process(Packet packet) throws UnauthorizedException, PacketException {
        if (conn != null&& !conn.isClosed()) {
            conn.deliver(packet);
        } else {
            remoteDeliveryFailed(packet);
        }
    }

    Server getServer() { return mServer; }
    
    private static RoutingTable sRoutingTable = XMPPServer.getInstance().getRoutingTable();
    
    private Server mServer;
    
    private static class ErrorPacketDeliverer implements PacketDeliverer {
        public void deliver(Packet packet) throws UnauthorizedException, PacketException {
            remoteDeliveryFailed(packet);
        }
    }
    
}
