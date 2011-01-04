/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.imap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.server.NioHandler;
import com.zimbra.cs.server.NioServer;
import com.zimbra.cs.server.NioCodecFactory;
import com.zimbra.cs.server.NioConnection;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;

public class NioImapServer extends NioServer implements ImapServer {

    public NioImapServer(ImapConfig config) throws ServiceException {
        super(config);
        registerMBean(config.isSslEnabled() ? "NioImapSSLServer" : "NioImapServer");
    }

    @Override
    public NioHandler createHandler(NioConnection conn) {
        return new NioImapHandler(this, conn);
    }

    @Override
    protected ProtocolCodecFactory getProtocolCodecFactory() {
        return new NioCodecFactory() {
            @Override
            public ProtocolDecoder getDecoder(IoSession session) {
                return new NioImapDecoder(getStats(), config.getWriteChunkSize());
            }
        };
    }

    @Override
    public ImapConfig getConfig() {
        return (ImapConfig) super.getConfig();
    }

    @Override
    public Log getLog() {
        return ZimbraLog.imap;
    }
}
