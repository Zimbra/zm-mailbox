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

package com.zimbra.cs.pop3;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.tcpserver.NioHandler;
import com.zimbra.cs.tcpserver.NioServer;
import com.zimbra.cs.tcpserver.NioCodecFactory;
import com.zimbra.cs.tcpserver.NioConnection;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;

public class NioPop3Server extends NioServer implements Pop3Server {

    public NioPop3Server(Pop3Config config) throws ServiceException {
        super(config);
        registerMBean(config.isSslEnabled() ? "NioPop3SSLServer" : "NioPop3Server");
    }

    @Override
    public NioHandler createHandler(NioConnection conn) {
        return new NioPop3Handler(this, conn);
    }

    @Override
    protected ProtocolCodecFactory getProtocolCodecFactory() {
        return new NioCodecFactory() {
            @Override
            public ProtocolDecoder getDecoder(IoSession session) {
                return new NioPop3Decoder(getStats());
            }
        };
    }

    @Override
    public Pop3Config getConfig() {
        return (Pop3Config) super.getConfig();
    }
}
