/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * Part of the Zimbra Collaboration Suite Server.
 *
 * The Original Code is Copyright (C) Jive Software. Used with permission
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */

//package com.zimbra.cs.im.xmpp.srv.net;
//
//import com.zimbra.cs.im.xmpp.srv.container.BasicModule;
//import com.zimbra.cs.im.xmpp.srv.XMPPServer;
//import com.zimbra.cs.im.xmpp.srv.XMPPServerInfo;
//import com.zimbra.cs.im.xmpp.srv.ServerPort;
//import com.zimbra.cs.im.xmpp.util.Log;
//import com.zimbra.cs.im.xmpp.admin.AdminConsole;
//
//import javax.jmdns.JmDNS;
//import javax.jmdns.ServiceInfo;
//import java.io.IOException;
//import java.util.Iterator;
//
///**
// * Publishes Wildfire as a service using the Multicast DNS (marketed by Apple
// * as Rendezvous) protocol. This lets other nodes on the local network to discover
// * the name and port of Wildfire.<p>
// *
// * The multicast DNS entry is published with a type of "_xmpp-client._tcp.local.".
// *
// * @author Matt Tucker
// */
//public class MulticastDNSService extends BasicModule {
//
//    private JmDNS jmdns;
//    private ServiceInfo serviceInfo;
//
//    public MulticastDNSService() {
//        super("Multicast DNS Service");
//    }
//
//    public void initialize(XMPPServer server) {
//       
//    }
//
//    public void start() throws IllegalStateException {
//        if (jmdns != null) {
//            Runnable startService = new Runnable() {
//                public void run() {
//                    XMPPServerInfo info = XMPPServer.getInstance().getServerInfo();
//                    Iterator ports = info.getServerPorts();
//                    int portNum = -1;
//                    while (ports.hasNext()) {
//                        ServerPort port = (ServerPort)ports.next();
//                        if (port.isClientPort() && !port.isSecure()) {
//                            portNum = port.getPort();
//                        }
//                    }
//                    try {
//                        if (jmdns == null) {
//                            jmdns = new JmDNS();
//                        }
//                        if (portNum != -1) {
//                            String serverName = AdminConsole.getAppName();
//                            serviceInfo = new ServiceInfo("_xmpp-client._tcp.local.",
//                                    serverName, portNum, 0, 0, "XMPP Server");
//                            jmdns.registerService(serviceInfo);
//                        }
//                    }
//                     catch (IOException ioe) {
//                        Log.error(ioe);
//                    }
//                }
//            };
//            new Thread(startService).start();
//        }
//    }
//
//
//    public void stop() {
//        if (jmdns != null) {
//            try {
//                jmdns.close();
//            }
//            catch (Exception e) { }
//        }
//    }
//
//    public void destroy() {
//        if (jmdns != null) {
//            jmdns = null;
//        }
//    }
//}