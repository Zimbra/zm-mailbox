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
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.pop3;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mina.MinaHandler;
import com.zimbra.cs.mina.MinaRequest;
import com.zimbra.cs.mina.MinaServer;
import com.zimbra.cs.mina.MinaTextLineRequest;
import com.zimbra.cs.server.ServerConfig;

import java.io.IOException;

import org.apache.mina.common.IoSession;

public class MinaPop3Server extends MinaServer {
    public static boolean isEnabled() {
        return MinaServer.isEnabled() || LC.nio_pop3_enabled.booleanValue();
    }

    MinaPop3Server(ServerConfig config) throws IOException, ServiceException {
        super(config);
    }

    @Override public MinaHandler createHandler(IoSession session) {
        return new MinaPop3Handler(this, session);
    }

    @Override public MinaRequest createRequest(MinaHandler handler) {
        return new MinaTextLineRequest();
    }

    @Override public Log getLog() { return ZimbraLog.pop; }
}
