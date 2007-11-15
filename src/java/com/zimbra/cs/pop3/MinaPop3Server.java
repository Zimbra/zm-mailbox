/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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

package com.zimbra.cs.pop3;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mina.MinaHandler;
import com.zimbra.cs.mina.MinaRequest;
import com.zimbra.cs.mina.MinaServer;
import com.zimbra.cs.mina.MinaTextLineRequest;
import com.zimbra.cs.server.ServerConfig;
import org.apache.mina.common.IoSession;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

public class MinaPop3Server extends MinaServer {
    public static boolean isEnabled() {
        return MinaServer.isEnabled() || LC.nio_pop3_enabled.booleanValue();
    }

    MinaPop3Server(ServerConfig config, ExecutorService pool)
            throws IOException, ServiceException {
        super(config, pool);
    }

    @Override public MinaHandler createHandler(IoSession session) {
        return new MinaPop3Handler(this, session);
    }

    @Override public MinaRequest createRequest(MinaHandler handler) {
        return new MinaTextLineRequest();
    }

    @Override public Log getLog() { return ZimbraLog.pop; }
}
