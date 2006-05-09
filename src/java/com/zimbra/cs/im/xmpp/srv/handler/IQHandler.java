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
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.im.xmpp.srv.handler;

import com.zimbra.cs.im.xmpp.srv.*;
import com.zimbra.cs.im.xmpp.srv.auth.UnauthorizedException;
import com.zimbra.cs.im.xmpp.srv.container.BasicModule;
import com.zimbra.cs.im.xmpp.util.LocaleUtils;
import com.zimbra.cs.im.xmpp.util.Log;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Packet;
import org.xmpp.packet.PacketError;

/**
 * Base class whose main responsibility is to handle IQ packets. Subclasses may
 * only need to specify the IQHandlerInfo (i.e. name and namespace of the packets
 * to handle) and actually handle the IQ packet. Simplifies creation of simple
 * TYPE_IQ message handlers.
 *
 * @author Gaston Dombiak
 */
public abstract class IQHandler extends BasicModule implements ChannelHandler {

    protected PacketDeliverer deliverer;
    protected SessionManager sessionManager;

    /**
     * Create a basic module with the given name.
     *
     * @param moduleName The name for the module or null to use the default
     */
    public IQHandler(String moduleName) {
        super(moduleName);
    }

    public void process(Packet packet) throws PacketException {
        IQ iq = (IQ) packet;
        try {
            iq = handleIQ(iq);
            if (iq != null) {
                deliverer.deliver(iq);
            }
        }
        catch (com.zimbra.cs.im.xmpp.srv.auth.UnauthorizedException e) {
            if (iq != null) {
                try {
                    IQ response = IQ.createResultIQ(iq);
                    response.setChildElement(iq.getChildElement().createCopy());
                    response.setError(PacketError.Condition.not_authorized);
                    Session session = sessionManager.getSession(iq.getFrom());
                    if (!session.getConnection().isClosed()) {
                        session.process(response);
                    }
                }
                catch (Exception de) {
                    Log.error(LocaleUtils.getLocalizedString("admin.error"), de);
                    sessionManager.getSession(iq.getFrom()).getConnection().close();
                }
            }
        }
        catch (Exception e) {
            Log.error(LocaleUtils.getLocalizedString("admin.error"), e);
        }
    }

    /**
     * Handles the received IQ packet.
     *
     * @param packet the IQ packet to handle.
     * @return the response to send back.
     * @throws UnauthorizedException if the user that sent the packet is not
     *      authorized to request the given operation.
     */
    public abstract IQ handleIQ(IQ packet) throws UnauthorizedException;

    /**
     * Returns the handler information to help generically handle IQ packets.
     * IQHandlers that aren't local server iq handlers (e.g. chatbots, transports, etc)
     * return <tt>null</tt>.
     *
     * @return The IQHandlerInfo for this handler
     */
    public abstract IQHandlerInfo getInfo();

    public void initialize(XMPPServer server) {
        super.initialize(server);
        deliverer = server.getPacketDeliverer();
        sessionManager = server.getSessionManager();
    }
}