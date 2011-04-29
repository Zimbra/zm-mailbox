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

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

import com.zimbra.cs.mina.MinaStats;

public class MinaMilterDecoder extends CumulativeProtocolDecoder {
    private final MinaStats stats;
    
    MinaMilterDecoder(MinaStats stats) {
        this.stats = stats;
    }
    
    @Override public boolean doDecode(IoSession session, ByteBuffer in, ProtocolDecoderOutput out) {
        if (!in.prefixedDataAvailable(4))
            return false;
        
        int len = in.getInt();
        byte cmd = in.get();
        byte[] data = null;
        if (len > 1) {
            data = new byte[len - 1];
            in.get(data);
        }
        MilterPacket packet = new MilterPacket(len, cmd, data);
        out.write(packet);
        
        if (stats != null) {
            stats.receivedBytes.addAndGet(len + 4);
        }
        return true;
    }
}
