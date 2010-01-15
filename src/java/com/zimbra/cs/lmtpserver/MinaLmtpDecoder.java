/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.lmtpserver;

import com.zimbra.cs.mina.LineBuffer;
import com.zimbra.cs.mina.MinaServer;
import com.zimbra.cs.mina.MinaStats;
import org.apache.mina.filter.codec.ProtocolDecoderAdapter;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.ByteBuffer;

public class MinaLmtpDecoder extends ProtocolDecoderAdapter {
    private final MinaStats stats;
    private final LineBuffer cmd = new LineBuffer(132);
    private LineBuffer data;

    MinaLmtpDecoder(MinaStats stats) {
        this.stats = stats;
    }
    
    public void decode(IoSession session, ByteBuffer in, ProtocolDecoderOutput out) {
        java.nio.ByteBuffer bb = in.buf();
        while (bb.hasRemaining()) {
            if (data != null) {
                // Receiving LMTP data...
                if (data.parse(bb)) {
                    if (stats != null) {
                        stats.receivedBytes.addAndGet(data.size());
                    }
                    out.write(data);
                    if (data.matches(".\r\n")) {
                        data = null;
                    } else {
                        // Assume subsequent lines are of the same size
                        data = new LineBuffer(data.size());
                    }
                }
            } else if (cmd.parse(bb)) {
                String line = cmd.toString().trim();
                out.write(line);
                if (stats != null) {
                    stats.receivedBytes.addAndGet(cmd.size());
                }
                cmd.reset();
                if ("DATA".equalsIgnoreCase(getCommand(line))) {
                    data = new LineBuffer();
                }
            }
        }
    }

    private static String getCommand(String line) {
        int i = line.indexOf(' ');
        return i > 0 ? line.substring(0, i) : line;
    }
}
