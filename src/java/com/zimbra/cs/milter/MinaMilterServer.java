/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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
package com.zimbra.cs.milter;

import java.util.concurrent.ExecutorService;

import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mina.MinaCodecFactory;
import com.zimbra.cs.mina.MinaHandler;
import com.zimbra.cs.mina.MinaServer;
import com.zimbra.cs.mina.MinaSession;
import com.zimbra.cs.server.ServerConfig;

public class MinaMilterServer extends MinaServer implements MilterServer {

    public MinaMilterServer(ServerConfig config, ExecutorService pool) throws ServiceException {
        super(config, pool);
        registerMinaStatsMBean("MinaMilterServer");
    }
    
    @Override public MinaHandler createHandler(MinaSession session) {
        return new MinaMilterHandler(this, session);
    }

    @Override protected ProtocolCodecFactory getProtocolCodecFactory() {
        return new MinaCodecFactory() {
            @Override public ProtocolDecoder getDecoder() {
                return new MinaMilterDecoder(getStats());
            }
            
            @Override public ProtocolEncoder getEncoder() {
                return new MinaMilterEncoder(getStats());
            }
        };
    }
    
    @Override public MilterConfig getConfig() {
        return (MilterConfig) super.getConfig();
    }
    
    @Override public Log getLog() {
        return ZimbraLog.milter;
    }
}
