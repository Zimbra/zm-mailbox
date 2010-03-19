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
package com.zimbra.cs.imap;

import com.zimbra.cs.mina.LineBuffer;
import com.zimbra.cs.mina.MinaStats;
import com.zimbra.common.localconfig.LC;
import org.apache.mina.filter.codec.ProtocolDecoderAdapter;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;

public class MinaImapDecoder extends ProtocolDecoderAdapter {
    private final MinaStats stats;
    private final LineBuffer buf = new LineBuffer();
    private int count = -1;

    private static final int MAX_BYTES = LC.nio_imap_write_chunk_size.intValue();

    MinaImapDecoder(MinaStats stats) {
        this.stats = stats;
    }

    public void decode(IoSession session, ByteBuffer in, ProtocolDecoderOutput out) {
        java.nio.ByteBuffer bb = in.buf();
        while (bb.hasRemaining()) {
            if (count >= 0) {
                int len = Math.min(Math.min(bb.remaining(), count), MAX_BYTES);
                byte[] b = new byte[len];
                bb.get(b);
                out.write(b);
                if (stats != null) {
                    stats.receivedBytes.addAndGet(len);
                }
                count -= len;
                if (count == 0) {
                    count = -1;
                }
            } else if (buf.parse(bb)) {
                String line = buf.getLine();
                out.write(line);
                if (stats != null) {
                    stats.receivedBytes.addAndGet(buf.size());
                }
                buf.reset();
                try {
                    LiteralInfo li = LiteralInfo.parse(line);
                    if (li != null) {
                        count = li.count;
                    }
                } catch (IllegalArgumentException e) {
                    // Let handler send error response
                }
            }
        }
    }

}
