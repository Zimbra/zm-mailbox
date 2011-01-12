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

import com.google.common.base.Charsets;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.server.NioHandler;
import com.zimbra.cs.server.NioServer;
import com.zimbra.cs.server.NioConnection;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.textline.LineDelimiter;
import org.apache.mina.filter.codec.textline.TextLineDecoder;

public final class NioPop3Server extends NioServer implements Pop3Server {
    private static final ProtocolDecoder DECODER = new TextLineDecoder(Charsets.ISO_8859_1, LineDelimiter.AUTO);

    public NioPop3Server(Pop3Config config) throws ServiceException {
        super(config);
        registerMBean(getName());
    }

    @Override
    public String getName() {
        return config.isSslEnabled() ? "NioPop3SSLServer" : "NioPop3Server";
    }

    @Override
    public NioHandler createHandler(NioConnection conn) {
        return new NioPop3Handler(this, conn);
    }

    @Override
    protected ProtocolCodecFactory getProtocolCodecFactory() {
        return new ProtocolCodecFactory() {
            @Override
            public ProtocolEncoder getEncoder(IoSession session) throws Exception {
                return DEFAULT_ENCODER;
            }

            @Override
            public ProtocolDecoder getDecoder(IoSession session) {
                return DECODER;
            }
        };
    }

    @Override
    public Pop3Config getConfig() {
        return (Pop3Config) super.getConfig();
    }
}
