/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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
package com.zimbra.common.zmime;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.mail.internet.SharedInputStream;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.util.ByteUtil.PositionInputStream;
import com.zimbra.common.util.ByteUtil.SegmentInputStream;

public class ZSharedFileInputStream extends PositionInputStream implements SharedInputStream {
    private final File file;
    private final long soff, eoff;

    public ZSharedFileInputStream(String path) {
        this(new File(path));
    }

    public ZSharedFileInputStream(File file) {
        super(null);
        this.file = file;
        this.soff = 0;
        this.eoff = file.length();
    }

    public ZSharedFileInputStream(File file, long soff, long eoff) {
        super(null);
        assert soff <= eoff;
        this.file = file;
        this.soff = soff;
        this.eoff = eoff;
    }

    private void buffer() throws IOException {
        if (in == null) {
            if (soff == 0 && eoff == file.length()) {
                in = new BufferedInputStream(new FileInputStream(file), 4096);
            } else {
                // not sure what the better way to do this is...
                in = new BufferedInputStream(SegmentInputStream.create(new FileInputStream(file), soff, eoff));
//                InputStream rafis = Channels.newInputStream(new RandomAccessFile(file, "r").getChannel().position(soff));
//                in = new BufferedInputStream(new SegmentInputStream(rafis, eoff - soff), 4096);
            }
        }
    }

    @Override
    public int read() throws IOException {
        buffer();
        return super.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        buffer();
        return super.read(b, off, len);
    }

    @Override
    public synchronized boolean markSupported() {
        try {
            buffer();
            return super.markSupported();
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public synchronized void mark(int readlimit) {
        try {
            buffer();
            super.mark(readlimit);
        } catch (IOException e) {
            // well, we must be at position 0, so what the hell...
        }
    }

    @Override
    public synchronized void reset() throws IOException {
        buffer();
        super.reset();
    }

    @Override
    public long skip(long n) throws IOException {
        buffer();
        return super.skip(n);
    }

    @Override
    public int available() throws IOException {
        return (int) (eoff - soff);
    }

    @Override
    public ZSharedFileInputStream newStream(long start, long end) {
        return new ZSharedFileInputStream(file, Math.min(eoff, soff + start), end >= 0 ? Math.min(eoff, soff + end) : eoff);
    }

    @VisibleForTesting
    boolean isBuffered() {
        return in != null;
    }
}
