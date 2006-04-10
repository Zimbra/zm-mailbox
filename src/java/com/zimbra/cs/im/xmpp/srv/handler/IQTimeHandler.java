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

package com.zimbra.cs.im.xmpp.srv.handler;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.QName;
import com.zimbra.cs.im.xmpp.srv.IQHandlerInfo;
import com.zimbra.cs.im.xmpp.srv.disco.ServerFeaturesProvider;
import com.zimbra.cs.im.xmpp.util.FastDateFormat;
import org.xmpp.packet.IQ;

import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;

/**
 * Implements the TYPE_IQ jabber:iq:time protocol (time info) as
 * as defined by JEP-0090. Allows Jabber entities to query each
 * other's local time.  The server will respond with its local time.
 * <p/>
 * <h2>Assumptions</h2>
 * This handler assumes that the time request is addressed to itself.
 * An appropriate TYPE_IQ tag matcher should be placed in front of this
 * one to route TYPE_IQ time requests not addressed to the server to
 * another channel (probably for direct delivery to the recipient).
 * <p/>
 * <h2>Warning</h2>
 * There should be a way of determining whether a session has
 * authorization to access this feature. I'm not sure it is a good
 * idea to do authorization in each handler. It would be nice if
 * the framework could assert authorization policies across channels.
 *
 * @author Iain Shigeoka
 */
public class IQTimeHandler extends IQHandler implements ServerFeaturesProvider {

     // todo Make display text match the locale of user (xml:lang support)
    private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.MEDIUM);
    private static final DateFormat TIME_FORMAT = DateFormat.getTimeInstance(DateFormat.LONG);
    // UTC and not JEP-0082 time format is used as per the JEP-0090 specification.
    private static final FastDateFormat UTC_FORMAT = FastDateFormat
            .getInstance("yyyyMMdd'T'HH:mm:ss", TimeZone.getTimeZone("GMT+0"));

    private Element responseElement;
    private IQHandlerInfo info;

    public IQTimeHandler() {
        super("XMPP Server Time Handler");
        info = new IQHandlerInfo("query", "jabber:iq:time");
        responseElement = DocumentHelper.createElement(QName.get("query", "jabber:iq:time"));
        responseElement.addElement("utc");
        responseElement.addElement("tz").setText(TIME_FORMAT.getTimeZone().getDisplayName());
        responseElement.addElement("display");
    }

    public IQ handleIQ(IQ packet) {
        IQ response = null;
        response = IQ.createResultIQ(packet);
        response.setChildElement(buildResponse());
        return response;
    }

    /**
     * Build the responseElement packet
     */
    private Element buildResponse() {
        Element response = responseElement.createCopy();
        Date current = new Date();
        response.element("utc").setText(UTC_FORMAT.format(current));
        StringBuilder display = new StringBuilder(DATE_FORMAT.format(current));
        display.append(' ');
        display.append(TIME_FORMAT.format(current));
        response.element("display").setText(display.toString());
        return response;
    }

    public IQHandlerInfo getInfo() {
        return info;
    }

    public Iterator<String> getFeatures() {
        return Collections.singleton("jabber:iq:time").iterator();
    }
}