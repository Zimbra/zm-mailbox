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

package com.zimbra.cs.imap;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mina.MinaHandler;
import com.zimbra.cs.mina.MinaRequest;
import com.zimbra.cs.mina.MinaServer;
import org.apache.mina.common.IoSession;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

/**
 * MINA-based IMAP server implementation.
 */
public class MinaImapServer extends MinaServer {
    public static boolean isEnabled() {
        return MinaServer.isEnabled() || LC.nio_imap_enabled.booleanValue();
    }

    MinaImapServer(ImapConfig config, ExecutorService pool)
            throws IOException, ServiceException {
        super(config, pool);
    }

    @Override
    public MinaHandler createHandler(IoSession session) {
        return new MinaImapHandler(this, session);
    }

    @Override
    public MinaRequest createRequest(MinaHandler handler) {
        return new MinaImapRequest(handler);
    }

    @Override
    public Log getLog() { return ZimbraLog.imap; }
}
