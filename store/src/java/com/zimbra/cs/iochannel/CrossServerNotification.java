/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.iochannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;

import org.dom4j.DocumentException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.JSONElement;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.soap.ZimbraNamespace;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.cs.session.SoapSession;
import com.zimbra.cs.session.SoapSession.RemoteNotifications;
import com.zimbra.soap.ZimbraSoapContext;

public class CrossServerNotification extends Message {

    public static final String AppId = "xsn";

    public static CrossServerNotification create(SoapSession session, ZimbraSoapContext zsc) throws MessageChannelException, ServiceException {
        Element soapElement;
        try {
            soapElement = Element.create(SoapProtocol.Soap12, "base");
        } catch (ServiceException e) {
            throw MessageChannelException.CannotCreate("element");
        }
        session.putNotifications(soapElement, zsc, 0);
        Element notifyElement = soapElement.getOptionalElement(ZimbraNamespace.E_NOTIFY);
        if (notifyElement == null) {
            throw MessageChannelException.CannotCreate("no notification");
        }
        notifyElement.addAttribute(HeaderConstants.A_SEQNO, (String)null);
        StringBuilder soap = new StringBuilder();
        try {
            notifyElement.marshal(soap);
        } catch (IOException e) {
            throw MessageChannelException.CannotCreate("marshall");
        }
        String accountId = zsc.getAuthtokenAccountId();
        return new CrossServerNotification(accountId, "", soap.toString());
    }

    @Override
    protected int size() {
        // 4 byte int padding for length of each strings.
        return 2 * (accountId.length() + sessionId.length() + payload.length()) + 12;
    }


    @Override
    protected void serialize(ByteBuffer buffer) throws IOException {
        writeString(buffer, accountId);
        writeString(buffer, sessionId);
        writeString(buffer, payload);
    }

    @Override
    protected Message construct(ByteBuffer buffer) throws IOException {
        return new CrossServerNotification(buffer);
    }

    @Override
    public String getAppId() {
        return AppId;
    }

    @Override
    public String getRecipientAccountId() {
        return accountId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getPayload() {
        return payload;
    }

    CrossServerNotification() {
    }

    public CrossServerNotification(ByteBuffer buffer) throws IOException {
        super();
        accountId = readString(buffer);
        sessionId = readString(buffer);
        payload = readString(buffer);
    }

    private CrossServerNotification(String aid, String sid, String ntfn) {
        super();
        accountId = aid;
        sessionId = sid;
        payload = ntfn;
    }

    @Override
    public MessageHandler getHandler() {
        return new MessageHandler() {
            @Override
            public void handle(Message m, String clientId) {
                if (!(m instanceof CrossServerNotification)) {
                    return;
                }
                CrossServerNotification message = (CrossServerNotification)m;
                Collection<Session> sessions = SessionCache.getSoapSessions(m.getRecipientAccountId());
                if (sessions == null) {
                    log.warn("no active sessions for account %s", m.getRecipientAccountId());
                    return;
                }
                RemoteNotifications soapNtfn = null, jsonNtfn = null;
                try {
                    org.dom4j.Document dom = org.dom4j.DocumentHelper.parseText(message.getPayload());
                    soapNtfn = new RemoteNotifications(Element.convertDOM(dom.getRootElement(), XMLElement.mFactory));
                    jsonNtfn = new RemoteNotifications(Element.convertDOM(dom.getRootElement(), JSONElement.mFactory));
                } catch (DocumentException e) {
                    log.warn("cannot parse notification from %s", clientId, e);
                    return;
                }
                for (Session session : sessions) {
                    log.debug("notifying session %s", session.toString());
                    SoapSession ss = (SoapSession)session;
                    SoapProtocol responseProtocol = ss.getResponseProtocol();
                    if (responseProtocol == SoapProtocol.Soap11 || responseProtocol == SoapProtocol.Soap12) {
                        ss.addRemoteNotifications(soapNtfn);
                    } else if (responseProtocol == SoapProtocol.SoapJS) {
                        ss.addRemoteNotifications(jsonNtfn);
                    }
                    ss.forcePush();
                }
            }
        };
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(AppId).append(":");
        buf.append(accountId).append(":");
        buf.append(sessionId).append(":");
        buf.append(payload);
        return buf.toString();
    }

    private String accountId;
    private String sessionId;
    private String payload;
}
