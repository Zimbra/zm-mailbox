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

package com.zimbra.cs.nio;

import com.zimbra.cs.server.ServerConfig;

import java.nio.ByteBuffer;
import java.io.IOException;
import java.io.OutputStream;

public class NioOutputStream extends OutputStream {
    private NioSession session;
    private ByteBuffer buf;

    public NioOutputStream(NioSession session) {
        this.session = session;
    }

    @Override
    public synchronized void write(byte[] b, int off, int len)
            throws IOException {
        if ((off | len | (b.length - (len + off)) | (off + len)) < 0) {
            throw new IndexOutOfBoundsException();
        }
        while (len > 0) {
            if (buf == null) {
                buf = ByteBuffer.allocate(
                    Math.max(len, getConfig().getNioWriteChunkSize()));
            }
            int count = Math.min(len, buf.remaining());
            buf.put(b, off, count);
            if (!buf.hasRemaining()) {
                flush();
            }
            len -= count;
            off += count;
        }
    }

     public void write(String s) throws IOException {
         write(s.getBytes("UTF8"));
     }

     @Override
     public void write(int b) throws IOException {
         write(new byte[] { (byte) b });
     }

     @Override
     public void flush() throws IOException {
         if (buf != null && buf.position() > 0) {
             buf.flip();
             send(buf);
             buf = null;
         }
     }

     private void send(ByteBuffer bb) throws IOException {
         session.send(bb);
         int threshold = getConfig().getNioMaxScheduledWriteBytes();
         if (threshold > 0 && threshold < session.getScheduledWriteBytes()) {
             long timeout = getConfig().getNioWriteTimeout() * 1000;
             if (!session.drainWriteQueue(threshold, timeout)) {
                 throw new IOException("Timed-out while writing data");
             }
         }
     }

     private ServerConfig getConfig() {
         return session.getServer().getConfig();
     }

     @Override
     public void close() throws IOException {
         flush();
         session.close();
     }
}
