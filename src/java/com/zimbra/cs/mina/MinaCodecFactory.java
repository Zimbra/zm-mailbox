/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.mina;

import org.apache.mina.common.IoSession;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

/*
 * MINA protocol codec factory. Decodes request bytes and then passes complete
 * request to MINA handler.
 */
public abstract class MinaCodecFactory implements ProtocolCodecFactory {
    protected MinaCodecFactory() {
    }

    public ProtocolEncoder getEncoder() {
        return new ProtocolEncoderAdapter() {
            public void encode(IoSession session, Object msg, ProtocolEncoderOutput out) {
                if (msg instanceof ByteBuffer) {
                    ByteBuffer bb = (ByteBuffer) msg;
                    out.write(bb);
                }
            }
        };
    }
}
