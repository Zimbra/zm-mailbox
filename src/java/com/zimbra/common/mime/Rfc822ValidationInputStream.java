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
