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
