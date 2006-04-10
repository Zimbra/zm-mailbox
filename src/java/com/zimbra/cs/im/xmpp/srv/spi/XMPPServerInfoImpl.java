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

package com.zimbra.cs.im.xmpp.srv.spi;

import com.zimbra.cs.im.xmpp.util.Version;
import com.zimbra.cs.im.xmpp.srv.XMPPServerInfo;
import com.zimbra.cs.im.xmpp.srv.ConnectionManager;
import com.zimbra.cs.im.xmpp.util.JiveGlobals;

import java.util.Date;
import java.util.Iterator;
import java.util.Collections;

/**
 * Implements the server info for a basic server. Optimization opportunities
 * in reusing this object the data is relatively static.
 *
 * @author Iain Shigeoka
 */
public class XMPPServerInfoImpl implements XMPPServerInfo {

    private Date startDate;
    private Date stopDate;
    private String name;
    private Version ver;
    private ConnectionManager connectionManager;

    /**
     * Simple constructor
     *
     * @param serverName the server's serverName (e.g. example.org).
     * @param version the server's version number.
     * @param startDate the server's last start time (can be null indicating
     *      it hasn't been started).
     * @param stopDate the server's last stop time (can be null indicating it
     *      is running or hasn't been started).
     * @param connectionManager the object that keeps track of the active ports.
     */
    public XMPPServerInfoImpl(String serverName, Version version, Date startDate, Date stopDate,
            ConnectionManager connectionManager)
    {
        this.name = serverName;
        this.ver = version;
        this.startDate = startDate;
        this.stopDate = stopDate;
        this.connectionManager = connectionManager;
    }

    public Version getVersion() {
        return ver;
    }

    public String getName() {
        return name;
    }

    public void setName(String serverName) {
        name = serverName;
        if (serverName == null) {
            JiveGlobals.deleteProperty("xmpp.domain");
        }
        else {
            JiveGlobals.setProperty("xmpp.domain", serverName);
        }
    }

    public Date getLastStarted() {
        return startDate;
    }

    public Date getLastStopped() {
        return stopDate;
    }

    public Iterator getServerPorts() {
        if (connectionManager == null) {
            return Collections.EMPTY_LIST.iterator();
        }
        else {
            return connectionManager.getPorts();
        }
    }
}