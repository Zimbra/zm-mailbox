/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
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
package com.zimbra.common.util;

import java.io.IOException;
import java.io.Writer;

/**
 * Wraps a <tt>Writer</tt>.  Writes up to the specified number
 * of characters and ignores the rest.
 */
public class TruncatingWriter extends Writer {

    private final Writer mWriter;
    private final int mMaxChars;
    
    private int mCharsWritten;
    private boolean mWasTruncated = false;

    /**
     * @param writer the <tt>Writer</tt> to which data will be written
     * @param maxChars the maximum number of characters to write
     */
    public TruncatingWriter(Writer writer, int maxChars) {
        if (writer == null) {
            throw new NullPointerException("writer cannot be null");
        }
        if (maxChars < 0) {
            throw new IllegalArgumentException("maxChars cannot be less than 0");
        }
        mWriter = writer;
        mMaxChars = maxChars;
    }
    
    public boolean wasTruncated() {
        return mWasTruncated;
    }
    
    @Override
    public void close() throws IOException {
        mWriter.close();
    }

    @Override
    public void flush() throws IOException {
        mWriter.flush();
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        if (mWasTruncated) {
            return;
        }
        if (cbuf != null && mCharsWritten + len >= mMaxChars) {
            // Hit the limit
            int actualLen = Math.min(len, mMaxChars - mCharsWritten);
            if (actualLen > 0) {
                mWriter.write(cbuf, off, actualLen);
                mCharsWritten += actualLen;
            }
            mWasTruncated = true;
        } else {
            mWriter.write(cbuf, off, len);
            mCharsWritten += len;
        }
    }
}
