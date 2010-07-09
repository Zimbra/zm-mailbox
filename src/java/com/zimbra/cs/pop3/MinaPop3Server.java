/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.pop3;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mina.MinaHandler;
import com.zimbra.cs.mina.MinaServer;
import com.zimbra.cs.mina.MinaCodecFactory;
import com.zimbra.cs.mina.MinaSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

public class MinaPop3Server extends MinaServer implements Pop3Server {
    public MinaPop3Server(Pop3Config config, ExecutorService pool) throws ServiceException {
        super(config, pool);
        registerMinaStatsMBean(
            config.isSslEnabled() ? "MinaPop3SSLServer" : "MinaPop3Server");
    }

    @Override
    public MinaHandler createHandler(MinaSession session) {
        return new MinaPop3Handler(this, session);
    }

    @Override
    protected ProtocolCodecFactory getProtocolCodecFactory() {
        return new MinaCodecFactory() {
            public ProtocolDecoder getDecoder() {
                return new MinaPop3Decoder(getStats());
            }
        };
    }

    public Pop3Config getConfig() {
        return (Pop3Config) super.getConfig();
    }
    
    @Override
    public Log getLog() { return ZimbraLog.pop; }
}
