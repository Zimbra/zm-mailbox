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

import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.common.ByteBuffer;

import com.zimbra.cs.mina.MinaStats;

public class MinaMilterEncoder extends ProtocolEncoderAdapter {
    private final MinaStats stats;
    
    MinaMilterEncoder(MinaStats stats) {
        this.stats = stats;
    }
    
    @Override public void encode(IoSession session, Object msg, ProtocolEncoderOutput out) {
        MilterPacket packet = (MilterPacket) msg;
        
        ByteBuffer buffer = ByteBuffer.allocate(4 + packet.getLength(), false);
        buffer.setAutoExpand(true);
        buffer.putInt(packet.getLength());
        buffer.put(packet.getCommand());       
        byte[] data = packet.getData();
        if (data != null && data.length > 0)
            buffer.put(data);
        buffer.flip();
        out.write(buffer);
        
        if (stats != null) {
            stats.sentBytes.addAndGet(buffer.capacity());
        }
    }
}
