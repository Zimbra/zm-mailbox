/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2007 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.store;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import javax.mail.internet.SharedInputStream;

import com.zimbra.common.util.ZimbraLog;


public class BlobInputStream extends InputStream
implements SharedInputStream {
    
    private File mFile;
    private RandomAccessFile mRAF;
    private Long mMarkPos;
    private long mPos;
    private int mMarkReadLimit;
    private long mStart;
    private long mEnd;

    public BlobInputStream(File file)
    throws IOException {
        this(file, null, null);
    }

    private BlobInputStream(File file, Long start, Long end)
    throws IOException {
        mFile = file;
        mRAF = new RandomAccessFile(file, "r");
        if (start == null) {
            mStart = 0;
            mPos = 0;
        } else {
            mRAF.seek(start);
            mStart = start;
            mPos = start;
        }
        if (end == null) {
            mEnd = mRAF.length();
        } else {
            if (end > mRAF.length()) {
                String msg = String.format("end value %d exceeded file size %d", end, mRAF.length());
                throw new IOException(msg);
            }
            mEnd = end;
        }
    }
    
    ////////////// InputStream methods //////////////
    
    @Override
    public int available() {
        return (int) (mEnd - mPos);
    }

    @Override
    public void close() throws IOException {
        mRAF.close();
        mPos = mEnd;
    }

    @Override
    public synchronized void mark(int readlimit) {
        mMarkPos = mPos;
        mMarkReadLimit = readlimit;
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public int read() throws IOException {
        if (mPos >= mEnd) {
            return -1;
        }
        int c = mRAF.read();
        if (c >= 0) {
            mPos++;
        }
        return c;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (mPos >= mEnd) {
            return -1;
        }
        // Make sure we don't read past the endpoint passed to the constructor
        len = (int) Math.min(len, mEnd - mPos);
        
        int numRead = mRAF.read(b, off, len);
        if (numRead >= 0) {
            mPos += numRead;
        }
        return numRead;
    }

    @Override
    public synchronized void reset() throws IOException {
        if (mMarkPos == null) {
            throw new IOException("reset() called before mark()");
        }
        if (mPos - mMarkPos > mMarkReadLimit) {
            throw new IOException("Mark position was invalidated because more than " + mMarkReadLimit + " bytes were read.");
        }
        mRAF.seek(mMarkPos);
        mPos = mMarkPos;
    }

    @Override
    public long skip(long n) throws IOException {
        if (n <= 0 || mPos >= mEnd) {
            return 0;
        }
        n = (int) Math.min(n, mEnd - mPos);
        int numSkipped = mRAF.skipBytes((int) n);
        mPos += numSkipped;
        return numSkipped;
    }
    
    @Override
    protected void finalize() throws Throwable {
        mRAF.close();
    }

    ////////////// SharedInputStream methods //////////////

    public long getPosition() {
        // If this is a substream, return the position relative to the
        // starting point.  If this is the main stream, mStart = 0.
        return mPos - mStart;
    }

    public InputStream newStream(long start, long end) {
        if (start < 0) {
            throw new IllegalArgumentException("start cannot be less than 0");
        }
        // The start and end markers are relative to this
        // stream's view of the file, not necessarily the entire file.
        // Calculate the actual start/end offsets in the file.
        start += mStart;
        if (end < 0) {
            end = mEnd;
        } else {
            end += mStart;
        }
        try {
            return new BlobInputStream(mFile, start, end);
        } catch (IOException e) {
            ZimbraLog.misc.warn("Unable to create substream for %s", mFile.getPath(), e);
        }
        return null;
    }
}
