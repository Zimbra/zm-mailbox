/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.mime;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Passthrough <tt>InputStream</tt> that performs very simple validation of
 * an RFC822 message.  If a line in the message data exceeds the maximum number
 * of characters, {@link #isValid} returns <tt>false</tt>.
 */
public class Rfc822ValidationInputStream
extends FilterInputStream {
    
    private long mMaxLineLength;
    private long mCurrentLineLength = 0;
    private boolean mIsValid = true;
    
    public Rfc822ValidationInputStream(InputStream in, long maxLineLength) {
        super(in);
        mMaxLineLength = maxLineLength;
    }
    
    public boolean isValid() {
        return mIsValid;
    }

    @Override
    public int read() throws IOException {
        int c = super.read();
        if (c >= 0) {
            check(c);
        }
        return c;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int numRead = super.read(b, off, len);
        if (numRead > 0) {
            for (int i = 0; i < numRead; i++) {
                check(b[off + i]);
            }
        }
        return numRead;
    }
    
    private void check(int c) {
        if (c == '\r' || c == '\n') {
            mCurrentLineLength = 0;
        } else {
            mCurrentLineLength++;
            if (mCurrentLineLength > mMaxLineLength) {
                mIsValid = false;
            }
        }
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public synchronized void reset() throws IOException {
        mCurrentLineLength = 0;
        super.reset();
    }

    @Override
    public long skip(long n) throws IOException {
        mCurrentLineLength = 0;
        return super.skip(n);
    }
}
