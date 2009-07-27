/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 *
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.imap;

import com.zimbra.cs.mina.LineBuffer;
import org.apache.mina.filter.codec.ProtocolDecoderAdapter;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;

public class MinaImapDecoder extends ProtocolDecoderAdapter {
    private final LineBuffer buf = new LineBuffer();
    private int count = -1;

    private static final int MAX_BYTES = 8192;

    public void decode(IoSession session, ByteBuffer in, ProtocolDecoderOutput out) {
        java.nio.ByteBuffer bb = in.buf();
        while (bb.hasRemaining()) {
            if (count >= 0) {
                int len = Math.min(Math.min(bb.remaining(), count), MAX_BYTES);
                byte[] b = new byte[len];
                bb.get(b);
                out.write(b);
                count -= len;
                if (count == 0) {
                    count = -1;
                }
            } else if (buf.parse(bb)) {
                String line = buf.toString();
                out.write(line);
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
